package Wattos.Soup.Constraint;

import java.io.File;
import java.io.Serializable;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.Indices.Index;
import Wattos.Database.Indices.IndexSortedInt;
import Wattos.Soup.Gumbo;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Strings;
import cern.colt.list.IntArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;
/**
 * Property of 3 atoms. Value contains the angle in degrees.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */ 
public class Rdc extends SimpleConstr implements Serializable {
                    
	private static final long serialVersionUID = -1886027960322072182L;
 
	public Rdc(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        //General.showDebug("back in angle constructor");
        resetConvenienceVariables();
    }

    /** The relationSetName is a parameter so non-standard relation sets  
     *can be created; e.g. AtomTmp with a relation named AtomTmpMain etc.
     */
    public Rdc(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
//        General.showDebug("back in Rdc constructor");
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables(); 
    }
    
    public boolean init(DBMS dbms) {
//        General.showDebug("now in Rdc.init()");
        super.init(dbms);        
//        General.showDebug("back in Rdc.init()");
        name =                Constr.DEFAULT_ATTRIBUTE_SET_RDC[RELATION_ID_SET_NAME];
        // MAIN RELATION in addition to the ones in PropNAtom item.        
        DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
        
        relationName    = Constr.DEFAULT_ATTRIBUTE_SET_RDC[RELATION_ID_MAIN_RELATION_NAME];
        atomRelationName=relationName+SimpleConstr.DEFAULT_ATTRIBUTE_ATOM_RELATION_EXTENSION;
        violRelationName=relationName+SimpleConstr.DEFAULT_ATTRIBUTE_VIOL_RELATION_EXTENSION;
        try {
            mainRelation = new Relation(relationName, dbms, this);
            simpleConstrAtom = new Relation(atomRelationName, dbms, this);
            simpleConstrViol = new Relation(violRelationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        //General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in angle: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        mainRelation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS);
        addRelation( mainRelation );

        // OTHER RELATIONS HERE
        //..
        //General.showDebug( "DEFAULT_ATTRIBUTES_TYPES in angle: " + Strings.toString(DEFAULT_ATTRIBUTES_TYPES));
        // Create the fkcs without checking that the columns exist yet.
        A_DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
        A_DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));

        A_DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
        A_DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );

        A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_RDC[         RELATION_ID_MAIN_RELATION_NAME]});
        A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
        A_DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, A_DEFAULT_ATTRIBUTE_FKCS_FROM_TO, 
                atomRelationName);        
        simpleConstrAtom.insertColumnSet( 0, A_DEFAULT_ATTRIBUTES_TYPES, A_DEFAULT_ATTRIBUTES_ORDER, 
                A_DEFAULT_ATTRIBUTE_VALUES, A_DEFAULT_ATTRIBUTE_FKCS);
        addRelation( simpleConstrAtom );        

        V_DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
        V_DEFAULT_ATTRIBUTES_TYPES.put( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],  new Integer(DATA_TYPE_INT));
        V_DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ] );
        V_DEFAULT_ATTRIBUTES_ORDER.add( Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ] );
        V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_RDC[      RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_RDC[         RELATION_ID_MAIN_RELATION_NAME]});
        V_DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[ RelationSet.RELATION_ID_COLUMN_NAME ],    Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[    RELATION_ID_MAIN_RELATION_NAME]});
        
        simpleConstrViol.insertColumnSet( 0, V_DEFAULT_ATTRIBUTES_TYPES, V_DEFAULT_ATTRIBUTES_ORDER, 
                V_DEFAULT_ATTRIBUTE_VALUES, V_DEFAULT_ATTRIBUTE_FKCS);
        addRelation( simpleConstrViol );   
        return true;
    }    
        
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        ATTRIBUTE_SET_SUB_CLASS         = Constr.DEFAULT_ATTRIBUTE_SET_RDC;
        ATTRIBUTE_SET_SUB_CLASS_LIST    = Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST;
        scListIdMain          =             mainRelation.getColumnInt(    Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[  RELATION_ID_COLUMN_NAME]);
        scListIdAtom          =             simpleConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[  RELATION_ID_COLUMN_NAME]);
        scIdAtom              =             simpleConstrAtom.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_RDC[  RELATION_ID_COLUMN_NAME ]);
        
        scListIdViol          =             simpleConstrViol.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_RDC_LIST[  RELATION_ID_COLUMN_NAME]);
        scMainIdViol          =             simpleConstrViol.getColumnInt(  Constr.DEFAULT_ATTRIBUTE_SET_RDC[       RELATION_ID_COLUMN_NAME]);
        
        return true;
    }

    
    
    /** Ambies are not allowed by XPLOR convention.
    ASSI { 6} (( segid "SH3 " and resid 53 and name N )) (( segid "SH3 " and resid 53 and name CA ))  
         { 6} (( segid "SH3 " and resid 53 and name C )) (( segid "SH3 " and resid 54 and name C  )) 1.00 -60.00 40.00 2
         1st energy constant
         2nd target
         3rd range around target
         4th exponent
         */
    public boolean toXplorOrSo(BitSet scSet, String fn, 
            int fileCount, String atomNomenclature, boolean sortRestraints, String format) {
        int scCountTotal = scSet.cardinality();
//        General.showDebug( "Total number of  constraints todo: " + scCountTotal );        
        if ( scCountTotal == 0 ) {
            General.showWarning("No constraints selected in toXplor.");
            return true;
        }
        
        // Write them in a sorted fashion if needed.
        int[] map = null;
        if ( sortRestraints ) {
            if ( ! sortAll( scSet, 0 )) {
                General.showError("Couldn'sort the simple constraints");
                return false;
            }
            map = mainRelation.getRowOrderMap(Relation.DEFAULT_ATTRIBUTE_ORDER_ID  ); // Includes just the scs in this list
            if ( (map != null) && (map.length != scCountTotal )) {
                General.showWarning("Trying to get an order map but failed to give back the correct number of elements: " + scCountTotal + " instead found: " + map.length );
                map = null;
            }
            if ( map == null ) {
                General.showWarning("Failed to get the row order sorted out for  constraints; using physical ordering."); // not fatal
                map = PrimitiveArray.toIntArray( scSet );
                if ( map == null ) {
                    General.showError("Failed to get the used row map list so not writing this table.");
                    return false;
                }
            }
        } else {
            map = PrimitiveArray.toIntArray( scSet );
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
        mapXor.xor(scSet);
        if ( mapXor.cardinality() != 0 ) {
            General.showError("The map after reordering doesn't contain all the elements in the original set or vice versa.");
            General.showError("In the original set:" + PrimitiveArray.toString( scSet ));
            General.showError("In the ordered  set:" + PrimitiveArray.toString( mapAsSet ));
            General.showError("Xor of both sets   :" + PrimitiveArray.toString( mapXor ));
            General.showError("The order column   :" + PrimitiveArray.toString( mainRelation.getColumnInt(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)));
            return false;
        }

        
        // Important to get the indexes after the sortAll.
        IndexSortedInt indexMainAtom = (IndexSortedInt) simpleConstrAtom.getIndex(
                Constr.DEFAULT_ATTRIBUTE_SET_RDC[RelationSet.RELATION_ID_COLUMN_NAME ], 
                Index.INDEX_TYPE_SORTED);
        if ( indexMainAtom == null ) {
            General.showCodeBug("Failed to get all indexes to sc main");
            return false;
        }
        
        StringBuffer sb    = new StringBuffer();
        StringBuffer sbRst = new StringBuffer();
        Parameters p = new Parameters();
        // FOR EACH CONSTRAINT
        int constraintId = 0;
        for (;constraintId<map.length;constraintId++) { 
            int currentSCId = map[ constraintId ];
//            General.showDebug("Preparing  constraint: " + constraintId + " at rid: " + currentSCId);
            Integer currentSCIdInteger = new Integer(currentSCId);
            IntArrayList scAtoms = (IntArrayList) indexMainAtom.getRidList(  currentSCIdInteger, 
                    Index.LIST_TYPE_INT_ARRAY_LIST, null);
//            General.showDebug("Found the following rids of sc atoms in constraint: " + 
//                    PrimitiveArray.toString( scAtoms ));                
            if ( scAtoms == null ) {
                General.showError("Failed to get any atoms");
                return false;
            }                    
            if ( scAtoms.size() != 2 ) {
                General.showError("Failed to get exactly two atoms");
                return false;
            }
            if ( ! PrimitiveArray.orderIntArrayListByIntArray( scAtoms, orderAtom )) {
                General.showError("Failed to order atoms by order column");
                return false;
            }
            sbRst.setLength(0);
            String[] atomOrientationList = { "OO", "X", "Y", "Z"};
            String segiOrientation = getSegiOrientation(fileCount);
            sbRst.append( "assi{" + Format.sprintf( "%5d", p.add( (constraintId+1) )) + "}");
            for (int o=0;o<4;o++) {
                sbRst.append(Format.sprintf( "(atom \"%4s\" 999 %-4s)",
                        p.add(segiOrientation).add(atomOrientationList[o])));
            }
            sbRst.append("\n           ");
            for (int atom_id=0;atom_id<2;atom_id++) {
                // FOR EACH ATOM in member
                int currentSCAtomId = scAtoms.getQuick( atom_id );
                int currentAtomId   = atomIdAtom[       currentSCAtomId];
                int currentResId    = gumbo.atom.resId[ currentAtomId];
                int currentMolId    = gumbo.res.molId[  currentResId];
                if ( Defs.isNull( currentAtomId ) ) { // Was the atom actually matched in the structure?
                    General.showError("Got null for atomId in Rdc");
                    return false;
                }
                String atomName = gumbo.atom.nameList[currentAtomId ];
                int resNum      = gumbo.res.number[   currentResId ];
                String resName  = gumbo.res.nameList[ currentResId ];
                int molNum      = gumbo.mol.number[   currentMolId];                             
                sbRst.append(constr.toXplorAtomSel(molNum, resNum, resName, atomName,atomNomenclature));                
            } // end of loop per atom
            float[] xplorValueSet = toXplorValueSet( 
                    target[  currentSCId],
                    uppBound[currentSCId]
                    );
            if ( xplorValueSet == null ) {
            	General.showError("Failed to convert rdcs to xplor for restraint: " +
            			toString(currentSCId));
            	return false;
            }
            for (int i=0;i<2;i++ ) {
            	sbRst.append(Format.sprintf( "%9.4f ", p.add( xplorValueSet[i] )));
            }                
            sbRst.append('\n');                                    
            sb.append(sbRst.toString()); 
        } // end of loop over restraints.
        fn = fn + "_rdc";
        String outputFileName = InOut.addFileNumberBeforeExtension( fn, fileCount, true, 3 );        
        File f = new File(outputFileName+".tbl");
//        General.showDebug(sb.toString());
        InOut.writeTextToFile(f,sb.toString(),true,false);
        General.showOutput("Written " + Strings.sprintf(constraintId,"%5d") + " rdcs to: " + f.toString());
        return true;
    }       
    
    public static String getSegiOrientation(int fileCount) {
        Parameters p = new Parameters();
        String result = Format.sprintf("O%03d", p.add( fileCount));
        if ( result == null ) {
            General.showError("Failed to convert fileCount to segi orientation for: ["+ fileCount+"]");
            return null;
        }
        return result;
    }

    /** Returns target and error from tar and upp */
    public static float[] toXplorValueSet(float tar, float upp) { 
        float err = upp-tar;
        return new float[] {tar, err};
    }
}
 
