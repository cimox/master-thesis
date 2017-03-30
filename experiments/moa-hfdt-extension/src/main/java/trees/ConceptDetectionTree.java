package trees;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.conditionaltests.InstanceConditionalTest;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.core.DoubleVector;
import moa.core.MiscUtils;
import moa.core.Utils;
import com.yahoo.labs.samoa.instances.Instance;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import redis.clients.jedis.Jedis;
import utils.MyKafkaProducer;

public class ConceptDetectionTree extends MyHoeffdingTree {

    private static final long serialVersionUID = 1L;

    private PrintWriter conceptFileWriter;
    private boolean printToKafka;
    private String kafkaTopic = "experiment";
    private MyKafkaProducer producer;
    private JSONObject previousRoot;
    private Jedis redis;

    public ConceptDetectionTree(PrintWriter conceptFileWriter) {
        this.conceptFileWriter = conceptFileWriter;
        this.printToKafka = false;
        initKafkaProducer();
        initRedis();
    }

    public ConceptDetectionTree(PrintWriter conceptFileWriter, boolean printToKafka) {
        this.conceptFileWriter = conceptFileWriter;
        this.printToKafka = printToKafka;
        initKafkaProducer();
        initRedis();
    }

    public ConceptDetectionTree(PrintWriter conceptFileWriter, boolean printToKafka, String kafkaTopic) {
        this.conceptFileWriter = conceptFileWriter;
        this.printToKafka = printToKafka;
        this.kafkaTopic = kafkaTopic;
        initKafkaProducer(this.kafkaTopic);
        initRedis();
    }

    public ConceptDetectionTree(PrintWriter conceptFileWriter, boolean printToKafka, String kafkaTopic,
                                String redisHost, int redistPort) {
        this.conceptFileWriter = conceptFileWriter;
        this.printToKafka = printToKafka;
        this.kafkaTopic = kafkaTopic;
        initKafkaProducer(this.kafkaTopic);
        initRedis(redisHost, redistPort);
    }

    private void initKafkaProducer() {
        this.producer = new MyKafkaProducer();
    }

    private void initKafkaProducer(String kafkaTopic) {
        this.producer = new MyKafkaProducer(kafkaTopic);
    }

    private void initRedis() {
        this.redis = new Jedis("localhost", 6379);
        this.redis.flushAll();
    }

    private void initRedis(String host, int port) {
        this.redis = new Jedis(host, port);
    }

    @Override
    public String getPurposeString() {
        return "Hoeffding Adaptive Tree for evolving data streams that uses ADWIN to replace branches for new ones.";
    }

    public interface NewNode {

        // Change for adwin
        //public boolean getErrorChange();
        public int numberLeaves();

        public double getErrorEstimation();

        public double getErrorWidth();

        public boolean isNullError();

        public void killTreeChilds(ConceptDetectionTree ht);

        public void learnFromInstance(Instance inst, ConceptDetectionTree ht, SplitNode parent, int parentBranch);

        public void filterInstanceToLeaves(Instance inst, SplitNode myparent, int parentBranch, List<FoundNode> foundNodes,
                                           boolean updateSplitterCounts);
    }

    public static class AdaSplitNode extends SplitNode implements NewNode {

        private static final long serialVersionUID = 1L;

        private PrintWriter conceptFileWriter;
        private Jedis redis;
        protected Node alternateTree;
        protected ADWIN estimationErrorWeight;
        public boolean ErrorChange = false;
        protected int randomSeed = 1;
        protected Random classifierRandom;

        @Override
        public int calcByteSizeIncludingSubtree() {
            int byteSize = calcByteSize();
            if (alternateTree != null) {
                byteSize += alternateTree.calcByteSizeIncludingSubtree();
            }
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            for (Node child : this.children) {
                if (child != null) {
                    byteSize += child.calcByteSizeIncludingSubtree();
                }
            }
            return byteSize;
        }

        public AdaSplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size, PrintWriter pw,
                            Jedis redis) {
            super(splitTest, classObservations, size);
            this.classifierRandom = new Random(this.randomSeed);
            this.conceptFileWriter = pw;
            this.redis = redis;
        }

        public AdaSplitNode(InstanceConditionalTest splitTest, double[] classObservations, PrintWriter pw,
                            Jedis redis) {
            super(splitTest, classObservations);
            this.classifierRandom = new Random(this.randomSeed);
            this.conceptFileWriter = pw;
            this.redis = redis;
        }

        @Override
        public int numberLeaves() {
            int numLeaves = 0;
            for (Node child : this.children) {
                if (child != null) {
                    numLeaves += ((NewNode) child).numberLeaves();
                }
            }
            return numLeaves;
        }

        @Override
        public double getErrorEstimation() {
            return this.estimationErrorWeight.getEstimation();
        }

        @Override
        public double getErrorWidth() {
            double w = 0.0;
            if (!isNullError()) {
                w = this.estimationErrorWeight.getWidth();
            }
            return w;
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        // SplitNodes can have alternative trees, but LearningNodes can't
        // LearningNodes can split, but SplitNodes can't
        // Parent nodes are allways SplitNodes
        @Override
        public void learnFromInstance(Instance inst, ConceptDetectionTree ht, SplitNode parent, int parentBranch) {
            String replacementStatus = "none";

            int trueClass = (int) inst.classValue();
            // New option core
            Instance weightedInst = inst.copy();

            // Compute ClassPrediction using filterInstanceToLeaf
            int ClassPrediction = 0;
            if (filterInstanceToLeaf(inst, parent, parentBranch).node != null) {
                ClassPrediction = Utils.maxIndex(filterInstanceToLeaf(inst, parent, parentBranch).node.getClassVotes(inst, ht));
            }

            boolean blCorrect = (trueClass == ClassPrediction);

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }
            double oldError = this.getErrorEstimation();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect ? 0.0 : 1.0);
            if (this.ErrorChange && oldError > this.getErrorEstimation()) {
                // If error is decreasing, don't do anything
                this.ErrorChange = false;
            }

            // Check condition to build a new alternate tree
            if (this.ErrorChange) {
                // Start a new alternative tree : learning node
                this.alternateTree = ht.newLearningNode();
                ht.alternateTrees++;
            } else if (this.alternateTree != null && !((NewNode) this.alternateTree).isNullError()) { // Check condition to replace tree
                if (this.getErrorWidth() > 300 && ((NewNode) this.alternateTree).getErrorWidth() > 300) {
                    double fDelta = .05;
                    double fN = 1.0 / ((NewNode) this.alternateTree).getErrorWidth() + 1.0 / this.getErrorWidth();
                    double oldErrorRate = this.getErrorEstimation();
                    double altErrorRate = ((NewNode) this.alternateTree).getErrorEstimation();
                    double hoeffdingBound = Math.sqrt(2.0 * oldErrorRate * (1.0 - oldErrorRate) * Math.log(2.0 / fDelta) * fN);
                    double diffErrorRate = oldErrorRate - altErrorRate;

                    this.redis.lpush(this.nodeID + "_hoeffdingBound", String.valueOf(hoeffdingBound));
                    this.redis.lpush(this.nodeID + "_oldErrorRate", String.valueOf(oldErrorRate));
                    this.redis.lpush(this.nodeID + "_altErrorRate", String.valueOf(altErrorRate));
                    this.redis.lpush(this.nodeID + "_diff", String.valueOf(diffErrorRate));

                    if (hoeffdingBound < oldErrorRate - altErrorRate) {
                        // Switch alternate tree
                        System.out.println("Old tree " + this.splitTest.hashCode()
                                + " replaced with alternating tree, bound: " + hoeffdingBound);
                        replacementStatus = "replaced";

                        ht.activeLeafNodeCount -= this.numberLeaves();
                        ht.activeLeafNodeCount += ((NewNode) this.alternateTree).numberLeaves();
                        killTreeChilds(ht);
                        if (parent != null) {
                            parent.setChild(parentBranch, this.alternateTree);
                        } else {
                            // Switch root tree
                            ht.treeRoot = ((AdaSplitNode) ht.treeRoot).alternateTree;
                        }
                        ht.switchedAlternateTrees++;
                    } else if (hoeffdingBound < altErrorRate - oldErrorRate) {
                        replacementStatus = "erased";

                        // Erase alternate tree
                        if (this.alternateTree instanceof ActiveLearningNode) {
                            this.alternateTree = null;
                        } else if (this.alternateTree instanceof InactiveLearningNode) {
                            this.alternateTree = null;
                        } else {
                            ((AdaSplitNode) this.alternateTree).killTreeChilds(ht);
                        }
                        ht.prunedAlternateTrees++;
                    }
                    this.conceptFileWriter.println(replacementStatus);
                }
                else {
                    double fDelta = .05;
                    double fN = 1.0 / ((NewNode) this.alternateTree).getErrorWidth() + 1.0 / this.getErrorWidth();
                    double oldErrorRate = this.getErrorEstimation();
                    double altErrorRate = ((NewNode) this.alternateTree).getErrorEstimation();
                    double hoeffdingBound = Math.sqrt(2.0 * oldErrorRate * (1.0 - oldErrorRate) * Math.log(2.0 / fDelta) * fN);
                    double diffErrorRate = oldErrorRate - altErrorRate;

                    this.redis.lpush(this.nodeID + "_hoeffdingBound", String.valueOf(hoeffdingBound));
                    this.redis.lpush(this.nodeID + "_oldErrorRate", String.valueOf(oldErrorRate));
                    this.redis.lpush(this.nodeID + "_altErrorRate", String.valueOf(altErrorRate));
                    this.redis.lpush(this.nodeID + "_diff", String.valueOf(diffErrorRate));
                }
            }
            this.redis.set(this.nodeID + "_status", replacementStatus);

            //learnFromInstance alternate Tree and Child nodes
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).learnFromInstance(weightedInst, ht, parent, parentBranch);
            }
            int childBranch = this.instanceChildIndex(inst);
            Node child = this.getChild(childBranch);
            if (child != null) {
                ((NewNode) child).learnFromInstance(weightedInst, ht, this, childBranch);
            }
        }

        @Override
        public void killTreeChilds(ConceptDetectionTree ht) {
            for (Node child : this.children) {
                if (child != null) {
                    //Delete alternate tree if it exists
                    if (child instanceof AdaSplitNode && ((AdaSplitNode) child).alternateTree != null) {
                        ((NewNode) ((AdaSplitNode) child).alternateTree).killTreeChilds(ht);
                        ht.prunedAlternateTrees++;
                    }
                    //Recursive delete of SplitNodes
                    if (child instanceof AdaSplitNode) {
                        ((NewNode) child).killTreeChilds(ht);
                    }
                    if (child instanceof ActiveLearningNode) {
                        child = null;
                        ht.activeLeafNodeCount--;
                    } else if (child instanceof InactiveLearningNode) {
                        child = null;
                        ht.inactiveLeafNodeCount--;
                    }
                }
            }
        }

        //New for option votes
        //@Override
        public void filterInstanceToLeaves(Instance inst, SplitNode myparent,
                                           int parentBranch, List<FoundNode> foundNodes,
                                           boolean updateSplitterCounts) {
            if (updateSplitterCounts) {
                this.observedClassDistribution.addToValue((int) inst.classValue(), inst.weight());
            }
            int childIndex = instanceChildIndex(inst);
            if (childIndex >= 0) {
                Node child = getChild(childIndex);
                if (child != null) {
                    ((NewNode) child).filterInstanceToLeaves(inst, this, childIndex,
                            foundNodes, updateSplitterCounts);
                } else {
                    foundNodes.add(new FoundNode(null, this, childIndex));
                }
            }
            if (this.alternateTree != null) {
                ((NewNode) this.alternateTree).filterInstanceToLeaves(inst, this, -999,
                        foundNodes, updateSplitterCounts);
            }
        }
    }

    public static class AdaLearningNode extends LearningNodeNBAdaptive implements NewNode {

        private static final long serialVersionUID = 1L;

        protected ADWIN estimationErrorWeight;

        public boolean ErrorChange = false;

        protected int randomSeed = 1;

        protected Random classifierRandom;

        @Override
        public int calcByteSize() {
            int byteSize = super.calcByteSize();
            if (estimationErrorWeight != null) {
                byteSize += estimationErrorWeight.measureByteSize();
            }
            return byteSize;
        }

        public AdaLearningNode(double[] initialClassObservations) {
            super(initialClassObservations);
            this.classifierRandom = new Random(this.randomSeed);
        }

        @Override
        public int numberLeaves() {
            return 1;
        }

        @Override
        public double getErrorEstimation() {
            if (this.estimationErrorWeight != null) {
                return this.estimationErrorWeight.getEstimation();
            } else {
                return 0;
            }
        }

        @Override
        public double getErrorWidth() {
            return this.estimationErrorWeight.getWidth();
        }

        @Override
        public boolean isNullError() {
            return (this.estimationErrorWeight == null);
        }

        @Override
        public void killTreeChilds(ConceptDetectionTree ht) {
        }

        @Override
        public void learnFromInstance(Instance inst, ConceptDetectionTree ht, SplitNode parent, int parentBranch) {
            int trueClass = (int) inst.classValue();

            //New option vore
            int k = MiscUtils.poisson(1.0, this.classifierRandom);
            Instance weightedInst = (Instance) inst.copy();
            if (k > 0) {
                weightedInst.setWeight(inst.weight() * k);
            }
            //Compute ClassPrediction using filterInstanceToLeaf
            int ClassPrediction = Utils.maxIndex(this.getClassVotes(inst, ht));

            boolean blCorrect = (trueClass == ClassPrediction);

            if (this.estimationErrorWeight == null) {
                this.estimationErrorWeight = new ADWIN();
            }
            double oldError = this.getErrorEstimation();
            this.ErrorChange = this.estimationErrorWeight.setInput(blCorrect == true ? 0.0 : 1.0);
            if (this.ErrorChange && oldError > this.getErrorEstimation()) {
                this.ErrorChange = false;
            }

            //Update statistics
            learnFromInstance(weightedInst, ht);

            //Check for Split condition
            double weightSeen = this.getWeightSeen();
            if (weightSeen
                    - this.getWeightSeenAtLastSplitEvaluation() >= ht.gracePeriodOption.getValue()) {
                ht.attemptToSplit(this, parent, parentBranch);
                this.setWeightSeenAtLastSplitEvaluation(weightSeen);
            }
        }

        @Override
        public double[] getClassVotes(Instance inst, MyHoeffdingTree ht) {
            double[] dist;
            int predictionOption = ((ConceptDetectionTree) ht).leafpredictionOption.getChosenIndex();
            if (predictionOption == 0) { //MC
                dist = this.observedClassDistribution.getArrayCopy();
            } else if (predictionOption == 1) { //NB
                dist = NaiveBayes.doNaiveBayesPrediction(inst,
                        this.observedClassDistribution, this.attributeObservers);
            } else { //NBAdaptive
                if (this.mcCorrectWeight > this.nbCorrectWeight) {
                    dist = this.observedClassDistribution.getArrayCopy();
                } else {
                    dist = NaiveBayes.doNaiveBayesPrediction(inst,
                            this.observedClassDistribution, this.attributeObservers);
                }
            }
            //New for option votes
            double distSum = Utils.sum(dist);
            if (distSum * this.getErrorEstimation() * this.getErrorEstimation() > 0.0) {
                Utils.normalize(dist, distSum * this.getErrorEstimation() * this.getErrorEstimation()); //Adding weight
            }
            return dist;
        }

        //New for option votes
        @Override
        public void filterInstanceToLeaves(Instance inst,
                                           SplitNode splitparent, int parentBranch,
                                           List<FoundNode> foundNodes, boolean updateSplitterCounts) {
            foundNodes.add(new FoundNode(this, splitparent, parentBranch));
        }
    }

    protected int alternateTrees;

    protected int prunedAlternateTrees;

    protected int switchedAlternateTrees;

    @Override
    protected LearningNode newLearningNode(double[] initialClassObservations) {
        // IDEA: to choose different learning nodes depending on predictionOption
        return new AdaLearningNode(initialClassObservations);
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest, double[] classObservations, int size) {
        return new AdaSplitNode(splitTest, classObservations, size, this.conceptFileWriter, this.redis);
    }

    @Override
    protected SplitNode newSplitNode(InstanceConditionalTest splitTest, double[] classObservations) {
        return new AdaSplitNode(splitTest, classObservations, this.conceptFileWriter, this.redis);
    }

    private JSONObject prepareNewTreeRoot() {
        JSONObject root = new JSONObject();
        JSONArray rootChildren = new JSONArray();
        root.put("className", "root");
        root.put("children", rootChildren);
        return root;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (this.treeRoot == null) {
            this.treeRoot = newLearningNode();
            this.activeLeafNodeCount = 1;
        } else if (this.printToKafka) {
            JSONObject root = prepareNewTreeRoot();
            getModelDescriptionJSON((JSONArray) root.get("children"));

            try {
                if (!root.toJSONString().equals(this.previousRoot.toJSONString())) {
                    this.producer.sendMessage("key", root.toJSONString());
                }
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
            this.previousRoot = root;
        }
        ((NewNode) this.treeRoot).learnFromInstance(inst, this, null, -1);
    }

    //New for options vote
    public FoundNode[] filterInstanceToLeaves(Instance inst,
                                              SplitNode parent, int parentBranch, boolean updateSplitterCounts) {
        List<FoundNode> nodes = new LinkedList<FoundNode>();
        ((NewNode) this.treeRoot).filterInstanceToLeaves(inst, parent, parentBranch, nodes, updateSplitterCounts);
        return nodes.toArray(new FoundNode[nodes.size()]);
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        if (this.treeRoot != null) {
            FoundNode[] foundNodes = filterInstanceToLeaves(inst,
                    null, -1, false);
            DoubleVector result = new DoubleVector();
            int predictionPaths = 0;
            for (FoundNode foundNode : foundNodes) {
                if (foundNode.parentBranch != -999) {
                    Node leafNode = foundNode.node;
                    if (leafNode == null) {
                        leafNode = foundNode.parent;
                    }
                    double[] dist = leafNode.getClassVotes(inst, this);
                    result.addValues(dist);
                }
            }
            return result.getArrayRef();
        }
        return new double[0];
    }
}
