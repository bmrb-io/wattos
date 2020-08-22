/*
 * Created on August 27, 2003, 3:04 PM
 */

package Wattos.Soup;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.StringArrayList;

/**
 * (Pro-) chiral definitions
methylene (CH2) 
amide (NH2) on Asn,Gln,Arg, and Nucleic Acids
amino (NH3) on Lys and N-terminus
sec-amino (NH2) on Pro N-terminus 
methyl (CH3) 
isopropyl ((CH3)2) on Val and Leu 
guanidine ((NH2)2) on Arg 
oxygens on phosphates 
oxygens on Asp, Glu, and C-terminus 
Aromatic C and H on Phe and Tyr 
 *
 * @author Jurgen F. Doreleijers
 */
public class AtomNomenLib implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Local resource */
    static final String STR_FILE_LOCATION = "Data/AtomNomenLib.str";
    
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_METHYLENE_CH2                                 = 0;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_AMIDE_NH2_ON_ASN_GLN_ARG_AND_NUCLEIC_ACIDS    = 1;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_AMINO_NH3_ON_LYS_AND_N_TERMINUS               = 2;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_SEC_AMINO_NH2_ON_PRO_N_TERMINUS               = 3;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_METHYL_CH3                                    = 4;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_ISOPROPYL_CH3_2_ON_VAL_AND_LEU                = 5;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_GUANIDINE_NH2_2_ON_ARG                        = 6;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_PHOSPHATES                         = 7;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_OXYGENS_ON_ASP_GLU__AND_C_TERMINUS            = 8;
    public static final int DEFAULT_PROCHIRAL_DEFINITIONS_AROMATIC_C_AND_H_ON_PHE_AND_TYR               = 9;                                         
    

    /** Access pattern will be by residue type and then by stereo type giving an
     * ArrayList of StringArrayList of atom names
     */
    public HashOfHashes recordList = new HashOfHashes();
    
    public AtomNomenLib() {                
    }
        
    public boolean readStarFile( URL url) {
        if ( url == null ) {
            url = getClass().getResource(STR_FILE_LOCATION);
        }
//        if ( true ) return false;
        DBMS dbms_local = new DBMS(); // Create a local copy so anything can be read in.
        StarFileReader sfr = new StarFileReader(dbms_local);        
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        Object o_tmp_1 = sn.get(0);
        if ( !(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;
        
        SaveFrame sf = db.getSaveFrameByCategory( "atom_nomen_lib", true );
        if ( sf == null ) {
            General.showError("Failed to find SaveFrame with category atom_nomen_lib");
            return false;
        }
//        String name = sf.title;
//        General.showDebug("Found saveframe with name: " + name);

        ArrayList result = sf.getTagTableList("_Stereo_type"); 
        if ( result == null || (result.size() != 1) ) {
            if ( result == null ) {
                General.showWarning("Found number of loops: null");
            } else {
                General.showWarning("Found number of loops: " + result.size());
            }
            General.showError("Expected one loop defining the mapping but didn't found one.");
            return false;
        }
        Object o_tmp_3 = result.get(0);

        if ( !(o_tmp_3 instanceof TagTable)) {
            General.showError("Expected object of type TagTable as second node in saveframe but got type: " + o_tmp_3.getClass().getName());
            return false;
        }
        TagTable tT = (TagTable) o_tmp_3;
        tT.convertDataTypeColumn("_Stereo_type", Relation.DATA_TYPE_INT, null);
        
        int[] stereoType        = tT.getColumnInt("_Stereo_type");
        String[] resName        = tT.getColumnString("_Residue_type");
        String[] atomName_1     = tT.getColumnString("_Atom_ID_1");
        String[] atomName_2     = tT.getColumnString("_Atom_ID_2");
        String[] atomName_3     = tT.getColumnString("_Atom_ID_3");
        String[] atomName_4     = tT.getColumnString("_Atom_ID_4");
        String[] atomName_5     = tT.getColumnString("_Atom_ID_5");
        String[] atomName_6     = tT.getColumnString("_Atom_ID_6");
        String[] atomName_7     = tT.getColumnString("_Atom_ID_7");
        String[] atomName_8     = tT.getColumnString("_Atom_ID_8");
        String[] atomName_9     = tT.getColumnString("_Atom_ID_9");
        String[] atomName_10    = tT.getColumnString("_Atom_ID_10");
        String[] atomName_11    = tT.getColumnString("_Atom_ID_11");
        
        if (    stereoType     == null || 
                resName        == null ||
                atomName_1     == null ||
                atomName_2     == null ||
                atomName_3     == null ||
                atomName_4     == null ||
                atomName_5     == null ||
                atomName_6     == null ||
                atomName_7     == null ||
                atomName_8     == null ||
                atomName_9     == null ||
                atomName_10    == null ||
                atomName_11    == null                 ) {
            General.showError("Failed to find String[] columns in tagtable as expected.");
            return false;
        }
        String[][] atomNames = {
            atomName_1,
            atomName_2,
            atomName_3,
            atomName_4,
            atomName_5,
            atomName_6,
            atomName_7,
            atomName_8,
            atomName_9,
            atomName_10,
            atomName_11};

        for (int i=tT.used.nextSetBit(0); i>=0; i=tT.used.nextSetBit(i+1)) {
            StringArrayList atomList = new StringArrayList();            
            for ( int a=0;a<atomNames.length;a++ ) {
//                General.showDebug("Processing atom: " + atomNames[a][i] );
                if ( Defs.isNullString(atomNames[a][i])) {
                    break;
                }
                atomList.add( atomNames[a][i] );
            }
            // Next lines are all you need to know the data structure
            ArrayList listOfSameStereoTypeAndSameResidueType = new ArrayList();
            Object tmpObj = recordList.get( resName[i], new Integer( stereoType[i] ) );
            if ( tmpObj == null ) {
                recordList.put( resName[i], new Integer( stereoType[i] ), listOfSameStereoTypeAndSameResidueType);
            } else {
                listOfSameStereoTypeAndSameResidueType = (ArrayList) tmpObj;                
            }
            listOfSameStereoTypeAndSameResidueType.add(atomList);
        }
        return true;
    }
    
    public String toString() {
        return recordList.toString();
    }
}
