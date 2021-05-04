package at.aau.moose;

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

    /**
     * Return true if x and y BOTH have > 0 values
     * @return Boolean
     */
    public boolean hasCoord() {
        return (x > 0) && (y > 0);
    }

    /**
     * Get the string representation
     * @return String
     */
    public String toString() {
        return "(" + x + " , " + y + ")";
    }
}
