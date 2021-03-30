package at.aau.moose;

/***
 * Point class but with fload coordinates
 */
public class Foint {
    public float x, y;

    /**
     * Return true if x and y BOTH have > 0 values
     * @return Boolean
     */
    public boolean hasCoord() {
        return (x > 0) && (y > 0);
    }

    public String toString() {
        return "(" + x + " , " + y + ")";
    }
}
