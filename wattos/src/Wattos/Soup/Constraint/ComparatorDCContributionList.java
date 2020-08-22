package Wattos.Soup.Constraint;

import Wattos.Soup.*;
import Wattos.Utils.*;
import java.util.*;
import java.util.Comparator;
import cern.colt.list.*;

/**
 * Only returns 0 if all in comparable part of the list are the same
 *on both sides. Todo; remove if still not used.
 *Assumes that the left/right are already sorted.
 * @author Jurgen F. Doreleijers
 */
public class ComparatorDCContributionList implements Comparator {
    
    public Gumbo gumbo;
    public ComparatorDCContributionList(Gumbo gumbo) {
        this.gumbo = gumbo;        
    }
    
    public int compare(Object o1, Object o2) {
        if ( ! (o1 instanceof ArrayList)) {
            General.showError("Failed to ComparatorDCContributionList.compare because element is not of instance ArrrayList but is: " + o1.getClass().getName());
            return 0;
        }
        if ( ! (o2 instanceof ArrayList)) {
            General.showError("Failed to ComparatorDCContributionList.compare because element is not of instance ArrrayList but is: " + o1.getClass().getName());
            return 0;
        }
        ArrayList al1 = (ArrayList) o1;
        ArrayList al2 = (ArrayList) o2;
        for (int i=0;i<al1.size();i++ ) {
            Object[] a1 = (Object[]) al1.get(i);
            Object[] a2 = (Object[]) al2.get(i);
            for (int m=0;m<2;m++ ) { // loops over members
                int index = 0;
                if ( m == 1 ) {
                    index = 1;
                }
                IntArrayList atomRid1    = (IntArrayList) a1[index];
                IntArrayList atomRid2    = (IntArrayList) a2[index];
                int status = gumbo.atom.compare( atomRid1, atomRid2, true ); // compare all elements.
                if ( status != 0 ) { // They were different
                    return status;
                }
            }
        }
        // no diff found
        return 0;
    }
}
