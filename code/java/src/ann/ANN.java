package ann;

import math.Matrix;
import math.Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ANN {

    // Implements a 2-layer ANN

    public static final double initSigma = 0.02;
    public static final double relStopMargin = 0.01;
    public static int maxIterations = 1000000;
    public static double stepSize = 0.01;
    public Matrix hiddenWeights, outputWeights; // outputWeights are stored as a row matrix

    public static void main(String[] args) {
        ANN.test();
    }

    public ANN(int inputSize, int hiddenSize) {
        this.hiddenWeights = Matrix.createRandNorm(inputSize, hiddenSize, 0, ANN.initSigma);
        this.outputWeights = Matrix.createRandNorm(hiddenSize, 1, 0, ANN.initSigma);
    }

    // Saving/loading

    public void save(String path) {

        // Saves this ANN to the given path

        int inputSize = this.hiddenWeights.width;
        int hiddenSize = this.hiddenWeights.height;
        ByteBuffer buffer = ByteBuffer.allocate(2 * 4 + 8 * inputSize * hiddenSize + 8 * hiddenSize);
        buffer.putInt(inputSize);
        buffer.putInt(hiddenSize);

        // Write hidden weights
        for (int x = 0; x < inputSize; x++) {
            for (int y = 0; y < hiddenSize; y++) {
                buffer.putDouble(this.hiddenWeights.values[x][y]);
            }
        }

        // Write output weights
        for (int x = 0; x < hiddenSize; x++) {
            buffer.putDouble(this.outputWeights.values[x][0]);
        }

        try {
            Files.write(Paths.get(path), buffer.array());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static ANN load(String path) {

        // Loads an ANN from the given path

        ANN ann = null;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(path));
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
            buffer.put(bytes);
            buffer.rewind();
            int inputSize = buffer.getInt();
            int hiddenSize = buffer.getInt();
            ann = new ANN(inputSize, hiddenSize);

            // Read hidden weights
            for (int x = 0; x < inputSize; x++) {
                for (int y = 0; y < hiddenSize; y++) {
                    buffer.putDouble(ann.hiddenWeights.values[x][y]);
                }
            }

            // Read output weights
            for (int x = 0; x < hiddenSize; x++) {
                buffer.putDouble(ann.outputWeights.values[x][0]);
            }

        } catch (NoSuchFileException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ann;

    }

    // Helper methods

    public static double activation(double value) {
        return Math.tanh(value);
    }

    public static Vector activation(Vector vector) {
        // Applies the activation function to an entire vector in-place
        for (int i = 0; i < vector.height; i++) {
            vector.values[i] = ANN.activation(vector.values[i]);
        }
        return vector;
    }

    public static double dactivation(double value) {
        return 1 - Math.pow(Math.tanh(value), 2);
    }

    public static double frobNorm(Matrix a, Matrix b) {
        // Calculates the total Frobenius-norm of both matrices a and b
        return Math.sqrt(Math.pow(a.frobNorm(), 2) + Math.pow(b.frobNorm(), 2));
    }

    // ANN methods

    public double predict(Vector input) {

        // Predicts the output of this input vector
        // input is always normalized in this call

        Vector res = this.outputWeights.multiply(ANN.activation(this.hiddenWeights.multiply(input.normalize())));
        return ANN.activation(res.values[0]);

    }

    public void train(Example[] trainingSet, Example[] validationSet, String annBasePath, String annPerformancePath) {

        // Trains this ann.ANN on the given array of examples
        // input vectors will always be normalized in this call

        // if validationSet, annBasePath and annPerformancePath are non-zero, accuracy on training and validation set will be measured and saved every round along with the current state

        long start = System.nanoTime();

        // Input normalization
        for (Example example : trainingSet) {
            example.input.normalize();
        }

        if (validationSet != null) {
            // Clear performance measurements
            (new File(annPerformancePath)).delete();
        }

        // Pass over all examples until weights have converged enough
        int iteration = 0;
        int round = 0;
        double lastSave = 0;
        while (true) {

            Matrix prevHiddenWeights = null, prevOutputWeights = null;
            if (validationSet != null) {

                // Test and report performance

                double trainingRMSE = ANN.RMSE(this, trainingSet);
                double validationRMSE = ANN.RMSE(this, validationSet);
                double trainingAccuracy = ANN.accuracy(this, trainingSet);
                double validationAccuracy = ANN.accuracy(this, validationSet);

                // Save current ANN periodically
                if (System.nanoTime() / 1000000000.0 > lastSave + 1) {
                    System.out.println("Results after round " + round + ":");
                    System.out.println("Training set RMSE: " + trainingRMSE);
                    System.out.println("Validation set RMSE: " + validationRMSE);
                    System.out.println("Training set accuracy: " + trainingAccuracy);
                    System.out.println("Validation set accuracy: " + validationAccuracy);
                    save(annBasePath + round);
                    lastSave = System.nanoTime() / 1000000000.0;

                    System.out.println("Hidden weights");
                    for (int x = 0; x < this.hiddenWeights.width; x++) {
                        System.out.println((new Vector(this.hiddenWeights.values[x])).norm() + " " + Arrays.toString(this.hiddenWeights.values[x]));
                    }
                    System.out.println("Output weights");
                    System.out.println("[");
                    for (int x = 0; x < this.outputWeights.width; x++) {
                        System.out.print(this.outputWeights.values[x][0] + ", ");
                    }
                    System.out.println("]\n");
                }

                // Save accuracies

                // Calculate output
                String output = null;
                try {
                    // Read file
                    List<String> lines = Files.readAllLines(Paths.get(annPerformancePath));
                    output = lines.get(0) + ", " + trainingRMSE + "\n" + lines.get(1) + ", " + validationRMSE
                            + "\n" + lines.get(2) + ", " + trainingAccuracy + "\n" + lines.get(3) + ", " + validationAccuracy;
                } catch (NoSuchFileException e) {
                    output = "trainingRMSE = " + trainingRMSE + "\nvalidationRMSE = " + validationRMSE
                            + "\ntrainingAccuracy = " + trainingAccuracy + "\nvalidationAccuracy = " + validationAccuracy;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                // Save output
                BufferedWriter writer = null;
                try {
                    writer = new BufferedWriter(new FileWriter(new File(annPerformancePath + ".tmp")));
                    writer.write(output);
                    (new File(annPerformancePath + ".tmp")).renameTo(new File(annPerformancePath));
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } else {
                // Store old weights to check for convergence later
                prevHiddenWeights = this.hiddenWeights.deepcopy();
                prevOutputWeights = this.outputWeights.deepcopy();
            }


            // Go through all examples
            for (Example example : trainingSet) {

                // Calculate outputs
                Vector hiddenInput = this.hiddenWeights.multiply(example.input);
                Vector hiddenOutput = ANN.activation(hiddenInput.deepcopy());
                double finalInput = this.outputWeights.multiply(hiddenOutput).values[0];
                double predictedOutput = ANN.activation(finalInput);

                // Used in calculations
                double predictionError = example.output - predictedOutput;
                double outputDerivative = ANN.dactivation(finalInput);

                // Adjust output weights and calculate requested hidden change
                Matrix requestedHiddenChange = this.outputWeights.deepcopy().multiply(predictionError * outputDerivative); // Row matrix, like output weights
                for (int i = 0; i < this.outputWeights.width; i++)
                    this.outputWeights.values[i][0] += ANN.stepSize * predictionError * hiddenOutput.values[i];

                // Back-propagate requested hidden change to adjust hidden weights
                for (int i = 0; i < this.hiddenWeights.width; i++) {
                    for (int j = 0; j < this.hiddenWeights.height; j++) {
                        this.hiddenWeights.values[i][j] += ANN.stepSize * requestedHiddenChange.values[j][0] * ANN.dactivation(hiddenInput.values[j]) * example.input.values[i];
                    }
                }

                if (validationSet == null) {
                    // Check stop criteria
                    iteration++;
                    if (iteration >= ANN.maxIterations)
                        break;
                }

            }

            if (validationSet == null) {
                // Check stop criteria
                if (iteration >= ANN.maxIterations)
                    break;
                // Convergence check
                double diff = ANN.frobNorm(prevHiddenWeights.multiply(-1).add(this.hiddenWeights), prevOutputWeights.multiply(-1).add(this.outputWeights));
                double base = ANN.frobNorm(this.hiddenWeights, this.outputWeights);
                if (base >= math.CustomMath.epsilon && diff / base < ann.ANN.relStopMargin)
                    break;
            }

            round++;

        }

        if (validationSet == null) {
            System.out.println((System.nanoTime() - start) / 1000000000.0);
            System.out.println("Stopped training after " + iteration + " iterations.");
        }

    }

    // Testing methods

    public static void test() {

        // Test the ann.ANN by having it model another ann.ANN with identical topology but unknown weights

        int inputSize = 5;
        int hiddenSize = 3;
        ANN real = new ANN(inputSize, hiddenSize);
        ANN model = new ANN(inputSize, hiddenSize);

        // Generate training data
        int amount = 10000;
        Example[] trainingSet = ANN.generateExamples(amount, inputSize, real);
        Example[] validationSet = ANN.generateExamples(amount, inputSize, real);

        // Print initial analysis, then train, then print new analysis
        ANN.printRMSEs(model, trainingSet, validationSet);
        model.train(trainingSet, null, null, null);
        ANN.printRMSEs(model, trainingSet, validationSet);

    }

    public static Example[] generateExamples(int amount, int inputSize, ANN real) {

        // Generates examples based on an ann.ANN
        Example[] examples = new Example[amount];
        Random rand = new Random();
        for (int i = 0; i < amount; i++) {
            double[] inputValues = new double[inputSize];
            for (int j = 0; j < inputSize; j++)
                inputValues[j] = rand.nextGaussian();
            Vector input = new Vector(inputValues);
            examples[i] = new Example(input, real.predict(input));
        }
        return examples;

    }

    public static void printRMSEs(ANN model, Example[] trainingSet, Example[] validationSet) {

        // Calculates and prints the RMSE's of this model on the training and validation set
        System.out.println("RMSE on training data: " + ANN.RMSE(model, trainingSet));
        System.out.println("RMSE on validation data: " + ANN.RMSE(model, validationSet));

    }

    public static double RMSE(ANN ann, Example[] examples) {

        // Calculates the RMSE of this ann on this set of examples
        double total = 0;
        for (Example example : examples) {
            total += Math.pow(example.output - ann.predict(example.input), 2);
        }
        return Math.sqrt(total / examples.length);

    }

    public static double accuracy(ANN ann, Example[] examples) {

        // Checks if ANN predicts right sign
        double total = 0;
        for (Example example : examples) {
            if (ann.predict(example.input) * example.output > 0) {
                total++;
            }
        }
        return total / examples.length;

    }

}
