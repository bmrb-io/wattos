/*
 * Molecule.java
 *
 * Created on November 8, 2002, 4:41 PM
 */

package Wattos.Soup;

import java.io.File;
import java.io.Serializable;
import java.util.BitSet;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.ForeignKeyConstrSet;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Database.RelationSoS;
import Wattos.Database.SQLSelect;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.StringIntMap;
import Wattos.Utils.Strings;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 *Molecule contains one or more residues.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Molecule extends GumboItem implements Serializable {
    private static final long serialVersionUID = -1207795172754062330L;        
        
    /** Convenience variables */
    public int[]       modelId;        // starting with fkcs
    public int[]       entryId;          
    //public int[]       type;    // e.g. polymer
    public int[]       polType; // e.g. polydeoxyribonucleotide
    public String[]     asymId;
    
    // enumerations will be taken as an integer value.
    public static String[] typeEnum = new String[] {
            "polymer", // 0
            "non-polymer", // 1 and has to stay 1 for hardcoded
            "water",
            "other",
            "unknown" // last one should always be unknown.
    };
    /** Molecule is a polymer */
    public static int POLYMER_TYPE      =  0;
    /** Molecule is NOT a polymer */
    public static int NON_POLYMER_TYPE  =  1;   
    /** Molecule is a polymer */
    public static int WATER_TYPE        =  2;
    /** Molecule might be a polymer, non-polymer, water, or other, it's unknown at this point.*/
    public static int UNKNOWN_TYPE      =  typeEnum.length -1;   
    public static StringIntMap typeEnumMap;
    
    public static String[] polTypeEnum = new String[] {
        "DNA/RNA hybrid",  // 0
        "polydeoxyribonucleotide" ,
        "polypeptide(D)", 
        "polypeptide(L)", 
        "polyribonucleotide", 
        "polysaccharide(D)", 
        "polysaccharide(L)",
        "other",
        "non-polymer",
        "unknown" // last one should always be unknown.
};
    public static int UNKNOWN_POL_TYPE  =  polTypeEnum.length -1;   
    public static int NON_POLYMER_POL_TYPE  =  polTypeEnum.length -2;   
    public static StringIntMap polTypeEnumMap;    

    public static String[] polTypeEnumXplor = new String[] {
        "rna",  // need to patch only the deoxies.
        "dna" ,
        "prot", 
        "prot", 
        "rna", 
        "SUGAR",          // needs to be added to psfGen defs
        "SUGAR",
        "OTHER",
        "NON-POLYMER",
        "UNKNOWN" // last one should always be unknown.
};
    
    
    public Molecule(DBMS dbms, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent); 
    }

    public Molecule(DBMS dbms, String relationSetName, RelationSoS relationSoSParent) {
        super(dbms, relationSoSParent);
        name = relationSetName;
    }

    static {
        int[] sequentialArray = PrimitiveArray.createSequentialArray( typeEnum.length, 1);
        typeEnumMap = new StringIntMap( typeEnum, sequentialArray );
        sequentialArray = PrimitiveArray.createSequentialArray( polTypeEnum.length, 1);
        polTypeEnumMap = new StringIntMap( polTypeEnum, sequentialArray );
    }
    
    /** Translates 1,2,..,26,27,... to A,B,C,..,AA,AB,..
     *Returns Defs.NULL_STRING_NULL for out of range.
     */ 
    public static String toChain(int i) {        
        if ( i>0 && i<=26 ) {
            char c = (char) (i + 64);
            return new String(new char[] {c});
        }
        if ( i>26 && i<=52 ) {
            char c = (char) ((i-26) + 64);
            return new String(new char[] {'A', c});
        }
        return Defs.NULL_STRING_NULL;
    }
    
    /** Translates 1,A,2B3 to 1,2,3 as per example from mmCIF:_struct_asym.id
     *Returns Defs.NULL_STRING_NULL for out of range.
     *The map should be prefilled with
     *null,"1","A","2B3"
     */ 
    public static int fromChain(String s, HashMap map) {        
        Object i = map.get(s);
        if ( i == null ) {
            return Defs.NULL_INT;
        }
        return ((Integer) i).intValue();
    }
    
    
    public boolean init(DBMS dbms) {
        if ( ! super.init(dbms) ) {
            General.showCodeBug("Failed to Molecule.super.init()");
            return false;
        }
            

        name = Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_SET_NAME];

        // MAIN RELATION in addition to the ones in gumbo item.
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME], new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_POL_TYPE,                           new Integer(DATA_TYPE_INT));
        DEFAULT_ATTRIBUTES_TYPES.put( Gumbo.DEFAULT_ATTRIBUTE_ASYM_ID,                            new Integer(DATA_TYPE_STRINGNR));

        
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RELATION_ID_COLUMN_NAME ]);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RELATION_ID_COLUMN_NAME ]);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_POL_TYPE);         
        DEFAULT_ATTRIBUTES_ORDER.add( Gumbo.DEFAULT_ATTRIBUTE_ASYM_ID);        
        
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[RELATION_ID_MAIN_RELATION_NAME]});
        DEFAULT_ATTRIBUTE_FKCS_FROM_TO.add( new String[] { Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_COLUMN_NAME],  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[RELATION_ID_MAIN_RELATION_NAME]});
            
        Relation relation = null;
        String relationName = Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[RELATION_ID_MAIN_RELATION_NAME];
        try {
            relation = new Relation(relationName, dbms, this);
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }

        // Create the fkcs without checking that the columns exist yet.
        DEFAULT_ATTRIBUTE_FKCS = ForeignKeyConstrSet.createFromRelation(dbms, DEFAULT_ATTRIBUTE_FKCS_FROM_TO, relationName);        
        if ( ! relation.insertColumnSet( 0, DEFAULT_ATTRIBUTES_TYPES, DEFAULT_ATTRIBUTES_ORDER, 
            DEFAULT_ATTRIBUTE_VALUES, DEFAULT_ATTRIBUTE_FKCS) ) {
            General.showCodeBug("Failed to initialize Molecule with column set given");
            return false;
        }
        addRelation( relation );
        mainRelation = relation;

        // OTHER RELATIONS HERE
        
        return true;
    }            
    
    
    /** Adds a new molecule in the array, filling in all the required properties
     *as available from the parent.
     *If no name is given (null value) then the routine will automatically name
     *it using a number postfixed to the entry name as in molmol. The first
     *molecules under 1000 will be numbered with formatting to allow alphabetical
     *sort (%03i).
     *Returns -1 for failure.
     */
    public int add(String molName, char chainId, int parentId, String asymIdValue) {
        // How many molecules are already in this model?
        /** Something like the sql:
         *SELECT FROM MOLECULE m
         *WHERE m.model_id = parentId
         */
        BitSet moleculesInSameModel = SQLSelect.selectBitSet( dbms, 
            mainRelation,                                                               // Relation
            Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME],    // column
            SQLSelect.OPERATION_TYPE_EQUALS,                                            // operation
            new Integer( parentId ), false);                                            // value
        if ( moleculesInSameModel == null ) {
            General.showWarning("Failed to get the number of molecules already in this model by SQLSelect method.");
            General.showWarning("Not adding molecule.");
            return -1;
        }
        int molCount = moleculesInSameModel.cardinality();
        molCount++;

        // If no name given then simply number them starting at 1.
        if ( molName == null ) {
            Parameters p = new Parameters(); // Printf parameters
            p.add( gumbo.entry.nameList[ gumbo.model.entryId[ parentId ]] );            
            p.add( molCount );
            molName = Format.sprintf("%s%03i", p);                            
            //General.showDebug("Came up with molecule name: [" + molName + "]");
        }
//        General.showDebug("In Molecule.add used molecule name: [" + molName + "]");
        int result = super.add( molName );
        if ( result < 0 ) {
            General.showCodeBug( "Failed to get a new row id for a molecule with name: " + molName);
            return -1;
        }        
        if ( asymIdValue == null ) {
           asymIdValue = toChain(molCount);
//           General.showDebug("asymIdValue: ["+asymIdValue+"]");
        }
//        General.showDebug("asymIdValue(2): ["+asymIdValue+"]");
        modelId[    result ] = parentId;
        entryId[    result ] = gumbo.model.entryId[ parentId ];
        number[     result ] = molCount;
        asymId[     result ] = asymIdValue;
        return result;
    }

    public void setType(int rid, String typeStr ) {
        if ( ! typeEnumMap.containsKey(typeStr)) {
            General.showWarning("For Molecule rid: " + rid + " type: "     + typeStr + 
                    " isn't in enumeration: " + Strings.toString( typeEnum));
            typeStr = "unknown";
            General.showWarning("set to: " + typeStr );            
        }
        type[rid] = typeEnumMap.getInt(typeStr);
    } 
    
    public void setPolType(int rid, String typeStr ) {
        if ( ! polTypeEnumMap.containsKey(typeStr)) {
            General.showWarning("Molecule rid: " + rid + " polymer type: " + typeStr + 
                    " isn't in enumeration: " + Strings.toString( polTypeEnum));
            typeStr = "unknown";
            General.showWarning("set to: " + typeStr );            
        }
        polType[rid] = polTypeEnumMap.getInt(typeStr);
    } 
    
    public BitSet getResSet(int rowId ) {
        BitSet resInSameMol = SQLSelect.selectBitSet( dbms, 
            gumbo.res.mainRelation,                                                     // Relation
            Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[ RelationSet.RELATION_ID_COLUMN_NAME],      // column
            SQLSelect.OPERATION_TYPE_EQUALS,                                            // operation
            new Integer( rowId ), false);                                               // value
        if ( resInSameMol == null ) {
            General.showWarning("Failed to get the residues in a molecule by SQLSelect method.");
            return null;
        }
        return resInSameMol;
    }
    /** The result will be char separated as selected.
     * In case no residues exist for this molecule (perhaps all were deleted) the
     * <CODE>Defs.NULL_STRING_DOT</CODE> string will be returned.
     * @param rowId Molecule identifier
     * @param useFullName Use the full name (3 characters usually) or just 1 char.
     * @param residuesPerLine Number of residues per line or zero for infinite
     *residues per line.
     * @param separator Defs.NULL_CHAR seperator indicates no separator.
     * @param eol End of line string
     * @return String with sequence or <CODE>Defs.NULL_STRING_DOT</CODE> when no
     * sequence is present.
     */
    public String getSequence( int rowId, boolean useFullName, int residuesPerLine, char separator, String eol ) {
        BitSet resInSameMol = getResSet(rowId);
        if ( resInSameMol == null ) {
            General.showWarning("Not able to return the full sequence.");
            return null;
        }
        int resCount = resInSameMol.cardinality(); 
        //General.showDebug("Getting sequence count of: " + resCount);
        if ( resCount == 0 ) {
            return Defs.NULL_STRING_DOT;
        }
        StringBuffer result;
        // For optimized buffer; doesn't really matter.
        if ( useFullName ) {
            if ( Defs.isNull(separator) ) {
                result = new StringBuffer(3*resCount);
            } else {
                result = new StringBuffer(4*resCount);
            }
        } else {
            if ( Defs.isNull(separator) ) {
                result = new StringBuffer(resCount);
            } else {
                result = new StringBuffer(2*resCount);
            }
        }
        String value;
//        char v;
        boolean doSep = true;
        if ( Defs.isNull(separator) ) {
            doSep = false;
        }
        int count = 1;
        for (int RID=resInSameMol.nextSetBit(0); RID >=0; RID=resInSameMol.nextSetBit(RID+1)) {
            value = gumbo.res.mainRelation.getValueString(RID, Relation.DEFAULT_ATTRIBUTE_NAME);
            if ( value == null ) {
                General.showError("Failed to get a residue name in getSequenceFull");
                return null;
            }                   
            if ( useFullName ) {
                result.append( value );
            } else {
                result.append( Biochemistry.residueName2OneChar(value) );
            }
            if ( doSep ) {
                result.append(separator);
            }
            if ( (residuesPerLine!=0) && (count % residuesPerLine) == 0 ) {
                result.append(eol);
            }
            count++;
        }
        if ( doSep ) {
            result.setLength(result.length()-1); // Truncate the last char.
        }
        return result.toString();                
    }

    /** All molecules in bitset given are renumbered within model.
     * See same method in Residue class.*/
    public boolean renumberRows( String columnLabel, BitSet toDo, int startNumber ) {

        int model;
        String columnLabelModelId   = Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[   RelationSet.RELATION_ID_COLUMN_NAME];

        // Find the unique models for the given mols to do.
        // Actually finding molecules that have different models as parent
        BitSet selMolSub = SQLSelect.getDistinct(dbms, mainRelation, columnLabelModelId, toDo);
        if ( selMolSub == null ) {
            General.showCodeBug("Failed to get a distince set of models for the given molecules: " + PrimitiveArray.toString(toDo));
            return false;
        }
        // For each model (through the molecule)
        for (int mol=selMolSub.nextSetBit(0);   mol>=0; mol=selMolSub.nextSetBit(mol+1)) {
            model = modelId[mol];                   
            // Get the molecules for this model that are also in toDo.
            BitSet selMolSub2 = SQLSelect.selectBitSet(dbms, mainRelation, 
                columnLabelModelId, SQLSelect.OPERATION_TYPE_EQUALS, new Integer(model), false);
            if ( selMolSub2 == null ) {
                General.showCodeBug("Failed to get a molecule set in model with rid: " + model);
                return false;
            }
            selMolSub2.and( toDo );
            //General.showDebug("In model with rid: " + model + " found molecules to renumber: " + selMolSub2.cardinality());
            // Renumber the resiudes in R2 starting at 1.
            mainRelation.renumberRows( Relation.DEFAULT_ATTRIBUTE_NUMBER, selMolSub2, 1);
        }
        //General.showDebug("Renumbered molecules at this point");
        return true;
    }            
    
    
    /**     */
    public boolean resetConvenienceVariables() {        
        super.resetConvenienceVariables();
        modelId         = (int[])   mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_MODEL[ RelationSet.RELATION_ID_COLUMN_NAME]);
        entryId         = (int[])   mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_SET_ENTRY[ RelationSet.RELATION_ID_COLUMN_NAME]);        
        polType         = (int[])   mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_POL_TYPE );
        asymId          = (String[])mainRelation.getColumn(  Gumbo.DEFAULT_ATTRIBUTE_ASYM_ID);        
        return true;
    }    
    
    /** 
     * @param rid for given molecule
     * @return xplor segid just like the convert_star2pdb routine does. null when over 36 segis. 
     */
    public String getXplorSEGI(int rid) {
        int beginIndex = number[rid]; // starts at 1.
        if ( beginIndex > 36 ) {
            General.showError("Currently not supporting more than 36 molecules for writing xplor segis");
            return null;
        }
        String result = "   " + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".substring(beginIndex-1, beginIndex);
        return result;
    }
    
    /** Will write xplor-nih python code for that defines enough detail to create a psf file
     * from it with the routine psfGen.seqToPSF
     * Note that since xplor-nih doesn't run on Windows, it doesn't make sense to use 
     * Windows end of lines.
     * @param format See AtomMap TODO:
     */
    public boolean toXplorOrSor(BitSet todo, String fn, String format ) {
        int fileCount = 0;
//        Relation resMain    = res.mainRelation;
//        boolean isXplor = format.startsWith("XPLOR");
//        boolean isDyana = format.startsWith("DYANA");
//        if ( ! isXplor || isDyana ) {
//            General.showCodeBug("Failed to determine format to Xplor or Dyana from string: " + format);
//            return false;
//        }

        for (int rid = todo.nextSetBit(0); rid >= 0; rid=todo.nextSetBit(rid+1)) {
            fileCount++;
            String outputFileName = fn+"_seq";
            outputFileName = InOut.addFileNumberBeforeExtension( fn, fileCount, true, 3 );
            outputFileName = outputFileName + ".py";
            StringBuffer sb = new StringBuffer();
            String segi = getXplorSEGI(rid);
            if ( segi == null ) {
                General.showError("Failed to generate xplor segi");
                return false;
            }
            sb.append("segName='"+segi+"'\n");            
            String sequence = getSequence(rid,true,10,' ', "\n");
            sb.append("seq=\"\"\"\n"+
                    sequence+"\n"
                    +"\"\"\"\n");
            // TODO add deoxy,disulfides, and other patches were needed.
            sb.append("patch_list=[]\n");
            String seqType = polTypeEnumXplor[polType[rid]];
            if ( Chemistry.sequenceIsIonsOnly(sequence) ) {
                seqType="ion";
            }
            sb.append("seqType='"+seqType+"'\n");
            sb.append("seqSplit = seq.split()\n");
            sb.append("#print \"finished reading [\"+`len(seqSplit)`+\"] residue(s) for a [\"+seqType+\"] type segi: [\"+segName+\"]\"\n");            

            
            if ( ! InOut.writeTextToFile(new File(outputFileName), sb.toString().toCharArray(), true, false)) {
                General.showError("Failed to write file: " + outputFileName);
                return false;
            }     
            sequence = getSequence(rid,false,99999,Defs.NULL_CHAR, "\n");
//            General.showDebug("seq: " + sequence);
            int sequenceLength = sequence.length();
            General.showOutput("Written "+Strings.sprintf(seqType, "%10s")+
                    " with "+ Strings.sprintf(sequenceLength, "%3d")+ " residues to :" +outputFileName);
        }
        return true;
    }

    public float getMass(int molrid) {
        float result = 0f;
        BitSet bs = getResSet(molrid);
        for (int i=bs.nextSetBit(0);i>=0;i=bs.nextSetBit(i+1)) {
            float mass = gumbo.res.getMass(i);
            result += mass;            
        }
        return result;
    }
    
    public float getMass(BitSet molSet) {
        float result = 0f;
        for (int i=molSet.nextSetBit(0);i>=0;i=molSet.nextSetBit(i+1)) {
            float mass = getMass(i);
            result += mass;
        }
        return result;
    }

    public BitSet getRes(BitSet molSet) {
        BitSet result = new BitSet();
        for (int molRid=molSet.nextSetBit(0);molRid>=0;molRid=molSet.nextSetBit(molRid+1)) {
            BitSet resSet = getRes(molRid);
            if ( resSet == null ) {
                General.showCodeBug("Failed to getRes(BitSet molSet) for molecule: " + nameList[molRid]);
                return null;
            }
            result.or(resSet);
        }
        return result;
    }     
    
    private BitSet getRes(int molRid) {
        BitSet result = SQLSelect.selectBitSet(dbms, gumbo.res.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[  RELATION_ID_COLUMN_NAME], 
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(molRid), false);
        if ( result == null ) {
            General.showCodeBug("Failed to getRes(int molRid) for molecule: " + nameList[molRid]);
            return null;
        }
        return result;
    }

    public BitSet getAtoms(BitSet molSet) {
        BitSet result = new BitSet();
        for (int molRid=molSet.nextSetBit(0);molRid>=0;molRid=molSet.nextSetBit(molRid+1)) {
            BitSet atomSet = getAtoms(molRid);
            if ( atomSet == null ) {
                General.showCodeBug("Failed to getAtoms(BitSet molSet) for molecule(A): " + nameList[molRid]);
                return null;
            }
            result.or(atomSet);
        }
        return result;
    }     
    
    public BitSet getAtoms(int molRid) {
        BitSet result = SQLSelect.selectBitSet(dbms, gumbo.atom.mainRelation, Gumbo.DEFAULT_ATTRIBUTE_SET_MOL[  RELATION_ID_COLUMN_NAME], 
                SQLSelect.OPERATION_TYPE_EQUALS, new Integer(molRid), false);
        if ( result == null ) {
            General.showCodeBug("Failed to getAtoms(BitSet molSet) for molecule(B): " + nameList[molRid]);
            return null;
        }
        return result;
    }     
}
 
