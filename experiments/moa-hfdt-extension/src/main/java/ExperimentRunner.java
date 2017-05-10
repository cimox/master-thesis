import com.github.javacliparser.*;
import experiments.Experiment;
import experiments.ExperimentException;
import generators.*;
import moa.classifiers.Classifier;
import moa.streams.ArffFileStream;
import moa.streams.InstanceStream;
import moa.streams.generators.*;
import moa.streams.generators.HyperplaneGenerator;
import trees.ConceptDetectionTree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;


public class ExperimentRunner {
    private static int NUM_INSTANCES = 500000;
    private final static boolean IS_TESTING = true;

    public static void main(String[] args) throws IOException {
//        ExperimentConceptDetection exp = new ExperimentConceptDetection();
//        ExperimentTelcoChurn exp = new ExperimentTelcoChurn();
//        ExperimentRandomTree exp = new ExperimentRandomTree();
//        ExperimentRBFND exp = new ExperimentRBFND();

        System.out.println("Running Telco Churn experiment");
        ExperimentTelcoChurn expTelco = new ExperimentTelcoChurn();
        expTelco.runExperiment(NUM_INSTANCES, IS_TESTING);

        System.out.println("Running Airlines experiment");
        ExperimentAirlines expAir = new ExperimentAirlines();
        expAir.runExperiment(NUM_INSTANCES, IS_TESTING);

        System.out.println("Running synthetic experiments");
        ExperimentAllSynthetic allQuantitativeExperiments = new ExperimentAllSynthetic();
        allQuantitativeExperiments.runAllExperiments();
    }

    private static class ExperimentTreeTraining extends Experiment {
        private FileWriter file;
        private BufferedWriter bw;
        private PrintWriter fileWriter;

        private ExperimentTreeTraining() {
            try {
                this.file = new FileWriter("tree-training.json", true);
                this.bw = new BufferedWriter(file);
                this.fileWriter = new PrintWriter(this.bw);
                this.fileWriter.println("[");

                Classifier learner = new ConceptDetectionTree(this.fileWriter);
                HyperplaneGenerator stream = new HyperplaneGenerator();
                stream.numClassesOption = new IntOption("numClasses", 'c',
                        "The number of classes to generate.", 4, 2, Integer.MAX_VALUE);
                stream.numDriftAttsOption = new IntOption("numDriftAtts", 'k',
                        "The number of attributes with drift.", 5, 0, Integer.MAX_VALUE);
                stream.magChangeOption = new FloatOption("magChange", 't',
                        "Magnitude of the change for every example", 0.10, 0.0, 1.0);
                stream.prepareForUse();

                super.prepareExperiment(stream, learner);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
                this.fileWriter.println("]");
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentConceptDetection extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;

        private ExperimentConceptDetection() {
            try {
                // Concept conceptsFile writer.
                this.conceptsFile = new FileWriter("tree-concepts.csv", true);
                this.conceptsBufferedWriter = new BufferedWriter(conceptsFile);
                this.conceptFileWriter = new PrintWriter(this.conceptsBufferedWriter);

                // Accuracy conceptsFile write.
                this.accuracyFile = new FileWriter("accuracy.csv", true);
                this.accuracyBufferedWriter = new BufferedWriter(this.accuracyFile);
                this.accuracyFileWriter = new PrintWriter(this.accuracyBufferedWriter);

                // Create classifier and stream generator.
//                ConceptDetectionTree learner = new ConceptDetectionTree(true);
                ConceptDetectionTree learner = new ConceptDetectionTree(false);
                learner.leafpredictionOption = new MultiChoiceOption(
                        "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                        "MC", "NB", "NBAdaptive"}, new String[]{
                        "Majority class",
                        "Naive Bayes",
                        "Naive Bayes Adaptive"}, 2);
                learner.gracePeriodOption = new IntOption(
                        "gracePeriod",
                        'g',
                        "The number of instances a leaf should observe between split attempts.",
                        200, 0, Integer.MAX_VALUE);

                HyperplaneGenerator stream = new HyperplaneGenerator();
                stream.numClassesOption = new IntOption("numClasses", 'c',
                        "The number of classes to generate.", 2, 2, Integer.MAX_VALUE);
                stream.numDriftAttsOption = new IntOption("numDriftAtts", 'k',
                        "The number of attributes with drift.", 5, 0, Integer.MAX_VALUE);
                stream.magChangeOption = new FloatOption("magChange", 't',
                        "Magnitude of the change for every example", 0.001, 0.0, 1.0);

                stream.prepareForUse();

                // Prepare experiment.
                super.prepareExperiment(stream, learner);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentRandomTree extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;
        private PrintWriter conceptFileWriter, accuracyFileWriter;

        private ExperimentRandomTree() {
            // Create classifier and stream generator.
            ConceptDetectionTree learner = new ConceptDetectionTree(true);
            learner.leafpredictionOption = new MultiChoiceOption(
                    "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                    "MC", "NB", "NBAdaptive"}, new String[]{
                    "Majority class",
                    "Naive Bayes",
                    "Naive Bayes Adaptive"}, 2);
            learner.gracePeriodOption = new IntOption(
                    "gracePeriod",
                    'g',
                    "The number of instances a leaf should observe between split attempts.",
                    200, 0, Integer.MAX_VALUE);

            RandomTreeGenerator stream = new RandomTreeGenerator();
            stream.prepareForUse();

            // Prepare experiment.
            super.prepareExperiment(stream, learner);
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentRBFND extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;
        private PrintWriter conceptFileWriter, accuracyFileWriter;

        private ExperimentRBFND() {
            // Create classifier and stream generator.
            ConceptDetectionTree learner = new ConceptDetectionTree(true);
            learner.leafpredictionOption = new MultiChoiceOption(
                    "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                    "MC", "NB", "NBAdaptive"}, new String[]{
                    "Majority class",
                    "Naive Bayes",
                    "Naive Bayes Adaptive"}, 2);
            learner.gracePeriodOption = new IntOption(
                    "gracePeriod",
                    'g',
                    "The number of instances a leaf should observe between split attempts.",
                    5, 0, Integer.MAX_VALUE);

            RandomRBFGenerator stream = new RandomRBFGenerator();
            stream.prepareForUse();

            // Prepare experiment.
            super.prepareExperiment(stream, learner);
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentTelcoChurn extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;

        private ExperimentTelcoChurn() throws IOException {
            // Create classifier and stream generator.
            ConceptDetectionTree learner = new ConceptDetectionTree(true);
            learner.leafpredictionOption = new MultiChoiceOption(
                    "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                    "MC", "NB", "NBAdaptive"}, new String[]{
                    "Majority class",
                    "Naive Bayes",
                    "Naive Bayes Adaptive"}, 2);
            learner.gracePeriodOption = new IntOption(
                    "gracePeriod",
                    'g',
                    "The number of instances a leaf should observe between split attempts.",
                    5, 0, Integer.MAX_VALUE);

            TelcoChurn stream = new TelcoChurn();
            stream.prepareForUse();
            NUM_INSTANCES = 7043;

            this.accuracyFile = new FileWriter("telco.csv", true);
            this.accuracyBufferedWriter = new BufferedWriter(this.accuracyFile);
            this.accuracyFileWriter = new PrintWriter(this.accuracyBufferedWriter);

            // Prepare experiment.
            super.prepareExperiment(stream, learner);
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentAirlines extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;

        private ExperimentAirlines() throws IOException {
            // Create classifier and stream generator.
            ConceptDetectionTree learner = new ConceptDetectionTree(true);
            learner.leafpredictionOption = new MultiChoiceOption(
                    "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                    "MC", "NB", "NBAdaptive"}, new String[]{
                    "Majority class",
                    "Naive Bayes",
                    "Naive Bayes Adaptive"}, 2);
            learner.gracePeriodOption = new IntOption(
                    "gracePeriod",
                    'g',
                    "The number of instances a leaf should observe between split attempts.",
                    5, 0, Integer.MAX_VALUE);

            ArffFileStream stream = new ArffFileStream();
            stream.arffFileOption = new FileOption("data/air/airlines.arff", 'f',
                    "ARFF file to load.", "data/air/airlines.arff", "arff", false);
            stream.prepareForUse();
            NUM_INSTANCES = 539383;

            this.accuracyFile = new FileWriter("air.csv", true);
            this.accuracyBufferedWriter = new BufferedWriter(this.accuracyFile);
            this.accuracyFileWriter = new PrintWriter(this.accuracyBufferedWriter);

            // Prepare experiment.
            super.prepareExperiment(stream, learner);
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentAllSynthetic extends Experiment {
        private ConceptDetectionTree learner;
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;

        private ExperimentAllSynthetic() throws IOException {
            // Create classifier and stream generator.
            this.learner = new ConceptDetectionTree(false);
            learner.leafpredictionOption = new MultiChoiceOption(
                    "leafprediction", 'l', "Leaf prediction to use.", new String[]{
                    "MC", "NB", "NBAdaptive"}, new String[]{
                    "Majority class",
                    "Naive Bayes",
                    "Naive Bayes Adaptive"}, 2);
            learner.gracePeriodOption = new IntOption(
                    "gracePeriod",
                    'g',
                    "The number of instances a leaf should observe between split attempts.",
                    200 , 0, Integer.MAX_VALUE);
        }

        public void runExperiment(long trainingInstancesCount, boolean isTesting) {
            try {
                super.runExperiment(trainingInstancesCount, isTesting);
            } catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }

        private void initStreams(LinkedList<InstanceStream> streams) {
            // Prepare and run Hyp_1 experiment.
            generators.HyperplaneGenerator hyp1 = new generators.HyperplaneGenerator();
            hyp1.prepareForUse();
            streams.add(hyp1);

            generators.HyperplaneGenerator hyp10 = new generators.HyperplaneGenerator();
            hyp10.balanceRatioOption = new IntOption(
                    "balanceRatio", 'r',
                    "Ratio between the positive and negative class as 1:r, where r is the value of this parameter.", 10, 1, Integer.MAX_VALUE);
            hyp10.balanceClassesOption = new moa.options.FlagOption("balanceClasses",
                    'b', "Balance the number of instances of each class.");
            hyp10.prepareForUse();

            streams.add(hyp10);

            generators.HyperplaneGenerator hyp100 = new generators.HyperplaneGenerator();
            hyp100.balanceRatioOption = new IntOption(
                    "balanceRatio", 'r',
                    "Ratio between the positive and negative class as 1:r, where r is the value of this parameter.", 100, 1, Integer.MAX_VALUE);
            hyp100.balanceClassesOption = new moa.options.FlagOption("balanceClasses",
                    'b', "Balance the number of instances of each class.");
            hyp100.prepareForUse();

            streams.add(hyp100);
        }

        private void runAllExperiments() throws IOException {
            LinkedList<InstanceStream> streams = new LinkedList<>();
            initStreams(streams);

            int i = 1;
            for (InstanceStream stream : streams) {
                System.out.println("Running " + i + " experiment");

                super.prepareExperiment(stream, learner);

                this.accuracyFile = new FileWriter("exp" + i++ + ".csv", true);
                this.accuracyBufferedWriter = new BufferedWriter(this.accuracyFile);
                this.accuracyFileWriter = new PrintWriter(this.accuracyBufferedWriter);

                runExperiment(NUM_INSTANCES, IS_TESTING);
            }
        }
    }
}
