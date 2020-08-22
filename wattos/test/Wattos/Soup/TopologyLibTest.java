/*
 * TopologyLibTest.java
 * JUnit based test
 *
 * Created on November 22, 2005, 4:22 PM
 */

package Wattos.Soup;

import java.net.URL;
import java.util.BitSet;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import Wattos.Utils.General;

/**
 *
 * @author jurgen
 */
public class TopologyLibTest extends TestCase {
    
    public TopologyLibTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TopologyLibTest.class);        
        return suite;
    }

    public void testReadWIFFile() {
        URL url = null;
        TopologyLib instance = new TopologyLib();        
        boolean expResult = true;
        boolean result = instance.readWIFFile(url,null);
        assertEquals(expResult, result);
    }

    public void testMain() {        
        General.verbosity = General.verbosityDebug;
        
        TopologyLib topologyLib = new TopologyLib();
        if ( ! topologyLib.readWIFFile(null,null)) {
            fail("Reading topologyLib");            
            return;
        }
        General.showDebug("Done reading topologyLib");
        float bondSDAllowed  = 20f;
        float angleSDAllowed = 50f;
        // Retain only the outliers by 1 sigma.
        BitSet bondOutliers         = topologyLib.gumbo.bond.getOutliers(         topologyLib.gumbo.bond.used,  bondSDAllowed );
        BitSet angleOutliers        = topologyLib.gumbo.angle.getOutliers(        topologyLib.gumbo.angle.used, angleSDAllowed );
 
//        String result = topologyLib.dbms.toString();
        String result = topologyLib.gumbo.atom.toString(topologyLib.gumbo.atom.used);
        if ( result == null ) {
            fail("Converting topology lib to string");
            return;
        }
        General.showDebug("Topology lib:\n" + result);
        
        General.showDebug("Bond outliers by >"+bondSDAllowed+ " sigma");        
        result = topologyLib.gumbo.bond.toString(bondOutliers);
        if ( result == null ) {
            fail("Converting bonds to string");
            return;
        }
        General.showDebug( result);
        
        General.showDebug("Angle outliers by >"+angleSDAllowed+ " sigma");
        result = topologyLib.gumbo.angle.toString(angleOutliers);
        if ( result == null ) {
            fail("Converting angles to string");
            return;
        }
        General.showDebug( result);
    }    
}
