package Wattos.Soup;

import java.io.File;
import java.net.URL;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Strings;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
Converts a coordinate list from NMR-STAR format to PDB format following the 
specifications at: 
- BMRB NMR-STAR data dictionary (2004), 
http://www.bmrb.wisc.edu/dictionary/htmldocs/nmr_star/dictionary.html.
- Atomic Coordinate Entry Format Description,
http://www.rcsb.org/pdb/docs/format/pdbguide2.2/guide2.2_frame.html
        
# Limitiations:
# -1- Assumes all atom info is always on 1 line in the STAR file.
# -2- Only 36 Chains are supported.
# -3- Only 9,999,999 atoms are supported.
# -4- Quotes around values except atom names are not supported.
# -5- Quotes around any value with white space are not supported.
# -6- Won't do element ids.
# -7- Atom names like HN4_2 will be truncated to HN42 in order to fit 4 character
#       space. HN432 will not be allowed.
# No limitation:
# -1- TER and ATOM records are numbered sequentially over file. Just like PDB entries.
# -2- Numeral and single quote at the end of a atom name is supported.
   
*/
public class PdbWriter {
        
    /**
    Conformer_family_coord_set_ID   = $1
    Model_ID                        = $2 # model number
    ID                              = $3 # atom number
    Label_entity_assembly_ID        = $4 # chain id
    Label_entity_ID                 = $5 # 
    Label_comp_index_ID             = $6 # res numb
    Label_comp_ID                   = $7 # res name
    Label_atom_ID                   = $8 # atom name
#    Auth_segment_code               = $9
#    Auth_seq_ID                     = $10
#    Auth_comp_ID                    = $11
#    Auth_atom_ID                    = $12
#  Type_symbol                     = substr(Label_atom_ID,1,1)
    Type_symbol                     = $13
    Cartn_x                         = $14
    Cartn_y                         = $15
    Cartn_z                         = $16
    PDB_extracted_Bfactor_col       = $17
    
    
     * @param inputFileName
     * @param outputFileName
     * @return
     */
    static public boolean processFile( String inputFileName, String outputFileName) {
        General.showDebug("Reading: " + inputFileName);
        General.showDebug("Writing: " + outputFileName);
        // Use a temporary dbms so the data doesn't clobber the regular one.
        DBMS dbmsTemp = new DBMS();
        StarFileReader sfr = new StarFileReader(dbmsTemp);   
        URL url = InOut.getUrlFileFromName(inputFileName);
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("entry.readNmrStarFormattedFile was unsuccessful. Failed to read nmrstar formatted file");
            return false;
        }                     
/**    _Atom_site.Model_ID
       _Atom_site.ID
       _Atom_site.Label_entity_assembly_ID
       _Atom_site.Label_entity_ID
       _Atom_site.Label_comp_index_ID
       _Atom_site.Label_comp_ID
       _Atom_site.Label_atom_ID
       _Atom_site.Type_symbol
       _Atom_site.Cartn_x
       _Atom_site.Cartn_y
       _Atom_site.Cartn_z
       _Atom_site.Occupancy
       _Atom_site.Auth_asym_ID
       _Atom_site.Auth_seq_ID
       _Atom_site.Auth_comp_ID
       _Atom_site.Auth_atom_ID
       _Atom_site.Entry_ID
       _Atom_site.Conformer_family_coord_set_ID
       1    1 1 1  1 DG  O5'  O 74.601 52.638 36.007 0.0 C  1 DG  O5'  1 1  */     
        TagTable tT = sn.getTagTable("_Atom_site.Model_ID", true);
        if ( tT == null ) {
            General.showError("No coordinate section found in STAR file.");
            return false;
        }
        StarDictionary sd = new StarDictionary();
        sd.readCsvFile(null);
        if ( ! sn.translateToNativeTypesByDict(sd, false)) {
            General.showError("Failed to translateToNativeTypesByDict.");
            return false;
        }
        
        int[] Model_ID                      = tT.getColumnInt(      "_Atom_site.Model_ID");
        int[] Label_entity_assembly_ID      = tT.getColumnInt(      "_Atom_site.Label_entity_assembly_ID");
        int[] Label_comp_index_ID           = tT.getColumnInt(      "_Atom_site.Label_comp_index_ID");
        String[] Label_comp_ID              = tT.getColumnString(   "_Atom_site.Label_comp_ID");
        String[] Label_atom_ID              = tT.getColumnString(   "_Atom_site.Label_atom_ID");
        String[] Type_symbol                = tT.getColumnString(   "_Atom_site.Type_symbol");
        float[] Cartn_x                     = tT.getColumnFloat(    "_Atom_site.Cartn_x");
        float[] Cartn_y                     = tT.getColumnFloat(    "_Atom_site.Cartn_y");
        float[] Cartn_z                     = tT.getColumnFloat(    "_Atom_site.Cartn_z");
        float[] PDB_extracted_Bfactor_col   = tT.getColumnFloat(    "_Atom_site.Uncertainty");
        float[] occupancy                   = tT.getColumnFloat(    "_Atom_site.Occupancy");

        if (    Model_ID == null ||
                Label_entity_assembly_ID == null ||
                Label_comp_index_ID == null ||
                Label_comp_ID == null ||
                Label_atom_ID == null ||
                Type_symbol == null ||
                Cartn_x == null ||
                Cartn_y == null ||
                Cartn_z == null ||
                PDB_extracted_Bfactor_col == null ||
                occupancy == null 
                ) {
            General.showError("Not all columns in atom loop found");
            return false;
        }

        /** Regular atom count */
        int atom_count = 0;
        /** Atom count including records for TER. */
        int atom_ter_count = 0;            
        int Model_IDOrg = Defs.NULL_INT;
        char Label_entity_assembly_IDOrg = Defs.NULL_CHAR;
        StringBuffer sb = new StringBuffer();
        Parameters p = new Parameters(); // for printf
        
        /** For the same atom order as in STAR */
        for (int r=tT.used.nextSetBit(0);r>=0;r=tT.used.nextSetBit(r+1)) {
//            String thisLabel_atom_ID            = PdbFile.translateAtomNameFromPdb( Label_atom_ID[r]);
            char thisLabel_entity_assembly_ID   = 'X';
            if ( Label_entity_assembly_ID[r] <= 26 ) {
                thisLabel_entity_assembly_ID   = Molecule.toChain(Label_entity_assembly_ID[r]).charAt(0);
            }
            String thisType_symbol              = translateType_symbol(     Type_symbol[r]);
            String thisLabel_comp_ID            = PdbFile.translateResidueNameFromPDB(     Label_comp_ID[r]);
            
            int    thisModel_ID                 = Model_ID[r];            
            int    thisLabel_comp_index_ID      = Label_comp_index_ID[r];            
            String thisLabel_atom_ID            = Label_atom_ID[r];
            float  thisCartn_x                  = Cartn_x[r];            
            float  thisCartn_y                  = Cartn_y[r];            
            float  thisCartn_z                  = Cartn_z[r];
//            String thisPDB_extracted_Bfactor_col= PDB_extracted_Bfactor_col[r];
            
            // I break for chains and models.  
            if ( thisModel_ID != Model_IDOrg ) {
                if ( Defs.isNull(Model_IDOrg) ) { 
                    sb.append( "REMARK\n" ); 
                    sb.append( "REMARK PDB v3.1 file by Wattos\n" );
                    sb.append( "REMARK\n"  );  
//                    General.showDebug("Found sb: ["+sb.toString()+"]");
                } else {
                    sb.append( Strings.sprintf(atom_ter_count, "TER %7d\n"  )); 
                    atom_ter_count++;
                    sb.append(  "ENDMDL\n"                    );              
                }
                sb.append(  Strings.sprintf(thisModel_ID, "MODEL      %3d\n"));                
                Model_IDOrg = thisModel_ID;
            } else {
                if ( thisLabel_entity_assembly_ID != Label_entity_assembly_IDOrg ) {
                    if ( ! Defs.isNull( Label_entity_assembly_IDOrg )) {
                        // At this point other atoms should have preceded so the count is >=1
                        sb.append( Strings.sprintf( atom_ter_count, "TER %7d\n"));             
                        atom_ter_count++;
                    }
                }
            }
            Label_entity_assembly_IDOrg = thisLabel_entity_assembly_ID;
            /**
        #6    1 -  6        Record name     "ATOM  "
        #5    7 - 11        Integer         serial        Atom serial number.
        #4   13 - 16        Atom            name          Atom name.
        #1   17             Character       altLoc        Alternate location indicator.
        #3   18 - 20        Residue name    resName       Residue name.
        #1   22             Character       chainID       Chain identifier.
        #4   23 - 26        Integer         resSeq        Residue sequence number.
        #1   27             AChar           iCode         Code for insertion of residues.
        #8   31 - 38        Real(8.3)       x             Orthogonal coordinates for X in
        #8   39 - 46        Real(8.3)       y             Orthogonal coordinates for Y in
        #8   47 - 54        Real(8.3)       z             Orthogonal coordinates for Z in
        #6   55 - 60        Real(6.2)       occupancy     Occupancy.
        #6   61 - 66        Real(6.2)       tempFactor    Temperature factor.
        #4   73 - 76        LString(4)      segID         Segment identifier, left-justified.
        #2   77 - 78        LString(2)      element       Element symbol, right-justified funny....
        #2   79 - 80        LString(2)      charge        Charge on the atom.
            */
            atom_ter_count++;
            atom_count++;
            p.add(atom_ter_count);
            sb.append(  Format.sprintf( "ATOM%7d ", p));
            String fmt = " %-3s ";
            if ( thisLabel_atom_ID.length() == 4 ) {
                fmt = "%-4s ";
            }
            p.add(thisLabel_atom_ID);
            sb.append(  Format.sprintf( fmt, p));
            p.add(thisLabel_comp_ID);
            sb.append(  Format.sprintf( "%3s ", p));
            p.add(thisLabel_entity_assembly_ID);
            p.add(thisLabel_comp_index_ID);
            sb.append(  Format.sprintf( "%c%4d    ", p));

            p.add(thisCartn_x);
            p.add(thisCartn_y);
            p.add(thisCartn_z);
            sb.append(  Format.sprintf( "%8.3f%8.3f%8.3f  ", p));

            // Second time this gets written
            p.add(thisLabel_entity_assembly_ID);
            sb.append(  Format.sprintf( "1.00  0.00      %c  ", p));

            p.add(thisType_symbol);
            sb.append(  Format.sprintf( "  %-2s\n", p));            
        }
//        General.showDebug("Writing " + atom_count + " atoms to PDB file: " + outputFileName);
        InOut.writeTextToFile(new File(outputFileName), sb.toString(), true, false);
        return true;
    }
    

    /** Translates . to ""
     * 
     * @return
     */
    static public String translateType_symbol( String xxx ) {
        if ( Defs.isNullString(xxx) ) {
            return Defs.EMPTY_STRING;
        }
        return xxx;
    }
    
}
