/*
 * Statistics.java
 *
 * Created on February 7, 2006, 1:36 PM
 *
 */
package Wattos.Utils.Wiskunde;

import Wattos.Database.*;
import Wattos.Utils.*;
import cern.colt.list.*;
import cern.jet.stat.*;
/**
 *Contains some wrappers for functionality in cern.jet.stat and some
 *convenience methods for doing statistics.
 *See cern.jet.stat package.
 * @author jurgen
 */
public class Statistics {
    
    /** Creates a new instance of Statistics */
    public Statistics() {
    }
    
    /** Simply returns the number of s.d. the value v is away from the average.
     *If sd is zero then return Defs.NULL_FLOAT without mentioning an error.
     */
    public static float getNumberOfStandardDeviation( float v, float av, float sd) {
        if ( Defs.isNull(sd) ) {
            General.showError("Fails to getNumberOfStandardDeviation with NULL value for sd.");
            return Defs.NULL_FLOAT;
        }
        if ( sd==0f ) {
//            General.showError("Fails to getNumberOfStandardDeviation with zero value for sd.");
            return Defs.NULL_FLOAT;
        }
        return (v - av) / sd;
    }
    
    /** convenience method
     */
    public static float getMax( float[] v ) {
        return PrimitiveArray.getMax(v);
    }
    /** convenience method
     */
    public static float[] getMinMax( float[] v ) {
        return PrimitiveArray.getMinMax(v);
    }
    /** convenience method
     */
    public static float getSum( float[] v ) {
        return PrimitiveArray.getSum(v);
    }
    
    /** Returns null on error. Returns the average and sd of a set of values when
     *there are more than 1 valid values. If there are no values then no average
     *can be calculated so the average returned will be Defs.NULL. Same if there
     *is only one value; no sd but Defs.NULL.
     *
     *Note that the method is not optimized for speed but uses an external api
     *for the actual calculation.
     */
    public static float[] getAvSd( float[] v) {
        float[] result = new float[2];
        result[0] = Defs.NULL_FLOAT;
        result[1] = Defs.NULL_FLOAT;
        if ( v.length == 0 ) {
            General.showDebug("No valid values so returning null values for av/sd" );
            return result;            
        }
        double[] d = PrimitiveArray.toDoubleArray(v);
        DoubleArrayList data = new DoubleArrayList(d);
        int size = v.length;
        double mean = (float) cern.jet.stat.Descriptive.mean(data);
        result[0] = (float) mean;
        
        if ( size > 1 ) {
            double sampleVariance = Descriptive.sampleVariance(data,mean);
            double sd = Descriptive.sampleStandardDeviation(size,sampleVariance);
            result[1] = (float) sd;
        }
        return result;            
    }

}
