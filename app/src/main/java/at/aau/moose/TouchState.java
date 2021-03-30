package at.aau.moose;

import android.view.MotionEvent.PointerCoords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/***
 * The state of the touch on the screen
 */
public class TouchState {

    private List<PointerCoords> fingersPoCos = new ArrayList<>();
    private long time;

    /**
     * Contructor 1
     * @param pcs List of PointerCoords
     * @param t Time (in millisecs)
     */
    public TouchState(PointerCoords[] pcs, long t) {
        fingersPoCos = Arrays.asList(pcs);
        time = t;
    }

    /**
     * Contructor 2
     * @param pcs List of PointerCoords
     */
    public TouchState(PointerCoords[] pcs) {
        // Sort the fingers based on their positions and add to the list
        fingersPoCos = Arrays.asList(pcs);
    }

    /**
     * Constructor 3 (Empty)
     */
    public TouchState() {}

    /**
     * Add a finger pointer coords to the list and sort it again
     * @param poco
     */
    public void addFingerPoCo(PointerCoords poco) {
        fingersPoCos.add(poco);
        Collections.sort(fingersPoCos, (poco1, poco2) -> {
            return Math.round(poco1.x - poco2.x); // -: 1 < 2 | 0: 1 = 2 | +: 1 > 2
        });
    }

    /**
     * Set the time
     * @param t Time (in millisecs)
     */
    public void setTime(long t) {
        time = t;
    }

    /**
     * Clear all the coords
     */
    public void reset() {
        fingersPoCos.clear();
    }

    /**
     * Get the String of everything
     * @return String
     */
    public String toSring() {
        StringBuilder rs = new StringBuilder();
        for (PointerCoords poco: fingersPoCos) {
            rs.append(poco.x).append(", ");
        }
        rs.append(" - Time: ").append(time);

        return rs.toString();
    }

}
