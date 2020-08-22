/*
 * PrimitiveArrayTest.java
 * JUnit based test
 *
 * Created on January 19, 2006, 3:08 PM
 */

package Wattos.Utils;

import junit.framework.*;
import java.util.*;

import cern.colt.list.IntArrayList;

/**
 *
 * @author jurgen
 */
public class PrimitiveArrayTest extends TestCase {
    
    public PrimitiveArrayTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(PrimitiveArrayTest.class);
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityNothing;
        
        return suite;
    }

    public void testConvertBit2String() {
        BitSet columnIn = new BitSet();
        columnIn.set(1);
        columnIn.set(3);
        int dataType = 0; // dummy
        String format = "%c";
        
        Object expResult = "[F;T;F;T;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F;F]";
        String[] result = (String[]) PrimitiveArray.convertBit2String(columnIn, dataType, format);
        String resultOneString = Strings.toString(result);
        //General.showOutput("result: " + resultOneString);
        assertEquals(expResult, resultOneString);
    }

    public void testToStringMakingCutoff1() {
//        General.verbosity = General.verbosityDebug;
        float[] valueList = new float[] { .2f,    0f, .8f, 0f, 0f, .6f, 0f, .12f, .11f, 0f };
        float cutoff = .1f;
        boolean smallerThanCutoff = false;
        General.showDebug("toStringMakingCutoff: [" + PrimitiveArray.toStringMakingCutoff( 
                valueList,cutoff,smallerThanCutoff) +"]");
    } 

    public void testToStringMakingCutoff2() {
        General.verbosity = General.verbosityDebug;
        ArrayList al = new ArrayList();
        al.add( new float[] { .06f,   0f, .3f, 0f, 0f, .9f, 0f, .21f, .05f, 0f });
        al.add( new float[] { .2f,    0f, .8f, 0f, 0f, .6f, 0f, .12f, .11f, 0f });
        float cutoff = .1f;
        boolean smallerThanCutoff = false;
        General.showDebug("toStringMakingCutoff: [" + PrimitiveArray.toStringMakingCutoff( 
                al,cutoff,smallerThanCutoff) +"]");
    } 

    public void testToString( ) {
        IntArrayList in = new IntArrayList();
        boolean printEOLAfterEach = true;
        boolean useBrackets = false;
        in.add(1);
        in.add(2);
        String txt = PrimitiveArray.toString(in,useBrackets,printEOLAfterEach);
        General.showDebug("IntArrayList represented by: [" + txt + "]");
        assertEquals("1\n2", txt);                        
    }
    
    public void testLearningJavaAgain( ) {
        float[] fList = new float[2];
        General.showDebug("fList length: [" + fList.length + "]");
        float[][] fLoL = new float[2][3];
        General.showDebug("fLoL length: [" + fLoL.length + "]");
        General.showDebug("fLoL[0] length: [" + fLoL[0].length + "]");
    }
    
    /**
    public void testConvertInt2String() {
        Object columnIn = null;
        int dataType = 0;
        String format = "";
        
        Object expResult = null;
        Object result = PrimitiveArray.convertInt2String(columnIn, dataType, format);
        assertEquals(expResult, result);
    }

    public void testConvertString2String() {
        Object columnIn = null;
        int dataType = 0;
        String format = "";
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2String(columnIn, dataType, format);
        assertEquals(expResult, result);
    }

    public void testConvertString2Float() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2Float(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2Double() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2Double(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2Bit() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2Bit(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2Int() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2Int(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2ArrayOfInt() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2ArrayOfInt(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2ArrayOfFloat() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2ArrayOfFloat(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2ArrayOfString() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2ArrayOfString(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2Short() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2Short(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testConvertString2StringNR() {
        Object columnIn = null;
        int dataType = 0;
        
        Object expResult = null;
        Object result = PrimitiveArray.convertString2StringNR(columnIn, dataType);
        assertEquals(expResult, result);
    }

    public void testSortTogether() {
        Object[] lists = null;
        
        boolean expResult = true;
        boolean result = PrimitiveArray.sortTogether(lists);
        assertEquals(expResult, result);
    }

    public void testInvertFromToMap() {
        int[] map = null;
        BitSet todo = null;
        
        int[] expResult = null;
        int[] result = PrimitiveArray.invertFromToMap(map, todo);
        assertEquals(expResult, result);
    }

    public void testGetRidsByValue() {
        int[] list = null;
        int value = 0;
        
        BitSet expResult = null;
        BitSet result = PrimitiveArray.getRidsByValue(list, value);
        assertEquals(expResult, result);
    }

    public void testSetValueByRids() {
        FloatArrayList in = null;
        BitSet rids = null;
        float value = 0.0F;
        
        boolean expResult = true;
        boolean result = PrimitiveArray.setValueByRids(in, rids, value);
        assertEquals(expResult, result);
    }

    public void testSetValueByArray() {
        int[] in = null;
        BitSet rids = null;
        
        boolean expResult = true;
        boolean result = PrimitiveArray.setValueByArray(in, rids);
        assertEquals(expResult, result);
    }

    public void testHashCode() {
        int[] in = null;
        
        int expResult = 0;
        int result = PrimitiveArray.hashCode(in);
        assertEquals(expResult, result);
    }

    public void testStripDashedMembers() {
        String[] in = null;
        
        String[] expResult = null;
        String[] result = PrimitiveArray.stripDashedMembers(in);
        assertEquals(expResult, result);
    }


     */    
    
}
