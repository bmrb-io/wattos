/*
 * HashOfHashesOfHashesTest.java
 * JUnit based test
 *
 * Created on December 12, 2005, 10:24 AM
 */

package Wattos.Utils;

import junit.framework.*;

/**
 *
 * @author jurgen
 */
public class HashOfHashesOfHashesTest extends TestCase {
    
    public HashOfHashesOfHashesTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(HashOfHashesOfHashesTest.class);
        
        return suite;
    }

    /**
    public void testPut() {
        Object key1 = null;
        Object key2 = null;
        Object key3 = null;
        Object value = null;
        HashOfHashesOfHashes instance = new HashOfHashesOfHashes();
        
        Object expResult = null;
        Object result = instance.put(key1, key2, key3, value);
        assertEquals(expResult, result);
    }

    public void testGet() {
        Object key1 = null;
        Object key2 = null;
        Object key3 = null;
        HashOfHashesOfHashes instance = new HashOfHashesOfHashes();
        
        Object expResult = null;
        Object result = instance.get(key1, key2, key3);
        assertEquals(expResult, result);
    }

    public void testGetHashOfHashes() {
        Object key1 = null;
        HashOfHashesOfHashes instance = new HashOfHashesOfHashes();
        
        HashOfHashes expResult = null;
        HashOfHashes result = instance.getHashOfHashes(key1);
        assertEquals(expResult, result);
    }

     */
    public void testToString() {
        //General.verbosity = General.verbosityDebug;
        HashOfHashesOfHashes instance = new HashOfHashesOfHashes();
        instance.put( "aapje", "olifant", "boompje", "glijbaan" );
        Object value = instance.get( "aapje", "olifant", "boompje" );
        General.showDebug("Value looks like: " + value.toString() );            
        General.showDebug("Map looks like:\n" + instance.toString() );
        String expResult = "Key 1: aapje Key 2: olifant Key 3: boompje Value: glijbaan\nNumber of key combinations in three-dimensional map is: 1";
        String result = instance.toString();
        if ( ! Strings.equalsIgnoreWhiteSpace(result, expResult)) {
            assertEquals(expResult, result);
        }
        
/**
        HashOfHashes m2 = instance.getHashOfHashes("aapje");
        result = m2.toString();
        General.showDebug("Map 2D on 'aapje' looks like:\n" + result );
        expResult = "Key 1: olifant Key 2: boompje Value: glijbaan\nNumber of values in two-dimensional map is: 1";
        assertEquals(expResult, result);        
 */
    }

/**
    public void testCardinality() {
        HashOfHashesOfHashes instance = new HashOfHashesOfHashes();
        
        int expResult = 0;
        int result = instance.cardinality();
        assertEquals(expResult, result);
    }
*/    
}
