import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import moa.classifiers.Classifier;
import moa.classifiers.trees.HoeffdingAdaptiveTree;
import moa.core.TimingUtils;
import moa.streams.generators.*;
import com.yahoo.labs.samoa.instances.Instance;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class App {
    private final static int NUM_INSTANCES = 1000000;
    private final static boolean IS_TESTING = true;

    public static void main(String[] args) {
        System.out.println("Running experiment");

        ExperimentConceptDetection exp = new ExperimentConceptDetection();
        exp.run(NUM_INSTANCES, IS_TESTING);
    }

    /*
    private static class Experiment {
        private FileWriter file;
        private BufferedWriter bw;
        private PrintWriter fileWriter;

        public Experiment() {
            try {
                this.file = new FileWriter("tree-training.json", true);
                this.bw = new BufferedWriter(file);
                this.fileWriter = new PrintWriter(this.bw);
                this.fileWriter.println("[");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(int numInstances, boolean isTesting) {
//            Classifier learner = new EnhancedHoeffdingTree(this.conceptFileWriter, numInstances/1000);
            Classifier learner = new HoeffdingAdaptiveTree();
            RandomRBFGenerator stream = new RandomRBFGenerator();
            stream.numClassesOption = new IntOption("numClasses", 'c',
                    "The number of classes to generate.", 3, 3, Integer.MAX_VALUE);
            stream.prepareForUse();

            learner.setModelContext(stream.getHeader());
            learner.prepareForUse();

            int numberSamplesCorrect = 0;
            int numberSamples = 0;
            boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
            long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

            while (stream.hasMoreInstances() && numberSamples < numInstances) {
                Instance trainInst = stream.nextInstance().getData();
                if (isTesting) {
                    if (learner.correctlyClassifies(trainInst)) {
                        numberSamplesCorrect++;
                    }
                }
                numberSamples++;
                if (numberSamples % 100000 == 0) {
                    System.out.println(numberSamples + " samples processed");
                }
                learner.trainOnInstance(trainInst);
            }

            this.fileWriter.println("]");
            this.fileWriter.close();
            double accuracy = 100.0 * (double) numberSamplesCorrect / (double) numberSamples;
            double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
            System.out.println(numberSamples + " instances processed with " + accuracy + "% accuracy in " + time + " seconds.");

            StringBuilder out = new StringBuilder();
            learner.getDescription(out, 4);
            System.out.println(out.toString());
        }
    }
    */

    private static class ExperimentConceptDetection {
        private FileWriter file;
        private BufferedWriter bw;
        private PrintWriter conceptFileWriter;

        public ExperimentConceptDetection() {
            try {
                // Concept file writer
                this.file = new FileWriter("tree-concepts.csv", true);
                this.bw = new BufferedWriter(file);
                this.conceptFileWriter = new PrintWriter(this.bw);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(int numInstances, boolean isTesting) {
            Classifier learner = new ConceptDetectionTree(this.conceptFileWriter);
            HyperplaneGenerator stream = new HyperplaneGenerator();
            stream.numClassesOption = new IntOption("numClasses", 'c',
                    "The number of classes to generate.", 4, 2, Integer.MAX_VALUE);
            stream.numDriftAttsOption = new IntOption("numDriftAtts", 'k',
                    "The number of attributes with drift.", 5, 0, Integer.MAX_VALUE);
            stream.magChangeOption = new FloatOption("magChange", 't',
                    "Magnitude of the change for every example", 0.10, 0.0, 1.0);
            stream.prepareForUse();

            learner.setModelContext(stream.getHeader());
            learner.prepareForUse();

            int numberSamplesCorrect = 0;
            int numberSamples = 0;
            TimingUtils.enablePreciseTiming();
            long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();

            this.conceptFileWriter.println("current node, alt tree, hoeffding bound, old tree error, alt tree error, diff, status");
            while (stream.hasMoreInstances() && numberSamples < numInstances) {
                Instance trainInst = stream.nextInstance().getData();

                if (isTesting) {
                    if (learner.correctlyClassifies(trainInst)) {
                        numberSamplesCorrect++;
                    }
                }

                // Debug output
                if (++numberSamples % 100000 == 0) {
                    System.out.println(numberSamples + " samples processed. Correct samples: " + numberSamplesCorrect);
                }

                learner.trainOnInstance(trainInst);
            }
            // Close files
            this.conceptFileWriter.close();

            // Print accuracy and processing time
            double accuracy = 100.0 * (double) numberSamplesCorrect / (double) numberSamples;
            double time = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - evaluateStartTime);
            System.out.println(numberSamples + " instances processed with " + accuracy + "% accuracy in " + time + " seconds.");
        }
    }
}
