package Wattos.Soup;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSoS;
import Wattos.Utils.General;
/**
 * Property of 2 atoms. Value contains the distance in Angstrom.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Distance extends PropNAtom implements Serializable {
        
    private static final long serialVersionUID = 2811686898379420444L;

    public Distance(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        //General.showDebug("back in Distance constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public Distance(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Distance constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        //General.showDebug("now in Distance.init()");
        super.init(dbms);        
        //General.showDebug("back in Distance.init()");
        name =                Gumbo.DEFAULT_ATTRIBUTE_SET_DISTANCE[RELATION_ID_SET_NAME];
        
        // MAIN RELATION in addition to the ones in PropNAtom item.        
        
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_DISTANCE[RELATION_ID_MAIN_RELATION_NAME];        
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        //General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in bond: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( relation );
        mainRelation = relation;
        dbms.foreignKeyConstrSet.removeForeignKeyConstrFrom(relationName, Gumbo.DEFAULT_ATTRIBUTE_ATOM_C_ID);
        dbms.foreignKeyConstrSet.removeForeignKeyConstrFrom(relationName, Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID);

        // OTHER RELATIONS HERE
        //..
        
        return true;        
    }    
    /** Calculates the distance values value including the model related if requested.
     *Make sure that the models are synced before calling this routine.
     */
    public boolean calculateValues(BitSet todo, boolean doModelRelated) {
        int distRid=todo.nextSetBit(0);
        if ( distRid < 0 ) {
            General.showWarning("No values to calculate for distances.");
            return true;
        }
        int atomRid=atom_A_Id[distRid];
        int modelCount = gumbo.atom.modelSiblingIds[atomRid].length;
        
        for (;distRid>=0;distRid=todo.nextSetBit(distRid+1)) {
            int atom_A_rid =atom_A_Id[distRid];
            int atom_B_rid =atom_B_Id[distRid];
            value_1[distRid] = gumbo.atom.calcDistance( atom_A_rid, atom_B_rid);
            if ( doModelRelated ) {
                value_1List[distRid] = new float[modelCount];
                value_1List[distRid][0] = value_1[distRid];
                for (int n=1;n<modelCount;n++) {
                    int ARid = gumbo.atom.modelSiblingIds[atom_A_rid][n];
                    int BRid = gumbo.atom.modelSiblingIds[atom_B_rid][n];
                    value_1List[distRid][n] = gumbo.atom.calcDistance( ARid, BRid);
                }
            }
        }
        return true;
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        return true;
    }    
}
 
