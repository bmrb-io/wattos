/*
 * StarGeneral.java
 *
 * Created on April 25, 2003, 1:28 PM
 */

package Wattos.Star;

import EDU.bmrb.starlibj.StarParser;
import Wattos.Utils.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.*;

/**
 *Code common for all nodes in a star tree. Settings for verbosity, flavor and 
 *quotes.
 * @author Jurgen F. Doreleijers
 */
public class StarGeneral {
    
    public static final int STANDARD_FLAVOR_NMRSTAR = 1;
    public static final int STANDARD_FLAVOR_CIF     = 2;
    public static int       STANDARD_FLAVOR_DEFAULT = STANDARD_FLAVOR_NMRSTAR ;

    public static final int STANDARD_VERBOSITY_TERSE        = 0;
    public static final int STANDARD_VERBOSITY_ONLY_ERRORS  = 1;
    public static final int STANDARD_VERBOSITY_WARNINGS     = 2;
    public static final int STANDARD_VERBOSITY_DEBUG        = 9;
    public static final int STANDARD_VERBOSITY_DEFAULT      = STANDARD_VERBOSITY_WARNINGS;
    
    public static final int DATA_NODE_TYPE_GLOBALBLOCK      = 1; // Not supported
    public static final int DATA_NODE_TYPE_DATABLOCK        = 2;
    public static final int DATA_NODE_TYPE_SAVEFRAME        = 3;
    public static final int DATA_NODE_TYPE_TAGTABLE         = 4; // Tagtable doesn't extend datanode because it already extends Relation
    public static final int DATA_NODE_TYPE_TAGVALUE         = 5;    
    public static final int DATA_NODE_TYPE_DEFAULT          = DATA_NODE_TYPE_DATABLOCK;
    
    public static final String[] DATA_NODE_TYPE_DESCRIPTION = 
        { "Invalid", "GlobalBlock", "DataBlock", "SaveFrame", "TagTable", "TagValue" };
    
    public static final String INVALID_STAR_STRING = "\'Not a valid STAR string\'"; // yeah, we know a few.

    public static final String WILDCARD             = "*"; // In selections will select all
    public static final String STAR_NULL_STRING     = "."; // In selections will select NOTHING
            
    public static final boolean USE_SINGLE_QUOTE_BY_DEFAULT = false; // to make consistent with Python STAR API
    
    public int verbosity               = STANDARD_VERBOSITY_DEFAULT;
    /** Indent for loop_ */
    public int loopIdentSize           = 4;
    /** Indent for free tag names  */
    public int freeIdentSize           = loopIdentSize;       
    /** Indent for looped tag names  */
    public int tagnamesIdentSize       = loopIdentSize + 3;
    public boolean showStopTag         = true;

    /** StarParser to be used only once; silly. Initialized in constructor of this class*/
    public static EDU.bmrb.starlibj.StarParser sp = null;
    
    static private final Matcher     m_starting_semicolon;    
    static {
        m_starting_semicolon            = Pattern.compile("^;", Pattern.MULTILINE).matcher("");
        sp = new StarParser( new BufferedReader(new StringReader("_data"))); // dummy        
    }
    
    /** Creates a new instance of StarGeneral */
    public StarGeneral() {
    }
    
    public boolean init() {
        setStarFlavor( STANDARD_FLAVOR_NMRSTAR );
        return true;
    }

    public boolean setStarFlavor(int flavorType) {
        // Still to be used.
        if ( flavorType == STANDARD_FLAVOR_NMRSTAR ) {
            // Number of spaces before the loop_ tag. 0 in CIF
            loopIdentSize           = 4;
            freeIdentSize           = loopIdentSize;       
            tagnamesIdentSize       = loopIdentSize + 3;
            showStopTag             = true;
        } else if ( flavorType == STANDARD_FLAVOR_CIF ) {
            loopIdentSize           = 0;
            freeIdentSize           = 0;       
            tagnamesIdentSize       = 0;
            showStopTag             = false;
        } else {
            General.showError("code bug; unknown star flavor");
            return false;
        }
        return true;
    }
    
    /** Use the starlibj api to print the star formatting nicely
     *
    public static String toSTARNice( String ugly ) {
        String result = null;
        try {
            // Now do things nicely.
            StringReader sr = new StringReader( ugly );
            if ( sp == null ) {
                 sp = new StarParser( new StringReader("")); // give a dummy string for now.
            }
            sp.ReInit(sr);
            sp.DataLoopNodeParse(sp);
            EDU.bmrb.starlibj.StarNode sn = sp.popResult();
            ByteArrayOutputStream baos = new ByteArrayOutputStream(ugly.length());
            StarUnparser myUnparser = new StarUnparser( new BufferedOutputStream(baos));
            myUnparser.setFormatting( true );
            myUnparser.setIndentSize(4);
            myUnparser.setExtraColumnSpaces(0); // new parameter introduced for more compact (mmcif-like) tables
            myUnparser.writeOut( sn, 0 ); 
            
            baos.close();                        
            result = baos.toString();                 
        } catch ( Throwable t) {
            General.showError("Other error: " +  t.getMessage() );
        }
        return result;
    }
     */
    
    /** Oposite of addQuoteStyle;-)
     * coded for speed so it DOESN'T remove semicolon quoted styles
     *  
     * */
        public static String removeQuoteStyle( String in )    {   
            int len = in.length();
            if ( len < 2 ) {
                return in;
            }
            char c0 = in.charAt(0);
            char cn = in.charAt(in.charAt(len-1));
            
            if ( ( c0 == '"' && cn == '"' ) ||
                 ( c0 == '\'' && cn == '\'' )) {
                return in.substring(1,len-1);
            }
            return in;
        }

    /** Returns true if the string given is valid for a
      * single-quote delimiter in a DataValueNode.
     *Stolen from starlibj added case of tab following ' and
     *removed a perfectly correct return true statement for style points.
     *Generalized for delim types '"' and '\'' which might be bad for performance.
      */
    public static boolean isValidValueForDelim( String s, char c ) {
        int i;
        // Invalid if it contains line breaks within or
        // a single-tick quote followed by space|tab.
        for( i = 0 ; i < s.length() ; i++ )	{
            if ( s.charAt(i) == '\n' || s.charAt(i) == '\r' ) {
                return false;
            }
            if ( s.charAt(i) == c ) {
                if ( i < s.length() - 1 ) {
                    if ( s.charAt(i+1) == ' ' || s.charAt(i+1) == '\t' ) {
                        return false;
                    }
                }
//                else return true;
            }
        }
        return true;
    }

    
    /** Stolen from STARlibj. Removed the case of a saveframe code being
     *invalid for non delim. Optimized a little bit by caching lower case
     *value of string.
     *
     * Returns true if the string given is valid for a
      * nondelimited DataValueNode (no whitespace).
      * @return true if valid, false if invalid
      */
    public static boolean isValidValueForNonDelim( String s ) {
	
//        int i;

        // Must have at leas some value.
        if ( s.length() == 0 ) {
            return false;
        }
        // Invalid if it starts with ' or " or _ [or $]
        if ( s.charAt(0) == '\'' ) {
            return false;
        }
        if ( s.charAt(0) == '\"' ) {
            return false;
        }
//        if ( s.charAt(0) == '$' ) 
//            return false;
        if ( s.charAt(0) == '_' ) {
            return false;
        }
        // Invalid if the first character is a comment starter
        //    ('#') - hashes are legal inside the value as long as
        //     it is not the first character of the string.  If it
        //     is the first character, then this really should be a
        //     comment, not a value.
        // Invalid if contains whitespace within.
        if ( s.charAt(0) == '#' ) {
            return false;
        }
        
        if ( Strings.m_whitespace.reset(s).find()) {
            return false;
        }
//        for( i = 0 ; i < s.length() ; i++ ) {
//            if( Character.isWhitespace( s.charAt(i) ) )
//            return false;
//        }

        // Invalid if it is a keyword:
        // Cache for speed.
        String sLowerCase = s.toLowerCase();
        if( sLowerCase.startsWith("data_") ||
            sLowerCase.startsWith("save_") ||
            sLowerCase.equals("global_") ||
            sLowerCase.equals("loop_") ||
            sLowerCase.equals("stop_") ) {
            return false;
        }
        return true;
    }

    
    /** Now in contrast to the python code; don't use regexp but use Steve's code
     *for checking the validity of each quote style. This is highly optimized for
     *the case where there are mostly small NONs. Doesn't quote values starting with
     *a dollar sign($).
     */
    public static String addQuoteStyle( String in )    {

//        General.showDebug("In addQuoteStyle: [" + in + "]");
	// Find the appropriate delimiter type:
	// (Never pick FRAMECODE unless explicity told to do so.)
        if( isValidValueForNonDelim( in ) ) {
//            General.showDebug("In addQuoteStyle: Found isValidValueForNonDelim");
            return in;
        }
        
        //        /** Next test needs to be after the above because of values that start with
//         *$ but aren't sf codes e.g. in 1qrj: '$$$$$$$$---PRO35 is TRANS- but qd is doubtfull!--$$$$$$$#'
//         *Still doesn't cover the case where a multiline value starts with $ but that has not
//         *been observed yet.
//         */
////        if( in.charAt(0) == '$') {
////            if ( ! Strings.p_whitespace.matcher(in).find()) {
////                return in;
////            }
////        }

        // The order of the next two ifs determines the prefered quote style. 
        // changed to " on 2007-01-23 because it's Steve's starlib default.
        // changed back to ' on 2007-02-22 because Wattos own internal testing code used it.
        if ( USE_SINGLE_QUOTE_BY_DEFAULT ) {
            if( isValidValueForDelim( in, '\'' ) ) {
              return "'" + in + "'";
            }
            if( isValidValueForDelim( in, '"' ) ) {
              return "\"" + in + "\"";
            }
        } else {
            if( isValidValueForDelim( in, '"' ) ) {
                return "\"" + in + "\"";
            }
            if( isValidValueForDelim( in, '\'' ) ) {
                return "'" + in + "'";
            }            
        }
        
        m_starting_semicolon.reset( in );
        // The next condition is very rare so no problem to scan twice in case it does happen.
        // In the MR file for PDB entry 1TUS this does occur:
        /**;or other options have been superceded.
;
         */
        //General.showOutput("Looking for a ; at the beginning of a line in string: [" + in + "]");
        if ( m_starting_semicolon.find() ) { // Finds the first occurance of the regular expression.
            General.showWarning("Matched a ; at the beginning of a line which will be represented by a ; preceded by a space in order to maintain a valid STAR grammar.");
            in = m_starting_semicolon.replaceAll(" ;");             
        }
        // Anything is allowed to occur in this quote style except the situation 
        // caught above.
        return "\n" +
               ";" + in + 
                "\n"+ 
               ";" + 
                "\n";          
    }
    
    /** Return null on error or a valid string. For now only all white spaces are caught.
     */
    public static String toValidSaveFrameName( String in )    {
        String out = Strings.replace( in, "\\s", "_" );
        if ( out == null  ) {
            General.showError("Failed to generate a valid saveframe name for input: " + in);
            return null;
        }
        return out;
    }
            
        
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if ( true ) {
            String[] tests = {"foo\n;" };
            //String[] tests = {"foo\n;", "a", "a b", "###", "#", "a\nb", "'' ''", "'''", "'", "\"", "\"a", "\'a" };
            for (int i=0;i< tests.length; i++ ) {
                General.showOutput("INPUT:  ######################################################" );
                General.showOutput( tests[i] );
                General.showOutput("OUTPUT: ######################################################" );
                General.showOutput( addQuoteStyle(tests[i]) );
            }
        }
        if ( true ) {
            String in = "test je";
            String out = toValidSaveFrameName(in);
            General.showDebug("in:  " + in);
            General.showDebug("out: " + out);
        }
    }    
}
