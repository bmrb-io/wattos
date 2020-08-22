/* 
 * PdbFile.java
 *
 * Created on November 15, 2002, 2:53 PM
 */

package Wattos.Soup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Database.RelationSet;
import Wattos.Utils.CharArray;
import Wattos.Utils.General;
import Wattos.Utils.Strings;

 

/** Methods for fast read/write of PDB formatted coordinate files.
 * Reading PDB entry 3EZB (17 Mb) takes 3 seconds and a formatted write to STAR takes 15 seconds.<BR>
 * The conversion of the STAR file to PDB by a gawk script then takes 31 seconds.
 * This task is clearly ready for optimalization if need be.<BR>
 * Specifications: 200,000 atoms in 40 models, 2 chains, 344 residues, and 5,000 atoms.<BR>
 * Machine: Mobile Pentium IV 3 GHz running Windows. Java: VM SUN SDK 1.4.2_04 with 512Mb java available.<BR>
 * Local disks were used.
 * The following formatting is interpreted:
 * <PRE>
 * 6    1 -  6        Record name     ATOM__
 * 5    7 - 11        Integer         serial        Atom serial number.
 * 4   13 - 16        Atom            name          Atom name.
 * 1   17             Character       altLoc        Alternate location indicator.
 * 3   18 - 20        Residue name    resName       Residue name.
 * 1   22             Character       chainID       Chain identifier.
 * 4   23 - 26        Integer         resSeq        Residue sequence number.
 * 1   27             AChar           iCode         Code for insertion of residues.
 * 8   31 - 38        Real(8.3)       x             Orthogonal coordinates for X in
 * 8   39 - 46        Real(8.3)       y             Orthogonal coordinates for Y in
 * 8   47 - 54        Real(8.3)       z             Orthogonal coordinates for Z in
 * 6   55 - 60        Real(6.2)       occupancy     Occupancy.
 * 6   61 - 66        Real(6.2)       tempFactor    Temperature factor (B-factor or equiv.??).
 * 4   73 - 76        LString(4)      segID         Segment identifier, left-justified.
 * 2   77 - 78        LString(2)      element       Element symbol, right-justified.
 * 2   79 - 80        LString(2)      charge        Charge on the atom.
 * </PRE>
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class PdbFile {
    
    public char[]    DEFAULT_OCCUPANCY                       = "  1.00".toCharArray();
    public char[]    DEFAULT_TEMPERATURE_FACTOR              = "  0.00".toCharArray();
    public char[]    DEFAULT_SEGMENT_IDENTIFIER              = "    ".toCharArray();
    public char[]    DEFAULT_ELEMENT_SYMBOL                  = "  ".toCharArray();
    public char[]    DEFAULT_CHARGE                          = "  ".toCharArray();
    public char[]    DEFAULT_ATOM                            = "ATOM  ".toCharArray();
    public char[]    DEFAULT_RESIDUE_TYPE                    = "ALA ".toCharArray();
    public char      DEFAULT_CHAIN_ID                        = 'A';
   
    public int BUFFER_SIZE = 32 * 1024;

    public static final char[] RECORD_ATOM      = "ATOM  ".toCharArray();
    public static final char[] RECORD_HETATM    = "HETATM".toCharArray();
    public static final char[] RECORD_SEQRES    = "SEQRES".toCharArray();
    public static final char[] RECORD_MODEL     = "MODEL ".toCharArray();
    public static final char[] RECORD_TER       = "TER".toCharArray(); // can be 3 chars or more
    
    public String atomNomenclatureFlavor;

    /** Estimated maximum length of one line (atom) in pdb file.
     */
    public static final int LENGTH_MAX_LINE = 81;

    /** BEGIN BLOCK COPY FROM Wattos.Soup.PdbFile */
    public DBMS         dbms;
    public Gumbo        gumbo;    
    public Atom         atom;
    public Residue      res;
    public Molecule     mol;
    public Model        model;
    public Entry        entry;
    public Relation     atomMain;
    public Relation     resMain;
    public Relation     molMain;
    public Relation     modelMain;
    public Relation     entryMain;
    
    int currentAtomId   = -1;
    int currentResId    = -1;
    int currentMolId    = -1;
    int currentModelId  = -1;
    int currentEntryId  = -1;
    /** END BLOCK */
    
    /** Creates new PdbFile */
    public PdbFile( DBMS dbms ) {
        this.dbms = dbms;
        initConvenienceVariables();        
    } 

    /** BEGIN BLOCK FOR SETTING LOCAL CONVENIENCE VARIABLES COPY FROM Wattos.Soup.PdbFile */
    public boolean initConvenienceVariables() {
        atomMain = dbms.getRelation( Gumbo.DEFAULT_ATTRIBUTE_SET_ATOM[RelationSet.RELATION_ID_MAIN_RELATION_NAME] );        
        if ( atomMain == null ) {
            General.showError("failed to find the atom main relation");
            return false;
        }
        atom = (Atom) atomMain.getRelationSetParent();
        if ( atom == null ) {
            General.showError("failed to find atom RelationSet");
            return false;
        }        
        gumbo = (Gumbo) atom.getRelationSoSParent();
        if ( gumbo == null ) {
            General.showError("failed to find the gumbo RelationSoS");
            return false;
        }        
        atom    = gumbo.atom;
        res     = gumbo.res;
        mol     = gumbo.mol;
        model   = gumbo.model;
        entry   = gumbo.entry;
        atomMain   = atom.mainRelation;
        resMain    = res.mainRelation;
        molMain    = mol.mainRelation;
        modelMain  = model.mainRelation;
        entryMain  = entry.mainRelation;
        return true;
    }
    /** END BLOCK */
    
    /** Returns true on success.
<PRE> From the PDB web site:
http://www.rcsb.org/pdb/docs/format/pdbguide2.2/guide2.2_frame.html
Record Format
 
COLUMNS        DATA TYPE       FIELD         DEFINITION
---------------------------------------------------------------------------------
6    1 -  6        Record name     "ATOM  "
5    7 - 11        Integer         serial        Atom serial number.
4   13 - 16        Atom            name          Atom name.
1   17             Character       altLoc        Alternate location indicator.
3   18 - 20        Residue name    resName       Residue name.
1   22             Character       chainID       Chain identifier.
4   23 - 26        Integer         resSeq        Residue sequence number.
1   27             AChar           iCode         Code for insertion of residues.
8   31 - 38        Real(8.3)       x             Orthogonal coordinates for X in
8   39 - 46        Real(8.3)       y             Orthogonal coordinates for Y in
8   47 - 54        Real(8.3)       z             Orthogonal coordinates for Z in
6   55 - 60        Real(6.2)       occupancy     Occupancy.
6   61 - 66        Real(6.2)       tempFactor    Temperature factor.
4   73 - 76        LString(4)      segID         Segment identifier, left-justified.
2   77 - 78        LString(2)      element       Element symbol, right-justified funny....
2   79 - 80        LString(2)      charge        Charge on the atom.
</PRE>
     * @param file_name
     *
     *
        * 3ezb 200,000 atoms in 40 models, 2 chains, 344 residues;5,000 atoms 17 Mb file, Clore
        *Writing takes 4.6 seconds on whelk (single 3 GHz Pentium IV).
        *
     */
    public boolean writeFile( String file_name ) {

        General.showWarning("The code to write a PDB file is VERY INCOMPLETE.\n" + General.eol +
            "Models and residue numbers aren't supported." + General.eol +
            "It's probably useless at this point." + General.eol +
            "Instead, use the script convert_star2pdb on a star file generated by Wattos." + General.eol +
            "The script is located in the scripts directory of the Wattos installation." + General.eol +
            "Alternatively, one can rewrite the script in Java...;-)"
            );

        Relation atomMain = gumbo.atom.mainRelation;        
        if ( atomMain == null ) {
            General.showError("found a null reference for the atom relation.");
            return false;
        }
        // Tested the performance on the buffer size but it doesn't seem to matter at all.
        char[] buf = new char[ LENGTH_MAX_LINE ];
        StringBuffer sb = new StringBuffer( 3 * 8 );
        Arrays.fill( buf, ' '); // zap a line.
        
        File f = new File( file_name );
        General.showOutput("Writing file: " + file_name );
        
        System.arraycopy( DEFAULT_ATOM,                  0, buf, 0, 6 );
        buf[                                            21]                 = DEFAULT_CHAIN_ID;
        System.arraycopy( DEFAULT_OCCUPANCY,             0, buf,54, 6 );
        System.arraycopy( DEFAULT_TEMPERATURE_FACTOR,    0, buf,60, 6 );
        System.arraycopy( DEFAULT_SEGMENT_IDENTIFIER,    0, buf,72, 4 );
        
        System.arraycopy( DEFAULT_CHARGE,                0, buf,78, 2 );
        buf[                                            80]                 = '\n'; // default is unix eol.

        HashMap translatedAtomNames = new HashMap( 1000 );

        char[] atomNamePdbCharArray = new char[4];
        int prevResId = Defs.NULL_INT;
        String residueName;
        try {             
            BufferedWriter w = new BufferedWriter( new FileWriter(f), BUFFER_SIZE );
            int atom_count = 1;
            int residueNumber;
//            String[] atomNameList = (String[]) atomMain.getColumn(  Relation.DEFAULT_ATTRIBUTE_NAME );

            // Use fastest iterator on bitset.
//            int count = 0;
            BitSet used = atomMain.used; // Cache it if the optimizer doesn't do it already.
            String atomNamePdb  = null;
            for (int i=used.nextSetBit(0); i>=0; i=used.nextSetBit(i+1))  {
                if ( ! atom.selected.get(i) ) {
                    General.showDebug("Skipping atom with rid: " + i );
                    continue;
                }
            //for (int i=0; i<atomMain.sizeRows; i++) {
                String atomName     = atom.nameList[i];
                if ( translatedAtomNames.containsKey( atomName ) ) { // Fast lookup from hashmap.
                    atomNamePdb = (String) translatedAtomNames.get(atomName);
                } else {
                    atomNamePdb = translateAtomNameToPdb(atomName);
                    translatedAtomNames.put(atomName, atomNamePdb);
                    //General.showDebug("doing : translateAtomNameToPdb for  : " +  atomName );
                    //General.showDebug("doing : translateAtomNameToPdb found: " +  atomNamePdb );
                }
                currentResId = atom.resId[ i ];
                if ( currentResId != prevResId ) {
                    residueNumber = res.number[currentResId];
                    residueName   = res.nameList[currentResId];
                    //System.arraycopy(residueName.toCharArray(), 0, buf,         17, 4 );
                    CharArray.insertLeftAlign(  residueName,   buf,             17, 4);                    
                    CharArray.insertRightAlign( residueNumber, buf,             22, 4);                    
                }
                if ( atomNamePdb != null ) {
                    atomNamePdbCharArray = atomNamePdb.toCharArray();
                } else {
                    General.showError("No atom name for atom" );
                    Arrays.fill( atomNamePdbCharArray, ' ');
                }
                // Adjust the atom_count for each model
                if ( atom_count > 99999 ) { // quick hack todo: take out.
                    atom_count = 1;
                }
                CharArray.insertRightAlign( atom_count, buf,                6, 5);                
                System.arraycopy( atomNamePdbCharArray, 0, buf,            12, 4);
                
                sb.setLength(0); // empty buffer                       
                // Put the floats into a stringbuffer and then into the buf
                Strings.appendRightAlign(atom.xList[i], sb, 8, 3);
                Strings.appendRightAlign(atom.yList[i], sb, 8, 3);
                Strings.appendRightAlign(atom.zList[i], sb, 8, 3);                
                sb.getChars(0,24,buf,30);
                
                int element_id = atom.elementId[ i ];
                if ( element_id == Defs.NULL_INT ) {
                    buf[76] = ' ';
                    buf[77] = ' ';
                } else {
                    String tmp = Chemistry.ELEMENT_SYMBOLS_UPPER_CASE_RIGHT_JUSTIFIED[ element_id ];
                    buf[76] = tmp.charAt(0);
                    buf[77] = tmp.charAt(1);
                }
                
                // Transfer a line to buffered output stream from buffer.
                w.write(buf);
                atom_count++;
            }
            w.close();
        }
        catch ( FileNotFoundException e ) {
            General.showError("File not found: " +  e.getMessage() );
            return false;
        } catch ( IOException e_io ) {
            General.showError("I/O error: " +  e_io.getMessage() );
            return false;
        }
        
        //General.showOutput("Number of atom written: " +  atom.size );
        return true;
    }
    
    
    /** Speed check
        * 3ezb 200,000 atoms in 40 models, 2 chains, 344 residues;5,000 atoms 17 Mb file, Clore
        *Reading takes 1.6 seconds on whelk (single 3 GHz Pentium IV).
        *
     */
    public boolean readFile(URL url, String atomNomenclatureFlavor ) {        
        this.atomNomenclatureFlavor = atomNomenclatureFlavor;
        PdbFileReader pfr = new PdbFileReader(dbms);
        boolean status = pfr.myReader2( url );
        if ( ! status ) {
            General.showError( "PdbFile readFile is unsuccessfull" );
        }
        return status;
    }

    
    /** Sometimes the atom name ends with a digit in which case the
     *number at the end may go to the end: 
     *"HD21" -> "1HD2"
     *"N"    -> " N  "
     *"1HA"  -> "1HA "
     *"HB3"  -> " HB3"
     *"H5''" -> "'H5'" // for nucleic acids
     *"0"    -> "0   " // which would be invalid 
     *The input may have spaces but may not be the empty string.
     */
    public static String translateAtomNameToPdb( String inputName ) {
        if ( inputName == null || (inputName.length() ==0)) {
            return null;
        }
        String result       = inputName.trim();
        int stringLength    = result.length();
        char lastChar       = result.charAt(stringLength-1);
        char firstChar      = result.charAt(0);

        if ( Character.isDigit( firstChar ) || (firstChar == '\'') ) {            
            // Repetive code for efficiency.
            switch ( stringLength ) {
                case 1:
                    return result + "   ";
                case 2:
                    return result + "  ";
                case 3:
                    return result + " ";
                case 4:
                    return result;
                default: {
                    General.showError("Atom name too long for PDB formatting for name: [" + inputName + "]");
                    return null;
                }
            }
        }
        
        // Not that common
        if ( (stringLength == 4 ) && (Character.isDigit( lastChar ) || (lastChar == '\''))) {
                return lastChar + result.substring(0,3);
        }
        // Repetive code for efficiency.
        switch ( stringLength ) {
            case 1:
                return " " + result + "  ";
            case 2:
                return " " + result + " ";
            case 3:
                return " " + result;
            case 4:
                return result;
            default: {
                General.showError("Atom name too long for PDB formatting for name: [" + inputName + "]");
                return null;
            }
        }
    }
        
    /** Sometimes the atom name starts with a digit which needs to go 
     * to the back: 
     *"1HD2" -> "HD21"
     *"1HB " -> " HB1"
     *" H  " -> " H  "
     *"'H5'" -> "H5''"   // for nucleic acids
     *Only looks at first 4 chars.
     */     
    public static void translateAtomNameFromPdb( char[] buf, int startIdx ) {
        char digit = buf[startIdx];
        // Check if it's a digit.
        if ( Character.isDigit( digit ) || digit == '\'' || digit == '\"' ) {
            // Is there room?
            if ( buf[startIdx+3] == ' ' ) {
                // swap digit with space.
                buf[startIdx]   = ' ';
                buf[startIdx+3] = digit;
            } else {
                // rotate characters to the left.
                buf[startIdx]   = buf[startIdx+1];
                buf[startIdx+1] = buf[startIdx+2];
                buf[startIdx+2] = buf[startIdx+3];
                buf[startIdx+3] = digit;
            }
        }        
    }
     
    /** See namesake method although algorithm is completely different. The
     input should not contain whitespace.
     */
    public static String translateAtomNameFromPdb( String name ) {
        char ch = name.charAt(0);
        if ( Character.isDigit( ch ) || (ch=='\'') || (ch=='\"')) { 
            return name.substring(1,name.length()) + ch;
        }
        return name;        
    }    
    
    /** Sometimes the residue has a trailing sign indicating charge:
     * "HIS+" -> "HIS". The sign will be removed if present.
     */
    public static void translateResidueNameFromPDB( char[] buf, int startIdx ) {
        char last_char = buf[ startIdx + 3 ];
        if ( last_char == '+' || last_char == '-' ) {
             buf[ startIdx + 3 ] = ' ';
        }
    }
    
    /** Sometimes the residue has a trailing sign indicating charge:
     * "HIS+" -> "HIS". The sign will be removed if present.
     */
    public static String translateResidueNameFromPDB( String buf ) {
        int len = buf.length();
        char last_char = buf.charAt(len-1);
        if ( last_char == '+' || last_char == '-' ) {
             return buf.substring(0,len-1);
        }
        return buf;
    }
    
    /** Sometimes the residue has a trailing sign in the charge string:
     * "1-" -> -1.0e+00.
     */
    public static float translateAtomCharge( char[] buf, int startIdx ) {
        int startLineIdx = startIdx - 78;
        int c = buf[startIdx];
        // Is it a digit
        if ( c < 58 && c > 47 ) {
            // translate to an int now; very quick in place.
            c -= 48;
            if ( buf[startIdx+1] == '-' ) {
                c = -c;
            }
            return (float) c;
        }
        if ( !( c == ' ' && buf[startIdx+1] == ' ' )) {
            General.showWarning("Failed to translate the atom charge to a float value for String: [" + 
                new String( buf,startIdx, 2) + "]");            
            General.showWarning("For line: " +
                new String( buf,startLineIdx, 80) + "]"); 
        }
        return Defs.NULL_FLOAT;
    }            
}
