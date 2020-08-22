package Wattos.Utils.Wiskunde;

import junit.framework.TestCase;
import Wattos.Database.Defs;
import Wattos.Utils.General;

public class GeometryTest extends TestCase {

    /** Like to think in degrees */
    public void testAverageAngles() {
        General.setVerbosityToDebug();
        double[] testValues = new double[] { 178, -178 };
        double[] testValuesRad = new double[testValues.length];
        for (int i = 0; i < testValues.length; i++) {
            testValuesRad[i] = Math.toRadians(testValues[i]);
            // General.showDebug("Converted "+testValues[i]+" degrees to " + testValuesRad[i] + " rads");
        }
        double averageRad = Geometry.averageAngles(testValuesRad);
        // General.showDebug("Average angle is " + averageRad + " rads");
        double average = Math.toDegrees(averageRad);
        double expected = 180.000000000001;
        assertEquals(expected, average, Geometry.ANGLE_EPSILON);
    }

    public void testAverageAngles_2() {
        General.setVerbosityToDebug();
        double[] testValues = new double[] { 0, 0 };
        double[] testValuesRad = new double[testValues.length];
        for (int i = 0; i < testValues.length; i++) {
            testValuesRad[i] = Math.toRadians(testValues[i]);
            // General.showDebug("Converted "+testValues[i]+" degrees to " + testValuesRad[i] + " rads");
        }
        double averageRad = Geometry.averageAngles(testValuesRad);
        assertEquals(0d, averageRad, Geometry.ANGLE_EPSILON);
    }

    public void testAverageAngles_3() {
        General.setVerbosityToDebug();
//        WARNING: input was (deg): [-29.999999999999996;150.00000000000003]
        double[] testValuesRad = new double[] { -0.5235987755982988, 2.6179938779914944 };
        double averageRad = Geometry.averageAngles(testValuesRad);
        assertEquals(Defs.NULL_DOUBLE, averageRad, Geometry.ANGLE_EPSILON);
    }

    /** Like to think in degrees */
    public void ttttestDifferenceAngles() {
        double[][] testValues = new double[][] { { 5, 15, 10 }, { 345, 5, 20 }, { 5, 345, -20 }, { 180, 180, 0 },
                { 90, -70, -160 } };
        double[][] testValuesRad = new double[testValues.length][3];
        for (int i = 0; i < testValues.length; i++) {
            for (int j = 0; j < testValues[i].length; j++) {
                testValuesRad[i][j] = Math.toRadians(testValues[i][j]);
                // General.showDebug("Converted "+testValues[i][j]+" degrees to " + testValuesRad[i][j] + " rads");
            }
        }
        for (int i = 0; i < testValues.length; i++) {
            double diffRad = Geometry.differenceAngles(testValuesRad[i][0], testValuesRad[i][1]);
            double diff = Math.toDegrees(diffRad);
            // General.showDebug("Difference angle is " + diff + " degree(s)");
            assertEquals(testValues[i][2], diff, Math.toDegrees(Geometry.ANGLE_EPSILON));
        }
    }

    /**
     */
    public void testViolationAngles() {
        // lower bound
        // upper bound
        // angle
        // expected violation
        // id
        double[][] testValues = new double[][] { { 5, 6, 15, 9, 0 }, { 169, -172, 100, 69, 1 }, // low viol
                { 172, -169, 100, 72, 1 }, // low viol
                { -10, 20, 25, 5, 2 }, // upp viol
                { 10, 20, 6, 4, 3 }, { 180, 180, 180, 0, 4 }, { 0, 0, 0, 0, 5 }, { 140, 150, -70, 140, 6 }, // low viol
                { 90, 100, -70, 160, 7 }, // upp viol
                { 70, 100, -70, 140, 8 }, // low viol
                { 5, -5, 2, 3, 9 }, // pathological low viol
                { -169, 172, 180, 8, 10 }, // pathological upp viol triggered change in code.
                { -172, 169, 180, 8, 11 }, // pathological low viol triggered change in code.
                { -2, 5, 3, 0, 12 }, // simple setup first
                { -722, 725, 722, 0, 13 }, // same but subtracted/added 2pi
                { -722, 725, 728, 3, 14 }, // 
                { -722, 725, -726, 4, 15 }, // 
                { -200, -160, 180, 0, 16 }, // 
                { -120, 120, -35, 0, 17 }, // 
                { 0, 0, 180, 180, 18 }, // 
                { -0, 0, 180, 180, 19 }, // 
                { -0, -0, 180, 180, 20 }, // 
                { 0, -0, 180, 180, 21 }, // 
                { -120, 120, -35, 0, 22 }, // 
                { 23, 41, -19, 42, 22 }, // 
                { 23, 41, -24, 47, 22 }, // 
        };

        double[][] testValuesRad = new double[testValues.length][5];
        for (int i = 0; i < testValues.length; i++) {
            for (int j = 0; j < testValues[i].length; j++) {
                testValuesRad[i][j] = Math.toRadians(testValues[i][j]);
                // General.showDebug("Converted "+testValues[i][j]+" degrees to " + testValuesRad[i][j] + " rads");
            }
        }

        for (int i = 0; i < testValues.length; i++) {
            double violRad = Geometry.violationAngles(testValuesRad[i][0], testValuesRad[i][1], testValuesRad[i][2]);
            double viol = Math.toDegrees(violRad);
            General.showDebug("Viol angle [" + i + "] is " + viol + " degree(s)");
            assertEquals(testValues[i][3], viol, Geometry.ANGLE_EPSILON);
        }
    }

}
