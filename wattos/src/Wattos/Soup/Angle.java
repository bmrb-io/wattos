package Wattos.Soup;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSoS;
import Wattos.Utils.General;
import Wattos.Utils.Wiskunde.Geometry;
/**
 * Property of 3 atoms. Value contains the angle in degrees.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */ 
public class Angle extends PropNAtom implements Serializable {
            
        
	private static final long serialVersionUID = -1886027960322072182L;

	public Angle(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        //General.showDebug("back in angle constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public Angle(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in angle constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        //General.showDebug("now in angle.init()");
        super.init(dbms);        
        //General.showDebug("back in angle.init()");
        name =                Gumbo.DEFAULT_ATTRIBUTE_SET_ANGLE[RELATION_ID_SET_NAME];
        
        // MAIN RELATION in addition to the ones in PropNAtom item.        
        
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_ANGLE[RELATION_ID_MAIN_RELATION_NAME];        
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        //General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in angle: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;
        dbms.foreignKeyConstrSet.removeForeignKeyConstrFrom(relationName, Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID);

        // OTHER RELATIONS HERE
        //..
        
        return true;        
    }    
    

    /** Calculates the angle length. Look at Namot on how to do angle and dihedrals...todo*/
    public void calculateValues(BitSet todo) {
        for (int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            value_1[rid] = (float) (Geometry.CF * gumbo.atom.calcAngle( atom_A_Id[rid], atom_B_Id[rid], atom_C_Id[rid] ));
        }
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        return true;
    }    
}
 
