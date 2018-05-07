import java.util.Random;

public class Matrix {

    public double[][] values;

    public Matrix(double[][] values) {
        // values-array isn't copied, so caller needs to copy it himself if necessary
        this.values = values;
    }

    public static Matrix createRandNorm(int width, int height, double mu, double sigma) {
        // Creates a matrix with given size with values taken from a normal distribution with the given parameters
        Random rand = new Random();
        double[][] values = new double[width][height];
        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                values[x][y] = mu + rand.nextGaussian()*sigma;
            }
        }
        return new Matrix(values);
    }

    public Vector multiply(Vector vector) {
        // Right-multiplies the matrix with a column vector
        // Vector's size must be equal to this matrix's width

        return null;
    }

}
