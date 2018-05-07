package math;

import java.util.Random;

public class Matrix {

    public double[][] values;
    public int width;
    public int height;

    public Matrix(double[][] values) {
        // values-array isn't copied, so caller needs to copy it himself if necessary
        this.values = values;
        this.width = values.length;
        this.height = values[0].length;
    }

    public Matrix deepcopy() {
        // Returns a deep copy of this matrix
        double[][] newValues = new double[width][height];
        for(int i = 0; i < width; i++) {
            for(int j = 0; j < height; j++) {
                newValues[i][j] = this.values[i][j];
            }
        }
        return new Matrix(newValues);
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

    public double frobNorm() {
        // Calculates the Frobenius-norm of this matrix
        double res = 0;
        for(int i = 0; i < this.width; i++) {
            for(int j = 0; j < this.height; j++) {
                res += Math.pow(this.values[i][j], 2);
            }
        }
        return Math.sqrt(res);
    }

    public Matrix add(Matrix other) {
        // Adds the given matrix to this matrix in-place
        // Matrices need to have equal size
        for(int i = 0; i < this.width; i++) {
            for(int j = 0; j < this.height; j++) {
                this.values[i][j] += other.values[i][j];
            }
        }
        return this;
    }

    public Matrix multiply(double scalar) {
        // Multiplies this matrix with a scalar in-place
        for(int i = 0; i < this.height; i++) {
            for(int j = 0; j < this.height; j++) {
                this.values[i][j] *= scalar;
            }
        }
        return this;
    }

    public Vector multiply(Vector vector) {

        // Right-multiplies the matrix with a column vector
        // math.Vector's size must be equal to this matrix's width
        // Returns a new vector

        double[] res = new double[this.height];
        for(int i = 0; i < this.height; i++) {
            for(int j = 0; j < this.width; j++) {
                res[i] +=  this.values[j][i]*vector.values[j];
            }
        }
        return new Vector(res);

    }

}
