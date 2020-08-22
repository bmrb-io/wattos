package Wattos.Soup.Constraint;

import Wattos.Utils.General;
import Wattos.Utils.Wiskunde.Geometry;
import junit.framework.TestCase;

public class CdihTest extends TestCase {

    /** Like to think in degrees */
    public void testToXplorSet() {
        General.setVerbosityToDebug();
        float[] testValues = new float[] { 345, 5 };
        float[] xplorSet = Cdih.toXplorSet(testValues[0], testValues[1]);
        float targetFound = xplorSet[0];
        float rangeFound = xplorSet[1];
        General.showDebug("targetFound " + targetFound + " degrees");
        General.showDebug("rangeFound " + rangeFound + " degrees");
        float targetExpected = 355.0000001f;
        assertEquals(targetExpected, targetFound, Geometry.ANGLE_EPSILON);
    }
}
