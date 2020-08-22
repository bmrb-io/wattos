/*
 * ComparatorAtom.java
 *
 * Created on September 29, 2003, 2:15 PM
 */

package Wattos.Soup.Comparator;

import java.util.Comparator;
import Wattos.Utils.ObjectIntPair;

/**
 * @author Jurgen F. Doreleijers
 */
public class ComparatorAtomPerModelMolResAtom implements Comparator {
    
    /**This comparator could be a bit faster perhaps when using an initializer with the relation
     *to cache and then just sort on an array with IntIntPair objects. O well, next time.
     */
    public ComparatorAtomPerModelMolResAtom() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        ObjectIntPair oip1= (ObjectIntPair) o1;        
        ObjectIntPair oip2= (ObjectIntPair) o2;        
        
        int[]       values_1    = (int[])  oip1.o;
        int[]       values_2    = (int[])  oip2.o;

        // Cache the int values so less array look ups are required.
        //General.showDebug("doing compare between rows: " + oip1.i + " and " + oip2.i);        
        
        // model
        int iv_1 = values_1[0];
        int iv_2 = values_2[0];
        //General.showDebug("Model: " + iv_1 + " and " + iv_2);        
        if ( iv_1 < iv_2 ) {
            return -1;
        } 
        if ( iv_1 > iv_2 ) {
            return 1;
        }
        // mol
        iv_1 = values_1[1];
        iv_2 = values_2[1];
        //General.showDebug("Mol  : " + iv_1 + " and " + iv_2);        
        if ( iv_1 < iv_2 ) {
            return -1;
        } 
        if ( iv_1 > iv_2 ) {
            return 1;
        }
        // residue
        iv_1 = values_1[2];
        iv_2 = values_2[2];
        //General.showDebug("Res  : " + iv_1 + " and " + iv_2);        
        if ( iv_1 < iv_2 ) {
            return -1;
        } 
        if ( iv_1 > iv_2 ) {
            return 1;
        }
        // atom
        iv_1 = values_1[3];
        iv_2 = values_2[3];
        //General.showDebug("Atom  : " + iv_1 + " and " + iv_2);        
        if ( iv_1 < iv_2 ) {
            return -1;
        } 
        if ( iv_1 > iv_2 ) {
            return 1;
        }
        return 0;
    }    
}
