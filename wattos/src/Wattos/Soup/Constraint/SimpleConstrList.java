/*
 */

package Wattos.Soup.Constraint;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Soup.Gumbo;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.StringSet;

/**A list of simple restraints. 
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class SimpleConstrList extends ConstrItem implements Serializable {
    
    private static final long serialVersionUID = -1207795172754062330L;
    
    
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
    public float[]     cutoff;
    
    /**
     * public int[]       constrLowCount;
     * public int[]       violLowCount;
     * public float[]     violLowTotal;
     * public float[]     violLowMax;
     * public float[]     violLowRms;
     * public float[]     violLowAll;
     * public float[]     violLowAvViol;
     */
        
    public StarDictionary starDict;
    
    /** E.g. Constr.DEFAULT_ATTRIBUTE_SET_CDIH_LIST */
    public String[] ATTRIBUTE_SET_SUB_CLASS_LIST = null;
    /** E.g. Constr.DEFAULT_ATTRIBUTE_SET_CDIH */
    public String[] ATTRIBUTE_SET_SUB_CLASS = null;
    
        
    public SimpleConstrList(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);        
        //General.showDebug("back in Atom constructor");
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }
    
    /** The relationSetName is a parameter so non-standard relation sets
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public SimpleConstrList(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Atom constructor");
        name = relationSetName;
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
//        General.showDebug("now in SimpleConstrList.init()");
        super.init(dbms); 
//        General.showDebug("back in SimpleConstrList.init()");
                
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
    
    
    /**
     * @param format TODO */
    public boolean toXplorOrSo(int entryRID, String fn, String atomNomenclature, boolean sortRestraints, String format) {
        int fileCount = 0;
        BitSet todo = SQLSelect.selectBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RelationSet.RELATION_ID_COLUMN_NAME], 
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer( entryRID ), false);
        SimpleConstr sc = constr.getSimpleConstrByAttributeSet(ATTRIBUTE_SET_SUB_CLASS); 
        for (int listRID = todo.nextSetBit(0); listRID >= 0; listRID=todo.nextSetBit(listRID+1)) {
            fileCount++;
            boolean status = sc.toXplorOrSo( listRID, fn, fileCount, atomNomenclature, sortRestraints, format );
            if ( ! status ) {
                General.showError("Failed sc.toXplor");
                General.showError("Not writing any more scLists");
                return false;
            }
        }
        return true;
    }
}
