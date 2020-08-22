/*
 * Logic.java
 *
 * Created on March 14, 2006, 10:24 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Utils.Wiskunde;

/**
 *
 * @author jurgen
 */
public class Logic {
    
    /** Creates a new instance of Logic */
    public Logic() {
    }
    
    /** Test order of given values depending on set boolean. 
     * <BR> if ( testSmaller ) return (v1 .LT. v2);  <BR> 
     *It is probably small enough for the compiler to 'inline' it.
     */
    public static boolean smallerThanOrLargerThan( boolean testSmaller, float v1, float v2 ) {
        if ( testSmaller ) {
            return (v1 < v2);
        }
        return (v1 > v2);
    }
}
