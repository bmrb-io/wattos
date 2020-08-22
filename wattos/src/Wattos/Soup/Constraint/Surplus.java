/*
 * Surplus.java
 *
 * Created on February 3, 2004, 3:46 PM
 */
package Wattos.Soup.Constraint;

import java.util.*;
import java.io.*;
import Wattos.Soup.*;
import Wattos.Utils.*;
import Wattos.CloneWars.*;
import Wattos.Database.*;
import Wattos.Database.Indices.*;
import Wattos.Episode_II.Globals;
import Wattos.Star.NMRStar.*;
import Wattos.Star.*;
import cern.colt.list.*;

/**Code to find and remove impossible and redundant distance restraints.
 * @see <a href="dc_surplus.html">verbose document</a>
 * @author Jurgen F. Doreleijers
 */
public class Surplus {

    /** Default percentage (currently 5 %) to allow a couple more constraint not to be redundant.
     *For entry 1brv this brings the number or intra residual restraints from
     *26 (0%) to 18 (5%) out of 74.
     */
    public static final float THRESHOLD_REDUNDANCY_DEFAULT = 5f;

    UserInterface ui;
    DistConstr dc;
    DistConstrList dcList;
    Gumbo gumbo;
    PrintWriter printWriter = null;

    /** convenience variables that are arrays in distance constraints */
    public float[] upp_theo;
    public float[] low_theo;

    public String tagNameSurplusSf_category;
//    public String tagNameSurplusEntry_ID;
//    public String tagNameSurplusDC_surplus_ID;
    public String tagNameSurplusRedundancy_threshold_pct;
    public String tagNameSurplusUpdate_original_restraints;
    public String tagNameSurplusOnly_filter_fixed;
    public String tagNameSurplusAveraging_method;
    public String tagNameSurplusNumber_of_monomers_sum_average;
    public String tagNameSurplusRestraint_count;
    public String tagNameSurplusRestraint_exceptional_count;
    public String tagNameSurplusRestraint_double_count;
    public String tagNameSurplusRestraint_impossible_count;
    public String tagNameSurplusRestraint_fixed_count;
    public String tagNameSurplusRestraint_redundant_count;
    public String tagNameSurplusRestraint_surplus_count;
    public String tagNameSurplusRestraint_nonsurplus_count;
    public String tagNameSurplusDetails;
    public StarDictionary starDict;

    public static String explanation = null;
    static {
        int i=1;
        explanation = "\n" +
                "A detailed methodology description is available at:\n" +
                Globals.wattos_home_page+"/doc/Wattos/Soup/Constraint/dc_surplus.html\n" +
                "\n" +
                "Description of the tags in this list:\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Threshold percentage for determining redundant restraints.\n" +
                "*  "+i+++" * Should surplus be removed.\n" +
                "*  "+i+++" * Should only the fixed restraints be considered surplus.\n" +
                "*  "+i+++" * Method for averaging distances (center, R^-6 or sum).\n" +
                "*  "+i+++" * In case of sum averaging the monomer count.\n" +
                "*  "+i+++" * Number of restraints in restraint list.           Set U\n" +
                "*  "+i+++" * Number of surplus restraints.                     Set S = E u D u I u F u R \n" +
                "*  "+i+++" * Non-surplus restraints.                           Set N = U - S\n" +
                "*  "+i+++" * Exceptional restraints, i.e. with an unknown atom.Set E\n" +
                "*  "+i+++" * Double restraints.                                Set D\n" +
                "*  "+i+++" * Impossible restraints.                            Set I\n" +
                "*  "+i+++" * Fixed restraints.                                 Set F\n" +
                "*  "+i+++" * Redundant intraresidual restraints.               Set R\n" +
                "*  "+i+++" * This tag";
    }

    /** Creates a new instance of Surplus */
    public Surplus(UserInterface ui) {
        this.ui = ui;
        dc = ui.constr.dc;
        dcList = ui.constr.dcList;
        gumbo = ui.gumbo;
        starDict = ui.wattosLib.starDictionary;

        if ( ! dc.mainRelation.containsColumn( DistConstr.DEFAULT_UPP_THEO ) ) {
            if ( ! dc.mainRelation.insertColumn(-1, DistConstr.DEFAULT_UPP_THEO, RelationSet.DATA_TYPE_FLOAT, null)) {
                General.showError("Failed to insert new column into dc for upper limits theo");
            }
        }
        if ( ! dc.mainRelation.containsColumn( DistConstr.DEFAULT_LOW_THEO ) ) {
            if ( ! dc.mainRelation.insertColumn(-1, DistConstr.DEFAULT_LOW_THEO, RelationSet.DATA_TYPE_FLOAT, null)) {
                General.showError("Failed to insert new column into dc for upper limits theo");
            }
        }
        if (!resetConvenienceVariables()) {
            General.showError("Failed to resetConvenienceVariables; proceed at your own risk.");
        }
    }


    /** Will mark a selection of double constraints like: HA-HA and many other types.
     * See documentation in dc_surplus.html
     */
    private boolean setSelectionDoubles(BitSet todo, boolean verbosity) {
        if ( todo.cardinality() == 0 ) {
            return true;
        }
        BitSet stillTodo = (BitSet) todo.clone();

        if ( ! setSelectionDoubles_Type_1(stillTodo, verbosity) ) {
            General.showError("Failed to set doubles of type 1");
            return false;
        }
        stillTodo.xor( dc.mainRelation.getColumnBit( "DSet" ) );

        if ( ! setSelectionDoubles_Type_2(stillTodo, verbosity) ) {
            General.showError("Failed to set doubles of type 2");
            return false;
        }
        stillTodo.xor( dc.mainRelation.getColumnBit( "DSet" ) );

//        General.showWarning("Skipping setSelectionDoubles_Type_3");
        if ( ! setSelectionDoubles_Type_3(stillTodo, verbosity) ) {
            General.showError("Failed to set doubles of type 3");
            return false;
        }
        stillTodo.xor( dc.mainRelation.getColumnBit( "DSet" ) );
        return true;
    }

    /** Will return a selection of double constraints like:
     * HA-HA. Note that also restraints like HA,HB2-HA are considered double.
     */
    private boolean setSelectionDoubles_Type_1(BitSet todo, boolean verbosity) {

        if ( todo.cardinality() == 0 ) {
            return true;
        }
        boolean constraintToBeSkipped = false;
        if ( verbosity ) {
            General.showDebug("Getting a selection of constraints that are double with the remaining" );
        }
        BitSet DSet =  dc.mainRelation.getColumnBit( "DSet" );

        IndexSortedInt indexMembAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) dc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) dc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);

        if ( indexMembAtom == null ||
            indexNodeMemb == null ||
            indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }
        // FOR EACH CONSTRAINT
        for (int currentDCId = todo.nextSetBit(0);currentDCId>=0;currentDCId = todo.nextSetBit(currentDCId+1)) {
            Integer currentDCIdInteger = new Integer(currentDCId);
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            if ( dcNodes == null ) {
                General.showCodeBug("Failed to get all nodes for currentDCId: " + currentDCId );
                return false;
            }
            constraintToBeSkipped = false;
            // FOR EACH NODE
            for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodes.size(); currentDCNodeBatchId++) {
                int currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId ); // quick enough?;-)
                int logOp = dc.logicalOp[currentDCNodeId];
                if ( ! Defs.isNull( logOp ) ) {
                    if ( logOp != DistConstr.DEFAULT_LOGICAL_OPERATION_ID_OR ) {
                        General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]");
                        constraintToBeSkipped = true;
                        break;
                    }
                    continue;
                }
                IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                Index.LIST_TYPE_INT_ARRAY_LIST, null);
                if ( dcMembs.size() != 2 ) {
                    General.showError("Are we using a number different than 2 as the number of members in a node for constraint rid: " + currentDCId);
                    return false;
                }
                int currentDCMembId  = dcMembs.get(0);
                int currentDCMembIdJ = dcMembs.get(1);
                //General.showDebug("Working from member with RID: " + currentDCMembId);

                IntArrayList atomRids = dc.distConstrAtom.getValueListBySortedIntIndex(
                    indexMembAtom,
                    currentDCMembId,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
                    null );

                //General.showDebug("Found the following rids of atoms in member: " + PrimitiveArray.toString( atomRids ));
                if ( atomRids.size() < 1 ) {
                    General.showError("Didn't find a single atom for member I in constraint: " + dc.toString( currentDCId, false, true ));
                    return false;
                }
                //General.showDebug("Working from member with RID: " + currentDCMembIdJ);

                // Very important optimalization to use a cached index even though the relation changes
                // the index is only supposed to pick up the old/unchanged dc atom record and it will!
                // Speeds up by a factor of 2 overall! If not used the index will be regenerated for each
                // constraint.
                IntArrayList atomRidsJ = dc.distConstrAtom.getValueListBySortedIntIndex(
                indexMembAtom,
                currentDCMembIdJ,
                Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
                null );
                //General.showDebug("Found the following rids of atoms in member J: " + PrimitiveArray.toString( atomRidsJ ));
                if ( atomRidsJ.size() < 1 ) {
                    General.showError("Didn't find a single atom for member J in constraint: " + dc.toString( currentDCId, false, true ));
                    return false;
                }
                if ( PrimitiveArray.hasIntersection(atomRids, atomRidsJ) ) {
                    DSet.set( currentDCId ); // setting bit to indicate this constraint is double.
                    //General.showDebug("Found the same atoms between members A: " + PrimitiveArray.toString( atomRids));
                    //General.showDebug("and members B                         : " + PrimitiveArray.toString( atomRidsJ));
                    break;
                }
                if ( DSet.get( currentDCId ) ) {
                    break;
                }
            } // end of node loop
            if ( constraintToBeSkipped ) { // Not important now but if code got extended this would be crucial
                continue;
            }
            // extending code here.
        } // end of loop per constraint
        return true;
    }

    /** Will note and simplify double constraints.
     * In different nodes of the same constraint.
     * Example:
     * (HA) (HB) or
     * (HA or HD) (HB or HC)
     * will become:
     * (HA or HD) (HB or HC)
     * because the first 'contribution is contained within the second.
     *
     * The implementation uses the following ideas.
     * - Create a list of contributions with one atom on each side.
     * - Create a matrix representation of the list using the class DCContributionMatrix
     * - Simplify the contributions by taking only the non-redundant together.
     * - The whole constraint will be rewritten (unless it's between
     * two atoms perhaps) in memory.
     *
     * Only or-ed logical constraints can be handled, others will simply be skipped!
     */
    private boolean setSelectionDoubles_Type_2(BitSet todo, boolean verbosity) {
        if ( todo.cardinality() == 0 ) {
            return true;
        }
//        boolean constraintToBeSkipped = false;
        IntArrayList dcNodesToBeRemoved = new IntArrayList( todo.length()*3 ); // 3 is a conservative estimate, might be more.

        //General.showDebug("Starting routine: setSelectionDoubles_Type_2");
        IndexSortedInt indexMembAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) dc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) dc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMembAtom == null ||
        indexNodeMemb == null ||
        indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }
        // FOR EACH CONSTRAINT
        for (int currentDCId = todo.nextSetBit(0);currentDCId>=0;currentDCId = todo.nextSetBit(currentDCId+1)) {
            //General.showDebug("Doing constraint with rid: " + currentDCId );
            Integer currentDCIdInteger = new Integer(currentDCId);
//            int currentDCListId = dc.dcListIdMain[   currentDCId ];
//            int currentEntryId  = dc.entryIdMain[    currentDCId ];

            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            ArrayList contributions = dc.getContributions(currentDCId, indexMembAtom, indexNodeMemb, indexMainNode, false);
            if ( contributions == null ) {
                General.showCodeBug("getContributions failed in setSelectionDoubles_Type_2");
                return false;
            }

            DistConstr.showContributions( contributions );
            //General.showDebug("Doing rewrite to standardized and compact form of contributions");
            DCContributionMatrix dCContributionMatrix = new DCContributionMatrix( contributions, gumbo );
            //General.showDebug("Initialized DCContributionMatrix");
            contributions = dCContributionMatrix.getReorderedContributions();
            if ( contributions == null ) {
                General.showCodeBug("getReorderedContributions failed");
                return false;
            }
            //General.showDebug("Done with getReorderedContributions");
            if ( ! dc.setContributions(currentDCId,contributions,dcNodes)) {
                General.showCodeBug("setContributions failed");
                return false;
            }
            dcNodesToBeRemoved.addAllOf( dcNodes );
        } // end of loop per constraint

        // Remove old nodes by the bunch so we save time; mainly because index doesn't need to be regenerated for each constraints.
        int dcNodeSizeOld = dc.distConstrNode.sizeRows;
        if ( ! dc.distConstrNode.removeRowsCascading(dcNodesToBeRemoved, false)) {
            General.showError("Failed to do cascading remove on dc nodes");
            return false;
        }
        if ( dc.distConstrNode.sizeRows >= dcNodeSizeOld ) {
            General.showError("After cascading remove expected some removes (new/old): " + dc.distConstrNode.sizeRows +"/" + dcNodeSizeOld);
            return false;
        }
        //General.showDebug("Cascading remove of rows numbered:: " + (dcNodeSizeOld - distConstrNode.sizeRows) );
        return true;
    }


    /** Look for identical constraints and retain only the tightest of bounds
     * Will combine a constraint HA-HB with c_low, c_high (see definitions in dc_surplus.html)
     * of 3, 5 Ang. with a constraint HA-HB with c_low, c_high of 4, 6 to a constraint having the tightest bounds
     * (representing all information in both constraints) i.e. HA-HB with c_low, c_high of 4, 5.
     * Will not combine doubles that aren't exact matches on the atoms involved.
     *
     * A bug in the design of the algorithm is that it will combine restraints:
     * (HA,HX)-HB and HA-(HB,HX); so it doesn't matter where in the restraint
     * the atoms are located.
     * Method is private because it assumes the constraints are already 'sorted'.
     * Method marks the doubles in the bitset DSet.
     * Method will 'reorder' the constraints because they might not be when going into the method.
     */
    private boolean setSelectionDoubles_Type_3(BitSet todo, boolean verbosity) {

        if ( todo.cardinality() == 0 ) {
            return true;
        }
        if ( ! dc.sortAll(todo,0)) {
            General.showError("Failed to dc.sortAll in setSelectionDoubles_Type_3.");
            return false;
        }
        if ( ! resetConvenienceVariables()) {
            General.showError("Failed to resetConvenienceVariables in setSelectionDoubles_Type_3.");
            return false;
        }

        int countRedundant = 0;

        BitSet DSet =  dc.mainRelation.getColumnBit( "DSet" );
        if ( DSet == null ) {
            General.showError("Failed to get DSet column on the distance constraints.");
            return false;
        }

        int[] orderMap = dc.mainRelation.getRowOrderMap( Relation.DEFAULT_ATTRIBUTE_ORDER_ID, todo );
        if ( orderMap == null ) {
            General.showError("Failed to get the row order sorted out; so giving up on setSelectionDoubles_Type_3.");
            return false;
        }
        if ( orderMap.length == 0 ) {
            General.showError("Order map lenght was null.");
            return false;
        }
//        General.showDebug("*** Row order map on constraints is: " + PrimitiveArray.toString( orderMap ));

        // Go through the ordered constraints and only for those that have the same atoms collapse
        // the constraints by marking the encountered constraint as double and using it's
        // bound(s) if they're more restrictive.
        int dcRidPrev = orderMap[0];
        BitSet dcNonLogicalNodeSetPrev = dc.getdCNodeSetWithoutLogicalOpRid(dcRidPrev);
        int dcFirstNonLogicalNodeRidPrev = dcNonLogicalNodeSetPrev.nextSetBit(0);
        if ( dcFirstNonLogicalNodeRidPrev < 0 ) {
            General.showError("Failed dc.getdCNodeFirstWithoutLogicalOpRid(dcRidPrev)");
            return false;
        }
        ComparatorDC comparatorDC = new ComparatorDC( dc );
        for (int r=1; r<orderMap.length;r++)  {
            int dcRid = orderMap[r];
            BitSet dcNonLogicalNodeSet = dc.getdCNodeSetWithoutLogicalOpRid(dcRid);
            int dcFirstNonLogicalNodeRid = dcNonLogicalNodeSet.nextSetBit(0);
            if ( dcFirstNonLogicalNodeRid < 0 ) {
                General.showError("Failed dc.getdCNodeFirstWithoutLogicalOpRid(dcRid)");
                return false;
            }
            int compare = comparatorDC.compare( new Integer( dcRid ), new Integer( dcRidPrev ));
//            General.showDebug("----Comparing: constraints: " + dcRidPrev + " with: " + dcRid + " gives: " + compare);
            if ( compare == 0 ) {
                if ( DSet.get( dcRid ) ) {
                    General.showError("Constraint was already set as redundant before this routine: " + dcRid );
                    return false;
                }
//                General.showDebug("Collapsing restraints together (B): " + dcRid + " with previous (A): " + dcRidPrev);
//                General.showDebug(dc.toString(dcRid));
//                General.showDebug(dc.toString(dcRidPrev));
                DSet.set( dcRid ); // Mark as redundant.
                // Update the previous constraint with the most restrictive bound if needed.
                // The target will be nilled if it's out of bounds.
                float lowBound = DistConstr.getStrictLowBound( dc.lowBound[ dcFirstNonLogicalNodeRidPrev ],
                                                       dc.lowBound[ dcFirstNonLogicalNodeRid     ] );
                float uppBound = DistConstr.getStrictUppBound( dc.uppBound[ dcFirstNonLogicalNodeRidPrev ],
                                                       dc.uppBound[ dcFirstNonLogicalNodeRid     ] );
                float target = dc.target[dcFirstNonLogicalNodeRidPrev];

                if ( (target < lowBound) ||
                     (target > uppBound) ) {
                    target = Defs.NULL_FLOAT;
                }
                for (int dcNodeRid=dcNonLogicalNodeSetPrev.nextSetBit(0);dcNodeRid>=0;dcNodeRid=dcNonLogicalNodeSetPrev.nextSetBit(dcNodeRid+1)) {
                    dc.lowBound[dcNodeRid] = lowBound;
                    dc.uppBound[dcNodeRid] = uppBound;
                    dc.target[dcNodeRid]   = target;
                }
//                General.showDebug("Updated restraint looks like: ");
//                General.showDebug(dc.toString(dcRidPrev));
                countRedundant++;
            } else { // end of identity found.
                dcRidPrev = dcRid;
                dcNonLogicalNodeSetPrev      = dcNonLogicalNodeSet;
                dcFirstNonLogicalNodeRidPrev = dcFirstNonLogicalNodeRid;
            }
        }
        General.showDebug("Found redundant in setSelectionDoubles_Type_3: " + countRedundant );
        return true;
    }

    /** Will mark a selection of constraints that are impossible OR fixed OR redundant with the molecular topology.
     * See documentation.
     * Skips constraints for which not all atoms are known.
     *If updateOriginal is set then the redundant bounds will be reset to absent.
     */
    private boolean setSelectionImpFixRed(
            BitSet todo,
            float thresholdRedundancy,
            boolean onlyFilterFixed,
            int avg_method,
            int numberMonomers,
            boolean verbosity,
            boolean updateOriginal) {

        General.showDebug("Now in setSelectionImpFixRed");

        if ( todo.cardinality() == 0 ) {
            return true;
        }
//        boolean constraintToBeSkipped;

        BitSet ISet = dc.mainRelation.getColumnBit( "ISet" );
        BitSet FSet = dc.mainRelation.getColumnBit( "FSet" );
        BitSet RSet = dc.mainRelation.getColumnBit( "RSet" );

        // Split up the distances per entry as we will use a different theoretical
        // maximum distance for each.
        String columnLabelEntry = Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ];
        String columnLabelModel = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[    RelationSet.RELATION_ID_COLUMN_NAME ];
        BitSet uniqueEntryDcRidSet = SQLSelect.getDistinct(dc.dbms, dc.mainRelation, columnLabelEntry, todo );
        if ( uniqueEntryDcRidSet == null || uniqueEntryDcRidSet.cardinality() < 1 ) {
            General.showError("Failed to get any a non-empty set of unique entries for the constraints to do");
            return false;
        }

        IndexSortedInt indexEntryMain  = (IndexSortedInt)             dc.mainRelation.getIndex(columnLabelEntry, Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexEntryModel = (IndexSortedInt) gumbo.model.mainRelation.getIndex(columnLabelEntry, Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexModelAtom  = (IndexSortedInt)  gumbo.atom.mainRelation.getIndex(columnLabelModel, Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMembAtom   = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb   = (IndexSortedInt) dc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode   = (IndexSortedInt) dc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);

        if ( indexEntryMain == null ||
        indexEntryModel == null ||
        indexModelAtom == null ||
        indexMembAtom == null ||
        indexNodeMemb == null ||
        indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }

        /** Usually this loop will only execute once */
        for ( int uniqueEntryDcRid = uniqueEntryDcRidSet.nextSetBit(0); uniqueEntryDcRid>=0; uniqueEntryDcRid = uniqueEntryDcRidSet.nextSetBit( uniqueEntryDcRid + 1)) {
            int currentEntryRid = dc.entryIdMain[ uniqueEntryDcRid ];
            BitSet dcConstrTodo = (BitSet) indexEntryMain.getRidList( new Integer( currentEntryRid ), Index.LIST_TYPE_BITSET );
            if ( dcConstrTodo == null || dcConstrTodo.cardinality() < 1 ) {
                General.showError("Failed to get a non-empty set of unique constraints to do");
                return false;
            }
            dcConstrTodo.and( todo ); // do only those that intersect with the todo set.
            if ( dcConstrTodo.cardinality() < 1 ) {
                // This is a real error as we only selected entries for which we knew had distance constraints todo.
                General.showError("Failed to get a non-empty set of unique distance constraints in 1 entry after doing -AND- with the todo set.");
                return false;
            }

            /** Get an estimate for the largest possible distance within one model.
             * This could be wrong in pathological cases like a misfolded/collapsed model
             * but if the model is somewhat reasonable this gives a nice upper limit estimate.
             */
            float diameter = Model.DEFAULT_DIAMETER;
            BitSet uniqueModels = (BitSet) indexEntryModel.getRidList( new Integer( currentEntryRid ), Index.LIST_TYPE_BITSET );
            if ( uniqueModels == null || uniqueModels.cardinality() < 1 ) {
                General.showWarning("Failed to get any a non-empty set of unique models to analyze for diameter. Assuming it's big: " + Model.DEFAULT_DIAMETER);
            } else {
                int currentModelRid = uniqueModels.nextSetBit(0); // just pick the first one.
                BitSet atomsInModel = (BitSet) indexModelAtom.getRidList( new Integer( currentModelRid ), Index.LIST_TYPE_BITSET );
                if ( atomsInModel == null || atomsInModel.cardinality() < 1 ) {
                    General.showWarning("Failed to get any a non-empty set of unique atoms to analyze for diameter. Assuming it's big: " + Model.DEFAULT_DIAMETER);
                } else {
                    diameter = gumbo.atom.getDiameter( atomsInModel );
                    if ( Defs.isNull( diameter )) {
                        General.showWarning("Failed to get diameter for atoms in model. Assuming it's big: " + Model.DEFAULT_DIAMETER );
                        diameter = Model.DEFAULT_DIAMETER;
                    }
                    if ( diameter < Chemistry.ELEMENT_RADIUS_HYDROGEN ) {
                        General.showError("The diameter of the molecule is found to be smaller than the radius of a hydrognen atom; something must be wrong: " + diameter);
                        return false;
                    }
                }
            }
//            General.showDebug("Using an estimated maximum diameter of the model of: " + diameter );

            // FOR EACH CONSTRAINT in ENTRY
            boolean atomFound = true; // signals at least one atom could not be found when 'false'
            for (int currentDCId = dcConstrTodo.nextSetBit(0);currentDCId>=0;currentDCId = dcConstrTodo.nextSetBit(currentDCId+1)) {
//                General.showDebug("Doing constraint with rid: " + currentDCId);
//                General.showDebug(dc.toString(currentDCId));
                Integer currentDCIdInteger = new Integer(currentDCId);
//                int currentDCListId= dc.dcListIdMain[    currentDCId ];

                if ( Defs.isNull( avg_method ) ) {
                    General.showError("No averaging method set; assuming default averaging method and monomer count: " + DistConstrList.DEFAULT_AVERAGING_METHOD_NAMES[ DistConstrList.DEFAULT_AVERAGING_METHOD ]);
                    return false;
                }

                IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
                if ( dcNodes == null || dcNodes.size() < 1 ) {
                    General.showError("Failed to get a list of dc nodes.");
                    return false;
                }
                int dcNodesSize = dcNodes.size(); // optimalization.
//                constraintToBeSkipped = false;
                ArrayList atomsInvolved = new ArrayList();
                // FOR EACH NODE
                for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodesSize; currentDCNodeBatchId++) {
                    int currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId );
                    int logOp = dc.logicalOp[currentDCNodeId];
                    if ( ! Defs.isNull( logOp ) ) {
                        if ( logOp != DistConstr.DEFAULT_LOGICAL_OPERATION_ID_OR ) {
                            General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]");
//                            constraintToBeSkipped = true;
                            break;
                        }
                        continue;
                    }
                    IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    if ( dcMembs.size() != 2 ) {
                        General.showError("Are we using a number different than 2 as the number of members in a node? Can't calculte distance for it yet.");
                        return false;
                    }
                    //General.showDebug("Found the following rids of members in constraint node (" + currentDCNodeId + "): " + PrimitiveArray.toString( dcMembs ));

                    IntArrayList dcAtomsA = (IntArrayList) indexMembAtom.getRidList(  new Integer(dcMembs.get(0)),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    IntArrayList dcAtomsB = (IntArrayList) indexMembAtom.getRidList(  new Integer(dcMembs.get(1)),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    //General.showDebug("Found the following rids of dc atoms in member A: " + PrimitiveArray.toString( dcAtomsA ));
                    //General.showDebug("Found the following rids of dc atoms in member B: " + PrimitiveArray.toString( dcAtomsB ));
                    // Get the real atom ids into a new list.
                    IntArrayList atomRidsA = new IntArrayList(dcAtomsA.size());
                    IntArrayList atomRidsB = new IntArrayList(dcAtomsB.size());
                    atomRidsA.setSize( dcAtomsA.size() );
                    atomRidsB.setSize( dcAtomsB.size() );
                    for (int i=0;i<dcAtomsA.size();i++) {
                        int rid = dc.atomIdAtom[ dcAtomsA.getQuick(i) ];
                        if ( Defs.isNull( rid )) {
                            General.showWarning("Failed to find atom; skipping constraint");
                            atomFound = false;
                            break;
                        }
                        atomRidsA.setQuick( i, rid );
                    }
                    for (int i=0;i<dcAtomsB.size();i++) {
                        int rid = dc.atomIdAtom[ dcAtomsB.getQuick(i) ];
                        if ( Defs.isNull( rid )) {
                            General.showWarning("Failed to find atom; skipping constraint");
                            atomFound = false;
                            break;
                        }
                        atomRidsB.setQuick( i, rid );
                    }
                    if ( ! atomFound ) {
                        break;
                    }
                    //General.showDebug("Found the following rids of atoms in member A: " + PrimitiveArray.toString( atomRidsA ));
                    //General.showDebug("Found the following rids of atoms in member B: " + PrimitiveArray.toString( atomRidsB ));
                    if ( atomRidsA.size() < 1 ) {
                        General.showError("Didn't find a single atom for A member in constraint");
                        return false;
                    }
                    if ( atomRidsB.size() < 1 ) {
                        General.showError("Didn't find a single atom for B member in constraint");
                        return false;
                    }
                    atomsInvolved.add( new IntArrayList[] { atomRidsA, atomRidsB } );
                } // end of loop for each node
                if ( ! atomFound ) {
                    General.showWarning("Failed to find all atoms in constraint: " + currentDCId + "That should have been recorded before. Skipping constraint.");
                    continue; // continue with other constraints.
                }

                float lowTheo = Defs.NULL_FLOAT;
                float uppTheo = Defs.NULL_FLOAT;
                boolean lowTheoExists = true;
                boolean uppTheoExists = true;
                float[] distTheo = calcDistanceTheo( atomsInvolved, avg_method, numberMonomers, diameter );
                if ( distTheo == null ) {
                    General.showWarning("Failed to calculate the theoretical distances for constraint: " + currentDCId );
                    lowTheoExists = false;
                    uppTheoExists = false;
                }  else {
                    // if an array is returned the values in it will be valid.
                    lowTheo = distTheo[0];
                    uppTheo = distTheo[1];
                    low_theo[ currentDCId ] = lowTheo;
                    upp_theo[ currentDCId ] = uppTheo;
                    if ( Defs.isNull( lowTheo ) ) {
                        General.showCodeBug("found theo distances but low is null; that's not allowed");
                        return false;
                    }
                }
//                General.showDebug("**** Found theo dist low: " + lowTheo + ", upp: " + uppTheo + " and they exist: " + lowTheoExists + ", and " + uppTheoExists);

                // cache the values
                int firstWithDistDCNodeId = dcNodes.getQuick(0);
                int logOp = dc.logicalOp[firstWithDistDCNodeId];
                if ( ! Defs.isNull( logOp ) ) { // It's a logical node instead of a node with distance info.
                    if ( dcNodes.size() < 3 ) {
                        General.showError("Expected at least 3 nodes in a restraint with a logical node but there are only: " + dcNodes.size());
                        return false;
                    }
                    firstWithDistDCNodeId = dcNodes.getQuick(1); // And the assumption is that they are all the same for OR-ed constraints.
                }

                float lowCons = dc.lowBound[firstWithDistDCNodeId];
                float tarCons = dc.target[  firstWithDistDCNodeId];
                float uppCons = dc.uppBound[firstWithDistDCNodeId];

//                General.showDebug("**** Found first const dist low: " + lowCons + " target: " + tarCons + ", upp: " + uppCons);

                boolean lowConsExists = true;
                boolean uppConsExists = true;
                boolean tarConsExists = true;  // Simplifies the code for now.
                if ( Defs.isNull( lowCons )) lowConsExists = false;
                if ( Defs.isNull( uppCons )) uppConsExists = false;
                if ( Defs.isNull( tarCons )) tarConsExists = false;
                if ( lowCons <= RedundantLib.LOWER_DISTANCE_MINIMUM ) {
//                    General.showDebug("Found constraint distance below or equal to RedundantLib.LOWER_DISTANCE_MINIMUM; ignoring lower limit.");
                    lowConsExists = false;
                }

                // No harm in executing the next 3 ifs when the cons don't exist.
                if ( lowConsExists && (lowCons <= Chemistry.smallestBondEver )) {
//                    General.showDebug("-T- lowCons <= Chemistry.smallestBondEver");
                    lowConsExists = false;
                }
                // could be refined by analyzing the element types and applying averaging method.
                if ( tarConsExists &&  // code is explicit but redundant
                    ((tarCons < Chemistry.smallestBondEver) ||
                     (tarCons > diameter ))) {
//                    General.showDebug("-T- (tarCons < Chemistry.smallestBondEver) || (tarCons > diameter )");
                    tarConsExists = false;
                }
                if ( uppConsExists && (uppCons > diameter) ) {
//                    General.showDebug("-T- uppCons > diameter");
                    uppConsExists = false;
                }

                // IMPOSSIBLE CHECKS (one violation enough to qualify)
                // some checks are left to the compiler to optimize...for now.
                boolean isImpossible = false;
                if (
                ( tarConsExists && lowTheoExists && (tarCons < lowTheo))      ||
                ( tarConsExists && uppTheoExists && (tarCons > uppTheo))  ) {
//                    General.showDebug("-T- tarCons < lowTheo or tarCons > uppTheo");
                    tarConsExists = false;
                }
//                General.showDebug("-T- constraint target exists: " + tarConsExists);
                if ( ((!lowConsExists) && (!uppConsExists) && (!tarConsExists) )) {
//                    General.showDebug("-1- No lower, upper, or target distance exist");
                    isImpossible = true;
                } else if (
                ( lowConsExists && uppTheoExists && (lowCons > uppTheo))      ||
                ( uppConsExists && lowTheoExists && (uppCons < lowTheo))
                ) {
//                    General.showDebug("-2- lowCons > uppTheo or uppCons < lowTheo");
                    isImpossible = true;
                } else if ( uppConsExists && lowConsExists && (uppCons < lowCons)) {
//                    General.showDebug("-3- uppCons < lowCons");
                    isImpossible = true;
                } else if (
                ( tarConsExists && lowConsExists && (tarCons < lowCons))      ||
                ( tarConsExists && uppConsExists && (tarCons > uppCons))  ) {
//                    General.showDebug("-4- tarCons < lowCons or tarCons > uppCons");
                    isImpossible = true;
                }
                if ( isImpossible ) {
                    if ( ! onlyFilterFixed ) {
//                        General.showDebug("Found an impossible constraint.");
                        ISet.set( currentDCId );
                    }
                    continue;
                }

                // FIXED CHECKS
                if ( lowTheoExists && uppTheoExists && ( lowTheo == uppTheo )) { // Exact comparsion should work as they're read from same ascii values. but check...
//                    General.showDebug("Found a fixed constraint.");
                    FSet.set( currentDCId );
                    continue;
                }

                // REDUNDANCY CHECKS
                /** Needs to be redundant on all accounts in contrast to the impossible checks.*/
                boolean lowRedundant = ! lowConsExists; // consider a distance redundant if it doesn't exist.
                boolean uppRedundant = ! uppConsExists;
                boolean tarRedundant = ! tarConsExists;
                float lowTheoCorrection = lowTheo * thresholdRedundancy / 100f;
                float uppTheoCorrection = uppTheo * thresholdRedundancy / 100f;
//                General.showDebug("lowTheoCorrection, uppTheoCorrection: " + lowTheoCorrection + ", " + uppTheoCorrection );
                if ( lowConsExists && lowTheoExists && (lowCons <= ( lowTheo - lowTheoCorrection) ) ) {
                    lowRedundant = true;
                }
                if ( uppConsExists && uppTheoExists && (uppCons >= ( uppTheo + uppTheoCorrection) ) ) {
                    uppRedundant = true;
                }
                // Next if is about considering the target constraint as redundant; this might need updating?
                if ( tarConsExists && (
                    ( uppTheoExists && (tarCons > (uppTheo + uppTheoCorrection))) ||
                    ( lowTheoExists && (tarCons < lowTheo))
                    )) {
                    tarRedundant = true;
                }

//                General.showDebug("lowRedundant, uppRedundant, tarRedundant: " + lowRedundant + ", " + uppRedundant + ", " + tarRedundant );

                if ( lowRedundant && uppRedundant && tarRedundant && (!onlyFilterFixed) ) {
//                    General.showDebug("Redundant distance found.");
                    RSet.set( currentDCId );
                    continue;
                }
                /** Consider changing the original data for some cases. */
                if ( updateOriginal ) {
                    if ( lowRedundant ) {
                        PrimitiveArray.setValueByRids( dc.lowBound, dcNodes, Defs.NULL_FLOAT );
                    }
                    if ( tarRedundant ) {
                        PrimitiveArray.setValueByRids( dc.target,   dcNodes, Defs.NULL_FLOAT );
                    }
                    if ( uppRedundant ) {
                        PrimitiveArray.setValueByRids( dc.uppBound, dcNodes, Defs.NULL_FLOAT );
                    }
                }
            } // end of loop per constraint
        }
        return true;
    }


    /** Will combine different categories of distance constraints as surplus.
     * Will only operate on constraints todo. Will simplify constraints
     * if usefull. E.g. HA-(HB2 or HB2) will become HA-HB2.
     * See package.html documentation!
     * Notes:
     *that it will only write dc lists if requested.
     *only the dcs that are the same entry as the first dc will be written.
     *returns the surplus selection which doesn't exist anymore if selected
     *to remove it by the parameter to this method.
     *If updateOriginal is set then the redundant bounds will be reset to absent.
     */
    public BitSet getSelectionSurplus(
            BitSet todo,
            float thresholdRedundancy,
            boolean updateOriginal,
            boolean onlyFilterFixed,
            int avg_method,
            int monomers,
            String file_name_base,
            boolean append,
            boolean writeNonRedundant,
            boolean writeRedundant,
            boolean removeSurplus
            ) {

        boolean verbosity = ( General.verbosity >= General.verbosityOutput );

        if ( ! initConvenienceVariablesStar()) {
            General.showError("Failed: Surplus.initConvenienceVariablesStar");
            return null;
        }

        if ( todo.cardinality() < 1 ) {
            General.showWarning("No constraints to check so no checks done");
            return (BitSet) todo.clone();
        }
        // Store the selections that can get changed by this code.
        BitSet atomSelectedSave     = (BitSet) gumbo.atom.selected.clone();
        BitSet entrySelectedSave    = (BitSet) gumbo.entry.selected.clone();
        BitSet molSelectedSave      = (BitSet) gumbo.mol.selected.clone();

        BitSet dcSelectedSave       = (BitSet) dc.selected.clone();
        BitSet dcListSelectedSave   = (BitSet) dcList.selected.clone();

        PrintWriter printWriter = null;
        String summary_file_name      = file_name_base + "_summary.txt";
        String summaryFileNameSurplus = file_name_base + "_summary.str";
        try { // Make sure anything in the printWriter is written and the writer is closed
            General.showOutput("Writing surplus results to text file: " + summary_file_name);
            printWriter = InOut.getPrintWriter( summary_file_name, append );
            if ( printWriter == null ) {
                General.showError("Failed to write to surplus check summary file with name: " + summary_file_name );
                return null;
            }

            /** Create the sets anew */
            String[] dcSetNames = {
                "USet", // universe
                // "QSet", // unparsed     NOTE: unused so far.
                // "ASet", // unmatched    NOTE: unused so far.
                "ESet", // exception
                // "CSet", // no coordinate NOTE: unused so far.
                "DSet", // double
                "ISet", // impossible
                "FSet", // fixed
                "RSet", // redundant
                "NSet", // non-redundant
                "SSet"  // surplus
            };
            for (int c=0;c< dcSetNames.length;c++) {
                if ( dc.mainRelation.containsColumn( dcSetNames[ c ] ) ) {
                    if ( dc.mainRelation.getColumnDataType( dcSetNames[ c ]  ) != Relation.DATA_TYPE_BIT ) {
                        General.showWarning("Existing column isn't of type BitSet from dc main relation with name: " + dcSetNames[ c ] );
                    }
                    Object tmpObject = dc.mainRelation.removeColumn( dcSetNames[ c ] );
                    if ( tmpObject == null ) {
                        General.showError("Failed to remove existing column from dc main relation with name: " + dcSetNames[ c ] );
                        return null;
                    }
                }
                if ( ! dc.mainRelation.insertColumn( dcSetNames[ c ], Relation.DATA_TYPE_BIT, null )) {
                    General.showError("Failed to insert bitset column to dc main relation with name: " + dcSetNames[ c ] );
                    return null;
                }
            }

            BitSet USet =  dc.mainRelation.getColumnBit( "USet" );
            //BitSet QSet =  mainRelation.getColumnBit( "QSet" ); unused so far
            //BitSet ASet =  mainRelation.getColumnBit( "ASet" ); unused so far
            BitSet ESet =  dc.mainRelation.getColumnBit( "ESet" );
            //BitSet CSet =  dc.mainRelation.getColumnBit( "CSet" );
            BitSet DSet =  dc.mainRelation.getColumnBit( "DSet" );
            BitSet ISet =  dc.mainRelation.getColumnBit( "ISet" );
            BitSet FSet =  dc.mainRelation.getColumnBit( "FSet" );
            BitSet RSet =  dc.mainRelation.getColumnBit( "RSet" );
            BitSet NSet =  dc.mainRelation.getColumnBit( "NSet" );
            BitSet SSet =  dc.mainRelation.getColumnBit( "SSet" );

            StringBuffer sb_summary = new StringBuffer();
            USet.or( todo ); // All to be checked.

            String msg = "Found number of todo constraints:                       " + USet.cardinality();
            //General.showOutput("Constraints (U): " + PrimitiveArray.toString( USet ));
            sb_summary.append( msg + General.eol );
            if ( verbosity ) {
                printWriter.println( msg );
            }

// EXCEPTIONAL
            ESet.or( todo );
            NSet.or( todo ); // Nset will continue to shrink as other sets are subtracted from it.
            ESet.and( dc.hasUnLinkedAtom );
            NSet.xor( ESet );    // equal to but faster than: .and( ! ESet ) ?
            msg = "Found number of exceptional constraints:                " + ESet.cardinality();
            //General.showOutput("Constraints (E): " + PrimitiveArray.toString( ESet ));
            //General.showOutput("Constraints (N): " + PrimitiveArray.toString( NSet ));
            sb_summary.append( msg + General.eol );
            if ( verbosity ) {
                printWriter.println( msg );
                String out = dc.toString( ESet, false, true );
                if ( out != null ) {
                    printWriter.println( out );
                }
            }

// DOUBLES
            boolean status = setSelectionDoubles(NSet, verbosity);
//            General.showWarning("Not selecting doubles");
//            boolean status = true;
            if ( ! status ) {
                General.showError("Failed to get the selection of double constraints; giving up");
                return null;
            }
            msg = "Found number of constraints to be double with others:   " + DSet.cardinality();
            sb_summary.append( msg + General.eol );
            if ( verbosity ) {
                printWriter.println( msg );
                String out = dc.toString( DSet, false, true );
                if ( out != null ) {
                    printWriter.println( out );
                }
            }
            NSet.xor( DSet );

// IMPOSSIBLES, FIXED AND REDUNDANT
            status = setSelectionImpFixRed(
                NSet,
                thresholdRedundancy,
                onlyFilterFixed,
                avg_method,
                monomers,
                verbosity,
                updateOriginal
             );
            //status = true;
            if ( ! status ) {
                General.showError("Failed to get the selection of impossible and fixed constraints; giving up");
                return null;
            }
            String msgI = "Found number of impossible constraints     :            " + ISet.cardinality();
            String msgF = "Found number of fixed constraints          :            " + FSet.cardinality();
            String msgR = "Found number of redundant constraints      :            " + RSet.cardinality();
            sb_summary.append( msgI + General.eol );
            sb_summary.append( msgF + General.eol );
            sb_summary.append( msgR + General.eol );
            if ( verbosity ) {
                printWriter.println( msgI );
                String out = dc.toString( ISet, false, true );
                if ( out != null ) {
                    printWriter.println( out );
                }
                printWriter.println( msgF );
                out = dc.toString( FSet, false, true );
                if ( out != null ) {
                    printWriter.println( out );
                }
                printWriter.println( msgR );
                out = dc.toString( RSet, false, true );
                if ( out != null ) {
                    printWriter.println( out );
                }
            }
            NSet.xor( ISet );
            NSet.xor( FSet );
            NSet.xor( RSet );

            msg = "Found number of non-redundant constraints:              " + NSet.cardinality();
            sb_summary.append( msg + General.eol );
            if ( verbosity ) {
                printWriter.println( msg );
            }
            SSet.or(ESet);
            //SSet.or(CSet);
            SSet.or(DSet);
            SSet.or(ISet);
            SSet.or(FSet);
            SSet.or(RSet);

            msg = "Found number of constraints to be surplus (E+C+D+I+F+R):" + SSet.cardinality();
            sb_summary.append( msg + General.eol );
            if ( verbosity ) {
                printWriter.println( msg );
            }

            // Write STAR file
            DataBlock db = new DataBlock();
            if ( db == null ) {
                General.showError( "Failed to init datablock.");
                return null;
            }
            int entryId = gumbo.entry.getEntryId();
            db.title = gumbo.entry.nameList[entryId];
            SaveFrame sF = getSFTemplate();
            if ( sF == null ) {
                General.showError( "Failed to getSFTemplate.");
                return null;
            }
            db.add(sF);
            int rowIdx = 0;
            // INTRO

            TagTable tT = (TagTable) sF.get(0);
            tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID , 0);

            printWriter.println("The following OPTIONS are used");
            printWriter.println("Threshold percentage for redundancy                            : " + thresholdRedundancy );
            printWriter.println("Update original constraints                                    : " + updateOriginal    );
            printWriter.println("Only filter fixed                                              : " + onlyFilterFixed    );
            printWriter.println("Averaging method                                               : " + DistConstrList.DEFAULT_AVERAGING_METHOD_NAMES[avg_method] );
            printWriter.println("Number of monomers (only important for sum averaging)          : " + monomers    );
            printWriter.println("File name base (for this and other files)                      : " + file_name_base   );
            printWriter.println("Appending to summary file                                      : " + append );
            printWriter.println("Write nonsurplus constraints                                   : " + writeNonRedundant );
            printWriter.println("Write surplus constraints                                      : " + writeRedundant );
            printWriter.println("Remove surplus constraints                                     : " + removeSurplus );

            tT.setValue(rowIdx, tagNameSurplusRedundancy_threshold_pct      , thresholdRedundancy);
            tT.setValue(rowIdx, tagNameSurplusUpdate_original_restraints    , updateOriginal);
            tT.setValue(rowIdx, tagNameSurplusOnly_filter_fixed             , onlyFilterFixed);
            tT.setValue(rowIdx, tagNameSurplusAveraging_method              , DistConstrList.DEFAULT_AVERAGING_METHOD_NAMES[avg_method]);
            tT.setValue(rowIdx, tagNameSurplusNumber_of_monomers_sum_average, monomers);
            tT.setValue(rowIdx, tagNameSurplusRestraint_count               , USet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_exceptional_count   , ESet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_double_count        , DSet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_impossible_count    , ISet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_fixed_count         , FSet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_redundant_count     , RSet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_surplus_count       , SSet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusRestraint_nonsurplus_count    , NSet.cardinality());
            tT.setValue(rowIdx, tagNameSurplusDetails                       , explanation);

            General.showDebug("Reformat the columns by dictionary defs");
            if ( ! sF.toStarTextFormatting(starDict)) {
                General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
            }
            General.showOutput("Writing surplus results to STAR file: " + summaryFileNameSurplus);
            if ( ! db.toSTAR(summaryFileNameSurplus)) {
                General.showError("Failed to write the file.");
                return null;
            }


            ArrayList setsToWrite    = new ArrayList();
            ArrayList filesToWrite    = new ArrayList();
            if ( writeNonRedundant ) {
                setsToWrite.add( "NSet" ); filesToWrite.add( "nonred" );
            }
            if ( writeRedundant ) {
                setsToWrite.add( "SSet" ); filesToWrite.add( "red" );
            }
            // find first entry and store variable in class instance.
            int currentEntryId = dc.gumbo.entry.selected.nextSetBit(0);
            if ( currentEntryId < 0 ) {
                General.showError("No entries selected");
                return null;
            }

            gumbo.entry.selected.clear();
            gumbo.mol.selected.clear();
            gumbo.atom.selected.clear();
            gumbo.entry.selected.set(currentEntryId);   // only use the current entry

            for (int setId=0;setId<setsToWrite.size();setId++) {
                String setName = (String) setsToWrite.get(setId);
                BitSet setToWrite = dc.mainRelation.getColumnBit(setName);
                String fileName = file_name_base + "_"+filesToWrite.get(setId)+".str";
                dc.selected.clear();
                dc.selected.or(setToWrite);
                if ( dc.selected.cardinality() > 0 ) {
                    General.showOutput("Writing the set: " + setName + " to file: " + fileName
                        + " with number of dcs: " + dc.selected.cardinality());
                    gumbo.entry.writeNmrStarFormattedFileSet(fileName,null,ui);
                } else {
                    General.showOutput("Not writing set: " + setName + " to file: " + fileName
                        + " because there are no dcs in it");
                    File oldDump = new File( fileName );
                    if ( oldDump.exists() && oldDump.isFile() && oldDump.canRead() ) {
                        //General.showDebug("Removing old dump.");
                        if ( ! oldDump.delete() ) {
                            General.showWarning("Failed to remove old dump");
                        }
                    }
                }
            }

            if ( removeSurplus && (SSet != null) ) {
                boolean updateLinkedLists = false;
                //General.showOutput(dc.mainRelation.toString());
                //General.showOutput(dc.distConstrNode.toString());
                if ( ! dc.mainRelation.removeRowsCascading( SSet, updateLinkedLists )) {
                    General.showError( "Failed to remove surplus.");
                } else {
                    General.showDebug( "Removed surplus: "       + SSet.cardinality());
                }
                dcSelectedSave.andNot(SSet); // in the saved set of selected dc clear those that get nilled here.
            }

            // Always show the summary
            printWriter.println();
            printWriter.println();
            printWriter.println();
            printWriter.println( "SUMMARY:" );
            printWriter.println( sb_summary.toString() );

            return SSet;
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        } finally {
            if ( printWriter != null ) {
                printWriter.close();
            }
            // Restore the selections that can get changed by this code.
            gumbo.atom.selected.clear();
            gumbo.entry.selected.clear();
            dc.selected.clear();
            dcList.selected.clear();

            gumbo.atom.selected.or(     atomSelectedSave);
            gumbo.entry.selected.or(    entrySelectedSave);
            gumbo.mol.selected.or(      molSelectedSave);
            dc.selected.or(             dcSelectedSave);
            dcList.selected.or(         dcListSelectedSave);
        }
    }


    /** Calculate the theoretically possible lower and upper bound distances given the atoms involved
     * rids and the averaging method and the maximum distance in the model.
     * The ArrayList contain elements of type:  IntArrayList[2] for each node.
     * In it are the atom rids that do all exist.
     *
     * On error it will return null. If an array is returned it will be valid.
     *
     *The minimum low bound will be 1.8 Ang: 2 H radii - 0.2 Ang.
     *
     *TODO: disable the code dealing with the element specific radii. It's not being used.
     */
    private float[] calcDistanceTheo(ArrayList atomsInvolved, int avg_method, int numberMonomers, float diameter) {

        if ( atomsInvolved == null ) {
            General.showError("Sets of atomsInvolved involved in distance can't be null");
            return null;
        }
        if ( atomsInvolved.size() == 0 ) {
            General.showError("Sets of atomsInvolved involved in distance can't be of zero size");
            return null;
        }

        FloatArrayList minDistances = new FloatArrayList();
        FloatArrayList maxDistances = new FloatArrayList();

        /** Use a lookup with pseudo atoms as needed with center averaging. */
        if (avg_method == DistConstrList.DEFAULT_AVERAGING_METHOD_CENTER) {
            if ( atomsInvolved.size() > 1 ) {
                General.showError("Averaging method center doesn't make sense for multiple contributions");
                return null;
            }
            IntArrayList[] atomsInMemberLoL = (IntArrayList[]) atomsInvolved.get(0); // only 1 node
            IntArrayList atomsInMemberListA = atomsInMemberLoL[0];
            IntArrayList atomsInMemberListB = atomsInMemberLoL[1];
            String atomNameA    = null;
            String atomNameB    = null;
//            float radiusA       = Defs.NULL_FLOAT;
//            float radiusB       = Defs.NULL_FLOAT;
            int resiRidA        = Defs.NULL_INT;
            String resiNameA    = null;
            int resiRidB        = Defs.NULL_INT;
            // loop over left and right.
            for (int i=0;i<2;i++) {
//                float radius = Chemistry.ELEMENT_RADIUS_HYDROGEN; // Assume the smallest radii known.
                String atomName = null;
                int resiRid = Defs.NULL_INT;
                String resiName = null;
                IntArrayList atomsInMemberList = null;
                if ( i == 0 ) {
                    atomsInMemberList = atomsInMemberListA;
                } else {
                    atomsInMemberList = atomsInMemberListB;
                }
                //General.showDebug("Atoms in member: \n" + ui.gumbo.atom.toString( PrimitiveArray.toBitSet( atomsInMemberList, -1)));

                int atomRid = atomsInMemberList.getQuick(0);
                resiRid            = gumbo.atom.resId[     atomRid ];
                resiName           = gumbo.res.nameList[  resiRid ];
                if ( atomsInMemberList.size() == 1 ) {
                    atomName           = gumbo.atom.nameList[  atomRid ];
                    int elementId      = gumbo.atom.elementId[ atomRid ];
                    if ( Defs.isNull(elementId) || (elementId<1) || (elementId>=Chemistry.elementCount)) {
                        //General.showDebug("Failed to get element id for atom : " + gumbo.atom.toString( atomRid ) + " assuming smallest known: H ");
                    } else {
//                        radius = Chemistry.radii[ elementId ];
                    }
                } else {
                    // Require that all atoms in pseudo have to be in the list
                    // e.g. for ALA MB all three HB1-HB3 need to be present
                    atomName = ui.wattosLib.pseudoLib.getCommonPseudoParent( atomsInMemberList, ui, true );
                    if ( atomName == null || atomName.equals(Defs.EMPTY_STRING)) {
                        General.showError("Can't calcDistanceTheo with Center averaging for multiple atoms in one member that don't resolved to one pseudo atom");
                        General.showError("Atoms in member: " + ui.gumbo.atom.toString( PrimitiveArray.toBitSet( atomsInMemberList, -1)));
                        return null;
                    }

                }
                if ( i == 0 ) {
//                    radiusA = radius;
                    atomNameA = atomName;
                    resiRidA = resiRid;
                    resiNameA = resiName;
                } else {
//                    radiusB = radius;
                    atomNameB = atomName;
                    resiRidB = resiRid;
                    // no resiname b needed.
                }
            }
            //General.showDebug("");
            float[] bnds = null;
            // Look for intra residual contacts only in lib.
            if ( resiRidA == resiRidB ) {
                // look for bounds but will be null if not present.
                bnds = (float[]) ui.wattosLib.redundantLib.bounds.get( resiNameA, atomNameA, atomNameB );
                if ( bnds == null ) {
                    General.showWarning("Failed to find intra residual distance in redundancy library between atoms: " +
                        atomNameA + " and " + atomNameB + " in residue type: " + resiNameA);
                    General.showDebug("Will use the sum of radii and the molecules' diameter as estimates for the lower and upper bounds respectively");
                    /**
                    General.showDebug("radius A : " + radiusA);
                    General.showDebug("radius B : " + radiusB);
                    General.showDebug("atom A   : " + atomNameA);
                    General.showDebug("atom B   : " + atomNameB);
                    General.showDebug("resiRid A: " + resiRidA);
                    General.showDebug("resiRid B: " + resiRidB);
                     */
                }
            }
            // If it isn't initialized use a different estimate.
            if ( bnds == null ) {
                bnds = new float[2];
                bnds[0] = Chemistry.smallestBondEver; // might be optimize later on for hydrogen/non/- bonded states but for this is the best theo upp bound for now.
                // was: bnds[0] = radiusA + radiusB - RedundantLib.LOWER_DISTANCE_MINIMUM_CORRECTION;
                bnds[1] = diameter; // upper bound
            }
            //General.showDebug("result is: " + PrimitiveArray.toString( bnds ));
            return bnds;
        }


        // In the case of anything but center averaging...
        // for each node
        float[] result = new float[2];
        for ( int i=0;i<atomsInvolved.size(); i++) {
            IntArrayList[] atomsInMemberLoL = (IntArrayList[]) atomsInvolved.get(i);
            IntArrayList atomsInMemberListA = atomsInMemberLoL[0];
            IntArrayList atomsInMemberListB = atomsInMemberLoL[1];
            // for each combination
            for ( int a=0; a<atomsInMemberListA.size(); a++) {
                int atomRidA = atomsInMemberListA.getQuick(a);
                String atomNameA    = gumbo.atom.nameList[  atomRidA ];
                int elementIdA      = gumbo.atom.elementId[ atomRidA ];
                int resiRidA        = gumbo.atom.resId[     atomRidA ];
                String resiNameA    = gumbo.res.nameList[  resiRidA ];
//                float radiusA = Chemistry.ELEMENT_RADIUS_HYDROGEN; // Assume the smallest radii known.
                if ( Defs.isNull(elementIdA) || (elementIdA<1) || (elementIdA>=Chemistry.elementCount)) {
                    //General.showDebug("Failed to get element id for atom A: " + gumbo.atom.toString( atomRidA ) + " assuming smallest known: H ");
                } else {
//                    radiusA = Chemistry.radii[ elementIdA ];
                }

                // Can be optimized by caching values for radii, names, etc...
                for ( int b=0; b<atomsInMemberListB.size(); b++) {
                    int atomRidB = atomsInMemberListB.getQuick(b);
                    String atomNameB    = gumbo.atom.nameList[  atomRidB ];
                    int elementIdB      = gumbo.atom.elementId[ atomRidB ];
                    int resiRidB        = gumbo.atom.resId[     atomRidB ];
//                    String resiNameB    = gumbo.res.nameList[  resiRidB ];
//                    float radiusB = Chemistry.ELEMENT_RADIUS_HYDROGEN; // Assume the smallest radii known.
                    if ( Defs.isNull(elementIdB) || (elementIdB<1) || (elementIdB>=Chemistry.elementCount)) {
                        //General.showDebug("Failed to get element id for atom B: " + gumbo.atom.toString( atomRidB ) + " assuming smallest known: H ");
                    } else {
//                        radiusB = Chemistry.radii[ elementIdB ];
                    }

                    float[] bnds = null;
                    // Look for intra residual contacts in lib.
                    if ( resiRidA == resiRidB ) {
                        // look for bounds but will be null if not present.
                        bnds = (float[]) ui.wattosLib.redundantLib.bounds.get( resiNameA, atomNameA, atomNameB );
                        if ( bnds == null ) {
                            General.showWarning("Failed to find intra residual distance between atoms: " +
                                gumbo.atom.toString( atomRidA ) + " and " +
                                gumbo.atom.toString( atomRidB ) );
                            //General.showDebug("Will use the sum of radii and the molecules' diameter as estimates for the lower and upper bounds respectively");
                        }
                    }
                    // If it isn't initialized use a different estimate.
                    if ( bnds == null ) {
                        bnds = new float[2];
                        // was bnds[0] = radiusA + radiusB - RedundantLib.LOWER_DISTANCE_MINIMUM_CORRECTION;
                        bnds[0] = Chemistry.smallestBondEver; // might be optimize later on for hydrogen/non/- bonded states but for this is the best theo upp bound for now.
                        bnds[1] = diameter; // upper bound
                    }
                    minDistances.add( bnds[0] );
                    maxDistances.add( bnds[1] );
                } // end loop over atoms B
            } // end loop over atoms A
        } // end of loop over nodes.

        result[ 0 ] = PrimitiveArray.getAverage( minDistances, avg_method, -6.0d, numberMonomers );
        result[ 1 ] = PrimitiveArray.getAverage( maxDistances, avg_method, -6.0d, numberMonomers );
        if ( Defs.isNull( result[ 0 ] ) ) {
            General.showError("Failed to get the theoretical min. averaged distance from: " + PrimitiveArray.toString( minDistances));
            return null;
        }
        if ( Defs.isNull( result[ 1 ] ) ) {
            General.showError("Failed to get the theoretical max. averaged distance from: " + PrimitiveArray.toString( maxDistances));
            return null;
        }
        //General.showDebug("result is: " + PrimitiveArray.toString( result ));
        return result;
    }



    public boolean resetConvenienceVariables() {
        upp_theo = dc.mainRelation.getColumnFloat(DistConstr.DEFAULT_UPP_THEO);
        low_theo = dc.mainRelation.getColumnFloat(DistConstr.DEFAULT_LOW_THEO);

        if (
            upp_theo          == null ||
            low_theo          == null
        ) {
            return false;
        }
        return true;
    }

    /** Returns a template with the star formatted output template
     */
    private SaveFrame getSFTemplate() {
        SaveFrame sF = new SaveFrame();
        sF.setTitle("Distance_constraint_surplus");
        // Default variables.
        HashMap             namesAndTypes;
        ArrayList           order;
        HashMap             namesAndValues;
        TagTable            tT;
        try {
            // INTRO
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            DBMS dbms = new DBMS(); // Use a temporary dbms for this because we don't
            // want to hold on to this data for ever.
            tT                      = new TagTable("Distance_constraint_surplus_list", dbms);
            tT.isFree               = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            String cat = "distance_constraint_surplus";
            namesAndValues.put( tagNameSurplusSf_category, cat);
//            namesAndValues.put( tagNameSurplusEntry_ID, new Integer(1));
//            namesAndValues.put( tagNameSurplusDC_surplus_ID, new Integer(1));

            starDict.putFromDict( namesAndTypes, order, tagNameSurplusSf_category                   );
//            starDict.putFromDict( namesAndTypes, order, tagNameSurplusEntry_ID                      );
//            starDict.putFromDict( namesAndTypes, order, tagNameSurplusDC_surplus_ID                 );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRedundancy_threshold_pct      );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusUpdate_original_restraints    );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusOnly_filter_fixed             );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusAveraging_method              );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusNumber_of_monomers_sum_average);
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_count               );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_surplus_count       );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_nonsurplus_count    );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_exceptional_count   );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_double_count        );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_impossible_count    );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_fixed_count         );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusRestraint_redundant_count     );
            starDict.putFromDict( namesAndTypes, order, tagNameSurplusDetails                       );

            // Append columns after order id column.
            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return sF;
    }
    /**
     * @return <CODE>true</CODE> for success
     */
    public boolean initConvenienceVariablesStar() {
        // Please note that the following names are not hard-coded as star names.
        try {
            tagNameSurplusSf_category                    = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Sf_category                   ");
//            tagNameSurplusEntry_ID                       = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Entry_ID                      ");
//            tagNameSurplusDC_surplus_ID                  = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.DC_surplus_ID                 ");
            tagNameSurplusRedundancy_threshold_pct       = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Redundancy_threshold_pct      ");
            tagNameSurplusUpdate_original_restraints     = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Update_original_restraints    ");
            tagNameSurplusOnly_filter_fixed              = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Only_filter_fixed             ");
            tagNameSurplusAveraging_method               = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Averaging_method              ");
            tagNameSurplusNumber_of_monomers_sum_average = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Number_of_monomers_sum_average");
            tagNameSurplusRestraint_count                = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_count               ");
            tagNameSurplusRestraint_exceptional_count    = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_exceptional_count   ");
            tagNameSurplusRestraint_double_count         = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_double_count        ");
            tagNameSurplusRestraint_impossible_count     = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_impossible_count    ");
            tagNameSurplusRestraint_fixed_count          = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_fixed_count         ");
            tagNameSurplusRestraint_redundant_count      = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_redundant_count     ");
            tagNameSurplusRestraint_surplus_count        = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_surplus_count       ");
            tagNameSurplusRestraint_nonsurplus_count     = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Restraint_nonsurplus_count    ");
            tagNameSurplusDetails                        = starDict.getTagName( "distance_constraint_surplus","_Distance_constraint_surplus.Details                       ");

        } catch ( Exception e ) {
            General.showThrowable(e);
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            return false;
        }
        if (
            tagNameSurplusSf_category                    == null ||
//            tagNameSurplusEntry_ID                       == null ||
//            tagNameSurplusDC_surplus_ID                  == null ||
            tagNameSurplusRedundancy_threshold_pct       == null ||
            tagNameSurplusUpdate_original_restraints     == null ||
            tagNameSurplusOnly_filter_fixed              == null ||
            tagNameSurplusAveraging_method               == null ||
            tagNameSurplusNumber_of_monomers_sum_average == null ||
            tagNameSurplusRestraint_count                == null ||
            tagNameSurplusRestraint_exceptional_count    == null ||
            tagNameSurplusRestraint_double_count         == null ||
            tagNameSurplusRestraint_impossible_count     == null ||
            tagNameSurplusRestraint_fixed_count          == null ||
            tagNameSurplusRestraint_redundant_count      == null ||
            tagNameSurplusRestraint_surplus_count        == null ||
            tagNameSurplusDetails                        == null ||
            tagNameSurplusRestraint_nonsurplus_count     == null
                ) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary.");
            return false;
        }
        /** debug */
//        if ( false ) {
//            String[] tagNames = {
//                tagNameSurplusSf_category,
////                tagNameSurplusEntry_ID,
////                tagNameSurplusDC_surplus_ID,
//                tagNameSurplusRedundancy_threshold_pct,
//                tagNameSurplusUpdate_original_restraints,
//                tagNameSurplusOnly_filter_fixed,
//                tagNameSurplusAveraging_method,
//                tagNameSurplusNumber_of_monomers_sum_average,
//                tagNameSurplusRestraint_count,
//                tagNameSurplusRestraint_exceptional_count,
//                tagNameSurplusRestraint_double_count,
//                tagNameSurplusRestraint_impossible_count,
//                tagNameSurplusRestraint_fixed_count,
//                tagNameSurplusRestraint_redundant_count,
//                tagNameSurplusRestraint_surplus_count,
//                tagNameSurplusRestraint_nonsurplus_count,
//                tagNameSurplusDetails
//            };
//            General.showDebug("Tagnames:\n"+Strings.toString(tagNames,true));
//        }


        return true;
    }
    }
