/*
 */

package Wattos.Soup.Constraint;

import java.io.*;
import java.util.*;
import Wattos.Soup.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Database.Indices.*;
import Wattos.Star.*;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.Wiskunde.Statistics;


/**A list of distance restraints. Contains method to write the violation STAR
 *statistics.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class DistConstrList extends ConstrItem implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;
    
    /** The averaging methods known in Wattos.
     */
    public static HashMap logicalOperationString2Int= new HashMap(); // To get linear look up times.
    public static int DEFAULT_AVERAGING_METHOD_CENTER               = 0;
    public static int DEFAULT_AVERAGING_METHOD_SUM                  = 1;
    public static int DEFAULT_AVERAGING_METHOD_R6                   = 2;
    public static String[] DEFAULT_AVERAGING_METHOD_NAMES          = { "center", "sum", "R^-6"};
//    public static int DEFAULT_AVERAGING_METHOD                     = DEFAULT_AVERAGING_METHOD_CENTER;
    /** Set to sum averaging as center averaging fails for ambis */
    public static int DEFAULT_AVERAGING_METHOD                     = DEFAULT_AVERAGING_METHOD_SUM;
    public static int DEFAULT_AVERAGING_MONOMER_COUNT              = 1;
    
    public static String[] DEFAULT_TYPE_LIST = new String[] {
        "unknown",
        "disulfide bond",          //Disulfide bond distance constraints" 
        "general distance",        //Specific distance constraint is not known" 
        "hydrogen bond",           //Hydrogen bond distance constraints" 
        "NOE",                     //Distance constraints determined from NOE experiments" 
        "NOE build-up",            //Distance constraints determined from a series of NOE experiments with different mixing times" 
        "NOE not seen",            //Distance constraints supplied when an expected NOE was not observed" 
        "paramagnetic relaxation", //Distance constraints determined from paramagnetic relaxation studies" 
        "ROE",                     //Distance constraints determined from ROE experiments" 
        "ROE build-up",            //Distance constraints determine from a series of ROE experiments with different mixing times" 
        "symmetry",                //Distance constraint enforces symmetry - for example to enforce symmetry between monomers in a multimer"                 
    };
    public static int DEFAULT_TYPE_UNKNOWN = 0; // Matches above
    public static StringArrayList DEFAULT_TYPE_ARRAYLIST = null;
    /** Convenience variables */
    public int[]       entry_id;                    // starting with fkcs
    
    public String[]    subTypeList;
    public StringSet   subTypeListNR;
    public String[]    formatList;
    public StringSet   formatListNR;
    public String[]    programList;
    public StringSet   programListNR;
    public int[]       position;
    
    public int[]       constrCount;
    public int[]       violCount;
    public float[]     violTotal;
    public float[]     violMax;
    public float[]     violRms;
    public float[]     violAll;
    public float[]     violAvViol;
    
    /**
     * public int[]       constrLowCount;
     * public int[]       violLowCount;
     * public float[]     violLowTotal;
     * public float[]     violLowMax;
     * public float[]     violLowRms;
     * public float[]     violLowAll;
     * public float[]     violLowAvViol;
     */
    
    public int[]       avgMethod;
    public int[]       numberMonomers;
    public BitSet      pseudoCorNeeded;
    public int[]       floatingChirality;
    public float[]     cutoff;
    
    public DistConstr dc = null;
    public StarDictionary starDict;
    
    public String tagNameDCStats_Sf_category;
//    public String tagNameDCStatsL_Entry_ID;
//    public String tagNameDCStatsL_Distance_constraint_stats_ID;
    public String tagNameDCStats_Restraint_list_ID;
    public String tagNameDCStats_Restraint_count;
    public String tagNameDCStats_Viol_count;
    public String tagNameDCStats_Viol_total;
    public String tagNameDCStats_Viol_max;
    public String tagNameDCStats_Viol_rms;
    public String tagNameDCStats_Viol_average_all_restraints;
    public String tagNameDCStats_Viol_average_violations_only;
    public String tagNameDCStats_Cutoff_violation_report;
    public String tagNameDCStats_Details;
    public String tagNameDCStats_Restraint_ID;
    public String tagNameDCStats_Atom_1_entity_ID;
    public String tagNameDCStats_Atom_1_comp_index_ID;
    public String tagNameDCStats_Atom_1_comp_ID;
    public String tagNameDCStats_Atom_1_ID;
    public String tagNameDCStats_Atom_2_entity_ID;
    public String tagNameDCStats_Atom_2_comp_index_ID;
    public String tagNameDCStats_Atom_2_comp_ID;
    public String tagNameDCStats_Atom_2_ID;
    public String tagNameDCStats_Node_1_distance_val;
    public String tagNameDCStats_Node_1_distance_lower_bound_val;
    public String tagNameDCStats_Node_1_distance_upper_bound_val;
    public String tagNameDCStats_Distance_minimum;
    public String tagNameDCStats_Distance_average;
    public String tagNameDCStats_Distance_maximum;
    public String tagNameDCStats_Max_violation;
    public String tagNameDCStats_Max_violation_model_number;
    public String tagNameDCStats_Above_cutoff_violation_count;
    public String tagNameDCStats_Above_cutoff_violation_per_model;
//    public String tagNameDCStats_Entry_ID;
    public String tagNameDCStats_Distance_constraint_stats_ID;
    public String tagNameDCPer_resAtom_entity_ID;
    public String tagNameDCPer_resAtom_comp_index_ID;
    public String tagNameDCPer_resAtom_comp_ID;
    public String tagNameDCPer_resTotal_violation;
    public String tagNameDCPer_resMax_violation;
    public String tagNameDCPer_resMax_violation_model_number;
    public String tagNameDCPer_resAbove_cutoff_violation_count;
    public String tagNameDCPer_resAbove_cutoff_violation_per_model;
//    public String tagNameDCPer_resEntry_ID;
//    public String tagNameDCPer_resDistance_constraint_stats_ID;
    
    public static String explanation = null;
    
    static {
        DEFAULT_TYPE_ARRAYLIST = new StringArrayList( PrimitiveArray.toArrayList(DEFAULT_TYPE_LIST));
        int i=1;
        explanation =
                "\nDescription of the tags in this list:\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * ID of the restraint list.                                                              \n" +
                "*  "+i+++" * Number of restraints in list.                                                          \n" +
                "*  "+i+++" * Number of violated restraints (each model violation is used).                          \n" +
                "*  "+i+++" * Sum of violations in Angstrom.                                                         \n" +
                "*  "+i+++" * Maximum violation of a restraint without averaging in any way.                         \n" +
                "*  "+i+++" * Rms of violations over all restraints.                                                 \n" +
                "*  "+i+++" * Average violation over all restraints.                                                 \n" +
                "*  "+i+++" * Average violation over violated restraints.                                            \n" +
                "           This violation is averaged over only those models in which the restraint is violated.   \n" +
                "           These definitions are from: Doreleijers, et al., J. Mol. Biol. 281, 149-164 (1998).     \n" +
                "*  "+i+++" * Threshold for reporting violations (in Angstrom) in the last columns of the next table.\n" +
                "*  "+i+++" * This tag                                                                               \n";
        i = 1;
        explanation = explanation +
                "\nDescription of the tags in the per residue table below:\n" +
                "*  "+i+++" * Chain identifier (can be absent if none defined)                   \n" +
                "*  "+i+++" * Residue number                                                     \n" +
                "*  "+i+++" * Residue name                                                       \n" +
                "*  "+i+++" * Maximum violation in ensemble of models (without any averaging)\n" +
                "*  "+i+++" * Model number with the maximum violation\n" +
                "*  "+i+++" * Number of models with a violation above cutoff\n" +
                "*  "+i+++" * List of models (1 character per model) with a violation above cutoff.\n" +
                "           An '*' marks a violation above the cutoff. A '+' indicates the largest\n"+
                "           violation above the cutoff and a '-' marks the smallest violation over cutoff.\n" +
                "           For models  5, 15, 25,... a ' ' is replaced by a '.'.\n" +                
                "           For models 10, 20, 30,... a ' ' is replaced by a digit starting at 1.\n" +                
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag\n";
        i = 1;
        explanation = explanation +
                "\nDescription of the tags in the per restraint table below:\n" +
                "*  "+i+++" * Restraint ID within restraint list.                                \n" +
                "           First node, FIRST member, first atom's:                              \n" +
                "*  "+i+++" * Chain identifier (can be absent if none defined)                   \n" +
                "*  "+i+++" * Residue number                                                     \n" +
                "*  "+i+++" * Residue name                                                       \n" +
                "*  "+i+++" * Name of (pseudo-)atom                                              \n" +
                "           First node, SECOND member, first atom's:                             \n" +
                "*  "+i+++" * Chain identifier (can be absent if none defined)                   \n" +
                "*  "+i+++" * Residue number                                                     \n" +
                "*  "+i+++" * Residue name                                                       \n" +
                "*  "+i+++" * Name of (pseudo-)atom                                              \n" +
                "           FIRST node's:\n" +
                "*  "+i+++" * Target distance value (Angstrom)\n" +
                "*  "+i+++" * Lower bound distance (Angstrom)\n" +
                "*  "+i+++" * Upper bound distance (Angstrom)\n" +
                "*  "+i+++" * Average distance in ensemble of models\n" +
                "*  "+i+++" * Minimum distance in ensemble of models\n" +
                "*  "+i+++" * Maximum distance in ensemble of models\n" +
                "*  "+i+++" * Maximum violation (without any averaging)\n" +
                "*  "+i+++" * Model number with the maximum violation\n" +
                "*  "+i+++" * Number of models with a violation above cutoff\n" +
                "*  "+i+++" * List of models with a violation above cutoff. See description above.\n" +
                "*  "+i+++" * Administrative tag\n" +
                "*  "+i+++" * Administrative tag";
    }
    
    public DistConstrList(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Atom constructor");
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }
    
    /** The relationSetName is a parameter so non-standard relation sets
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public DistConstrList(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Atom constructor");
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        //General.showDebug("now in Atom.init()");
        super.init(dbms);
        //General.showDebug("back in Atom.init()");
        
        name = Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[RELATION_ID_SET_NAME];
        
        // MAIN RELATION in addition to the ones in wattos item.
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        
        DEFAULT_ATTRIBUTES_TYPES.put( Relation.DEFAULT_ATTRIBUTE_COUNT,           new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_CONSTR_COUNT,  new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_COUNT  ,  new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_TOTAL  ,  new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_MAX    ,  new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_RMS    ,  new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_ALL    ,  new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_AV_VIOL,  new Integer(DATA_TYPE_FLOAT));
        /**
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_CONSTR_LOW_COUNT,  new Integer(DATA_TYPE_INT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_COUNT  ,  new Integer(DATA_TYPE_INT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_TOTAL  ,  new Integer(DATA_TYPE_FLOAT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX    ,  new Integer(DATA_TYPE_FLOAT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_RMS    ,  new Integer(DATA_TYPE_FLOAT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_ALL    ,  new Integer(DATA_TYPE_FLOAT));
         * DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_AV_VIOL,  new Integer(DATA_TYPE_FLOAT));
         */
        
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SUB_TYPE,  new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_FORMAT  ,  new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_PROGRAM ,  new Integer(DATA_TYPE_STRINGNR));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_POSITION,  new Integer(DATA_TYPE_INT));
        
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_AVG_METHOD,          new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_NUMBER_MONOMERS,     new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_PSEUDO_COR_NEEDED,   new Integer(DATA_TYPE_BIT));
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_FLOATING_CHIRALITY,  new Integer(DATA_TYPE_INT));
        
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_CUTOFF,              new Integer(DATA_TYPE_FLOAT));
        
        
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME] );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_CONSTR_COUNT );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_COUNT   );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_TOTAL   );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_MAX     );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_RMS     );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_ALL     );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_AV_VIOL );
        /**
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_CONSTR_LOW_COUNT );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_COUNT   );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_TOTAL   );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX     );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_RMS     );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_ALL     );
         * DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_AV_VIOL );
         */
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SUB_TYPE );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_FORMAT );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_PROGRAM );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_POSITION );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_AVG_METHOD );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_NUMBER_MONOMERS );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_PSEUDO_COR_NEEDED );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_FLOATING_CHIRALITY );
        DEFAULT_ATTRIBUTES_ORDER.add( Relation.DEFAULT_ATTRIBUTE_COUNT );
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_CUTOFF );
        
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME]});
        
        Relation relation = null;
        String relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[RELATION_ID_MAIN_RELATION_NAME];
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
        avgMethod        = (int[])       mainRelation.getColumn(    Constr.DEFAULT_ATTRIBUTE_AVG_METHOD       );
        numberMonomers   = (int[])       mainRelation.getColumn(    Constr.DEFAULT_ATTRIBUTE_NUMBER_MONOMERS  );
        pseudoCorNeeded  =               mainRelation.getColumnBit( Constr.DEFAULT_ATTRIBUTE_PSEUDO_COR_NEEDED);
        floatingChirality= (int[])       mainRelation.getColumn(    Constr.DEFAULT_ATTRIBUTE_FLOATING_CHIRALITY);
        
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
                || avgMethod         == null
                || numberMonomers    == null
                || pseudoCorNeeded   == null
                || floatingChirality == null
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
    
    /**
     * @param format TODO */
    public boolean toXplorOrSo(BitSet todo, String fn, String atomNomenclature,
            boolean sortRestraints, String format) {
        dc = constr.dc; // get a ref now because it was hard before.
        int fileCount = 0;
        for (int listRID = todo.nextSetBit(0); listRID >= 0; listRID=todo.nextSetBit(listRID+1)) {
            fileCount++;
            BitSet ridsDC = SQLSelect.selectBitSet(dbms, 
                    dc.mainRelation, 
                    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[RelationSet.RELATION_ID_COLUMN_NAME],
                    SQLSelect.OPERATION_TYPE_EQUALS, 
                    new Integer( listRID ), 
                    false);
            if ( ridsDC == null ) {
                General.showError("Failed to get ridsDC for toXplor.");
                return false;
            }
            boolean status = dc.toXplorOrSo( ridsDC, fn, fileCount, atomNomenclature, sortRestraints, format );
            if ( ! status ) {
                General.showError("Failed dc.toXplor");
                General.showError("Not writing any more dcLists");
                return false;
            }
        }
        return true;
    }
    
    
    public BitSet getDCRidsInListAndTodo(int ridList, BitSet todoDC ) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms,
                constr.dc.mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(ridList),false);
        if ( result == null ) {
            General.showError("Failed to SQLSelect.selectBitSet in getDCRidsInListAndTodo");
            return null;
        }
        result.and(todoDC);
        return result;
    }
    
    public BitSet getDCViolRidsByListId(int ridList ) {
        // Get all restraints in list
        BitSet result = SQLSelect.selectBitSet(dbms,
                constr.dc.distConstrViol,
                Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(ridList),false);
        if ( result == null ) {
            General.showError("Failed to SQLSelect.selectBitSet in getDCViolRidsByListId");
            return null;
        }
        return result;
    }
    
    
    
    /** Remove existing violation records for this list if they exist.
     */
    public boolean removeViolationsByListId( int currentDCListId ) {
        BitSet dcViolRidSet = getDCViolRidsByListId(currentDCListId );        
        if ( dcViolRidSet == null ) {
            General.showError("Failed to getDCRidsInListAndTodo in removeViolationsByListId");
            return false;
        }
        if ( dcViolRidSet.cardinality() != 0 ) {
//            General.showDebug("Will remove old list of violations by list id numbered: " + dcViolRidSet.cardinality());
        }
        return constr.dc.distConstrViol.removeRowsCascading(dcViolRidSet,false);
    }
    
    
    /**
     * Calculates the violation for given list and only consider restraints
     *given in the todoDC set.
     *If the cutoff is Defs.NULL then it will be assumed to be 0.5 Ang. It sets list
     *properties too.
     */
    public boolean calcViolation(BitSet todoDC, int currentDCListId, float cutoffValue ) {
        dc = constr.dc; // get a ref now because it was hard before.
        if ( Defs.isNull(cutoffValue)) {
            cutoffValue = .5f;
        }
        
        BitSet selectedModels = gumbo.model.selected;        
        BitSet todoDCFiltered = getDCRidsInListAndTodo(currentDCListId,todoDC);
        if ( todoDCFiltered == null ) {
            General.showError("Failed to getDCRidsInListAndTodo");
            return false;
        }
//        if ( todoDC.cardinality() != todoDCFiltered.cardinality() ) {
//            General.showDebug("Filtered restraints because todoDC: " + 
//                    todoDC.cardinality() +" and todoDCFiltered: " +  todoDCFiltered.cardinality() );
//        }
        
        int count = todoDCFiltered.cardinality();
        int selectedModelsCount = selectedModels.cardinality();
        if ( selectedModelsCount < 1 ) {
            General.showWarning("No models selected in calcDistance.");
            return true;
        }
        float[][] violationMatrix = new float[count][selectedModelsCount];
        
//        General.showDebug("Calculating violations for dcs numbering: "   + count +
//                " in models numbering: "                      + selectedModelsCount);
//        General.showDebug("Dcs   : " + PrimitiveArray.toString( todoDCFiltered));
        int[] selectedModelArray = PrimitiveArray.toIntArray( selectedModels ); // for efficiency.
//        General.showDebug("Models: " + PrimitiveArray.toString( selectedModelArray ));
        
        if ( count == 0 ) {
            General.showWarning("No distance constraints selected in calcDistance.");
            return true;
        }
        
        // Remove existing violation records for this list if they exist.
        if ( ! removeViolationsByListId(currentDCListId)) {
            General.showError("Failed to get all indexes.");
            return false;
        }
        
        IndexSortedInt indexMembAtom = (IndexSortedInt) dc.distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) dc.distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) dc.distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if (    indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }
        
        int currentDCViolId = 0;        
        int dcCount         = 0;
        int violationCount  = 0;
        BitSet unlinkedAtomSelected = (BitSet) dc.hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and( todoDCFiltered );
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if ( unlinkedAtomCount > 0 ) {
            General.showWarning("Will skip distance calculation for number of constraints: " + unlinkedAtomCount + " because not all atoms are linked for them." );
        } else {
//            General.showDebug("Found no unlinked atoms in the constraints.");
        }
        // FOR EACH CONSTRAINT
        for (int currentDCId = todoDCFiltered.nextSetBit(0);currentDCId>=0;currentDCId = todoDCFiltered.nextSetBit(currentDCId+1)) {
            float[] distanceList = dc.calcDistance( currentDCId, selectedModelArray);
            if ( distanceList == null ) {
                General.showError("Failed to calculate distance for restraint:\n" + toString(currentDCId));
                return false;
            }
            if ( Defs.isNull( distanceList[0])){
                General.showError("The restraint has unlinked atoms or a failure to find all atoms for restraint:\n" + toString(currentDCId));
                return false;
            }
//            int currentDCEntryId = dc.entryIdMain[     currentDCId ];
            
            float[] lowTargetUppBound = dc.getLowTargetUppTheoBound( currentDCId );
            float low = lowTargetUppBound[ 0 ]; // cache some variables for speed and convenience.
            float upp = lowTargetUppBound[ 2 ];
            
            dc.violUppMax[currentDCId] = -1f;
            dc.violLowMax[currentDCId] = -1f;
            // When the set of atoms involved is collected for the first model, the other models can easily be done too.
            // FOR EACH selected MODEL
            for ( int currentModelId=0; currentModelId<selectedModelArray.length; currentModelId++) {
                float dist = distanceList[currentModelId];
                if ( Defs.isNull( dist ) ) {
                    General.showError("Failed to calculate the distance for constraint: " + currentDCId + " in model: " + (currentModelId+1) + " will NOT try other models.");
                    return false;
                }
                float viol = Defs.NULL_FLOAT;  // Will be reset if there are upper and/or lower bounds.
                /** See xplor version 4.0 manual. This can be changed later to accomodate other potential functions.
                 * Always a positive violation!
                 */
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
                currentDCViolId = dc.distConstrViol.getNextReservedRow(currentDCViolId);
                // Check if the relation grew in size because not all relations can be adaquately estimated.
                if ( currentDCViolId == Relation.DEFAULT_VALUE_INDICATION_RELATION_MAX_SIZE_GREW ) {
                    if ( ! dc.resetConvenienceVariables() ) {
                        General.showCodeBug("Failed to resetConvenienceVariables.");
                        return false;
                    }
                    currentDCViolId = dc.distConstrViol.getNextReservedRow(0); // now it should be fine.
                }
                if ( currentDCViolId < 0 ) {
                    General.showCodeBug("Failed to get next reserved row in distance constraint violation table.");
                    return false;
                }
                
                dc.modelIdViol[  currentDCViolId ] = selectedModelArray[ currentModelId ];
                dc.dcMainIdViol[ currentDCViolId ] = currentDCId;
                dc.dcListIdViol[ currentDCViolId ] = dc.dcListIdMain[    currentDCId ];
                dc.entryIdViol[  currentDCViolId ] = dc.entryIdMain[     currentDCId ];
                dc.distance[     currentDCViolId ] = dist;
                dc.violation[    currentDCViolId ] = viol;
                
                int modelNumber = gumbo.model.number[ selectedModelArray[ currentModelId ] ];
                if ( isUppViol && (dc.violUppMax[currentDCId] < viol) ) {
                    dc.violUppMax[currentDCId] = viol;
                    dc.violUppMaxModelNum[currentDCId] = modelNumber;
                }
                if ( isLowViol && (dc.violLowMax[currentDCId] < viol) ) {
                    dc.violLowMax[currentDCId] = viol;
                    dc.violLowMaxModelNum[currentDCId] = modelNumber;
                }
                if ( isUppViol || isLowViol) {
                    violationCount++;
                }
                violationMatrix[dcCount][currentModelId] = viol;
            } // end of loop per model
            
            // check to see if there actually was a viol.
            if ( dc.violUppMax[currentDCId] == -1f ) {
                dc.violUppMax[currentDCId] = Defs.NULL_FLOAT;
            }
            if ( dc.violLowMax[currentDCId] == -1f ) {
                dc.violLowMax[currentDCId] = Defs.NULL_FLOAT;
            }
            dcCount++;
        } // end of loop per constraint
        dc.distConstrViol.cancelAllReservedRows();
        
        float[] violArray   = PrimitiveArray.toFloatArray(  violationMatrix);
        float[] avSd        = Statistics.getAvSd(           violArray);
        float sum           = Statistics.getSum(            violArray);
        
        constrCount[ currentDCListId ] = count;
        violCount  [ currentDCListId ] = violationCount;
        violTotal  [ currentDCListId ] = sum*selectedModelsCount;
        violMax    [ currentDCListId ] = Statistics.getMax(violArray);
        violRms    [ currentDCListId ] = avSd[1];
        violAll    [ currentDCListId ] = avSd[0];
        violAvViol [ currentDCListId ] = sum/violationCount;
        cutoff[      currentDCListId ] = cutoffValue;
        
//        General.showDebug("Calculated distances for number of constraints: " + dcCount + " in number of models: " + selectedModelArray.length);
        return true;
    }
    
    
    /**
     * Returns a saveframe or null for error even if there are no given restraints.
     * Showtheo is not implemented yet. showViolations is always done.
     * Make sure that the violations have been gathered before by:
     * DistConstr.calcViolation
     */
    public SaveFrame toSTAR( BitSet todoDC, int listNumber, int currentDCListId,
            boolean showViolations, boolean showTheo, float cutoffValue ) {
        dc=constr.dc;
        starDict = dbms.ui.wattosLib.starDictionary;
        if ( ! initConvenienceVariablesStar()) {
            General.showError("Failed initConvenienceVariablesStar");
            return null;
        }
        
        SaveFrame sF = getSFTemplate();
        if ( sF == null ) {
            General.showError( "Failed to getSFTemplate.");
            return null;
        }
        
        
        /**
         * float[] upp_theo = null;
         * float[] low_theo = null;
         *
         * if ( mainRelation.containsColumn( DEFAULT_UPP_THEO) &&
         * mainRelation.containsColumn( DEFAULT_LOW_THEO) ) {
         * upp_theo = mainRelation.getColumnFloat(DEFAULT_UPP_THEO);
         * low_theo = mainRelation.getColumnFloat(DEFAULT_LOW_THEO);
         * }
         * boolean containsTheos = true;
         *
         * if (
         * upp_theo          == null ||
         * low_theo          == null
         * ) {
         * containsTheos = false;
         * }
         */
        
        int entryId = gumbo.entry.getEntryId();
        if ( ! gumbo.entry.modelsSynced.get( entryId ) ) { // should always be returning true
            General.showError("Failed to get synced models as required for toSTAR.");
            return null;
        }
        
        IndexSortedInt indexListMain = (IndexSortedInt) dc.mainRelation.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexListMain == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        int currentDCId             = Defs.NULL_INT;
//        int currentDCEntryId        = Defs.NULL_INT;
        BitSet todoList = indexListMain.getRidListForQuery(SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCListId));
        if ( todoList == null ) {
            General.showError("Failed to get todo list inside");
            return null;
        }
        if ( todoList.cardinality() == 0 ) {
            General.showCodeBug("No distance constraints selected for list in toSTAR.");
            return null;
        }
        
        todoList.and(todoDC); // disable those that aren't selected in the first place.
        
        int count = todoList.cardinality();
        if ( count == 0 ) {
            General.showCodeBug("No filtered distance constraints selected for list in toSTAR.");
            return null;
        }
        
        
        if ( showViolations ) {
            dc.calcViolation(todoList, cutoffValue);
        }
        
        BitSet unlinkedAtomSelected = (BitSet) dc.hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and( todoList );
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if ( unlinkedAtomCount > 0 ) {
            General.showWarning("Skipping toString for " + unlinkedAtomCount + " constraints because not all their atoms are linked -1-." );
            return null;
        } else {
//            General.showDebug("Found no unlinked atoms in constraints." );            
        }
        
        
        // INTRO
        TagTable tT = (TagTable) sF.get(0);
        //int rowIdx = tT.getNewRowId(); // do no error handeling.
        int rowIdx = 0;
        tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID , 0);
//        tT.setValue(rowIdx, tagNameDCStatsL_Distance_constraint_stats_ID,listNumber);
        tT.setValue(rowIdx, tagNameDCStats_Restraint_list_ID            ,listNumber);
        tT.setValue(rowIdx, tagNameDCStats_Restraint_count              ,count);
        tT.setValue(rowIdx, tagNameDCStats_Viol_count                   ,violCount[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Viol_total                   ,violTotal[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Viol_max                     ,violMax[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Viol_rms                     ,violRms[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Viol_average_all_restraints  ,violAll[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Viol_average_violations_only ,violAvViol[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Cutoff_violation_report      ,cutoff[currentDCListId]);
        tT.setValue(rowIdx, tagNameDCStats_Details                      ,explanation);
        
        // PER RESIDUE LISTING
        if ( ! setTagTableRes( sF, todoList, cutoff[currentDCListId], listNumber )) {
            General.showError("Failed setTagTableRes");
            return null;
        }
        
        // LISTING
        tT = (TagTable) sF.get(2);
        int dcCount = 0;
        for (currentDCId = todoList.nextSetBit(0);currentDCId>=0;currentDCId = todoList.nextSetBit(currentDCId+1)) {
            if ( ! dc.toSTAR( tT, currentDCId, currentDCListId, dcCount, listNumber )) {
                General.showError("Failed dc.toSTAR for restraint: " + dc.toString(currentDCId));
                return null;
            }
            dcCount++;
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
            tagNameDCStats_Sf_category                      = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Sf_category                 ");
//            tagNameDCStatsL_Entry_ID                        = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Entry_ID                    ");
//            tagNameDCStatsL_Distance_constraint_stats_ID    = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Distance_constraint_stats_ID");
            tagNameDCStats_Restraint_list_ID                = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Restraint_list_ID           ");
            tagNameDCStats_Restraint_count                  = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Restraint_count             ");
            tagNameDCStats_Viol_count                       = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_count                  ");
            tagNameDCStats_Viol_total                       = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_total                  ");
            tagNameDCStats_Viol_max                         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_max                    ");
            tagNameDCStats_Viol_rms                         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_rms                    ");
            tagNameDCStats_Viol_average_all_restraints      = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_average_all_restraints ");
            tagNameDCStats_Viol_average_violations_only     = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Viol_average_violations_only");
            tagNameDCStats_Cutoff_violation_report          = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Cutoff_violation_report     ");
            tagNameDCStats_Details                          = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_list.Details                     ");
            tagNameDCStats_Restraint_ID                     = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Restraint_ID                     ");
            tagNameDCStats_Atom_1_entity_ID                 = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_1_entity_ID                 ");
            tagNameDCStats_Atom_1_comp_index_ID             = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_1_comp_index_ID             ");
            tagNameDCStats_Atom_1_comp_ID                   = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_1_comp_ID                   ");
            tagNameDCStats_Atom_1_ID                        = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_1_ID                        ");
            tagNameDCStats_Atom_2_entity_ID                 = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_2_entity_ID                 ");
            tagNameDCStats_Atom_2_comp_index_ID             = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_2_comp_index_ID             ");
            tagNameDCStats_Atom_2_comp_ID                   = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_2_comp_ID                   ");
            tagNameDCStats_Atom_2_ID                        = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Atom_2_ID                        ");
            tagNameDCStats_Node_1_distance_val              = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Node_1_distance_val              ");
            tagNameDCStats_Node_1_distance_lower_bound_val  = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Node_1_distance_lower_bound_val  ");
            tagNameDCStats_Node_1_distance_upper_bound_val  = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Node_1_distance_upper_bound_val  ");
            tagNameDCStats_Distance_minimum                 = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Distance_minimum                 ");
            tagNameDCStats_Distance_average                 = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Distance_average                 ");
            tagNameDCStats_Distance_maximum                 = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Distance_maximum                 ");
            tagNameDCStats_Max_violation                    = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Max_violation                    ");
            tagNameDCStats_Max_violation_model_number       = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Max_violation_model_number       ");
            tagNameDCStats_Above_cutoff_violation_count     = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Above_cutoff_violation_count     ");
            tagNameDCStats_Above_cutoff_violation_per_model = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Above_cutoff_violation_per_model ");
//            tagNameDCStats_Entry_ID                         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Entry_ID                         ");
            tagNameDCStats_Distance_constraint_stats_ID     = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats.Distance_constraint_stats_ID     ");
            tagNameDCPer_resAtom_entity_ID                       = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Atom_entity_ID                  ");
            tagNameDCPer_resAtom_comp_index_ID                   = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Atom_comp_index_ID              ");
            tagNameDCPer_resAtom_comp_ID                         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Atom_comp_ID                    ");
            tagNameDCPer_resTotal_violation                      = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Total_violation                   ");
            tagNameDCPer_resMax_violation                        = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Max_violation                   ");
            tagNameDCPer_resMax_violation_model_number           = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Max_violation_model_number      ");
            tagNameDCPer_resAbove_cutoff_violation_count         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Above_cutoff_violation_count    ");
            tagNameDCPer_resAbove_cutoff_violation_per_model     = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Above_cutoff_violation_per_model");
//            tagNameDCPer_resEntry_ID                             = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Entry_ID                        ");
//            tagNameDCPer_resDistance_constraint_stats_ID         = starDict.getTagName( "distance_constraint_statistics","_Distance_constraint_stats_per_res.Distance_constraint_stats_ID    ");
            
        } catch ( Exception e ) {
            General.showError("Failed to get all the tag names from dictionary compare code with dictionary");
            General.showThrowable(e);
            return false;
        }
        if (
                tagNameDCStats_Sf_category  == null ||
//                tagNameDCStatsL_Entry_ID  == null ||
//                tagNameDCStatsL_Distance_constraint_stats_ID  == null ||
                tagNameDCStats_Restraint_list_ID  == null ||
                tagNameDCStats_Restraint_count  == null ||
                tagNameDCStats_Viol_count  == null ||
                tagNameDCStats_Viol_total  == null ||
                tagNameDCStats_Viol_max  == null ||
                tagNameDCStats_Viol_rms  == null ||
                tagNameDCStats_Viol_average_all_restraints  == null ||
                tagNameDCStats_Viol_average_violations_only  == null ||
                tagNameDCStats_Cutoff_violation_report  == null ||
                tagNameDCStats_Details  == null ||
                tagNameDCStats_Restraint_ID  == null ||
                tagNameDCStats_Atom_1_entity_ID  == null ||
                tagNameDCStats_Atom_1_comp_index_ID  == null ||
                tagNameDCStats_Atom_1_comp_ID  == null ||
                tagNameDCStats_Atom_1_ID  == null ||
                tagNameDCStats_Atom_2_entity_ID  == null ||
                tagNameDCStats_Atom_2_comp_index_ID  == null ||
                tagNameDCStats_Atom_2_comp_ID  == null ||
                tagNameDCStats_Atom_2_ID  == null ||
                tagNameDCStats_Node_1_distance_val  == null ||
                tagNameDCStats_Node_1_distance_lower_bound_val  == null ||
                tagNameDCStats_Node_1_distance_upper_bound_val  == null ||
                tagNameDCStats_Distance_minimum  == null ||
                tagNameDCStats_Distance_average  == null ||
                tagNameDCStats_Distance_maximum  == null ||
                tagNameDCStats_Max_violation  == null ||
                tagNameDCStats_Max_violation_model_number  == null ||
                tagNameDCStats_Above_cutoff_violation_count  == null ||
                tagNameDCStats_Above_cutoff_violation_per_model  == null ||
//                tagNameDCStats_Entry_ID  == null ||
                tagNameDCStats_Distance_constraint_stats_ID  == null ||
                tagNameDCPer_resAtom_entity_ID == null ||
                tagNameDCPer_resAtom_comp_index_ID == null ||
                tagNameDCPer_resAtom_comp_ID == null ||
                tagNameDCPer_resTotal_violation == null ||
                tagNameDCPer_resMax_violation == null ||
                tagNameDCPer_resMax_violation_model_number == null ||
                tagNameDCPer_resAbove_cutoff_violation_count == null ||
                tagNameDCPer_resAbove_cutoff_violation_per_model == null 
//                tagNameDCPer_resEntry_ID == null ||
//                tagNameDCPer_resDistance_constraint_stats_ID == null
                ) {
            General.showError("Failed to get all the tag names from dictionary, compare code with dictionary.");
            return false;
        }
        return true;
    }
    
    
    /** Returns a template with the star formatted output template
     */
    private SaveFrame getSFTemplate() {
        SaveFrame sF = new SaveFrame();
        sF.setTitle("distance_constraint_statistics");
        // Default variables.
        HashMap             namesAndTypes;
        ArrayList           order;
        HashMap             namesAndValues;
        TagTable            tT;
//        int                 RID;
        try {
            // INTRO
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            DBMS dbms = new DBMS(); // Use a temporary dbms for this because we don't
            // want to hold on to this data for ever.
            tT                      = new TagTable("DC_viol_list", dbms);
            tT.isFree               = true;
            tT.getNewRowId(); // Sets first row bit in used to true.
            String cat = "distance_constraint_statistics";
            namesAndValues.put( tagNameDCStats_Sf_category, cat);
//            namesAndValues.put( tagNameDCStatsL_Entry_ID, new Integer(1));
//            namesAndValues.put( tagNameDCStatsL_Distance_constraint_stats_ID, new Integer(1));
            
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Sf_category                          );
//            starDict.putFromDict( namesAndTypes, order, tagNameDCStatsL_Entry_ID                            );
//            starDict.putFromDict( namesAndTypes, order, tagNameDCStatsL_Distance_constraint_stats_ID        );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Restraint_list_ID                    );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Restraint_count                      );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_count                           );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_total                           );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_max                             );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_rms                             );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_average_all_restraints          );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Viol_average_violations_only         );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Cutoff_violation_report              );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Details                              );
            
            
            // Append columns after order id column.
            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );
            
            // BY RESIDUE
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            tT                      = new TagTable("DC_viol_res", dbms);
            tT.isFree = false;
            
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resAtom_entity_ID                  );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resAtom_comp_index_ID              );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resAtom_comp_ID                    );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resTotal_violation                   );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resMax_violation                   );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resMax_violation_model_number      );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resAbove_cutoff_violation_count    );
            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resAbove_cutoff_violation_per_model);
//            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resEntry_ID                        );
//            starDict.putFromDict( namesAndTypes, order, tagNameDCPer_resDistance_constraint_stats_ID    );
            
            if ( ! tT.insertColumnSet(1, namesAndTypes, order, namesAndValues, null)) {
                General.showError("Failed to tT.insertColumnSet");
                return null;
            }
            sF.add( tT );
            
            // BY RESTRAINT
            namesAndTypes           = new HashMap();
            order                   = new ArrayList();
            namesAndValues          = new HashMap();
            tT                      = new TagTable("DC_viol", dbms);
            tT.isFree = false;
            //namesAndValues.put( tagNameDCStats_Entry_ID, new Integer(1));
            //namesAndValues.put( tagNameDCStats_Distance_constraint_stats_ID, new Integer(1));
            
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Restraint_ID                         );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_1_entity_ID                     );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_1_comp_index_ID                 );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_1_comp_ID                       );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_1_ID                            );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_2_entity_ID                     );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_2_comp_index_ID                 );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_2_comp_ID                       );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Atom_2_ID                            );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Node_1_distance_val                  );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Node_1_distance_lower_bound_val      );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Node_1_distance_upper_bound_val      );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Distance_average                     );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Distance_minimum                     );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Distance_maximum                     );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Max_violation                        );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Max_violation_model_number           );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Above_cutoff_violation_count         );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Above_cutoff_violation_per_model     );
//            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Entry_ID                             );
            starDict.putFromDict( namesAndTypes, order, tagNameDCStats_Distance_constraint_stats_ID         );
            
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
    
    
    /** Get the elements per residue into a table.
     */
    private boolean setTagTableRes(SaveFrame sF, BitSet dcToDo, 
            float cutoffValue, int listNumber) {
        dc = constr.dc;
        boolean smallerThanCutoff = false;
        
        TagTable tT = (TagTable) sF.get(1);
//        if ( false ) {
//            int x = tT.getNewRowId();            
//            tT.setValue(x, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    x);
//            return true;
//        }
        
        BitSet atomsInvolved = dc.getAtomRidSet( dcToDo );
        BitSet resInvolved = gumbo.atom.getResidueList(atomsInvolved);
        // Get the (index -1) of last residue with 
        int resMax = resInvolved.length();
//        General.showDebug("Involved atoms: " + PrimitiveArray.toString( atomsInvolved));
//        General.showDebug("Involved res  : " + PrimitiveArray.toString( resInvolved));
//        General.showDebug("Involved res  : " + gumbo.res.toString( resInvolved));
        if ( resMax < 0 ) {
            General.showError("Failed to get at least 1 residue in setTagTableRes");
            return false; // this return added after code debugging.
        }
        // Each object contains an arrayList per restraint of float[] per model of violations.
        ArrayList[] violPerRes      = new ArrayList[resMax];
        float[] maxViolModel        = new float[resMax];
        int[] maxViolModelNumber    = new int[resMax];
        
        // Get number of models from first listed restraint
        float[] violListTmp = dc.getModelViolationsFromViol(dcToDo.nextSetBit(0));
        int modelCount = violListTmp.length;
        
        // For each restraint
        for (int rid=dcToDo.nextSetBit(0);rid>=0;rid=dcToDo.nextSetBit(rid+1)) {

            BitSet atomRidSet = dc.getAtomRidSet(rid);
            BitSet resRidSet = gumbo.atom.getResidueList(atomRidSet);
            
            float[] violList = dc.getModelViolationsFromViol(rid);
            if ( violList == null ) {
                General.showError("Failed to getModelViolationsFromViol for toSTAR");
                return false;
            }
            if ( violList.length != modelCount ) {
                General.showError("From getModelViolationsFromViol got wrong number of values for models for restraint.");
                General.showError("Expected: " + modelCount + " but got: " + violList.length );
                General.showError("Restraint: " + dc.toString(rid) );
//                return false; TODO enable this exit again
            }
            //General.showDebug("getModelViolationsFromViol found: " + PrimitiveArray.toString(violList));
            
            // Set all involved residues once per restraint.
            for (int rrid=resRidSet.nextSetBit(0);rrid>=0;rrid=resRidSet.nextSetBit(rrid+1)) {
                if ( rrid >= violPerRes.length ) {
                    General.showCodeBug("in setTagTableRes");
                    return false;
                }
                ArrayList falList = violPerRes[rrid];
                if ( falList == null ) {
                    falList = new ArrayList();
                    violPerRes[rrid] = falList;
                    //General.showDebug("Added new viols LoL for residue: " + rrid);
                }
                falList.add( violList );
                FloatIntPair fip = dc.getMaxViolAndModelNumber( rid );
                if ( ! Defs.isNull(fip.f) ) {
                    if ( maxViolModel[rrid] < fip.f ) {
                        maxViolModel[       rrid] = fip.f;
                        maxViolModelNumber[ rrid] = fip.i;
                        /**General.showDebug("Found new max viol for residue: " + rrid + " at: " + fip.f + 
                                " for model number: " + fip.i + " from restraint: " + rid);
                         */
                    }
                }
            }
        }
        int rid = 0;
        for (int rrid=resInvolved.nextSetBit(0);rrid>=0;rrid=resInvolved.nextSetBit(rrid+1)) {

            ArrayList falList = (ArrayList) violPerRes[rrid];
            if ( falList == null ) {
                General.showCodeBug("ArrayList falList for violations per residue is empty.");
                return false;
            }
            float[] fal = PrimitiveArray.toFloatArray(falList);
            if ( fal == null ) {
                General.showError("Failed to toFloatArray for restraints in residue: " + 
                        gumbo.res.toString(rrid));
                return false;
            }
            if ( fal.length == 0 ) {
                General.showError("Residue involved with restraints has no restraints at this moment for residue:" + gumbo.res.toString(rrid));
//                return false; // TODO enable this exit again
                continue;
            }
            String above_cutoff_violation_per_model = null;
            try {
                above_cutoff_violation_per_model =
                        PrimitiveArray.toStringMakingCutoff( 
                        falList, cutoffValue, smallerThanCutoff);
            } catch ( Throwable t ) {
                General.showError("For res: " + gumbo.res.toString(rrid) + " toFloatArray found: " + PrimitiveArray.toString(fal));            
                General.showThrowable(t);
//                return false;
            }
            if ( above_cutoff_violation_per_model == null ) {
                General.showError("Failed PrimitiveArray.toStringMakingCutoff in setTagTableRes for residue:");
                General.showError(gumbo.res.toString(rrid));
                General.showError("For res: " + gumbo.res.toString(rrid) + " toFloatArray found: " + PrimitiveArray.toString(fal));            
//                return false;
            }
            above_cutoff_violation_per_model = "[" + above_cutoff_violation_per_model + "]";
            float maxViol = PrimitiveArray.getMax(fal);
//            if ( maxViol < 0.0001 ) { // really helps to focus the eye.
//                maxViol = Defs.NULL_FLOAT;
//            }
            float sumViol = PrimitiveArray.getSum(fal);
//            if ( sumViol < 0.0001 ) { // really helps to focus the eye.
//                sumViol = Defs.NULL_FLOAT;
//            }
            
            int max_violation_model_number = maxViolModelNumber[ rrid];
            if ( max_violation_model_number == 0 ) { // model numbers start at 1.
                max_violation_model_number = Defs.NULL_INT;
            }
            
            int countMakingCutoff = PrimitiveArray.countMakingCutoff( above_cutoff_violation_per_model );
            
            rid = tT.getNewRowId();
            tT.setValue(rid, Relation.DEFAULT_ATTRIBUTE_ORDER_ID,    rid);
            tT.setValue(rid, tagNameDCPer_resAtom_entity_ID,                         gumbo.mol.number[gumbo.res.molId[rrid]]);
            tT.setValue(rid, tagNameDCPer_resAtom_comp_index_ID,                     gumbo.res.number[rrid]);
            tT.setValue(rid, tagNameDCPer_resAtom_comp_ID,                           gumbo.res.nameList[rrid]);
            tT.setValue(rid, tagNameDCPer_resMax_violation,                          maxViol);
            tT.setValue(rid, tagNameDCPer_resTotal_violation,                        sumViol);
            tT.setValue(rid, tagNameDCPer_resMax_violation_model_number,             max_violation_model_number);
            tT.setValue(rid, tagNameDCPer_resAbove_cutoff_violation_count,           countMakingCutoff);
            tT.setValue(rid, tagNameDCPer_resAbove_cutoff_violation_per_model,       above_cutoff_violation_per_model);
//            tT.setValue(rid, tagNameDCPer_resEntry_ID,                               1);
//            tT.setValue(rid, tagNameDCPer_resDistance_constraint_stats_ID,           listNumber);        
        }
        return true;
    }
}
