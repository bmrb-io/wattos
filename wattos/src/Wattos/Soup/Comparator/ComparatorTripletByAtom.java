/*
 * ComparatorTripletByRestraints.java
 *
 * Created on January 11, 2006, 10:39 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Soup.Comparator;

import Wattos.Soup.Gumbo;
import Wattos.Soup.Constraint.Triplet;
import java.util.Comparator;

/**
 *
 * @author jurgen
 */
public class ComparatorTripletByAtom implements Comparator {
        
    public Gumbo gumbo;
    /** Creates a new instance of ComparatorAtom */
    public ComparatorTripletByAtom(Gumbo gumbo) {
        this.gumbo = gumbo;        
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {  
        Triplet t1 = (Triplet) o1;
        Triplet t2 = (Triplet) o2;
        int atomRid1 = t1.atomRids[0];
        int atomRid2 = t2.atomRids[0];
        return gumbo.atom.compare( atomRid1, atomRid2 );
    }    
}
