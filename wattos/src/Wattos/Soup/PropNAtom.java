package Wattos.Soup;

import java.io.Serializable;
import java.util.BitSet;

import Wattos.CloneWars.WattosItem;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Utils.General;
import Wattos.Utils.PrimitiveArray;
import cern.colt.list.IntArrayList;

import com.braju.format.Format;
import com.braju.format.Parameters;
/**
 *Property of one to N atoms together. E.g. 2 atoms for a bond property see the
 *Bond class. Just set the other atom rids to Defs.NULL_INT. Current implementation
 *allows for a maximum of 4 atoms.
 *The property can span residues and molecules AND models and entries. To span
 *models you have to use the xxxxList parameters. They are of the dimension of the
 *number of models at the time of creation. If a model is removed then these parameters
 *are not automatically resized so watch out!
 *Only the first two atoms are actually included in the fkcs.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class PropNAtom extends WattosItem implements Serializable {
        
    private static final long serialVersionUID = 6773493380485521751L;
    public static final String REFERENCE_VALUE              = "referenceValue";
    /** For Standard deviation */
    public static final String REFERENCE_SD                 = "referenceSD";
    
    /** Convenience variables */
    public int[]       atom_A_Id;          // starting with fkcs
    public int[]       atom_B_Id;          
    public int[]       atom_C_Id;          
    public int[]       atom_D_Id;          
    public int[]       modelId;          
    public int[]       entryId;          
    /** Use Angstroms for length units, and radians for angles.     */
    public float[]     value_1;
    /** Per model values. */
    public float[][]   value_1List;    
    /** Average over all models for which the property is defined. *
    public float[]     value_1ListAvg;
    /** Minimum over all models for which the property is defined. *
    public float[]     value_1ListMin;
    /** Maximum over all models for which the property is defined. *
    public float[]     value_1ListMax;
    /** The second value can be used for e.g. standard deviations.     */
    public float[]     value_2;           
    public float[]     referenceValue;           
    public float[]     referenceSD;           

    public Gumbo       gumbo;          // so cast doesn't need to be done.
    
    public static final int NUMBER_ATOMS = 4; // Add more later if needed.
    
    public PropNAtom(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }

    public PropNAtom(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
        gumbo = (Gumbo) relationSoSParent;
        resetConvenienceVariables();
    }

    public boolean init(DBMS dbms) {
        super.init(dbms);
 
        // MAIN RELATION in addition to the ones in gumbo item.        
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID,                          new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID,                          new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_C_ID,                          new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID,                          new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_VALUE_1,        new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_VALUE_1_LIST,   new Integer(DATA_TYPE_ARRAY_OF_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( DEFAULT_ATTRIBUTE_VALUE_2,        new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( REFERENCE_VALUE,                  new Integer(DATA_TYPE_FLOAT));
        DEFAULT_ATTRIBUTES_TYPES.put( REFERENCE_SD,                     new Integer(DATA_TYPE_FLOAT));
        
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RELATION_ID_COLUMN_NAME ]);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RELATION_ID_COLUMN_NAME ]);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_C_ID);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID);         
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_VALUE_1);                            
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_VALUE_1_LIST);                            
        DEFAULT_ATTRIBUTES_ORDER.add( DEFAULT_ATTRIBUTE_VALUE_2);                            
        DEFAULT_ATTRIBUTES_ORDER.add( REFERENCE_VALUE);                            
        DEFAULT_ATTRIBUTES_ORDER.add( REFERENCE_SD);                            
        
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID,                           Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID,                           Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_ATOM_C_ID,                           Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID,                           Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RELATION_ID_MAIN_RELATION_NAME]});
        return true;
    }            

    
    public BitSet getOutliers( BitSet todo, float sdCount ) {
        BitSet result = new BitSet( todo.size());
        float dev;
        for (int i=todo.nextSetBit(0);i>=0;i=todo.nextSetBit(i+1)) {
            dev = value_1[i] - referenceValue[i];
            if ( Math.abs( dev/referenceSD[i]) > sdCount ) {
                result.set(i);                
            }
            value_2[i] = (value_1[i] - referenceValue[i]) / referenceSD[i];                        
        }
        return result;
    }
    
   /** Adds a new entry; Only the first atom rid needs to exist. Non-existing atom 
    rids should be set to Defs.NULL_INT
    */
    public int add(//empty                                                     // WattosItem attributes
        //boolean has_coor, float[] coor, float charge,                        // GumboItem attributes
        int atom_A_Id,                                                         // Prop4Atom attributes.
        int atom_B_Id, 
        int atom_C_Id, 
        int atom_D_Id, 
        int type ) {                                                           
            
        int maxSize = mainRelation.sizeMax;
        int result = super.add( null );
        if ( result < 0 ) {
            General.showCodeBug( "Failed to get a new row id for a prop4Atom ");
            return -1;
        }                        
        if ( maxSize != mainRelation.sizeMax) {
            resetConvenienceVariables();
        }
        this.atom_A_Id[ result ] = atom_A_Id;
        this.atom_B_Id[ result ] = atom_B_Id;
        this.atom_C_Id[ result ] = atom_C_Id;
        this.atom_D_Id[ result ] = atom_D_Id;
        this.type[      result ] = type;
        modelId[            result ] = gumbo.atom.modelId[   atom_A_Id ];
        entryId[            result ] = gumbo.atom.entryId[   atom_A_Id ];
        
        return result;
    }     
       


    
    /** Returns how many atoms are actually involved in this prop by looking
     *at whether the atoms involved are Defs.NULL_INT.
     */
    public int getAtomCount(int rid) {
        int atom_rid = Defs.NULL_INT;
        int atom_count = 0;
        for ( int a=0;a<NUMBER_ATOMS;a++) { // atomIds.length is fixed to 2 for propNAtoms but angles follow?
            switch ( a ) {
                case 0: { atom_rid=atom_A_Id[ rid ]; break; }                
                case 1: { atom_rid=atom_B_Id[ rid ]; break; }                
                case 2: { atom_rid=atom_C_Id[ rid ]; break; }                
                case 3: { atom_rid=atom_D_Id[ rid ]; break; }                
            }
            //General.showDebug("In getAtomCount with a=" + a + "and atom_rid: " + atom_rid);
            if ( Defs.isNull( atom_rid ) ) { // skip this and following atoms because prop isn't on all 4.
                break;
            }
            atom_count++;
            atom_rid = Defs.NULL_INT;
        }
        return atom_count;
    }
    
    public String toString( BitSet todo) {
        return toString( todo, false, Defs.NULL_FLOAT );
    }
    
    /** Gives a header and the list    
     */
    public String toString(BitSet todo, boolean showModelRelated, float cutoff) {
        int atom_count = 1; // A minimum of 1 is garanteed.
        int rid = todo.nextSetBit(0);
        if ( rid >= 0 ) {
            atom_count = getAtomCount(rid);
        }

        StringBuffer sb = new StringBuffer();
        for ( int a=0;a<atom_count;a++) { // atomIds.length is fixed to 2 for propNAtoms but angles follow?
            sb.append("Atom Res#(Nam) Mol# ");
        }
        sb.append("Model# Entry# Value_1  Value_2 RefValue    RefSD");        
        sb.append(" Type   Minimum  Average  Maximum Under cutoff\n");        
        for (rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            sb.append( toString( rid, showModelRelated, cutoff ) + General.eol );            
        }
        return sb.toString();
    }
    
    public String toString( int rid ) {
        return toString( rid, false, Defs.NULL_FLOAT );
    }
    
    /** Return the entry, model, mol, res, and atom ids as a string.
     *The model related can be shown if requested. The Min, max, average and a 
     *list of them under the cutoff will be shown.
     *From the molmol help as implemented here:
     
     <PRE>In the report produced by this command, a table of
     distances will be created for each set of molecules
     with the same structure. A '+' sign indicates that
     the short distance is present in the molecule
     corresponding to the column, a '*' marks the smallest
     of these distances. The '+' sign is replaced by a '5'
     for molecules 5, 15, 25, ... and by a '0' for
     molecules 10, 20, 30, ...
     *</PRE> 
     *Wattos uses a '-' for the smallest value and a '+'
     *for the largest value. An '*' marks a value that meets the cutoff.
     */
    public String toString( int rid, boolean showModelRelated, float cutoff ) {
        if ( rid < 0 ) {
            General.showError("Can't do toString for prop4Atom rid < 0: " + rid);
            return null;
        }
        StringBuffer sb = new StringBuffer();        
        Parameters p = new Parameters(); // for printf
        p.autoClear(true);
        int atom_rid = 0;
        for ( int a=0;a<NUMBER_ATOMS;a++) { // atomIds.length is fixed to 2 for propNAtoms but angles follow?
            switch ( a ) {
                case 0: { atom_rid=atom_A_Id[ rid ]; break; }                
                case 1: { atom_rid=atom_B_Id[ rid ]; break; }                
                case 2: { atom_rid=atom_C_Id[ rid ]; break; }                
                case 3: { atom_rid=atom_D_Id[ rid ]; break; }                
            }
            if ( Defs.isNull( atom_rid ) ) { // skip this and following atoms because prop isn't on all 4.
                break;
            }
            int res_rid = gumbo.atom.resId[ atom_rid ];
            int mol_rid = gumbo.atom.molId[ atom_rid ];
            String name     = gumbo.atom.nameList[ atom_rid ];
            int resNumber   = gumbo.res.number[    res_rid];
            String resName  = gumbo.res.nameList[  res_rid];
            int molNumber   = gumbo.mol.number[    mol_rid];
            p.add( name );
            p.add( resNumber);
            p.add( resName);
            p.add( molNumber);
            sb.append( Format.sprintf( "%-4s %4d(%-3s) %4d ", p));
        }
        
        atom_rid=atom_A_Id[ rid ]; // take the first atom which always is present for real.
        int model_rid   = gumbo.atom.modelId[   atom_rid ];
        int entry_rid   = gumbo.atom.entryId[   atom_rid ];
        
        int modelNumber = gumbo.model.number[ model_rid ];
        int entryNumber = gumbo.entry.number[ entry_rid ];            
        
        p.add( modelNumber);
        p.add( entryNumber);
        sb.append( Format.sprintf( "%6d %6d", p));
        
        float[] values = new float[] {        
            value_1[         rid],
            value_2[         rid],
            referenceValue[  rid],
            referenceSD[     rid]
        };
        for (int i=0;i<values.length;i++) {
            float v = values[i];
            if ( Defs.isNull(v)) {
                sb.append( "         " );
            } else {
                p.add( v );
                sb.append( Format.sprintf( "%8.3f ", p));
            }
        }             
        p.add( type[rid] );
        sb.append( Format.sprintf( " %4d", p)); 
        if ( showModelRelated ) {
            float min = PrimitiveArray.getMin(       value_1List[rid]);
            float max = PrimitiveArray.getMax(       value_1List[rid]);
            p.add( min );
            sb.append( Format.sprintf( " %8.3f", p));             
            p.add( PrimitiveArray.getAverage(   value_1List[rid]));
            sb.append( Format.sprintf( " %8.3f", p));             
            p.add( max );
            sb.append( Format.sprintf( " %8.3f", p)); 
            sb.append( " " + PrimitiveArray.toStringMakingCutoff(value_1List[rid],cutoff,true));
        }
        return sb.toString();
    }
    

    /** both atoms need to be present for this to be true
     */
    public boolean isInStandardResidue(int rid) {
        int atom_rid = 0;
        for ( int a=0;a<NUMBER_ATOMS;a++) { // atomIds.length is fixed to 2 for propNAtoms but angles follow?
            switch ( a ) {
                case 0: { atom_rid=atom_A_Id[ rid ]; break; }                
                case 1: { atom_rid=atom_B_Id[ rid ]; break; }                
                case 2: { atom_rid=atom_C_Id[ rid ]; break; }                
                case 3: { atom_rid=atom_D_Id[ rid ]; break; }                
            }
            if ( Defs.isNull( atom_rid ) ) { // skip this and following atoms because prop isn't on all 4.
                break;
            }            
            if ( ! Biochemistry.commonResidueNameAA_NA.containsKey( gumbo.res.nameList[gumbo.atom.resId[atom_rid]])) {
                return false;
            }
        }
        return true;
    }

    
    /** If any of the items contain an atom contained in the residueList given then
     it's a hit. Only the todo list will be scanned.
     */
    public BitSet scanForResidues(BitSet todo, int[] residueList) {
        BitSet result = new BitSet();
        if ( residueList.length == 0 ) {
            return result;
        }
        int atom_rid = 0;
//        int res_rid = 0;
        int a=0;
        int r=0;
        for ( int rid=todo.nextSetBit(0);rid>=0;rid=todo.nextSetBit(rid+1)) {
            for ( a=0;a<NUMBER_ATOMS;a++) { // atomIds.length is fixed to 2 for propNAtoms but angles follow?
                switch ( a ) {
                    case 0: { atom_rid=atom_A_Id[ rid ]; break; }                
                    case 1: { atom_rid=atom_B_Id[ rid ]; break; }                
                    case 2: { atom_rid=atom_C_Id[ rid ]; break; }                
                    case 3: { atom_rid=atom_D_Id[ rid ]; break; }                
                }
                if ( Defs.isNull( atom_rid ) ) { // skip this and following atoms because prop isn't on all 4.
                    break;
                }           
                for ( r=residueList.length-1;r>=0;r--) {
                    if ( gumbo.atom.resId[atom_rid]==residueList[r]) {
                        result.set(rid);
                        break;
                    }
                }
            }
        }
        return result;
    }
    
    /** Returns the list of atom rids for the atom selected (A, B, etc.)
     */
    public IntArrayList getAtomList(IntArrayList list, int atomPos) {
        //General.showDebug("input: " + list);
        if ( list == null ) {
            return null;
        }
        if ( (atomPos < 0) || (atomPos>3) ) {
            General.showError("In PropNAtom.getAtomList the position of the atom should be in range: [0,3] but was: "+atomPos);
            return null;
        }
        int s = list.size();
        IntArrayList result = new IntArrayList(s);
        result.setSize(s);
        switch ( atomPos ) { // switch is outside the loop for speed.
            case 0: { 
                for (int i=0;i<s;i++) { 
                    result.setQuick(i, atom_A_Id[list.getQuick(i)]);
                }
                break;
            }
            case 1: { 
                for (int i=0;i<s;i++) { 
                    result.setQuick(i, atom_B_Id[list.getQuick(i)]);
                }
                break;
            }
            case 2: { 
                for (int i=0;i<s;i++) { 
                    result.setQuick(i, atom_C_Id[list.getQuick(i)]);
                }
                break;
            }
            case 3: { 
                for (int i=0;i<s;i++) { 
                    result.setQuick(i, atom_D_Id[list.getQuick(i)]);
                }
                break;
            }
        }
        //General.showDebug("output:" + result);
        return result;
    }
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        //General.showDebug("Now in resetConvenienceVariables in PropNAtom");
        modelId             =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME]);
        entryId             =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME]);
        atom_A_Id           =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_ATOM_A_ID);                 
        atom_B_Id           =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_ATOM_B_ID);                 
        atom_C_Id           =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_ATOM_C_ID);                 
        atom_D_Id           =               mainRelation.getColumnInt(  Gumbo.DEFAULT_ATTRIBUTE_ATOM_D_ID);                 
        value_1             =               mainRelation.getColumnFloat(      DEFAULT_ATTRIBUTE_VALUE_1);                   // PropNAtom (non fkcs)
        value_2             =               mainRelation.getColumnFloat(      DEFAULT_ATTRIBUTE_VALUE_2);                   
        referenceValue      =               mainRelation.getColumnFloat(      REFERENCE_VALUE);                
        referenceSD         =               mainRelation.getColumnFloat(      REFERENCE_SD);                   
        value_1List         = (float[][])   mainRelation.getColumnFloatList(  DEFAULT_ATTRIBUTE_VALUE_1_LIST );            
        
        if ( modelId == null || entryId == null 
          || atom_A_Id == null 
          || atom_B_Id == null 
          || atom_C_Id == null 
          || atom_D_Id == null 
          || value_1 == null 
          || value_2 == null 
          || referenceValue == null 
          || referenceSD == null 
          || value_1List == null                
          ) {
            return false;
        }
        return true;
    }            
}
 
