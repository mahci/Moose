package at.aau.moose;

import java.util.Objects;

/***
 * Point class but with fload coordinates
 */
public class Foint {
    public float x, y;

    /**
     * Constructor
     * @param inX X coord
     * @param inY Y coord
     */
    public Foint(float inX, float inY) {
        x = inX;
        y = inY;
    }

    /**
     * Empty constructor
     */
    public Foint() {

    }

    public double dist(Foint fp) {
        return Math.sqrt(Math.pow(x - fp.x, 2) + Math.pow(y - fp.y, 2));
    }

    /**
     * Return true if x and y BOTH have > 0 values
     * @return Boolean
     */
    public boolean hasCoord() {
        return (x > 0) && (y > 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Foint foint = (Foint) o;
        return Float.compare(foint.x, x) == 0 &&
                Float.compare(foint.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Get the string representation
     * @return String
     */
    public String toString() {
        return "(" + x + " , " + y + ")";
    }
}
