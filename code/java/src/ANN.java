import java.util.Random;

public class ANN {

    // Implements a 2-layer ANN

    public static final double initSigma = 0.02;
    public static final double relStopMargin = 0.01;
    public static int maxIterations = 1000000;
    public static double stepSize = 0.1;
    public Matrix hiddenWeights, outputWeights; // outputWeights are stored as a row matrix

    public static void main(String[] args) {
        ANN.test();
    }

    public ANN(int inputSize, int hiddenSize) {
        this.hiddenWeights = Matrix.createRandNorm(inputSize, hiddenSize, 0, ANN.initSigma);
        this.outputWeights = Matrix.createRandNorm(hiddenSize, 1, 0, ANN.initSigma);
    }

    // Helper methods

    public static double activation(double value) {
        return Math.tanh(value);
    }

    public static Vector activation(Vector vector) {
        // Applies the activation function to an entire vector in-place
        for(int i = 0; i < vector.height; i++) {
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

    public void train(Example[] examples) {

        // Trains this ANN on the given array of examples
        // input vectors will always be normalized in this call

        long start = System.nanoTime();

        // Input normalization
        for(Example example : examples) {
            example.input.normalize();
        }

        // Pass over all examples until weights have converged enough
        int iteration = 0;
        while(true) {

            // Store old weights to check for convergence later
            Matrix prevHiddenWeights = this.hiddenWeights.deepcopy();
            Matrix prevOutputWeights = this.outputWeights.deepcopy();

            // Go through all examples
            for(Example example : examples) {

                // Calculate outputs
                Vector hiddenInput = this.hiddenWeights.multiply(example.input);
                Vector hiddenOutput = ANN.activation(hiddenInput.deepcopy());
                double finalInput = this.outputWeights.multiply(hiddenOutput).values[0];
                double predictedOutput = ANN.activation(finalInput);

                // Used in calculations
                double predictionError = example.output - predictedOutput;
                double outputDerivative = ANN.dactivation(finalInput);

                // Adjust output weights and calculate requested hidden change
                Matrix requestedHiddenChange = this.outputWeights.deepcopy().multiply(predictionError*outputDerivative); // Row matrix, like output weights
                for(int i = 0; i < this.outputWeights.width; i++)
                    this.outputWeights.values[i][0] += ANN.stepSize*predictionError*hiddenOutput.values[i];

                // Backpropagate requested hidden change to adjust hidden weights
                for(int i = 0; i < this.hiddenWeights.width; i++) {
                    for(int j = 0; j < this.hiddenWeights.height; j++) {
                        this.hiddenWeights.values[i][j] += ANN.stepSize*requestedHiddenChange.values[j][0]*ANN.dactivation(hiddenInput.values[j])*example.input.values[i];
                    }
                }

                // Check stop criteria
                iteration++;
                if (iteration >= ANN.maxIterations)
                    break;

            }

            // Check stop criteria
            if (iteration >= ANN.maxIterations)
                break;
            // Convergence check
            double diff = ANN.frobNorm(prevHiddenWeights.multiply(-1).add(this.hiddenWeights), prevOutputWeights.multiply(-1).add(this.outputWeights));
            double base = ANN.frobNorm(this.hiddenWeights, this.outputWeights);
            //if (base >= CustomMath.epsilon && diff/base < ANN.relStopMargin)
            //    break;

        }

        System.out.println((System.nanoTime() - start)/1000000000.0);
        System.out.println("Stopped training after " + iteration + " iterations.");

    }

    // Testing methods

    public static void test() {

        // Test the ANN by having it model another ANN with identical topology but unknown weights

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
        model.train(trainingSet);
        ANN.printRMSEs(model, trainingSet, validationSet);

    }

    public static Example[] generateExamples(int amount, int inputSize, ANN real) {

        // Generates examples based on an ANN
        Example[] examples = new Example[amount];
        Random rand = new Random();
        for(int i = 0; i < amount; i++) {
            double[] inputValues = new double[inputSize];
            for(int j = 0; j < inputSize; j++)
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
        for(Example example : examples) {
            total += Math.pow(example.output - ann.predict(example.input), 2);
        }
        return Math.sqrt(total/examples.length);

    }

}
