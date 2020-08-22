package Wattos.Soup.Constraint;

import Wattos.Soup.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Database.Indices.*;
import java.util.Comparator;
import cern.colt.list.*;

/**
 * Only returns 0 if all in comparable part of the list are the same
 *on both sides.
 *Assumes that the left/right are already sorted.
 * @author Jurgen F. Doreleijers
 */
public class ComparatorDC implements Comparator {
    
    public DistConstr dc;
    public IndexSortedInt indexMainAtom;
    
    /** Creates a new instance of ComparatorAtom */
    public ComparatorDC(DistConstr dc) {
        this.dc = dc;        
        // Assuming the list itself will not be changed during sorting caching it can make a big difference.
        indexMainAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_DC[  RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        if ( indexMainAtom == null ) {
            General.showCodeBug("Failed to get all indexes.");
        }
        
    }
    
    /** 
     *Returns equality even if the distance values and contributions are different between the
     *two distance constraints.
     *The compare is a little expensive. As it keeps creating and sorting new objects every time.
     *If this becomes a problem these sorted objects can easily be cached.
     */
    public int compare(Object o1, Object o2) {             
        
        IntArrayList atomRids1 = dc.distConstrAtom.getValueListBySortedIntIndex(             
            indexMainAtom,
            ((Integer) o1).intValue(), 
            Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
            null );
        IntArrayList atomRids2 = dc.distConstrAtom.getValueListBySortedIntIndex(             
            indexMainAtom,
            ((Integer) o2).intValue(), 
            Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
            null );
        /**
        General.showDebug(  "Before order lists: " + PrimitiveArray.toString( dcAtomRids1 ) + 
                                          " and: " + PrimitiveArray.toString( dcAtomRids2 ));
         */
        if ( ! dc.gumbo.atom.order(atomRids1) ) {
            General.showError("Failed to order atoms in list: " + PrimitiveArray.toString( atomRids1 ));
            return 0;
        }
        if ( ! dc.gumbo.atom.order(atomRids2) ) {
            General.showError("Failed to order atoms in list: " + PrimitiveArray.toString( atomRids2 ));
            return 0;
        }
        /**
        General.showDebug(  "Order atoms in list: " + PrimitiveArray.toString( dcAtomRids1 ) + 
                            " with atoms in: "      + PrimitiveArray.toString( dcAtomRids2 ));
         */
        return dc.gumbo.atom.compare( atomRids1, atomRids2, true );
    }
}
