/*
 * RelationList.java
 *
 * Created on April 10, 2003, 6:43 PM
 */

package Wattos.Database;

import java.util.*;
import java.io.*;
import Wattos.Utils.*;

/**
 *A set of relations is used if multiple relations are used for modelling a single object.
 *Not much functionality yet here. It might be convenient to store related info together
 *in the source.
 *There is always a main relation for this set. All the parents of the relations have
 *to exist prior to creation of this relation set. E.g. residues need to exist before
 *atoms etc...
 * @author Jurgen F. Doreleijers
 */
public class RelationSet implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;    

    /** Link a couple of definitions up from the Relation class 
    * The set can't be extending the Relation because it would end up in the
     *dbms as a relation that way.
     */
    public static final int DATA_TYPE_INVALID        = Relation.DATA_TYPE_INVALID;          
    public static final int DATA_TYPE_BIT            = Relation.DATA_TYPE_BIT;         
    public static final int DATA_TYPE_CHAR           = Relation.DATA_TYPE_CHAR;        
    public static final int DATA_TYPE_BYTE           = Relation.DATA_TYPE_BYTE;                
    public static final int DATA_TYPE_SHORT          = Relation.DATA_TYPE_SHORT;              
    public static final int DATA_TYPE_INT            = Relation.DATA_TYPE_INT;              
    public static final int DATA_TYPE_FLOAT          = Relation.DATA_TYPE_FLOAT;               
    public static final int DATA_TYPE_DOUBLE         = Relation.DATA_TYPE_DOUBLE;               
    public static final int DATA_TYPE_LINKEDLIST     = Relation.DATA_TYPE_LINKEDLIST;       
    public static final int DATA_TYPE_LINKEDLISTINFO = Relation.DATA_TYPE_LINKEDLISTINFO;
    public static final int DATA_TYPE_STRING         = Relation.DATA_TYPE_STRING;          
    public static final int DATA_TYPE_STRINGNR       = Relation.DATA_TYPE_STRINGNR; 
    public static final int DATA_TYPE_ARRAY_OF_INT   = Relation.DATA_TYPE_ARRAY_OF_INT; 
    public static final int DATA_TYPE_ARRAY_OF_FLOAT = Relation.DATA_TYPE_ARRAY_OF_FLOAT; 
    public static final int DATA_TYPE_OBJECT         = Relation.DATA_TYPE_OBJECT; 
        
    public static final String DEFAULT_ATTRIBUTE_ID                = Relation.DEFAULT_ATTRIBUTE_ID;
    public static final String DEFAULT_ATTRIBUTE_PARENT            = Relation.DEFAULT_ATTRIBUTE_PARENT;
    public static final String DEFAULT_ATTRIBUTE_CHILD_LIST        = Relation.DEFAULT_ATTRIBUTE_CHILD_LIST;
    public static final String DEFAULT_ATTRIBUTE_SIBLING_LIST      = Relation.DEFAULT_ATTRIBUTE_SIBLING_LIST;
    public static final String DEFAULT_ATTRIBUTE_SELECTED          = Relation.DEFAULT_ATTRIBUTE_SELECTED;
    public static final String DEFAULT_ATTRIBUTE_NAME              = Relation.DEFAULT_ATTRIBUTE_NAME;
    public static final String DEFAULT_ATTRIBUTE_TYPE              = Relation.DEFAULT_ATTRIBUTE_TYPE;    
    
    public static final String DEFAULT_ATTRIBUTE_ORDER_ID          = Relation.DEFAULT_ATTRIBUTE_ORDER_ID;
    public static final String DEFAULT_ATTRIBUTE_NUMBER            = Relation.DEFAULT_ATTRIBUTE_NUMBER;
    public static final String DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN   = Relation.DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN;
    public static final String DEFAULT_ATTRIBUTE_VALUE_1           = Relation.DEFAULT_ATTRIBUTE_VALUE_1;   
    public static final String DEFAULT_ATTRIBUTE_VALUE_1_LIST      = Relation.DEFAULT_ATTRIBUTE_VALUE_1_LIST;
    public static final String DEFAULT_ATTRIBUTE_VALUE_2           = Relation.DEFAULT_ATTRIBUTE_VALUE_2;   
    
    public static final int    DEFAULT_ATTRIBUTE_PARENT_VALUE      = Relation.DEFAULT_ATTRIBUTE_PARENT_VALUE;    
    public static final int    NOT_AN_INDEX                        = Relation.NOT_AN_INDEX;

    /** Standard position in the String[]      */
    public static int RELATION_ID_SET_NAME              = 0;
    public static int RELATION_ID_MAIN_RELATION_NAME    = 1;
    public static int RELATION_ID_COLUMN_NAME           = 2;    
        
    public static String DEFAULT_RELATIONSET_NAME           = "relationset_default_name";
    public static String DEFAULT_ATTRIBUTE_MAIN_POSTFIX     = "_main";
    public static String DEFAULT_ATTRIBUTE_MAIN_ID          = "main_id";  // Refers back to mainRelation in RelationSet's related relation columns.
    
    /** Defines the standard attribute names and data types */
    public HashMap     DEFAULT_ATTRIBUTES_TYPES = new HashMap();
    /** Defines the standard attribute names and data types */
    public ArrayList   DEFAULT_ATTRIBUTES_ORDER = new ArrayList();    
    /** Defines the standard values for some attributes */     
    public HashMap     DEFAULT_ATTRIBUTE_VALUES = new HashMap();
    /** Defines the standard fkcs for some attributes */    
    public ForeignKeyConstrSet DEFAULT_ATTRIBUTE_FKCS   = null;
    /** Store standard definitions for fkcs just specifying the from
     *relation column name and the to relation name as a String[2] object
     *inside the ArrayList.
     */
    public ArrayList DEFAULT_ATTRIBUTE_FKCS_FROM_TO = new ArrayList();
    
    /** General name of the collection of relations */
    public String name = DEFAULT_RELATIONSET_NAME;
    
    /** Set of relations */
    public HashMap relationSet;

    /** The most important/main relation */
    public Relation mainRelation;

    /** Keep a local reference of the dbms so it doesn't need to be passed
     *for each method. */
    public DBMS dbms;

    /** Parent if this relation is in a Relation set
     *otherwise null.     */
    public RelationSoS relationSoSParent;
    
    /** NB this method calls the specialized class init.
     */
    public RelationSet(DBMS dbms, RelationSoS relationSoSParent) {
        this.relationSoSParent = relationSoSParent;
        //General.showDebug("now in RelationSet constructor");
        init(dbms); // Goes to any extending class NOT to the init() here.
        //General.showDebug("back in RelationSet constructor");
    }
    
    public boolean init(DBMS dbms) {
        this.dbms = dbms;
        //General.showDebug("now in RelationSet.init()");
        // Remove any previous existing relations.        
        removeAllRelations();
        mainRelation = null;
        return true;
    }

    /**  Removes all indices on all relations in this class.   */
    public boolean removeAllIndices() {        
        if ( relationSet != null ) {
            Set keys = relationSet.keySet();
            for (Iterator key=keys.iterator();key.hasNext();) {
                String relationName = (String) key.next();
                if ( dbms.containsRelation(relationName) ) {
                    Relation r = dbms.getRelation(relationName);
                    if ( ! r.removeIndices()) {
                        General.showError("Failed to remove all indices from relation: " + r.name);
                        return false;
                    }
                } else {
                    General.showError("Failed to find relation in dbms with name: " + relationName);
                    return false;                    
                }
            }
        }
        return true;
    }
    
    /** Remove any previous existing relations.        
     */
    public boolean removeAllRelations() {
        if ( relationSet != null ) {
            Set keys = relationSet.keySet();
            for (Iterator key=keys.iterator();key.hasNext();) {
                String relationName = (String) key.next();
                if ( dbms.containsRelation(relationName) ) {
                    dbms.removeRelation(dbms.getRelation(relationName));
                }
            }
        }
        relationSet = new HashMap(); // simply nil them.
        return true;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Iterator i=relationSet.keySet().iterator();i.hasNext();) {
            Object key = i.next();
            Relation relation = (Relation) relationSet.get(key);
            if ( relation == mainRelation ) {
                sb.append("Main relation\n");
            }
            sb.append( relation.toString() );            
        }
        return sb.toString();
    }
    
    public String toString(BitSet todo) {
        StringBuffer sb = new StringBuffer();
        for (int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            sb.append( toString( rid ) + General.eol );            
        }
        return sb.toString();
    }

    /** TODO: Add other relations later.
     */
    public String toSTAR(BitSet todo) {
        return mainRelation.toSTAR(todo);
    }
    

    public String toString(int rid) {
        General.showError("Method toString in RelationSet should be overwritten");
        return null;
    }
    
    
    /** Returns a reference to the Relation with the given name
     */
    public Relation getRelation( Object key) {
        //General.showDebug("getting relation for key: [" + key + "]");
        Object value = relationSet.get( key );
        if ( value == null ) {
            return null;
        }
        if ( ! (value instanceof Relation) ) {
            General.showError("found a non relation instance in the relation set");
            return null;
        }        
        return (Relation) value;
    }
    
    /** Adds a relation to this set. Note that relation is already added to the dbms
     *which is the preferred way to get to the data. The second preferred way to 
     *get to the data is by direct pointer maintained in subclasses. E.g. in the
     *RelationSet Atom there is a variable atomMain that is a pointer to the 
     *relation atomMain. This just does a put into the hashmap for this class.
     *Returns null if a relation already exists with this name.
     */
    public boolean addRelation ( Relation relation ) {
        if ( relationSet.containsKey( relation.name ) ) {
            General.showError("relationSet: " + name + " already containsKey: " + relation.name );
            return false;
        }
        relationSet.put( relation.name, relation );
        //General.showDebug("Done adding relation to set with name           :     " + relation.name);        
        return true;
    }

    /** Returns the name of the row i */
    public String getName( int i ) {
        return mainRelation.getName( i );
    }
    
    /** Parses a star file and extracts one saveframe to a list of relationSet. Each loop
     *becomes a relation and an object in a list of "datanodes". Each tag will become
     *at least one object therefore; no speed problem there. It will be up to the calling
     *method to cast any known columns to a datatype different than String.
     
    public int readStarFile( String fileName, String saveFrameCategoryName, int verbosityLevel ) {        
        FileInputStream inStream =  new FileInputStream(fileName);
        StarParser myParser = new StarParser( inStream );
        /** Read all at once; might benefit from QDParsing 
        
        myParser.StarFileNodeParse(Wattos.Star.StarGeneral.sp);
        StarFileNode sfnInput = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();
        SaveFrameNode sfn = getFirstSaveFrameByCategory( snInput, saveFrameCategoryName );
        name = sfn.getLabel();         
    }
     */
     
    
    /** Convenience method */
    public String getDefaultMainRelationName() {        
        return mainRelation.name;
    }
    
    /** The 3 strings will look like: atom, atom_main, atom_main_id
     */
    public static void setDerivedNames( String[] names ) {
        names[RELATION_ID_MAIN_RELATION_NAME]   = names[RELATION_ID_SET_NAME].concat(           
            RelationSet.DEFAULT_ATTRIBUTE_MAIN_POSTFIX);
        names[RELATION_ID_COLUMN_NAME]          = names[RELATION_ID_MAIN_RELATION_NAME].concat( 
            Relation.DEFAULT_ATTRIBUTE_PHYSICAL_COLUMN_ID_POSTFIX);
    }

    /** Simple */
    public RelationSoS getRelationSoSParent() {
        return relationSoSParent;
    }

    /** This method will be called by the relation if it resizes.
     *For now only need to reset the convenience variables.
     */
    public boolean doSizeUpdateRelation(String relationName) {
        resetConvenienceVariables();
        return true;
    }
    
    /** Some hardcoded variable names need to be reset when the columns resize. */
    public boolean resetConvenienceVariables() {
        General.showWarning("The relationSet contains only a generic method called: resetConvenienceVariables");
        General.showWarning("it should have been overriden by specializing classes.");        
        return false;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }    
}
