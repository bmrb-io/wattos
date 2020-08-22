/*
 * StarDictionaryTest.java
 * JUnit based test
 *
 * Created on January 18, 2006, 3:22 PM
 */

package Wattos.Star.NMRStar;

import Wattos.Database.Relation;
import junit.framework.*;
import Wattos.Utils.*;

/**
 *
 * @author jurgen
 */
public class StarDictionaryTest extends TestCase {
    
    public StarDictionaryTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(StarDictionaryTest.class);        
        return suite;
    }

    public void testReadCsvFile() {
        String csv_file_location = null;
        General.verbosity = General.verbosityDebug;
        StarDictionary instance = new StarDictionary();                
        assertEquals(true, instance.readCsvFile(csv_file_location));
        
        String id = "_Entity_poly_seq.Entity_ID";
        String idCIF = id.toLowerCase();
        
        
        assertEquals(id   , instance.getTagName(   "res_main", "mol_id"));
        assertEquals(idCIF, instance.getTagNameCIF("res_main", "mol_id"));

        assertEquals(Relation.DATA_TYPE_INT, instance.getDataType(id   ,false)); // funny; they're different
        assertEquals(Relation.DATA_TYPE_INT, instance.getDataType(idCIF,true));                
        
        General.showDebug( idCIF + " in CIF gives Wattos: " +
                PrimitiveArray.toString( instance.fromCIF.get(idCIF) ));
//        General.showDebug( "instance: " + instance.toString());
    }    
}
