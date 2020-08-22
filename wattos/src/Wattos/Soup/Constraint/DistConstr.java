/*
 * Atom.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup.Constraint;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultTableXYDataset;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstr;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedFloat;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Gumbo;
import Wattos.Soup.PseudoLib;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarGeneral;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.FloatIntPair;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.ObjectIntPair;
import Wattos.Utils.PdfGeneration;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringArrayList;
import Wattos.Utils.StringSet;
import Wattos.Utils.Strings;
import Wattos.Utils.Charts.ResiduePlot;
import Wattos.Utils.Comparators.ComparatorIntIntPair;
import Wattos.Utils.Wiskunde.Statistics;
import cern.colt.list.IntArrayList;
import cern.colt.list.ObjectArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 *This set of relations is made up of the main relation as usual and then
 *relations: distConstrAtom, distConstrDist, distConstrAvg, and distConstrViolsPerConstr.
 *The classes Completeness and Surplus piggy-back of this class.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class DistConstr extends ConstrItem implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;
    
    /** The logical operations are dealt with through ints rather than strings
     *for efficiency
     */
    public static HashMap logicalOperationString2Int= new HashMap(); // To get linear look up times.
    public static final int DEFAULT_LOGICAL_OPERATION_ID_OR                = 0;
    public static final int DEFAULT_LOGICAL_OPERATION_ID_AND               = 1;
    public static final int DEFAULT_LOGICAL_OPERATION_ID_SYMM              = 2;
    public static final int DEFAULT_LOGICAL_OPERATION_ID_XOR               = 3;
    public static final String[] DEFAULT_LOGICAL_OPERATION_NAMES              = { "OR", "AND", "SYMM", "XOR"};
    
    public static HashMap classString2Int= new HashMap(); // To get linear look up times.
    public static final int DEFAULT_CLASS_UNDETERMINED         = 0;
    public static final int DEFAULT_CLASS_INTRA                = 1;
    public static final int DEFAULT_CLASS_SEQ                  = 2;
    public static final int DEFAULT_CLASS_MEDIUM               = 3;
    public static final int DEFAULT_CLASS_LONG                 = 4;
    public static final int DEFAULT_CLASS_INTER                = 5;
    public static final int DEFAULT_CLASS_MIXED                = 6; // Mixture of the above
    public static final String[] DEFAULT_CLASS_NAMES           = { "undetermined", "intraresidue", "sequential", "medium-range", "long-range", "intermolecular", "mixed"};
    
    /** Default cutoff for reporting violations
     */
    public static final float CUTOFF_REPORTING_VIOLATIONS = .5f;
    
    /** Index of the lower, target, upper, and theoretical bound value in the standard array returned */
    public static final int LOW_IDX = 0;
    public static final int TAR_IDX = 1;
    public static final int UPP_IDX = 2;
    public static final int THE_IDX = 3;
    
    /** A constraint is considered to be in the class of medium range constraints when it is no more
     *than 4 residues apart, according to IUPAC defs.
     */
    public static int MEDIUM_BORDER = 4;
    
    /** Used for surplus */
    public static final String DEFAULT_UPP_THEO = "upp_theo";
    public static final String DEFAULT_LOW_THEO = "low_theo";
    
    /** Convenience variables */
    public Relation    distConstrAtom;
    public Relation    distConstrMemb;
    public Relation    distConstrNode;
    public Relation    distConstrViol;
    
    public int[]       entryIdMain;                     // refs to entry
    public int[]       entryIdViol;
    public int[]       entryIdNode;
    public int[]       entryIdMemb;
    public int[]       entryIdAtom;
    
    public int[]       dcListIdMain;                     // refs to dcList
    public int[]       dcListIdViol;
    public int[]       dcListIdNode;
    public int[]       dcListIdMemb;
    public int[]       dcListIdAtom;
    
    public int[]       dcMainIdViol;                     // refs to dcMain
    public int[]       dcMainIdNode;
    public int[]       dcMainIdMemb;
    public int[]       dcMainIdAtom;
    
    public int[]       dcNodeIdMemb;                     // refs to dcNode
    public int[]       dcNodeIdAtom;
    
    public int[]       dcMembIdAtom;                     // refs to dcMemb
    
    public int[]       atomIdAtom;                       // refs to atom
    public int[]       dcIdAtom;
    
    //public int[]       dcListIdMain;                    defined through super class.
    public int[]       numbNode;
    public int[]       numbMemb;
    //public int[]       numbAtom;
    //public int[]       numbViol;                    // To expect?
    public int[]       orderAtom;
    
    public float[]     violUppMax;              // non-fkcs in dcMain
    public int[]       violUppMaxModelNum;      // could be a fkc but not implemented as such
    public float[]     violLowMax;
    public int[]       violLowMaxModelNum;
    public BitSet      hasUnLinkedAtom;         // set to false if any atom in constraint could NOT be linked. true otherwise.
    public float[]     distTheo;                // distance over selected models for completeness analysis.
    
    
    public int[]       modelIdViol;                 // fkcs in dcViol.
    /** See xplor v 4.0 manual formula 18.1-4 */
    public float[]     distance;                    // non-fkcs in dcViol. Doesn't have a meaning in case of xplor's "high dim"
    /** See xplor v 4.0 manual formula 18.9 */
    public float[]     violation;                   // positive violation is a violation of the upper bound and negative one is on lower bound
    
    public int[]       nodeId;
    public int[]       downId;                       // non-fkcs in dcNode could have been modelled as fkcs but not done!
    public int[]       rightId;                      // they do refer to node id's and not the rid.
    public int[]       logicalOp;
    public float[]     target;
    public float[]     uppBound;
    public float[]     lowBound;
    public String[]    peakIdList;
    public StringSet   peakIdListNR;
    public float[]     weight;
    public float[]     contribution;
    
    public String[]    authMolNameList;              // non-fkcs in dcAtom
    public StringSet   authMolNameListNR;           // Only the first atom contains actual references, the other refs are null.
    public String[]    authResNameList;
    public StringSet   authResNameListNR;
    public String[]    authResIdList;
    public StringSet   authResIdListNR;
    public String[]    authAtomNameList;
    public StringSet   authAtomNameListNR;
    
    public String[]    atomNameList;
    public StringSet   atomNameListNR;
    public int[]       resIdList;
    public String[]    resNameList;
    public StringSet   resNameListNR;
    public int[]       molIdList;
    public int[]       entityIdList;
    
    
    
    static {
        for (int i=0;i<DEFAULT_LOGICAL_OPERATION_NAMES.length;i++) {
            logicalOperationString2Int.put( DEFAULT_LOGICAL_OPERATION_NAMES[i], new Integer(i));
        }
    }
    
    public DistConstr(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        resetConvenienceVariables();
    }
    
    /** The relationSetName is a parameter so non-standard relation sets
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public DistConstr(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        //General.showDebug("now in Atom.init()");
        super.init(dbms);
        //General.showDebug("back in Atom.init()");
        
        name = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME];
        String relationName = null;
        
        // MAIN RELATION in addition to the ones in gumbo item.
        if ( true ) {
            //General.showDebug("Adding columns and fkcs to relation dc_main");
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_MAX,                                            new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX,                                        new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM,                              new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM,                              new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM,                                   new Integer(DATA_TYPE_BIT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DIST_THEORETICAL,                                    new Integer(DATA_TYPE_FLOAT));
            
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_MAX);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DIST_THEORETICAL );
            
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[       RELATION_ID_MAIN_RELATION_NAME]});
            
            Relation relation = null;
            relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_MAIN_RELATION_NAME];
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
        }
        
        // NODE RELATION
        if ( true ) {
            //General.showDebug("Adding columns and fkcs to relation dc_node");
            DEFAULT_ATTRIBUTES_TYPES.clear();
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Relation.DEFAULT_ATTRIBUTE_NUMBER,                           new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DOWN_ID,                            new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_RIGHT_ID,                           new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_LOGIC_OP,                           new Integer(DATA_TYPE_INT));
            
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_TARGET,                              new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_UPP_BOUND,                           new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_LOW_BOUND,                           new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_PEAK_ID,                             new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_WEIGHT,                              new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_CONTRIBUTION,                        new Integer(DATA_TYPE_FLOAT));
            
            DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Relation.DEFAULT_ATTRIBUTE_NUMBER);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DOWN_ID);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_RIGHT_ID);
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_LOGIC_OP);
            
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_TARGET      );
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_UPP_BOUND   );
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_LOW_BOUND   );
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_PEAK_ID     );
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_WEIGHT      );
            DEFAULT_ATTRIBUTES_ORDER.add(Constr.DEFAULT_ATTRIBUTE_CONTRIBUTION);
            
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC[         RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[       RELATION_ID_MAIN_RELATION_NAME]});
            Relation relation = null;
            relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_node";
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
            distConstrNode = relation;
        }
        
        // VIOL RELATION
        if ( true ) {
            //General.showDebug("Adding columns and fkcs to relation dc_viol");
            DEFAULT_ATTRIBUTES_TYPES.clear();
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_VIOLATION                                         ,  new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DISTANCE                                          ,  new Integer(DATA_TYPE_FLOAT));
            DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_VIOLATION );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DISTANCE );
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[       RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC[         RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[       RELATION_ID_MAIN_RELATION_NAME]});
            Relation relation = null;
            relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_viol";
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
            distConstrViol = relation;
        }
        
        // MEMBER RELATION
        if ( true ) {
            //General.showDebug("Adding columns and fkcs to relation dc_member");
            DEFAULT_ATTRIBUTES_TYPES.clear();
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                          new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Relation.DEFAULT_ATTRIBUTE_NUMBER,  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID                                         );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Relation.DEFAULT_ATTRIBUTE_NUMBER );
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID                                        ,    Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_node"});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC[         RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[       RELATION_ID_MAIN_RELATION_NAME]});
            Relation relation = null;
            relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_member";
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
            distConstrMemb = relation;
        }
        
        
        // ATOM RELATION
        if ( true ) {
            //General.showDebug("Adding columns and fkcs to relation dc_atom");
            DEFAULT_ATTRIBUTES_TYPES.clear();
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                          new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                          new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME,                                        new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME,                                        new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID,                                          new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME,                                       new Integer(DATA_TYPE_STRINGNR));
            
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME,                                            new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_RES_ID,                                               new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME,                                             new Integer(DATA_TYPE_STRINGNR));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_MOL_ID,                                               new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID,                                            new Integer(DATA_TYPE_INT));
            DEFAULT_ATTRIBUTES_TYPES.put( Relation.DEFAULT_ATTRIBUTE_ORDER_ID,                                          new Integer(DATA_TYPE_INT));
            
            DEFAULT_ATTRIBUTES_ORDER = new ArrayList();
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID                                         );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID                                         );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ] );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME                                       );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME                                       );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID                                         );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME                                      );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME                                    );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_RES_ID                                       );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_RES_NAME                                     );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_MOL_ID                                       );
            DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID                                    );
            DEFAULT_ATTRIBUTES_ORDER.add( Relation.DEFAULT_ATTRIBUTE_ORDER_ID                                  );
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[        RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID                                        ,    Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_member"});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID                                        ,    Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_node"});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC[         RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
            DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[    RelationSet.RELATION_ID_COLUMN_NAME ],    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[       RELATION_ID_MAIN_RELATION_NAME]});
            distConstrAtom = null;
            relationName = Constr.DEFAULT_ATTRIBUTE_SET_DC[RELATION_ID_SET_NAME]+"_atom";
            try {
                distConstrAtom = new Relation(relationName, dbms, this);
            } catch ( Exception e ) {
                General.showThrowable(e);
                return false;
            }
            // Create the fkcs without checking that the columns exist yet.
            DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);
            if ( ! distConstrAtom.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER,
                    DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS)) {
                General.showError("Failed to insert columns for dc_atom relation.");
                return false;
            }
            addRelation( distConstrAtom );
            
            // Adjust for the fact that not all atom refs need to be valid.
            ForeignKeyConstr fkc = dbms.foreignKeyConstrSet.getForeignKeyConstrFrom(relationName, Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ]);
            if ( fkc == null ) {
                General.showError("Failed to get fkc for dc atom to atom");
                return false;
            }
            fkc.refNullable = true;
        }
        return true;
    }
    
    /**     */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        //General.showDebug("Now in resetConvenienceVariables in DistConstr");
        
        if ( true ) {
            entryIdMain          =             mainRelation.getColumnInt(    Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME]);
            entryIdAtom          =             distConstrAtom.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME]);
            entryIdNode          =             distConstrNode.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME]);
            entryIdMemb          =             distConstrMemb.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME]);
            entryIdViol          =             distConstrViol.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[  RELATION_ID_COLUMN_NAME]);
            
            dcListIdMain          =             mainRelation.getColumnInt(    Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[  RELATION_ID_COLUMN_NAME]);
            dcListIdAtom          =             distConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[  RELATION_ID_COLUMN_NAME]);
            dcListIdMemb          =             distConstrMemb.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[  RELATION_ID_COLUMN_NAME]);
            dcListIdNode          =             distConstrNode.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[  RELATION_ID_COLUMN_NAME]);
            dcListIdViol          =             distConstrViol.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[  RELATION_ID_COLUMN_NAME]);
            dcMainIdAtom          =             distConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC[  RELATION_ID_COLUMN_NAME]);
            dcMainIdMemb          =             distConstrMemb.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC[  RELATION_ID_COLUMN_NAME]);
            dcMainIdNode          =             distConstrNode.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC[  RELATION_ID_COLUMN_NAME]);
            dcMainIdViol          =             distConstrViol.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC[  RELATION_ID_COLUMN_NAME]);
            dcNodeIdAtom          =             distConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID);
            dcNodeIdMemb          =             distConstrMemb.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID);
            dcMembIdAtom          =             distConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID);
            atomIdAtom            =             distConstrAtom.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RELATION_ID_COLUMN_NAME ]);
            dcIdAtom              =             distConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_DC[      RELATION_ID_COLUMN_NAME ]);
            numbNode              =             distConstrNode.getColumnInt(  Relation.DEFAULT_ATTRIBUTE_NUMBER );
            numbMemb              =             distConstrMemb.getColumnInt(  Relation.DEFAULT_ATTRIBUTE_NUMBER );
            
            violUppMax            = (float[])   mainRelation.getColumn(     Constr.DEFAULT_ATTRIBUTE_VIOL_MAX );
            violLowMax            = (float[])   mainRelation.getColumn(     Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX );
            violUppMaxModelNum    =             mainRelation.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_VIOL_UPP_MAX_MODEL_NUM );
            violLowMaxModelNum    =             mainRelation.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_VIOL_LOW_MAX_MODEL_NUM );
            hasUnLinkedAtom       =             mainRelation.getColumnBit(  Constr.DEFAULT_ATTRIBUTE_HAS_UNLINKED_ATOM );
            distTheo              = (float[])   mainRelation.getColumn(     Constr.DEFAULT_ATTRIBUTE_DIST_THEORETICAL );
            
            modelIdViol           =             distConstrViol.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME ] );
            distance              = (float[])   distConstrViol.getColumn(     Constr.DEFAULT_ATTRIBUTE_DISTANCE );
            violation             = (float[])   distConstrViol.getColumn(     Constr.DEFAULT_ATTRIBUTE_VIOLATION );
            
            nodeId                =             distConstrNode.getColumnInt(  Relation.DEFAULT_ATTRIBUTE_NUMBER );
            downId                =             distConstrNode.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_DOWN_ID );
            rightId               =             distConstrNode.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_RIGHT_ID );
            logicalOp             =             distConstrNode.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_LOGIC_OP );
            target                  = (float[])   distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_TARGET );
            uppBound                = (float[])   distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_UPP_BOUND );
            lowBound                = (float[])   distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_LOW_BOUND );
            peakIdList              = (String[])  distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_PEAK_ID);
            peakIdListNR            =             distConstrNode.getColumnStringSet( Constr.DEFAULT_ATTRIBUTE_PEAK_ID);
            weight                  = (float[])   distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_WEIGHT );
            contribution            = (float[])   distConstrNode.getColumn(     Constr.DEFAULT_ATTRIBUTE_CONTRIBUTION );
            
            authMolNameList     = (String[])    distConstrAtom.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME );
            authMolNameListNR   =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_MOL_NAME );
            authResNameList     = (String[])    distConstrAtom.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME );
            authResNameListNR   =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_NAME );
            authResIdList       = (String[])    distConstrAtom.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID );
            authResIdListNR     =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_RES_ID );
            authAtomNameList    = (String[])    distConstrAtom.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME );
            authAtomNameListNR  =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_AUTH_ATOM_NAME);
            
            atomNameList     = (String[])    distConstrAtom.getColumn(           Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME );
            atomNameListNR   =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_ATOM_NAME );
            resNameList      = (String[])    distConstrAtom.getColumn(           Gumbo.DEFAULT_ATTRIBUTE_RES_NAME );
            resNameListNR    =               distConstrAtom.getColumnStringSet(  Gumbo.DEFAULT_ATTRIBUTE_RES_NAME );
            resIdList        = distConstrAtom.getColumnInt(           Gumbo.DEFAULT_ATTRIBUTE_RES_ID );
            molIdList        = distConstrAtom.getColumnInt(           Gumbo.DEFAULT_ATTRIBUTE_MOL_ID );
            entityIdList     = distConstrAtom.getColumnInt(           Gumbo.DEFAULT_ATTRIBUTE_ENTITY_ID );
            orderAtom        = distConstrAtom.getColumnInt(           Relation.DEFAULT_ATTRIBUTE_ORDER_ID );
            
            if (
                    entryIdMain          == null ||
                    entryIdViol          == null ||
                    entryIdNode          == null ||
                    entryIdMemb          == null ||
                    entryIdAtom          == null ||
                    dcListIdMain       == null ||
                    dcListIdAtom       == null ||
                    dcListIdMemb       == null ||
                    dcListIdNode       == null ||
                    dcListIdViol       == null ||
                    dcMainIdAtom       == null ||
                    dcMainIdMemb       == null ||
                    dcMainIdNode       == null ||
                    dcMainIdViol       == null ||
                    dcNodeIdAtom       == null ||
                    dcNodeIdMemb       == null ||
                    dcMembIdAtom       == null ||
                    atomIdAtom         == null ||
                    dcIdAtom           == null ||
                    numbNode           == null ||
                    numbMemb           == null ||
                    violUppMax         == null ||
                    violLowMax         == null ||
                    violUppMaxModelNum == null ||
                    violLowMaxModelNum == null ||
                    hasUnLinkedAtom    == null ||
                    distTheo           == null ||
                    nodeId             == null ||
                    downId             == null ||
                    rightId            == null ||
                    logicalOp          == null ||
                    modelIdViol        == null ||
                    distance           == null ||
                    violation          == null ||
                    target             == null ||
                    uppBound           == null ||
                    lowBound           == null ||
                    peakIdList         == null ||
                    peakIdListNR       == null ||
                    weight             == null ||
                    contribution       == null ||
                    authMolNameList    == null ||
                    authMolNameListNR  == null ||
                    authResNameList    == null ||
                    authResNameListNR  == null ||
                    authResIdList      == null ||
                    authResIdListNR    == null ||
                    authAtomNameList   == null ||
                    authAtomNameListNR == null ||
                    atomNameList       == null ||
                    atomNameListNR     == null ||
                    resNameList        == null ||
                    resNameListNR      == null ||
                    resIdList          == null ||
                    molIdList          == null ||
                    entityIdList       == null ||
                    orderAtom          == null
                    ) {
                return false;
            }
        }
        //General.showDebug("Done dc resetConvenienceVariables");
        return true;
    }
    
    /** Simple function. Returns Defs.NULL_STRING_NULL if id is null.
     *Returns a descriptive string when the id is not in the known range.
     */
    public static String getLogicalOperation(int id) {
        if ( Defs.isNull( id )) {
            return Defs.NULL_STRING_NULL;
        }
        if ( (id >= 0) && (id <  DEFAULT_LOGICAL_OPERATION_NAMES.length)) {
            return DEFAULT_LOGICAL_OPERATION_NAMES[id]; // MOST COMMON EXIT.
        }
        General.showError("failed to match logical operation to string representation for logical operation id: " + id);
        return "NO match logical operation to string representation for logical operation id: " + id;
    }
    
    /**
     * A Write new nodes, members and atoms.
     * B Return the the old nodes so they and their members and redundant dc atoms (those not updated) by a list of node rids
     *can be deleted.
     */
    boolean setContributions( int currentDCId,
            ArrayList contributions, IntArrayList dcNodes ) {
        
        //General.showDebug("Doing setContributions on currentDCId: " + currentDCId );
        int currentDCListId = dcListIdMain[   currentDCId ];
        int currentEntryId  = entryIdMain[    currentDCId ];
        
        // Step A
        int nodeCount = contributions.size();
        int membCount = nodeCount*2;
        if ( nodeCount > 1 ) {
            nodeCount++;
        }
        int atomCount = countAtoms( contributions );
        if ( atomCount == 0 ) {
            General.showError("Didn't expect that there are no atoms left after reordering contributions");
            return false;
        }
        
        //General.showDebug("Will create new nodes: " + nodeCount);
        //General.showDebug("Will create new membs: " + membCount);
        //General.showDebug("Will create new atoms: " + atomCount);
        BitSet dcNodesNew = distConstrNode.getNewRows(nodeCount);
        BitSet dcMembsNew = distConstrMemb.getNewRows(membCount);
        BitSet dcAtomsNew = distConstrAtom.getNewRows(atomCount);
        if ( dcNodesNew == null || dcMembsNew == null || dcAtomsNew == null ) {
            General.showError("Failed to allocate the new rows for nodes and/or members, atoms");
            return false;
        }
        resetConvenienceVariables(); // Perhaps we grew?
        int currentDCNodeId         = -1;
        int currentDCMembId         = -1;
        int currentDCAtomId         = -1;
        int logicalOperationInt     = Defs.NULL_INT;
        int downI                   = Defs.NULL_INT;
        int rightI                  = Defs.NULL_INT;
        int constrNodeIdOrg = 0;
        if ( dcNodes.size() > 1 ) { // there was a more complex constraint before.
            constrNodeIdOrg = 1;
        }
        int constrNodeRIDOrg = dcNodes.get( constrNodeIdOrg );
        int k = 0; // keeps track of where in contributions we are.
        for ( int nodeNumber = 1; nodeNumber <= nodeCount; nodeNumber++ ) {
            currentDCNodeId = dcNodesNew.nextSetBit( currentDCNodeId + 1 );
            //General.showDebug( "**********for node number: " + nodeNumber + " rid: " + currentDCNodeId );
            
            if ( nodeCount > 1 ) {
                if ( nodeNumber == 1 ) {
                    logicalOperationInt = DEFAULT_LOGICAL_OPERATION_ID_OR;
                    downI = 2;
                    rightI = Defs.NULL_INT;
                } else {
                    logicalOperationInt = Defs.NULL_INT;
                    downI = Defs.NULL_INT;
                    if ( nodeNumber < nodeCount ) {
                        rightI = nodeNumber + 1;
                    } else {
                        rightI = Defs.NULL_INT;
                    }
                }
            } else {
                logicalOperationInt = Defs.NULL_INT;
                downI               = Defs.NULL_INT;
                rightI              = Defs.NULL_INT;
            }
            
            
            //General.showDebug( "Dc node is a constraint node and not a logical node." );
            // The next copy takes care of all attributes like contribution, weight, etc.
            //General.showDebug("Doing copy node row from: " + constrNodeRIDOrg + " to: " + currentDCNodeId);
            if ( ! distConstrNode.copyRow( constrNodeRIDOrg, currentDCNodeId) ) {
                General.showError( "Failed to do copy row on dc node table from/to: " + constrNodeRIDOrg + "/" + currentDCNodeId);
                return false;
            }
            
            /** already done.
             * dcMainIdNode[    currentDCNodeId ] = currentDCId;
             * dcListIdNode[    currentDCNodeId ] = currentDCListId;
             * entryIdNode[     currentDCNodeId ] = currentEntryId;
             */
            nodeId[          currentDCNodeId ] = nodeNumber;
            downId[          currentDCNodeId ] = downI;
            rightId[         currentDCNodeId ] = rightI;
            logicalOp[       currentDCNodeId ] = logicalOperationInt;
            
            if ( ! Defs.isNull( logicalOperationInt ) ) { // only continue here if there is no logical operation set.
                //return true;
                //General.showDebug("Not creating members, atoms, etc. for logical op. node");
                continue;
            }
            
            Object[] contrib = (Object[]) contributions.get(k++); // k is only incremented after get and only for constraint nodes.
            //General.showDebug("Contribution: " + PrimitiveArray.toStringMakingCutoff( contrib ));
            for (int m=1;m<=2;m++) {
                currentDCMembId = dcMembsNew.nextSetBit( currentDCMembId + 1 );
                //General.showDebug( "**********for memb id: " + m + " rid: " + currentDCMembId );
                dcNodeIdMemb[    currentDCMembId ] = currentDCNodeId;  // funny enough this table has only all-fkc columns
                dcMainIdMemb[    currentDCMembId ] = currentDCId;
                dcListIdMemb[    currentDCMembId ] = currentDCListId;
                entryIdMemb[     currentDCMembId ] = currentEntryId;
                numbMemb[        currentDCMembId ] = m;
                // Create new dc atoms by copying old ones.
                IntArrayList dcAR   = (IntArrayList) contrib[2];
                if ( m == 2 ) {
                    dcAR    = (IntArrayList) contrib[3];
                }
                int dcARSize = dcAR.size();
                for (int a=0;a<dcARSize;a++) {
                    int currentDCAtomIdOrg = dcAR.getQuick(a);
                    currentDCAtomId = dcAtomsNew.nextSetBit( currentDCAtomId + 1 );
                    //General.showDebug("Doing copy dc atom row from: " + currentDCAtomIdOrg + " to: " + currentDCAtomId);
                    if ( ! distConstrAtom.copyRow( currentDCAtomIdOrg, currentDCAtomId) ) {
                        General.showError( "Failed to do copy row on dc atom table from/to: " + currentDCAtomIdOrg + "/" + currentDCAtomId);
                        return false;
                    }
                    //General.showDebug( "After copy, replacing some info into dc atom rid: " + currentDCAtomId + " from dc atom rid: " + currentDCAtomIdOrg);
                    orderAtom[       currentDCAtomId ] = Defs.NULL_INT; // just null it so the physical order can be taken up on writing out.
                    dcMembIdAtom[    currentDCAtomId ] = currentDCMembId;
                    dcNodeIdAtom[    currentDCAtomId ] = currentDCNodeId; // This will also ensure that they will not be removed.
                }
            } // end of loop over members
        } // end of loop over nodes.
        return true;
    }
    
    /** Return the contributions for the given dc rid as they appear in the internal table but now in an object oriented way.*/
    ArrayList getContributions( int currentDCId, IndexSortedInt indexMembAtom, IndexSortedInt indexNodeMemb , IndexSortedInt indexMainNode, boolean returnIndividualContributions ) {
        //General.showDebug("Doing getContributions for constraint with rid: " + currentDCId );
        if ( returnIndividualContributions ) {
            General.showWarning("Returning individual contributions but do we really need that?");
        }
        Integer currentDCIdInteger = new Integer(currentDCId);
//        int currentDCListId = dcListIdMain[   currentDCId ];
//        int currentEntryId  = entryIdMain[    currentDCId ];
        
        IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        int dcNodesSize = dcNodes.size();
        if ( dcNodesSize < 1 ) {
            General.showError("Got less than 1 node for constraint");
            return null;
        }
        
        ArrayList contributions = new ArrayList(dcNodesSize);
        // FOR EACH NODE
        for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodesSize; currentDCNodeBatchId++) {
            int currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId );
            //General.showDebug("Doing node with rid: " + currentDCNodeId );
            
            // For each constraint node (those with distance etc but without logic) add the distance info to
            // the distance star loop.
            int logOp = logicalOp[currentDCNodeId];
            if ( ! Defs.isNull( logOp ) ) {
                if ( logOp != DEFAULT_LOGICAL_OPERATION_ID_OR ) {
                    General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]");
                    break;
                }
                continue;
            }
            IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            if ( dcMembs.size() != 2 ) {
                General.showError("Are we using a number different than 2 as the number of members in a node? Can't calculte distance for it yet.");
                return null;
            }
            int currentDCMembBatchId=0;
            int currentDCMembId  = dcMembs.get(currentDCMembBatchId);
            int currentDCMembIdJ = dcMembs.get(currentDCMembBatchId+1);
            //General.showDebug("Working from member with RID: " + currentDCMembId);
            
            IntArrayList dcAtomRids = (IntArrayList) indexMembAtom.getRidList(
                    new Integer(currentDCMembId), Index.LIST_TYPE_INT_ARRAY_LIST, null );
            //General.showDebug("dcAtomRids: " + PrimitiveArray.toStringMakingCutoff( dcAtomRids ));
            IntArrayList atomRids = distConstrAtom.getValueListBySortedIntIndex(
                    indexMembAtom,
                    currentDCMembId,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
                    null );
            if ( dcAtomRids.size() != atomRids.size() ) {
                General.showError("Got 2 different sizes for atom lists which should be of same size: " + dcAtomRids.size() + " and " + atomRids.size() );
                return null;
            }
            //General.showDebug("Found the following rids of atoms in member: " + PrimitiveArray.toStringMakingCutoff( atomRids ));
            if ( dcAtomRids.size() < 1 ) {
                General.showError("Didn't find a single DC atom for member in constraint");
                return null;
            }
            if ( atomRids.size() < 1 ) {
                General.showError("Didn't find a single atom for member in constraint");
                return null;
            }
            //General.showDebug("Working from member with RID: " + currentDCMembIdJ);
            
            IntArrayList dcAtomRidsJ = (IntArrayList) indexMembAtom.getRidList(
                    new Integer(currentDCMembIdJ), Index.LIST_TYPE_INT_ARRAY_LIST, null );
            //General.showDebug("dcAtomRidsJ: " + PrimitiveArray.toStringMakingCutoff( dcAtomRidsJ ));
            IntArrayList atomRidsJ = distConstrAtom.getValueListBySortedIntIndex(
                    indexMembAtom,
                    currentDCMembIdJ,
                    Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[     RelationSet.RELATION_ID_COLUMN_NAME ],
                    null );
            if ( dcAtomRidsJ.size() != atomRidsJ.size() ) {
                General.showError("Got 2 different sizes for atom lists which should be of same size: " + dcAtomRidsJ.size() + " and " + atomRidsJ.size() );
                return null;
            }
            //General.showDebug("Found the following rids of atoms in member J: " + PrimitiveArray.toStringMakingCutoff( atomRidsJ ));
            if ( atomRidsJ.size() < 1 ) {
                General.showError("Didn't find a single atom for member J in constraint");
                return null;
            }
            if ( returnIndividualContributions ) {
                for (int k=0;k<dcAtomRids.size();k++) {
                    for (int l=0;l<dcAtomRidsJ.size();l++) {
                        IntArrayList a               = new IntArrayList(); // size of 1 garantees no growth needed at once.
                        IntArrayList a_Partner       = new IntArrayList();
                        IntArrayList dcA             = new IntArrayList();
                        IntArrayList dcA_Partner     = new IntArrayList();
                        // Kind of expensive to create the objects per contribution
                        contributions.add( new Object[] {a, a_Partner, dcA, dcA_Partner} );
                        // This could be as much as N**2 contributions with N the number of atoms in 1 model.
                        // Say 1,000 times 1,000 but usually only 1 * 1.
                        a.add(              atomRids.getQuick(      k ));
                        a_Partner.add(      atomRidsJ.getQuick(     l ));
                        dcA.add(            dcAtomRids.getQuick(    k ));
                        dcA_Partner.add(    dcAtomRidsJ.getQuick(   l ));
                    }
                }
            } else {
                // Kind of expensive to create the objects per contribution
                contributions.add( new Object[] {atomRids, atomRidsJ, dcAtomRids, dcAtomRidsJ} );
            }
        } // end of node loop
        //showContributions( contributions );
        return contributions;
    }

    /** Remove the largest violating distance restraints that meet a certain cutoff.
     *Violation are not averaged over models for this purpose.
     *Writes the removed restraints to a file.
     */
    public boolean filterHighDistanceViol(float cutoffValue, int maxRemove, String fileName) {
        int todoCount = selected.cardinality();
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in filterHighDistanceViol.");
            return true;
        }
        boolean encounteredFatalError = false;
        if ( ! calcViolation() ) {
            General.showError("Failed to do calcViolation");
            return false;
        }

        if ( ! mainRelation.insertColumn("tmpMaxViol",Relation.DATA_TYPE_FLOAT,null)) {
            General.showError("Failed to insertColumn tmpMaxViol");
            return false;            
        }
        
        try {         // encapsulate because we want to remove the tmp column just inserted.
            float[] maxViolOverallTmp = mainRelation.getColumnFloat("tmpMaxViol");
            float[] maxViolOverall = PrimitiveArray.getMax( violLowMax, violUppMax );
//            General.showDebug("violUppMax: " + PrimitiveArray.toString(maxViolOverall));
            for (int r=selected.nextSetBit(0);r>=0;r=selected.nextSetBit(r+1)) {
                maxViolOverallTmp[r] = maxViolOverall[r];
            }
            IndexSortedFloat index = (IndexSortedFloat) mainRelation.getIndex("tmpMaxViol",Index.INDEX_TYPE_SORTED);
            if ( index == null ) {
                throw new Exception("Failed to get index on tmpMaxViol");
            }
//            General.showDebug("index: " + index.toString(true,true));
            int[] rids = index.getRids();
            float[] values = index.getValues();
            int maxRemoveTodo = maxRemove;
            BitSet toRemove = new BitSet();
            int r = values.length -1;
            while ( maxRemoveTodo > 0 && r>=0 ) {
                if ( Defs.isNull(values[r])) { // there will be many here so optimize?
                    r--;
                    continue;
                }
                if ( values[r] < cutoffValue) {
                    break; // no more down
                }
                toRemove.set(rids[r]);
                maxRemoveTodo--;
                r--;
            }


            /** a different way of looking at it */
            BitSet result = SQLSelect.selectBitSet(dbms,mainRelation,"tmpMaxViol",SQLSelect.OPERATION_TYPE_GREATER_THAN_OR_EQUAL,
                    new Float(cutoffValue),false);
            
            
            General.showOutput("There are " + result.cardinality() + " restraints with violations > " + cutoffValue);
            General.showOutput("Will remove " + toRemove.cardinality() + " restraints with violations > " + cutoffValue);
            if ( result.cardinality() > toRemove.cardinality() ) {
                General.showWarning("There are more violatins over the threshold than will be removed");
            }
            
            BitSet checkSet = (BitSet) toRemove.clone();
            checkSet.andNot(result);
            if ( checkSet.cardinality() != 0 ) {
                General.showError("There are restraints marked to be removed that do not fullfill the criteria. Stopping to delete.");
                General.showError( General.eol + toString(checkSet,true,false));
                return false;
            }

            if ( fileName.equals(Defs.NULL_STRING_DOT) || toRemove.cardinality() == 0 ) {
                General.showOutput("Skipping write of restraints to be removed.");
            } else {
                boolean showViolations = true;
                boolean showTheo = false;
                String msg = toSTAR(toRemove, CUTOFF_REPORTING_VIOLATIONS, showViolations, showTheo);
                if ( msg == null ) {
                    String txt = "Failed to do constr.toSTAR";
//                    General.showError(txt);
                    throw new Exception(txt);
                }
//                General.showDebug("Came back from toSTAR");
                if ( ! InOut.writeTextToFile(new File(fileName),msg,true, false)) {
                    String txt = "Failed to write violations to be removed; not actually removing.";
//                    General.showError(txt);
                    throw new Exception(txt);
                }
            }
            if ( ! mainRelation.removeRowsCascading(toRemove,false)) {
                    String txt = "Failed to remove violations.";
//                    General.showError(txt);
                    throw new Exception(txt);                
            }            
        } catch ( Throwable t ) {
            General.showThrowable(t);
            encounteredFatalError = true;
        }
//        General.showDebug("Removing tmp column");
        if ( mainRelation.removeColumn("tmpMaxViol") == null ) {
            General.showError("Failed to mainRelation.removeColumn(\"tmpMaxViol\")");
            return false;            
        }
        
//        General.showDebug("Exiting filter routine.");
        return ! encounteredFatalError;
    }

    /** Calculate the DC violations for a given cut off.
     */
    public boolean calcDistConstraintViolation(float cutoffValue, String fileName) {
        /**
         * BitSet sel = getSelectionWithViolation(cutoff);
         * if ( sel == null ) {
         * General.showError("Failed calcDistConstraintViolation");
         * return false;
         * }
         * int violationCount = sel.cardinality();
         * General.showOutput("From selected constraints numbered: " + used.cardinality() +
         * " there are the following number of constraints with violations above the threshold: " + cutoff +
         * " namely: " + violationCount);
         */
        // Show only the selected ones.
        boolean showViolations = true;
        boolean showTheo = false;
        
        String msg = toSTAR(selected, cutoffValue, showViolations, showTheo);
        if ( msg == null ) {
            General.showError("Failed to do constr.toSTAR");
            return false;
        }
        if ( ! InOut.writeTextToFile(new File(fileName),msg,true, false)) {
            General.showError("Failed to write summary for violations");
        }
        return true;
    }
    
    /** Return a bitset of selected constraints in which the absolute value of the violation is
     *at least the value given for at least one model.
     *Returns null on failure. Note that in the current implementation the violation is always
     * set to be positive.
     *Note that before calling this method the violations will be precalculated by this routine.
     *
     */
    public BitSet getSelectionWithViolation( float max_viol_report ) {
        
        if ( ! calcViolation() ) {
            General.showError("Failed to calcDistance.");
            return null;
        }
        
        BitSet result = new BitSet();
        IndexSortedInt indexMainViol = (IndexSortedInt) distConstrViol.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMainViol == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        
        // FOR EACH CONSTRAINT
        for (int currentDCId = selected.nextSetBit(0);currentDCId>=0;currentDCId = selected.nextSetBit(currentDCId+1)) {
            IntArrayList dcViols = (IntArrayList) indexMainViol.getRidList(  new Integer(currentDCId),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            // FOR EACH MODEL
            for (int m=0;m<dcViols.size();m++) {
                int currentDCViolId = dcViols.getQuick(m);
                float absViol = violation[ currentDCViolId ];
                if ( Defs.isNull( absViol ) ) {
                    General.showCodeBug("Violation is null for restraint: " +
                            toString(currentDCId,true,false) + "in model: " + m);
                    return null;
                }
                absViol = Math.abs( absViol );
                if ( absViol >= max_viol_report ) {
                    result.set( currentDCId );
                    break; // dont' do the other models; this is already a hit.
                }
            }
        }
        return result;
    }
    
    /** Convenience method that returns the 4 numbers */
    public float[] getLowTargetUppTheoBound( int rid ) {
        float[] result = new float[4];
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        Integer currentDCIdInteger = new Integer(rid);
        IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        // Using the last current dc node id might not be correct here. Preference for using the
        // first 'constraint' current dc node id because that's the most likely place for the input
        // data to occur.
        int currentDCNodeId = dcNodes.getQuick(dcNodes.size()-1);
        result[LOW_IDX] = lowBound[ currentDCNodeId ]; // cache some variables for speed and convenience.
        result[TAR_IDX] = target[   currentDCNodeId ];
        result[UPP_IDX] = uppBound[ currentDCNodeId ];
        result[THE_IDX] = distTheo[ rid ];
        return result;
    }
    
    
    /** Convenience method
     */
    public boolean calcViolation() {
        return calcViolation(selected, CUTOFF_REPORTING_VIOLATIONS);
    }
    
    
    /** Calculate the averaged distance for the each given constraint in all
     *selected models and puts all as new entries in the violation
     *relation. The violation will often be zero.
     *Presumes the model sibling atoms are ok if initialized. 
     * Skips constraints with unknown atoms.
     */
    public boolean calcViolation(BitSet todo, float cutoff) {
        
        int todoCount = todo.cardinality();
//        StringBuffer sb = new StringBuffer();
        
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in toSTAR.");
            return true;
        }
        
        BitSet dcListRids = getDCListSetFromDCSet(todo);
        if ( dcListRids == null ) {
            General.showError("Failed getDCListSetFromDCSet");
            return false;
        }
        
        if ( dcListRids.cardinality() == 0 ) {
            General.showWarning("Got empty set from getDCListSetFromDCSet.");
            return true;
        }
        
        for ( int currentDCListId=dcListRids.nextSetBit(0);currentDCListId>=0;currentDCListId=dcListRids.nextSetBit(currentDCListId+1)) {
            if ( ! constr.dcList.calcViolation(todo,currentDCListId,cutoff)) {
                General.showError("Failed to calcDistance for dc list: " + currentDCListId);
                return false;
            } else {
//                General.showDebug("violation relation has number of elements: " + distConstrViol.used.cardinality());
//                General.showDebug(distConstrViol.toString(true,true,false,true,true,false));
            }
        }
        return true;
    }
    
    
    /** Calculate the averaged distance for the given constraint in all
     *given models.
     *Presumes the model sibling atoms are ok if initialized.
     *The parameter selectedModelArray needs to contain the rids.
     *If the parameter is null then all models
     *will be used. The models are derived from the sibling list of the first
     *atom in the restraint.
     *A returned result with a Defs.NULL_FLOAT for the first element indicates
     *the restraint has unlinked atoms or a failure to find all atoms.
     */
    public float[] calcDistance(int currentDCId, int[] selectedModelArray  ) {
        
        IndexSortedInt indexMembAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        
        if ( indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        
        int currentDCNodeId         = Defs.NULL_INT;
        //int currentDCId             = Defs.NULL_INT;
        int currentDCListId         = Defs.NULL_INT;
//        int currentDCEntryId        = Defs.NULL_INT;

        int modelCount = 0;
        if ( selectedModelArray == null ) {
            IndexSortedInt indexMainAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
            if ( indexMainAtom == null ) {
                General.showCodeBug("Failed to get indexMainAtom index.");
                return null;
            }
            IntArrayList dcAtoms = (IntArrayList) indexMainAtom.getRidList(  new Integer(currentDCId),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            int dcAtomRid = dcAtoms.get(0);
            int atomRidFirstAtom = atomIdAtom[dcAtomRid];
            if ( Defs.isNull(atomRidFirstAtom)) {
                General.showError("Failed to calcDistance for restraint: " + toString(currentDCId));
                return null;
            }
            int[] tmp = gumbo.atom.modelSiblingIds[atomRidFirstAtom];
            if ( tmp == null ) {
                General.showError("found an empty gumbo.atom.modelSiblingIds["+atomRidFirstAtom+"]");
                General.showError("where the atom is: " + gumbo.atom.toString(atomRidFirstAtom));
                return null;
            }
            modelCount = tmp.length;
            selectedModelArray = new int[modelCount];
            for (int m=0;m<modelCount;m++) { // And really fill it.
                selectedModelArray[m] = gumbo.atom.modelId[tmp[m]];
            }
        } else {
            modelCount = selectedModelArray.length;
        }
        float[] result = new float[modelCount];
        result[0] = Defs.NULL_FLOAT; // Indication of the restraint has unlinked atoms or a failure to find all atoms.
        
        if ( hasUnLinkedAtom.get( currentDCId )) {
            General.showDetail("Skipping distance calculation for constraint at rid: " + currentDCId + " because not all atoms are linked." );
            return result;
        }
        currentDCListId  = dcListIdMain[    currentDCId ];
//        currentDCEntryId = entryIdMain[     currentDCId ];
        int avgMethod      = constr.dcList.avgMethod[        currentDCListId ];
        int numberMonomers = constr.dcList.numberMonomers[   currentDCListId ];
        boolean atomFound = true; // signals at least one atom could not be found when 'false'
        Integer currentDCIdInteger = new Integer(currentDCId);
        IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toStringMakingCutoff( dcNodes ));
        
        int dCNodeNumber = 1;
        // Define a list of n constraint nodes with 2 sets of atoms so elements are of type IntArrayList[2]
        ArrayList atomsInvolved = new ArrayList();
        
        
        // FOR EACH NODE
        for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodes.size(); currentDCNodeBatchId++) {
            currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId ); // quick enough?;-)
            //General.showDebug("Using distance constraint node: " + dCNodeNumber + " at rid: " + currentDCNodeId );
            
            // For each constraint node (those with distance etc but without logic) add the distance info to
            // the distance star loop.
            int logOp = logicalOp[currentDCNodeId];
            if ( ! Defs.isNull( logOp ) ) {
                if ( logOp != DEFAULT_LOGICAL_OPERATION_ID_OR ) {
                    General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]");
                    return null;
                }
                dCNodeNumber++;
                continue;
            }
            // Assumption at this point is that the constraint nodes are OR-ed. Change code here to
            // allow different type of trees.
            //General.showDebug( "Dc node is a constraint node and not a logical node." );
            
            // FOR BOTH MEMBER (usually just 2)
            IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            //General.showDebug("Found the following rids of members in constraint node (" + currentDCNodeId + "): " + PrimitiveArray.toStringMakingCutoff( dcMembs ));
            if ( dcMembs.size() != 2 ) {
                General.showError("Are we using a number different than 2 as the number of members in a node? Can't calculate distance for it yet.");
                return null;
            }
            int currentDCMembIdA = dcMembs.get(0);
            int currentDCMembIdB = dcMembs.get(1);
            //General.showDebug("Working on members with RID: " + currentDCMembIdA + " and " + currentDCMembIdB);
            
            IntArrayList dcAtomsA = (IntArrayList) indexMembAtom.getRidList(  new Integer(currentDCMembIdA),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            IntArrayList dcAtomsB = (IntArrayList) indexMembAtom.getRidList(  new Integer(currentDCMembIdB),
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
            // Get the real atom ids into a new list.
            IntArrayList atomRidsA = new IntArrayList();
            IntArrayList atomRidsB = new IntArrayList();
            atomRidsA.setSize( dcAtomsA.size() );
            atomRidsB.setSize( dcAtomsB.size() );
            for (int i=0;i<dcAtomsA.size();i++) {
                int rid = atomIdAtom[ dcAtomsA.getQuick(i) ];
                if ( Defs.isNull( rid )) {
                    General.showWarning("Failed to find atom; that should have been recorded before");
                    atomFound = false;
                    break;
                }
                atomRidsA.setQuick( i, rid );
            }
            for (int i=0;i<dcAtomsB.size();i++) {
                int rid = atomIdAtom[ dcAtomsB.getQuick(i) ];
                if ( Defs.isNull( rid )) {
                    General.showError("Failed to find atom; that should have been recorded before");
                    atomFound = false;
                    break;
                }
                atomRidsB.setQuick( i, rid );
            }
            if ( ! atomFound ) {
                break;
            }
            //General.showDebug("Found the following rids of atoms in member A: " + PrimitiveArray.toStringMakingCutoff( atomRidsA ));
            //General.showDebug("Found the following rids of atoms in member B: " + PrimitiveArray.toStringMakingCutoff( atomRidsB ));
            if ( atomRidsA.size() < 1 ) {
                General.showError("Didn't find a single atom for A member in constraint");
                return null;
            }
            if ( atomRidsB.size() < 1 ) {
                General.showError("Didn't find a single atom for B member in constraint");
                return null;
            }
            atomsInvolved.add( new IntArrayList[] { atomRidsA, atomRidsB } );
            dCNodeNumber++;
        } // end of loop for each node
        if ( ! atomFound ) {
            General.showWarning("Failed to find all atoms in constraint: " + currentDCId + "That should have been recorded before.");
            return result;
        }
        
        // When the set of atoms involved is collected for the first model, the other models can easily be done too.
        // FOR EACH selected MODEL
        for ( int currentModelId=0; currentModelId<modelCount; currentModelId++) {
            //General.showDebug("Working on model: " + (currentModelId+1)); // used to seeing model numbers starting at 1.
            // Get a new list of atoms for other models
            // The atoms in the first model need not be special coded although it could be done faster.
            ArrayList atomsInvolvedModel = new ArrayList();
            for (int p=0;p<atomsInvolved.size();p++) {
                IntArrayList[] atomsInvolvedNode = (IntArrayList[]) atomsInvolved.get(p);
                IntArrayList dcAtomsA = atomsInvolvedNode[0];
                IntArrayList dcAtomsB = atomsInvolvedNode[1];
                IntArrayList dcAtomsANew = new IntArrayList();
                IntArrayList dcAtomsBNew = new IntArrayList();
                dcAtomsANew.setSize( dcAtomsA.size() );
                dcAtomsBNew.setSize( dcAtomsB.size() );
                for (int i=0;i<dcAtomsA.size();i++) {
                    dcAtomsANew.setQuick( i, gumbo.atom.modelSiblingIds[ dcAtomsA.getQuick(i) ][currentModelId]);
                }
                for (int i=0;i<dcAtomsB.size();i++) {
                    dcAtomsBNew.setQuick( i, gumbo.atom.modelSiblingIds[ dcAtomsB.getQuick(i) ][currentModelId]);
                }
                //General.showDebug("Found the following rids of atoms in model in member A: " + PrimitiveArray.toStringMakingCutoff( dcAtomsANew ));
                //General.showDebug("Found the following rids of atoms in model in member B: " + PrimitiveArray.toStringMakingCutoff( dcAtomsBNew ));
                atomsInvolvedModel.add(new IntArrayList[] {dcAtomsANew, dcAtomsBNew});
            }
            
            float dist = gumbo.atom.calcDistance( atomsInvolvedModel, avgMethod, numberMonomers );
            if ( Defs.isNull( dist ) ) {
                General.showError("Failed to calculate the distance for constraint: " + currentDCId + " in model: " + (currentModelId+1) + " will try other models.");
                return null;
            }
            result[currentModelId]=dist;
        } // end of loop per model
        return result;
    }
    
    /** Simple function */
    private int countAtoms( ArrayList contributions ) {
        int atomCount = 0;
        for (int k=0;k<contributions.size();k++) {
            //General.showDebug("Counting atoms in contribution : " + k );
            Object[] contrib = (Object[]) contributions.get(k);
            IntArrayList atomRid     = (IntArrayList) contrib[0];
            IntArrayList atomRidJ    = (IntArrayList) contrib[1];
            atomCount += atomRid.size();
            atomCount += atomRidJ.size();
        }
        return atomCount;
    }
    
    /** Sort the todo elements in this list. The number attribute will be renumbered to start with offset.
     *The following types of sorts will be executed.
     *atoms, members, nodes and constraints within this selection
     * regardless of whether they belong to the same dc list or even entry.
     *Make sure caller indexes AND convenience variables are reset because the set really
     *changes. The only garantee is that the constraint rid will remain valid and the same.
     *This method will reset the indexes and convenience variables of this class.
     */
    public boolean sortAll(BitSet todo, int offset) {
        int todoSize = todo.cardinality();
        General.showDebug("Sorting constraints numbered: " + todoSize);
        IntArrayList dcNodesToBeRemoved = new IntArrayList( todo.length()*3 ); // 3 is a conservative estimate, might be more.
        
        //General.showDebug("Starting routine: setSelectionDoubles_Type_2");
        IndexSortedInt indexMembAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }
        int i = -1;
        for (int currentDCId=todo.nextSetBit(0); currentDCId>=0; currentDCId=todo.nextSetBit(currentDCId+1) )  {
            Integer currentDCIdInteger = new Integer(currentDCId);
            i++;
            //General.showDebug("Sorting constraint internally: " + i);
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList( currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            ArrayList contributions = getContributions( currentDCId, indexMembAtom, indexNodeMemb, indexMainNode, false );
            if ( contributions == null ) {
                General.showCodeBug("getContributions failed in sortAll");
                return false;
            }
            sortContributions( contributions, gumbo );
            if ( ! setContributions( currentDCId, contributions, dcNodes ) ) {
                General.showError("Failed to put contributions back into table.");
                return false;
            }
            dcNodesToBeRemoved.addAllOf( dcNodes );
        }
        // Remove old nodes by the bunch so we save time; mainly because index doesn't need to be regenerated for each constraints.
        int dcNodeSizeOld = distConstrNode.sizeRows;
        if ( ! distConstrNode.removeRowsCascading(dcNodesToBeRemoved, false)) {
            General.showError("Failed to do cascading remove on dc nodes");
            return false;
        }
        if ( distConstrNode.sizeRows >= dcNodeSizeOld ) {
            General.showError("After cascading remove expected some removes (new/old): " + distConstrNode.sizeRows +"/" + dcNodeSizeOld);
            return false;
        }
        
        if ( ! sort(todo, offset)) {
            General.showError("Failed to do sort of constraints");
            return false;
        }
        if ( ! resetConvenienceVariables()) {
            General.showError("Failed to resetConvenienceVariables");
            return false;
        }
        if ( ! removeAllIndices()) {
            General.showError("Failed to removeAllIndices");
            return false;
        }
        return true;
    }
    
    /** Assumes the constraints are already sorted by node, member, and atom.
     */
    private boolean sort(BitSet todo, int offset) {
        int todoSize = todo.cardinality();
        Object[] elements = new Object[todoSize];
        int i = -1;
        for (int currentDCId=todo.nextSetBit(0); currentDCId>=0; currentDCId=todo.nextSetBit(currentDCId+1) )  {
            Integer currentDCIdInteger = new Integer(currentDCId);
            i++;
            elements[i] = currentDCIdInteger;
        }
        Arrays.sort(elements, new ComparatorDC( this ));
        Arrays.fill( order, Defs.NULL_INT ); // Reset all first
        for (i=0; i<todoSize; i++)  {
            order[ ((Integer)elements[i]).intValue() ] = i + offset;
        }
        return true;
    }
    
    /** Orders the contributions (nodes) by the atoms making them. It will also order the atoms
     *within each contribution left/right and even order within one side (left or right member).
     */
    static boolean sortContributions( ArrayList contributions, Gumbo gumbo ) {
        // sort left to right.
        //General.showDebug("Before sortContributions.");
        showContributions( contributions );
        for (int k=0;k<contributions.size();k++) {
            Object[] contrib = (Object[]) contributions.get(k);
            IntArrayList atomRid     = (IntArrayList) contrib[0];
            IntArrayList atomRidJ    = (IntArrayList) contrib[1];
            IntArrayList dcAtomRid   = (IntArrayList) contrib[2];
            IntArrayList dcAtomRidJ  = (IntArrayList) contrib[3];
            int[] sortMap = gumbo.atom.getOrderMap( atomRid );
            //General.showDebug("Sort map is: " + PrimitiveArray.toStringMakingCutoff( sortMap ));
            if ( ! PrimitiveArray.sortTogether( sortMap, new Object[] { atomRid, dcAtomRid } ) ) {
                General.showError("Failed to sort IntArrayLists together");
                return false;
            }
            sortMap = gumbo.atom.getOrderMap( atomRidJ );
            //General.showDebug("Sort map is: " + PrimitiveArray.toStringMakingCutoff( sortMap ));
            if ( ! PrimitiveArray.sortTogether( sortMap, new Object[] { atomRidJ, dcAtomRidJ } ) ) {
                General.showError("Failed to sort IntArrayLists together");
                return false;
            }
            if ( gumbo.atom.compare( atomRid, atomRidJ ) > 0 ) {
                //General.showDebug("Swapping left/right because atoms were out of order.");
                sortContributionLeftRight(contributions, k);
            } else {
                //General.showDebug("left/right were in order.");
            }
        }
        //General.showDebug("Before sort top/bottom:");
        showContributions( contributions );
        // sort top to bottom
        Collections.sort( contributions, new ComparatorDCContribution( gumbo ) );
        //General.showDebug("After sort top/bottom:");
        showContributions( contributions );
        return true;
    }
    
    /** Swap elements concurrently */
    private static boolean sortContributionLeftRight(ArrayList contributions, int i) {
        Object[] contrib = (Object[]) contributions.get(i);
        Object tmp = contrib[0];
        contrib[0] = contrib[1];
        contrib[1] = tmp;
        tmp = contrib[2];
        contrib[2] = contrib[3];
        contrib[3] = tmp;
        return true;
    }
    
    /** show them */
    static void showContributions( ArrayList contributions ) {
        for (int i=0;i<contributions.size();i++) {
//            Object[] contrib = (Object[]) contributions.get(i);
            //General.showDebug("*** Contribution " + i + " :" + PrimitiveArray.toStringMakingCutoff( contrib ));
        }
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
        tmpSet.set( rid );
        return toString(tmpSet, showViolations, showTheos);
    }
    
    
    /** The supported output formats for xplor include Aria's:
ASSI { 6} (( segid "SH3 " and resid 53 and name HA )) (( segid "SH3 " and resid 53 and name HE1 )) 3.600 1.700 1.700 peak 6 weight 0.10000E+01 volume 0.14383E-02 ppm1 4.578 ppm2 9.604 CV 1 
  OR { 6} (( segid "SLP " and resid 83 and name HB )) (( segid "SH3 " and resid 53 and name HE1 ))
     * @param format TODO
     */
    public boolean toXplorOrSo(BitSet dcSet, String fn, int fileCount, String atomNomenclature,
            boolean sortRestraints, String format ) {

        boolean isXplor = format.startsWith("XPLOR");
        boolean isDyana = format.startsWith("DYANA");
        if ( ! isXplor || isDyana ) {
            General.showCodeBug("Failed to determine format to Xplor or Dyana from string: " + format);
            return false;
        }
        int currentDCId;
        int dcCount = 0;                

        int dcCountTotal = dcSet.cardinality();
        if ( dcCountTotal == 0 ) {
            General.showWarning("No distance constraints selected in toXplor.");
            return true;
        }
        StringBuffer sb = new StringBuffer( dcCountTotal * 80 * 5); // rough unimportant estimation of 80 chars per restraint.
        
        //General.showDebug( "Total number of distance constraints todo: " + dcCountTotal );
        

        // Write them in a sorted fashion if needed.
        int[] map = null;
        if ( sortRestraints ) {
            if ( ! sortAll( dcSet, 0 )) {
                General.showError("Couldn'sort the dcs");
                return false;
            }
            map = mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID  ); // Includes just the dcs in this list
            if ( (map != null) && (map.length != dcCountTotal )) {
                General.showWarning("Trying to get an order map but failed to give back the correct number of elements: " + dcCountTotal + " instead found: " + map.length );
                map = null;
            }
            if ( map == null ) {
                General.showWarning("Failed to get the row order sorted out for distance constraints; using physical ordering."); // not fatal
                map = PrimitiveArray.toIntArray( dcSet );
                if ( map == null ) {
                    General.showError("Failed to get the used row map list so not writing this table.");
                    return false;
                }
            }
        } else {
            map = PrimitiveArray.toIntArray( dcSet );
            if ( map == null ) {
                General.showError("Failed to get the used row map list so not writing this table.");
                return false;
            }            
        }
        
        BitSet mapAsSet = PrimitiveArray.toBitSet(map,-1);
        if ( mapAsSet == null ) { 
            General.showCodeBug("Failed to create bitset back from map.");
            return false;
        }   
        BitSet mapXor = (BitSet) mapAsSet.clone();
        mapXor.xor(dcSet);
        if ( mapXor.cardinality() != 0 ) {
            General.showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
            General.showError("In the original set:" + PrimitiveArray.toString( dcSet ));
            General.showError("In the ordered  set:" + PrimitiveArray.toString( mapAsSet ));
            General.showError("Xor of both sets   :" + PrimitiveArray.toString( mapXor ));
            General.showError("The order column   :" + PrimitiveArray.toString( mainRelation.getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
            return false;
        }

        
        // important to get the indexes after the sortAll.
        IndexSortedInt indexMembAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMembAtom == null ||
             indexNodeMemb == null ||
             indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes to dc main in atom/memb/node");
            return false;
        }
        
        StringBuffer sbRst = new StringBuffer();
        Parameters p = new Parameters();
        
        // FOR EACH CONSTRAINT
        for (int d = 0; d<map.length;d++) { 
            sbRst.setLength(0);
            sbRst.append( "assi {" + Format.sprintf( "%4d", p.add( (d+1) )) + "} ");
            currentDCId = map[ d ];
            //General.showDebug("Preparing distance constraint: " + dcCount + " at rid: " + currentDCId);
            Integer currentDCIdInteger = new Integer(currentDCId);
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toString( dcNodes ));                
            if ( dcNodes == null ) {
                General.showError("Failed to get nodes");
                return false;
            }                    
            if ( dcNodes.size() < 1 ) {
                General.showError("Failed to get at least one node");
                return false;
            }
            if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcNodes, numbNode )) {
                General.showError("Failed to order nodes by order column");
                return false;
            }

            int dCNodeNumber = 1;                 
            // FOR EACH NODE
            boolean containsAriaOR = false;
            for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodes.size(); currentDCNodeBatchId++) {
                int currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId ); // quick enough?;-)
                //General.showDebug("Preparing distance constraint node: " + dCNodeNumber + " at rid: " + currentDCNodeId );

                // For each constraint node (those with distance etc but without logic) add the distance info to
                // the distance star loop.
                if ( ! Defs.isNull( logicalOp[currentDCNodeId] ) ) { 
                    dCNodeNumber++;
                    containsAriaOR = true;
                    continue;
                }
                
                if ( containsAriaOR && (dCNodeNumber>2)) {
                    sbRst.append('\n');
                    sbRst.append("    or      ");
                }

                // FOR EACH MEMBER (usually just 2)
                //General.showDebug("Looking for members in constraint node rid:" + currentDCNodeId );
                IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId), 
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
                //General.showDebug("Found the following rids of members in constraint node (" + currentDCNodeId + "): " + PrimitiveArray.toString( dcMembs ));
                if ( dcMembs.size() != 2 ) {
                    General.showError("Are we using a number different than 2 as the number of members in a node?");
                    return false;
                }                    
                if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcMembs, numbMemb )) {
                    General.showError("Failed to order members by order column");
                    return false;
                }
                for ( int currentDCMembBatchId=0;currentDCMembBatchId<dcMembs.size(); currentDCMembBatchId++) {
                    int currentDCMembID = dcMembs.getQuick( currentDCMembBatchId );
                    sbRst.append("(");
                    //General.showDebug("Working on member with RID: " + currentDCMembID);

                    // FOR EACH ATOM in member
                    int molNum      = Defs.NULL_INT;
//                    int prevMolNum  = Defs.NULL_INT;
                    //int entityNum   = Defs.NULL_INT;
                    String atomName     = null;                       

                    /** Next objects can be reused if code needs optimalization. */
                    IntArrayList dcAtoms = (IntArrayList) indexMembAtom.getRidList(  new Integer(currentDCMembID), 
                        Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    if ( dcAtoms.size() < 1 ) {
                        General.showError("No atoms in member");
                        return false;
                    }

                    if ( ! PrimitiveArray.orderIntArrayListByIntArray( dcAtoms, orderAtom )) {
                        General.showError("Failed to order atoms by order column");
                        return false;
                    }
                    // Get the real atom ids into a new list.
                    IntArrayList atomRids = new IntArrayList();
                    atomRids.ensureCapacity( dcAtoms.size() );
                    atomRids.setSize( dcAtoms.size() );
                    for (int i=0;i<dcAtoms.size();i++) {
                        atomRids.setQuick( i, atomIdAtom[ dcAtoms.getQuick(i) ]);
                    }                        
                    //General.showDebug("Found the following rids of atoms in constraint node (" + dCNodeNumber + "): " + PrimitiveArray.toString( atomRids ));
                    if ( atomRids.size() < 1 ) {
                        General.showError("Didn't find a single atom for a member in constraint node (" + dCNodeNumber + "): for constraint number: " + dcCount);
                        return false;
                    }
                    IntArrayList    statusList         = new IntArrayList();    // List of status (like ok, pseudo, deleted) for the original atoms
                    ObjectArrayList pseudoNameList     = new ObjectArrayList(); // List of pseudo atom names
                    statusList.ensureCapacity( dcAtoms.size() );
                    statusList.setSize( dcAtoms.size() );
                    pseudoNameList.ensureCapacity( dcAtoms.size() );
                    pseudoNameList.setSize( dcAtoms.size() );
                    if ( ! gumbo.atom.collapseToPseudo( atomRids, statusList, pseudoNameList, dbms.ui.wattosLib.pseudoLib )) {                    
                        General.showError("Failed to collapse atoms to pseudo atom names." );
                        return false;                    
                    }
                    for ( int currentDCAtomBatchId=0; currentDCAtomBatchId<dcAtoms.size(); currentDCAtomBatchId++) {
                        int statusAtom = statusList.getQuick( currentDCAtomBatchId );
                        if ( statusAtom == PseudoLib.DEFAULT_REDUNDANT_BY_PSEUDO ) {
                            continue; // skip atoms that are redundant with a pseudo (that -will- be written)
                        }
                        int currentDCAtomId = dcAtoms.getQuick( currentDCAtomBatchId );
                        if ( ! distConstrAtom.used.get( currentDCAtomId )) {
                            General.showCodeBug("Got an currentDCAtomId for an unused row: " + currentDCAtomId);
                            return false;
                        }

                        // Optimize further perhaps.
                        // Make sure that they're always sorted in memory or rewrite code here.
                        int nodeId  = dcNodeIdAtom[ currentDCAtomId ];
                        int membId  = dcMembIdAtom[ currentDCAtomId ];
                        int atomId  = atomIdAtom[currentDCAtomId];
                        boolean atomFound = true;
                        if ( Defs.isNull( atomId ) ) { // Was the atom actually matched in the structure?
                            atomFound = false;
                        }

                        if ( Defs.isNull( nodeId )) {
                            General.showCodeBug("Got a null value for nodeId in dc atom with rid: " + currentDCAtomId);
                            return false;
                        }
                        if ( Defs.isNull( membId )) {
                            General.showCodeBug("Got a null value for membId in dc atom with rid: " + currentDCAtomId);
                            return false;
                        }

                        int resNum      = Defs.NULL_INT;
                        String resName  = Defs.NULL_STRING_NULL;
                        if ( atomFound ) {
                            int resId   =  gumbo.atom.resId[ atomId];
                            int molId   =  gumbo.atom.molId[ atomId];
                            if ( Defs.isNull( resId ) ){
                                General.showError("Didn't find a resId from atom id: " + atomId + " for atom with name: " + atomName);
                                return false;
                            }
                            if ( Defs.isNull( molId ) ){
                                General.showError("Didn't find a resId from mol id: " + atomId + " for atom with name: " + atomName);
                                return false;
                            }

                            molNum  =  gumbo.mol.number[ molId ];
//                            if ( molNum != prevMolNum ) { 
//                                entityNum    = ((Integer)molNumber2EntityNumberMap.get( new Integer(molNum))).intValue(); // expensive so only do when changes are possible.
//                            }
                            if ( statusAtom == PseudoLib.DEFAULT_REPLACED_BY_PSEUDO ) {
                                atomName    = (String) pseudoNameList.getQuick( currentDCAtomBatchId ); // get the next one from the list.
                            } else {
                                atomName = gumbo.atom.nameList[atomId];                            
                                if ( statusAtom != PseudoLib.DEFAULT_OK ) {
                                    General.showError("Didn't expect this one in File31 for atom name: " + atomName);
                                    return false;
                                }
                            }
                            resNum  = gumbo.res.number[             resId ];
                            resName = gumbo.res.nameList[           resId ];
                        } else {
                            molNum          = molIdList[         currentDCAtomId ];
//                            entityNum       = entityIdList[      currentDCAtomId ];
                            resNum          = resIdList[         currentDCAtomId ];
                            resName         = resNameList[       currentDCAtomId ];
                            atomName        = atomNameList[      currentDCAtomId ];
                        }
                                                
                        sbRst.append(constr.toXplorAtomSel(molNum, resNum, resName, atomName,atomNomenclature));

//                        varDCConstraintsID[             currentStarDCAtomId ] = dcListCount;
//                        varDCDistconstrainttreeID[      currentStarDCAtomId ] = dcCount;
//                        varDCTreenodemembernodeID[      currentStarDCAtomId ] = nodeId[              nodeId  ];         
//                        varDCContributionfractionalval[ currentStarDCAtomId ] = contribution[        nodeId ];
//                        varDCConstrainttreenodememberID[currentStarDCAtomId ] = numbMemb[ membId ];
//                        varDCLabelentityassemblyID[     currentStarDCAtomId ] = molNum;
//                        varDCLabelentityID[             currentStarDCAtomId ] = entityNum;
//                        varDCLabelcompindexID[          currentStarDCAtomId ] = resNum;
//                        varDCLabelcompID[               currentStarDCAtomId ] = resName;
//                        varDCLabelatomID[               currentStarDCAtomId ] = atomName;
//                        varDCAuthsegmentcode[           currentStarDCAtomId ] = authMolNameList[     currentDCAtomId ];
//                        varDCAuthseqID[                 currentStarDCAtomId ] = authResIdList[       currentDCAtomId ];
//                        varDCAuthcompID[                currentStarDCAtomId ] = authResNameList[     currentDCAtomId ];
//                        varDCAuthatomID[                currentStarDCAtomId ] = authAtomNameList[    currentDCAtomId ];
//                        sbRst.append(") ");                    
                    } // end of loop per atom
                    sbRst.append(") ");                    
                } // end of loop per member

                boolean showDistanceList = false; // show only once per restraint.
                if ( containsAriaOR ) {
                    if ( dCNodeNumber == 2 ) {
                        showDistanceList = true;
                    }
                } else if ( dCNodeNumber == 1 ) {
                    showDistanceList = true;
                }
                if ( showDistanceList ) { 
                    float[] xplorDistSet = toXplorDistanceSet( 
                    		target[  currentDCNodeId],
                    		lowBound[currentDCNodeId],
                    		uppBound[currentDCNodeId]
                    		);
                    if ( xplorDistSet == null ) {
                    	General.showError("Failed to convert distances to xplor for restraint: " +
                    			toString(currentDCId));
                    	return false;
                    }
                    for (int i=0;i<3;i++ ) {
                    	sbRst.append(Format.sprintf( "%8.3f ", p.add( xplorDistSet[i] )));
                    }
                }         
                
                dCNodeNumber++;
            } // end of loop per node 
            sb.append(sbRst.toString()); // need to instantiate the string?
            sb.append('\n');
            dcCount++;
        }
//        dcCount--; // started with one
        fn = fn + "_dc";
        String outputFileName = InOut.addFileNumberBeforeExtension( fn, fileCount, true, 3 );        
        File f = new File(outputFileName+".tbl");
        InOut.writeTextToFile(f,sb.toString(),true,false);
        General.showOutput("Written " + Strings.sprintf(dcCount,"%5d") + " dcs to: " + f.toString());
        return true;
    }
     
    /** WARNING; this format will change in the future.
     *Optionaly shows theoretically possible distances if available.
     */
    public String toString(BitSet todo, boolean showViolations, boolean showTheos) {
        
        int todoCount = todo.cardinality();
        StringBuffer sb = new StringBuffer( todoCount * 80 * 5); // rough unimportant estimation of 80 chars per restraint.
        
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);
        
        if ( todoCount == 0 ) {
            return "No distance constraints selected in toString.";
        }
        
        float[] upp_theo = null;
        float[] low_theo = null;
        
        if ( mainRelation.containsColumn( DEFAULT_UPP_THEO) &&
                mainRelation.containsColumn( DEFAULT_LOW_THEO) ) {
            upp_theo = mainRelation.getColumnFloat(DEFAULT_UPP_THEO);
            low_theo = mainRelation.getColumnFloat(DEFAULT_LOW_THEO);
        }
        boolean containsTheos = true;
        
        if (
                upp_theo          == null ||
                low_theo          == null
                ) {
            containsTheos = false;
        }
        
        //sb.append( "Total number of distance constraints todo: " + todoCount + "\n");
        
        IndexSortedInt indexMembAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeMemb = (IndexSortedInt) distConstrMemb.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,                                    Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMembAtom == null ||
                indexNodeMemb == null ||
                indexMainNode == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        
//        int currentDCViolId         = Defs.NULL_INT;
        int currentDCNodeId         = Defs.NULL_INT;
        int currentDCId             = Defs.NULL_INT;
//        int currentDCListId         = Defs.NULL_INT;
        int currentDCEntryId        = Defs.NULL_INT;
        
        int dcCount = 0;
        // FOR EACH CONSTRAINT
        BitSet unlinkedAtomSelected = (BitSet) hasUnLinkedAtom.clone();
        unlinkedAtomSelected.and( selected );
        int unlinkedAtomCount = unlinkedAtomSelected.cardinality();
        if ( unlinkedAtomCount > 0 ) {
            General.showWarning("Skipping toString for " + unlinkedAtomCount + " constraints because not all their atoms are linked -1-." );
        }
        for (currentDCId = todo.nextSetBit(0);currentDCId>=0;currentDCId = todo.nextSetBit(currentDCId+1)) {
            Integer currentDCIdInteger = new Integer(currentDCId);
            if ( dcCount != 0 ) {
                sb.append( General.eol );
            }
            sb.append( "DC: " + currentDCId );
            if ( showViolations ) {
                
                if ( ! Defs.isNull( violLowMax[currentDCId]) ) {
                    sb.append( " violLowMax: " + df.format(violLowMax[currentDCId]) +
                            " at model: " + violLowMaxModelNum[currentDCId]);
                }
                if ( ! Defs.isNull( violUppMax[currentDCId]) ) {
                    sb.append( " violUppMax: " + df.format(violUppMax[currentDCId]) +
                            " at model: " + violUppMaxModelNum[currentDCId]);
                }
            }
            if ( showTheos ) {
                if ( containsTheos ) {
                    sb.append( " lowTheo/uppTheo: " +
                            Defs.toString( low_theo[ currentDCId ]) + "/" +
                            Defs.toString( upp_theo[ currentDCId ])
                            );
                } else {
                    sb.append( " no lowTheo/uppTheo: ");
                }
            }
            
            sb.append( General.eol );
            if ( hasUnLinkedAtom.get( currentDCId )) {
                General.showDetail("Skipping toString for constraint at rid: " + currentDCId + " because not all atoms are linked." );
                continue;
            }
//            currentDCListId  = dcListIdMain[    currentDCId ];
            currentDCEntryId = entryIdMain[     currentDCId ];
            if ( ! gumbo.entry.modelsSynced.get( currentDCEntryId ) ) {
                if ( ! gumbo.entry.syncModels( currentDCEntryId )) {
                    General.showError("Failed to sync models as required for toString.");
                }
                //General.showDebug("Sync-ed models for calculating distances for constraints");
            }
            // Not yet used:
//            int avgMethod      = constr.dcList.avgMethod[        currentDCListId ];
//            int numberMonomers = constr.dcList.numberMonomers[   currentDCListId ];
            /**
             * General.showDebug("Found constraint at rid: " + currentDCId +
             * " in list with rid: " + currentDCListId +
             * " with avgMethod: " + DistConstrList.DEFAULT_AVERAGING_METHOD_NAMES[avgMethod] +
             * " and number of monomers: " + numberMonomers);
             */
            
            
            boolean atomFound = true; // signals at least one atom could not be found when 'false'
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toStringMakingCutoff( dcNodes ));
            
            int dCNodeNumber = 1;
            // Define a list of n constraint nodes with 2 sets of atoms so elements are of type IntArrayList[2]
//            ArrayList atomsInvolved = new ArrayList();
            
            
            // FOR EACH NODE
            for ( int currentDCNodeBatchId=0;currentDCNodeBatchId<dcNodes.size(); currentDCNodeBatchId++) {
                currentDCNodeId = dcNodes.getQuick( currentDCNodeBatchId ); // quick enough?;-)
                //General.showDebug("Using distance constraint node: " + dCNodeNumber + " at rid: " + currentDCNodeId );
                
                // For each constraint node (those with distance etc but without logic) add the distance info to
                // the distance star loop.
                int logOp = logicalOp[currentDCNodeId];
                sb.append( "Node " + dCNodeNumber + " " );
                if ( ! Defs.isNull( logOp ) ) {
                    sb.append( " " + DEFAULT_LOGICAL_OPERATION_NAMES[logOp] + " ");
                }
                
                sb.append( "Low/target/Upp" );
                sb.append( ": " );
                sb.append( Defs.toString( lowBound[ currentDCNodeId ]) +
                        " " + Defs.toString( target[   currentDCNodeId ]) +
                        " " + Defs.toString( uppBound[ currentDCNodeId ]) + General.eol);
                
                if ( ! Defs.isNull( logOp ) ) {
                    if ( logOp != DEFAULT_LOGICAL_OPERATION_ID_OR ) {
                        General.showError("Can't deal with logical operations different than OR but found: [" + logOp + "]");
                        return null;
                    }
                    dCNodeNumber++;
                    continue;
                }
                // Assumption at this point is that the constraint nodes are OR-ed. Change code here to
                // allow different type of trees.
                //General.showDebug( "Dc node is a constraint node and not a logical node." );
                
                // FOR BOTH MEMBER (just 2)
                IntArrayList dcMembs = (IntArrayList) indexNodeMemb.getRidList(  new Integer(currentDCNodeId),
                        Index.LIST_TYPE_INT_ARRAY_LIST, null);
                //General.showDebug("Found the following rids of members in constraint node (" + currentDCNodeId + "): " + PrimitiveArray.toStringMakingCutoff( dcMembs ));
                if ( dcMembs.size() != 2 ) {
                    General.showError("Are we using a number different than 2 as the number of members in a node? Can't do toString for it yet.");
                    return null;
                }
                
                /** Next objects can be reused if code needs optimalization. */
                for (int m=0;m<dcMembs.size();m++ ) {
                    IntArrayList dcAtoms = (IntArrayList) indexMembAtom.getRidList(  new Integer(dcMembs.get(m)),
                            Index.LIST_TYPE_INT_ARRAY_LIST, null);
                    String prefix = "Member " + m + " ";
                    // Get the real atom ids into a new list.
                    for (int i=0;i<dcAtoms.size();i++) {
                        int rid = atomIdAtom[ dcAtoms.getQuick(i) ];
                        if ( Defs.isNull( rid )) {
                            General.showWarning("Failed to find atom; that should have been recorded before");
                            atomFound = false;
                            break;
                        }
                        sb.append( prefix + gumbo.atom.toString(rid) + General.eol );
                    }
                }
                if ( ! atomFound ) {
                    break;
                }
                dCNodeNumber++;
            } // end of loop for each node
            if ( ! atomFound ) {
                General.showWarning("Failed to find all atoms in constraint: " + dcCount + "That should have been recorded before.");
                continue; // continue with other constraints.
            }
            dcCount++;
        } // end of loop per constraint
        return sb.toString();
    }
    
    /** Returns an (empty) set of dc list rids for the given dc set.
     */
    public BitSet getDCListSetFromDCSet( BitSet todo ) {
        BitSet result = new BitSet();
        
        int todoCount = todo.cardinality();
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in getDCListSetFromDCSet.");
            return result;
        }
        
        BitSet dcMainListRids = SQLSelect.getDistinct(
                dbms,
                mainRelation,
                Constr.DEFAULT_ATTRIBUTE_SET_DC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],
                (BitSet)todo.clone());
        if ( dcMainListRids == null ) {
            General.showWarning("Failed to get a list of dc lists.");
            return result;
        }
        if ( dcMainListRids.cardinality() == 0 ) {
            General.showWarning("No distance constraint lists selected in getDCListSetFromDCSet.");
            return result;
        }
        for ( int rid=dcMainListRids.nextSetBit(0);rid>=0;rid=dcMainListRids.nextSetBit(rid+1)) {
            result.set(dcListIdMain[rid]);
        }
        /**
         * General.showDebug("Found dcMainListRids: " + PrimitiveArray.toString(dcMainListRids));
         * General.showDebug("Found result        : " + PrimitiveArray.toString(result));
         */
        return result;
    }
    
    /** Returns a list of saveframes for each dc list or the empty string if
     *there are no given restraints.
     *Showtheo is not implemented yet. showViolations is always done.
     */
    public String toSTAR(BitSet todo, float cutoffValue, boolean showViolations, boolean showTheo) {
        
        int todoCount = todo.cardinality();
        if ( todoCount == 0 ) {
            General.showOutput("No distance constraints selected in toSTAR.");
            return "";
        }
//        if ( ! calcViolation(todo, cutoffValue )) {
//            General.showError("Failed to calcViolation");
//            return null;
//        }
        
        BitSet dcListRids = getDCListSetFromDCSet(todo);
        if ( dcListRids == null ) {
            General.showError("Failed getDCListSetFromDCSet");
            return null;
        }
        
        if ( dcListRids.cardinality() == 0 ) {
            General.showCodeBug("Got empty set from getDCListSetFromDCSet.");
            return null;
        }
        
        // Create star nodes
        DataBlock db = new DataBlock();
        if ( db == null ) {
            General.showError( "Failed to init datablock.");
            return null;
        }
        int entryId = gumbo.entry.getEntryId();
        db.title = gumbo.entry.nameList[entryId];
        
        
        int dcListCount = 1;
        for ( int currentDCListId=dcListRids.nextSetBit(0);currentDCListId>=0;currentDCListId=dcListRids.nextSetBit(currentDCListId+1)) {
            General.showDebug("Doing constr.dcList.toSTAR for dcListCount: " + dcListCount);
            SaveFrame sF = constr.dcList.toSTAR(todo,dcListCount,
                    currentDCListId,showViolations,showTheo,cutoffValue);
//            General.showDebug("Done with constr.dcList.toSTAR for dcListCount: " + dcListCount);
            if ( sF == null ) {
                General.showError("Failed to generate saveframe for dc list: " + dcListCount);
                return null;
            }
            sF.setTitle("distance_constraint_statistics_"+dcListCount);
            if (dcListCount != 1) {
                TagTable tT = sF.getTagTable( constr.dcList.tagNameDCStats_Details,true);
                tT.setValue(0,constr.dcList.tagNameDCStats_Details,Defs.NULL_STRING_DOT);
            }
            db.add(sF);
            dcListCount++;
        }
        
        if ( ! db.toStarTextFormatting( dbms.ui.wattosLib.starDictionary )) {
            General.showWarning("Failed to format all columns as per dictionary this is not fatal however.");
        }
//        General.showDebug("Rendering dc results to STAR text.");
        String result = db.toSTAR();
//        General.showDebug("Back from dc results to STAR text.");
        if ( result == null ) {
            General.showError("Failed to render dc results to STAR text.");
            return null;
        }
        
        return result;
    }
    
    /** Returns the rid of the first node of this restraint the slow way. Returns -1 on error.
     */
    public int getdCNodeFirstWithoutLogicalOpRid( int currentDCId ) {
        //IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        BitSet dcNodesSet = SQLSelect.selectBitSet(dbms,
                distConstrNode,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCId),
                false);
        if ( dcNodesSet == null ) {
            General.showError("Failed to get nodes for getdCNodeFirstWithoutLogicalOpRid");
            return -1;
        }
        //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toStringMakingCutoff( dcNodes ));
        int lowestDCNodeNumber = Integer.MAX_VALUE;
        int lowestDCNodeRid = Integer.MAX_VALUE;
        for ( int rid=dcNodesSet.nextSetBit(0);rid>=0; rid=dcNodesSet.nextSetBit(rid+1)) {
            //General.showDebug("Looking at dc node rid: " + rid);
            if ( numbNode[rid] < lowestDCNodeNumber ) {
                if ( Defs.isNull( logicalOp[rid] ) ) {
                    lowestDCNodeNumber = numbNode[rid];
                    lowestDCNodeRid = rid;
                }
            }
        }
        if ( lowestDCNodeRid == Integer.MAX_VALUE ) {
            General.showError("Failed to get nodes in getdCNodeFirstWithoutLogicalOpRid, no nodes in set perhaps.");
            return -1;
        }
        return lowestDCNodeRid;
    }
    
    /** Returns the rids of the nodes of this restraint without the first.
     */
    public BitSet getdCNodeSetWithoutLogicalOpRid( int currentDCId ) {
        //IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        BitSet dcNodesSet = SQLSelect.selectBitSet(dbms,
                distConstrNode,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCId),
                false);
        if ( dcNodesSet == null ) {
            General.showError("Failed to get nodes for getdCNodeListWithoutLogicalOpRid");
            return null;
        }
        //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toStringMakingCutoff( dcNodes ));
        for ( int rid=dcNodesSet.nextSetBit(0);rid>=0; rid=dcNodesSet.nextSetBit(rid+1)) {
            //General.showDebug("Looking at dc node rid: " + rid);
            if ( ! Defs.isNull( logicalOp[rid] ) ) {
                dcNodesSet.clear(rid);
            }
        }
        return dcNodesSet;
    }
    
    
    
    /** Returns the rid of the first node of this restraint the slow way. Returns -1 on error.
     */
    public int[] getdCMembFirst2Rid( int currentDCNodeId ) {
        //IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
        BitSet set = SQLSelect.selectBitSet(dbms,
                distConstrMemb,
                Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID,
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCNodeId),
                false);
        if ( set == null ) {
            General.showError("Failed to getdCMembFirst2Rid.");
            return null;
        }
        
        ArrayList tempList = new ArrayList();
        // Fill temp array; kind of expensive.
        for (int i=set.nextSetBit(0); i>=0; i=set.nextSetBit(i+1))  {
            ObjectIntPair pair = new ObjectIntPair(new Integer(numbMemb[i]),i);
            tempList.add( pair );
        }
        // sort collection based on the old id
        Collections.sort( tempList, new ComparatorIntIntPair());
        
        ObjectIntPair p1 = (ObjectIntPair) tempList.get(0);
        ObjectIntPair p2 = (ObjectIntPair) tempList.get(1);
        int[] result = new int[] { p1.i, p2.i };
        return result;
    }
    
    /** Get the first atom in the member. sorted in the normal order.
     *Todo, use pseudo atom name in code above.
     */
    public int getAtomFirstRid(int dCMembRid) {
        BitSet dcAtomSet = SQLSelect.selectBitSet(dbms,
                distConstrAtom,
                Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(dCMembRid),
                false);
        if ( dcAtomSet == null ) {
            General.showError("Failed to get dc atoms for toSTAR");
            return -1;
        }
        //General.showDebug("Found the following rids of nodes in constraint: " + PrimitiveArray.toStringMakingCutoff( dcNodes ));
        int rid=dcAtomSet.nextSetBit(0);
        if ( rid < 0 ) {
            General.showError("Found no dc atom, no dc atoms in set perhaps.");
            return -1;
        }
        int atomRid = atomIdAtom[rid];
        int lowestAtomRid = atomRid;
        for ( ;rid>=0; rid=dcAtomSet.nextSetBit(rid+1)) {
            atomRid = atomIdAtom[rid];
            if ( gumbo.atom.compare( atomRid, lowestAtomRid, false, false ) < 0 ) {
                lowestAtomRid = atomRid;
            }
        }
        
        return lowestAtomRid;
    }
    
    /** Get the first atom (pseudo-)name in the member. sorted in the normal order.
     */
    public String getAtomFirstPseudoOrRegularName(int dCMembRid) {
//        General.showOutput("Looking at member: " + dCMembRid);
        
        BitSet dcAtomSet = SQLSelect.selectBitSet(dbms,
                distConstrAtom,
                Constr.DEFAULT_ATTRIBUTE_DC_MEMB_ID,
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(dCMembRid),
                false);        
        if ( dcAtomSet == null ) {
            General.showError("Failed to get dc atoms for toSTAR");
            return null;
        }
        IntArrayList dcAtoms = PrimitiveArray.toIntArrayList(dcAtomSet);
        if ( dcAtoms.size() < 1 ) {
            General.showError("No atoms in member");
            return null;
        }
        
        int lowestAtomRid = atomIdAtom[dcAtoms.get(0)]; // atom rid
        // Faster...
        if ( dcAtoms.size() == 1 ) {
            String atomName = gumbo.atom.nameList[lowestAtomRid];
//            General.showOutput("Returning atom: " + atomName);
            return atomName;
        }
        
        IntArrayList atomRidSet = new IntArrayList();
        atomRidSet.setSize( dcAtoms.size() );
        int lowestDCAtomRid = 0;
        for (int i=0;i<dcAtoms.size();i++) {
            int atomRid = atomIdAtom[dcAtoms.getQuick(i)];
            atomRidSet.setQuick( i, atomRid);
//            General.showOutput("Added atom: " + gumbo.atom.toString(atomRid));
            if ( gumbo.atom.compare( atomRid, lowestAtomRid, false, false ) < 0 ) {
                lowestAtomRid = atomRid;
                lowestDCAtomRid = i;   
            }
        }                        
//        General.showOutput("First atom: " + gumbo.atom.toString(lowestAtomRid));
        
        // find any 
        IntArrayList    statusList         = new IntArrayList();    // List of status (like ok, pseudo, deleted) for the original atoms
        ObjectArrayList pseudoNameList     = new ObjectArrayList(); // List of pseudo atom names also parrallel to dcAtoms.
        statusList.setSize( dcAtoms.size() );
        pseudoNameList.setSize( dcAtoms.size() );
        
        if ( ! gumbo.atom.collapseToPseudo( atomRidSet, statusList, pseudoNameList, dbms.ui.wattosLib.pseudoLib )) {                    
            General.showError("Failed to collapse atoms to pseudo atom names." );
            return null;                    
        }
//        General.showOutput("atoms:          " + gumbo.atom.toString(PrimitiveArray.toBitSet(atomRidSet)));
//        General.showOutput("statusList:     " + PrimitiveArray.toString(statusList));
//        General.showOutput("pseudoNameList: " + PrimitiveArray.toString(pseudoNameList));
        int statusAtom = statusList.getQuick( lowestDCAtomRid );
        String atomName = null;
        if ( statusAtom == PseudoLib.DEFAULT_OK ) {
            atomName = gumbo.atom.nameList[lowestAtomRid]; // there was a bug here by using index: lowestDCAtomRid                            
        } else {
            atomName = (String) pseudoNameList.getQuick( lowestDCAtomRid ); // get the next one from the list.
        }
        
//        General.showOutput("Returning atom: " + atomName);
        return atomName;
    }
    
    
    public float[] getModelViolationsFromViol(int currentDCId) {
        BitSet dcViolSet = SQLSelect.selectBitSet(dbms,
                distConstrViol,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCId),
                false);
        if ( dcViolSet == null ) {
            General.showError("Failed to getModelDistancesFromViol.");
            return null;
        }
        int[] dcViolList = PrimitiveArray.toIntArray(dcViolSet);
        /**
        if ( dcViolList.length != 2 ) {
            General.showError("Got dc viol list length not 2 but: " + dcViolList.length );
            General.showError("Rst: " + toString(currentDCId));
            General.showError("dcViolSet: " + PrimitiveArray.toString(dcViolSet));
            General.showError("dcViolList: " + PrimitiveArray.toString(dcViolList));
            return null;
        }
         */
        float[] result = new float[dcViolList.length];
        for (int i=0;i<dcViolList.length;i++) {
            result[i] = violation[ dcViolList[i] ];
        }
        return result;
    }
    
    public float[] getModelDistancesFromViol(int currentDCId) {
        BitSet dcViolSet = SQLSelect.selectBitSet(dbms,
                distConstrViol,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ],
                SQLSelect.OPERATION_TYPE_EQUALS,
                new Integer(currentDCId),
                false);
        if ( dcViolSet == null ) {
            General.showError("Failed to getModelDistancesFromViol.");
            return null;
        }
        int[] dcViolList = PrimitiveArray.toIntArray(dcViolSet);
        
        float[] result = new float[dcViolList.length];
        for (int i=0;i<dcViolList.length;i++) {
            result[i] = distance[ dcViolList[i] ];
        } 
        return result;
    }
    
    /** Add to the tagtable the info for the current DC */
    public boolean toSTAR( TagTable tT, int currentDCId, int currentDCListId,
            int dcCount, int listNumber ) {
        
        DistConstrList dcl = constr.dcList;
        if ( hasUnLinkedAtom.get( currentDCId )) {
            General.showWarning("Skipping toSTAR rendering of atom ids for constraint at rid: " + currentDCId + " because not all atoms are linked." );
        }
        
        int dCNodeFirstRid = getdCNodeFirstWithoutLogicalOpRid(currentDCId);
        if ( dCNodeFirstRid < 0 ) {
            General.showError("Failed to getdCNodeFirstRid for toSTAR");
            return false;
        }
        int currentDCNodeId = dCNodeFirstRid;
        
        int[] dCMembFirst2Rids = getdCMembFirst2Rid(currentDCNodeId);
        if ( dCMembFirst2Rids == null ) {
            General.showError("Failed to getdCMembFirst2Rid for toSTAR");
            return false;
        }
        
        int rowIdx = tT.getNewRowId();
        if ( rowIdx < 0 ) {
            General.showError("Failed tT.getNewRowId.");
            return false;
        }
        FloatIntPair fip = getMaxViolAndModelNumber(currentDCId);
        float maxViol = fip.f;        
        int max_violation_model_number = fip.i;
                
        float[] distanceList = getModelDistancesFromViol(currentDCId);
        if ( distanceList == null ) {
            General.showError("Failed to getModelDistancesFromViol for toSTAR");
            return false;
        }
        float[] avSd = Statistics.getAvSd(distanceList);
        float[] minMaxDist = Statistics.getMinMax(distanceList);
        
        float[] violList = getModelViolationsFromViol(currentDCId);
        if ( violList == null ) {
            General.showError("Failed to getModelViolationsFromViol for toSTAR");
            return false;
        }
        boolean smallerThanCutoff = false;
        String above_cutoff_violation_per_model =
                PrimitiveArray.toStringMakingCutoff(
                violList, dcl.cutoff[currentDCListId], smallerThanCutoff);
        if ( above_cutoff_violation_per_model == null ) {
            General.showError("Failed PrimitiveArray.toStringMakingCutoff in setTagTableRes");
            return false;
        }
        above_cutoff_violation_per_model = "[" + above_cutoff_violation_per_model + "]";
        
        int countMakingCutoff = PrimitiveArray.countMakingCutoff(above_cutoff_violation_per_model);
        
        float low = lowBound[ currentDCNodeId ];
        if ( low <= 2.0f ) {
            low = Defs.NULL_FLOAT;
        }
        if ( (maxViol < 0.0001) &&  (maxViol > -0.0001) ) { // really helps to focus the eye.
            maxViol = Defs.NULL_FLOAT;
        }
        tT.setValue(rowIdx, Relation.DEFAULT_ATTRIBUTE_ORDER_ID , dcCount);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Restraint_ID                    , dcCount+1);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Node_1_distance_val             , target[   currentDCNodeId ]);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Node_1_distance_lower_bound_val , low);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Node_1_distance_upper_bound_val , uppBound[ currentDCNodeId ]);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Distance_minimum                , minMaxDist[0]);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Distance_average                , avSd[0]);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Distance_maximum                , minMaxDist[1]);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Max_violation                   , maxViol);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Max_violation_model_number      , max_violation_model_number);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Above_cutoff_violation_count    , countMakingCutoff);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Above_cutoff_violation_per_model, above_cutoff_violation_per_model);
//        tT.setValue(rowIdx, dcl.tagNameDCStats_Entry_ID                        , 1);
        tT.setValue(rowIdx, dcl.tagNameDCStats_Distance_constraint_stats_ID    , listNumber);
        
        // FOR BOTH MEMBER        
        /** Next objects can be reused if code needs optimalization. */
        for (int m=0;m<2;m++ ) {
            int memberRid = dCMembFirst2Rids[m];
            int atomRid = getAtomFirstRid(memberRid);
            if ( atomRid <  0 ) {
                General.showError("Failed to getAtomFirstRid for toSTAR");
                return false;
            }
            String atomName = getAtomFirstPseudoOrRegularName(memberRid);
            
            int resRid = gumbo.atom.resId[atomRid];
            int molRid = gumbo.atom.molId[atomRid];
            int molNumber = gumbo.mol.number[molRid];
            String resName = gumbo.res.nameList[resRid];
            int resNumber  = gumbo.res.number[resRid];            
                    
            String atom_entity_ID       = dcl.tagNameDCStats_Atom_1_entity_ID;
            String atom_comp_index_ID   = dcl.tagNameDCStats_Atom_1_comp_index_ID;
            String atom_comp_ID         = dcl.tagNameDCStats_Atom_1_comp_ID;
            String atom_ID              = dcl.tagNameDCStats_Atom_1_ID;
            
            if ( m == 1 ) {
                atom_entity_ID       = dcl.tagNameDCStats_Atom_2_entity_ID;
                atom_comp_index_ID   = dcl.tagNameDCStats_Atom_2_comp_index_ID;
                atom_comp_ID         = dcl.tagNameDCStats_Atom_2_comp_ID;
                atom_ID              = dcl.tagNameDCStats_Atom_2_ID;
            }
            tT.setValue(rowIdx, atom_entity_ID                , molNumber);
            tT.setValue(rowIdx, atom_comp_index_ID            , resNumber);
            tT.setValue(rowIdx, atom_comp_ID                  , resName);
            tT.setValue(rowIdx, atom_ID                       , atomName);
        }
        return true;
    }
    
    
    /** A wrapper around the classify method that will also print the results
     */
    public boolean getClassification(BitSet todo ) {
        if ( ! classify( todo )) {
            General.showError("Failed to classify the constraints. Perhaps none were selected?");
            return false;
        }
        Parameters p = new Parameters(); // for printf
        int total = 0;
        
        /** Create the sets anew */
        String[] dcSetNames = DEFAULT_CLASS_NAMES;
        for (int c=0;c< dcSetNames.length;c++) {
            String setName = dcSetNames[ c ];
            BitSet set = mainRelation.getColumnBit( setName );
            if ( set == null ) {
                return false;
            }
            String tmpje = Format.sprintf( "%-15s", p.add( setName ));
            int cardi = set.cardinality();
            total += cardi;
            General.showOutput("Class " + c + ": " + tmpje + " contains number of elements: " + cardi);
        }
        General.showOutput("Total: " + total );
        return true;
    }
    
    /** Convenience method to combine low and upp viols to one overall and get
     *the responsible model number back.
     */
    FloatIntPair getMaxViolAndModelNumber( int rid ) {
        
        FloatIntPair fip = new FloatIntPair();
        fip.f = Defs.NULL_FLOAT;
        
        if ( ! Defs.isNull(violUppMax[rid])) {
            fip.f = violUppMax[rid];
            fip.i = violUppMaxModelNum[rid];
        }
        
        if ( ! Defs.isNull(violLowMax[rid])) {
            if ( Defs.isNull(violUppMax[rid])) {                
                fip.f = violLowMax[rid];
                fip.i = violLowMaxModelNum[rid];
            } else if ( violLowMax[rid] > fip.f ) {
                fip.f = violLowMax[rid];
                fip.i = violLowMaxModelNum[rid];                
            }
        }
        
        return fip;
    }
    
    /** Finds the restraint rids for restraints that involve one or more of the
     *atoms given. The method is optimized for a large number of atoms given. For
     *that case a simple scan works best.
     */
    public BitSet getWithAtoms( BitSet atomList ) {
        BitSet result = new BitSet();
        for (int currentDCAtomId = distConstrAtom.used.nextSetBit(0);currentDCAtomId>=0;currentDCAtomId=distConstrAtom.used.nextSetBit(currentDCAtomId+1)) {
            if ( atomList.get(atomIdAtom[currentDCAtomId])) {
                result.set(     dcIdAtom[currentDCAtomId]);
            }
        }
        return result;
    }
    
    
    /** Using the classification scheme below. Taken from IUPAC recommendations
     *and added the category undetermined for constraints in which the different contributions
     *don't fall in the same class or for which the atom failed to be matched to the topology.
     *<PRE>
     *intraresidue      residue spacing 0
     *sequential        residue spacing 1
     *medium-range      residue spacing 2-4
     *long-range        all other intra-molecular
     *intermolecular    between different molecules
     *undetermined      unmatched topology
     *mixed             conflicting classification among the different contributions
     *</PRE>
     *A bitset hashed under exactly that wording is added if it doesn't exist already.
     *
     *Returns true if no restraints are todo. Even the bitset will be created before returning.
     */
    public boolean classify(BitSet todo ) {
        
        /** Create the sets anew */
        String[] dcSetNames = DEFAULT_CLASS_NAMES;
        for (int c=0;c< dcSetNames.length;c++) {
            if ( mainRelation.containsColumn( dcSetNames[ c ] ) ) {
                if ( mainRelation.getColumnDataType( dcSetNames[ c ]  ) != Relation.DATA_TYPE_BIT ) {
                    General.showWarning("Existing column isn't of type BitSet from dc main relation with name: " + dcSetNames[ c ] );
                    Object tmpObject = mainRelation.removeColumn( dcSetNames[ c ] );
                    if ( tmpObject == null ) {
                        General.showError("Failed to remove existing column from dc main relation with name: " + dcSetNames[ c ] );
                        return false;
                    }
                }
            } else {
                if ( ! mainRelation.insertColumn( dcSetNames[ c ], Relation.DATA_TYPE_BIT, null )) {
                    General.showError("Failed to insert bitset column to dc main relation with name: " + dcSetNames[ c ] );
                    return false;
                }
            }
        }
        BitSet USet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_UNDETERMINED ] );
        BitSet ISet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_INTRA ] );
        BitSet SSet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_SEQ ] );
        BitSet MSet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_MEDIUM ] );
        BitSet LSet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_LONG ] );
        BitSet NSet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_INTER ] );
        BitSet XSet =  mainRelation.getColumnBit( DEFAULT_CLASS_NAMES[ DEFAULT_CLASS_MIXED ] );
        
        IndexSortedInt indexMainAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexNodeAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_DC_NODE_ID, Index.INDEX_TYPE_SORTED);
        IndexSortedInt indexMainNode = (IndexSortedInt) distConstrNode.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( (indexMainAtom == null) ||
                (indexNodeAtom == null) ||
                (indexMainNode == null) ) {
            General.showCodeBug("Failed to get all indexes.");
            return false;
        }
        
        int currentDCId             = Defs.NULL_INT;
        int currentDCNodeId         = Defs.NULL_INT;
//        int currentDCAtomId         = Defs.NULL_INT;
        
        BitSet todoNow = (BitSet) todo.clone();
        USet.or( todoNow );
        USet.and( hasUnLinkedAtom );
        todoNow.xor( USet );
        
        int todoCount = todo.cardinality();        
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in DistanceConstr.classify().");
            return true;
        }
        
        // FOR EACH CONSTRAINT
        for (currentDCId = todoNow.nextSetBit(0);currentDCId>=0;currentDCId = todoNow.nextSetBit(currentDCId+1)) {
            Integer currentDCIdInteger = new Integer(currentDCId);
            IntArrayList dcAtoms = (IntArrayList) indexMainAtom.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            int atomCount = dcAtoms.size();
            int[] normalAtoms = new int[ atomCount ];
            for (int i=0;i<atomCount;i++) {
                normalAtoms[i] = atomIdAtom[ dcAtoms.getQuick( i ) ];
            }
            int status = getClassificiation( normalAtoms );
            //General.showDebug("found class: " + status );
            IntArrayList dcNodes = (IntArrayList) indexMainNode.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            // FOR EACH NODE
            // check to see if the classification concurs.
            for (int nodeId=0;nodeId<dcNodes.size();nodeId++) { // loops usually 1-10 times
                currentDCNodeId = dcNodes.getQuick(nodeId);
                if ( ! Defs.isNull( logicalOp[ currentDCNodeId ])) {
                    //General.showDebug("Skipping logical node");
                    continue;
                }
                Integer currentDCNodeIdInteger = new Integer(currentDCNodeId);
                //General.showDebug("For node rid: " + currentDCNodeIdInteger);
                IntArrayList dcAtomsPerNode = (IntArrayList) indexNodeAtom.getRidList(  currentDCNodeIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
                if ( dcAtomsPerNode == null || dcAtomsPerNode.size() < 2 ) {
                    General.showError("Failed to find at least 2 atoms in node for dc: " + currentDCIdInteger);
                    return false;
                }
                int atomCountPerNode = dcAtomsPerNode.size();
                int[] normalAtomsPerNode = new int[ atomCountPerNode ];
                for (int i=0;i<atomCountPerNode;i++) {
                    normalAtomsPerNode[i] = atomIdAtom[ dcAtomsPerNode.getQuick( i ) ];
                }
                int statusPerNode = getClassificiation( normalAtomsPerNode );
                //General.showDebug("For node: " + nodeId + " found class: " + statusPerNode );
                if ( statusPerNode != status ) {
                    status = DEFAULT_CLASS_MIXED;
                    break;
                }
            }
            
            switch ( status ) {
                case DEFAULT_CLASS_UNDETERMINED: {
                    USet.set( currentDCId );
                    General.showDebug("Classified as undetermined constraint under rid: " + currentDCId);
                    General.showOutput(toString(currentDCId));
                    break;
                }
                case DEFAULT_CLASS_INTRA: {
                    ISet.set( currentDCId );
                    //General.showDebug("Classified as intra constraint under rid: " + currentDCId);
                    break;
                }
                case DEFAULT_CLASS_SEQ: {
                    SSet.set( currentDCId );
                    //General.showDebug("Classified as seq constraint under rid: " + currentDCId);
                    break;
                }
                case DEFAULT_CLASS_MEDIUM: {
                    MSet.set( currentDCId );
                    //General.showDebug("Classified as medium constraint under rid: " + currentDCId);
                    break;
                }
                case DEFAULT_CLASS_LONG: {
                    LSet.set( currentDCId );
                    //General.showDebug("Classified as long constraint under rid: " + currentDCId);
                    break;
                }
                case DEFAULT_CLASS_INTER: {
                    NSet.set( currentDCId );
                    //General.showDebug("Classified as inter constraint under rid: " + currentDCId);
                    break;
                }
                case DEFAULT_CLASS_MIXED: {
                    XSet.set( currentDCId );
                    //General.showDebug("Classified as mixed constraint under rid: " + currentDCId);
                    break;
                }
                default: {
                    General.showError("Failed to classify constraint under rid: " + currentDCId);
                    USet.set( currentDCId );
                    //General.showDebug("Classified as undetermined constraint under rid: " + currentDCId);
                }
            }
        }
        return true;
    }
    
    /** Returns the classification. Doesn't modify the argument because
     *it makes a private copy.
     */
    public int getClassificiation( int[] normalAtoms ) {
        int[] newA = (int[]) normalAtoms.clone();
        IntArrayList a = new IntArrayList( newA );
        boolean status = gumbo.atom.order(a);
        if ( ! status ) {
            return -1; // invalid id.
        }
        
        int firstAtomId = a.getQuick(0);
        int lastAtomId  = a.getQuick(normalAtoms.length-1);
        
        // last atom is undefined?
        if ( Defs.isNull( lastAtomId )) {
            return DEFAULT_CLASS_UNDETERMINED;
        }
        
        if ( gumbo.atom.molId[ firstAtomId ] != gumbo.atom.molId[ lastAtomId ] ) {
            return DEFAULT_CLASS_INTER;
        }
        int residueDiff = Math.abs( gumbo.atom.resId[ firstAtomId ] - gumbo.atom.resId[ lastAtomId ] );
        if ( residueDiff == 0 ) {
            return DEFAULT_CLASS_INTRA;
        }
        if ( residueDiff == 1 ) {
            return DEFAULT_CLASS_SEQ;
        }
        if ( residueDiff < 5 ) {
            return DEFAULT_CLASS_MEDIUM;
        }
        return DEFAULT_CLASS_LONG;
    }
    
    /** Hashes the distance constraints based on the sorted list of atom rids in the constraint.
     *This not an absolute garantee a unique hash but it works for now. Pretty hard to fool
     *with real NMR data.
     *One concurrency problem would be:
     *HA    | HB,HC
     *HA,HB | HC
     *
     *The value in the hashmap is an ArrayList of Integers to the dc rid.
     *Constraints with any unlinked atoms
     *will be skipped for now.
     *IMPORTANT NOTE: for duplicate constraints only 1 of them (the last one) will end up in the map. This
     *may or may not be a problem to your application. If it is, use remove surplus before.
     */
    public HashMap getHashMap(BitSet todo) {
        HashMap map = new HashMap();
        int todoCount = todo.cardinality();
        if ( todoCount == 0 ) {
            General.showWarning("No distance constraints selected in DistanceConstr.getHashMap().");
            return map;
        }
        
        IndexSortedInt indexMainAtom = (IndexSortedInt) distConstrAtom.getIndex(Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ], Index.INDEX_TYPE_SORTED);
        if ( indexMainAtom == null ) {
            General.showCodeBug("Failed to get all indexes.");
            return null;
        }
        
        int currentDCId             = Defs.NULL_INT;
//        int currentDCAtomId         = Defs.NULL_INT;
        
        BitSet todoNow = (BitSet) todo.clone();
        todoNow.andNot( hasUnLinkedAtom );
        
        // FOR EACH CONSTRAINT
        for (currentDCId = todoNow.nextSetBit(0);currentDCId>=0;currentDCId = todoNow.nextSetBit(currentDCId+1)) {
            Integer currentDCIdInteger = new Integer(currentDCId);
            
            IntArrayList dcAtoms = (IntArrayList) indexMainAtom.getRidList(  currentDCIdInteger, Index.LIST_TYPE_INT_ARRAY_LIST, null);
            int[] atoms = new int[dcAtoms.size()];
            for (int i=0;i<dcAtoms.size();i++) {
                atoms[i] = atomIdAtom[dcAtoms.getQuick(i)];
            }
            // BEGIN BLOCK MAKE SURE THE HASHING METHOD IS THE SAME AS ELSEWHERE FOR THIS OBJECT
            //mainRelation.hash[ currentDCId ] = PrimitiveArray.hashCode(elements); // precalculate the hash code.
            Arrays.sort( atoms );
            //General.showDebug("Using elements: " + PrimitiveArray.toStringMakingCutoff(atoms));
            int hash = PrimitiveArray.hashCode(atoms); // precalculate the hash code.
            //General.showDebug("Has hash code : " + hash);
            // in java 1.5 use the Arrays.hashCode method.
            // END BLOCK MAKE SURE THE HASHING METHOD IS THE SAME AS ELSEWHERE FOR THIS OBJECT
            
            Integer hashInt = new Integer(hash);
            ArrayList oa = null;
            if ( map.containsKey( hashInt ) ) {
                oa = (ArrayList) map.get(hashInt);
                int prevDCRid = ((Integer) oa.get(0)).intValue();
                BitSet dcAtomsPrevSet = SQLSelect.selectBitSet( dbms,
                        distConstrAtom,
                        Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME ],
                        SQLSelect.OPERATION_TYPE_EQUALS, new Integer(prevDCRid), false );
                
                int[] atomsPrev = new int[dcAtomsPrevSet.cardinality()];
                int b=0;
                for (int a=dcAtomsPrevSet.nextSetBit(0); a>=0; a=dcAtomsPrevSet.nextSetBit(a+1)) {
                    atomsPrev[b++] = atomIdAtom[ a ];
                }
                /**                    // expensive to execute normally.
                 * General.showDebug(
                 * "Found a duplicate constraint on the basis of the hash on the basis of the constituting atoms:" + General.eol +
                 * gumbo.atom.toString( PrimitiveArray.toBitSet(atoms,-1))            + General.eol +
                 * "The first dc is: "+ toString(prevDCRid,false,false)            + General.eol +
                 * "Atom rids duplicate are: " + PrimitiveArray.toString( atoms )     + General.eol +
                 * "Atom rids first  are: " + PrimitiveArray.toString( atomsPrev ) + General.eol +
                 * "hash duplicate : " + PrimitiveArray.hashCode( atoms )             + General.eol +
                 * "hash first     : " + PrimitiveArray.hashCode( atomsPrev )
                 * );
                 */
            } else {
                oa = new ArrayList();
                map.put( hashInt, oa );
            }
            oa.add( currentDCIdInteger );
        }
        return map;
    }
    
    
    public BitSet getAtomRidSet( int dcRid ) {
        BitSet atomDCAtomRidSet = SQLSelect.selectBitSet(dbms,
                distConstrAtom,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME],      // selection column
                SQLSelect.OPERATION_TYPE_EQUALS,                                            // selection operation
                new Integer( dcRid ), false);                                               // selection value/uniqueness.
        if ( atomDCAtomRidSet == null ) {
            General.showError("In getAtomRidSet for restraint: " + toString(dcRid));
            return null;
        }
        BitSet atomRidSet = new BitSet();
        // the next scan is of course slower than doing it on the list which in general is short.
        for (int rid=atomDCAtomRidSet.nextSetBit(0);rid>=0;rid=atomDCAtomRidSet.nextSetBit(rid+1)) {
            atomRidSet.set( atomIdAtom[rid]);
        }
        return atomRidSet;
    }
    
    
    public BitSet getAtomRidSet( BitSet dcRidSet ) {
        IndexSortedInt isi = (IndexSortedInt) distConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME],
                Index.INDEX_TYPE_SORTED);
        BitSet atomDCAtomRidSet = (BitSet) isi.getRidList(dcRidSet);
        if ( atomDCAtomRidSet == null ) {
            General.showError("In getAtomRidSet(2) for restraint: " + toString(dcRidSet));
            return null;
        }
        BitSet atomRidSet = new BitSet();
        // the next scan is of course slower than doing it on the list which in general is short.
        for (int rid=atomDCAtomRidSet.nextSetBit(0);rid>=0;rid=atomDCAtomRidSet.nextSetBit(rid+1)) {
            atomRidSet.set( atomIdAtom[rid]);
        }
        return atomRidSet;
    }
    
    
    /** Change the atom ids in dc_atom so that any stereo assignment from the
     *triplet is swapped.
     *Note that the caller needs to make sure that there is a stereospecific
     *presence of the triplet in this restraint.
     *E.g. For triplet VAL QG a restraint VAL MG1 <-> GLU H will be made
     *                                    VAL MG2 <-> GLU H
     *
     *The algorithm simply swaps every dc atomAtom rid in the restraint for which
     *the atom rid is represented in the triplet, with the dc atom rid of its
     *stereo sibling. Note that this invalidates any index on dc atomAtom
     *and it's destroyed here. When this is too slow a different strategy
     *should be followed.
     */
    public boolean swapStereoAssignment( int dcRid, Triplet triplet, PseudoLib pseudoLib) {
        
        int tripletAtomRid      = triplet.atomRids[0];
        int tripletResRid       = gumbo.atom.resId[tripletAtomRid];
        String tripletResName   = gumbo.res.nameList[ tripletResRid ];
        ArrayList tripletAtomNameList = (ArrayList) pseudoLib.toAtoms.get(tripletResName,triplet.name);
        
        BitSet atomDCAtomRidSet = SQLSelect.selectBitSet(gumbo.dbms,
                distConstrAtom,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME],      // selection column
                SQLSelect.OPERATION_TYPE_EQUALS,                                            // selection operation
                new Integer( dcRid ), false);                                               // selection value/uniqueness.
        if ( atomDCAtomRidSet == null ) {
            General.showError("In swapStereoAssignment for restraint: " + toString(dcRid));
            return false;
        }
        // the next scan is of course slower than doing it on the list which in general is short.
        for (int ridAtomDC=atomDCAtomRidSet.nextSetBit(0);ridAtomDC>=0;ridAtomDC=atomDCAtomRidSet.nextSetBit(ridAtomDC+1)) {
            int rid = atomIdAtom[ridAtomDC];
            int resRid = gumbo.atom.resId[ rid ];
            String resName = gumbo.res.nameList[ resRid ];
            if ( tripletResRid != resRid ) {
                continue;
            }
            String atomName = gumbo.atom.nameList[ rid ];
            if ( ! tripletAtomNameList.contains(atomName)) {
                continue;
            }
            String atomNameStereoSibling = pseudoLib.getStereoSibling(resName, atomName, triplet.name);
            if ( atomNameStereoSibling == null ) {
                General.showError("Failed to get atomNameStereoSibling in swapStereoAssignment for restraint: "
                        + toString(dcRid));
                return false;
            }
            int atomRidStereoSibling = gumbo.atom.getRidByAtomNameAndResRid(atomNameStereoSibling,resRid);
            if ( atomRidStereoSibling < 0 ) {
                General.showError("Failed to get atomRidStereoSibling in swapStereoAssignment for restraint: "
                        + toString(dcRid));
                return false;
            }
            atomIdAtom[ridAtomDC] = atomRidStereoSibling;
        }
        
        // Destroy index even though it needs to be used again at the beginning of this method.
        if ( distConstrAtom.containsIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[ RelationSet.RELATION_ID_COLUMN_NAME ] )){
            distConstrAtom.removeIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[ RelationSet.RELATION_ID_COLUMN_NAME ]);
        }
        return true;
    }
    /** Add atoms in dc_atom so that any stereo assignment from the
     *triplet is effectively deassigned. No correction is added because
     *sum averaging is assumed. In sum averaging the distance will always be
     *smaller after adding atoms.
     *
     *Note that the caller needs to make sure that there is a stereospecific
     *presence of the triplet in this restraint.
     *E.g. For triplet VAL QG a restraint VAL MG1 <-> GLU H will be made
     *                                    VAL QG  <-> GLU H
     *
     *The algorithm simply adds an atom in dc atomAtom rid in the restraint for
     *each atom rid that is represented in the triplet, with the dc atom rid of its
     *stereo sibling. Note that this invalidates any index on dc atomAtom
     *and it's destroyed here. When this is too slow a different strategy
     *should be followed.
     */
    public boolean deassignStereoAssignment( int dcRid, Triplet triplet, PseudoLib pseudoLib) {
        
        int tripletAtomRid      = triplet.atomRids[0];
        int tripletResRid       = gumbo.atom.resId[tripletAtomRid];
        String tripletResName   = gumbo.res.nameList[ tripletResRid ];
        ArrayList tripletAtomNameList = (ArrayList) pseudoLib.toAtoms.get(tripletResName,triplet.name);
        
        BitSet atomDCAtomRidSet = SQLSelect.selectBitSet(gumbo.dbms,
                distConstrAtom,
                Constr.DEFAULT_ATTRIBUTE_SET_DC[ RelationSet.RELATION_ID_COLUMN_NAME],      // selection column
                SQLSelect.OPERATION_TYPE_EQUALS,                                            // selection operation
                new Integer( dcRid ), false);                                               // selection value/uniqueness.
        if ( atomDCAtomRidSet == null ) {
            General.showError("In swapStereoAssignment for restraint: " + toString(dcRid));
            return false;
        }
        // the next scan is of course slower than doing it on the list which in general is short.
        for (int ridAtomDC=atomDCAtomRidSet.nextSetBit(0);ridAtomDC>=0;ridAtomDC=atomDCAtomRidSet.nextSetBit(ridAtomDC+1)) {
            int rid = atomIdAtom[ridAtomDC];
            int resRid = gumbo.atom.resId[ rid ];
            String resName = gumbo.res.nameList[ resRid ];
            if ( tripletResRid != resRid ) {
                continue;
            }
            String atomName = gumbo.atom.nameList[ rid ];
            if ( ! tripletAtomNameList.contains(atomName)) {
                continue;
            }
            String atomNameStereoSibling = pseudoLib.getStereoSibling(resName, atomName, triplet.name);
            if ( atomNameStereoSibling == null ) {
                General.showError("Failed to get atomNameStereoSibling in swapStereoAssignment for restraint: "
                        + toString(dcRid));
                return false;
            }
            int atomRidStereoSibling = gumbo.atom.getRidByAtomNameAndResRid(atomNameStereoSibling,resRid);
            if ( atomRidStereoSibling < 0 ) {
                General.showError("Failed to get atomRidStereoSibling in swapStereoAssignment for restraint: "
                        + toString(dcRid));
                return false;
            }
            // Copy the row to the sibling the slow way.
            int distConstrAtomSizeMax = distConstrAtom.sizeMax; // cache for check on growth
            int newRowIdx = distConstrAtom.getNewRowId();
            if ( newRowIdx < 0 ) {
                General.showError("Failed to distConstrAtom.getNewRowId in swapStereoAssignment for restraint: "
                        + toString(dcRid));
                return false;
            }
            if ( distConstrAtomSizeMax != distConstrAtom.sizeMax ) {
                resetConvenienceVariables();
            }
            distConstrAtom.copyRow(ridAtomDC,newRowIdx);
            atomIdAtom[newRowIdx] = atomRidStereoSibling; // only modification needed.
        }
        
        // Destroy index even though it needs to be used again at the beginning of this method.
        if ( distConstrAtom.containsIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[ RelationSet.RELATION_ID_COLUMN_NAME ] )){
            distConstrAtom.removeIndex( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[ RelationSet.RELATION_ID_COLUMN_NAME ]);
        }
        return true;
    }
    
    /** Shows a plot for the total and maximum violations per residue */
    public boolean showPlotPerResidue(URL url, boolean saveImage, String file_name_base_dc) {
        DistConstrList dcl = constr.dcList;
        if ( ! dcl.initConvenienceVariablesStar()) {
            General.showError("Failed initConvenienceVariablesStar");
            return false;
        }
        
        StarFileReader sfr = new StarFileReader(dbms); 
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("Parse not successful");
            return false;
        }
        General.showDebug("Parse successful");
        // data type modifications on column
        ArrayList tTList = (ArrayList) sn.getTagTableList(StarGeneral.WILDCARD, 
                StarGeneral.WILDCARD, StarGeneral.WILDCARD, dcl.tagNameDCPer_resTotal_violation);
        if ( tTList == null ) {
            General.showError("Expected a match but none found");
            return false;
        }
        if ( tTList.size() != 1 ) {
            General.showError("Expected exactly 1 match but found: " + tTList.size() );
            return false;
        }
        TagTable tT = (TagTable) tTList.get(0);
        if ( 
            (!tT.convertDataTypeColumn(dcl.tagNameDCPer_resTotal_violation,       TagTable.DATA_TYPE_FLOAT, null ))||
            (!tT.convertDataTypeColumn(dcl.tagNameDCPer_resMax_violation,         TagTable.DATA_TYPE_FLOAT,   null ))||
            (!tT.convertDataTypeColumn(dcl.tagNameDCPer_resAtom_entity_ID,        TagTable.DATA_TYPE_INT,   null ))||
            (!tT.convertDataTypeColumn(dcl.tagNameDCPer_resAtom_comp_index_ID,    TagTable.DATA_TYPE_INT,   null ))) {
            General.showError("Failed converting (some of) the data to the types expected.");
            return false;
        }
        
        // Create a nice plot
        try {        
            StringArrayList columnNameListValue = new StringArrayList();
            columnNameListValue.add(dcl.tagNameDCPer_resTotal_violation);
            columnNameListValue.add(dcl.tagNameDCPer_resMax_violation);
            StringArrayList seriesNameList = new StringArrayList();
            String leftPropLabelName = "Total violation (A)";
            String rightPropLabelName = "Maximum violation (A)";
            seriesNameList.add(leftPropLabelName);
            seriesNameList.add(rightPropLabelName);
            DefaultTableXYDataset dataSet = ResiduePlot.createDatasetFromRelation(
                    tT, 
                    columnNameListValue,
                    seriesNameList);

            JFreeChart chart = ResiduePlot.createChart(dataSet,
                    tT, 
                    dcl.tagNameDCPer_resAtom_entity_ID,
                    dcl.tagNameDCPer_resAtom_comp_index_ID,
                    dcl.tagNameDCPer_resAtom_comp_ID);
//            XYPlot plot = (XYPlot) chart.getPlot();
//            int seriesCount = dataSet.getSeriesCount();
//            for (int i=0;i<seriesCount;i++) {
//                XYSeries series = dataSet.getSeries(i);
//                String key = (String) series.getKey();
//                General.showDebug("Changing series: " + key);
//                NumberAxis axis = (NumberAxis) plot.getRangeAxisForDataset(i);
//                if ( key.equals(complLabelName)) {
//                    General.showDebug("Changing axis");
//                    axis.setRange(0,100);
//                    axis.setTickUnit(new NumberTickUnit(20));
//                } else if ( key.equals(restrLabelName)) {
//                    General.showDebug("Changing axis");
//                    axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());                    
//                }
//            }
            if ( dbms.ui.hasGuiAttached() ) {
                ChartPanel chartPanel = new ChartPanel(chart);                
                dbms.ui.gui.setJPanelToBeFilled( 1, chartPanel ); // executed on event-dispatching thread                
            } else {
                General.showOutput("Skipping the actual show because there is not Gui attached.");
            }
//            ResiduePlot.show(chart); 

            if ( saveImage ) {
                String fileName = file_name_base_dc+".jpg";
                int width = 1024;
                int height = 768;
                General.showOutput("Saving chart as JPEG");
                ChartUtilities.saveChartAsJPEG(new File(fileName), chart, width, height);            
                General.showOutput("Saving chart as PDF");
                fileName = file_name_base_dc+".pdf";
                PdfGeneration.convertToPdf(chart, width, height, fileName);                        
            }
        } catch (Throwable t) {
            General.showThrowable(t);
            return false;
        }
        return true;
    }

    /** Returns the strictes distance of the two for the lower bound. In example
     *the highest value. Ignoring any null values.
     */
    public static float getStrictLowBound(float f, float f0) {
        if ( Defs.isNull(f) ) {
            if ( Defs.isNull(f0) ) {
                return Defs.NULL_FLOAT;
            } else {
                return f0;
            }
        } else {
            if ( Defs.isNull(f0) ) {
                return f;
            } else {
                return Math.max( f, f0 );
            }            
        }
    }

    /** Returns the strictes distance of the two for the upper bound. In example
     *the lowest value. Ignoring any null values.
     */
    public static float getStrictUppBound(float f, float f0) {
        if ( Defs.isNull(f) ) {
            if ( Defs.isNull(f0) ) {
                return Defs.NULL_FLOAT;
            } else {
                return f0;
            }
        } else {
            if ( Defs.isNull(f0) ) {
                return f;
            } else {
                return Math.min( f, f0 );
            }            
        }
    }    
    
    /** Returns the distances target, lowerbound dev, upperbound dev given
     * target, low, upp. Returns null when given input can't be translated
     * e.g. all were nulls. In summary, 3 binary states in combination gives
     * 8 possibilities detailed below. NB that a value of 0.0 for the target
     * as in entry 1ai0 will be assumed a null.
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
     * t&l      [l,t]   t   t-l 0
     * t&u      [t,u]   u   u-t 0
     * l&u      [l,u]   u   u-l 0

     * t&l&u    [l,t,u] t   t-l u-t
     * no data  error
     * </PRE>
     * Fast but verbose routine.
     */

    public static float[] toXplorDistanceSet(float tar, float low, float upp) {
    	float tarNew=Defs.NULL_FLOAT;
        float lowDev=Defs.NULL_FLOAT;
        float uppDev=Defs.NULL_FLOAT;
        int state = -1;
        if ( tar == 0.0 ) { // For entry 1ai0
            tar = Defs.NULL_FLOAT;
        }
        // sanity checks first
//        General.showDebug("tar is: " + tar);
//        General.showDebug("low is: " + low);
//        General.showDebug("upp is: " + upp);
        if ( !Defs.isNull(low) && !Defs.isNull(tar) && low > tar ) {
            General.showDebug("Found lowerbound (" + low + ") larger than target (" + tar + ")");
//            return null;
        }
        if ( !Defs.isNull(low) && !Defs.isNull(upp) && low > upp ) {
            General.showWarning("Found lowerbound (" + low + ") larger than upperbound (" + upp + ")");
//            return null;
        }
        if ( !Defs.isNull(tar) && !Defs.isNull(upp) && tar > upp ) {
            General.showWarning("Found target (" + tar + ") larger than upperbound (" + upp + ")");
//            return null;
        }
        // 3 combinations where 1 is known
        if ( !Defs.isNull(tar) && Defs.isNull(low) && Defs.isNull(upp)) {
            state = 1;
        	tarNew = 0f;
            lowDev = 0f;
            uppDev = tar;        	
        } 
        if ( Defs.isNull(tar) && !Defs.isNull(low) && Defs.isNull(upp)) {
            state = 2;
            tarNew = low;
            lowDev = 0f;
            uppDev = 999.999f; // needs to match the %8.3f formatting used for printing above            
        } 
        if ( Defs.isNull(tar) && Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 3;
            tarNew = 0;
            lowDev = 0;
            uppDev = upp;       
        } 
        // 3 combinations where 2 are known 
        if ( !Defs.isNull(tar) && !Defs.isNull(low) && Defs.isNull(upp)) {
            state = 4;
            tarNew = tar;
            lowDev = tar-low;
            uppDev = 0f;            
        } 
        if ( !Defs.isNull(tar) &&  Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 5;
            tarNew = upp;
            lowDev = upp-tar;
            uppDev = 0f;               
        } 
        
        if (  Defs.isNull(tar) && !Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 6;
            tarNew = upp;
            lowDev = upp-low;
            uppDev = 0f;   
        } 
        // the other 2 combinations; none known or all three known.
        if ( Defs.isNull(tar) && Defs.isNull(low) && Defs.isNull(upp)) {
            General.showError("Found all distances to be nonexisting");
            return null;
        } 
        if ( !Defs.isNull(tar) && !Defs.isNull(low) && !Defs.isNull(upp)) {
            state = 7;
            tarNew = tar;
            lowDev = tar-low;
            uppDev = upp-tar;            
        }
        
        if ( Defs.isNull(tarNew) || Defs.isNull(lowDev) || Defs.isNull(uppDev) ){
            General.showCodeBug("Found at least one of the distances to be nonexisting");
            General.showCodeBug("In state: "+state);
        }
        // self checks
        if ( Defs.isNull(tarNew) ){
            General.showCodeBug("Found tarNew to be nonexisting");
            return null;
        }
        if ( Defs.isNull(lowDev) ){
            General.showCodeBug("Found lowDev to be nonexisting");
            return null;
        }
        if ( Defs.isNull(uppDev) ){
            General.showCodeBug("Found uppDev to be nonexisting");
            return null;
        }
        return new float[] {tarNew, lowDev, uppDev};
    }
    
}
