public class Vector {

    public double[] values;

    public Vector(double[] values) {
        // values-array isn't copied, so caller needs to copy it himself if necessary
        this.values = values;
    }

}
