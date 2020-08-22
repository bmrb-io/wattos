package Wattos.Soup.Constraint;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
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
import Wattos.Utils.FloatIntPair;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringSet;

/**
 *This set of relations is made up of the main relation as usual and then relation: simpleConstrAtom and
 * simpleConstrViol..
 *
 *The idea is that dihedral and rdc restraints can be extended from this class. Kinda like PropNAtom class for regular
 * dihedrals, distances, etc.
 *
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class SimpleConstr extends ConstrItem implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    /**
     * Default cutoff for reporting violations
     */
    public final float CUTOFF_REPORTING_VIOLATIONS = 5.f;

    /** Similar to RelationSet defs. of same variables without A_ */
    /** Defines the standard attribute names and data types */
    public HashMap A_DEFAULT_ATTRIBUTES_TYPES;
    /** Defines the standard attribute names and data types */
    public ArrayList A_DEFAULT_ATTRIBUTES_ORDER;
    /** Defines the standard values for some attributes */
    public HashMap A_DEFAULT_ATTRIBUTE_VALUES;
    /** Defines the standard fkcs for some attributes */
    public ForeignKeyConstrSet A_DEFAULT_ATTRIBUTE_FKCS;
    /**
     * Store standard definitions for fkcs just specifying the from relation column name and the to relation name as a
     * String[2] object inside the ArrayList.
     */
    public ArrayList A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO;

    public HashMap V_DEFAULT_ATTRIBUTES_TYPES;
    public ArrayList V_DEFAULT_ATTRIBUTES_ORDER;
    public HashMap V_DEFAULT_ATTRIBUTE_VALUES;
    public ForeignKeyConstrSet V_DEFAULT_ATTRIBUTE_FKCS;
    public ArrayList V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO;

    public static final String DEFAULT_ATTRIBUTE_ATOM_RELATION_EXTENSION = "_atom";
    public static final String DEFAULT_ATTRIBUTE_VIOL_RELATION_EXTENSION = "_viol";

    /** Convenience variables */
    public Relation simpleConstrAtom;
    public Relation simpleConstrViol;

    public int[] entryIdMain; // refs to entry
    public int[] entryIdViol;
    public int[] entryIdAtom;
    public int[] scListIdMain; // refs to scList
    public int[] scListIdViol;
    public int[] scListIdAtom;
    public int[] scMainIdViol; // refs to scMain
    /** In main relation point to ... Set by subclass. */
    // public int[] scMainIdAtom;
    /** In simpleConstrAtom relation point to REAL atom relation. Set by subclass. */
    public int[] atomIdAtom;
    /** In simpleConstrAtom relation point to main relation. Set by subclass. */
    public int[] scIdAtom;

    // public int[] scListIdMain; defined through super class.
    // public int[] numbNode;
    // public int[] numbMemb;
    // public int[] numbAtom;
    // public int[] numbViol; // To expect?
    public int[] orderAtom;

    public float[] violUppMax; // non-fkcs in scMain
    public int[] violUppMaxModelNum; // could be a fkc but not implemented as such
    public float[] violLowMax;
    public int[] violLowMaxModelNum;
    public BitSet hasUnLinkedAtom; // set to false if any atom in constraint could NOT be linked. true otherwise.

    public int[] modelIdViol; // fkcs in dcViol.
    public float[] value; // non-fkcs in dcViol. Doesn't have a meaning in case of xplor's "high dim"
    public float[] violation; // positive violation is a violation of the upper bound and negative one is on lower bound

    public float[] target;
    public float[] targetError;
    public float[] uppBound;
    public float[] lowBound;

    public String[] authMolNameList; // non-fkcs in scAtom
    public StringSet authMolNameListNR; // Only the first atom contains actual references, the other refs are null.
    public String[] authResNameList;
    public StringSet authResNameListNR;
    public String[] authResIdList;
    public StringSet authResIdListNR;
    public String[] authAtomNameList;
    public StringSet authAtomNameListNR;

    /** These containers become obsolete because they're avail by atomIdAtom */
    // public String[] atomNameList;
    // public StringSet atomNameListNR;
    // public int[] resIdList;
    // public String[] resNameList;
    // public StringSet resNameListNR;
    // public int[] molIdList;
    // public int[] entityIdList;
    public String violRelationName;
    public String atomRelationName;
    public String relationName;

    /** E.g. Constr.DEFAULT_ATTRIBUTE_SET_CDIH */
    public String[] ATTRIBUTE_SET_SUB_CLASS = null;
    /** E.g. Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST */
    public String[] ATTRIBUTE_SET_SUB_CLASS_LIST = null;

    public SimpleConstr(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        resetConvenienceVariables();
    }

    /**
     * The relationSetName is a parameter so non-standard relation sets can be created; e.g. AtomTmp with a relation
     * named AtomTmpMain etc.
     */
    public SimpleConstr(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        // General.showDebug("now in SimpleConstr.init()");
        super.init(dbms);
        // General.showDebug("back in SimpleConstr.init()");

        // MAIN RELATION in addition to the ones in gumbo item.
        if (true) {
            // General.showDebug("Adding columns and fkcs to relation sc_main");
            DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VIOL_MAX, new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX, new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM, new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM, new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM, new Integer(DATA_TYPE_BIT));

            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_TARGET, new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_TARGET_ERR, new Integer(DATA_TYPE_FLOAT));

            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_UPP_BOUND, new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_LOW_BOUND, new Integer(DATA_TYPE_FLOAT));

            DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME]);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VIOL_MAX);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_TARGET);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_TARGET_ERR);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_UPP_BOUND);
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_LOW_BOUND);

            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME] });
        }

        // ATOM RELATION
        if (true) {
            // General.showDebug("Adding columns and fkcs to relation sc_atom");
            // Nothing was added to the below so can be inited here.
            A_DEFAULT_ATTRIBUTES_TYPES = new HashMap();
            A_DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            A_DEFAULT_ATTRIBUTE_VALUES = new HashMap();
            A_DEFAULT_ATTRIBUTE_FKCS = null;
            /**
             * Store standard definitions for fkcs just specifying the from relation column name and the to relation
             * name as a String[2] object inside the ArrayList.
             */
            A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();

            A_DEFAULT_ATTRIBUTES_TYPES.clear();
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_COLUMN_NAME],
                    new Integer(DATA_TYPE_INT));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    new Integer(DATA_TYPE_INT));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME, new Integer(DATA_TYPE_STRINGNR));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME, new Integer(DATA_TYPE_STRINGNR));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID, new Integer(DATA_TYPE_STRINGNR));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME, new Integer(DATA_TYPE_STRINGNR));

            // A_DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME, new Integer(DATA_TYPE_STRINGNR));
            // A_DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_RES_ID, new Integer(DATA_TYPE_INT));
            // A_DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME, new Integer(DATA_TYPE_STRINGNR));
            // A_DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_MOL_ID, new Integer(DATA_TYPE_INT));
            // A_DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID, new Integer(DATA_TYPE_INT));
            A_DEFAULT_ATTRIBUTES_TYPES.put(Relation.DEFAULT_ATTRIBUTE_ORDER_ID, new Integer(DATA_TYPE_INT));

            A_DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_COLUMN_NAME]);
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME]);
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME);
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
            A_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);
            // A_DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME );
            // A_DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_RES_ID );
            // A_DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME );
            // A_DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_MOL_ID );
            // A_DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID );
            A_DEFAULT_ATTRIBUTES_ORDER.add(Relation.DEFAULT_ATTRIBUTE_ORDER_ID);
            A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_COLUMN_NAME],
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME] });
            A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME] });
        }

        // VIOL RELATION
        if (true) {
            // General.showDebug("Adding columns and fkcs to relation simple_viol");
            V_DEFAULT_ATTRIBUTES_TYPES = new HashMap();
            V_DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            V_DEFAULT_ATTRIBUTE_VALUES = new HashMap();
            V_DEFAULT_ATTRIBUTE_FKCS = null;

            V_DEFAULT_ATTRIBUTES_TYPES.clear();
            V_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                    new Integer(DATA_TYPE_INT));
            V_DEFAULT_ATTRIBUTES_TYPES.put(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    new Integer(DATA_TYPE_INT));
            V_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VIOLATION, new Integer(DATA_TYPE_FLOAT));
            V_DEFAULT_ATTRIBUTES_TYPES.put(Constr.DEFAULT_ATTRIBUTE_VALUE, new Integer(DATA_TYPE_FLOAT));
            V_DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            V_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME]);
            V_DEFAULT_ATTRIBUTES_ORDER.add(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME]);
            V_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VIOLATION);
            V_DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_VALUE);
            V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                    Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME],
                    Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME] });
            V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add(new String[] {
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME],
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME] });
        }

        return true;
    }

    /**     */
    public boolean resetConvenienceVariables() {
        if (!super.resetConvenienceVariables()) {
            General.showError("Failed super's resetConvenienceVariables in SimpleConstr");
            return false;
        }
        // General.showDebug("Now in resetConvenienceVariables in simpleConstr");

        if (true) {
            entryIdMain = mainRelation.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME]);
            entryIdAtom = simpleConstrAtom.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME]);
            entryIdViol = simpleConstrViol.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME]);

            atomIdAtom = simpleConstrAtom.getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_COLUMN_NAME]);
            modelIdViol = simpleConstrViol
                    .getColumnInt(Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RelationSet.RELATION_ID_COLUMN_NAME]);
            violation = (float[]) simpleConstrViol.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOLATION);
            value = (float[]) simpleConstrViol.getColumn(Constr.DEFAULT_ATTRIBUTE_VALUE);

            violUppMax = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_MAX);
            violLowMax = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX);
            violUppMaxModelNum = mainRelation.getColumnInt(Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM);
            violLowMaxModelNum = mainRelation.getColumnInt(Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM);
            hasUnLinkedAtom = mainRelation.getColumnBit(Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM);

            target = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_TARGET);
            targetError = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_TARGET_ERR);
            uppBound = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_UPP_BOUND);
            lowBound = (float[]) mainRelation.getColumn(Constr.DEFAULT_ATTRIBUTE_LOW_BOUND);

            authMolNameList = (String[]) simpleConstrAtom.getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME);
            authMolNameListNR = simpleConstrAtom.getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME);
            authResNameList = (String[]) simpleConstrAtom.getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
            authResNameListNR = simpleConstrAtom.getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME);
            authResIdList = (String[]) simpleConstrAtom.getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
            authResIdListNR = simpleConstrAtom.getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID);
            authAtomNameList = (String[]) simpleConstrAtom.getColumn(Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);
            authAtomNameListNR = simpleConstrAtom.getColumnStringSet(Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);

            // atomNameList = (String[]) simpleConstrAtom.getColumn( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME );
            // atomNameListNR = simpleConstrAtom.getColumnStringSet( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME );
            // resNameList = (String[]) simpleConstrAtom.getColumn( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME );
            // resNameListNR = simpleConstrAtom.getColumnStringSet( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME );
            // resIdList = simpleConstrAtom.getColumnInt( Gumbo.DEFAULT_ATTRIBUTE_RES_ID );
            // molIdList = simpleConstrAtom.getColumnInt( Gumbo.DEFAULT_ATTRIBUTE_MOL_ID );
            // entityIdList = simpleConstrAtom.getColumnInt( Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID );
            orderAtom = simpleConstrAtom.getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID);

            if (entryIdMain == null
                    || entryIdViol == null
                    || entryIdAtom == null
                    || scListIdMain == null
                    ||
                    // scListIdAtom == null ||
                    // scListIdViol == null ||
                    // scMainIdAtom == null ||
                    // scMainIdViol == null ||
                    atomIdAtom == null || scIdAtom == null || violUppMax == null || violLowMax == null
                    || violUppMaxModelNum == null || violLowMaxModelNum == null || hasUnLinkedAtom == null
                    || violation == null || uppBound == null || lowBound == null || target == null
                    || targetError == null || authMolNameList == null || authMolNameListNR == null
                    || authResNameList == null || authResNameListNR == null || authResIdList == null
                    || authResIdListNR == null || authAtomNameList == null || authAtomNameListNR == null ||
                    // atomNameList == null ||
                    // atomNameListNR == null ||
                    // resNameList == null ||
                    // resNameListNR == null ||
                    // resIdList == null ||
                    // molIdList == null ||
                    // entityIdList == null ||
                    orderAtom == null) {
                return false;
            }
        }
        // General.showDebug("Done sc resetConvenienceVariables");
        return true;
    }

    /**
     * Convenience method
     */
    public boolean calcViolation() {
        return calcViolation(selected, CUTOFF_REPORTING_VIOLATIONS);
    }

    private boolean calcViolation(BitSet selected, float cutoff_reporting_violations2) {
        General.showCodeBug("Method SimpleConstr.calcViol needs to be overriden");
        return false;
    }

    /**
     * Returns an (empty) set of sc list rids for the given sc set. So given a set of sc rids it returns the list rids
     * of the sc that the sc are part of.. In case of an error it will return an empty BitSet.
     */
    public BitSet getSCListSetFromSCSet(BitSet todo) {
        BitSet result = new BitSet();

        int todoCount = todo.cardinality();
        if (todoCount == 0) {
            General.showWarning("No simple constraints selected in getSCListSetFromSCSet.");
            return result;
        }
        // The below was a bug.
        // BitSet scMainListRids = SQLSelect.getDistinct(
        // dbms,
        // mainRelation,
        // ATTRIBUTE_SET_SUB_CLASS[ RelationSet.RELATION_ID_COLUMN_NAME ], // TODO: Is this wrong? it should be the
        // physical row order.
        // (BitSet)todo.clone());
        // if ( scMainListRids == null ) {
        // General.showWarning("Failed to get a list of simple constraint lists.");
        // return result;
        // }
        // if ( scMainListRids.cardinality() == 0 ) {
        // General.showWarning("No distance constraint lists selected in getSCListSetFromSCSet.");
        // return result;
        // }
        for (int rid = todo.nextSetBit(0); rid >= 0; rid = todo.nextSetBit(rid + 1)) {
            result.set(scListIdMain[rid]);
        }
        // General.showDebug("Found result        : " + PrimitiveArray.toString(result));

        return result;
    }

    /**
     * Sort the todo elements in this list. The number attribute will be renumbered to start with offset. The following
     * types of sorts will be executed. regardless of whether they belong to the same sc list or even entry. Make sure
     * caller indexes AND convenience variables are reset because the set really changes. The only garantee is that the
     * constraint rid will remain valid and the same. This method will reset the indexes and convenience variables of
     * this class.
     */
    public boolean sortAll(BitSet todo, int offset) {
        General.showDebug("Sorting simple constraints numbered: " + todo.cardinality());

        if (!sort(todo, offset)) {
            General.showError("Failed to do sort of simple constraints");
            return false;
        }
        if (!resetConvenienceVariables()) {
            General.showError("Failed to resetConvenienceVariables");
            return false;
        }
        if (!removeAllIndices()) {
            General.showError("Failed to removeAllIndices");
            return false;
        }
        return true;
    }

    boolean sort(BitSet todo, int offset) {
        int todoSize = todo.cardinality();
        Object[] elements = new Object[todoSize];
        int i = 0;
        for (int currentscId = todo.nextSetBit(0); currentscId >= 0; currentscId = todo.nextSetBit(currentscId + 1)) {
            Integer currentscIdInteger = new Integer(currentscId);
            elements[i] = currentscIdInteger;
            i++;
        }
        Comparator comparatorSC = new ComparatorSC(this);

        Arrays.sort(elements, comparatorSC);
        Arrays.fill(order, Defs.NULL_INT); // Reset all first
        for (i = 0; i < todoSize; i++) {
            order[((Integer) elements[i]).intValue()] = i + offset; // pretty clever.
        }
        return true;
    }

    /** Convenience method */
    public String toString(int rid) {
        boolean showViolations = false;
        boolean showTheos = false;
        return toString(rid, showViolations, showTheos);
    }

    /** Convenience method */
    public String toString(int rid, boolean showViolations, boolean showTheos) {
        BitSet tmpSet = new BitSet();
        tmpSet.set(rid);
        return toString(tmpSet, showViolations, showTheos);
    }

    public boolean toXplorOrSo(BitSet scSet, String fn, int fileCount, String atomNomenclature, boolean sortRestraints,
            String format) {
        General.showCodeBug("Method toXplor in SC should have been overrided");
        return false;
    }

    /**
     * The supported output formats for xplor include Aria's: ASSI { 6} (( segid "SH3 " and resid 53 and name HA )) ((
     * segid "SH3 " and resid 53 and name HE1 )) 3.600 1.700 1.700 peak 6 weight 0.10000E+01 volume 0.14383E-02 ppm1
     * 4.578 ppm2 9.604 CV 1 OR { 6} (( segid "SLP " and resid 83 and name HB )) (( segid "SH3 " and resid 53 and name
     * HE1 ))
     *
     * @param format
     *            TODO
     */
    public boolean toXplorOrSo(int listRID, String fn, int fileCount, String atomNomenclature, boolean sortRestraints,
            String format) {
        BitSet scSet = SQLSelect.selectBitSet(dbms, mainRelation,
                ATTRIBUTE_SET_SUB_CLASS_LIST[RelationSet.RELATION_ID_COLUMN_NAME], SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(listRID), false);
        if (scSet == null) {
            General.showError("Failed to get ridsSC for toXplor.");
            return false;
        }

        if (!toXplorOrSo(scSet, fn, fileCount, atomNomenclature, sortRestraints, format)) {
            General.showError("Failed to do toXplor.");
            return false;
        }

        return true;
    }

    /**
     * WARNING; this format will change in the future. Optionaly shows theoretically possible distances if available.
     */
    public String toString(BitSet todo, boolean showViolations, boolean showTheos) {
        return null;

        /*
         * int todoCount = todo.cardinality(); StringBuffer sb = new StringBuffer( todoCount 80 5); // rough unimportant
         * estimation of 80 chars per restraint.
         *
         * DecimalFormat df = new DecimalFormat(); df.setMaximumFractionDigits(3);
         *
         * if ( todoCount == 0 ) { return "No distance constraints selected in toString."; }
         *
         * float[] upp_theo = null; float[] low_theo = null;
         *
         * if ( mainRelation.containsColumn( DEFAULT_UPP_THEO) && mainRelation.containsColumn( DEFAULT_LOW_THEO) ) {
         * upp_theo = mainRelation.getColumnFloat(DEFAULT_UPP_THEO); low_theo =
         * mainRelation.getColumnFloat(DEFAULT_LOW_THEO); } boolean containsTheos = true;
         *
         * if ( upp_theo == null || low_theo == null ) { containsTheos = false; }
         *
         * //sb.append( "Total number of distance constraints todo: " + todoCount + "\n");
         *
         * IndexSortedInt indexMembAtom = (IndexSortedInt)
         * simpleConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_SC_MEMB_ID, Index.INDEX_TYPE_SORTED); IndexSortedInt
         * indexNodeMemb = (IndexSortedInt) simpleConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_SC_NODE_ID,
         * Index.INDEX_TYPE_SORTED); IndexSortedInt indexMainNode = (IndexSortedInt)
         * simpleConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_SC[ RelationSet.RELATION_ID_COLUMN_NAME ],
         * Index.INDEX_TYPE_SORTED); if ( indexMembAtom == null || indexNodeMemb == null || indexMainNode == null ) {
         * General.showCodeBug("Failed to get all indexes."); return null; }
         *
         * // int currentSCViolId = Defs.NULL_INT; int currentSCNodeId = Defs.NULL_INT; int currentscId = Defs.NULL_INT;
         * // int currentscListId = Defs.NULL_INT; int currentSCEntryId = Defs.NULL_INT;
         *
         * int scCount = 0; // FOR EACH CONSTRAINT BitSet unlinkedAtomSelected = (BitSet) hasUnLinkedAtom.clone();
         * unlinkedAtomSelected.and( selected ); int unlinkedAtomCount = unlinkedAtomSelected.cardinality(); if (
         * unlinkedAtomCount > 0 ) { General.showWarning("Skipping toString for " + unlinkedAtomCount +
         * " constraints because not all their atoms are linked -1-." ); } for (currentscId =
         * todo.nextSetBit(0);currentscId>=0;currentscId = todo.nextSetBit(currentscId+1)) { Integer currentscIdInteger
         * = new Integer(currentscId); if ( scCount != 0 ) { sb.append( General.eol ); } sb.append( "SC: " + currentscId
         * ); if ( showViolations ) {
         *
         * if ( ! Defs.isNull( violLowMax[currentscId]) ) { sb.append( " violLowMax: " +
         * df.format(violLowMax[currentscId]) + " at model: " + violLowMaxModelNum[currentscId]); } if ( ! Defs.isNull(
         * violUppMax[currentscId]) ) { sb.append( " violUppMax: " + df.format(violUppMax[currentscId]) + " at model: "
         * + violUppMaxModelNum[currentscId]); } } if ( showTheos ) { if ( containsTheos ) { sb.append(
         * " lowTheo/uppTheo: " + Defs.toString( low_theo[ currentscId ]) + "/" + Defs.toString( upp_theo[ currentscId
         * ]) ); } else { sb.append( " no lowTheo/uppTheo: "); } }
         *
         * sb.append( General.eol ); if ( hasUnLinkedAtom.get( currentscId )) {
         * General.showDetail("Skipping toString for constraint at rid: " + currentscId +
         * " because not all atoms are linked." ); continue; } // currentscListId = scListIdMain[ currentscId ];
         * currentSCEntryId = entryIdMain[ currentscId ]; if ( ! gumbo.entry.modelsSynced.get( currentSCEntryId ) ) { if
         * ( ! gumbo.entry.syncModels( currentSCEntryId )) {
         * General.showError("Failed to sync models as required for toString."); }
         * //General.showDebug("Sync-ed models for calculating distances for constraints"); } // Not yet used: // int
         * avgMethod = constr.scList.avgMethod[ currentscListId ]; // int numberMonomers = constr.scList.numberMonomers[
         * currentscListId ];
         *//**
         * General.showDebug("Found constraint at rid: " + currentscId + " in list with rid: " + currentscListId +
         * " with avgMethod: " + simpleConstrList.DEFAULT_AVERAGING_METHOD_NAMES[avgMethod] +
         * " and number of monomers: " + numberMonomers);
         */
        /*
         *
         *
         * boolean atomFound = true; // signals at least one atom could not be found when 'false' IntArrayList scNodes =
         * (IntArrayList) indexMainNode.getRidList( currentscIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
         * //General.showDebug("Found the following rids of nodes in constraint: " +
         * PrimitiveArray.toStringMakingCutoff( scNodes ));
         *
         * int dCNodeNumber = 1; // Define a list of n constraint nodes with 2 sets of atoms so elements are of type
         * IntArrayList[2] // ArrayList atomsInvolved = new ArrayList();
         *
         *
         * // FOR EACH NODE for ( int currentSCNodeBatchId=0;currentSCNodeBatchId<scNodes.size();
         * currentSCNodeBatchId++) { currentSCNodeId = scNodes.getQuick( currentSCNodeBatchId ); // quick enough?;-)
         * //General.showDebug("Using distance constraint node: " + dCNodeNumber + " at rid: " + currentSCNodeId );
         *
         * // For each constraint node (those with distance etc but without logic) add the distance info to // the
         * distance star loop. int logOp = logicalOp[currentSCNodeId]; sb.append( "Node " + dCNodeNumber + " " ); if ( !
         * Defs.isNull( logOp ) ) { sb.append( " " + DEFAULT_LOGICAL_OPERATION_NAMES[logOp] + " "); }
         *
         * sb.append( "Low/target/Upp" ); sb.append( ": " ); sb.append( Defs.toString( lowBound[ currentSCNodeId ]) +
         * " " + Defs.toString( target[ currentSCNodeId ]) + " " + Defs.toString( uppBound[ currentSCNodeId ]) +
         * General.eol);
         *
         * if ( ! Defs.isNull( logOp ) ) { if ( logOp != DEFAULT_LOGICAL_OPERATION_ID_OR ) {
         * General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]"); return
         * null; } dCNodeNumber++; continue; } // Assumption at this point is that the constraint nodes are OR-ed.
         * Change code here to // allow different type of trees. //General.showDebug(
         * "Dc node is a constraint node and not a logical node." );
         *
         * // FOR BOTH MEMBER (just 2) IntArrayList scMembs = (IntArrayList) indexNodeMemb.getRidList( new
         * Integer(currentSCNodeId), Index.LIST_TYPE_INT_ARRAY_LIST, null);
         * //General.showDebug("Found the following rids of members in constraint node (" + currentSCNodeId + "): " +
         * PrimitiveArray.toStringMakingCutoff( scMembs )); if ( scMembs.size() != 2 ) {General.showError(
         * "Are we using a number different than 2 as the number of members in a node? Can't do toString for it yet.");
         * return null; }
         *//** Next objects can be reused if code needs optimalization. */
        /*
         * for (int m=0;m<scMembs.size();m++ ) { IntArrayList scAtoms = (IntArrayList) indexMembAtom.getRidList( new
         * Integer(scMembs.get(m)), Index.LIST_TYPE_INT_ARRAY_LIST, null); String prefix = "Member " + m + " "; // Get
         * the real atom ids into a new list. for (int i=0;i<scAtoms.size();i++) { int rid = atomIdAtom[
         * scAtoms.getQuick(i) ]; if ( Defs.isNull( rid )) {
         * General.showWarning("Failed to find atom; that should have been recorded before"); atomFound = false; break;
         * } sb.append( prefix + gumbo.atom.toString(rid) + General.eol ); } } if ( ! atomFound ) { break; }
         * dCNodeNumber++; } // end of loop for each node if ( ! atomFound ) {
         * General.showWarning("Failed to find all atoms in constraint: " + scCount +
         * "That should have been recorded before."); continue; // continue with other constraints. } scCount++; } //
         * end of loop per constraint return sb.toString();
         */}

    /**
     * Finds the restraint rids for restraints that involve one or more of the atoms given. The method is optimized for
     * a large number of atoms given. For that case a simple scan works best.
     */
    public BitSet getWithAtoms(BitSet atomList) {
        BitSet result = new BitSet();
        for (int currentSCAtomId = simpleConstrAtom.used.nextSetBit(0); currentSCAtomId >= 0; currentSCAtomId = simpleConstrAtom.used
                .nextSetBit(currentSCAtomId + 1)) {
            if (atomList.get(atomIdAtom[currentSCAtomId])) {
                result.set(scIdAtom[currentSCAtomId]);
            }
        }
        return result;
    }

    /**
     * Returns the distances target, lowerbound dev, upperbound dev given target, low, upp. Returns null when given
     * input can't be translated e.g. all were nulls. In summary, 3 binary states in combination gives 8 possibilities
     * detailed below.
     *
     * <PRE>
     * t target
     * l lower bound
     * u upper bound
     * oo infinity
     * DATA     RANGE   t lowdev uppdev
     * --------------------------------
     * only t   [t]     0   0   t
     * only l   [t,oo]  l   0   oo
     * only u	[0,u]   0   0   u
     *
     * t&amp;l      [l,t]   t   t-l 0
     * t&amp;u      [t,u]   u   u-t 0
     * l&amp;u      [l,u]   u   u-l 0
     *
     * t&amp;l&amp;u    [l,t,u] t   t-l u-t
     * no data  error
     * </PRE>
     *
     * Fast but verbose routine.
     */

    public static float[] toXplor(float tar, float low, float upp) {
        float tarNew = Defs.NULL_FLOAT;
        float lowDev = Defs.NULL_FLOAT;
        float uppDev = Defs.NULL_FLOAT;
        int state = -1;
        // sanity checks first
        // General.showDebug("tar is: " + tar);
        // General.showDebug("low is: " + low);
        // General.showDebug("upp is: " + upp);
        if (!Defs.isNull(low) && !Defs.isNull(tar) && low > tar) {
            General.showWarning("Found lowerbound (" + low + ") larger than target (" + tar + ")");
            // return null;
        }
        if (!Defs.isNull(low) && !Defs.isNull(upp) && low > upp) {
            General.showWarning("Found lowerbound (" + low + ") larger than upperbound (" + upp + ")");
            // return null;
        }
        if (!Defs.isNull(tar) && !Defs.isNull(upp) && tar > upp) {
            General.showWarning("Found target (" + tar + ") larger than upperbound (" + upp + ")");
            // return null;
        }
        // 3 combinations where 1 is known
        if (!Defs.isNull(tar) && Defs.isNull(low) && Defs.isNull(upp)) {
            state = 1;
            tarNew = 0f;
            lowDev = 0f;
            uppDev = tar;
        }
        if (Defs.isNull(tar) && !Defs.isNull(low) && Defs.isNull(upp)) {
            state = 2;
            tarNew = low;
            lowDev = 0f;
            uppDev = 999.999f; // needs to match the %8.3f formatting used for printing above
        }
        if (Defs.isNull(tar) && Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 3;
            tarNew = 0;
            lowDev = 0;
            uppDev = upp;
        }
        // 3 combinations where 2 are known
        if (!Defs.isNull(tar) && !Defs.isNull(low) && Defs.isNull(upp)) {
            state = 4;
            tarNew = tar;
            lowDev = tar - low;
            uppDev = 0f;
        }
        if (!Defs.isNull(tar) && Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 5;
            tarNew = upp;
            lowDev = upp - tar;
            uppDev = 0f;
        }

        if (Defs.isNull(tar) && !Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 6;
            tarNew = upp;
            lowDev = upp - low;
            uppDev = 0f;
        }
        // the other 2 combinations; none known or all three known.
        if (Defs.isNull(tar) && Defs.isNull(low) && Defs.isNull(upp)) {
            General.showError("Found all distances to be nonexisting");
            return null;
        }
        if (!Defs.isNull(tar) && !Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 7;
            tarNew = tar;
            lowDev = tar - low;
            uppDev = upp - tar;
        }

        if (Defs.isNull(tarNew) || Defs.isNull(lowDev) || Defs.isNull(uppDev)) {
            General.showCodeBug("Found at least one of the distances to be nonexisting");
            General.showCodeBug("In state: " + state);
        }
        // self checks
        if (Defs.isNull(tarNew)) {
            General.showCodeBug("Found tarNew to be nonexisting");
            return null;
        }
        if (Defs.isNull(lowDev)) {
            General.showCodeBug("Found lowDev to be nonexisting");
            return null;
        }
        if (Defs.isNull(uppDev)) {
            General.showCodeBug("Found uppDev to be nonexisting");
            return null;
        }
        return new float[] { tarNew, lowDev, uppDev };
    }

    /**
     * Convenience method to combine low and upp viols to one overall and get the responsible model number back.
     */
    FloatIntPair getMaxViolAndModelNumber(int rid) {

        FloatIntPair fip = new FloatIntPair();
        fip.f = Defs.NULL_FLOAT;
        fip.i = Defs.NULL_INT;

        if (!Defs.isNull(violUppMax[rid])) {
            fip.f = violUppMax[rid];
            fip.i = violUppMaxModelNum[rid];
        }

        if (!Defs.isNull(violLowMax[rid])) {
            if (Defs.isNull(violUppMax[rid])) {
                fip.f = violLowMax[rid];
                fip.i = violLowMaxModelNum[rid];
            } else if (violLowMax[rid] > fip.f) {
                fip.f = violLowMax[rid];
                fip.i = violLowMaxModelNum[rid];
            }
        }

        return fip;
    }

    public float[] getModelValuesFromViol(int currentSCId) {
        BitSet scViolSet = SQLSelect.selectBitSet(dbms, simpleConstrViol,
                ATTRIBUTE_SET_SUB_CLASS[RelationSet.RELATION_ID_COLUMN_NAME], SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentSCId), false);
        if (scViolSet == null) {
            General.showError("Failed to getModelValuesFromViol.");
            return null;
        }
        int[] scViolList = PrimitiveArray.toIntArray(scViolSet);

        float[] result = new float[scViolList.length];
        for (int i = 0; i < scViolList.length; i++) {
            result[i] = value[scViolList[i]];
        }
        return result;
    }

    public float[] getModelViolationsFromViol(int currentSCId) {
        BitSet scViolSet = SQLSelect.selectBitSet(dbms, simpleConstrViol,
                ATTRIBUTE_SET_SUB_CLASS[RelationSet.RELATION_ID_COLUMN_NAME], SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentSCId), false);
        if (scViolSet == null) {
            General.showError("Failed to getModelDistancesFromViol.");
            return null;
        }
        int[] scViolList = PrimitiveArray.toIntArray(scViolSet);
        /**
         * if ( dcViolList.length != 2 ) { General.showError("Got dc viol list length not 2 but: " + dcViolList.length
         * ); General.showError("Rst: " + toString(currentSCId)); General.showError("dcViolSet: " +
         * PrimitiveArray.toString(dcViolSet)); General.showError("dcViolList: " + PrimitiveArray.toString(dcViolList));
         * return null; }
         */
        float[] result = new float[scViolList.length];
        for (int i = 0; i < scViolList.length; i++) {
            result[i] = violation[scViolList[i]];
        }
        return result;
    }

    public BitSet getAtomRidSet(BitSet scRidSet) {
        IndexSortedInt isi = (IndexSortedInt) simpleConstrAtom.getIndex(
                ATTRIBUTE_SET_SUB_CLASS[RelationSet.RELATION_ID_COLUMN_NAME], Index.INDEX_TYPE_SORTED);
        BitSet atomSCAtomRidSet = (BitSet) isi.getRidList(scRidSet);
        if (atomSCAtomRidSet == null) {
            General.showError("In getAtomRidSet(2) for restraint: " + toString(scRidSet));
            return null;
        }
        BitSet atomRidSet = new BitSet();
        // the next scan is of course slower than doing it on the list which in general is short.
        for (int rid = atomSCAtomRidSet.nextSetBit(0); rid >= 0; rid = atomSCAtomRidSet.nextSetBit(rid + 1)) {
            atomRidSet.set(atomIdAtom[rid]);
        }
        return atomRidSet;
    }

    /** convenience method */
    public BitSet getAtomRidSet(int scRid) {
        BitSet bs = new BitSet();
        bs.set(scRid);
        return getAtomRidSet(bs);
    }

    /**
     * Calculate the SC violations for a given cut off.
     */
    public boolean calcSimpleConstraintViolation(float cutoffValue, String fileName) {
        // Show only the selected ones.
        boolean showViolations = true;
        String msg = toSTAR(selected, cutoffValue, showViolations);
        if (msg == null) {
            General.showError("Failed to do constr.toSTAR");
            return false;
        }
        if (msg.length() == 0) {
//            General.showDebug("Why isn't there a message? Is that for RDC?");
        } else {
            if (!InOut.writeTextToFile(new File(fileName), msg, true, false)) {
                General.showError("Failed to write summary for violations");
            }
        }
        return true;
    }

    String toSTAR(BitSet selected, float cutoffValue, boolean showViolations) {
        General.showCodeBug("Method SimpleConstr.calcViol needs to be overriden");
        return null;
    }

}
