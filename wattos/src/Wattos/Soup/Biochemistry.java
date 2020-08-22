/*
 * Created on September 25, 2003, 4:42 PM
 */

package Wattos.Soup;

import java.util.ArrayList;
import java.util.HashMap;

import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.MapSpecific;
import Wattos.Utils.Objects;
import Wattos.Utils.StringArrayList;

/**
 *Contains definitions such as the genetic code and atom definitions such as 
 *what's considered a backbone atom.
 * @author Jurgen F. Doreleijers
 */
public class Biochemistry {
    
    public static HashMap commonResidueNameAA = null;
    public static HashMap commonResidueNameNA = new HashMap();
    public static HashMap commonResidueNameAA_NA = new HashMap();
    public static StringArrayList ResiduePyrimidine = new StringArrayList();
    public static StringArrayList ResiduePurine = new StringArrayList();
    public static HashMap residueNameAA2OneChar = new HashMap();
    public static HashMap fromDNAtoProteinSequence = new HashMap();
    public static HashMap fromProteinToDNASequence;
    public static HashMap hydrogenBondDonor= new HashMap();
    public static HashMap hydrogenBondAccep= new HashMap();
    public static HashMap backboneNA = new HashMap();
    public static HashMap backboneAA = new HashMap();
    public static HashMap backboneAAandNA = new HashMap();
    /** Donor hydrogen atom name and residue name is listed first.
     * In python:
     * { 'G': { 'C': ( (1,'H41'), (0, 'O6'))}}
     * In Java:
     * "G","C",([1,H41,0,O6],etc.)
     * <PRE>
     * GC: C,N4  - G,O6
     *     G,N1  - C,N3
     *     G,N2  - C,O2
     * AU: A,N6 -  U,O4 (same for AT)
     *     U,N3  - A,N1
     */
    public static HashOfHashes WCBaseHBs = new HashOfHashes();
    
    
    /** The default character for an unknown residue name */
    public static char DEFAULT_RESIDUE_NAME_1CHAR = 'X';

    /** Atom names that are are the last N-terminal / 5' terminal atom;
     * don't include atoms like "H1","H2", "H3",   because they also occur
     * as non-terminal in nucleic acids.
     * */
    public static final String[] N_TERMINAL_ATOM_NAME_LIST = new String[] {
        "H",  "HN", "H'", "H''", "HO'", "HXT", "HO5'", "P", "OP1", "OP2"        
    };

    /** Atom names that are are the last C-terminal / 3' terminal atom */
    public static final String[] C_TERMINAL_ATOM_NAME_LIST = new String[] {
        "HXT", "HO3'", "O"   
    };
    public static HashMap N_TERMINAL_ATOM_NAME_MAP = null;
    public static HashMap C_TERMINAL_ATOM_NAME_MAP = null;
    
    /** The Cys SG bound to another residue SG or other atom (3 states plus mixing)? 
     * 1: free 
     * 2: disulfide
     * 3: other
     * */    
     public static final String[] thiolStateEnumerationAsInStar = new String[] {
        //                                  1 2 3
        "unknown",              
        "not present",                   //1
        "all disulfide bound",           //2  +
        "all free",                      //3+
        "all other bound",               //4    +
        "disulfide and other bound",     //5  + +
        "free and disulfide bound",      //6+ +
        "free and other bound",          //7+   +
        "free disulfide and other bound",//8+ + +
        "not reported", 
        "not available", 
//      "free and bound",                // +  ????
    };
    public static final int THIOL_STATE_UNKNOWN                         = 0;
    public static final int THIOL_STATE_NOT_PRESENT                     = 1;
    public static final int THIOL_STATE_ALL_DISULFIDE_BOUND             = 2;
    public static final int THIOL_STATE_ALL_FREE                        = 3;
    public static final int THIOL_STATE_ALL_OTHER_BOUND                 = 4;
    public static final int THIOL_STATE_DISULFIDE_AND_OTHER_BOUND       = 5;
    public static final int THIOL_STATE_FREE_AND_DISULFIDE_BOUND        = 6;
    public static final int THIOL_STATE_FREE_AND_OTHER_BOUND            = 7;
    public static final int THIOL_STATE_FREE_DISULFIDE_AND_OTHER_BOUND  = 8;
    
    static {    
        N_TERMINAL_ATOM_NAME_MAP = MapSpecific.toHashMap( N_TERMINAL_ATOM_NAME_LIST);
        C_TERMINAL_ATOM_NAME_MAP = MapSpecific.toHashMap( C_TERMINAL_ATOM_NAME_LIST);
        ArrayList gc = new ArrayList();
        gc.add(new String[] {"1","N4", "0","O6"});
        gc.add(new String[] {"0","N1", "1","N3"});
        gc.add(new String[] {"0","N2", "1","O2"});
        WCBaseHBs.put("G","C",gc);
        ArrayList au = new ArrayList();
        au.add(new String[] {"0","N6", "1","O4"});
        au.add(new String[] {"1","N3", "0","N1"});
        WCBaseHBs.put("A","U",au);
        WCBaseHBs.put("A","T",au.clone());
        
        hydrogenBondDonor.put("ALA", new String[] {"H"} );
        hydrogenBondDonor.put("ARG", new String[] {"H"} );
        hydrogenBondDonor.put("ASN", new String[] {"H"} );
        hydrogenBondDonor.put("ASP", new String[] {"H"} );
        hydrogenBondDonor.put("CYS", new String[] {"H"} );
        hydrogenBondDonor.put("GLN", new String[] {"H"} );
        hydrogenBondDonor.put("GLU", new String[] {"H"} );
        hydrogenBondDonor.put("GLY", new String[] {"H"} );
        hydrogenBondDonor.put("HIS", new String[] {"H"} );
        hydrogenBondDonor.put("ILE", new String[] {"H"} );
        hydrogenBondDonor.put("LEU", new String[] {"H"} );
        hydrogenBondDonor.put("LYS", new String[] {"H"} );
        hydrogenBondDonor.put("MET", new String[] {"H"} );
        hydrogenBondDonor.put("PHE", new String[] {"H"} );
        hydrogenBondDonor.put("PRO", new String[] {"H"} );
        hydrogenBondDonor.put("SER", new String[] {"H"} );
        hydrogenBondDonor.put("THR", new String[] {"H"} );
        hydrogenBondDonor.put("TRP", new String[] {"H"} );
        hydrogenBondDonor.put("TYR", new String[] {"H"} );
        hydrogenBondDonor.put("VAL", new String[] {"H"} );
                               
        hydrogenBondAccep.put("ALA", new String[] {"O"} );
        hydrogenBondAccep.put("ARG", new String[] {"O"} );
        hydrogenBondAccep.put("ASN", new String[] {"O"} );
        hydrogenBondAccep.put("ASP", new String[] {"O"} );
        hydrogenBondAccep.put("CYS", new String[] {"O"} );
        hydrogenBondAccep.put("GLN", new String[] {"O"} );
        hydrogenBondAccep.put("GLU", new String[] {"O"} );
        hydrogenBondAccep.put("GLY", new String[] {"O"} );
        hydrogenBondAccep.put("HIS", new String[] {"O"} );
        hydrogenBondAccep.put("ILE", new String[] {"O"} );
        hydrogenBondAccep.put("LEU", new String[] {"O"} );
        hydrogenBondAccep.put("LYS", new String[] {"O"} );
        hydrogenBondAccep.put("MET", new String[] {"O"} );
        hydrogenBondAccep.put("PHE", new String[] {"O"} );
        hydrogenBondAccep.put("PRO", new String[] {"O"} );
        hydrogenBondAccep.put("SER", new String[] {"O"} );
        hydrogenBondAccep.put("THR", new String[] {"O"} );
        hydrogenBondAccep.put("TRP", new String[] {"O"} );
        hydrogenBondAccep.put("TYR", new String[] {"O"} );
        hydrogenBondAccep.put("VAL", new String[] {"O"} );


        residueNameAA2OneChar.put( "ALA", new Character('A'));
        residueNameAA2OneChar.put( "ARG", new Character('R'));
        residueNameAA2OneChar.put( "ASN", new Character('N'));
        residueNameAA2OneChar.put( "ASP", new Character('D'));
        residueNameAA2OneChar.put( "CYS", new Character('C'));
        residueNameAA2OneChar.put( "GLN", new Character('Q'));
        residueNameAA2OneChar.put( "GLU", new Character('E'));
        residueNameAA2OneChar.put( "GLY", new Character('G'));
        residueNameAA2OneChar.put( "HIS", new Character('H'));
        residueNameAA2OneChar.put( "ILE", new Character('I'));
        residueNameAA2OneChar.put( "LEU", new Character('L'));
        residueNameAA2OneChar.put( "LYS", new Character('K'));
        residueNameAA2OneChar.put( "MET", new Character('M'));
        residueNameAA2OneChar.put( "PHE", new Character('F'));
        residueNameAA2OneChar.put( "PRO", new Character('P'));
        residueNameAA2OneChar.put( "SER", new Character('S'));
        residueNameAA2OneChar.put( "THR", new Character('T'));
        residueNameAA2OneChar.put( "TRP", new Character('W'));
        residueNameAA2OneChar.put( "TYR", new Character('Y'));
        residueNameAA2OneChar.put( "VAL", new Character('V'));
        /** From http://molbio.info.nih.gov/molbio/gcode.html */
        fromDNAtoProteinSequence.put( "TTT", new Character('F'));
        fromDNAtoProteinSequence.put( "TTC", new Character('F'));
        fromDNAtoProteinSequence.put( "TTA", new Character('L'));
        fromDNAtoProteinSequence.put( "TTG", new Character('L'));
        fromDNAtoProteinSequence.put( "TCT", new Character('S'));
        fromDNAtoProteinSequence.put( "TCC", new Character('S'));
        fromDNAtoProteinSequence.put( "TCA", new Character('S'));
        fromDNAtoProteinSequence.put( "TCG", new Character('S'));
        fromDNAtoProteinSequence.put( "TAT", new Character('Y'));
        fromDNAtoProteinSequence.put( "TAC", new Character('-'));
        fromDNAtoProteinSequence.put( "TAA", new Character('-'));
        fromDNAtoProteinSequence.put( "TAG", new Character('-'));
        fromDNAtoProteinSequence.put( "TGT", new Character('C'));
        fromDNAtoProteinSequence.put( "TGC", new Character('-'));
        fromDNAtoProteinSequence.put( "TGA", new Character('-'));
        fromDNAtoProteinSequence.put( "TGG", new Character('W'));
        fromDNAtoProteinSequence.put( "CTT", new Character('L'));
        fromDNAtoProteinSequence.put( "CTC", new Character('L'));
        fromDNAtoProteinSequence.put( "CTA", new Character('L'));
        fromDNAtoProteinSequence.put( "CTG", new Character('L'));
        fromDNAtoProteinSequence.put( "CCT", new Character('P'));
        fromDNAtoProteinSequence.put( "CCC", new Character('P'));
        fromDNAtoProteinSequence.put( "CCA", new Character('P'));
        fromDNAtoProteinSequence.put( "CCG", new Character('P'));
        fromDNAtoProteinSequence.put( "CAT", new Character('H'));
        fromDNAtoProteinSequence.put( "CAC", new Character('H'));
        fromDNAtoProteinSequence.put( "CAA", new Character('Q'));
        fromDNAtoProteinSequence.put( "CAG", new Character('Q'));
        fromDNAtoProteinSequence.put( "CGT", new Character('R'));
        fromDNAtoProteinSequence.put( "CGC", new Character('R'));
        fromDNAtoProteinSequence.put( "CGA", new Character('R'));
        fromDNAtoProteinSequence.put( "CGG", new Character('R'));
        fromDNAtoProteinSequence.put( "ATT", new Character('I'));
        fromDNAtoProteinSequence.put( "ATC", new Character('I'));
        fromDNAtoProteinSequence.put( "ATA", new Character('I'));
        fromDNAtoProteinSequence.put( "ATG", new Character('M'));
        fromDNAtoProteinSequence.put( "ACT", new Character('T'));
        fromDNAtoProteinSequence.put( "ACC", new Character('T'));
        fromDNAtoProteinSequence.put( "ACA", new Character('T'));
        fromDNAtoProteinSequence.put( "ACG", new Character('T'));
        fromDNAtoProteinSequence.put( "AAT", new Character('N'));
        fromDNAtoProteinSequence.put( "AAC", new Character('N'));
        fromDNAtoProteinSequence.put( "AAA", new Character('K'));
        fromDNAtoProteinSequence.put( "AAG", new Character('K'));
        fromDNAtoProteinSequence.put( "AGT", new Character('S'));
        fromDNAtoProteinSequence.put( "AGC", new Character('S'));
        fromDNAtoProteinSequence.put( "AGA", new Character('R'));
        fromDNAtoProteinSequence.put( "AGG", new Character('R'));
        fromDNAtoProteinSequence.put( "GTT", new Character('V'));
        fromDNAtoProteinSequence.put( "GTC", new Character('V'));
        fromDNAtoProteinSequence.put( "GTA", new Character('V'));
        fromDNAtoProteinSequence.put( "GTG", new Character('V'));
        fromDNAtoProteinSequence.put( "GCT", new Character('A'));
        fromDNAtoProteinSequence.put( "GCC", new Character('A'));
        fromDNAtoProteinSequence.put( "GCA", new Character('A'));
        fromDNAtoProteinSequence.put( "GCG", new Character('A'));
        fromDNAtoProteinSequence.put( "GAT", new Character('D'));
        fromDNAtoProteinSequence.put( "GAC", new Character('D'));
        fromDNAtoProteinSequence.put( "GAA", new Character('E'));
        fromDNAtoProteinSequence.put( "GAG", new Character('E'));
        fromDNAtoProteinSequence.put( "GGT", new Character('G'));
        fromDNAtoProteinSequence.put( "GGC", new Character('G'));
        fromDNAtoProteinSequence.put( "GGA", new Character('G'));
        fromDNAtoProteinSequence.put( "GGG", new Character('G'));
        
        fromProteinToDNASequence = MapSpecific.invertHashMap( fromDNAtoProteinSequence );
        commonResidueNameAA = (HashMap) Objects.deepCopy(residueNameAA2OneChar);
        commonResidueNameNA.put( "GUA", new Character('G'));
        commonResidueNameNA.put( "CYT", new Character('C'));
        commonResidueNameNA.put( "THY", new Character('T'));
        commonResidueNameNA.put( "ADE", new Character('A'));
        commonResidueNameNA.put( "URA", new Character('U'));
        commonResidueNameNA.put( "URI", new Character('U')); // Xplor standard; not conflicting with PDB?
        commonResidueNameNA.put( "G", new Character('G'));
        commonResidueNameNA.put( "C", new Character('C'));
        commonResidueNameNA.put( "T", new Character('T'));
        commonResidueNameNA.put( "A", new Character('A'));
        commonResidueNameNA.put( "U", new Character('U'));
        commonResidueNameNA.put( "DG", new Character('G')); // Following CCPN standard.
        commonResidueNameNA.put( "DC", new Character('C'));
        commonResidueNameNA.put( "DT", new Character('T'));
        commonResidueNameNA.put( "DA", new Character('A'));
        commonResidueNameNA.put( "DGU", new Character('G')); // Following WHATIF standard. First 3 chars.
        commonResidueNameNA.put( "DCY", new Character('C'));
        commonResidueNameNA.put( "DTH", new Character('T'));
        commonResidueNameNA.put( "DAD", new Character('A'));
        commonResidueNameNA.put( "RGU", new Character('G')); 
        commonResidueNameNA.put( "RCY", new Character('C'));
        commonResidueNameNA.put( "RUR", new Character('U'));
        commonResidueNameNA.put( "RAD", new Character('A'));
        commonResidueNameAA_NA.putAll( commonResidueNameAA );        
        commonResidueNameAA_NA.putAll( commonResidueNameNA );  
        ResiduePurine.add("GUA" );
        ResiduePurine.add("G" );
        ResiduePurine.add("ADE" );
        ResiduePurine.add("A" );
        ResiduePurine.add("DGU" );
        ResiduePurine.add("DAD" );
        ResiduePurine.add("RGU" );
        ResiduePurine.add("RAD" );

        ResiduePyrimidine.add("CYT" );
        ResiduePyrimidine.add("THY" );
        ResiduePyrimidine.add("URA" );
        ResiduePyrimidine.add("URI" );
        ResiduePyrimidine.add("C" );
        ResiduePyrimidine.add("T" );
        ResiduePyrimidine.add("U" );
        ResiduePyrimidine.add("DC" );
        ResiduePyrimidine.add("DT" );
        ResiduePyrimidine.add("DCY" );
        ResiduePyrimidine.add("DTH" );
        ResiduePyrimidine.add("RCY" );
        ResiduePyrimidine.add("RUR" );
        
        backboneAA.put("N", null);
        backboneAA.put("H", null);
        backboneAA.put("CA", null);
        backboneAA.put("HA", null);
        backboneAA.put("HA2", null);
        backboneAA.put("HA3", null);
        backboneAA.put("C", null);
        backboneAA.put("O", null);

        backboneNA.put("C1'", null);
        backboneNA.put("H1'", null);
        backboneNA.put("C2'", null);
        backboneNA.put("H2'", null);
        backboneNA.put("H2''", null);
        backboneNA.put("HO2''", null);
        backboneNA.put("O2'", null);
        backboneNA.put("C3'", null);
        backboneNA.put("H3'", null);
        backboneNA.put("O3'", null);
        backboneNA.put("C4'", null);
        backboneNA.put("H4'", null);
        backboneNA.put("O4'", null);
        backboneNA.put("C5'", null);
        backboneNA.put("H5'", null);
        backboneNA.put("H5''", null);
        backboneNA.put("O5'", null);
        backboneNA.put("OP1", null);
        backboneNA.put("OP2", null);
        backboneNA.put("P", null);
        backboneAAandNA.putAll( backboneAA );
        backboneAAandNA.putAll( backboneNA );               
    }
    
    
    
    /** Creates a new instance of BioChemistry */
    public Biochemistry() {
    }
    
    public static char residueName2OneChar( String in ) {        
        Object o = commonResidueNameAA_NA.get( in );
        if ( o == null ) {
            return DEFAULT_RESIDUE_NAME_1CHAR;
        }        
        return ((Character) o).charValue();
    }    
    
    /** Uses the genetic code for translation. Input should be upper case.
     */
    public static String translateSequence(String sequence, boolean fromDNAtoProtein) {
        if ( fromDNAtoProtein ) {
            return translateSequenceFromDNAtoProtein(sequence);
        }
        return translateSequenceFromProteinToDNA(sequence);            
    }
    
    /** Uses the genetic code for translation. Input should be upper case.
     */
    public static String translateSequenceFromDNAtoProtein(String sequence) {
        int idx = 0;
        int m = sequence.length()/3; // number of triples
        int overhang = sequence.length()%3;
        General.showDebug("Omitting number of nucleotides at the end: " + overhang);
        if ( overhang > 0 ) {
            sequence = sequence.substring(0,sequence.length()-overhang);            
        }
        
        if ( sequence.startsWith("ATG") ) {
            General.showDebug("Omitting start codon: ATG");
            sequence = sequence.substring(3);
        }
        if ( sequence.endsWith("TAA") ||
             sequence.endsWith("TAG") ||
             sequence.endsWith("TGA") 
             ) {
            General.showDebug("Omitting termination codon: " + sequence.substring(sequence.length()-3));
            sequence = sequence.substring(0,sequence.length()-3);
        }

        m = sequence.length()/3; // Might be reduced, for efficiency it is assumed not to happen often.
        
        char[] b = new char[m];
        int i = 0; // runs over triplets
        while ( i < m ) {
            String triplet = sequence.substring(idx,idx+3);
            Character c = (Character) fromDNAtoProteinSequence.get( triplet );
            if ( c == null ) {
                General.showError("Found an invalid codon in the sequence: " +  triplet );  
                return null;
            }
            b[i] = c.charValue();
            if ( b[i] == '-' ) {
                General.showError("Found an invalid codon in the sequence: " +  triplet );  
                return null;
            }
            i++;
            idx += 3;
        }
        return new String( b );
    }
    
    /** Uses the genetic code for translation. Input should be upper case.
     */
    public static String translateSequenceFromProteinToDNA(String sequence) {
        int i = 0; // runs over protein
        int j = 0; // runs over dna
        char[] s = sequence.toCharArray();
        int n = s.length; // number of amino acids in protein
        int m = n*3; // number of bases in dna
        char[] b = new char[m];
        while ( i < n ) {
            String c = (String) fromProteinToDNASequence.get( new Character(s[i] ));
            if ( c == null ) {
                General.showError("Found an invalid amino acid in the sequence: " +  s[i] );  
                return null;
            }
            //General.showOutput("found DNA match: " + c);
            b[j++] = c.charAt(0);
            b[j++] = c.charAt(1);
            b[j++] = c.charAt(2);
            i++;
        }
        return new String( b );
    }
    
    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        General.verbosity = General.verbosityDebug;
        String sequence = "AT";        
        General.showOutput("sequence is: " + sequence );
        if ( true ) {
            //General.showOutput("Map looks like: " + Strings.toString( fromProteinToDNASequence ));
            General.showOutput("sequence translated: " + translateSequence(sequence,false) );
        }            
    }

    /** Returns true only for the following Watson Crick hydrogen bonds:
     * <PRE>
     * Need to use IUPAC atom names and residue names as defined
     * in keys of commonResidueNameNA. Donor and acceptor might be
     * switched.
     * </PRE>
     * 
     * @param atomNameA donor atom name (not the hydrogen atom name itself)
     * @param resNameA
     * @param atomNameB 
     * @param resNameB  acceptor atom name
     * @return true
     */
    public static boolean isWCBond(String atomNameA, String resNameA, String atomNameB, String resNameB) {
//        General.showDebug("Looking for WC HB between: ["+ 
//                resNameA+ ","+atomNameA+"] and ["+ resNameB + ","+atomNameB+"]");
        String resNameASimple;
        String resNameBSimple;
        try { // conversions fail in case of null pointer retrieved.
            resNameASimple = ((Character) commonResidueNameNA.get(resNameA)).toString();
            resNameBSimple = ((Character) commonResidueNameNA.get(resNameB)).toString();
        } catch (RuntimeException e) {
//            General.showDebug("In isWCBond: Conversions fail in case of null pointer retrieved");
            return false;
        }
        ArrayList hbList = (ArrayList) WCBaseHBs.get(resNameASimple,resNameBSimple);
        boolean swapped = false;
        if ( hbList == null ) {
            hbList = (ArrayList) WCBaseHBs.get(resNameBSimple,resNameASimple);
            if ( hbList == null ) {
                return false;
            }
            swapped = true;
        }
//        General.showDebug("In isWCBond: swapped:" + swapped);
        
        // no more than 3 long in WC.
        for (int i=0;i<hbList.size();i++) {            
            String[] bond = (String[]) hbList.get(i);
//            General.showDebug("In isWCBond: looking at:" + i + Strings.toString(bond));
            if ( ! swapped ) {
                if ( (bond[0].equals("0")&&
                      bond[1].equals(atomNameA) &&
                      bond[3].equals(atomNameB)) ||
                     (bond[0].equals("1")&&
                      bond[3].equals(atomNameA) &&
                      bond[1].equals(atomNameB))) {
//                    General.showDebug("In isWCBond: find A");
                    return true;
                }
            } else {
                if ( (bond[0].equals("1")&&
                        bond[1].equals(atomNameA) &&
                        bond[3].equals(atomNameB)) ||
                       (bond[0].equals("0")&&
                        bond[3].equals(atomNameA) &&
                        bond[1].equals(atomNameB))) {
//                      General.showDebug("In isWCBond: find B");
                      return true;
                  }                
            }
        }        
        return false;
    }       
}
