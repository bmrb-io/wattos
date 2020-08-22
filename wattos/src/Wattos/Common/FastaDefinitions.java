/*
 * FastaDefinitions.java
 *
 * Created on December 30, 2002, 12:07 PM
 */

package Wattos.Common;

import Wattos.Utils.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * See {@link <a href="FastaSpecs.html">here</a>}
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class FastaDefinitions {
    
    public static final String  FASTA_FORMAT_ID_GENERAL = "FASTA_FORMAT_ID_GENERAL";
    public static final char    FASTA_DELIM             = '|';
    public static final char    FASTA_START_CHAR        = '>';
    public static final String  FASTA_DEFAULT_REPLACEMENT_STRING        = "_";

    /** The type of filtering possible to do. Keep the ints parallel with the String[].
     */
    public static final String[]    FASTA_FILTER_TYPES = { "none", "blastdb-suitable" };
    public static final int         FASTA_FILTER_TYPE_NONE                  = 0;
    public static final int         FASTA_FILTER_TYPE_BLASTDB               = 1;

    public static Pattern p_general_format;
    public static Matcher m_general_format;
    public static String regexp_general_format;
    
    public static Pattern p_end_of_record;
    public static Matcher m_end_of_record;
    public static String regexp_end_of_record;

    public static Pattern p_bar;
    public static Matcher m_bar;
    public static String regexp_bar;

    public static Pattern p_name_pipe_date;
    public static Matcher m_name_pipe_date;
    public static String regexp_name_pipe_date;

    public static Pattern p_invalid_chars;
    public static Matcher m_invalid_chars;
    public static String regexp_invalid_chars;

    
    static {
        try {
            regexp_general_format = 
                 /** The one above doesn't work well because it does allow all
                  *groups to be retrieved.*/
                    "([^|]+) \\|" + // db name
                    "([^|]*) \\|" + // db id
                    "([^|]*) \\|" + // db subid
                    "([^|]*) \\|" ; // mol name            
            p_general_format = Pattern.compile(regexp_general_format, Pattern.COMMENTS);            
            m_general_format = p_general_format.matcher(""); // default matcher on empty string.
            
            // Simple one...
            regexp_end_of_record = "\n \\S";
            p_end_of_record = Pattern.compile(regexp_end_of_record, Pattern.MULTILINE );
            m_end_of_record = p_end_of_record.matcher(""); // default matcher on empty string.
            // Simple one too...
            regexp_bar = "\\|";
            p_bar = Pattern.compile(regexp_bar);
            m_bar = p_bar.matcher(""); // default matcher on empty string.

            // Simple one too...
            regexp_name_pipe_date = 
                    "^([^|]*) \\|" +            // db id at beginning of line
                    "(\\d{4}-\\d{2}-\\d{2})$"; // date like 2002-11-04 at end of line
            p_name_pipe_date = Pattern.compile(regexp_name_pipe_date, Pattern.COMMENTS);
            m_name_pipe_date = p_name_pipe_date.matcher(""); // default matcher on empty string.

            // Simple one too...
            regexp_invalid_chars = 
                    "[ '`\"<>&]"; // simple list including space, quotes, angular brackets etc.
            p_invalid_chars = Pattern.compile(regexp_invalid_chars, Pattern.COMMENTS);
            m_invalid_chars = p_invalid_chars.matcher(""); // default matcher on empty string.

        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);            
        }
    }
    
    /** Creates a new instance of FastaDefinitions */
    public FastaDefinitions() {
    }

    /** Replaces invalid characters (angular brackets, spaces etc.) in the fasta formatted stringbuffer.
     */
    public static String replaceInvalidCharsFasta( String input) {
        m_invalid_chars.reset( input );
        return m_invalid_chars.replaceAll(FASTA_DEFAULT_REPLACEMENT_STRING);
    }
    
    /** Parses a summary line coming from fasta as echoed by blast
     */
    public static OrfIdList parseBlastId( String str_id ) {
        if ( str_id == null ) {
            General.showError("input string is null in parseBlastId");
            return null;
        }
                
        OrfIdList oiList = null;
        if ( str_id.endsWith( FASTA_DELIM + FASTA_FORMAT_ID_GENERAL ) ) {
            oiList =  parseBlastIdGeneral( str_id );
        } else if ( isBlastIdNamePipeDate(str_id) ) {
            oiList =  parseBlastIdNamePipeDate( str_id );            
        } else if ( str_id.indexOf('|') == -1 ) {
            oiList =  parseBlastIdSimple( str_id );
        } else {
            oiList =  parseBlastIdNR( str_id );
        }
        //General.showDebug("Fasta repres. of orfidlist: " + oiList.toFasta());
        return oiList;
    }
    
    /** Sees if this fullfills the regular expression?
     */
    public static boolean isBlastIdNamePipeDate( String str_id ) {
        str_id = str_id.trim();
        m_name_pipe_date.reset( str_id );
        boolean status = m_name_pipe_date.matches();
        //General.showDebug("Processing string: [" + str_id + "]");            
        //General.showDebug("name pipe date matches: [" + status+ "]");
        
        return status;
    }

    /** 
     * Nice examples:
     * 12345
     * at1g01990.save_at1g01990.seq
     */
    public static OrfIdList parseBlastIdSimple( String str_id ) {
        if (str_id==null) {
            General.showWarning("got an empty line in: parseBlastIdSimple");
            return null;
        }
        //General.showDebug("in parseBlastIdSimple working on record: ["+str_id+"]");        
                
        OrfIdList orf_id_list = new OrfIdList();
        OrfId orf_id = new OrfId();
        orf_id_list.orfIdList.add( orf_id );
        orf_id.orf_db_id      = str_id;
        
        return orf_id_list;
    }

    /** 
     * Nice examples:
     * P protein|2002-05-05
     */
    public static OrfIdList parseBlastIdNamePipeDate( String str_id ) {
        if (str_id==null) {
            General.showWarning("got an empty line in: parseBlastIdSimple");
            return null;
        }
        //General.showDebug("in parseBlastIdSimple working on record: ["+str_id+"]");        
        // Matcher was already done but we'll repeat for code clearity.
        m_name_pipe_date.reset( str_id );
        boolean status = m_name_pipe_date.matches();
        if (!status) {
            General.showError("code bug in parseBlastIdNamePipeDate");
            return null;
        }
        
        OrfIdList orf_id_list = new OrfIdList();
        OrfId orf_id = new OrfId();
        orf_id_list.orfIdList.add( orf_id );
        orf_id.orf_db_id      = m_name_pipe_date.group( 1 );
        orf_id.orf_db_subid   = m_name_pipe_date.group( 2 );
        
        return orf_id_list;
    }

    /** This method was written based on the specifications at
     * ftp://ftp.ncbi.nlm.nih.gov/blast/documents/blastdb.txt
     *Nice example:
<PRE>
sp|P30406|INS_MACFA INSULIN PRECURSOR
pir||JQ0178 insulin precursor - crab-eating macaque
gb|AAA36849.1| preproinsulin
</PRE>
     *or:
>gi|2914590|pdb|1JXP|A Chain A, Bk Strain Hepatitis C Virus (Hcv)
           Ns3-Ns4a
 gi|2914591|pdb|1JXP|B Chain B, Bk Strain Hepatitis C Virus (Hcv)
           Ns3-Ns4a
 gi|13096662|pdb|1DXW|A Chain A, Structure Of Hetero Complex Of Non
           Structural Protein (Ns) Of Hepatitis C Virus (Hcv) And
           Synthetic Peptidic Compound
 gi|5542135|pdb|1BT7|  The Solution Nmr Structure Of The N-Terminal
           Protease Domain Of The Hepatitis C Virus (Hcv)
           Ns3-Protein, From Bk Strain, 20 Structures
     */
    public static OrfIdList parseBlastIdNR( String str_id ) {        
        
        ArrayList lines = new ArrayList();
        m_end_of_record.reset( str_id );
        int start_position = 0;
        int end_position = 0; // doesn't matter
        String line;
        while ( m_end_of_record.find() ) {
            end_position = m_end_of_record.start();
            line = str_id.substring( start_position, end_position );
            lines.add( line );
            start_position = m_end_of_record.end() - 1;
        }
        line = str_id.substring( start_position );
        lines.add( line );

        if ( lines.size() < 1 ) {
            General.showError("didn't get at least 1 line when splitting: ["+str_id+"]");
            return null;
        }
        
        OrfIdList orf_id_list = new OrfIdList();
        for (int i=0;i<lines.size();i++ ) {
            // Trim and replace the eol with spaces to just 1 space
            String on_one_line = ((String) lines.get(i)).trim().replaceAll("\n\\s+", " ");
            OrfId orf_id = parseBlastIdNRSingle( on_one_line );
            if ( orf_id != null ) {
                orf_id_list.orfIdList.add( orf_id );
            }
        }
        return orf_id_list;
    }

    /** This method was written based on the specifications at
     * ftp://ftp.ncbi.nlm.nih.gov/blast/documents/blastdb.txt
     * Possible databases include:
     * <PRE>
     *  Database Name                     Identifier Syntax
     *  ============================      ========================
     *  GenBank                           gb|accession|locus
     *  EMBL Data Library                 emb|accession|locus
     *  DDBJ, DNA Database of Japan       dbj|accession|locus
     *  NBRF PIR                          pir||entry
     *  Protein Research Foundation       prf||name
     *  SWISS-PROT                        sp|accession|entry name  -- example doesn't follow standard
     *  Brookhaven Protein Data Bank      pdb|entry|chain
     *  Patents                           pat|country|number       -- no examples
     *  GenInfo Backbone Id               bbs|number               -- no examples
     *  General database identifier	  gnl|database|identifier  -- no examples
     *  NCBI Reference Sequence           ref|accession|locus
     * </PRE>
     *
     * Nice examples:
     * 
<PRE>
 bmrb|4020|BRSV-G|Bovine_Syncytal_Repiratory_Virus-G
 pdb|1EV3|A Chain A, Structure Of The Rhombohedral Form Of The
            M-CresolINSULIN R6 Hexamer
 sp|P30406|INS_MACFA INSULIN PRECURSOR
 pir||JQ0178 insulin precursor - crab-eating macaque
 gb|AAA36849.1| preproinsulin
</PRE>
     *
     */
    public static OrfId parseBlastIdNRSingle( String str_id ) {
        if (str_id==null) {
            General.showWarning("got an empty line in: parseBlastIdNRSingle");
            return null;
        }
        //General.showDebug("in parseBlastIdNRSingle working on record: ["+str_id+"]");        
        
        // Limit the number of resulting strings to 3 as there might be more bars
        // present in the molecule name or at least not interpretted for now.
        String[] parts = p_bar.split( str_id, 3 );
        
        if ( parts.length != 3 ) {
            if  ( ( parts.length == 2 ) && parts[0].equals("bbs") ) {
                ; // fine; allowed
            } else {
                General.showError("splitting line on bars: ["+str_id+"]");
                General.showError("found not the expected number of items 3 but : " + parts.length);
                General.showError("2 items is also allowed in case the db name is 'bbs'");
                return null;
            }
        }
        
        OrfId orf_id = new OrfId();
        orf_id.orf_db_name      = parts[0];
        
        if ( orf_id.orf_db_name.equals("pdb") ) {
            //  pdb|1EV3|A Chain A, Structure Of The Rhombohedral Form Of The 
            //          M-CresolINSULIN R6 Hexamer
            orf_id.orf_db_id        = parts[1];
            int idx = parts[2].indexOf(' ');
            if ( idx != -1 ) {
                orf_id.orf_db_subid     = parts[2].substring(0,idx);
                if ( idx != parts[2].length() ) {
                    orf_id.molecule_name    = parts[2].substring(idx+1).trim();
                }
            }
        } else if ( orf_id.orf_db_name.equals("pir") || 
                    orf_id.orf_db_name.equals("prf") ||
                    orf_id.orf_db_name.equals("sp")  ) {
            //(EXAMPLE AGAINST DEFINITIONS)  sp||INS_BALPH_2 [Segment 2 of 2] INSULIN
            // prf||560164B insulin""sp|P30406|INS_MACFA INSULIN PRECURSOR
            int idx = parts[2].indexOf(' ');
            if ( idx != -1 ) {
                orf_id.orf_db_id     = parts[2].substring(0,idx);
                if ( idx != parts[2].length() ) {
                    orf_id.molecule_name    = parts[2].substring(idx+1).trim();
                }
            }
        } else if ( orf_id.orf_db_name.equals("bbs") ) {
            //(NO EXAMPLE OBSERVED) *  GenInfo Backbone Id               bbs|number
            orf_id.orf_db_id        = parts[1];
            orf_id.molecule_name    = parts[1].trim();
        } else if ( orf_id.orf_db_name.equals("bmrb") ) {
            //bmrb|4020|BRSV-G|Bovine_Syncytal_Repiratory_Virus-G
            orf_id.orf_db_id        = parts[1];
            orf_id.orf_db_subid     = parts[2]; // This is the saveframe code without "save_"
            // Not available:
            /**
            orf_id.molecule_name    = parts[3].trim();
            orf_id.molecule_name     = Strings.breakWord( orf_id.molecule_name );
             */
        } else {
            // General case!
            orf_id.orf_db_id        = parts[1];
            if ( parts.length > 2 ) {
                orf_id.molecule_name    = parts[2].trim();
            }
        }
                
        
        return orf_id;
    }

    /** Follows specifications as described in FastaSpecs.txt in the same directory
     *as this source. 
     *TODO: doesn't yet handle multiple sets properly, just shows last
     *one. This is not a problem at this point but will have to be appended later on...
     */
    public static OrfIdList parseBlastIdGeneral( String str_id ) {
        
        //General.showDebug("in parseBlastIdGeneral working on record: ["+str_id+"]");        

        OrfIdList orf_id_list = new OrfIdList();        

        m_general_format.reset( str_id );
        boolean status = m_general_format.find();
        int i = 0;
        while ( status ) {
            i++;
            // DEBUGING
            //Strings.showMatchedGroups( m_general_format );
            OrfId orf_id = new OrfId();
            orf_id_list.orfIdList.add( orf_id );
            orf_id.orf_db_name       = m_general_format.group(1);
            orf_id.orf_db_id         = m_general_format.group(2);
            orf_id.orf_db_subid      = m_general_format.group(3);                        
            orf_id.molecule_name     = m_general_format.group(4);
            // Not allways needed?
            orf_id.molecule_name     = Strings.breakWord( orf_id.molecule_name );
            status = m_general_format.find();
        }
            
        if ( i < 1 ) {
            General.showError("Processing string: [" + str_id + "]");            
            General.showError("unexpectted formatting overall.");            
            General.showError("didn't match regular expression even once :[" + regexp_general_format + "]");            
            return null;
        }
        
        
        /** Check to see if there are multiple sets of each 4 items as expected
        int itemCount = m_general_format.groupCount(); // This already excludes the overall match
        int groupCount = itemCount / OrfId.COLUMN_COUNT;
        if ( ((itemCount % OrfId.COLUMN_COUNT) != 0) || (groupCount < 1) ) {
            General.showError("Processing string: [" + str_id + "]");            
            General.showError("didn't find multiple of "+OrfId.COLUMN_COUNT+" (and at least "+
                OrfId.COLUMN_COUNT+") for items but:[" + itemCount + "]");            
            return null;            
        }
        General.showDebug("found number of set ids: " + groupCount);            
        
                
        /** Add them neatly to the list
        OrfIdList orf_id_list = new OrfIdList();        
        int offset=0;
        for (int i=0;i<groupCount;i++) {
        }                        
         */

        return orf_id_list;
    }
    
    /** Parsing tests
     */
    public static void test_parse() {
//        FastaDefinitions fd = new FastaDefinitions();
        //String line         = "pdb|1o0y|B|Deoxyribose-Phosphate_Aldolase|FASTA_FORMAT_ID_GENERAL";
        /**
        String line         =   "pdb|1o0y|B|Deoxyribose-Phosphate_Aldolase|"+
                                "sw|1234||prot|FASTA_FORMAT_ID_GENERAL";
         */
        String line = 
            "sp||INS_BALPH_2 [Segment 2 of 2] INSULIN\n"+
            " pdb|1EV3|A Chain A, Structure Of The Rhombohedral Form Of The\n"+
            "          M-Cresol INSULIN R6 Hexamer\n"+
            " prf||560164B insulin\"\"sp|P30406|INS_MACFA INSULIN PRECURSOR\n"+
            " bmrb|4020|BRSV-G|Bovine_Syncytal_Repiratory_Virus-G\n"+
            " gb|AAA36849.1| preproinsulin\n";
        
        line = "12345|2003-01-04";
        
        //String line = "bmrb|4020|BRSV-G|Bovine_Syncytal_Repiratory_Virus-G|FASTA_FORMAT_ID_GENERAL";
        General.showOutput("line:        [" + line + "]");

        Orf o = new Orf();        
        o.orf_id_list = FastaDefinitions.parseBlastId(line);
        if ( o.orf_id_list == null ) {
            General.showError("Error in parse");
        } else {
            General.showOutput("orf id list: [\n" + o.orf_id_list + "]");                    
        }
    }
        
    /** Parsing tests
     */
    public static void test_replace_disallowed_chars() {
//        FastaDefinitions fd = new FastaDefinitions();       
        String line = "12345|\"2003\"-01- 04' `<>";        
        General.showOutput("line:           [" + line + "]");
        General.showOutput("line corrected: [" + FastaDefinitions.replaceInvalidCharsFasta( line ) + "]");
    }

    /** Matches the string value to an integer representation by array index.
     */
    public static int getFilterTypeId( String filter_type ) {
        for (int i=0;i<FASTA_FILTER_TYPES.length;i++ ) {
            if ( filter_type.equalsIgnoreCase(FASTA_FILTER_TYPES[i]) ){
                return i;
            }
        }
        General.showError("not existing filter type id");
        General.showError("should be one of:" + Strings.toString( FASTA_FILTER_TYPES ));
        General.showError("assumed first: " + FASTA_FILTER_TYPES[0]);
        return 0;        
    }
    
    /** Matches the string value to an integer representation by array index.
     */
    public static boolean satisfiesFilter( String sequence, int filter_type_id, boolean showFiltered) {
        if ( sequence == null ) {
            return false;
        }
        switch ( filter_type_id ) {
            case FASTA_FILTER_TYPE_NONE:
                return true;
            case FASTA_FILTER_TYPE_BLASTDB:                
                return BlastDefinitions.hasValidSequenceForBlastDb( sequence, showFiltered );
            default:
                General.showError("not existing filter type id");
                General.showError("should be one of:" + Strings.toString( FASTA_FILTER_TYPES ));
                General.showError("assumed first: " + FASTA_FILTER_TYPES[0]);
                return true;        
        }
                
    }
    
    
    /** Only reads file for certain flavors; e.g. cesg style (our own style). 
     *Currently only the Sesame puts out these types except for the code in
     *this package.
     *The routine tries to keep as little as possible in memomry so it can
     *handle large files as anticipated.
     */
    public static boolean convertFastaFile( String input_filename, String output_filename, String input_flavor,
           String output_flavor, String filter_type, int verbosity ) {
	int size_min = Integer.MAX_VALUE; // Will be reset on first accepted sequence.
        int size_max = Integer.MIN_VALUE;

        boolean showFiltered = false;
        if ( verbosity > 2 ) {
            showFiltered = true;
        }
        Orf orf = new Orf();         
        // Get the id first so the string comparison doesn't need to be made
        // for each orf.
        int filter_type_id = getFilterTypeId( filter_type );
        if ( ! input_flavor.equals("cesg") ) {
            return false;
        }
        
        int count_read = 0;
        int count_dropped = 0;
        
        try {
            FileReader fr = new FileReader( input_filename );
            LineNumberReader inputReader = new LineNumberReader( fr );
            BufferedWriter bw = new BufferedWriter( new FileWriter( output_filename));
            PrintWriter outputWriter = new PrintWriter( bw );

            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( ! inputReader.ready() ) {
                General.showDebug("input not ready or just empty..");
                return false; 
            }
            
            String line = inputReader.readLine();
            while ( line != null ) {
                //General.showDebug("Processing line: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? */                
                if ( ( line.length() > 0 ) && 
                     ( line.charAt(0) == FastaDefinitions.FASTA_START_CHAR )) {
                    count_read++;
                    // First line doesn't need a save of previous orf.
                    if ( inputReader.getLineNumber() != 1 ) {
                        orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
                        // Remove last * for input of TIGR 4.0 db.
                        if ( orf.sequence.endsWith("*") ) {
                            orf.sequence = orf.sequence.substring(0, orf.sequence.length()-1);
                        }
                        if ( satisfiesFilter( orf.sequence, filter_type_id, showFiltered ) ) {
                            outputWriter.write( orf.toFasta() );                            
                            if (orf.sequence.length()<size_min) {
                                size_min = orf.sequence.length();                                
                            }
                            if (orf.sequence.length()>size_max) {
                                size_max = orf.sequence.length();                                
                            }
                        } else {
                            count_dropped ++;
                            if ( verbosity > 2 ) {
                                General.showWarning("dropped sequence: [" + orf.toFasta() + "]");
                            }
                        }
                        orf.init();
                    }
                    boolean status = orf.orf_id_list.readFasta( line ); // Read header.
                    if ( ! status ) {
                        General.showError("Will skip this entry because of parse error.");
                    }                   
                } else {
                    orf.sequence = orf.sequence + line;
                }
                line = inputReader.readLine();
            }
            // Handle last sequence in file.
            orf.sequence = orf.sequence.replaceAll("\\s",""); // do here for efficiency
            if ( satisfiesFilter( orf.sequence, filter_type_id, showFiltered ) ) {
                outputWriter.write( orf.toFasta() );
                if (orf.sequence.length()<size_min) {
                    size_min = orf.sequence.length();                                
                }
                if (orf.sequence.length()>size_max) {
                    size_max = orf.sequence.length();                                
                }
            } else {
                count_dropped ++;
                if ( verbosity > 2 ) {
                    General.showWarning("dropped sequence: [" + orf.toFasta() + "]");
                }
            }
            inputReader.close();
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        if ( verbosity >= 2 ) {
            General.showOutput("Read sequences   : " + count_read);
            General.showOutput("Dropped sequences: " + count_dropped);
            General.showOutput("Written sequences: " + (count_read - count_dropped));
            General.showOutput("Shortest sequence: " + size_min);
            General.showOutput("Longest sequence : " + size_max);
        }
        return true;        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //test_parse();
        if ( false ) {
            test_replace_disallowed_chars();
        }
        if ( false ) {
            String filter_type = "blastdb-suitable";
            General.showOutput("filter type id for filter type [" + filter_type + "] is : " + getFilterTypeId( filter_type ) );
        }
        if ( false ) {
            boolean showFiltered = true;
            String filter_type = "blastdb-suitable";
            String sequence = "AEFG";
            General.showOutput("sequence is: " + sequence);
            General.showOutput("filter type id for filter type [" + filter_type + "] is : " + getFilterTypeId( filter_type ) );
            General.showOutput("satisfiesFilter: [" + satisfiesFilter( sequence, getFilterTypeId( filter_type ), showFiltered ) + "]");
        }            
        General.showOutput("Done");
    }    
}
