package experiments;

import com.yahoo.labs.samoa.instances.Instance;
import moa.classifiers.Classifier;
import moa.core.TimingUtils;
import moa.streams.InstanceStream;

/**
 * This is a base class for running Experiments.
 */
public abstract class Experiment {
    private InstanceStream stream;
    private Classifier learner;

    public Experiment() {
        TimingUtils.enablePreciseTiming();
    }

    private void printProgressTraining(long instancesSeen, long trainingInstanceCount) {
        if (++instancesSeen % Math.floor(trainingInstanceCount / 100) == 0) {
            System.out.println("[INFO]" + instancesSeen + " samples processed");
        }
    }

    private void testInstance(Instance trainInstance, long instancesCorrect, boolean isTesting) {
        if (isTesting && learner.correctlyClassifies(trainInstance)) {
            ++instancesCorrect;
        }
    }

    public void runExperiment(long trainingInstancesCount, boolean isTesting) {
        long instancesCorrect = 0, instancesSeen = 0;
        long startTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
        this.learner.setModelContext(this.stream.getHeader());
        this.learner.prepareForUse();

        while (stream.hasMoreInstances() && instancesSeen < trainingInstancesCount) {
            Instance trainInstance = stream.nextInstance().getData();
            testInstance(trainInstance, instancesCorrect, isTesting);
            printProgressTraining(instancesSeen, trainingInstancesCount);
            learner.trainOnInstance(trainInstance);
        }
        double accuracy = 100.0 * (double) instancesCorrect / (double) instancesSeen;
        double totalTime = TimingUtils.nanoTimeToSeconds(TimingUtils.getNanoCPUTimeOfCurrentThread() - startTime);
        System.out.println(
                instancesSeen + " instances processed with " + accuracy + "% accuracy in " + totalTime + " seconds."
        );
    }
}