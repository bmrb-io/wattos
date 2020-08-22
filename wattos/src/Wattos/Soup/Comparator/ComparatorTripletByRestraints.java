/*
 * ComparatorTripletByRestraints.java
 *
 * Created on January 11, 2006, 10:39 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Soup.Comparator;

import Wattos.Soup.Constraint.Triplet;
import java.util.Comparator;

/**
 *
 * @author jurgen
 */
public class ComparatorTripletByRestraints implements Comparator {
    
    public ComparatorTripletByRestraints() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {  
        Triplet t1 = (Triplet) o1;
        Triplet t2 = (Triplet) o2;
        if ( t1.countRestraints != t2.countRestraints ) {
            if (t1.countRestraints < t2.countRestraints) {
                return -1;
            } else {
                return 1;
            }                
        }
        if ( t1.countRestraintsUniqueAssigned != t2.countRestraintsUniqueAssigned ) {
            if (t1.countRestraintsUniqueAssigned < t2.countRestraintsUniqueAssigned) {
                return -1;
            } else {
                return 1;
            }
        }
        return 0;
    }    
}
