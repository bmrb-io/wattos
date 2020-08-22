package Wattos.Soup.Constraint;

import Wattos.Soup.*;
import java.util.Comparator;
import cern.colt.list.*;

/**
 * Only returns 0 if all in comparable part of the list are the same
 *on both sides.
 *Assumes that the left/right are already sorted.
 * @author Jurgen F. Doreleijers
 */
public class ComparatorDCContribution implements Comparator {
    
    public Gumbo gumbo;
    /** Creates a new instance of ComparatorAtom */
    public ComparatorDCContribution(Gumbo gumbo) {
        this.gumbo = gumbo;       
    }
    
    
    /** Optimized for cases where the first atom is already different
     */
    public int compare(Object o1, Object o2) {               
        Object[] a1 = (Object[]) o1;
        Object[] a2 = (Object[]) o2;
        IntArrayList atomRid1    = (IntArrayList) a1[0];
        IntArrayList atomRid2    = (IntArrayList) a2[0];
        int status = gumbo.atom.compare( atomRid1, atomRid2 ); // try one first as an optimalization
        if ( status != 0 ) {
            return status;
        }
        atomRid1    = (IntArrayList) a1[1];
        atomRid2    = (IntArrayList) a2[1];
        return gumbo.atom.compare( atomRid1, atomRid2 );                
    }
}
