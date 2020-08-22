/*
 * FastResult.java
 * Created on January 26, 2005, 11:18 AM
 */

package Wattos.Common;

import Wattos.Utils.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 *A data structure of a sequence plus a list of added strings per sequence.
 * @see <a href="FastrSpecs.html">Data on a sequence from a fasta file following the definitions</a>
 * @author Jurgen F. Doreleijers
 */
public class FastResult {
    public OrfIdList oiList = null;
    public ArrayList result = null;

    public static Pattern p_invalid_chars;
    public static Matcher m_invalid_chars;
    public static String regexp_invalid_chars;

    
    static {
        try {
            // Simple one too...
            regexp_invalid_chars = 
                    "[ '`\"<>&]"; // simple list including space, quotes, angular brackets etc.
            p_invalid_chars = Pattern.compile(regexp_invalid_chars, Pattern.COMMENTS);
            m_invalid_chars = p_invalid_chars.matcher(""); // default matcher on empty string.

        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);            
        }
    }
    
    
    /** Creates a new instance of FastResult */
    public FastResult() {
        init();
    }
    
    public void init() {
        oiList = new OrfIdList();
        result = new ArrayList();        
    }
    
    /** Make sure the result can have any lenght with zero included
     */
    public boolean parse( String str ) {
        if ( str == null ) {
            General.showError("input string is null in FastResult.parse.");
            return false;
        }                
        
        String BOTH = FastaDefinitions.FASTA_DELIM + FastaDefinitions.FASTA_FORMAT_ID_GENERAL;
        int startPos = str.indexOf( BOTH );
        if ( startPos < 0 ) {
            General.showWarning("Failed to find in fastr file the string: " + BOTH);
            General.showWarning("in line: " + str);
            return false;
        }
        oiList = FastaDefinitions.parseBlastIdGeneral( str.substring(1,startPos+1));
        if ( (oiList == null ) || (oiList.orfIdList.size()<1)) {
            General.showError("failed to FastaDefinitions.parseBlastIdGeneral from string: [" + str.substring(0,startPos+1) +"]");
            return false;
        }
        String remainder = str.substring(  startPos+BOTH.length());
        if (remainder == null ) {
            General.showError("failed to parseFastrResults from null string" );
            return false;
        }
        if ( remainder.length() == 0) {
            result.clear();
            return true;
        }
        
        result.clear();
        String delim = String.valueOf(FastaDefinitions.FASTA_DELIM);
        StringTokenizer tokens = new StringTokenizer( remainder, 
            delim,
            true); // Return delims
        while ( tokens.hasMoreTokens() ) {
            String token = tokens.nextToken();
            if ( token.equals( delim ) ) {
                result.add( "" );
                //General.showDebug("Adding element: " + token );
            } else {
                result.set( result.size() -1 , token );
                //General.showDebug("Adding token to element: " + token );
            }
        }

        //General.showDebug("Fasta repres. of orf id list: " + oiList.toFasta());
        return true;
    }

    /** Dummy routine because in will be out, ok, it will echo stats on the output.
     */
    public static boolean convertFastrFile( String input_filename, String output_filename ) {
        int count_read=0;
        FastResult fastr = new FastResult();
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
                //General.showDebug("Processing line number: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? */                
                if ( ( line.length() == 0 ) ||
                     ( line.charAt(0) != FastaDefinitions.FASTA_START_CHAR )) {
                    line = inputReader.readLine();
                    continue;
                }
                count_read++;
                fastr.init();
                if ( ! fastr.parse( line ) ) {
                    General.showError("Failed to read fastr record from line: [" + line + "]");
                    return false;
                }
                outputWriter.write( fastr.toFastr());
                line = inputReader.readLine();
            }
            inputReader.close();
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Read/wrote records: " + count_read);
        return true;        
    }
    
    public String toFastr() {
        StringBuffer sb = new StringBuffer();
        String oListStr = oiList.toFasta();
        //General.showDebug("oListStr :["+ oListStr + "]");
        sb.append( FastaDefinitions.FASTA_START_CHAR  );
        sb.append( oListStr );
        sb.append( FastaDefinitions.FASTA_DELIM  );
        sb.append( FastaDefinitions.FASTA_FORMAT_ID_GENERAL  );        
        for (int i=0;i<result.size();i++) {
            sb.append( FastaDefinitions.FASTA_DELIM );
            sb.append( result.get(i).toString() );
        }
        sb.append( General.eol );
        return sb.toString();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        General.showOutput("Starting");
        FastResult.convertFastrFile( args[0], args[1] );
        General.showOutput("Done");
    }
    
}
