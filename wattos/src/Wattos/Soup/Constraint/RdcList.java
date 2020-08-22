/*
 */

package Wattos.Soup.Constraint;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Soup.Gumbo;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;


/**A list of distance restraints. Contains method to write the violation STAR
 *statistics.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class RdcList extends SimpleConstrList implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** Convenience variables */
    public StarDictionary starDict;


    public RdcList(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
//        General.showDebug("back in RdcList constructor");
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public RdcList(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
//        General.showDebug("back in RdcList constructor");
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
//        General.showDebug("now in RdcList.init()");
        super.init(dbms);
//        General.showDebug("back in RdcList.init()");

        name = Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[RELATION_ID_SET_NAME];
//        General.showDebug("Found ATTRIBUTE_SET_SUB_CLASS:"+Strings.toString(ATTRIBUTE_SET_SUB_CLASS));
        // MAIN RELATION in addition to the ones in SimpleConstr item.
        // add more here.
        Relation relation = null;
        String relationName = Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER,
                DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;

        return true;
    }



    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();

        ATTRIBUTE_SET_SUB_CLASS      = Constr.DEFAULT_ATTRIBUTE_SET_RDC;
        ATTRIBUTE_SET_SUB_CLASS_LIST = Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST;

        entry_id                    = (int[])       mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[   RelationSet.RELATION_ID_COLUMN_NAME]);// Atom (starting with fkcs)

        constrCount              = (int[])       mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_CONSTR_COUNT);
        violCount                = (int[])       mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_COUNT  );
        violTotal                = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_TOTAL  );
        violMax                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_MAX    );
        violRms                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_RMS    );
        violAll                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_ALL    );
        violAvViol               = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_AV_VIOL);

        /**
         * constrLowCount              = (int[])       mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_CONSTR_LOW_COUNT);
         * violLowCount                = (int[])       mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_COUNT  );
         * violLowTotal                = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_TOTAL  );
         * violLowMax                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX    );
         * violLowRms                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_RMS    );
         * violLowAll                  = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_ALL    );
         * violLowAvViol               = (float[])     mainRelation.getColumn(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_AV_VIOL);
         */

        subTypeList     = (String[])    mainRelation.getColumn(             Constr.DEFAULT_ATTRIBUTE_SUB_TYPE );
        subTypeListNR   =               mainRelation.getColumnStringSet(    Constr.DEFAULT_ATTRIBUTE_SUB_TYPE );
        formatList      = (String[])    mainRelation.getColumn(             Constr.DEFAULT_ATTRIBUTE_FORMAT );
        formatListNR    =               mainRelation.getColumnStringSet(    Constr.DEFAULT_ATTRIBUTE_FORMAT );
        programList     = (String[])    mainRelation.getColumn(             Constr.DEFAULT_ATTRIBUTE_PROGRAM  );
        programListNR   =               mainRelation.getColumnStringSet(    Constr.DEFAULT_ATTRIBUTE_PROGRAM);

        position         = (int[])       mainRelation.getColumn(    Constr.DEFAULT_ATTRIBUTE_POSITION         );

        constrCount                 = (int[])       mainRelation.getColumn(  Relation.DEFAULT_ATTRIBUTE_COUNT);

        cutoff = mainRelation.getColumnFloat(    Constr.DEFAULT_ATTRIBUTE_CUTOFF);

        if ( entry_id == null || constrCount == null
                || subTypeList     == null
                || subTypeListNR   == null
                || formatList      == null
                || formatListNR    == null
                || programList     == null
                || programListNR   == null
                || position          == null
                || constrCount == null
                || violCount == null
                || violTotal == null
                || violMax == null
                || violRms == null
                || violAll == null
                || violAvViol == null
                /**
                 * || constrLowCount == null
                 * || violLowCount == null
                 * || violLowTotal == null
                 * || violLowMax == null
                 * || violLowRms == null
                 * || violLowAll == null
                 * || violLowAvViol == null
                 */
                || cutoff == null
                ) {
            return false;
        }
        return true;
    }

    /** */
    public boolean toXplor(BitSet todo, String fn, String atomNomenclature) {
        int fileCount = 0;
        for (int listRID = todo.nextSetBit(0); listRID >= 0; listRID=todo.nextSetBit(listRID+1)) {
            fileCount++;
            BitSet ridsRDC = SQLSelect.selectBitSet(dbms,
                    constr.rdc.mainRelation,
                    Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS,
                    new Integer( listRID ),
                    false);
            if ( ridsRDC == null ) {
                General.showError("Failed to get ridsRDC for toXplor.");
                return false;
            }
            boolean sortRestraints=true;
            boolean status = constr.rdc.toXplorOrSo( ridsRDC, fn, fileCount, atomNomenclature, sortRestraints, null );
            if ( ! status ) {
                General.showError("Failed rdc.toXplor");
                General.showError("Not writing any more rdcLists");
                return false;
            }
        }
        return true;
    }


    public BitSet getRDCRidsInListAndTodo(int ridList, BitSet todoRDC ) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms,
                constr.rdc.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(ridList),false);
        if ( result == null ) {
            General.showError("Failed to SQLSelect.selectBitSet in getRDCRidsInListAndTodo");
            return null;
        }
        result.and(todoRDC);
        return result;
    }
    /**
    public BitSet getRDCViolRidsByListId(int ridList ) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms,
                constr.rdc.distConstrViol,
                Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(ridList),false);
        if ( result == null ) {
            General.showError("Failed to SQLSelect.selectBitSet in getRDCViolRidsByListId");
            return null;
        }
        return result;
    }
    */



    /** Remove existing violation records for this list if they exist.
     *
    public boolean removeViolationsByListId( int currentRDCListId ) {
        BitSet rdcViolRidSet = getRDCViolRidsByListId(currentRDCListId );
        if ( rdcViolRidSet == null ) {
            General.showError("Failed to getRDCRidsInListAndTodo in removeViolationsByListId");
            return false;
        }
        if ( rdcViolRidSet.cardinality() != 0 ) {
//            General.showDebug("Will remove old list of violations by list id numbered: " + rdcViolRidSet.cardinality());
        }
        return constr.rdc.distConstrViol.removeRowsCascading(rdcViolRidSet,false);
    }
    */

    /**
     * Calculates the violation for given list and only consider restraints
     *given in the todoRDC set.
     *If the cutoff is Defs.NULL then it will be assumed to be 0.5 Ang. It sets list
     *properties too.
     *
    public boolean calcViolation(BitSet todoRDC, int currentRDCListId, float cutoffValue ) {
        rdc = constr.rdc; // get a ref now because it was hard before.
        if ( Defs.isNull(cutoffValue)) {
            cutoffValue = .5f;
        }

        BitSet selectedModels = gumbo.model.selected;
        BitSet todoRDCFiltered = getRDCRidsInListAndTodo(currentRDCListId,todoRDC);
        if ( todoRDCFiltered == null ) {
            General.showError("Failed to getRDCRidsInListAndTodo");
            return false;
        }
//        if ( todoRDC.cardinality() != todoRDCFiltered.cardinality() ) {
//            General.showDebug("Filtered restraints because todoRDC: " +
//                    todoRDC.cardinality() +" and todoRDCFiltered: " +  todoRDCFiltered.cardinality() );
//        }

        int count = todoRDCFiltered.cardinality();
        int selectedModelsCount = selectedModels.cardinality();
        if ( selectedModelsCount < 1 ) {
            General.showWarning("No models selected in calcDistance.");
            return true;
        }
        float[][] violationMatrix = new float[count][selectedModelsCount];

//        General.showDebug("Calculating violations for rdcs numbering: "   + count +
//                " in models numbering: "                      + selectedModelsCount);
//        General.showDebug("Dcs   : " + PrimitiveArray.toString( todoRDCFiltered));
        int[] selectedModelArray = PrimitiveArray.toIntArray( selectedModels ); // for efficiency.
//        General.showDebug("Models: " + PrimitiveArray.toString( selectedModelArray ));

        if ( count == 0 ) {
            General.showWarning("No distance constraints selected in calcDistance.");
            return true;
        }

        // Remove existing violation records for this list if they exist.
        if ( ! removeViolationsByListId(currentRDCListId)) {
            General.showError("Failed to get all indexes.");
            return false;
        }

        IndexSortedInt indexMembAtom = (IndexSortedInt) rdc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_RDC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) rdc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_RDC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) rdc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_RDC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if (    indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }

        int currentRDCViolId = 0;
        int rdcCount         = 0;
        int violationCount  = 0;
        BitSet unlinkedAtomSelected = (BitSet) rdc.hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and( todoRDCFiltered );
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if ( unlinkedAtomCount > 0 ) {
            General.showWarning("Will skip distance calculation for number of constraints: " + unlinkedAtomCount + " because not all atoms are linked for them." );
        } else {
//            General.showDebug("Found no unlinked atoms in the constraints.");
        }
        // FOR EACH CONSTRAINT
        for (int currentRDCId = todoRDCFiltered.nextSetBit(0);currentRDCId>=0;currentRDCId = todoRDCFiltered.nextSetBit(currentRDCId+1)) {
            float[] distanceList = rdc.calcDistance( currentRDCId, selectedModelArray);
            if ( distanceList == null ) {
                General.showError("Failed to calculate distance for restraint:\n" + toString(currentRDCId));
                return false;
            }
            if ( Defs.isNull( distanceList[0])){
                General.showError("The restraint has unlinked atoms or a failure to find all atoms for restraint:\n" + toString(currentRDCId));
                return false;
            }
//            int currentRDCEntryId = rdc.entryIdMain[     currentRDCId ];

            float[] lowTargetUppBound = rdc.getLowTargetUppTheoBound( currentRDCId );
            float low = lowTargetUppBound[ 0 ]; // cache some variables for speed and convenience.
            float upp = lowTargetUppBound[ 2 ];

            rdc.violUppMax[currentRDCId] = -1f;
            rdc.violLowMax[currentRDCId] = -1f;
            // When the set of atoms involved is collected for the first model, the other models can easily be done too.
            // FOR EACH selected MODEL
            for ( int currentModelId=0; currentModelId<selectedModelArray.length; currentModelId++) {
                float dist = distanceList[currentModelId];
                if ( Defs.isNull( dist ) ) {
                    General.showError("Failed to calculate the distance for constraint: " + currentRDCId + " in model: " + (currentModelId+1) + " will NOT try other models.");
                    return false;
                }
                float viol = Defs.NULL_FLOAT;  // Will be reset if there are upper and/or lower bounds.
                /** See xplor version 4.0 manual. This can be changed later to accomodate other potential functions.
                 * Always a positive violation!
                 *\/
                boolean isLowViol = false;
                boolean isUppViol = false;
                if ( !Defs.isNull(low)) {
                    if ( dist < low ) {
                        viol = low - dist;
                        isLowViol = true;
                    } else {
                        viol = 0f;
                    }
                }
                if ( !Defs.isNull(upp) ) {
                    if ( dist > upp) {
                        viol = dist - upp;
                        isUppViol = true;
                    } else if ( Defs.isNull(viol) ) {
                        viol = 0f;
                    }
                }

                //General.showDebug("**** Found dist, viol, upp, low: " + dist + ", " + viol + ", " + upp + ", " + low);
                currentRDCViolId = rdc.distConstrViol.getNextReservedRow(currentRDCViolId);
                // Check if the relation grew in size because not all relations can be adaquately estimated.
                if ( currentRDCViolId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                    if ( ! rdc.resetConvenienceVariables() ) {
                        General.showCodeBug("Failed to resetConvenienceVariables.");
                        return false;
                    }
                    currentRDCViolId = rdc.distConstrViol.getNextReservedRow(0); // now it should be fine.
                }
                if ( currentRDCViolId < 0 ) {
                    General.showCodeBug("Failed to get next reserved row in distance constraint violation table.");
                    return false;
                }

                rdc.modelIdViol[  currentRDCViolId ] = selectedModelArray[ currentModelId ];
                rdc.rdcMainIdViol[ currentRDCViolId ] = currentRDCId;
                rdc.rdcListIdViol[ currentRDCViolId ] = rdc.rdcListIdMain[    currentRDCId ];
                rdc.entryIdViol[  currentRDCViolId ] = rdc.entryIdMain[     currentRDCId ];
                rdc.distance[     currentRDCViolId ] = dist;
                rdc.violation[    currentRDCViolId ] = viol;

                int modelNumber = gumbo.model.number[ selectedModelArray[ currentModelId ] ];
                if ( isUppViol && (rdc.violUppMax[currentRDCId] < viol) ) {
                    rdc.violUppMax[currentRDCId] = viol;
                    rdc.violUppMaxModelNum[currentRDCId] = modelNumber;
                }
                if ( isLowViol && (rdc.violLowMax[currentRDCId] < viol) ) {
                    rdc.violLowMax[currentRDCId] = viol;
                    rdc.violLowMaxModelNum[currentRDCId] = modelNumber;
                }
                if ( isUppViol || isLowViol) {
                    violationCount++;
                }
                violationMatrix[rdcCount][currentModelId] = viol;
            } // end of loop per model

            // check to see if there actually was a viol.
            if ( rdc.violUppMax[currentRDCId] == -1f ) {
                rdc.violUppMax[currentRDCId] = Defs.NULL_FLOAT;
            }
            if ( rdc.violLowMax[currentRDCId] == -1f ) {
                rdc.violLowMax[currentRDCId] = Defs.NULL_FLOAT;
            }
            rdcCount++;
        } // end of loop per constraint
        rdc.distConstrViol.cancelAllReservedRows();

        float[] violArray   = PrimitiveArray.toFloatArray(  violationMatrix);
        float[] avSd        = Statistics.getAvSd(           violArray);
        float sum           = Statistics.getSum(            violArray);

        constrCount[ currentRDCListId ] = count;
        violCount  [ currentRDCListId ] = violationCount;
        violTotal  [ currentRDCListId ] = sum*selectedModelsCount;
        violMax    [ currentRDCListId ] = Statistics.getMax(violArray);
        violRms    [ currentRDCListId ] = avSd[1];
        violAll    [ currentRDCListId ] = avSd[0];
        violAvViol [ currentRDCListId ] = sum/violationCount;
        cutoff[      currentRDCListId ] = cutoffValue;

//        General.showDebug("Calculated distances for number of constraints: " + rdcCount + " in number of models: " + selectedModelArray.length);
        return true;
    }
    */

    /**
     * @return <CODE>true</CODE> for success
     */
    public boolean initConvenienceVariablesStar() {
        // Please note that the following names are not hard-coded as star names.
        try {
            starDict = dbms.ui.wattosLib.starDictionary;
//            tagNameRDCStats_Sf_category                      = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Sf_category                 ");
        } catch ( Exception e ) {
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            General.showThrowable(e);
            return false;
        }
//        if ( true
////                tagNameRDCStats_Sf_category  == null
//                ) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary.");
            return false;
//        }
//        return true;
    }
}
