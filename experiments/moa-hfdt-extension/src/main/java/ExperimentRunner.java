import com.github.javacliparser.FlagOption;
import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.github.javacliparser.MultiChoiceOption;
import experiments.Experiment;
import experiments.ExperimentException;
import moa.classifiers.Classifier;
import moa.streams.generators.*;
import trees.ConceptDetectionTree;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;


public class ExperimentRunner {
    private final static int NUM_INSTANCES = 150000;
    private final static boolean IS_TESTING = true;

    public static void main(String[] args) {
        ExperimentConceptDetection exp = new ExperimentConceptDetection();
        exp.runExperiment(NUM_INSTANCES, IS_TESTING);
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
            }
            catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    private static class ExperimentConceptDetection extends Experiment {
        private FileWriter conceptsFile, accuracyFile;
        private BufferedWriter conceptsBufferedWriter, accuracyBufferedWriter;
        private PrintWriter conceptFileWriter, accuracyFileWriter;

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
                        500, 0, Integer.MAX_VALUE);

                HyperplaneGenerator stream = new HyperplaneGenerator();
                stream.numClassesOption = new IntOption("numClasses", 'c',
                        "The number of classes to generate.", 4, 2, Integer.MAX_VALUE);
                stream.numDriftAttsOption = new IntOption("numDriftAtts", 'k',
                        "The number of attributes with drift.", 5, 0, Integer.MAX_VALUE);
                stream.magChangeOption = new FloatOption("magChange", 't',
                        "Magnitude of the change for every example", 0.10, 0.0, 1.0);
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
            }
            catch (ExperimentException e) {
                System.err.println(e.getMessage());
            }
        }
    }
}
