package math;

public class Vector {

    public double[] values;
    public int height;

    public Vector(double[] values) {
        // values-array isn't copied, so caller needs to copy it himself if necessary
        this.values = values;
        this.height = values.length;
    }

    public Vector deepcopy() {
        // Returns a deep copy of this vector
        double[] newValues = new double[height];
        for (int i = 0; i < height; i++) {
            newValues[i] = this.values[i];
        }
        return new Vector(newValues);
    }

    public double norm() {
        // Calculates the 2-norm of this vector (same as Frobenius-norm for vectors)
        double res = 0;
        for (int i = 0; i < this.height; i++) {
            res += Math.pow(this.values[i], 2);
        }
        return Math.sqrt(res);
    }

    public Vector add(Vector other) {
        // Adds the given vector to this vector in-place
        // Vectors need to have equal size
        for (int i = 0; i < this.height; i++) {
            this.values[i] += other.values[i];
        }
        return this;
    }

    public Vector multiply(double scalar) {
        // Multiplies this vector with a scalar in-place
        for (int i = 0; i < this.height; i++) {
            this.values[i] *= scalar;
        }
        return this;
    }

    public Vector normalize() {
        // Normalizes this vector in-place
        // If the vector has norm 0, there is no effect
        double length = norm();
        if (length >= CustomMath.epsilon)
            multiply(1 / length);
        return this;
    }

    public double dot(Vector other) {
        // Calculates the dot product of this vector and the given vector
        double res = 0;
        for (int i = 0; i < this.height; i++) {
            res += this.values[i] * other.values[i];
        }
        return res;
    }

}
