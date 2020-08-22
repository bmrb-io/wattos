/*
 * RelationSoS.java
 *
 * Created on August 14, 2003, 2:21 PM
 */

package Wattos.Database;

import java.util.*;
import java.io.*;
import Wattos.Utils.*;

/**
 * A set of sets of relations. Example is gumbo which is a set of e.g. atom and 
 *residue where e.g. atom is a set of relations with a main relation atom_main.
 *Just like RelationSet this is nothing more than a formal and convenient way to organize the
 *definitions to be used for the objects within.
 * @author Jurgen F. Doreleijers
 */
public class RelationSoS implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;    

    public static String DEFAULT_RELATIONSOS_NAME       = "relationSoS_default_name";
    
    /** General name of the collection of relationSets */
    public String name = DEFAULT_RELATIONSOS_NAME;
    /** Set of relationSets */
    public HashMap relationSoS;
    /** Keep a local reference of the dbms so it doesn't need to be passed
     *for each method. */
    public DBMS dbms;
    
    /** Creates a new instance of RelationSoS */
    public RelationSoS(DBMS dbms) {
        //General.showDebug("now in RelationSOS constructor");
        init(dbms); // Goes to any extending class NOT the init() here.
        //General.showDebug("back in RelationSOS constructor");
    }

    public void init(DBMS dbms) {
        //General.showDebug("now in RelationSOS.init()");
        this.dbms = dbms;
        removeAllRelationSets();
    }
    
    public boolean removeAllRelationSets() {
        // Remove any previous existing relations.        
        if ( relationSoS != null ) {
            Set keys = relationSoS.keySet();
            for (Iterator key=keys.iterator();key.hasNext();) {
                String relationSetName = (String) key.next();
                RelationSet relationSet = (RelationSet) relationSoS.get( relationSetName );
                relationSet.removeAllRelations();
            }
        }
        relationSoS = new HashMap(); // simply nil them.
        return true; 
    }    

    /** Adds a relation to this set. Note that relation is already added to the dbms
     *which is the preferred way to get to the data. The second preferred way to 
     *get to the data is by direct pointer maintained in subclasses. E.g. in the
     *RelationSet Atom there is a variable atomMain that is a pointer to the 
     *relation atomMain. This just does a put into the hashmap for this class.
     *Returns null if a relation already exists with this name.
     */
    public boolean addRelationSet ( RelationSet relationSet ) {
        if ( relationSoS.containsKey( relationSet.name ) ) {
            General.showError("relationSoS: " + name + " already containsKey: " + relationSet.name );
            return false;
        }
        //General.showDebug("Done adding relation set to set-of-set with name: " + relationSet.name);        
        relationSoS.put( relationSet.name, relationSet );
        return true;
    }
    
}
