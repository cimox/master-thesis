package experiments;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.core.TimingUtils;
import moa.streams.InstanceStream;
import weka.classifiers.evaluation.output.prediction.Null;

import java.io.PrintWriter;

/**
 * This is a base class for running Experiments.
 */
public class Experiment {
    private InstanceStream stream;
    private Classifier learner;
    protected PrintWriter conceptFileWriter, accuracyFileWriter;

    protected Experiment() {
        TimingUtils.enablePreciseTiming();
    }

    private long printProgressTraining(long instancesSeen, long trainingInstanceCount) {
        if (++instancesSeen % Math.floor(trainingInstanceCount / 100) == 0) {
            System.out.println("[INFO] " + instancesSeen + " samples processed");
        }
        return instancesSeen;
    }

    private long testInstance(Instance trainInstance, long instancesCorrect, boolean isTesting) {
        if (isTesting && learner.correctlyClassifies(trainInstance)) {
            ++instancesCorrect;
        }
        return instancesCorrect;
    }

    public void runExperiment(long trainingInstancesCount, boolean isTesting) throws ExperimentException {
        if (this.learner == null || this.stream.hasMoreInstances()) {
            try {
                this.stream.nextInstance().getData();
            } catch (NullPointerException e) {
                throw new ExperimentException("[ERROR] Learner or stream is null!");
            }
        }

        long instancesCorrect = 0, instancesSeen = 0;
        long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        this.learner.setModelContext(this.stream.getHeader());
        this.learner.prepareForUse();

        this.accuracyFileWriter.write("instance,accuracy\n");

        while (this.stream.hasMoreInstances() && instancesSeen < trainingInstancesCount) {
            Instance trainInstance = this.stream.nextInstance().getData();
            instancesCorrect = testInstance(trainInstance, instancesCorrect, isTesting);
            instancesSeen = printProgressTraining(instancesSeen, trainingInstancesCount);
            this.learner.trainOnInstance(trainInstance);

            // Print prequential accuracy
            this.accuracyFileWriter.write(instancesSeen + ","
                    + 100.0 * (double) (instancesCorrect-1) / (double) instancesSeen + "\n"
            );
        }
        double accuracy = 100.0 * (double) instancesCorrect / (double) instancesSeen;
        double totalTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
        System.out.println(
                instancesSeen + " instances processed with " + accuracy + "% accuracy in " + totalTime + " seconds."
        );

        StringBuilder sb = new StringBuilder();
        this.learner.getDescription(sb, 2);
        System.out.println(sb.toString());

        this.accuracyFileWriter.close();
    }

    public void prepareExperiment(InstanceStream stream, Classifier learner) {
        this.learner = learner;
        this.stream = stream;
    }
}