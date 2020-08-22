/*
 * BlastDefinitions.java
 *
 * Created on February 10, 2003, 2:16 PM
 */
   
package Wattos.Common;

import java.util.regex.*;
import com.braju.format.*;              // printf equivalent
import Wattos.Utils.*;

/**
 *Contains the defs for dealing with sequence comparisons done by Blast.
 * @author Jurgen F. Doreleijers
 */
public class BlastDefinitions {
    
    /** Number of good sequence chars; don't need to be consequetive */
    public static int MINIMUM_SEQUENCE_LENGTH         = 20;

    /** Characters that are allowed as input for query and building db for blast.
     * ATGCU for nucleotides (5 total)
     * A CDEFGHIKLMNPQRST VW Y for proteins (20 total)
     * together:
     * ACDEFGHIKLMNPQRSTUVWY (21 total)
     */
    public static final String  BLAST_ALLOWED_SEQ_CHAR_NOT_REGEX    =   "ABCDEFGHIKLMNPQRSTUVWXYZ";
    public static final String  BLAST_KNOWN_AA_AND_NA_NOT_REGEX     =   "ACDEFGHIKLMNPQRSTUVWY";
    public static final String  BLAST_ALLOWED_SEQ_CHAR              =   "[" + BLAST_ALLOWED_SEQ_CHAR_NOT_REGEX + "]";
    public static final String  BLAST_KNOWN_AA_AND_NA               =   "[" + BLAST_KNOWN_AA_AND_NA_NOT_REGEX + "]";
    public static final String  BLAST_ALLOWED_SEQ_CHARS             =   BLAST_ALLOWED_SEQ_CHAR + "+";
    public static final String  BLAST_KNOWN_AA_AND_NAS              =   BLAST_KNOWN_AA_AND_NA + "+";
    public static final String  BLAST_NOTALLOWED_SEQ_CHARS          =   "([^" + BLAST_ALLOWED_SEQ_CHAR_NOT_REGEX + "])";

    public static Matcher m_blast_allowed_seq_char;
    public static Matcher m_blast_allowed_seq_chars;
    public static Matcher m_blast_notallowed_seq_chars;
    public static Matcher m_blast_known_aa_and_nas;

    static {
        try {
            Pattern p_temp = null;
            p_temp = Pattern.compile(BLAST_ALLOWED_SEQ_CHAR, Pattern.CASE_INSENSITIVE);
            m_blast_allowed_seq_char = p_temp.matcher(""); // default matcher on empty string.

            p_temp = Pattern.compile(BLAST_ALLOWED_SEQ_CHARS, Pattern.CASE_INSENSITIVE);
            m_blast_allowed_seq_chars = p_temp.matcher(""); 

            p_temp = Pattern.compile(BLAST_NOTALLOWED_SEQ_CHARS, Pattern.CASE_INSENSITIVE);
            m_blast_notallowed_seq_chars = p_temp.matcher(""); 

            p_temp = Pattern.compile(BLAST_NOTALLOWED_SEQ_CHARS, Pattern.CASE_INSENSITIVE);
            m_blast_known_aa_and_nas = p_temp.matcher(""); 
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);            
        }
    }

    /** Creates a new instance of BlastDefinitions */
    public BlastDefinitions() {
    }

    /** See doc of method with same name
     */
    static public boolean hasValidSequenceForBlastDb( String sequence, boolean showErrors ) {
        return hasValidSequenceForBlastDb( sequence, showErrors,  MINIMUM_SEQUENCE_LENGTH );
    }
            
   /** Checks size and optionally prints errors.
     */
    static public boolean hasValidSequenceForBlastDb( String sequence, boolean showErrors, int min_length ) {
        
        if ( (sequence == null ) || (sequence.length() < min_length )) {
            if ( showErrors ) {
                if (sequence == null ) {
                    General.showOutput("Sequence is --NULL--" );
                } else {
                    Parameters p = new Parameters(); // for printf
                    p.add( min_length );
                    p.add( sequence.length());
                    p.add( Strings.substringCertainEnd(sequence, 0, 20 ));
                    Format.printf( "Sequence is smaller than: %3d.      Length is: %3d. Sequence: %20s ", p);
                }
            }                    
            return false;
        }

        m_blast_allowed_seq_chars.reset( sequence );
        
        boolean status = m_blast_allowed_seq_chars.matches();
        if ( ! status ) {
            m_blast_notallowed_seq_chars.reset( sequence );
            status = m_blast_notallowed_seq_chars.find();
            if ( status ) {
                if ( showErrors ) {
                    General.showOutput("First invalid character at position: " +
                        m_blast_notallowed_seq_chars.start(1) +
                        " reads: " + m_blast_notallowed_seq_chars.group(1));                
                    //General.showOutput("Sequence reads: [" + sequence + "]");
                }
            } else {
                General.showError("code bug in hasValidSequenceForBlastDb");
                General.showError("sequence reads: [" + sequence + "]");
            }
            return false;
        }
        
        
        int number_good_chars = numberGoodChars( sequence );
        if ( number_good_chars < min_length ) {
            Parameters p = new Parameters(); // for printf
            p.add( min_length );
            p.add( number_good_chars);
            p.add( Strings.substringCertainEnd(sequence, 0, 20 ));            
            Format.printf( "Sequence is smaller than: %3d. Good length is: %3d. Sequence: %20s ", p);
            return false;
        }
        
        return true;
    }
    
    /** Count the number of good characters according to the specifications in
     *the regular expression BLAST_KNOWN_AA_AND_NA_NOT_REGEX
     *Looks like a very complicated counting algorithm;-)
     *For this application certainly the counts per residue are useless.
     */
    static public int numberGoodChars( String sequence ) {
        int total = 0;
        int[] count = new int[256];
        for (int i=0;i<sequence.length();i++) {
            int position = (int) sequence.charAt(i);
            if ( position > 255 ) {
                position = 255;
            }
            count[ position ] ++;
        }
        for (int i=0;i<BLAST_KNOWN_AA_AND_NA_NOT_REGEX.length();i++) {
            total += count[ (int) BLAST_KNOWN_AA_AND_NA_NOT_REGEX.charAt(i) ];
        }
        return total;
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( true ) {
            String sequence = "AEFGHPPPPPPPPPPPXPPPPPPPPP";
            //String sequence = "a";
            General.showOutput("Sequence with length: " + sequence.length() + " reads: " + sequence);
            General.showOutput("numberGoodChars is: [" + numberGoodChars( sequence ) + "]");
            General.showOutput("hasValidSequenceForBlastDb is: [" + hasValidSequenceForBlastDb( sequence, true ) + "]");
        }
    }
    
}
 
