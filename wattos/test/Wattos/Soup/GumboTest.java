/*
 * GumboTest.java
 * JUnit based test
 *
 * Created on November 30, 2005, 9:49 AM
 */

package Wattos.Soup;

import junit.framework.*;
import Wattos.Common.*;
import Wattos.Database.*;
import Wattos.Utils.*;

/**
 *
 * @author jurgen
 */
public class GumboTest extends TestCase {
    
    public GumboTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(GumboTest.class);
        
        return suite;
    }


    public void testRenumberRows() {
        boolean doAtoms = true;
        DBMS dbms = new DBMS();        
        Gumbo instance = new Gumbo(dbms);        
        boolean expResult = true;
        boolean result = instance.renumberRows(doAtoms);
        assertEquals(expResult, result);
    }
/**
    public void testCheck() {
        boolean makeCorrections = true;
        Gumbo instance = null;
        
        boolean expResult = true;
        boolean result = instance.check(makeCorrections);
        assertEquals(expResult, result);
    }

    public void testToSTAR() {
        Gumbo instance = null;
        
        String expResult = "";
        String result = instance.toSTAR();
        assertEquals(expResult, result);
    }

 */
    public void testMain() {
//        String[] args = null;
        DBMS dbms = new DBMS();
        Gumbo gumbo = new Gumbo(dbms);

        //General.verbosity = General.verbosityDebug;
        General.showDebug( dbms.toString(true));
        
        // Test the atoms
        float[] c1 = { 0.1234567890123456789f, 0.2f, 0.3f };
        gumbo.atom.add("atomName1", true, c1, Defs.NULL_FLOAT);
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME];
        Relation atomMain = gumbo.atom.getRelation( relationName );
        if ( atomMain == null ) {
            fail("No relation with name: " + relationName);                
        }

        // Test adding an orf.
        OrfIdList orfIdList = new OrfIdList();
        OrfId orfId = new OrfId();
        orfId.orf_db_name = "pdb";
        orfId.orf_db_id   = "1brv";
        orfIdList.orfIdList.add( orfId );
        if ( gumbo.entry.add(orfId.orf_db_id,orfIdList,null) < 0 ) {
            fail("Test of adding an orf to entry in gumbo.");
        } else {
            General.showDebug(gumbo.entry.toString());
        }

        
        //Test the graph
        if ( true ) {
            if ( ! dbms.graphIsOkay()) {
                fail("DBMS graph test; graph is not ok : ");
            }
            General.showDebug("Graph: " + dbms.graph.toString());
            //dbms.foreignKeyConstrSet.checkConsistencySet(true,true));
        }
    }    
}
