/*
 * PrimitiveArray.java
 *
 * Created on November 13, 2002, 10:18 AM
 */

package Wattos.Utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import Wattos.Database.Defs;
import Wattos.Soup.Constraint.DistConstrList;
import Wattos.Utils.Comparators.ComparatorIntArray;
import Wattos.Utils.Wiskunde.Logic;
import cern.colt.list.BooleanArrayList;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.map.OpenIntIntHashMap;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * Many methods for fast handling of primitive arrays such as: int[]. Parsing/unparsing
 * from a String representation, converting to/from {@link java.util.Collections},
 * and <CODE>cern.colt</CODE> APIs. There are even some simple calculation methods
 *such as averaging floats and getting a MD5 check sum.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class PrimitiveArray {

    /** number of bytes for the md5 sum */
    public static final int MD5SumLength = 16;
    /** number of chars for the md5 sum */
    public static final int MD5SumStringLength = MD5SumLength * 2;


    public static final char CHARACTER_INDICATING_MAKING_CUTOFF = '*';

    public static Matcher countMakingCutoffMatcher = null;

    static {
        Pattern p = Pattern.compile("[\\*\\-\\+]");
        countMakingCutoffMatcher = p.matcher("");
    }



    /** Creates new PrimitiveArray */
    public PrimitiveArray( ) {
    }

    /** Fast method for rectangular matrix as input.
     *Input needs to be rectangular.
     */
    public static float[] toFloatArray(Object[] m) {
        FloatArrayList result = new FloatArrayList();
        for (int i=0;i<m.length;i++) {
            ArrayList falList = (ArrayList) m[i];
            for (int j=0;j<falList.size();j++) {
                float[] f = (float[]) falList.get(j);
                int idx = result.size();
                result.ensureCapacity(idx+f.length);
                float[] elements = result.elements();
                for (int k=0;k<f.length;k++) {
                    elements[idx] = f[k];
                    idx++;
                }
            }
        }
        return toFloatArray(result);
    }

    /** ArrayList of float[] gets converted fast.
     */
    public static float[] toFloatArray(ArrayList m) {
        if ( m == null ) {
            General.showError("Failed toFloatArray because input was null reference.");
            return null;
        }
        FloatArrayList result = new FloatArrayList();
        int idx = 0;
        for (int i=0;i<m.size();i++) {
            //General.showDebug("doing item i: " + i);
            float[] f = (float[]) m.get(i);
            //General.showDebug("result size (A): " + result.size());
            result.setSize(idx+f.length);
            //General.showDebug("result size (B): " + result.size());
            float[] elements = result.elements();
            for (int k=0;k<f.length;k++) {
                //General.showDebug("doing item k: " + k);
                elements[idx] = f[k];
                idx++;
            }
        }
        return toFloatArray(result);
    }


    /** Fast method for rectangular matrix as input.
     *Input needs to be rectangular.
     */
    public static float[] toFloatArray(float[][] m) {
        //FloatArrayList l = new FloatArrayList();
        float[] result = new float[m.length*m[0].length];
        int idx = 0;
        for (int i=0;i<m.length;i++) {
            System.arraycopy(m[i],0,
                             result,idx,
                             m[i].length);
            idx += m[i].length;
        }
        return result;
    }

    public static boolean equals( int[] l1, int[] l2 ) {
        IntArrayList ll1 = new IntArrayList(l1);
        IntArrayList ll2 = new IntArrayList(l2);

        return ll1.equals(ll2);
    }

    public static boolean containsNullObject( Object[] l ) {
        for (int i=0;i<l.length;i++) {
            if ( l[i]==null ) {
                return true;
            }
        }
        return false;
    }

    /** Returns the maximum number of columns of all rows.
     */
    public static int getMaxColumns( Object[][] o ) {
        int result = 0;
        for (int r=0;r<o.length; r++) {
            int columns = o[r].length;
            if ( columns > result ) {
                result = columns;
            }
        }
        return result;
    }

    /** Transforms the set to contain a maximum of max set bits. All the ones after
     *these will be nilled
     */
    public static void truncate ( BitSet b, int max ) {
        int count = 0;
        for (int i=b.nextSetBit(0);i>=0;i=b.nextSetBit(i+1)) {
            if (b.get(i)) {
                if ( count >= max ) {
                    b.clear(i);
                }
                count++;
            }
        }

    }
    public static boolean hasPluralCardinality( BitSet b ) {
        int next = b.nextSetBit(0);
        if ( next < 0 ) {
            return false;
        }
        next = b.nextSetBit(next+1);
        if ( next < 0 ) {
            return false;
        }
        return true;
    }

    /** Transfers the lines to content variable.
     */
    public static byte[] fillContent(ArrayList lines ) {
        /** Code could be optimized a bit by not materializing over and over...
         */
        if ( lines == null ) {
            General.showError("Can't fill content from lines because there aren't any (null ref)");
            return null;
        }
        if ( lines.size() < 1 ) {
            General.showError("Can't fill content from lines because there aren't any (zero lines)");
            return null;
        }
        //General.showDebug("In method PrimitiveArray.fillContent found number of lines: " + lines.size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        // output : Unicode to Cp850 (MS-DOS Latin-1)
        OutputStreamWriter out = null;
        try {

            out = new OutputStreamWriter(baos);
            //General.showDebug("Encoding: " + out.getEncoding());
            // web app failed here when encoding "Cp850" was given.
            Iterator i=lines.iterator();
//            int lineCount = 0;
            while (i.hasNext()) {
                String line = (String) i.next();
                //General.showDebug("line: " + ++lineCount + " has length: " + line.length());
                out.write( line );
                out.write( General.eol );
            }
            out.close(); // weird that this method is necessary. apparently baos doesn't get all data yet.
            return baos.toByteArray();
        } catch ( Exception e ) {
            General.showThrowable(e);
        }
        return null;
    }


    /** Creates an array with elements ascending in order starting from start */
    static public int[] createSequentialArray( int length, int start ) {
        if ( length < 1 ) {
            return null;
        }
        int[] result = new int[length];
        for (int i=length-1;i>=0;i--) {
            result[i] = start + i;
        }
        return result;
    }

    /** returns null on failure and a 16 char string on success
     */
    static public String getMD5SumString( byte[] input ) {
        byte[] result = getMD5Sum( input );
        if ( result == null ) {
            return null;
        }
        return Strings.toHex(result);
    }

    /** Generates a MD5 sum check using DigestInputStream.
     */
    static public byte[] getMD5Sum( byte[] input ) {
        byte[] result = null;
        long time = System.currentTimeMillis();
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance("MD5");
            ByteArrayInputStream bais = new ByteArrayInputStream(input);
            DigestInputStream dis = new DigestInputStream(bais,digester);
            // Now create a stream who's data can be read easily.
            DataInputStream dais = new DataInputStream(dis);
            byte[] buffer = new byte[ input.length ];
            dais.readFully(buffer);
            result = digester.digest();
            //General.showOutput("MD5 sum digest: " + Strings.toHex( result ));
            if ( result.length != MD5SumLength ) {
                General.showCodeBug("Didn't calculate a md5 sum of proper length.");
                General.showCodeBug("Expeced length: " + MD5SumLength);
                return null;
            }
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        time = System.currentTimeMillis() - time;
        //General.showOutput("Time for calculating MD5 Sum: " + time);
        return result;
    }

    /** Creates a map from value element i to i */
    static public OpenIntIntHashMap createHashMapByOrder( IntArrayList in ) {
        OpenIntIntHashMap m;
        if ( in == null ) { // garbage in garbage out
            return null;
        }
        if ( in.size() == 0 ) {
            return new OpenIntIntHashMap();
        }
        if ( in.size() > PrimitiveMap.MIN_SIZE_MAP ) {
            m = new OpenIntIntHashMap(in.size());
        } else {
            m = new OpenIntIntHashMap();
        }
        for (int i = in.size()-1; i>=0; i--) {
            //General.showDebug("filling OpenIntIntHashMap with key: " + in.getQuick(i) + " and value: " + i);
            m.put( in.getQuick(i), i);
        }
        return m;
    }


    /** Returns null if even one element isn't of type Integer */
    static public IntArrayList toIntArrayList( Collection c ) {
        IntArrayList result = new IntArrayList();
        result.setSize( c.size() );
        try {
            int index=0;
            for (Iterator i = c.iterator();i.hasNext();) {
                Integer value = (Integer) i.next();
                result.setQuick(index++, value.intValue());
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        return result;
    }


    static public boolean removeDuplicatesBySort( IntArrayList a ) {
        a.sort();
        for (int i=a.size(); i>0; i--) {
            if ( a.getQuick(i) == a.getQuick(i-1) ) {
                //General.showDebug("Removed duplicate " + a.get(i) + " at index: " + i );
                a.remove(i);
            }
        }
        return true;
    }

    /** Warning; does no checking of any kind */
    static public float getSum( float[] in ) {
        float sum = 0;
        for (int i=0; i<in.length; i++) {
            //General.showDebug("Using float: " + in[i]);
            sum += in[ i ];
        }
        return sum;
    }

    /** Warning; does no checking of any kind */
    static public int getSum( int[] in ) {
        int sum = 0;
        for (int i=0; i<in.length; i++) {
            //General.showDebug("Using float: " + in[i]);
            sum += in[ i ];
        }
        return sum;
    }

    static public float getAverage( float[] in ) {
        float sum = getSum( in );
        if ( Defs.isNull( sum ) ) {
            General.showError("Failed to get the sum for the floats: " + PrimitiveArray.toString( in ));
        }
        return sum/in.length;
    }

    /** Return index of the min value */
    static public int getMinLocation( float[] in ) {
        int loc = 0;
        float result = in[0];
        for (int i=1;i<in.length;i++) {
            if ( result > in[i] ){
                result = in[i];
                loc = i;
            }
        }
        return loc;
    }

    /** Return index of the max value */
    static public int getMaxLocation( float[] in ) {
        int loc = 0;
        float result = in[0];
        for (int i=1;i<in.length;i++) {
            if ( result < in[i] ){
                result = in[i];
                loc = i;
            }
        }
        return loc;
    }

    /** Return index of the max value below or above the cutoff or -1
     *if no such value exists. Returns -2 on error. If getMax is set to false
     *it will get the minimum value below or above the cutoff.
     *In case of an ArrayList argument for object in2 then the ArrayList is composed
     *of:
     *ArrayList -> float[] for every restraint -> float for every model
     */
    static public int getMaxLocationMakingCutoff( Object in2, float cutoff,
            boolean smallerThanCutoff, boolean getMax ) {
        float maxVal;
        float minVal;

        boolean isArrayList = false;
        if ( in2 instanceof ArrayList ) {
            isArrayList = true;
        }

        int loc = -1;
        float extreem = Defs.NULL_FLOAT;

        if ( ! isArrayList ) {
            float[] in = (float[]) in2;
            int countModels = in.length;
            if ( countModels == 0 ) {
                General.showError("Failed to getMaxLocationMakingCutoff as no models present.");
                return -2;
            }
            for (int i=0;i<countModels;i++) {
                if ( smallerThanCutoff ) {
                    //in[i] < cutoff
                    if ( in[i] < cutoff ) {
                        if ( Defs.isNull(extreem) ) {
                            extreem = in[i];
                            loc = i;
                        // ( in[i] > extreem )
                        } else if ( Logic.smallerThanOrLargerThan(getMax, extreem, in[i])) {
                            extreem = in[i];
                            loc = i;
                        }
                    }
                } else {
                    if ( in[i] > cutoff ) {
                        if ( Defs.isNull(extreem) ) {
                            extreem = in[i];
                            loc = i;
                        // ( in[i] > extreem )
                        } else if ( Logic.smallerThanOrLargerThan(getMax, extreem, in[i]) ) {
                            extreem = in[i];
                            loc = i;
                        }
                    }
                }
            }
        } else { // in case of ArrayList input
            ArrayList al = (ArrayList) in2;
            float[] valueList = (float[]) al.get(0);
            if ( (valueList == null) || (valueList.length==0)) {
                General.showError("Got invalid values in getMaxLocationMakingCutoff");
                return -2;
            }
            int countModels = valueList.length;
            if ( countModels == 0 ) {
                General.showError("Failed to getMaxLocationMakingCutoff as no models present.");
                return -2;
            }
            for (int i=0;i<countModels;i++) {   // loop over models
                minVal = Float.MAX_VALUE;
                maxVal = Float.MIN_VALUE;
                for (int j=0;j<al.size();j++) { // loop over restraints
                    valueList = (float[]) al.get(j);
                    if ( valueList.length != countModels ) {
                        General.showError("Inconsistent number of models from first constraint: " +
                                countModels + " and restraint: " + j + " where it is: " + valueList.length);
                        return -2;
                    }

                    if ( valueList[i] < minVal ) {
                        if ( Logic.smallerThanOrLargerThan(smallerThanCutoff, valueList[i], cutoff)) {
                            minVal = valueList[i];
                        }
                    }
                    if ( valueList[i] > maxVal) {
                        if ( Logic.smallerThanOrLargerThan(smallerThanCutoff, valueList[i], cutoff)) {
                            maxVal = valueList[i];
                        }
                    }
                }

                //General.showDebug("Found in model: " + i + " minVal: " + minVal);
                //General.showDebug("Found in model: " + i + " maxVal: " + maxVal);

                if ( getMax ) {
                    if ( maxVal != Float.MIN_VALUE ) {  // a value making cutoff existed.
                        if ( Defs.isNull(extreem) ) {
                            extreem = maxVal;
                            loc = i;
                        } else if ( maxVal > extreem ) {
                            extreem = maxVal;
                            loc = i;
                        }
                    }
                } else {
                    if ( minVal != Float.MAX_VALUE ) {  // a value making cutoff existed.
                        if ( Defs.isNull(extreem) ) {
                            extreem = minVal;
                            loc = i;
                        } else if ( minVal < extreem ) {
                            extreem = minVal;
                            loc = i;
                        }
                    }
                }
            }
        }
        return loc;
    }

    static public float getMin( float[] in ) {
        if ( in == null || (in.length == 0 )) {
            return Defs.NULL_FLOAT;
        }
        float result = Float.MAX_VALUE;
        for (int i=0;i<in.length;i++) {
            if ( result > in[i] ){
                result = in[i];
            }
        }
        return result;
    }

    /** Returns Defs.NULL_FLOAT on error */
    static public float getMax( float[] in ) {
        if ( in == null || (in.length == 0 )) {
            return Defs.NULL_FLOAT;
        }

        float result = Float.MIN_VALUE;
        for (int i=0;i<in.length;i++) {
            if ( result < in[i] ){
                result = in[i];
            }
        }
        return result;
    }

    /** Requires only one scan, so a little faster for large arrays than
     *calling to getMin and getMax individually.
     */
    static public float[] getMinMax( float[] in ) {
        if ( in == null || (in.length == 0 )) {
            return null;
        }

        float[] result = new float[] { in[0], in[0] };
        for (int i=1;i<in.length;i++) {
            if ( result[0] > in[i] ){
                result[0] = in[i];
            }
            if ( result[1] < in[i] ){
                result[1] = in[i];
            }
        }
        return result;
    }

    static public float[] toFloatArray( FloatArrayList in ) {
        if ( in == null ) {
            return null;
        }
        float[] in2 = in.elements();
        if ( in.size() != in2.length ) {
            in2 = new float[ in.size() ];
            System.arraycopy( in.elements(), 0, in2, 0, in.size());
        }
        return in2;
    }

    static public float[] toFloatArray( double in[] ) {
        if ( in == null ) {
            return null;
        }

        float[] result = new float[ in.length ];
        for (int i=0;i<in.length;i++) {
            result[i] = (float) in[i];
        }
        return result;
    }

    static public double[] toDoubleArray( float in[] ) {
        if ( in == null ) {
            return null;
        }

        double[] result = new double[ in.length ];
        for (int i=0;i<in.length;i++) {
            result[i] = (double) in[i];
        }
        return result;
    }

    /** doesn't make sense to center average over this does it? */
    static public float getAverage( FloatArrayList in, int avgMethod, double power, int numberMonomers ) {
        float[] in2 = toFloatArray( in );
        if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_SUM ) {
            return getAverageSum( in2, power, numberMonomers );
        }
        if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_R6 ) {
            return getAverageR6( in2, power );
        }
        if ( avgMethod == DistConstrList.DEFAULT_AVERAGING_METHOD_CENTER ) {
            General.showWarning("Averaging method of centering doesn't make sense when averaging plain distances. Return null.");
            return Defs.NULL_FLOAT;
        }
        General.showError("Unknown averaging method id: " + avgMethod);
        return Defs.NULL_FLOAT;
    }

    /** Sum averaging like done for distances in Xplor's sum averaging.
     *Use the power -6.0 normally.
     */
    static public float getAverageSum( float[] in, double power, int numberMonomers ) {
        double sum = 0; // There's no speed loss doing it on doubles as the pow function uses doubles.
        for (int i=0; i<in.length; i++) {
            sum += Math.pow( in[ i ], power);
        }
        sum = sum / numberMonomers;
        return (float) Math.pow( sum, 1/power );
    }

    /** Sum averaging like done for distances in Xplor's sum averaging.
     *Use the power -6.0 normally.
     *Name is a bit misleading because it can do any power obviously.
     */
    static public float getAverageR6( float[] in, double power ) {
        double sum = 0; // There's no speed loss doing it on doubles as the pow function uses doubles.
        for (int i=0; i<in.length; i++) {
            sum += Math.pow( in[ i ], power);
        }
        sum = sum / in.length;
        return (float) Math.pow( sum, 1/power );
    }


    /** Create a map for going from one list with n elements to a list where m
     *elements are deleted up shifting the remainder.
     * E.g.:
     *The mapping will be returned: e.g. going from 0,1,2,3,4,5,..n-1 deleting
     *elements 0 and 4 will give a mapping: 1,2,3,5,..n-3.
     *It is assumed that the indexes_todelete is sorted.
     *NOT TESTED!!!
     */
    static int[] createMapForCleanUp( int[] indexes_todelete, int size_original ) {

        General.showError("code needs to be checked before usage");

        // Next line can be remove.. its the assumption, right?
        //Arrays.sort( indexes );

        int size_new = size_original - indexes_todelete.length;
        int[] map = new int[ size_new ];
        int index_old = 0;
        int offset = 0;
        for (int i=0;i<size_original;i++) {
            if ( i == indexes_todelete[index_old] ) {
                index_old++;
                offset++;
                // Copy remaining; no more elements to be deleted left.
                if ( index_old == indexes_todelete.length ) {
                    for (int j=i;j<size_original;j++) {
                        map[i-offset] = i;
                    }
                    // Signal the job is done:
                    return map;
                }
            } else {
                map[i-offset] = i;
            }
        }
        // This shouldn't be the exit point
        General.doCodeBugExit( "Wrong exit point in code PrimitiveArray" );
        return null;
    }

    /** Look for the same value between lists a and b, return true on
     *first occurance found
     */
    public static boolean hasIntersection( IntArrayList a, IntArrayList b ) {
        int sizeA = a.size();
        int sizeB = b.size();
        for (int i=0;i<sizeA;i++) {
            int a_value = a.getQuick(i);
            for (int j=0;j<sizeB;j++) {
                if ( a_value == b.getQuick(j) ) {
                    return true;
                }
            }
        }
        return false;
    }
        /**
    public IntArrayList union( StringArrayList list_b ) {
        StringArrayList result = new StringArrayList();
        result.addAll(this);
        result.addAll(list_b);
        return result;
    }


    public StringArrayList intersection( StringArrayList list_b ) {
        StringArrayList result = new StringArrayList();
        for (Iterator i=this.iterator(); i.hasNext(); ) {
            String element = (String) i.next();
            if ( list_b.contains(element) ) {
                result.add(element);
            }
        }
        return result;
    }

    /** Multiset semantics...
         *Input should be sorted for speed.
         *a-b gives the elements in a that are not in b.
     */
    public static IntArrayList difference( IntArrayList list_a, IntArrayList list_b ) {

        IntArrayList result = new IntArrayList();
        for (int id_a=list_a.size()-1; id_a>=0; id_a--) { // reversed for speed
            int e_a = list_a.getQuick( id_a );
            // next method could be speeded up if range is given.
            if ( list_b.binarySearch( e_a ) < 0 ) { // wasn't present // requires sorted array
                result.add( e_a );
            }
        }
        return result;
    }


    /** Materialize a list of strings from a byte array with cp850 (pretty much ascii).
     */
    /** convert from the raw data to text lines.
     */
    public static ArrayList getLines( byte[] content ) {
        if ( content == null ) {
            General.showError("Can't do getLines from null ref of byte[]");
            return null;
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        //A stream that reads bytes from stream and converts them to characters.
        InputStreamReader isr = new InputStreamReader(bais);
        // A stream that can read a line at a time
        BufferedReader br = new BufferedReader(isr);
        // Loop over the lines.
        int line_count=0;
        String line = null;
        ArrayList lines = new ArrayList();

        try {
            do {
                line = br.readLine();
                if( line != null) {
                    lines.add( line );
                    line_count++;
                }
            } while( line != null );
            // Close stream
            br.close();
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
        //General.showDebug("Converted from the raw data: " + line_count + " text lines");
        return lines;
    }


    /** A simple function that takes a copy home.
     */

    public static int[] getElements( IntArrayList values ) {
        int[] result = new int[values.size()];
        System.arraycopy(values.elements(), 0, result, 0, values.size());
        return result;
    }

    /** Reshuffle the values by the order as specified in order parameter. */
    public static boolean orderIntArrayListByIntArray( IntArrayList values, int[] order ) {
        if ( values == null ) {
            General.showError("Values are null; failed to do orderIntArrayListByIntArray");
            return false;
        }
        if ( order == null ) {
            General.showError("Column for order is null; failed to do orderIntArrayListByIntArray");
            return false;
        }
        // special case 2 values present.
        int s = values.size();
        if ( s ==  2 ) {
            if ( order[ values.getQuick( 0 ) ] > order[ values.getQuick( 1 ) ] ) {
                // swap
                swap( values, 0, 1 );
            }
            return true;
        }
        // No need to sort on arrays less than 2 long anyway.
        if ( s < 2 ) {
            return true;
        }
        // Real effort:
        ArrayList l = new ArrayList();
        int[] valuesClone = new int[s]; // slightly cheaper than intArrayList
        for (int i=0;i<s;i++) {
            l.add( new int[] {order[ values.get( i)], i } );
            valuesClone[ i ] = values.getQuick( i );
        }
        try {
            Collections.sort(l, new ComparatorIntArray()); // sorts on first int in array of 2 ints.
        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }

        // Set the order
        for (int i=0;i<s;i++) {
            values.setQuick( i, valuesClone[ ((int[]) l.get(i))[1]] );
            //General.showDebug("Ordered value: " + values.getQuick(i) + " with order int: " + order[ values.getQuick(i) ] );
        }

        return true;
    }

    /** Swap the values in the array. Checks for too large row ids. */
    public static boolean swap( IntArrayList a, int row_1, int row_2 ) {
        if (a==null) {
            General.showError( "Array to swap elements in is null");
            return false;
        }
        int s = a.size();
        if ( row_1 >= s ) {
            General.showError( "Can't swap int values because row id 1 given: " + row_1 + " is beyond array length: " + s);
            return false;
        }
        if ( row_2 >= s ) {
            General.showError( "Can't swap int values because row id 2 given: " + row_2 + " is beyond array length: " + s);
            return false;
        }

        int tmp = a.getQuick( row_1 );
        a.setQuick(  row_1, a.getQuick(  row_2 ));
        a.setQuick(  row_2, tmp);
        return true;
    }


    /** Swap the values in the array. Checks for too large row ids. */
    public static boolean swap( int[] a, int row_1, int row_2 ) {
        if (a==null) {
            General.showError( "Array to swap elements in is null");
            return false;
        }

        if ( row_1 >= a.length ) {
            General.showError( "Can't swap int values because row id 1 given: " + row_1 + " is beyond array length: " + a.length);
            return false;
        }
        if ( row_2 >= a.length ) {
            General.showError( "Can't swap int values because row id 2 given: " + row_2 + " is beyond array length: " + a.length);
            return false;
        }

        int tmp = a[ row_1 ];
        a[ row_1 ] = a[ row_2 ] ;
        a[ row_2 ] = tmp;
        return true;
    }

    public static ArrayList toArrayList( Enumeration e ) {
        if ( e == null ) {
            return null;
        }
        ArrayList c = new ArrayList();
        for (; e.hasMoreElements() ;) {
          c.add( e.nextElement());
        }
        return c;
    }

    /** Signature says it all
     */
    public static ArrayList toArrayList(Object primitiveArray) {
        if ( primitiveArray instanceof float[] ) {
            float[] ar = (float[]) primitiveArray;
            ArrayList al = new ArrayList( ar.length );
            for (int i=0;i<ar.length;i++) {
                al.add( new Float( ar[i] ));
            }
            return al;
        }
        if ( primitiveArray instanceof String[] ) {
            String[] ar = (String[]) primitiveArray;
            ArrayList al = new ArrayList( ar.length );
            for (int i=0;i<ar.length;i++) {
                al.add( ar[i] );
            }
            return al;
        }
        General.showError("have to code toArrayList in PrimitiveArray for type: " + primitiveArray.getClass().getName());
        return null;
    }

    public static int[] getReverseIndex( int[] in) {
        int[] out = new int[in.length];
        for (int i=0;i<in.length;i++) {
            out[in[i]] = i;
        }
        return out;
    }

    public static ArrayList asList ( int[] in ) {
        ArrayList out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            out.add( new Integer(in[i]) );
        }
        return out;
    }

    public static String[] asStringArray ( ArrayList in ) {
        String[] out = new String[in.size()];
        for (int i=in.size()-1;i>=0;i--) {
            out[i] = (String) in.get(i);
        }
        return out;
    }

    public static String toString( short[] in ) {
        ArrayList out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            out.add( new Short(in[i]) );
        }
        return Strings.toString( out.toArray() );
    }

    public static String toString( boolean[] in ) {
        ArrayList out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            out.add( Boolean.valueOf(in[i]) );
        }
        return Strings.toString( out.toArray() );
    }
    /** Defaults to printing no eol char after each value */
    public static String toString( int[] in ) {
        return toString( in, false );
    }

    public static String toString( int[] in, boolean useBrackets ) {
        boolean printEOLAfterEach = false;
        return toString( in, printEOLAfterEach, useBrackets );
    }

    public static String toString( int[] in, boolean useBrackets, boolean printEOLAfterEach ) {
        if ( in == null ) {
            return null;
        }
        ArrayList out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            int value = in[i];
            if ( Defs.isNull(value) ) {
                out.add( null );
            } else {
                out.add( new Integer(value) );
            }
        }
        return Strings.toString( out.toArray(), printEOLAfterEach, useBrackets );
    }

    public static String toString( float[] in ) {
        ArrayList out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            out.add( new Float(in[i]) );
        }
        return Strings.toString( out.toArray() );
    }

    public static String toString( double[] in ) {
        ArrayList<Double> out = new ArrayList();
        for (int i=0;i<in.length;i++) {
            out.add( new Double(in[i]) );
        }
        return Strings.toString( out.toArray() );
    }

    public static String toString( Object o ) {
        if (o instanceof int[] ) {
            return toString( (int[]) o);
        }
        if (o instanceof float[] ) {
            return toString( (float[]) o);
        }
        if (o instanceof double[] ) {
            return toString( (double[]) o);
        }
        if (o instanceof short[] ) {
            return toString( (short[]) o);
        }
        if ( o == null ) {
            return Defs.NULL_STRING_NULL;
        }
        return o.toString();
    }

    public static String toString( IntArrayList in ) {
        return toString( in, true );
    }

    public static String toString( IntArrayList in, boolean useBrackets ) {
        boolean printEOLAfterEach = false;
        return toString( in, printEOLAfterEach, useBrackets );
    }

    public static String toString( IntArrayList in, boolean useBrackets, boolean printEOLAfterEach ) {
        int[] myElements = new int[in.size()];
        for (int i=myElements.length; --i >= 0; ) {
            myElements[i]=in.getQuick(i);
        }
        return toString( myElements, useBrackets, printEOLAfterEach );
    }

    public static String toString( BooleanArrayList in ) {
	boolean[] myElements = new boolean[in.size()];
	for (int i=myElements.length; --i >= 0; ) {
            myElements[i]=in.getQuick(i);
        }
        return toString( myElements );
    }

    public static String toString( Object[] in ) {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        for (int i=0;i<in.length;i++) {
            Object o = in[i];
            if ( o == null ) {
                sb.append("null");
            } else {
                sb.append(o.toString());
            }
            sb.append(", ");  // needs to be 2 chars or change below
        }
        if ( in.length == 0 ) {
            sb.append("empty");
        } else {
            sb.deleteCharAt(sb.length()-1);
            sb.deleteCharAt(sb.length()-1);
        }
        sb.append(']');
        return sb.toString();
    }



    /**
     */
    public static Object resizeArray( Object in, int size_new ) {
        if ( in instanceof float[] ) {
            float[] out = (float[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof int[] ) {
            int[] out = (int[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof BitSet ) {
            BitSet out = (BitSet) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof short[] ) {
            short[] out = (short[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof double[] ) {
            double[] out = (double[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof boolean[] ) {
            boolean[] out = (boolean[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof char[] ) {
            char[] out = (char[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof String[] ) {
            String[] out = (String[]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof int[][] ) {
            int[][] out = (int[][]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof float[][] ) {
            float[][] out = (float[][]) in;
            return resizeArray( out, size_new );
        } else if ( in instanceof Object[] ) { // ORDERING MATTERS FOR THIS
            Object[] out = (Object[]) in;
            return resizeArray( out, size_new );
        } else {
            General.showError("code error resizeArray called with object of class: " + in.getClass().getName());
            return null;
        }
    }

    /**
     */
    public static boolean fillArrayNulls( Object in, int fromIndex, int toIndex ) {
        if ( in instanceof float[] ) {
            Arrays.fill((float[]) in, fromIndex, toIndex, Defs.NULL_FLOAT);
        } else if ( in instanceof int[] ) {
            Arrays.fill((int[]) in, fromIndex, toIndex, Defs.NULL_INT);
        } else if ( in instanceof short[] ) {
            Arrays.fill((short[]) in, fromIndex, toIndex, Defs.NULL_SHORT);
        } else if ( in instanceof double[] ) {
            Arrays.fill((double[]) in, fromIndex, toIndex, Defs.NULL_DOUBLE);
        } else if ( in instanceof boolean[] ) {
            BitSet bs = (BitSet) in;
            bs.clear(fromIndex, toIndex);
        } else if ( in instanceof char[] ) {
            Arrays.fill((char[]) in, fromIndex, toIndex, Defs.NULL_CHAR);
        } else if ( in instanceof String[] ) {
            Arrays.fill((String[]) in, fromIndex, toIndex, Defs.NULL_STRING_NULL);
        } else if ( in instanceof int[][] ) {
            Arrays.fill((Object[]) in, fromIndex, toIndex, null);
        } else if ( in instanceof float[][] ) {
            Arrays.fill((Object[]) in, fromIndex, toIndex, null);
        } else if ( in instanceof BitSet ) {
            // already nice and nill.
        } else if ( in instanceof Object[] ) { // ORDERING MATTERS FOR THIS
            Arrays.fill((Object[]) in, fromIndex, toIndex, null);
        } else {
            General.showError("code error fillArrayNulls called with object of class: " + in.getClass().getName());
            return false;
        }
        return true;
    }


    public static BitSet resizeArray( BitSet in, int size_new ) {
        if ( size_new <= in.length() ) {
            General.showError("Not resizing BitSet from length: " + in.length() + " to: " + size_new);
            General.showError("Because bitset can not be shrinked at the moment");
            return null;
        }
        in.clear( size_new - 1); // This will cause the bitset to grow and have the last element be the default value (false);
        return in;
    }

    public static double[] resizeArray( double[] in, int size_new ) {
        double[] out = new double[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static float[] resizeArray( float[] in, int size_new ) {
        float[] out = new float[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static short[] resizeArray( short[] in, int size_new ) {
        short[] out = new short[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static int[] resizeArray( int[] in, int size_new ) {
        int[] out = new int[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static boolean[] resizeArray( boolean[] in, int size_new ) {
        boolean[] out = new boolean[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static char[] resizeArray( char[] in, int size_new ) {
        char[] out = new char[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static String[] resizeArray( String[] in, int size_new ) {
        String[] out = new String[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static Object[] resizeArray( Object[] in, int size_new ) {
        //General.showDebug("Using resizeArray on Object[]");
        Object[] out = new Object[size_new];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static int[][] resizeArray( int[][] in, int size_new ) {
        //General.showDebug("Using resizeArray on int[][]");
        int[][] out = new int[size_new][];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    public static float[][] resizeArray( float[][] in, int size_new ) {
        //General.showDebug("Using resizeArray on float[][]");
        float[][] out = new float[size_new][];
        int copySize = Math.min( in.length, size_new );
        System.arraycopy( in, 0, out, 0, copySize);
        return out;
    }

    /** Inverts the array contents in place
     */
    public static int[] invertArray( int[] in ) {
        /** E.g. for array of 5 elements 2 swaps are needed.
         */
        int sizeSwaps = in.length / 2; // Truncating the half
        //l=4 -> j=1;  l=5 -> j=2
        int j = (in.length + 1 )/ 2 - 1;
        for (int i=sizeSwaps-1;i>=0;i--) {
            j++;;
            int temp = in[i];
            in[i] = in[j];
            in[j] = temp;
        }
        return in;
    }

    /** Implementation of Array minus. Scales well to large sizes. Overkill for
     * small arrays. Memory intensive. Will delete all elements in the base
     * even if only 1 element occurs in min.
     * E.g.  input: {0,1,2,3,4,4}, {0,1,1,4,5}
     * gives output {2,3}.
     * @param base Original list
     * @param min List with elements to be deleted from the base.
     * @return Base minus min or base if either one is of length zero.
     */

    public static int[] minus( int[] base, int[] min ) {
        if ( min.length == 0 ) {
            return base;
        }
        if ( base.length == 0 ) {
            return base;
        }
        // don't want to sort so can't use: Arrays.binarySearch();
        int[] minCopy = new int[min.length];
        System.arraycopy( min, 0, minCopy, 0, min.length);
        int[] result = new int[base.length]; // will be resized later.

        int idx = 0;
        for (int i=0;i<base.length;i++ ) {
            int element = base[i];
            if ( Arrays.binarySearch( minCopy, element ) < 0 ) {
                // We copy the element because it doesn't occur in min.
                result[ idx ] = element;
                idx++;
            }
        }
        result = resizeArray(result, idx );
        return result;
    }

    /** Creates a int array where each element is the location where in the input
     *bitset the bit was set to true. This operation is useful if the BitSet is sparsely
     *populated.
     */
    public static IntArrayList toIntArrayList(BitSet selection) {
        /** No resizing needed. Longs are efficiently scanned.*/
        //int numberBitsSetToTrue = selection.cardinality();
        IntArrayList result = new IntArrayList();

        for (int r=selection.nextSetBit(0); r>=0; r=selection.nextSetBit(r+1)) {
            result.add( r );
        }
        return result;
    }

    /** Creates a int array where each element is the truncated value as in the array.
     */
    public static IntArrayList toIntArrayList(double[] values) {
        IntArrayList result = new IntArrayList(values.length);
        result.setSize(values.length);
        for (int i=0;i<values.length;i++) {
            result.setQuick(i, (int)values[i]);
        }
        return result;
    }

    /** Convenience method
     * Make it faster by using elements() from the colt api...*/
    public static int[] toIntArray(IntArrayList list) {
        int listSize = list.size();
        int[] result = new int[ listSize ];
        for (int i=0;i<listSize;i++) {
            result[i] = list.getQuick(i);
        }
        return result;
    }

    /** Convenience method */
    public static int[] toIntArray(BitSet selection) {
        int numberBitsSetToTrue = selection.cardinality();
//        if ( numberBitsSetToTrue == 0 ) {
//            General.showWarning("Can't create an int[] of length zero.");
//            return null;
//        }
        int[] result = new int[numberBitsSetToTrue];
        int i=0;
        for (int r=selection.nextSetBit(0); r>=0; r=selection.nextSetBit(r+1)) {
            result[i] = r;
            i++;
        }
        return result;
    }

    /** Convenience method
     */
    public static BitSet toBitSet(IntArrayList list ) {
        return toBitSet(list, -1);
    }

    /** Creates a bitset elements are set if the input array has the index.
     *This operation is useful if the array list is densely populated.
     *The target size of the bitSet can be given as -1 to indicate it is
     *supposed to grow as needed.
     */
    public static BitSet toBitSet(IntArrayList list, int bitSetSize ) {
        /** No resizing needed. Longs are efficiently scanned.*/
        BitSet result;
        if ( bitSetSize > 0 ) {
            result = new BitSet(bitSetSize);
        } else {
            result = new BitSet();
        }
        // Reverse fill faster on the done check
        // Get a reference so get can be done faster; Don't change the array!
        int[] actual_list = list.elements();
        for (int i=list.size()-1;i>-1;i--) {
            result.set( actual_list[ i ] );
        }
        return result;
    }

    public static BitSet toBitSet(int[] list, int bitSetSize ) {
        return toBitSet(new IntArrayList(list), bitSetSize );
    }

    /** Convenience method
     */
    public static BitSet toBitSet(int[] list ) {
        return toBitSet(list, -1);
    }

    /** Returns the number of set bits in the bitset.
     */
    public static int countSet( BitSet bs ) {
        int result = 0;
        for (int r=bs.nextSetBit(0); r>=0; r=bs.nextSetBit(r+1)) {
            if ( bs.get(r) ) {
                result++;
            }
        }
        return result;
    }

    public static String toString( BitSet bs ) {
        return toString( bs, true );
    }

    /** Renders the BitSet to a String representation on 1 line possibly
     *showing the T/F values
     */
    public static String toString( BitSet bs, boolean showValues ) {

        int count = bs.cardinality();
        StringBuffer sb = new StringBuffer(bs.size()+99);
        Parameters p = new Parameters(); // Printf parameters
        sb.append("Size: " );
        sb.append( Format.sprintf("%5d", p.add( bs.size() )));
        sb.append(" Cardinality: " );
        sb.append( Format.sprintf("%5d", p.add( count )));
        if ( showValues ) {
            sb.append(" [");
            for (int i=0;i<bs.size();i++) {
                if ( bs.get(i) ) {
                    sb.append('T');
                } else {
                    if ( i % 5 == 0 ) {
                        if ( i % 10 == 0 ) {
                            int r = i/10;
                            while ( r > 9 ) {
                                r -= 10;
                            }
                            sb.append(r);
                        } else {
                            sb.append('.');
                        }
                    } else {
                        sb.append(' ');
                    }
                }
            }
            sb.append(']');
        }
        return sb.toString();
    }


    /**
     * Returns the number of items over a threshold.
     */
    public static int countMakingCutoff( String text) {
        return Strings.countStrings(text,countMakingCutoffMatcher);
    }

    /**
     * Returns the number of items over a threshold.
     */
    public static int countMakingCutoff( float[] valueList, float cutoff,
            boolean smallerThanCutoff ) {
        int count = 0;
        /**
        General.showDebug("Doing countMakingCutoff with cutoff: " + cutoff +
                " smallerThanCutoff: " + smallerThanCutoff +
                " on: " + toString(valueList));
         */
        if ( smallerThanCutoff ) {
            for (int i=0;i<valueList.length;i++) {
                if ( valueList[i] < cutoff ) {
                    count++;
                }
            }
            return count;
        }

        for (int i=0;i<valueList.length;i++) {
            if ( valueList[i] > cutoff ) {
                //General.showDebug("counted up to: " + count);
                count++;
            }
        }
        return count;
    }

    /**
     * Given a list (per restraints) of list of values(per model) and a cutoff return
     * a string that shows for
     * each value whether it made the cutoff anywhere in the restraints or not
     * by showing a * for that value.
     * @see Wattos.Soup.Constraint.DistConstrList#setTagTableRes
     * @param listObject Can be an ArrayList of float[] or a float[].
     * @param cutoff Value of cutoff.
     * @param smallerThanCutoff False if looking for values that are larger than cutoff.
     * @return Descriptive text.
     */
    public static String toStringMakingCutoff( Object listObject, float cutoff,
            boolean smallerThanCutoff ) {

        float minVal = Float.MAX_VALUE;
        float maxVal = Float.MIN_VALUE;

        int maxId = getMaxLocationMakingCutoff(listObject,cutoff,smallerThanCutoff,true);
        if ( maxId == -2 ) {
            General.showError("Failed getMaxLocationMakingCutoff (with max) in toStringMakingCutoff");
            return null;
        }

        int minId = getMaxLocationMakingCutoff(listObject,cutoff,smallerThanCutoff,false);
        if ( minId == -2 ) {
            General.showError("Failed getMaxLocationMakingCutoff (with min) in toStringMakingCutoff");
            return null;
        }

        if ( minId == maxId ) {
            if ( smallerThanCutoff ) {
                maxId = -1;
            } else {
                minId = -1;
            }
        }

        int countModels = 0;
        boolean listIsArrayList = true;
        float[] valueList = null;
        ArrayList al = null;
        if ( listObject instanceof ArrayList ) {
            al = (ArrayList) listObject;
            valueList = (float[]) al.get(0);
            if ( (valueList == null) || (valueList.length==0)) {
                General.showError("Got invalid values in toStringMakingCutoff");
                return null;
            }
            countModels = valueList.length;
        } else {
            listIsArrayList = false;
            valueList = (float[]) listObject; // keep reference.
            countModels = valueList.length;
        }

        char[] s = new char[countModels];
        Arrays.fill(s,' ');

        int n = 0; // modelnumber
        for (int i=0;i<countModels;i++) { // loop over models
            n++;
            if ( i==minId) {
                s[i] = '-';
            } else if ( i==maxId) {
                s[i] = '+';
            } else {
                if ( ! listIsArrayList ) {
                    if ( ( smallerThanCutoff && ( valueList[i] < cutoff)) || // can be expressed faster, right?
                         (!smallerThanCutoff && ( valueList[i] > cutoff))) {
                        s[i] = CHARACTER_INDICATING_MAKING_CUTOFF;
                        continue;
                    }
                } else {
                    minVal = Float.MAX_VALUE;
                    maxVal = Float.MIN_VALUE;
                    for (int j=0;j<al.size();j++) { // loop over restraints
                        valueList = (float[]) al.get(j);
                        if ( smallerThanCutoff ) {
                            if ( valueList[i] < minVal) {
                                minVal = valueList[i];
                            }
                        } else {
                            if ( valueList[i] > maxVal) {
                                maxVal = valueList[i];
                            }
                        }
                    }
                    if ( ( smallerThanCutoff && ( minVal < cutoff)) ||
                         (!smallerThanCutoff && ( maxVal > cutoff))) {
                        s[i] = CHARACTER_INDICATING_MAKING_CUTOFF;
                        continue;
                    }
                }
                if ( n % 5 == 0 ) {
                    if ( n % 10 == 0 ) {
                        int r = n/10;
                        while ( r > 9 ) {
                            r -= 10;
                        }
                        s[i] = (char) (48 + r);
                    } else {
                        s[i] = '.';
                    }
                }
            }
        }

        return new String(s);
    }

    /** NOT USED
    public static Object convertFloat2Int(Object columnIn, int dataType, String format) {
        int previousValue;
        String previousString;
        int currentValue;
        String currentString;
        int[] column = (int[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[sizeMax];

        Parameters p = new Parameters(); // Printf parameters

        // Do the last one first.
        currentValue =  column[sizeMax-1];
        // Deal with this later for STAR cases.
        if ( format == null ) {
            currentString = Float.toString( currentValue );
        } else {
            currentString = Format.sprintf(format, p.add( currentValue ));
        }
        result[sizeMax-1] = currentString;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentValue = column[r];
            if ( currentValue == previousValue ) {
                result[r] = previousString;
            } else {
                if ( format == null ) {
                    currentString = Float.toString( currentValue );
                } else {
                    currentString = Format.sprintf(format, p.add( currentValue ));
                }
                result[r] = currentString;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }
     */

    /** See convertString2Float
     */
    public static Object convertFloat2String(Object columnIn, int dataType, String format) {
        float previousValue;
        String previousString;
        float currentValue;
        String currentString;
        if ( ! (columnIn instanceof float[] )) {
            General.showError("Column to convert to string needs to be of type float but is: " + columnIn.getClass().getName());
            return null;
        }
        float[] column = (float[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        Parameters p = new Parameters(); // Printf parameters

        // Do the last one first.
        currentValue =  column[sizeMax-1];

        // Deal with nulls in consistent way.
        if ( Defs.isNull( currentValue )) {
            currentString = null; // Default value for String
        } else {
            if ( format == null ) {
                currentString = Float.toString( currentValue );
            } else {
                currentString = Format.sprintf(format, p.add( currentValue ));
            }
        }
        result[sizeMax-1] = currentString;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentValue = column[r];
            if ( Defs.isNull( currentValue )) {
                result[r] = null; // Default value for string
            } else if ( currentValue == previousValue ) {
                result[r] = previousString;
            } else {
                if ( format == null ) {
                    currentString = Float.toString( currentValue );
                } else {
                    currentString = Format.sprintf(format, p.add( currentValue ));
                }
                result[r] = currentString;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }

    /** See convertString2Float
     */
    public static Object convertDouble2String(Object columnIn, int dataType, String format) {
        double previousValue;
        String previousString;
        double currentValue;
        String currentString;
        if ( ! (columnIn instanceof double[] )) {
            General.showError("Column to convert to string needs to be of type double but is: " + columnIn.getClass().getName());
            return null;
        }
        double[] column = (double[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        Parameters p = new Parameters(); // Printf parameters

        // Do the last one first.
        currentValue =  column[sizeMax-1];

        // Deal with nulls in consistent way.
        if ( Defs.isNull( currentValue )) {
            currentString = null; // Default value for String
        } else {
            if ( format == null ) {
                currentString = Double.toString( currentValue );
            } else {
                currentString = Format.sprintf(format, p.add( currentValue ));
            }
        }
        result[sizeMax-1] = currentString;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentValue = column[r];
            if ( Defs.isNull( currentValue )) {
                result[r] = null; // Default value for string
            } else if ( currentValue == previousValue ) {
                result[r] = previousString;
            } else {
                if ( format == null ) {
                    currentString = Double.toString( currentValue );
                } else {
                    currentString = Format.sprintf(format, p.add( currentValue ));
                }
                result[r] = currentString;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }


    /**
     *Use 0 and 1 by default to represent the value so that MySQL can read it.<BR>
     *In NMR-STAR 3.0 it is yes/no (use the format: %yesno).<BR>
     *Another possibility is simply T/F (use the format: %c).
     *format is allowed to be null.
     */
    public static Object convertBit2String(Object columnIn, int dataType, String format) {
        BitSet column = (BitSet) columnIn;
        int sizeMax = column.size();
        String[] result = new String[sizeMax];
        String strTrue = "1";
        String strFalse = "0";
        if ( format == null ) {
            format = Defs.EMPTY_STRING;
        }
        if ( format.equalsIgnoreCase("%c")) {
            strTrue = "T";
            strFalse = "F";
        }
        if ( format.equalsIgnoreCase("%yesno")) {
            strTrue = "yes";
            strFalse = "no";
        }
        Arrays.fill(result, strFalse); // A bit faster
        if ( column.size() == 0 ) {
            return result;
        }
        for (int i=column.nextSetBit(0); i>=0; i=column.nextSetBit(i+1))  {
            result[i] = strTrue;
        }
        return result;
    }


    public static Object convertInt2String(Object columnIn, int dataType, String format) {
        int previousValue;
        String previousString;
        int currentValue;
        String currentString;
        if ( ! (columnIn instanceof int[] )) {
            General.showError("Column to convert to string needs to be of type int but is: " + columnIn.getClass().getName());
            return null;
        }
        int[] column = (int[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        Parameters p = new Parameters(); // Printf parameters

        // Do the last one first.
        currentValue =  column[sizeMax-1];

        // Deal with nulls in consistent way.
        if ( Defs.isNull( currentValue )) {
            currentString = null; // Default value for String
        } else {
            if ( format == null ) {
                currentString = Integer.toString( currentValue );
            } else {
                currentString = Format.sprintf(format, p.add( currentValue ));
            }
        }
        result[sizeMax-1] = currentString;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentValue = column[r];
            if ( Defs.isNull( currentValue )) {
                result[r] = null; // Default value for string
            } else if ( currentValue == previousValue ) {
                result[r] = previousString;
            } else {
                if ( format == null ) {
                    currentString = Integer.toString( currentValue );
                } else {
                    currentString = Format.sprintf(format, p.add( currentValue ));
                }
                result[r] = currentString;
                previousValue = currentValue;
                previousString = currentString;
            }
            //General.showDebug("Using string for integer: [" + currentString + "]");
        }
        return result;
    }

    /** See convertString2Float
     */
    public static Object convertString2String(Object columnIn, int dataType, String format) {
        if ( format == null ) {
            General.showWarning("No need for conversion if format is null");
            return columnIn;
        }
        String previousValue;
        String previousString;
        String currentValue;
        String currentString;
        if ( ! (columnIn instanceof String[] )) {
            General.showError("Column to convert to string needs to be of type String but is: " + columnIn.getClass().getName());
            return null;
        }
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        Parameters p = new Parameters(); // Printf parameters

        // Do the last one first.
        currentValue =  column[sizeMax-1];

        // Deal with nulls in consistent way.
        if ( Defs.isNullString( currentValue )) {
            currentString = null; // Default value for String
        } else {
            currentString = Format.sprintf(format, p.add( currentValue ));
        }
        result[sizeMax-1] = currentString;
        previousValue = currentValue;
        previousString = currentString;

        //boolean formatEqualsPercentQ = format.equals("%q");
        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentValue = column[r];
            if ( Defs.isNullString( currentValue )) {
                result[r] = null; // Default value for string
            } else if ( currentValue == previousValue ) {
                result[r] = previousString;
            } else {
                //if ( formatEqualsPercentQ ) {
                //    currentString = currentValue + " ";
                //} else {
                    currentString = Format.sprintf(format, p.add( currentValue ));
                //}
                result[r] = currentString;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }

    /** Converts a whole column from string to float. In the case the
     *string is not a float, e.g. null or the "." string, the float will get a special null value as defined in the
     *database routines.
     * Will attempt to convert the data type of a certain column to the given
     * data type. Returns null on failure. Only 2 lines different from code for
     * other data types; please watch duplication!!!!!!!!!!!!!!!!!!!!11
     */
    public static Object convertString2Float(Object columnIn, int dataType) {
        float previousValue;
        String previousString;
        float currentValue;
        String currentString;
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        float[] result = new float[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        // Do the last one first.
        currentString =  column[sizeMax-1];
        if ( Defs.isNullString( currentString ) ) {
            currentValue = Defs.NULL_FLOAT; // Default value for float
        } else {
            currentValue = Float.parseFloat( column[sizeMax-1] );
        }
        result[sizeMax-1] = currentValue;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentString =  column[r];
            if (Defs.isNullString( currentString )) {
                result[r] = Defs.NULL_FLOAT;
            } else if ( currentString.equals( previousString ) ) {
                result[r] = previousValue;
            } else {
                currentValue = Float.parseFloat( currentString );
                result[r] = currentValue;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }

    /** Converts a whole column from string to double. In the case the
     *string is not a double, e.g. null or the "." string, the float will get a special null value as defined in the
     *database routines.
     * Will attempt to convert the data type of a certain column to the given
     * data type. Returns null on failure. Only 2 lines different from code for
     * other data types; please watch duplication!!!!!!!!!!!!!!!!!!!!11
     */
    public static Object convertString2Double(Object columnIn, int dataType) {
        double previousValue;
        String previousString;
        double currentValue;
        String currentString;
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        double[] result = new double[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        // Do the last one first.
        currentString =  column[sizeMax-1];
        //General.showError("string: [" + currentString + "]");
        if ( Defs.isNullString( currentString ) ) {
            currentValue = Defs.NULL_DOUBLE; // Default value for double
        } else {
            currentValue = Double.parseDouble( currentString );
        }
        result[sizeMax-1] = currentValue;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentString =  column[r];
            if (Defs.isNullString( currentString )) {
                result[r] = Defs.NULL_DOUBLE;
            } else if ( currentString.equals( previousString ) ) {
                result[r] = previousValue;
            } else {
                currentValue = Double.parseDouble( currentString );
                result[r] = currentValue;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }

    /** Converts a whole column from string to bit. In the case the
     *string is not a bit, e.g. null or the "." string, the bit will get NO special null value as defined in the
     *database routines. It will simply default to java's default: false.
     * Will attempt to convert the data type of a certain column to the given
     * data type. Returns null on failure. Only 2 lines different from code for
     * other data types; please watch duplication!!!!!!!!!!!!!!!!!!!!
     */
    public static Object convertString2Bit(Object columnIn, int dataType) {
        boolean previousValue;
        String previousString;
        boolean currentValue;
        String currentString;
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        BitSet result = new BitSet(column.length);
        if ( column.length == 0 ) {
            return result;
        }

        // Do the last one first.
        currentString =  column[sizeMax-1];
        //General.showError("string: [" + currentString + "]");
        if ( Defs.isNullString( currentString ) ) {
            currentValue = false; // Default value for bit
        } else {
            currentValue = Strings.parseBoolean( currentString );
        }
        result.set(sizeMax-1, currentValue);
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentString =  column[r];
            if (Defs.isNullString( currentString )) {
                result.set(r, false);
            } else if ( currentString.equals( previousString ) ) {
                result.set(r, previousValue);
            } else {
                currentValue = Strings.parseBoolean( currentString );
                result.set(r, currentValue);
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }


    /** See convertString2Float
     */
    public static Object convertString2Int(Object columnIn, int dataType) {
        int previousValue;
        String previousString;
        int currentValue;
        String currentString;
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        int[] result = new int[column.length];
        if ( column.length == 0 ) {
            return result;
        }
        int r=sizeMax-1;
        try {
            // Do the last one first.
            currentString =  column[sizeMax-1];
            if ( Defs.isNullString( currentString )) {
                currentValue = Defs.NULL_INT; // Null value for int.
            } else {
                currentValue = Integer.parseInt( column[r] );
            }
            result[sizeMax-1] = currentValue;
            previousValue = currentValue;
            previousString = currentString;

            // Do the rest.
            r=sizeMax-2;
            for (;r>-1;r--) {
                // Only do parse if previous string was different.
                currentString =  column[r];
                if ( Defs.isNullString( currentString )) {
                    result[r] = Defs.NULL_INT;
                } else if ( currentString.equals( previousString ) ) {
                    result[r] = previousValue;
                } else {
                    currentValue = Integer.parseInt( currentString );
                    result[r] = currentValue;
                    previousValue = currentValue;
                    previousString = currentString;
                }
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            General.showError("For value on row id: " + r + ". Note that rows are processed last first.");
            return null;
        }
        return result;
    }

    /** See convertString2Float
     */
    public static Object convertObject2String(Object columnIn ) {
        if ( ! (columnIn instanceof Object[]) ) {
            General.showError("argument is not a instanceof (Object[])");
            return null;
        }

        Object[] column = (Object[]) columnIn;
        int sizeMax = column.length;
        String[] result = new String[sizeMax];
        if ( sizeMax == 0 ) {
            return result;
        }

        // Do the rest.
        for (int r=0;r<sizeMax;r++) {
            if ( Defs.isNull( column[r] )) {
                result[r] = Defs.NULL_STRING_NULL;
                continue;
            }
            result[r] = column[r].toString();
        }
        return result;
    }


    /** See convertString2Float
     */
    public static Object convertString2ArrayOfInt(Object columnIn, int dataType) {
        final String regexp = " ";  // Extend later on to capture csv values too.
        String currentString;
        String[] column = (String[]) columnIn;
        int[][] result = new int[column.length][];
        if ( column.length == 0 ) {
            return result;
        }


        // Do the last one first.
        for (int r=column.length-1;r>=0;r--) {
            currentString = column[r];
            if ( Defs.isNullString( currentString )) {
                result[r] = (int[]) Defs.NULL_OBJECT_NULL; // Null value for array of ints.
            } else {
                String[] temp = currentString.split( regexp );
                result[r] = (int[]) convertString2Int(temp, dataType);
            }
        }
        return result;
    }

    /** See convertString2Float
     */
    public static Object convertString2ArrayOfFloat(Object columnIn, int dataType) {
        final String regexp = " ";  // Extend later on to capture csv values too.
        String currentString;
        String[] column = (String[]) columnIn;
        float[][] result = new float[column.length][];
        if ( column.length == 0 ) {
            return result;
        }


        // Do the last one first.
        for (int r=column.length-1;r>=0;r--) {
            currentString = column[r];
            if ( Defs.isNullString( currentString )) {
                result[r] = (float[]) Defs.NULL_OBJECT_NULL; // Null value for array of ints.
            } else {
                String[] temp = currentString.split( regexp );
                result[r] = (float[]) convertString2Float(temp, dataType);
            }
        }
        return result;
    }

    /** See convertString2Float
     */
    public static Object convertString2ArrayOfString(Object columnIn, int dataType) {
        final String regexp = " ";  // Extend later on to capture csv values too.
        String currentString;
        String[] column = (String[]) columnIn;
        String[][] result = new String[column.length][];
        if ( column.length == 0 ) {
            return result;
        }


        // Do the last one first.
        for (int r=column.length-1;r>=0;r--) {
            currentString = column[r];
            if ( Defs.isNullString( currentString )) {
                result[r] = (String[]) Defs.NULL_OBJECT_NULL; // Null value for array of ints.
            } else {
                result[r] = currentString.split( regexp );
            }
        }
        return result;
    }


    /** See convertString2Float
     */
    public static Object convertString2Short(Object columnIn, int dataType) {
        short previousValue;
        String previousString;
        short currentValue;
        String currentString;
        String[] column = (String[]) columnIn;
        int sizeMax = column.length;
        short[] result = new short[column.length];
        if ( column.length == 0 ) {
            return result;
        }

        // Do the last one first.
        currentString =  column[sizeMax-1];
        if ( Defs.isNullString( currentString )) {
            currentValue = Defs.NULL_SHORT;
        } else {
            currentValue = Short.parseShort( column[sizeMax-1] );
        }
        result[sizeMax-1] = currentValue;
        previousValue = currentValue;
        previousString = currentString;

        // Do the rest.
        for (int r=sizeMax-2;r>-1;r--) {
            // Only do parse if previous string was different.
            currentString =  column[r];
            if ( Defs.isNullString( currentString )) {
                result[r] = Defs.NULL_SHORT;
            } else if ( currentString.equals( previousString ) ) {
                result[r] = previousValue;
            } else {
                currentValue = Short.parseShort( currentString );
                result[r] = currentValue;
                previousValue = currentValue;
                previousString = currentString;
            }
        }
        return result;
    }

    /** Will attempt to convert the data type of a certain column to the given
     * data type. Returns null on failure. Returns ArrayList with String[] and
     * StringSet objects.
     */
    public static Object convertString2StringNR(Object columnIn, int dataType) {
        String[] column = (String[]) columnIn;
        return StringSet.convertColumnString2StringNR( column );
    }

    /** Sort the lists according to how the first lists gets sorted.
     *Input needs to be a list of IntArrayList.
     *Returns true for success only.
     *The algorithm is very wastefull with memory so watch out.
     */
    public static boolean sortTogether( Object[] lists ) {
        if ( lists == null ) {
            General.showError("List input to sortTogether is null" );
            return false;
        }
        if ( lists.length < 2 ) {
            General.showError("List input to sortTogether has only one list. Use a different algo." );
            return false;
        }

        IntArrayList fList = (IntArrayList) lists[0];
        ArrayList sList = new ArrayList(); // Used to build up the memory.
        for (int i=0;i<fList.size();i++) {
            sList.add( new int[] { fList.getQuick(i), i });
        }
        Collections.sort( sList, new ComparatorIntArray() ); // will sort on first and then possibly on second int (which is the original position!).

        Object[] listsCopy = (Object[]) Objects.deepCopy(lists);
        // Copy from the copy to the original list in the sorted order.
        int index = 0;
        for (int i=0;i<fList.size();i++) {
            index = ((int[])sList.get(i))[1]; // original position in index.
            for (int j=0;j<lists.length;j++) { // do ALL lists, including the list that's already sorted.
                IntArrayList oList = (IntArrayList) lists[j];
                IntArrayList cList = (IntArrayList) listsCopy[j];
                oList.setQuick(i, cList.getQuick(index));
            }
        }
        return true;
    }




    /** Will return the reverse of the mapping given.
     *E.g.
     *0123456789
     *6 52 013 4
     *returns a list: 563920
     */
    public static int[] invertFromToMap( int[] map, BitSet todo ) {
        return null; // todo.
    }

    /** Sort the lists according to the given map
     *Input needs to be a list of IntArrayList.
     *Returns true for success only.
     *
     *The algorithm is very wastefull with memory watch out.
     *E.g. a list of atom ids:
     *91,90,93,92 would perhaps be ordered 90,91,92,93
     *for that list a map would be returned:
     *1,0,3,2 to be read as
     *original element at postion 0 (91) should go to new position 1 etc.
     */
    public static boolean sortTogether( int[] orderMap, Object[] lists ) {
        int newPos;
        if ( lists == null ) {
            General.showError("List input to sortTogether is null" );
            return false;
        }
        if ( lists.length < 1 ) {
            General.showError("List input to sortTogether has no lists. What's going on?" );
            return false;
        }
        try {
            // Check length
            for (int j=0;j<lists.length;j++) {
                IntArrayList oList = (IntArrayList) lists[j];
                if ( oList.size() < orderMap.length ) {
                    General.showError("Failed to do sortTogether because at least 1 list isn't as long as the orderMap given");
                    return false;
                }
            }
            Object[] listsCopy = (Object[]) Objects.deepCopy(lists);
            // Copy from the copy to the original list in the sorted order.
            for (int i=0;i<orderMap.length;i++) {
                for (int j=0;j<lists.length;j++) {
                    IntArrayList oList = (IntArrayList) lists[j];
                    IntArrayList cList = (IntArrayList) listsCopy[j];
                    newPos = orderMap[i];
                    oList.setQuick(newPos, cList.getQuick(i));
                }
            }
        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /** set the bits in the results for those rids where the list equals the value
     */
    public static BitSet getRidsByValue( int[] list, int value) {
        BitSet result = new BitSet(); // the first set will determine lenght more efficiently than set here.
        for (int i=list.length-1;i>=0;i--) {
            if ( list[i]== value) {
                result.set(i);
            }
        }
        return result;
    }

    /** Simple utility */
    public static boolean setValueByRids( FloatArrayList in, BitSet rids, float value ) {
        for (int r=rids.nextSetBit(0); r>=0; r=rids.nextSetBit(r+1)) {
            in.setQuick(r, value);
        }
        return true;
    }

    /** Simple utility */
    public static boolean setValueByRids( IntArrayList in, BitSet rids, int value ) {
        for (int r=rids.nextSetBit(0); r>=0; r=rids.nextSetBit(r+1)) {
            in.setQuick(r, value);
        }
        return true;
    }

    /** Simple utility */
    public static boolean setValueByArray( int[] in, BitSet rids ) {
        for (int i=in.length-1;i>=0;i--) {
            rids.set( in[i] );
        }
        return true;
    }

    /** Set the values in "in" to the given value "value" for the set rids. */
    public static boolean setValueByRids( int[] in, BitSet rids, int value ) {
        for (int i=rids.nextSetBit(0);i>=0;i=rids.nextSetBit(i+1)) {
            if (rids.get(i)) {
                in[ i ] = value;
            }
        }
        return true;
    }

    /** Simple utility */
    public static boolean setValueByRids( IntArrayList in, IntArrayList rids, int value ) {
        for (int r=rids.size()-1; r>=0; r--) {
            in.setQuick(rids.getQuick(r), value);
        }
        return true;
    }

    /** Simple utility */
    public static boolean setValueByRids( FloatArrayList in, IntArrayList rids, float value ) {
        for (int r=rids.size()-1; r>=0; r--) {
            in.setQuick(rids.getQuick(r), value);
        }
        return true;
    }

    /** Simple utility */
    public static boolean setValueByRids( float[] in, IntArrayList rids, float value ) {
        for (int r=rids.size()-1; r>=0; r--) {
            in[ rids.getQuick(r)] = value;
        }
        return true;
    }

    public static int hashCode(int[] in) {
        /** old method give duplicates for e.g.:
ERROR: Atom rids duplicate are: [160,207]
ERROR: Atom rids previous  are: [161,176]

        int hashCode = 1;
        for (int i=0;i<in.length;i++) {
          hashCode = 31*hashCode + in[i];
        }
         */
        return toString(in).hashCode();
    }


    public static String[] stripDashedMembers( String[] in ) {
        LinkedList inList = new LinkedList( Arrays.asList( in )); // using linked list makes it faster to remove
        for (int i=0;i<inList.size();i++) {
            String value = (String) inList.get(i);
            if ( value.startsWith( "-" ) ) {
                inList.remove( i );
                i--;
            }
        }
        return Strings.toStringArray( inList );
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        General.verbosity = General.verbosityDebug;
//        if ( false ) {
//            int[] list = {0,1,2};
//            General.showOutput("List is: " + PrimitiveArray.toString(list));
//            PrimitiveArray.invertArray( list );
//            General.showOutput("List is: " + PrimitiveArray.toString(list));
//        }
//        if ( false ) {
//            int[] list1 = {0,1,2,2,3,4,4};
//            int[] list2 = {0,1,1,4,5};
//
//            General.showOutput("List is: " + PrimitiveArray.toString(list1));
//            General.showOutput("List is: " + PrimitiveArray.toString(list2));
//            int[] list3 = minus( list1, list2 );
//            General.showOutput("Minus is:" + PrimitiveArray.toString(list3));
//        }
//        if ( false ) {
//            Object[] list = { "a", "b" };
//            General.showOutput("list is:" + PrimitiveArray.toString(list));
//        }
//        if ( false ) {
//            float v1 = 5.5f;
//            float[] list = {v1,v1,v1 };
//            General.showOutput("list is:" + PrimitiveArray.toString(list));
//            General.showOutput("Average regular     : " + getAverage( list ));
//            General.showOutput("Average by sum      : " + getAverageSum( list, -6.0, 1 ));
//            General.showOutput("Average by R6       : " + getAverageR6( list, -6.0 ));
//        }
//        if ( false ) {
//            IntArrayList values = new IntArrayList();
//            values.setSize(4);
//            values.set(0,2);
//            values.set(1,1);
//            values.set(2,0);
//            values.set(3,1);
//            int[] order = { 103, 102, 101 };
//            General.showOutput("values are  :" + PrimitiveArray.toString(values));
//            General.showOutput("order is    :" + PrimitiveArray.toString(order));
//            orderIntArrayListByIntArray( values, order );
//            General.showOutput("values are  :" + PrimitiveArray.toString(values));
//        }
//        if ( false ) {
//            IntArrayList a = new IntArrayList();
//            a.setSize(4);
//            a.set(0,1);
//            a.set(1,1);
//            a.set(2,0);
//            a.set(3,1);
//            IntArrayList b = new IntArrayList();
//            b.setSize(4);
//            b.set(0,-1);
//            b.set(1,-2);
//            b.set(2,-3);
//            b.set(3,-4);
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("b values are  :" + PrimitiveArray.toString(b));
//            General.showOutput("has intersection:" + hasIntersection(a,b));
//        }
//        if ( false ) {
//            IntArrayList a = new IntArrayList();
//            a.setSize(4);
//            a.set(0,4);
//            a.set(1,3);
//            a.set(2,2);
//            a.set(3,2);
//            IntArrayList b = new IntArrayList();
//            b.setSize(4);
//            b.set(0,-1);
//            b.set(1,-2);
//            b.set(2,-3);
//            b.set(3,-4);
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("b values are  :" + PrimitiveArray.toString(b));
//            General.showOutput("sorted together");
//            int[] mapOrder = new int[] { 3,2,0,1};
//            if ( ! PrimitiveArray.sortTogether( mapOrder, new Object[] { a, b } ) ) {
//                General.showError("Failed to sort IntArrayLists together");
//            }
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("b values are  :" + PrimitiveArray.toString(b));
//        }
//        if ( false ) {
//            IntArrayList a = new IntArrayList();
//            a.setSize(4);
//            a.set(0,4);
//            a.set(1,3);
//            a.set(2,2);
//            a.set(3,2);
//            IntArrayList b = new IntArrayList();
//            b.setSize(4);
//            b.set(0,-1);
//            b.set(1,-2);
//            b.set(2,-3);
//            b.set(3,-4);
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("b values are  :" + PrimitiveArray.toString(b));
//            General.showOutput("sorted together");
//            if ( ! PrimitiveArray.sortTogether( new Object[] { a, b } ) ) {
//                General.showError("Failed to sort IntArrayLists together");
//            }
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("b values are  :" + PrimitiveArray.toString(b));
//        }
        if ( true ) {
            IntArrayList a = new IntArrayList();
            a.setSize(4);
            a.set(0,4);
            a.set(1,3);
            a.set(2,2);
            a.set(3,2);
            General.showOutput("a values are  : " + PrimitiveArray.toString(a, false));
            if ( ! PrimitiveArray.removeDuplicatesBySort( a ) ) {
                General.showError("Failed to removeDuplicates IntArrayLists");
            }
            General.showOutput("a values are  : " + PrimitiveArray.toString(a));
        }
//        if ( false ) {
//            int[] a = new int[] { 32, 37, 38, 39 };
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("hash          :" + PrimitiveArray.hashCode(a));
//        }
//        if ( false ) {
//            String[] a = new String[] { "a b", "-c", "d" };
//            General.showOutput("a values are  :" + PrimitiveArray.toString(a));
//            General.showOutput("a values are  :" + PrimitiveArray.toString(PrimitiveArray.stripDashedMembers(a)));
//        }
//        if ( false ) {
//            ArrayList lines = new ArrayList();
//            lines.add("test");
//            lines.add("testing");
//            byte[] content = null;
//            General.showOutput("Result from fillContent: " + fillContent(lines));
//        }
//        if ( false ) {
//            // Benched clock time
//            // 210 ms on Windows XP PIV           3.0 GHz java 1.4.2_04-b05 (WHELK)
//            // 293 ms on Linux      PIV           2.7 GHz java 1.4.2_01-b06 (HALFBEAK)
//            //1449 ms on Solarix    UltraSPARC-II 0.4 GHz java 1.4.2_04-b05 (MANATEE)
//            // Looks like it is roughly proportional to CPU clock speed! The fastest
//            // options to the JVM were choicen. E.g. -d64 over -d32 caused a time
//            // taken reduction from 1917 ms with 32 bit.
//            byte[] junk = new byte[1000*1000*10];
//            General.showOutput("Result from getMD5Sum: " + getMD5SumString(junk).length());
//        }
    }

    public static void add(FloatArrayList fal, float[] l) {
        for (int i=0;i<l.length;i++) {
            fal.add(l[i]);
        }
    }

    /** For each element returns the max in the result array.
     *funny thing is that it will take into consideration if a float is
     *considered to be defined first.
     */
    public static float[] getMax(float[] a, float[] b) {
        if ( a.length != b.length ) {
            General.showError("arrays need to be of same size but were not:");
            General.showError("a:" + toString(a));
            General.showError("b:" + toString(b));
            return null;
        }
        float[] result = new float[a.length];
        for (int i=0;i<a.length;i++) {
            if ( Defs.isNull(a[i]) ) {
                result[i] = b[i];
            } else if ( Defs.isNull(b[i]) ) {
                result[i] = a[i];
            } else {
                result[i] = Math.max(a[i],b[i]);
            }
        }
        return result;
    }

    public static void truncateElementsSet(BitSet set,
            int max) {
        int c=0;
        for (int i=set.nextSetBit(0);i>=0;i=set.nextSetBit(i+1)) {
            if ( set.get(i) ) {
                if ( c > max ) {
                    set.clear(i);
                }
                c++;
            }
        }
    }
}
