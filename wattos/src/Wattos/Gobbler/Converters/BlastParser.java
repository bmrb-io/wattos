/* * BlastParser.java
 *
 * Created on February 18, 2003, 4:32 PM
 */

package Wattos.Gobbler.Converters;

import java.io.*;
import java.util.regex.*;

import Wattos.Gobbler.*;
import Wattos.Common.*;
import Wattos.Utils.*;

/**
 * A parser like that in biojava.
 Example input:
<PRE> 
BLASTP 2.2.5 [Nov-16-2002]


Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaffer, 
Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman (1997), 
"Gapped BLAST and PSI-BLAST: a new generation of protein database search
programs",  Nucleic Acids Res. 25:3389-3402.

Query= BSGCAIR30310|2002-07-24aaaaaaaaaaaaaaaaaaaaaaaaaaa
aaaaa
         (297 letters)

Database: pdb_seqres.txt 
           44,233 sequences; 9,419,815 total letters

Searching..................................................done

                                                                 Score    E
Sequences producing significant alignments:                      (bits) Value

1mkk_B mol:protein length:96     Vascular Endothelial Growth Fac...    26   3.2  

>ref|NP_125847.1| ABC transporter, ATP-binding protein [Pyrococcus abyssi]
 pir||G75203 abc transporter, ATP-binding protein PAB0103 - Pyrococcus abyssi
           (strain Orsay)
 emb|CAB49078.1| ABC transporter, ATP-binding protein, substrate unknown [Pyrococcus
           abyssi]
          Length = 344

 Score =  248 bits (633), Expect = 1e-64
 Identities = 140/331 (42%), Positives = 206/331 (61%), Gaps = 42/331 (12%)

Query: 1   MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN 59
           ML+V ++SK +K+FKL+++SF++ +E + +ILGPSGAGK+VL++ IAGI++PD GRIILN
Sbjct: 1   MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN 60
...
Query: 261 KKVFVKYNGYIIKAFTERD--LNIGDNVGLE 289
            +V +K  G  +KAF  R   + +G + G E
Sbjct: 301 VRVNLKIGGITLKAFITRSSLIELGISEGRE 331


  Database: pdb_seqres.txt
    Posted date:  Dec 17, 2002  7:02 PM
  Number of letters in database: 9,419,815
  Number of sequences in database:  44,233
  
Lambda     K      H
   0.316    0.131    0.367 

Gapped
Lambda     K      H
   0.267   0.0410    0.140 


Matrix: BLOSUM62
Gap Penalties: Existence: 11, Extension: 1
Number of Hits to DB: 4,529,460
Number of Sequences: 44233
Number of extensions: 162094
Number of successful extensions: 284
Number of sequences better than 10.0: 23
Number of HSP's better than 10.0 without gapping: 5
Number of HSP's successfully gapped in prelim test: 18
Number of HSP's that attempted gapping in prelim test: 275
Number of HSP's gapped (non-prelim): 27
length of query: 274
length of database: 9,419,815
effective HSP length: 94
effective length of query: 180
effective length of database: 5,261,913
effective search space: 947144340
effective search space used: 947144340
T: 11
A: 40
X1: 16 ( 7.3 bits)
X2: 38 (14.6 bits)
X3: 64 (24.7 bits)
S1: 41 (21.6 bits)
S2: 57 (26.6 bits)
<PRE>
 * @author Jurgen F. Doreleijers
 */
public class BlastParser {

    /** Use a large buffer size so we wait less for reading/writing
    This will be especially usefull for larger files on both input/output.
     */
    static int FILE_OUT_BUFFER_SIZE = 10 * 1024 * 1024;
    static int FILE_IN_BUFFER_SIZE  = 10 * 1024 * 1024;
    // Reduce output after finished debugging.
    //static final boolean DEBUGGING = false;
    /** Matching example: "BLASTP 2.1.1 [Nov-16-2002]";  
     *Or BLASTN 2.2.6 [Apr-09-2003      
     *The first character has to be a regular character, i.e. not a special character
     *because of usage in containsIndicationNewQuery     */    
    static private final String OUTPUT_START_OF_RUN       = "^BLAST. +[\\w\\.]* \\[\\w+-\\w+-\\w+\\]";        
    /** Matching:                                     "***** No hits found ******";     */
//    static private final String OUTPUT_NO_HITS            = "\\*+ No hits found \\*+";
    static private final String EOL_TOKEN                 = "EOL_TOKEN";
    static private final String EMPTY_STRING              = "";
    /** Picks up 297 from line:
            (297 letters)            */
    static private final String regexp_query_length   =  "^ +\\(([\\d\\,]+) letters\\)\\s*";        
    /** Picks up 344 from line:
          Length = 344               */
    static private final String regexp_subject_length =  "^ +Length = *(\\d+)\\s*";        

 /** matches and picks up two numbers :
 Score =  248 bits (633), Expect = 1e-64
  */
    static private final String regexp_score_expect     =  "^ Score = *(\\d.*) +bits \\(\\d+\\), Expect =(.+)";        
 /** matches and picks up four numbers.
 Identities = 140/331 (42%), Positives = 206/331 (61%), Gaps = 42/331 (12%)
  */
    static private final String regexp_iden_pos_gap =  
       "^ Identities = " + "*(\\d+)/(\\d+).+" + 
        " Positives = "  + "*(\\d+)/.+" +
        " Gaps = "       + "*(\\d+)/.+";
 /** matches and picks up three numbers.
 Identities = 140/331 (42%), Positives = 206/331 (61%)
  */
    static private final String regexp_iden_pos =  
       "^ Identities = " + "*(\\d+)/(\\d+).+" + 
        " Positives = "  + "*(\\d+)/.+";
    /** for Nucleic acids
     */
    static private final String regexp_iden =  
       "^ Identities = " + "*(\\d+)/(\\d+).+"; 
    
    /** for Nucleic acids
     */
    static private final String regexp_strand =  
       "^ Strand = " + "*(Plus|Minus) / (Plus|Minus).*"; 
    
    static private final String regexp_query_sequence =  "^Query: *(\\d+) *([\\w-]+) *(\\d+)\\s*";
    static private final String regexp_sbjct_sequence =  "^Sbjct: *(\\d+) *([\\w-]+) *(\\d+)\\s*";
    
    static private final Pattern p_OUTPUT_START_OF_RUN;
//    static private final Pattern p_OUTPUT_NO_HITS;
    static private final Pattern p_query_sequence_length;
    static private final Pattern p_subject_sequence_length;
    static private final Pattern p_score_expect;
    static private final Pattern p_iden_pos_gap;
    static private final Pattern p_iden_pos;
    static private final Pattern p_iden;
    static private final Pattern p_strand;
    static private final Pattern p_query_sequence;
    static private final Pattern p_sbjct_sequence;

    static private final Matcher m_OUTPUT_START_OF_RUN;
//    static private final Matcher m_OUTPUT_NO_HITS;
    static private final Matcher m_query_sequence_length;
    static private final Matcher m_subject_sequence_length;
    static private final Matcher m_score_expect;
    static private final Matcher m_iden_pos_gap;
    static private final Matcher m_iden_pos;
    static private final Matcher m_iden;
    static private final Matcher m_strand;
    static private final Matcher m_query_sequence;
    static private final Matcher m_sbjct_sequence;
               
//    static private final String EOL_STRING = General.eol;


    static {
        p_OUTPUT_START_OF_RUN    = Pattern.compile(OUTPUT_START_OF_RUN);
//        p_OUTPUT_NO_HITS         = Pattern.compile(OUTPUT_NO_HITS);
        p_query_sequence_length  = Pattern.compile(regexp_query_length);            
        p_subject_sequence_length= Pattern.compile(regexp_subject_length);            
        p_score_expect           = Pattern.compile(regexp_score_expect);            
        p_iden_pos_gap           = Pattern.compile(regexp_iden_pos_gap);            
        p_iden_pos               = Pattern.compile(regexp_iden_pos);            
        p_iden                   = Pattern.compile(regexp_iden);            
        p_strand                 = Pattern.compile(regexp_strand);            
        p_query_sequence         = Pattern.compile(regexp_query_sequence);            
        p_sbjct_sequence         = Pattern.compile(regexp_sbjct_sequence);            

        m_OUTPUT_START_OF_RUN    = p_OUTPUT_START_OF_RUN.matcher(               EMPTY_STRING);
//        m_OUTPUT_NO_HITS         = p_OUTPUT_NO_HITS.matcher(                    EMPTY_STRING);
        m_query_sequence_length  = p_query_sequence_length.matcher(             EMPTY_STRING);
        m_subject_sequence_length= p_subject_sequence_length.matcher(           EMPTY_STRING);
        m_score_expect           = p_score_expect.matcher(                      EMPTY_STRING);
        m_iden_pos_gap           = p_iden_pos_gap.matcher(                      EMPTY_STRING);
        m_iden_pos               = p_iden_pos.matcher(                          EMPTY_STRING);
        m_iden                   = p_iden.matcher(                              EMPTY_STRING);
        m_strand                 = p_strand.matcher(                            EMPTY_STRING);
        m_query_sequence         = p_query_sequence.matcher(                    EMPTY_STRING);
        m_sbjct_sequence         = p_sbjct_sequence.matcher(                    EMPTY_STRING);
    }
    
    /** Creates a new instance of BlastParser */
    public BlastParser() {
        General.showError("Don't instantiate this class");        
    }

/* Returns the next line in the buffer or null if an error occured or read
 *the eof.
 Example input of what's read in this part:
<PRE>
BLASTP 2.2.5 [Nov-16-2002]


Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaffer, 
Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman (1997), 
"Gapped BLAST and PSI-BLAST: a new generation of protein database search
programs",  Nucleic Acids Res. 25:3389-3402.

Query= BSGCAIR30310|2002-07-24aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
aaaaa
         (297 letters)

Database: pdb_seqres.txt 
           44,233 sequences; 9,419,815 total letters

Searching..................................................done
</PRE>
 *
 *<BR>
 *Or for BLASTN:
<PRE>
BLASTN 2.2.6 [Apr-09-2003]


Reference: Altschul, Stephen F., Thomas L. Madden, Alejandro A. Schaffer, 
Jinghui Zhang, Zheng Zhang, Webb Miller, and David J. Lipman (1997), 
"Gapped BLAST and PSI-BLAST: a new generation of protein database search
programs",  Nucleic Acids Res. 25:3389-3402.

Query= GO.6065|3254|At1g02280
         (894 letters)

Database: at_introns_db 
           29,993 sequences; 67,661,507 total letters

Searching..................................................done
</PRE>
 *
 */
    public static String parseBlastListHeader( String line, LineNumberReader inputReader, BlastMatchList blastmatch_list ) {
        
        try {
            // Some checking never hurts.
            m_OUTPUT_START_OF_RUN.reset( line );
            if ( ! m_OUTPUT_START_OF_RUN.matches() ) {
                General.showError("expected header like: BLASTP 2.2.5 [Nov-16-2002] but found: ["+
                    line + "] or");
                General.showError("expected header like: BLASTN 2.2.6 [Apr-09-2003] but found: ["+
                    line + "]");
                return null;
            }
            
            // Position ourselves to query id list
            while ( (line != null ) && (! line.startsWith("Query= ")) ) {
                line = inputReader.readLine();
                //General.showDebug("Read parseBlastListHeader line: [" + line + "]");                
            }
            if ( line == null ) {
                General.showError("didn't find line starting with: \"Query= \"");
                return null;
            }
            
            // Parse the query id list, it continues if the line contains 10 spaces at the beginning
            String query_id_list_str = line.substring(7).trim();
            int count = 0;
            while ( (line != null ) && (line.charAt(0) != ' ')) {
                if ( count != 0 ) { 
                    query_id_list_str += line.trim();
                }
                line = inputReader.readLine();
                //General.showDebug("Read query id parseBlastListHeader line: [" + line + "]");
                
                count++;
            }
            if ( line == null ) {
                General.showError("while parsing query id list fell through.");
                return null;
            }
            //General.showDebug("parsing query id list from string: [" + query_id_list_str + "]");
            
            blastmatch_list.query_orf_id_list = FastaDefinitions.parseBlastId( query_id_list_str );
            if ( blastmatch_list.query_orf_id_list == null ) {
                General.showError("while parsing query id list with FastaDefinitions.parseBlastId.");
                return null;
            }
            //General.showDebug("found query orf id: [" + blastmatch_list.query_orf_id_list.toString() + "]");
            
            // Parse the count of characters in query 
            //General.showDebug("parsing query length from string: [" + line + "]");
            blastmatch_list.query_orf_length = getCountLettersQuery( line );
            if ( blastmatch_list.query_orf_length == 0 ) {
                General.showError("while parsing count for query orf length.");
                return null;
            }
            //General.showDebug("successfully parsed query length.");
            // Skip rest until AFTER the line with Searching so the line variable 
            while ( (line != null ) && (! line.startsWith("Searching")) ) {
                line = inputReader.readLine();
                //General.showDebug("Read parseBlastListHeader line: [" + line + "]");
            }
            if ( line == null ) {
                General.showError("didn't find line starting with: \"Searching\"");
                return null;
            }
            line = getNextNotEmptyLine( inputReader );            
            if ( line == null ) {
                General.showError("found empty line after line starting with: \"Searching\"");
                return null;
            }
        } catch ( IOException e ) {
            General.showThrowable(e);
            return null;
        }                    
            
        //General.showDebug("Successfully parsed header.");
        return line;
    }
    
    /** Return zero on error */
    static int getCountLettersQuery( String line ) {
        m_query_sequence_length.reset( line );
        if ( ! m_query_sequence_length.matches() ) {
            General.showError("failed to find the query sequence count with pattern: [" + regexp_query_length + "]");
            General.showError("line is:[" + line + "]");
            return 0;
        }
        String withCommaPerhaps = m_query_sequence_length.group(1);
        String withoutComma = withCommaPerhaps.replaceAll(",", "");
        int result = Integer.parseInt( withoutComma );
        return result;
    }

    /** Return zero on error.
     parses:<BR>
          Length = 135
     */
    static int getCountLettersSubject( String line ) {
        m_subject_sequence_length.reset( line );
        if ( ! m_subject_sequence_length.matches() ) {
            General.showError("failed to find the subject sequence count with pattern: [" + regexp_subject_length + "]");
            General.showError("line is:[" + line + "]");
            return 0;
        }
        int result = Integer.parseInt( m_subject_sequence_length.group(1) );
        //General.showDebug("m_subject_sequence_length: " + result);
        return result;
    }

    /**
     *Parse away:
<PRE>     
>ref|NP_125847.1| ABC transporter, ATP-binding protein [Pyrococcus abyssi]
 pir||G75203 abc transporter, ATP-binding protein PAB0103 - Pyrococcus abyssi
           (strain Orsay)
 emb|CAB49078.1| ABC transporter, ATP-binding protein, substrate unknown [Pyrococcus
           abyssi]
          Length = 344

 Score =  248 bits (633), Expect = 1e-64
 Identities = 140/331 (42%), Positives = 206/331 (61%), Gaps = 42/331 (12%)

Query: 1   MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN 59
           ML+V ++SK +K+FKL+++SF++ +E + +ILGPSGAGK+VL++ IAGI++PD GRIILN
Sbjct: 1   MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN 60

Query: 261 KKVFVKYNGYIIKAFTERD--LNIGDNVGLE 289
            +V +K  G  +KAF  R   + +G + G E
Sbjct: 301 VRVNLKIGGITLKAFITRSSLIELGISEGRE 331
</PRE>     

     
     <BR> Or for same subject but different match,
        note second subject isn't listed fully again
     *      and regions may even be the same! So stupid.
<PRE>     
>At1g02280 68414.t00146 GTP-binding protein (TOC33) identical to
            protein GI:11557972
          Length = 1951

 Score =  529 bits (267), Expect = e-149
 Identities = 267/267 (100%)
 Strand = Plus / Plus                                                                       
                                       
Query: 569  ctaagatgcgaaaacaagagtttgagg 595
            |||||||||||||||||||||||||||
Sbjct: 1166 ctaagatgcgaaaacaagagtttgagg 1192



 Score =  325 bits (164), Expect = 6e-88
 Identities = 164/164 (100%)
 Strand = Plus / Plus

                                                                        
Query: 662  aggctcttccaaatggtgaagcgtggatcccgaacttggttaaggcgataactgatgtag 721
            ||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||
Sbjct: 1441 aggctcttccaaatggtgaagcgtggatcccgaacttggttaaggcgataactgatgtag 1500

                                                                        

</PRE>     
*/
    public static String parseBlastHit( String line, LineNumberReader inputReader, BlastMatchList blastmatch_list ) {
            
        BlastMatch blastmatch = new BlastMatch();        
        
        //General.showDebug("Starting parseBlastHit with line: [" + line + "]");
        
        try {            
            // Parse the subject id list, it continues if the line contains 10 spaces at the beginning
            String subject_id_list_str = line.substring(1).trim();
            while ( line != null ) {
                line = inputReader.readLine();
                //General.showDebug("Read parseBlastHit line: [" + line + "]");
                
                if ( line == null ) {
                    General.showError("while parsing hit in method parseBlastHit");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;                        
                }
                // -TEN- spaces
                if ( line.startsWith("          Length =") ) {
                    break;
                }
                /** Two possibilities then: this orfid continues or a new orfid starts.
                 *All orf ids are collected in a list.
                 */                            
                // -NINE- spaces
                if ( line.startsWith("         ") ) {
                    // The orf id continues
                    subject_id_list_str += line.trim();
                } else if ( line.startsWith(" ") ) {
                    // A new orf id is started
                    // First parse and save the previous orf id                    
                    //General.showDebug("parsing one subject id from: [" + subject_id_list_str + "]");
                    OrfIdList orfIdList = FastaDefinitions.parseBlastId( subject_id_list_str );                    
                    if ( orfIdList == null ) {
                        General.showError("while parsing one subject id from string: [" + subject_id_list_str + "]");
                        General.showError("now on line  : " + inputReader.getLineNumber());
                        return null;                        
                    }
                    OrfId orfId = (OrfId) orfIdList.orfIdList.get(0);
                    //General.showDebug("found subject id: [" + orfId.toString() + "]");
                    blastmatch.subject_orf_id_list.orfIdList.add( orfId );
                    // Start the new one.
                    subject_id_list_str = line.trim();
                } else {
                    General.showError("while parsing subject id list found line: [" + line + "]");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;
                }
            }
            
            
            // do the last orf id
            //General.showDebug("parsing last subject id from: [" + subject_id_list_str + "]");
            OrfIdList orfIdList = FastaDefinitions.parseBlastId( subject_id_list_str );                    
            if ( orfIdList == null ) {
                General.showError("while parsing one subject id from string: [" + subject_id_list_str + "]");
                General.showError("now on line  : " + inputReader.getLineNumber());
                return null;                        
            }
            OrfId orfId = (OrfId) orfIdList.orfIdList.get(0);
            //General.showDebug("found subject id: [" + orfId.toString() + "]");
            blastmatch.subject_orf_id_list.orfIdList.add( orfId );                                    
            if ( line == null ) {
                General.showError("while parsing subject id list fell through.");
                General.showError("now on line  : " + inputReader.getLineNumber());
                return null;
            }            
                       
            // Parse the count of characters in subject
            blastmatch.subject_orf_length = getCountLettersSubject( line );
            if ( blastmatch.subject_orf_length == 0 ) {
                General.showError("while parsing count for subject orf length.");
                General.showError("now on line  : " + inputReader.getLineNumber());
                return null;
            }
            
            // Parse scores
            line = getNextNotEmptyLine( inputReader );            
            if ( line == null ) {
                General.showError("found eof or error after line starting with: \"Searching\"");
                General.showError("now on line  : " + inputReader.getLineNumber());
                return null;
            }
            boolean firstPass       = true;
            boolean foundAnotherHit = true; // Happens for nucleotide matches; the subject isnt' repeated for them...
            while ( firstPass || foundAnotherHit ) {
                BlastMatch blastmatch_org = blastmatch;
                blastmatch = new BlastMatch();
                blastmatch.subject_orf_id_list = (OrfIdList) blastmatch_org.subject_orf_id_list.clone();
                blastmatch.subject_orf_length  =             blastmatch_org.subject_orf_length;
                m_score_expect.reset( line ); 
                if ( ! m_score_expect.matches() ) {
                    General.showError("didn't match the score line with regexp: [" + regexp_score_expect + "]");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    General.showError("line reads: [" + line + "]");
                    return null;
                }
                blastmatch.bit_score            = Float.parseFloat(     m_score_expect.group( 1 ) );
                String exp_value                                      = m_score_expect.group( 2 ).trim();
                try {
                    blastmatch.expectation_value = Double.parseDouble( exp_value );
                // catch strings like "e-147" 
                } catch ( NumberFormatException e ) {
                    try {
                        String full_value = "1.0" + exp_value;                        
                        blastmatch.expectation_value = Double.parseDouble(full_value );
                    } catch ( Throwable t ) {
                        General.showError("Expectation value assumed zero for: " + "1.0" + exp_value);
                        General.showError("now on line  : " + inputReader.getLineNumber());
                        blastmatch.expectation_value = 0.0d;
                    }
                }

                // Parse counts
                line = inputReader.readLine();
                ////General.showDebug("Read parseBlastHit line: [" + line + "]");
                if ( line == null ) {
                    General.showError("found eof or error looking for counts.");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;
                }
                m_iden_pos_gap.reset( line );
                m_iden_pos.reset( line );
                m_iden.reset( line );
                boolean matches_all = m_iden_pos_gap.matches();
                boolean matches_two = m_iden_pos.matches();
                boolean matches_one = m_iden.matches();
                if ( (! matches_all) && (! matches_two) && (! matches_one) ) {
                    General.showError("didn't match any of the regexp: ");
                    General.showError("["+ regexp_iden_pos_gap  + "]");
                    General.showError("["+ regexp_iden_pos      + "]");
                    General.showError("["+ regexp_iden          + "]");
                    General.showError("for line: [" + line + "]");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;
                }
                if ( matches_all ) {
                    //General.showDebug("Matches all");
                    blastmatch.number_identities    = Integer.parseInt(     m_iden_pos_gap.group( 1 ) );
                    blastmatch.match_size           = Integer.parseInt(     m_iden_pos_gap.group( 2 ) );
                    blastmatch.number_positives     = Integer.parseInt(     m_iden_pos_gap.group( 3 ) );
                    blastmatch.number_gaps          = Integer.parseInt(     m_iden_pos_gap.group( 4 ) );
                } else if ( matches_two ) {
                    blastmatch.number_identities    = Integer.parseInt(     m_iden_pos.group( 1 ) );
                    blastmatch.match_size           = Integer.parseInt(     m_iden_pos.group( 2 ) );
                    blastmatch.number_positives     = Integer.parseInt(     m_iden_pos.group( 3 ) );
                    blastmatch.number_gaps          = 0;
                } else {
                    blastmatch.number_identities    = Integer.parseInt(     m_iden.group( 1 ) );
                    blastmatch.match_size           = Integer.parseInt(     m_iden.group( 2 ) );
                    blastmatch.number_positives     = 0;
                    blastmatch.number_gaps          = 0;                
                }
    /** parse:
     Strand = Plus / Plus */
                line = getNextNotEmptyLine( inputReader );            
                m_strand.reset( line );
                boolean matches_strand = m_strand.matches();
                //General.showDebug("matches_strand: " + matches_strand);
                if ( matches_strand ) {
                    String is_positive_strand = m_strand.group( 1 );
                    //General.showDebug("found matching: [" + is_positive_strand +"]");
                    if ( is_positive_strand.equalsIgnoreCase("Plus") ) {
                        blastmatch.query_is_positive_strand      = true;
                    } else if (is_positive_strand.equalsIgnoreCase("Minus") ) {
                        blastmatch.query_is_positive_strand      = false;
                    } else {
                        General.showError("Failed to parse the query strandness from line: " + line );
                        General.showError("found matching: [" + is_positive_strand +"]");
                        return null;
                    }
                    is_positive_strand = m_strand.group( 2 );
                    //General.showDebug("found matching: [" + is_positive_strand +"]");
                    if ( is_positive_strand.equalsIgnoreCase("Plus") ) {
                        blastmatch.subject_is_positive_strand      = true;
                    } else if (is_positive_strand.equalsIgnoreCase("Minus") ) {
                        blastmatch.subject_is_positive_strand      = false;
                    } else {
                        General.showError("Failed to parse the subject strandness from line: " + line );
                        General.showError("found matching: [" + is_positive_strand +"]");
                        return null;
                    }
                    line = getNextNotEmptyLine( inputReader );            // get next line if a match occured
                }

                //General.showDebug("Successfully parsed subject scores for hit.");


    /** parse:
    Query: 1   MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN 59
               ML+V ++SK +K+FKL+++SF++ +E + +ILGPSGAGK+VL++ IAGI++PD GRIILN
    Sbjct: 1   MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN 60

    Query: 261 KKVFVKYNGYIIKAFTERD--LNIGDNVGLE 289
                +V +K  G  +KAF  R   + +G + G E
    Sbjct: 301 VRVNLKIGGITLKAFITRSSLIELGISEGRE 331
     */
                m_query_sequence.reset( line );
                boolean matches_query = m_query_sequence.matches();
                // Store the start of the query and subject positions as reported by Blast
                boolean doneOnce = false;
                while ( matches_query ) {                    
                    line = inputReader.readLine(); // line with identicals and similars
                    //General.showDebug("Read parseBlastHit line: [" + line + "]");                    
                    line = inputReader.readLine(); // line with subject
                    //General.showDebug("Read parseBlastHit line: [" + line + "]");
                    m_sbjct_sequence.reset( line );
                    boolean matches_sbjct = m_sbjct_sequence.matches();
                    if ( ! matches_sbjct ) {
                        General.showError("didn't match query sequence with regexp: [" + regexp_sbjct_sequence + "]");
                        General.showError("for line: [" + line + "]");
                        General.showError("now on line  : " + inputReader.getLineNumber());
                        return null;
                    }
                    if ( blastmatch.query_orf_sequence == null ) {
                        blastmatch.query_orf_sequence = m_query_sequence.group(2);
                    } else {
                        blastmatch.query_orf_sequence = blastmatch.query_orf_sequence.concat( m_query_sequence.group(2) );
                    }                   
                    // Only do once per sequence.
                    if ( ! doneOnce ) {
                        blastmatch.query_orf_match_start   = Integer.parseInt( m_query_sequence.group(1));                        
                        blastmatch.subject_orf_match_start = Integer.parseInt( m_sbjct_sequence.group(1));                        
                        //General.showDebug("Found match starts query and subject: " + blastmatch.query_orf_match_start + ", " + blastmatch.subject_orf_match_start );
                        doneOnce = true;
                    }
                    
                    if ( blastmatch.subject_orf_sequence == null ) {
                        blastmatch.subject_orf_sequence = m_sbjct_sequence.group(2);
                    } else {
                        blastmatch.subject_orf_sequence = blastmatch.subject_orf_sequence.concat( m_sbjct_sequence.group(2) );
                    }         
                    //General.showOutput("query_orf_sequence   reads: " + blastmatch.query_orf_sequence);
                    //General.showOutput("subject_orf_sequence reads: " + blastmatch.subject_orf_sequence);
                    
                    line = getNextNotEmptyLine( inputReader );            
                    m_query_sequence.reset( line );
                    matches_query = m_query_sequence.matches();
                }
                // After the previous loop there should at least have been some sequence added.
                if ( blastmatch.query_orf_sequence == null || blastmatch.query_orf_sequence.length() < 1 ) {
                    General.showError("didn't match query sequence with regexp: [" + regexp_query_sequence + "]");
                    General.showError("for line: [" + line + "]");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;
                }             
                //General.showDebug("Successfully parsed hit.");            
                blastmatch_list.match_list.add( blastmatch );
                // at this point the line is already at the next hit or footer but the next method parses a couple more lines
                // in addition.
                line = gotoNextHitOrFooter( inputReader, line );
                if ( line == null ) {
                    General.showError("While looking for next hit or footer fell through.");
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return null;
                }

                m_score_expect.reset( line );
                if ( m_score_expect.matches() ) {
                    foundAnotherHit = true;                    
                } else {
                    foundAnotherHit = false;                    
                }
                //General.showDebug("m_score_expect.matches() " + m_score_expect.matches());
                firstPass = false;
            } // end of hit
        } catch ( IOException e ) {
            General.showThrowable(e);
            return null;
        }                    
            
        return line;
    }
    
    /** Throws away a number of lines at the end without interpretation; very fast!
<PRE>
  Database: pdb_seqres.txt
    Posted date:  Dec 17, 2002  7:02 PM
  Number of letters in database: 9,419,815
  Number of sequences in database:  44,233
  
Lambda     K      H
   0.316    0.131    0.367 

Gapped
Lambda     K      H
   0.267   0.0410    0.140 


Matrix: BLOSUM62
Gap Penalties: Existence: 11, Extension: 1
Number of Hits to DB: 4,529,460
Number of Sequences: 44233
Number of extensions: 162094
Number of successful extensions: 284
Number of sequences better than 10.0: 23
Number of HSP's better than 10.0 without gapping: 5
Number of HSP's successfully gapped in prelim test: 18
Number of HSP's that attempted gapping in prelim test: 275
Number of HSP's gapped (non-prelim): 27
length of query: 274
length of database: 9,419,815
effective HSP length: 94
effective length of query: 180
effective length of database: 5,261,913
effective search space: 947144340
effective search space used: 947144340
T: 11
A: 40
X1: 16 ( 7.3 bits)
X2: 38 (14.6 bits)
X3: 64 (24.7 bits)
S1: 41 (21.6 bits)
S2: 57 (26.6 bits)
     */

    public static String parseBlastListFooter( String line, LineNumberReader inputReader, BlastMatchList blastmatch_list ) {
            
        try {
            while ( (line != null ) && (!line.startsWith("BLAST")) ){
                line = inputReader.readLine();
                //General.showDebug("Read parseBlastListFooter line: [" + line + "]");                
            }
        } catch ( IOException e ) {
            General.showThrowable(e);
            return null;
        }
        // Let's assume that a null read at this point means we're done.
        if ( line == null ) {
            line = EOL_TOKEN;
        }
        //General.showDebug("Successfully parsed footer.");
        
        return line;
    }
    
    public static boolean hasMoreBlastHits( String line ) {
        if ( line.startsWith(">") ) {
            return true;
        }
        return false;
    }

    /** Moves the cursor to after the next not empty line and returns the
     *string with the non-empty line content. Returns null if the eof or
     *error was encountered before finding it.
     */
    public static String getNextNotEmptyLine( LineNumberReader inputReader ) {
        //General.showDebug("Doing getNextNotEmptyLine");
        String line = EMPTY_STRING;
        try {
            while ( line != null ) {
                line = inputReader.readLine();
                //General.showDebug("Read getNextNotEmptyLine line: [" + line + "]");                
                if ( line == null ) {
                    return null;
                }
                if ( ! line.trim().equals(EMPTY_STRING) ) {
                    return line;
                }
            }
        
        } catch ( IOException e ) {
            General.showThrowable(e);
            return null;
        }
        
        return null;
    }


    /** Moves the cursor to after the next line with a ">" and returns the
     *line with the >. Returns null if the eof or
     *error was encountered before finding it.
     * Parse away:
                                                                 Score    E
Sequences producing significant alignments:                      (bits) Value

1mkk_B mol:protein length:96     Vascular Endothelial Growth Fac...    26   3.2  
     *and parse away:
Query: 1   MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN 59
           ML+V ++SK +K+FKL+++SF++ +E + +ILGPSGAGK+VL++ IAGI++PD GRIILN
Sbjct: 1   MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN 60
...
Query: 261 KKVFVKYNGYIIKAFTERD--LNIGDNVGLE 289
            +V +K  G  +KAF  R   + +G + G E
Sbjct: 301 VRVNLKIGGITLKAFITRSSLIELGISEGRE 331
     */
    public static String gotoNextHitOrFooter( LineNumberReader inputReader, String line ) {
        //General.showDebug("Doing gotoNextHitOrFooter");
        try {
            while ( ( line != null ) &&
                    ( ! line.startsWith(">") ) && 
                    ( ! line.startsWith(" Score") ) &&  // newly added
                    ( ! line.startsWith("  Database:") ) ) {
                line = inputReader.readLine();
                //General.showDebug("Read gotoNextHitOrFooter line: [" + line + "]");                
            }
        } catch ( IOException e ) {
            General.showThrowable(e);
            return null;
        }
        return line;
    }

    /** Parses one blast text output file with possibly multiple queries. Returns false if an error occurred.
     *The BlastMatchList objects within can have no hits if such was found in
     *the blast output.
     */
    public static boolean convertBlast( String input_file_name, String output_file_name, int verbosity ) {

        General.verbosity = verbosity;
        //General.showDebug("Not all debugging info will be shown because of speed reasons.");
        
        try {
            FileReader fr = new FileReader( input_file_name );
            BufferedReader br = new BufferedReader(fr, FILE_IN_BUFFER_SIZE);
            LineNumberReader inputReader = new LineNumberReader( br );
            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( !inputReader.ready() ) {
                General.showError("input not ready or just empty..");
                return false; 
            }

            File outputFile = new File( output_file_name );
            if ( outputFile.exists() ) {
                General.showOutput("Output file already existed; will try to delete it first.");               
                if ( ! outputFile.delete() ) {
                    General.showError("Failed to delete output file: " + output_file_name);
                    return false;
                }
            }
            
            FileOutputStream        file_out = new FileOutputStream( output_file_name );
            BufferedOutputStream    bout = new BufferedOutputStream(file_out, FILE_OUT_BUFFER_SIZE );
            ObjectOutputStream      out = new ObjectOutputStream( bout );
            // Keep working until done.        
            // The cursor in the file will be after the line just parsed
            // The line contains the first thing to be parsed.
            int query_id = 0;
            String line = getNextNotEmptyLine( inputReader );
            while ( (line != null) && ( !line.equals(EOL_TOKEN)) ) {
                //General.showDebug("parsing query: " + query_id);
                //General.showDebug("starting with line: [" + line + "]");
                
                BlastMatchList blastmatch_list = new BlastMatchList();
                line = parseBlastListHeader( line, inputReader, blastmatch_list );
                if ( line == null ) {
                    General.showError("parsing blast header.");
                    General.showError("for query id : " + query_id);
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return false;
                }
                // Are there any hits? 
                if ( line.indexOf("No hits found") == -1 ) {
                    // Yes there are hits so parse them one by one.
                    line = gotoNextHitOrFooter( inputReader, line );
                    if ( line == null ) {
                        General.showError("didn't find promised hit.");
                        General.showError("for query id : " + query_id);
                        General.showError("now on line  : " + inputReader.getLineNumber());
                        return false;
                    }
                    int hit_id = 0;
                    while ( hasMoreBlastHits(line) ) {
                        line = parseBlastHit( line, inputReader, blastmatch_list );
                        if ( line == null ) {
                            General.showError("parsing hit  : " + hit_id);
                            General.showError("for query id : " + query_id);
                            General.showError("now on line  : " + inputReader.getLineNumber());
                            return false;
                        }          
                        hit_id++;
                    }
                } else {
                    General.showDebug("no hits for this query: " + query_id);                    
                }
                line = parseBlastListFooter( line, inputReader, blastmatch_list );
                if ( line == null ) {
                    General.showError("parsing blast header.");
                    General.showError("for query id : " + query_id);
                    General.showError("now on line  : " + inputReader.getLineNumber());
                    return false;
                }
                //General.showDebug("Serializing BlastMatchList: " + query_id);
                /** Force the output stream to be closed every time;
                 *Perhaps this will overcome the jvm holding onto objects after
                 *they'return written.                 
                 */
                out.writeObject( blastmatch_list );
                out.flush(); // This might take some time with the large buffer size choicen.
                out.reset(); // Don't remember state of flushed objects.
                //InOut.writeObject( blastmatch_list, output_file_name );
                //General.showDebug("Size now: " + (outputFile.length()/1024) + "k");
                //String fullText = blastmatch_list.toString();
                //General.showOutput("id: " + query_id + " has length of String: " + fullText.length());
                
                query_id++;
            }
            
            out.flush(); // This might take some time with the large buffer size choicen.
            out.close();
            
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        return  true;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( args.length != 3) {
            General.showOutput("Usage:  java -Xmx512m Wattos.Gobbler.Converters.BlastParser " +
            "<blast text input file> " +
            "<blast bin output file> " +
            "<verbosity>");
            General.showOutput("verbosity:          0-9:0 no output, 2 normal, including warnings, 9 debug");
            System.exit(1);
        }
        String input_file_name  = args[0];
        String output_file_name = args[1];
        int verbosity = Integer.parseInt( args[2] );

        General.showOutput("Version: 1" );
        if ( verbosity > 1 ) {
            General.showOutput("Reading  file: "+input_file_name);
            General.showOutput("Writing file: "+output_file_name);
        }
        boolean status = convertBlast( input_file_name, output_file_name, verbosity );
        if ( ! status ) {
            General.showError( "in convertBlast.");
            System.exit(1);
        } else {
            if ( verbosity > 1 ) {
                General.showOutput("Done successfully with Wattos.Gobbler.Converters.BlastParser");
            }
        }
    }    
}
