package Wattos.Soup;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Utils.General;
/**
 * Property of 2 atoms. Value contains the bond length.
 *
 * The first atom in the bond in case of a hydrogen bond is the donor, the
 * second is the acceptor.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Bond extends PropNAtom implements Serializable {
        
	private static final long serialVersionUID = -1886027960322072182L;
	
    /** So a value of zero denotes a 'single' bond type.
     */
    public static String[] BOND_TYPES = { 
        "unknown", 
        "single", 
        "double", 
        "peptide", 
        "partially double", 
        "hydrogen",
        "tentative"
    };
    public static final int BOND_TYPE_UNKNOWN               = 0;
    public static final int BOND_TYPE_SINGLE                = 1;
    public static final int BOND_TYPE_DOUBLE                = 2;
    public static final int BOND_TYPE_PEPTIDE               = 3;
    public static final int BOND_TYPE_PARTIALLY_DOUBLE      = 4;
    public static final int BOND_TYPE_HYDROGEN              = 5;
    public static final int BOND_TYPE_TENTATIVE             = 6; // Used for looking for potential bonds.
    
        
    public Bond(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        //General.showDebug("back in Bond constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets 
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public Bond(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        //General.showDebug("back in Bond constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }
    
    public boolean init(DBMS dbms) {
        //General.showDebug("now in Bond.init()");
        super.init(dbms);        
        //General.showDebug("back in Bond.init()");
        name =                Gumbo.DEFAULT_ATTRIBUTE_SET_BOND[RELATION_ID_SET_NAME];
        
        // MAIN RELATION in addition to the ones in PropNAtom item.        
        
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_BOND[RELATION_ID_MAIN_RELATION_NAME];        
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

    /** Calculates the bond length.
     *
     */
    public void calculateValues(BitSet todo) {
        for (int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            value_1[rid] = gumbo.atom.calcDistanceFast( atom_A_Id[rid], atom_B_Id[rid] );
        }
    }
    
    /** Assumes only one residue in soup which is the case for a soup from the
     * topology lib.
     * @param resName
     * @param atomName1
     * @param atomName2
     * @return
     */
    public double getBondLength( String resName, String atomName1, String atomName2 ) {
        BitSet resRidSet = SQLSelect.selectBitSet(dbms, gumbo.res.mainRelation, 
                Relation.DEFAULT_ATTRIBUTE_NAME, SQLSelect.OPERATION_TYPE_EQUALS, 
                resName, false);
        int resRid = resRidSet.nextSetBit(0);
        if ( resRid<0 ) {
            return Defs.NULL_DOUBLE;
        }
        if ( atomName1 == null ) {
            General.showError("Atom 1 argument can't be null to bond.getBondLength");
            return Defs.NULL_DOUBLE;
        }
        if ( atomName2 == null ) {
            General.showError("Atom 2 argument can't be null to bond.getBondLength");
            return Defs.NULL_DOUBLE;
        }
        int atomRid1 = gumbo.atom.getRidByAtomNameAndResRid(atomName1, resRid);
        int atomRid2 = gumbo.atom.getRidByAtomNameAndResRid(atomName2, resRid);
          
        Integer atomRid1Int = new Integer(atomRid1);
        Integer atomRid2Int = new Integer(atomRid2);
        BitSet bondList = SQLSelect.selectCombinationBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, SQLSelect.OPERATION_TYPE_EQUALS, atomRid1Int,
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, SQLSelect.OPERATION_TYPE_EQUALS, atomRid2Int,
                SQLSelect.OPERATOR_AND, false);
        int bondRid = bondList.nextSetBit(0);
        if ( bondRid<0 ) {
            bondList = SQLSelect.selectCombinationBitSet(dbms, mainRelation, 
                    Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, SQLSelect.OPERATION_TYPE_EQUALS, atomRid1Int,
                    Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, SQLSelect.OPERATION_TYPE_EQUALS, atomRid2Int,
                    SQLSelect.OPERATOR_AND, false);
        }
        bondRid = bondList.nextSetBit(0);
        if ( bondRid<0 ) {
            General.showDebug("Failed to get the bond lenght from What If lib; TODO: add generalized estimate.");
            return Defs.NULL_DOUBLE;
        }
        double bl = gumbo.atom.calcDistanceFast( atomRid1, atomRid2 );
//        General.showDebug("Found bond lenght in What If lib: " + bl);
//        General.showDebug(gumbo.atom.toString(atomRid1));
//        General.showDebug(gumbo.atom.toString(atomRid2));
        return bl;      
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        return true;
    }

    /** Simply uses both sides of the bond to see if it matches. May be called
     * by a non master atom.
     * */
    public BitSet getBondListForAtomRid(int atomRid) {
        int masterAtomRid = gumbo.atom.masterAtomId[ atomRid ];
        Integer atomRidInt = new Integer(masterAtomRid);
        BitSet bondRidListA = SQLSelect.selectBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                atomRidInt, false);                
        BitSet bondRidListB = SQLSelect.selectBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                atomRidInt, false);
        bondRidListA.or(bondRidListB);
        return bondRidListA;
    }
    
    /** Simply uses both sides of the bond to see if it matches */
    public BitSet getBondListForResRidSet(BitSet resRidSet) {        
        BitSet atomRidSet = gumbo.res.getAtoms(resRidSet);
        BitSet bondRidListA = SQLSelect.selectBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                atomRidSet, false);                
        BitSet bondRidListB = SQLSelect.selectBitSet(dbms, mainRelation, 
                Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID, SQLSelect.OPERATION_TYPE_EQUALS, 
                atomRidSet, false);
        bondRidListA.or(bondRidListB);
        return bondRidListA;
    }    
    
}
 
