/*
 * TagTableTest.java
 * JUnit based test
 *
 * Created on January 31, 2006, 9:52 AM
 */

package Wattos.Star;

import junit.framework.*;
import Wattos.Database.*;
import Wattos.Utils.*;

/**
 *
 * @author jurgen
 */
public class TagTableTest extends TestCase {
    
    public TagTableTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(TagTableTest.class);
        
        return suite;
    }


    /**
     * Test of toSTAR method, of class Wattos.Star.TagTable.
     */
    public void testToSTAR() throws Exception {
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;
        General.showDebug("toSTAR");
        
        DBMS dbms = new DBMS();
        TagTable instance = new TagTable("testTagTable",dbms);
        instance.insertColumn("test_columnFloat",Relation.DATA_TYPE_FLOAT,null);
        instance.insertColumn("test_columnStringNR");
        instance.isFree = false;
        instance.useDefaultOrdering = true;
        
        instance.getNewRowIdList(3);
        //General.showDebug("Got new rows: " + PrimitiveArray.toString(rowIdList));
        //General.showDebug("Got used rows: " + PrimitiveArray.toString(instance.used,true));
//        String expResult = "";
        String result = instance.toSTAR();
        General.showDebug("table in STAR:"+General.eol+result);
        //assertEquals(expResult, result);
    }
}
