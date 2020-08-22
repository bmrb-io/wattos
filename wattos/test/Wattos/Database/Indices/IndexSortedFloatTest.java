/*
 * IndexSortedFloatTest.java
 * JUnit based test
 *
 * Created on September 26, 2006, 11:47 AM
 */

package Wattos.Database.Indices;

import junit.framework.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 *
 * @author jurgen
 */
public class IndexSortedFloatTest extends TestCase {
    
    public IndexSortedFloatTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(IndexSortedFloatTest.class);
        
        return suite;
    }


    /**
     * Test of getRidListForQuery method, of class Wattos.Database.Indices.IndexSortedFloat.
     */
    public void testGetRidListForQuery() {
        General.setVerbosityToDebug();
        General.showDebug("getRidListForQuery");
        
        DBMS dbms = new DBMS();
        Relation t = null;
        try {
            t = new Relation("Relation T", 99, dbms);
        } catch ( Exception e ) {fail(e.getMessage());}
            
        t.insertColumn(0,"test",Relation.DATA_TYPE_FLOAT, null);

        
        int[] idx = t.getNewRowIdList(7);
        t.setValue(idx[0],"test",0f);
        t.setValue(idx[1],"test",0f);
        t.setValue(idx[2],"test",1f);
        t.setValue(idx[3],"test",2f);
        t.setValue(idx[4],"test",2f);
        t.setValue(idx[5],"test",3f);
        t.setValue(idx[6],"test",4f);
        
        int expResult = 4;
        int operationType = SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL;
        Float value = new Float( 1.99f );
        BitSet result = SQLSelect.selectBitSet(dbms,t,"test",operationType,value,false);
        General.showDebug("result A: " + PrimitiveArray.toString(result));
        assertEquals(expResult, result.cardinality());        

        t.setValue(idx[0],"test",0f);
        t.setValue(idx[1],"test",0f);
        t.setValue(idx[2],"test",1f);
        t.setValue(idx[3],"test",2f);
        t.setValue(idx[4],"test",2f);
        t.setValue(idx[5],"test",Defs.NULL_FLOAT);
        t.setValue(idx[6],"test",Defs.NULL_FLOAT);
        
        expResult = 2;
        BitSet result2 = SQLSelect.selectBitSet(dbms,t,"test",operationType,value,false);
        General.showDebug("result B: " + PrimitiveArray.toString(result2));
        assertEquals(expResult, result2.cardinality());            
    }    
}
