/*
 */

package Wattos.Soup.Constraint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Gumbo;
import Wattos.Star.SaveFrame;
import Wattos.Star.TagTable;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Wiskunde.Geometry;
import Wattos.Utils.Wiskunde.Statistics;

/**
 * A list of distance restraints. Contains method to write the violation STAR statistics. Angles will be shown in range
 * <-180,180] such as phi and psi.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class CdihList extends SimpleConstrList implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /** Convenience variables */
    public StarDictionary starDict;
    public Cdih cdih = null;

    public String tagNameTA_constraint_stats_list_Sf_category;
    // public String tagNameTA_constraint_stats_list_Entry_ID;
    // public String tagNameTA_constraint_stats_list_ID;
    // public String tagNameTA_constraint_stats_list_Constraint_file_ID;
    public String tagNameTA_constraint_stats_list_Constraint_count;
    public String tagNameTA_constraint_stats_list_Viol_count;
    public String tagNameTA_constraint_stats_list_Viol_total;
    public String tagNameTA_constraint_stats_list_Viol_max;
    public String tagNameTA_constraint_stats_list_Viol_rms;
    public String tagNameTA_constraint_stats_list_Viol_average_all_restraints;
    public String tagNameTA_constraint_stats_list_Viol_average_violations_only;
    public String tagNameTA_constraint_stats_list_Cutoff_violation_report;
    public String tagNameTA_constraint_stats_list_Details;
    public String tagNameTA_constraint_stats_Restraint_ID;
    public String tagNameTA_constraint_stats_Torsion_angle_name;
    public String tagNameTA_constraint_stats_Entity_assembly_ID_1;
    public String tagNameTA_constraint_stats_Comp_index_ID_1;
    public String tagNameTA_constraint_stats_Comp_ID_1;
    public String tagNameTA_constraint_stats_Atom_ID_1;
    public String tagNameTA_constraint_stats_Entity_assembly_ID_2;
    public String tagNameTA_constraint_stats_Comp_index_ID_2;
    public String tagNameTA_constraint_stats_Comp_ID_2;
    public String tagNameTA_constraint_stats_Atom_ID_2;
    public String tagNameTA_constraint_stats_Entity_assembly_ID_3;
    public String tagNameTA_constraint_stats_Comp_index_ID_3;
    public String tagNameTA_constraint_stats_Comp_ID_3;
    public String tagNameTA_constraint_stats_Atom_ID_3;
    public String tagNameTA_constraint_stats_Entity_assembly_ID_4;
    public String tagNameTA_constraint_stats_Comp_index_ID_4;
    public String tagNameTA_constraint_stats_Comp_ID_4;
    public String tagNameTA_constraint_stats_Atom_ID_4;
    public String tagNameTA_constraint_stats_Angle_lower_bound_val;
    public String tagNameTA_constraint_stats_Angle_upper_bound_val;
    public String tagNameTA_constraint_stats_Angle_average;
    public String tagNameTA_constraint_stats_Angle_minimum;
    public String tagNameTA_constraint_stats_Angle_maximum;
    public String tagNameTA_constraint_stats_Max_violation;
    public String tagNameTA_constraint_stats_Max_violation_model_number;
    public String tagNameTA_constraint_stats_Above_cutoff_violation_count;
    public String tagNameTA_constraint_stats_Above_cutoff_violation_per_model;
    // public String tagNameTA_constraint_stats_Entry_ID;
    // public String tagNameTA_constraint_stats_TA_constraint_stats_list_ID;

    public static String explanation = null;

    static {
        int i = 1;
        explanation = "\nDescription of the tags in this list:\n" + "*  "
                + i++
                + " * Administrative tag\n"
                + "*  "
                + i++
                + " * ID of the restraint list.                                                                 \n"
                + "*  "
                + i++
                + " * Number of restraints in list.                                                             \n"
                + "*  "
                + i++
                + " * Number of violated restraints (each model violation is used).                             \n"
                + "*  "
                + i++
                + " * Sum of violations in degrees.                                                             \n"
                + "*  "
                + i++
                + " * Maximum violation of a restraint without averaging in any way.                            \n"
                + "*  "
                + i++
                + " * Rms of violations over all restraints.                                                    \n"
                + "*  "
                + i++
                + " *  Average violation over all restraints.                                                   \n"
                + "*  "
                + i++
                + " *  Average violation over violated restraints.                                              \n"
                + "            This violation is averaged over only those models in which the restraint is violated.\n"
                + "            Threshold for reporting violations (degrees) in the last columns of the next table.     \n"
                + "*  " + i++
                + " * This tag.                                                                                \n";
        i = 1;
        explanation = explanation + "\nDescription of the tags in the per restraint table below:\n" + "*  " + i++
                + " * Restraint ID within restraint list.                             \n" + "*  " + i++
                + " * Torsion angle name where available.                             \n" + "*  " + i++
                + " *     First atom's:                                               \n" + "*  " + i++
                + " * Chain identifier (can be absent if none defined)                \n" + "*  " + i++
                + " * Residue number                                                  \n" + "*  " + i++
                + " * Residue name                                                    \n" + "*  " + i++
                + " * Name of (pseudo-)atom                                           \n" + "*  " + i++
                + " *  Second thru fourth atom's identifiers occupy columns 7 thru 18.\n";

        i = 19;
        explanation = explanation + "*  " + i++
                + " * Lower bound (degrees)                                               \n" + "*  " + i++
                + " * Upper bound (degrees)                                               \n" + "*  " + i++
                + " * Average angle in ensemble of models                                 \n" + "*  " + i++
                + " * Minimum angle in ensemble of models (counter clockwise from range)  \n" + "*  " + i++
                + " * Maximum angle in ensemble of models         (clockwise from range)  \n" + "*  " + i++
                + " * Maximum violation (without any averaging)                           \n" + "*  " + i++
                + " * Model number with the maximum violation                             \n" + "*  " + i++
                + " * Number of models with a violation above cutoff                      \n" + "*  " + i++
                + " * List of models with a violation above cutoff. See description above.\n" + "*  " + i++
                + " * Administrative tag                                                  \n" + "*  " + i++
                + " * Administrative tag                                                  \n";
    }

    public CdihList(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        // General.showDebug("back in CdihList constructor");
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    /**
     * The relationSetName is a parameter so non-standard relation sets can be created; e.g. AtomTmp with a relation
     * named AtomTmpMain etc.
     */
    public CdihList(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        // General.showDebug("back in CdihList constructor");
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        // General.showDebug("now in CdihList.init()");
        super.init(dbms);
        // General.showDebug("back in CdihList.init()");

        name = Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_SET_NAME];
        // General.showDebug("Found ATTRIBUTE_SET_SUB_CLASS:"+Strings.toString(ATTRIBUTE_SET_SUB_CLASS));
        // MAIN RELATION in addition to the ones in SimpleConstr item.
        // add more here.
        Relation relation = null;
        String relationName = Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch (Exception e) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO,
                relationName);
        relation.insertColumnSet(0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, DEFAULT_ATTRIBUTE_VALUES,
                DEFAULT_ATTRIBUTE_FKCS);
        addRelation(relation);
        mainRelation = relation;

        return true;
    }

    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();

        ATTRIBUTE_SET_SUB_CLASS = Constr.DEFAULT_ATTRIBUTE_SET_CDIH;
        ATTRIBUTE_SET_SUB_CLASS_LIST = Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST;

        entry_id = (int[]) mainRelation
                .getColumn(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME]);// Atom (starting
        // with fkcs)

        constrCount = (int[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_CONSTR_COUNT);
        violCount = (int[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_COUNT);
        violTotal = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_TOTAL);
        violMax = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_MAX);
        violRms = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_RMS);
        violAll = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_ALL);
        violAvViol = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_AV_VIOL);

        /**
         * constrLowCount = (int[]) mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_CONSTR_LOW_COUNT); violLowCount =
         * (int[]) mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_COUNT ); violLowTotal = (float[])
         * mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_TOTAL ); violLowMax = (float[])
         * mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX ); violLowRms = (float[])
         * mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_RMS ); violLowAll = (float[])
         * mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_ALL ); violLowAvViol = (float[])
         * mainRelation.getColumn( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_AV_VIOL);
         */

        subTypeList = (String[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_SUB_TYPE);
        subTypeListNR = mainRelation.getColumnStringSet(Constr.DEFAULT_ATTRIBUTE_SUB_TYPE);
        formatList = (String[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_FORMAT);
        formatListNR = mainRelation.getColumnStringSet(Constr.DEFAULT_ATTRIBUTE_FORMAT);
        programList = (String[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_PROGRAM);
        programListNR = mainRelation.getColumnStringSet(Constr.DEFAULT_ATTRIBUTE_PROGRAM);

        position = (int[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_POSITION);

        constrCount = (int[]) mainRelation.getColumn(Relation.DEFAULT_ATTRIBUTE_COUNT);

        cutoff = mainRelation.getColumnFloat(Constr.DEFAULT_ATTRIBUTE_CUTOFF);

        if (entry_id == null || constrCount == null || subTypeList == null || subTypeListNR == null
                || formatList == null || formatListNR == null || programList == null || programListNR == null
                || position == null || constrCount == null || violCount == null || violTotal == null || violMax == null
                || violRms == null || violAll == null || violAvViol == null
                /**
                 * || constrLowCount == null || violLowCount == null || violLowTotal == null || violLowMax == null ||
                 * violLowRms == null || violLowAll == null || violLowAvViol == null
                 */
                || cutoff == null) {
            return false;
        }
        return true;
    }

    /** */
    public boolean toXplor(BitSet todo, String fn, String atomNomenclature) {
        int fileCount = 0;
        for (int listRID = todo.nextSetBit(0); listRID >= 0; listRID = todo.nextSetBit(listRID + 1)) {
            fileCount++;
            BitSet ridsCDIH = SQLSelect.selectBitSet(dbms, constr.cdih.mainRelation,
                    Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, new Integer(listRID), false);
            if (ridsCDIH == null) {
                General.showError("Failed to get ridsCDIH for toXplor.");
                return false;
            }
            boolean sortRestraints = true;
            boolean status = constr.cdih.toXplorOrSo(ridsCDIH, fn, fileCount, atomNomenclature, sortRestraints, null);
            if (!status) {
                General.showError("Failed cdih.toXplor");
                General.showError("Not writing any more cdihLists");
                return false;
            }
        }
        return true;
    }

    public BitSet getCDIHRidsInListAndTodo(int ridList, BitSet todoCDIH) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms, constr.cdih.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(ridList), false);
        if (result == null) {
            General.showError("Failed to SQLSelect.selectBitSet in getCDIHRidsInListAndTodo");
            return null;
        }
        result.and(todoCDIH);
        return result;
    }

    /**
     * Calculates the violation for given list and only consider restraints given in the todoCDIH set. If the cutoff is
     * Defs.NULL then it will be assumed to be 5 degrees. It sets list properties too. Note that the value is given in
     * degrees whereas internally Wattos calculates in radians.
     */
    public boolean calcViolation(BitSet todoCDIH, int currentCDIHListId, float cutoffValue) {
        cdih = constr.cdih; // get a ref now because it was hard before.
        if (Defs.isNull(cutoffValue)) {
            cutoffValue = 5;
        }
        cutoffValue /= Geometry.CF; // stored in radians. only at time of write out translated to degrees.

        BitSet selectedModels = gumbo.model.selected;
        BitSet todoCDIHFiltered = getCDIHRidsInListAndTodo(currentCDIHListId, todoCDIH);
        if (todoCDIHFiltered == null) {
            General.showError("Failed to getCDIHRidsInListAndTodo");
            return false;
        }
        int count = todoCDIHFiltered.cardinality();
        int selectedModelsCount = selectedModels.cardinality();
        if (selectedModelsCount < 1) {
            General.showWarning("No models selected in CdihList.calcViolation.");
            return true;
        }
        float[][] violationMatrix = new float[count][selectedModelsCount];

        General.showDebug("Calculating violations for cdihs numbering: " + count + " in models numbering: "
                + selectedModelsCount);
        // General.showDebug("cdihs   : " + PrimitiveArray.toString( todoCDIHFiltered));
        int[] selectedModelArray = PrimitiveArray.toIntArray(selectedModels); // for efficiency.
        // General.showDebug("Models: " + PrimitiveArray.toString( selectedModelArray ));

        if (count == 0) {
            General.showWarning("No constraints selected in CdihList.calcViolation.");
            return true;
        }

        // Remove existing violation records for this list if they exist.
        if (!removeViolationsByListId(currentCDIHListId)) {
            General.showError("Failed to get all indexes.");
            return false;
        }

        int currentCDIHViolId = 0;
        int cdihCount = 0;
        int violationCount = 0;
        BitSet unlinkedAtomSelected = (BitSet) cdih.hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and(todoCDIHFiltered);
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if (unlinkedAtomCount > 0) {
            General.showWarning("Will skip violation calculation for number of constraints: " + unlinkedAtomCount
                    + " because not all atoms are linked for them.");
        }
        // FOR EACH CONSTRAINT
        for (int currentCDIHId = todoCDIHFiltered.nextSetBit(0); currentCDIHId >= 0; currentCDIHId = todoCDIHFiltered.nextSetBit(currentCDIHId + 1)) {
//            float[] valueList = cdih.calcValue(currentCDIHId, selectedModelArray);
            float[][] valueLoL = cdih.calcValue(currentCDIHId, selectedModelArray);
            // structure: valueLoL[swapIdx][modelIdx]
            if (valueLoL == null) {
                General.showError("Failed to calculate values for restraint:\n" + toString(currentCDIHId));
                return false;
            }
            if (Defs.isNull(valueLoL[0][0])) { // By contract the first/first element in the list can contain an error message.
                General.showError("The restraint has unlinked atoms or a failure to find all atoms for restraint:\n"
                        + toString(currentCDIHId));
                return false;
            }

            float low = cdih.lowBound[currentCDIHId]; // cache some variables for speed and convenience.
            float upp = cdih.uppBound[currentCDIHId];
//            General.showDebug("**** Found upp, low: " + Math.toDegrees(upp) + ", " + Math.toDegrees(low));

            cdih.violUppMax[currentCDIHId] = -1f;
            cdih.violLowMax[currentCDIHId] = -1f;
            int valueLoLLength = valueLoL.length;
            for (int currentModelId = 0; currentModelId < selectedModelArray.length; currentModelId++) {
                float[] violList = new float[valueLoLLength];
                for (int swapIdx = 0; swapIdx < valueLoLLength; swapIdx++ ) {
                    float value = valueLoL[swapIdx][currentModelId];
                    if (Defs.isNull(value)) {
                        General.showError("Failed to calculate the value for constraint (swapIdx:"+swapIdx+"): " +
                                currentCDIHId + " in model: " + (currentModelId + 1) + " will NOT try other models.");
                        return false;
                    }
                    violList[swapIdx] = (float) Geometry.violationAngles((double) low, (double) upp, (double) value);
                }
                int swapIdxSelected = 0;
                if ( valueLoLLength > 1 ) {
                    float af0 = Math.abs(violList[0]); // absolute float value for swap state 0
                    float af1 = Math.abs(violList[1]);
                    if ( ! Defs.isNull(af1)) {
                        if ( af1 < af0 ) {
                            swapIdxSelected = 1;
                        }
                    }
                }
                float value = valueLoL[swapIdxSelected][currentModelId];
                float viol = violList[swapIdxSelected];

                boolean[] isViol = Geometry.isLowUppViolationAngles((double) low, (double) upp, (double) value,
                        (double) viol);
                boolean isLowViol = isViol[0];
                boolean isUppViol = isViol[1];
//                General.showDebug("**** Found value, viol: " + Math.toDegrees(value) + ", " + Math.toDegrees(viol) );
                currentCDIHViolId = cdih.simpleConstrViol.getNextReservedRow(currentCDIHViolId);
                // Check if the relation grew in size because not all relations can be adequately estimated.
                if (currentCDIHViolId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW) {
                    if (!cdih.resetConvenienceVariables()) {
                        General.showCodeBug("Failed to resetConvenienceVariables.");
                        return false;
                    }
                    currentCDIHViolId = cdih.simpleConstrViol.getNextReservedRow(0); // now it should be fine.
                }
                if (currentCDIHViolId < 0) {
                    General.showCodeBug("Failed to get next reserved row in simple constraint violation table.");
                    return false;
                }
                cdih.modelIdViol[currentCDIHViolId] = selectedModelArray[currentModelId];
                cdih.scMainIdViol[currentCDIHViolId] = currentCDIHId;
                cdih.scListIdViol[currentCDIHViolId] = cdih.scMainIdViol[currentCDIHId];
                cdih.entryIdViol[currentCDIHViolId] = cdih.entryIdMain[currentCDIHId];
                cdih.value[currentCDIHViolId] = value;
                cdih.violation[currentCDIHViolId] = viol;

                if (viol > 0) {
                    violationCount++;
                    int modelNumber = gumbo.model.number[selectedModelArray[currentModelId]];
                    if (isUppViol && (cdih.violUppMax[currentCDIHId] < viol)) {
                        cdih.violUppMax[currentCDIHId] = viol;
                        cdih.violUppMaxModelNum[currentCDIHId] = modelNumber;
                    }
                    if (isLowViol && (cdih.violLowMax[currentCDIHId] < viol)) {
                        cdih.violLowMax[currentCDIHId] = viol;
                        cdih.violLowMaxModelNum[currentCDIHId] = modelNumber;
                    }
                }
                violationMatrix[cdihCount][currentModelId] = viol;
            } // end of loop per model
            cdihCount++;
        } // end of loop per constraint
        cdih.simpleConstrViol.cancelAllReservedRows();

        float[] violArray = PrimitiveArray.toFloatArray(violationMatrix);
        float[] avSd = Statistics.getAvSd(violArray);
        float sum = Statistics.getSum(violArray);

        constrCount[currentCDIHListId] = count;
        violCount[currentCDIHListId] = violationCount;
        violTotal[currentCDIHListId] = sum * selectedModelsCount; // why times models? TODO: check this.
        violMax[currentCDIHListId] = Statistics.getMax(violArray);
        violRms[currentCDIHListId] = avSd[1];
        violAll[currentCDIHListId] = avSd[0];
        violAvViol[currentCDIHListId] = sum / violationCount;
        cutoff[currentCDIHListId] = cutoffValue;

        // General.showDebug("Calculated values for number of constraints: " + cdihCount + " in number of models: " +
        // selectedModelArray.length);
        General.showOutput("Max violation in any model: " + Geometry.CF * violMax[currentCDIHListId]);
        return true;
    }

    /**
     * Returns a saveframe or null for error even if there are no given restraints. Showtheo is not implemented yet.
     * showViolations is always done. Make sure that the violations have been gathered before. DistConstr.calcViolation
     */
    public SaveFrame toSTAR(BitSet todoCDIH, int listNumber, int currentCDIHListId, boolean showViolations,
            float cutoffValue) {
        cdih = constr.cdih;
        starDict = dbms.ui.wattosLib.starDictionary;
        if (!initConvenienceVariablesStar()) {
            General.showError("Failed initConvenienceVariablesStar");
            return null;
        }

        SaveFrame sF = getSFTemplate();
        if (sF == null) {
            General.showError("Failed to getSFTemplate.");
            return null;
        }

        int entryId = gumbo.entry.getEntryId();
        if (!gumbo.entry.modelsSynced.get(entryId)) { // should always be returning true
            General.showError("Failed to get synced models as required for toSTAR.");
            return null;
        }

        IndexSortedInt indexListMain = (IndexSortedInt) cdih.mainRelation.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        if (indexListMain == null) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        int currentCDIHId = Defs.NULL_INT;
        // int currentCDIHEntryId = Defs.NULL_INT;
        BitSet todoList = indexListMain.getRidListForQuery(SQLSelect.OPERATION_TYPE_EQUALS, new Integer(
                currentCDIHListId));
        if (todoList == null) {
            General.showError("Failed to get todo list inside");
            return null;
        }
        if (todoList.cardinality() == 0) {
            General.showCodeBug("No distance constraints selected for list in toSTAR.");
            return null;
        }

        todoList.and(todoCDIH); // disable those that aren't selected in the first place.

        int count = todoList.cardinality();
        if (count == 0) {
            General.showCodeBug("No filtered distance constraints selected for list in toSTAR.");
            return null;
        }

        if (showViolations) {
            cdih.calcViolation(todoList, cutoffValue);
        }

        BitSet unlinkedAtomSelected = (BitSet) cdih.hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and(todoList);
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if (unlinkedAtomCount > 0) {
            General.showWarning("Skipping toString for " + unlinkedAtomCount
                    + " constraints because not all their atoms are linked -1-.");
            return null;
        } else {
            // General.showDebug("Found no unlinked atoms in constraints." );
        }

        // INTRO
        TagTable tT = (TagTable) sF.get(0);
        // int rowIdx = tT.getNewRowId(); // do no error handling.
        int rowIdx = 0;
        float violTotalDeg = (float) (violTotal[currentCDIHListId] * Geometry.CF);
        float violMaxDeg = (float) (violMax[currentCDIHListId] * Geometry.CF);
        float violRmsDeg = (float) (violRms[currentCDIHListId] * Geometry.CF);
        float violAllDeg = (float) (violAll[currentCDIHListId] * Geometry.CF);
        float violAvViolDeg = (float) (violAvViol[currentCDIHListId] * Geometry.CF);
        float cutoffDeg = (float) (cutoff[currentCDIHListId] * Geometry.CF);

        tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID, 0);
        // tT.setValue(rowIdx, tagNameTA_constraint_stats_list_ID ,listNumber);
        // tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Constraint_file_ID ,"1");
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Constraint_count, count);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_count, violCount[currentCDIHListId]);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_total, violTotalDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_max, violMaxDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_rms, violRmsDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_average_all_restraints, violAllDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Viol_average_violations_only, violAvViolDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Cutoff_violation_report, cutoffDeg);
        tT.setValue(rowIdx, tagNameTA_constraint_stats_list_Details, explanation);

        // LISTING
        tT = (TagTable) sF.get(1);
        int cdihCount = 0;
        for (currentCDIHId = todoList.nextSetBit(0); currentCDIHId >= 0; currentCDIHId = todoList
                .nextSetBit(currentCDIHId + 1)) {
            if (!cdih.toSTAR(tT, currentCDIHId, currentCDIHListId, cdihCount, listNumber)) {
                General.showError("Failed cdih.toSTAR for restraint: " + cdih.toString(currentCDIHId));
                return null;
            }
            cdihCount++;
        }
        return sF;
    }

    /**
     * Returns a template with the star formatted output template
     */
    private SaveFrame getSFTemplate() {
        SaveFrame sF = new SaveFrame();
        sF.setTitle("torsion_angle_constraint_statistics");
        // Default variables.
        HashMap namesAndTypes;
        ArrayList order;
        HashMap namesAndValues;
        TagTable tT;
        // int RID;
        try {
            // INTRO
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            DBMS dbms = new DBMS(); // Use a temporary dbms for this because we don't
            // want to hold on to this data for ever.
            tT = new TagTable("TA_viol_list", dbms);
            tT.isFree = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            String cat = sF.title;
            namesAndValues.put(tagNameTA_constraint_stats_list_Sf_category, cat);
            // namesAndValues.put( tagNameTA_constraint_stats_list_Constraint_file_ID , new Integer(1));

            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Sf_category);
            // starDict.putFromDict( namesAndTypes, order, tagNameTA_constraint_stats_list_Entry_ID );
            // starDict.putFromDict( namesAndTypes, order, tagNameTA_constraint_stats_list_ID );
            // starDict.putFromDict( namesAndTypes, order, tagNameTA_constraint_stats_list_Constraint_file_ID );
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Constraint_count);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_count);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_total);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_max);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_rms);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_average_all_restraints);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Viol_average_violations_only);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Cutoff_violation_report);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_list_Details);

            // Append columns after order id column.
            if (!tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add(tT);

            // BY RESTRAINT
            namesAndTypes = new HashMap();
            order = new ArrayList();
            namesAndValues = new HashMap();
            tT = new TagTable("CDIH_viol", dbms);
            tT.isFree = false;

            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Restraint_ID);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Torsion_angle_name);

            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Entity_assembly_ID_1);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_index_ID_1);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_ID_1);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Atom_ID_1);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Entity_assembly_ID_2);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_index_ID_2);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_ID_2);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Atom_ID_2);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Entity_assembly_ID_3);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_index_ID_3);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_ID_3);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Atom_ID_3);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Entity_assembly_ID_4);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_index_ID_4);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Comp_ID_4);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Atom_ID_4);

            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Angle_lower_bound_val);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Angle_upper_bound_val);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Angle_average);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Angle_minimum);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Angle_maximum);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Max_violation);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Max_violation_model_number);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Above_cutoff_violation_count);
            starDict.putFromDict(namesAndTypes, order, tagNameTA_constraint_stats_Above_cutoff_violation_per_model);
            // starDict.putFromDict( namesAndTypes, order, tagNameTA_constraint_stats_Entry_ID );
            // starDict.putFromDict( namesAndTypes, order, tagNameTA_constraint_stats_TA_constraint_stats_list_ID );

            if (!tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add(tT);
        } catch (Exception e) {
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
            starDict = dbms.ui.wattosLib.starDictionary;
            tagNameTA_constraint_stats_list_Sf_category = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Sf_category                 ");
            // tagNameTA_constraint_stats_list_Entry_ID = starDict.getTagName(
            // "torsion_angle_constraint_statistics","_TA_constraint_stats_list.Entry_ID                    ");
            // tagNameTA_constraint_stats_list_ID = starDict.getTagName(
            // "torsion_angle_constraint_statistics","_TA_constraint_stats_list.ID                          ");
            // tagNameTA_constraint_stats_list_Constraint_file_ID = starDict.getTagName(
            // "torsion_angle_constraint_statistics","_TA_constraint_stats_list.Constraint_file_ID          ");
            tagNameTA_constraint_stats_list_Constraint_count = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats_list.Constraint_count            ");
            tagNameTA_constraint_stats_list_Viol_count = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Viol_count                  ");
            tagNameTA_constraint_stats_list_Viol_total = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Viol_total                  ");
            tagNameTA_constraint_stats_list_Viol_max = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Viol_max                    ");
            tagNameTA_constraint_stats_list_Viol_rms = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Viol_rms                    ");
            tagNameTA_constraint_stats_list_Viol_average_all_restraints = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats_list.Viol_average_all_restraints ");
            tagNameTA_constraint_stats_list_Viol_average_violations_only = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats_list.Viol_average_violations_only");
            tagNameTA_constraint_stats_list_Cutoff_violation_report = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats_list.Cutoff_violation_report     ");
            tagNameTA_constraint_stats_list_Details = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats_list.Details                     ");
            tagNameTA_constraint_stats_Restraint_ID = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Restraint_ID                     ");
            tagNameTA_constraint_stats_Torsion_angle_name = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Torsion_angle_name               ");
            tagNameTA_constraint_stats_Entity_assembly_ID_1 = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Entity_assembly_ID_1             ");
            tagNameTA_constraint_stats_Comp_index_ID_1 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_index_ID_1                  ");
            tagNameTA_constraint_stats_Comp_ID_1 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_ID_1                        ");
            tagNameTA_constraint_stats_Atom_ID_1 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Atom_ID_1                        ");
            tagNameTA_constraint_stats_Entity_assembly_ID_2 = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Entity_assembly_ID_2             ");
            tagNameTA_constraint_stats_Comp_index_ID_2 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_index_ID_2                  ");
            tagNameTA_constraint_stats_Comp_ID_2 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_ID_2                        ");
            tagNameTA_constraint_stats_Atom_ID_2 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Atom_ID_2                        ");
            tagNameTA_constraint_stats_Entity_assembly_ID_3 = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Entity_assembly_ID_3             ");
            tagNameTA_constraint_stats_Comp_index_ID_3 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_index_ID_3                  ");
            tagNameTA_constraint_stats_Comp_ID_3 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_ID_3                        ");
            tagNameTA_constraint_stats_Atom_ID_3 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Atom_ID_3                        ");
            tagNameTA_constraint_stats_Entity_assembly_ID_4 = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Entity_assembly_ID_4             ");
            tagNameTA_constraint_stats_Comp_index_ID_4 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_index_ID_4                  ");
            tagNameTA_constraint_stats_Comp_ID_4 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Comp_ID_4                        ");
            tagNameTA_constraint_stats_Atom_ID_4 = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Atom_ID_4                        ");
            tagNameTA_constraint_stats_Angle_lower_bound_val = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Angle_lower_bound_val            ");
            tagNameTA_constraint_stats_Angle_upper_bound_val = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Angle_upper_bound_val            ");
            tagNameTA_constraint_stats_Angle_average = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Angle_average                    ");
            tagNameTA_constraint_stats_Angle_minimum = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Angle_minimum                    ");
            tagNameTA_constraint_stats_Angle_maximum = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Angle_maximum                    ");
            tagNameTA_constraint_stats_Max_violation = starDict.getTagName("torsion_angle_constraint_statistics",
                    "_TA_constraint_stats.Max_violation                    ");
            tagNameTA_constraint_stats_Max_violation_model_number = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Max_violation_model_number       ");
            tagNameTA_constraint_stats_Above_cutoff_violation_count = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Above_cutoff_violation_count     ");
            tagNameTA_constraint_stats_Above_cutoff_violation_per_model = starDict.getTagName(
                    "torsion_angle_constraint_statistics", "_TA_constraint_stats.Above_cutoff_violation_per_model ");
            // tagNameTA_constraint_stats_Entry_ID = starDict.getTagName(
            // "torsion_angle_constraint_statistics","_TA_constraint_stats.Entry_ID                         ");
            // tagNameTA_constraint_stats_TA_constraint_stats_list_ID = starDict.getTagName(
            // "torsion_angle_constraint_statistics","_TA_constraint_stats.TA_constraint_stats_list_ID      ");
        } catch (Exception e) {
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            General.showThrowable(e);
            return false;
        }
        if (tagNameTA_constraint_stats_list_Sf_category == null
                ||
                // tagNameTA_constraint_stats_list_Entry_ID == null ||
                // tagNameTA_constraint_stats_list_ID == null ||
                // tagNameTA_constraint_stats_list_Constraint_file_ID == null ||
                tagNameTA_constraint_stats_list_Constraint_count == null
                || tagNameTA_constraint_stats_list_Viol_count == null
                || tagNameTA_constraint_stats_list_Viol_total == null
                || tagNameTA_constraint_stats_list_Viol_max == null || tagNameTA_constraint_stats_list_Viol_rms == null
                || tagNameTA_constraint_stats_list_Viol_average_all_restraints == null
                || tagNameTA_constraint_stats_list_Viol_average_violations_only == null
                || tagNameTA_constraint_stats_list_Cutoff_violation_report == null
                || tagNameTA_constraint_stats_list_Details == null || tagNameTA_constraint_stats_Restraint_ID == null
                || tagNameTA_constraint_stats_Torsion_angle_name == null
                || tagNameTA_constraint_stats_Entity_assembly_ID_1 == null
                || tagNameTA_constraint_stats_Comp_index_ID_1 == null || tagNameTA_constraint_stats_Comp_ID_1 == null
                || tagNameTA_constraint_stats_Atom_ID_1 == null
                || tagNameTA_constraint_stats_Entity_assembly_ID_2 == null
                || tagNameTA_constraint_stats_Comp_index_ID_2 == null || tagNameTA_constraint_stats_Comp_ID_2 == null
                || tagNameTA_constraint_stats_Atom_ID_2 == null
                || tagNameTA_constraint_stats_Entity_assembly_ID_3 == null
                || tagNameTA_constraint_stats_Comp_index_ID_3 == null || tagNameTA_constraint_stats_Comp_ID_3 == null
                || tagNameTA_constraint_stats_Atom_ID_3 == null
                || tagNameTA_constraint_stats_Entity_assembly_ID_4 == null
                || tagNameTA_constraint_stats_Comp_index_ID_4 == null || tagNameTA_constraint_stats_Comp_ID_4 == null
                || tagNameTA_constraint_stats_Atom_ID_4 == null
                || tagNameTA_constraint_stats_Angle_lower_bound_val == null
                || tagNameTA_constraint_stats_Angle_upper_bound_val == null
                || tagNameTA_constraint_stats_Angle_average == null || tagNameTA_constraint_stats_Angle_minimum == null
                || tagNameTA_constraint_stats_Angle_maximum == null || tagNameTA_constraint_stats_Max_violation == null
                || tagNameTA_constraint_stats_Max_violation_model_number == null
                || tagNameTA_constraint_stats_Above_cutoff_violation_count == null
                || tagNameTA_constraint_stats_Above_cutoff_violation_per_model == null
        // tagNameTA_constraint_stats_Entry_ID == null ||
        // tagNameTA_constraint_stats_TA_constraint_stats_list_ID == null
        ) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary.");
            return false;
        }
        return true;
    }

    /**
     * Remove existing violation records for this list if they exist.
     */
    public boolean removeViolationsByListId(int currentCDIHListId) {
        BitSet cdihViolRidSet = getCDIHViolRidsByListId(currentCDIHListId);
        if (cdihViolRidSet == null) {
            General.showError("Failed to getCDIHRidsInListAndTodo in removeViolationsByListId");
            return false;
        }
        if (cdihViolRidSet.cardinality() != 0) {
            // General.showDebug("Will remove old list of violations by list id numbered: " +
            // cdihViolRidSet.cardinality());
        }
        return constr.cdih.simpleConstrViol.removeRowsCascading(cdihViolRidSet, false);
    }

    public BitSet getCDIHViolRidsByListId(int ridList) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms, constr.cdih.simpleConstrViol,
                Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(ridList), false);
        if (result == null) {
            General.showError("Failed to SQLSelect.selectBitSet in getCDIHViolRidsByListId");
            return null;
        }
        return result;
    }
}
