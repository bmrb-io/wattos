/*
 * GumboItem.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup.Constraint;

import java.io.*;
import Wattos.Soup.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.CloneWars.*;

/**
 *Encapsulates a few variables for all restraint items that specialize
 *the class.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class ConstrItem extends WattosItem implements Serializable {
       
    private static final long serialVersionUID = -1207795172754062330L;    
    public Constr      constr;          // so cast doesn't need to be done.
    public Gumbo       gumbo;           // so cast doesn't need to be done.

    public ConstrItem(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        constr = (Constr) relationSoSParent;
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public ConstrItem(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        super.init(dbms);
        return true;
    }
           
    /** Some hardcoded variable names need to be reset when the columns resize. */
    public boolean resetConvenienceVariables() {
        super.resetConvenienceVariables();
        //General.showDebug("Now in resetConvenienceVariables in ConstrItem");
        Relation atomMain = dbms.getRelation( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME] );        
        if ( atomMain == null ) {
            General.showError("failed to find the atom main relation");
            return false;
        }
        RelationSet atom = (Atom) atomMain.getRelationSetParent();
        if ( atom == null ) {
            General.showError("failed to find atom RelationSet");
            return false;
        }        
        gumbo = (Gumbo) atom.getRelationSoSParent();
        if ( gumbo == null ) {
            General.showError("failed to find the gumbo RelationSoS");
            return false;
        }                
        return true;
    }    
}
 
