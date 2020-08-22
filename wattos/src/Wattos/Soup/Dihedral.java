package Wattos.Soup;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Utils.Wiskunde.*;
import Wattos.Database.*;
/**
 *Property of 4 bonded atoms.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Dihedral extends PropNAtom implements Serializable {
            
        
    private static final long serialVersionUID = -1651123173126656773L;

    public Dihedral(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        resetConvenienceVariables();
    }

    public Dihedral(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        super.init(dbms);        
        name =                Gumbo.DEFAULT_ATTRIBUTE_SET_DIHEDRAL[RELATION_ID_SET_NAME];
        
        // MAIN RELATION in addition to the ones in PropNAtom item.        
        
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_DIHEDRAL[RELATION_ID_MAIN_RELATION_NAME];        
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        //General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in dihedral: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;

        // OTHER RELATIONS HERE
        //..
        
        return true;        
    }    
    

    /** Calculates the dihedral angle value.*/
    public void calculateValues(BitSet todo) {
        for (int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            value_1[rid] = (float) ( Geometry.CF * gumbo.atom.calcDihedral( atom_A_Id[rid], atom_B_Id[rid], atom_C_Id[rid], atom_D_Id[rid] ));
        }
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        return true;
    }    
}
 
