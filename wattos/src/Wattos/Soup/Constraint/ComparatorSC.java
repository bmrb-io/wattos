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
 * @author Jurgen F. Doreleijers
 */
public class ComparatorSC implements Comparator {
    
    public SimpleConstr sc;
    public IndexSortedInt indexMainAtom;
    
    public ComparatorSC(SimpleConstr sc) {
        this.sc = sc;        
        String sortColumnName = sc.ATTRIBUTE_SET_SUB_CLASS[RelationSet.RELATION_ID_COLUMN_NAME ];
//        General.showDebug("Using sort column: " + sortColumnName);
        // Assuming the list itself will not be changed during sorting caching it can make a big difference.
        indexMainAtom = (IndexSortedInt) sc.simpleConstrAtom.getIndex(
                sortColumnName, Index.INDEX_TYPE_SORTED);
        if ( indexMainAtom == null ) {
            General.showCodeBug("Failed to get all indexes.");
        }        
    }
    
    /** 
     *The compare is a little expensive. As it keeps creating and sorting new objects every time.
     *If this becomes a problem these sorted objects can easily be cached.
     */
    public int compare(Object o1, Object o2) {             
//        if ( indexMainAtom == null ) {
//            General.showCodeBug("In ComparatorSC index not set");
//        }
//        if ( sc == null ) {
//            General.showCodeBug("In sc not set");
//        }
        IntArrayList atomRids1 = sc.simpleConstrAtom.getValueListBySortedIntIndex(             
            indexMainAtom,
            ((Integer) o1).intValue(), 
            Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
            null );
        IntArrayList atomRids2 = sc.simpleConstrAtom.getValueListBySortedIntIndex(             
            indexMainAtom,
            ((Integer) o2).intValue(), 
            Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
            null );

//        General.showDebug(  "Before order lists: " + PrimitiveArray.toString( atomRids1 ) + 
//                                          " and: " + PrimitiveArray.toString( atomRids2 ));

        if ( ! sc.gumbo.atom.order(atomRids1) ) {
            General.showError("Failed to order atoms in list: " + PrimitiveArray.toString( atomRids1 ));
            return 0;
        }
        if ( ! sc.gumbo.atom.order(atomRids2) ) {
            General.showError("Failed to order atoms in list: " + PrimitiveArray.toString( atomRids2 ));
            return 0;
        }
//        General.showDebug(  "Order atoms in list: " + PrimitiveArray.toString( atomRids1 ) + 
//                            " with atoms in: "      + PrimitiveArray.toString( atomRids2 ));
        return sc.gumbo.atom.compare( atomRids1, atomRids2, true );
    }
}
