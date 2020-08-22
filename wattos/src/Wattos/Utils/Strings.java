/*
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Utils;

import org.apache.regexp.*;
import java.util.regex.*;
import java.text.NumberFormat;
//import java.text.FieldPosition;
import java.io.*;
import java.util.*;
import com.braju.format.*;              // printf equivalent

import Wattos.CloneWars.UserInterface;
import Wattos.Database.*;
import java.lang.reflect.Method;

/**
 * One of the largest classes in Wattos. Static utilities for dealing with String.
 *E.g. getting input from a BufferedReader,
 * @author Jurgen F. Doreleijers
 * @version 1.0
 */
public class Strings {

    public static Parameters p = new Parameters(); // for printf
    public static NumberFormat nf = NumberFormat.getInstance();
    public static RE re_parseDouble, re_getLines, re_areDigits;
    public static RE re_is_pdb_code, re_stripHtml;
    /** Defined here so it doesn't need to be initialized every time */
//    private static FieldPosition fieldPositionDummy;
    private static DoubleToString doubleToString = new DoubleToString();
    /** No need to specify it more than once. */
    public static final String EMPTY_STRING = "";
    public static final int MAX_PROMPTS = 20;
    public static Pattern p_whitespace;
    public static Pattern p_parseDouble;
    public static Matcher m_parseDouble;
    public static Matcher m_whitespace;


    public static String EOL            = General.eol;

    /** Used by getInputChar method */
    public static final char INVALID_CHAR_FOR_ANSWER = '\u0000';

    public static final Pattern EOL_MAC = Pattern.compile("\\r", Pattern.MULTILINE);
    public static final Pattern EOL_DOS = Pattern.compile("\\r\\n", Pattern.MULTILINE);
    public static final Pattern EOL_UNIX = Pattern.compile("([^\\r])(\\n)", Pattern.MULTILINE);
    public static final Pattern EOL_ANY  = Pattern.compile("[\\r\\n]", Pattern.MULTILINE);

    public static final String[] spaceStringList = {
        "",                             //0
        " ",
        "  ",
        "   ",
        "    ",
        "     ",
        "      ",
        "       ",
        "        ",
        "         ",
        "          ",                   //10
        "           ",
        "            ",
        "             ",
        "              ",
        "               ",
        "                ",
        "                 ",
        "                  ",
        "                   ",
        "                    "          //20
    };

    static {
        // Don't use grouping (e.g. use 1000 i.s.o. 1,000)
        nf.setGroupingUsed(false);
        p.autoClear(true);

        //Old style using apaches'
        try {
            re_parseDouble  = new RE("[DF]", RE.MATCH_CASEINDEPENDENT);
            re_getLines     = new RE("\r\n|\n|\r");
            re_areDigits    = new RE("^[:digit:]+$");
            re_is_pdb_code  = new RE("^[:digit:][:alnum:]{3}$");
            re_stripHtml    = new RE("<.*?>");

        } catch ( RESyntaxException e) {
            General.showError("Code error: RESyntaxException in parseDouble etc.:" + e.toString() );
        }
//        fieldPositionDummy =  new FieldPosition(0);
        /** Using new regexp in standard java 1.4...
         */

        // New style using standard Java API
        try {
            p_whitespace  = Pattern.compile("\\s");
            p_parseDouble = Pattern.compile("[DF]", Pattern.CASE_INSENSITIVE);
            m_parseDouble = p_parseDouble.matcher("");
            m_whitespace  = p_whitespace.matcher("");
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);
        }
    }

    public static String dos2unix(String text) {
        return EOL_DOS.matcher(text).replaceAll("\n");
    }
    public static String unix2dos(String text) {
        return EOL_UNIX.matcher(text).replaceAll("$1\r\n");
    }
    public static String mac2unix(String text) {
        return EOL_MAC.matcher(text).replaceAll("\n");
    }

    /**
     *Function behaves as expected. Works even if one of the arrays has length zero.
     */
    public static String[] append(String[] a, String[] b) {
        int newLength = a.length+b.length;
        if ( newLength==0) {
            General.showWarning("In Objects.append found a new length of zero");
            return null;
        }

        String[] result = new String[a.length+b.length];
        System.arraycopy(a,0,result,0,a.length);
        System.arraycopy(b,0,result,a.length,b.length);
        return result;
    }

    /** Returns null on error or the line number and character number of position in the txt.
     *The line number starts and the character number both start at zero.
     *
     */
    public static int[] getEndPosition( String txt ) {
        int[] result = new int[2];
        StringReader sr = new StringReader( txt );
        LineNumberReader lnr = new LineNumberReader(sr);
        //int lineNumber = 0;
        String l = null;
        try {
            String line = lnr.readLine();
            while ( line != null ) {
                l = line;
                line = lnr.readLine();
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }

        if ( l == null ) { // No line read meaning text was all empty
            result[0] = 0;
            result[1] = 0;
            return result;
        }
        result[0] = lnr.getLineNumber()-1;
        result[1] = l.length();
        return result;
    }

    /** all but the positionEnd character number are inclusive.
     */
    public static String getBlock( String txt, int[] positionBegin, int[] positionEnd ) {
        StringReader sr = new StringReader( txt );
        LineNumberReader lnr = new LineNumberReader(sr);
        StringBuffer result = new StringBuffer();
        int lineNumber = 0;
        try {
            String line = lnr.readLine();
            while ( line != null ) {
                if ( (lineNumber >= positionBegin[0])  &&
                     (lineNumber <= positionEnd[0]) ) {
                    // consider (fragment of) line
                    int idxBegin = 0;
                    int idxEnd = line.length();
                    if ( lineNumber == positionBegin[0] ) {
                        idxBegin = positionBegin[1];
                    }
                    if ( lineNumber == positionEnd[0] ) {
                        idxEnd = positionEnd[1];
                    }
                    result.append(line.substring(idxBegin,idxEnd));
                    if ( idxEnd == line.length()) {
                        result.append(General.eol);
                    }
                }
                lineNumber = lnr.getLineNumber();
                line = lnr.readLine();
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return result.toString();
    }



    /** Must be a very fast routine. Returns a non-redundant String of given size containing
     * only spaces.
     */
    public static String getSpacesString(int size) {
        if ( size < spaceStringList.length ) {
            return spaceStringList[size]; // hardcoded list of unique objects is much faster.
        }
        return createStringOfXTimesTheCharacter(' ',size);
    }

    /** Must be a very fast routine. Provide a empty or filled StringBuffer to reuse.
     *Reusing the stringbuffer does not always save time or memory.
     *If colContainsAQuotedValue is set then unquoted values will leave some space e.g.
     *
<PRE>
"bla die bla"
 real value
'bla bla'
"1.0 e06"
 2.0e07
</PRE>
     *Note:
     *  - that numbers are usually not quoted and right aligned.
     *  - routine assumes caller already checked if the text is of target length
     *      already and the text is not null and has at least 1 character.
     */
    public static boolean growToSize(String text, boolean leftAlign,
            char[] charsReusable, boolean colContainsAQuotedValue ) {

        //sbReusable.setLength(maxSizeElementsCol);
        Arrays.fill(charsReusable,' ');
        /**
        // No padding needed which is common so optimize outside this routine!
        if ( maxSizeElementsCol==text.length() ) {
            sbReusable.append(text);
            return true;
        }
         */
        boolean isQuoted = false;
        char c = text.charAt(0);
        if ( (c == '"') || (c == '\'') ) {
            isQuoted = true;
        }
        //int spacesStringLength = charsReusable.length-text.length();
        /** Four cases possible
         *                  Value quoted
         *                  T   F
         *Column quoted T   1   2
         *              F   3   4
         *Situation 1   - no special handling needed
         *          2   - padd extra space
         *          3   - error (goes uncaught)
         *          4   - see 1.
         */
        boolean specialPaddingNeeded = false;
        if ( colContainsAQuotedValue && (!isQuoted) ) {
            specialPaddingNeeded = true;
            //spacesStringLength -= 2;
        }
        int dstBegin = 0;

        //String emptySpaces = getSpacesString( spacesStringLength );
        if ( leftAlign ) {
            if ( specialPaddingNeeded ) {
                dstBegin = 1;
            }
        } else {
            dstBegin = charsReusable.length-text.length();
            if ( specialPaddingNeeded ) {
                dstBegin--;
            }
        }
        text.getChars(0, text.length(), charsReusable, dstBegin);
        return true;
    }

    public static boolean equalsIgnoreWhiteSpace( String s1, String s2) {
        String s1Mod = s1.replaceAll("\\s","");
        String s2Mod = s2.replaceAll("\\s","");
//        General.showDebug("s1Mod: " + s1Mod);
//        General.showDebug("s2Mod: " + s2Mod);
        return s1Mod.equals(s2Mod);
    }

    /** From textual representation of single to multiple lines.
    public static ArrayList getLines( String txt ) {

        ArrayList lines = new ArrayList();

        RE re=null;
        try {
            re = new RE("\n|\r|\r\n", RE.MATCH_SINGLELINE);
        } catch ( RESyntaxException e) {
            General.showThrowable(e);
            General.showOutput("Code error: setStrings" + e.toString() );
            System.exit(1);
        }
        int line_start  = 0;
        int txt_len     = txt.length();

        while ( line_start < txt_len ) {
            General.showOutput("line_start: [" + line_start + "]" );
            boolean matched = re.match( txt, line_start );
            if ( ! matched ) {
                String line = txt.substring( line_start, txt_len );
                General.showOutput("Line: [" + line + "]" );
                lines.add( line );
                break;
            }
            int startWholeExpr  = re.getParenStart(0);
            int endWholeExpr    = re.getParenEnd(0);
            String line = txt.substring( line_start, startWholeExpr );
            General.showOutput("Line: [" + line + "]" );
            lines.add( line );
            line_start = endWholeExpr + 1;
        }

        return lines;
    }

     */

    /** Probably in api already somewhere? */
    public static String getClassName( Object o ) {
        return o.getClass().getName();
    }


    /** Concatenates the elements from a and b
     */
    public static String[] join( String[] a, String[] b) {
        String[] c = new String[a.length+b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /** Append space to a string in order to fill up the length asked for
     */
    public static String makeStringOfLength( String in, int length ) {
        char[] c = new char[length];
        Arrays.fill(c, ' ');
        StringBuffer sb = new StringBuffer(new String(c));
        sb.insert(0,in);
        return sb.toString();
    }

    /** TODO: analyze and fix the inconsistency here of 1 being true and
     *0 being true at Defs.possibleWaysToSayYesLowerCaseArray
     */
    public static boolean parseBoolean( String in ) {
        if ( in == null ) {
            return false;
        }
        if ( in.equalsIgnoreCase("T") ||
             in.equalsIgnoreCase(Defs.STRING_TRUE) ||
             in.equalsIgnoreCase("yes") ||
             in.equalsIgnoreCase("y") ||
             in.equalsIgnoreCase("1")
             )  {
            return true;
        }
        return false;
    }

    /** Probably in api already somewhere? Inspired by Perl's chomp.
     *This one cuts off any leading and trailing white spaces.
     *Obsolete; use String.trim().
     *@see java.lang.String#trim
     */
    public static String chomp( String in ) {
        if ( in == null ) {
            return null;
        }
        //in = in.replaceAll("^\\s+(.*)\\s+$","\1"); wouldn't work
        in = in.replaceAll("^\\s+","");
        in = in.replaceAll("\\s+$","");
        return in;
    }

    /** Probably in api already somewhere? Inspired by Perl's chomp.
     *This one cuts off any leading and trailing white spaces.
     */
    public static void chomp( ArrayList in ) {
        for (int i=0;i<in.size();i++) {
            in.set(i, ((String) in.get(i)).trim());
        }
    }
    /** Probably in api already somewhere? Just like Perl chomp.
     */
    public static boolean changeNullsToEmpties( String[][] in ) {
        int rows = in.length;
        int columns = in[0].length;

        for (int c=0;c<columns;c++) {
            for (int r=0;r<rows;r++) {
                if ( in[r][c] == null ) {
                    in[r][c] = "";
                }
            }
        }
        return true;
    }

    /** Probably in api already somewhere? */
    public static String stripSingleQuotes( String in ) {
        if ( (in.charAt(0) != '\'' ) || (in.charAt(in.length()-1) != '\'' ) ) {
            General.showWarning("Outside characters aren't single quotes so didn't strip them: ["+in+"]");
            return in;
        }
        if ( in.length() <=2 ) {
            return "";
        }
        return in.substring(1,in.length()-1);
    }

    /** From textual representation of single to multiple lines.
     */
    public static String createStringOfXTimesTheCharacter ( char c, int count ) {
        /** Slow implementation:
        StringBuffer sb = new StringBuffer( count );
        for (int i=0;i<count;i++) {
            sb.append(c);
        }
         */
        char[] stringie = new char[ count ];
        Arrays.fill( stringie, c );
        String s = new String( stringie );
        return s;
    }


    /** From textual representation of single to multiple lines.
     */
    public static ArrayList getLines( String txt ) {

        String[] lines = Strings.re_getLines.split( txt );

        ArrayList al = new ArrayList();
        al.addAll( Arrays.asList( lines ) );

        return al;
    }

    /** Returns the first word of a line as separated by a space. Doesn't
     *work with tabs etc. for speed reasons. Single space separation logic only.
     */
    public static String getFirstWord( String line ) {
        int pos = line.indexOf(" ");
        if ( pos == -1 ) {
            return line;
        }
        return line.substring(0, pos);
    }

    /** Returns the sentence without the first word of a line as separated by a space. Doesn't
     *work with tabs etc. for speed reasons. Single space separation logic only.
     *If there is only 1 word then the empty string is returned.
     */
    public static String stripFirstWord( String line ) {
        int pos = line.indexOf(" ");
        if ( pos == -1 ) {
            return "";
        }
        return line.substring(pos+1).trim();
    }

    /** Returns the second word of a line as separated by a space. Doesn't
     *work with tabs etc. for speed reasons. Single space separation logic only.
     */
    public static String getSecondWord( String line ) {
        int pos = line.indexOf(" ");
        if ( pos == -1 ) {
            return line;
        }
        int pos2 = line.indexOf(" ", pos+1);
        if ( pos2 == -1 ) {
            if ( (pos+1)<line.length()) {
                return line.substring(pos+1);
            } else {
                return line;
            }
        }
        return line.substring(pos, pos2);
    }

   /** Splits input on ',' and converts to integer values.
     */
    public static int[] splitWithAllReturnedIntegers( String txt, char delim ) {
        String[] strs = Strings.splitWithAllReturned( txt, delim );
        if ( strs == null ) {
            General.showError("Failed to parse string into char seperated string values" );
            General.showError("Input: ["+txt+"]");
            return null;
        }
        int[] result = new int[strs.length];
        try {
            for (int i=0;i<strs.length;i++) {
                result[i] = Integer.parseInt(strs[i]);
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return null;
        }
        return result;
    }

    /** Split each string in the list and append it.
     */
    public static String[] splitAllNoEmpties( String[] txt, String regexp) {
        if ( txt == null ) {
            return null;
        }
        StringArrayList tmp = new StringArrayList();
        for (int i=0;i<txt.length;i++) {
            String t = txt[i];
            String[] r = t.split(regexp);
            StringArrayList s = new StringArrayList( Arrays.asList(r));
            for (int j=s.size()-1;j>=0;j--) {
                String rr = s.getString(j);
                if ( rr.length() ==0) {
                    s.remove(j);
                }
            }
            tmp.addAll( s );
        }
        return tmp.toStringArray();
    }

    /** Return elements even if they're empty. So:
     *a|b|c -> "a", "b", "c" and
     *||    -> "", "", "" if | is the delimiter.
     *Returns a list with 1 empty string when the input is an empty string.
     */
    public static String[] splitWithAllReturned( String txt, char delim ) {

        if ( txt == null ) {
            return null;
        }

        int txt_length = txt.length();
        ArrayList al = new ArrayList();
        // Add 1 empty string.
        if ( txt_length == 0 ) {
            al.add( "" );
        }

        int start_position_look = 0;
        while ( start_position_look < txt_length ) {
            int delim_position = txt.indexOf(delim, start_position_look);
            //General.showOutput("Looking from position: " + start_position_look + " found: " + delim_position);
            if ( delim_position != -1 ) {
                // New delimiter found
                if ( start_position_look != delim_position ) {
                    al.add( txt.substring( start_position_look, delim_position ));
                } else {
                    al.add( "" );
                }
                start_position_look = delim_position + 1;
                /** Add another element if the delim is found as the last
                 *character in the sequence.
                 */
                if ( start_position_look == txt_length ) {
                    al.add( "" );
                }
            } else {
                // No new delimiter found; exit point
                al.add( txt.substring( start_position_look ));
                break;
            }
        }
        String[] word_list = new String[ al.size() ];
        for (int i=0;i<al.size();i++) {
            word_list[i] = (String) al.get(i);
        }
        return word_list;
    }

    /** Calls substring method for each element if the element is at least
     *that long.
     */
    public static void doSubstr( String[] list, int startIdx, int endIdx) {
        for (int i=0;i<list.length;i++) {
            if ( list[i] != null && list[i].length() > startIdx ) {
                list[i]=list[i].substring(startIdx,endIdx);
            } else {
                if ( list[i] != null ) {
                    General.showWarning("String: " + list[i] + " is not long enough for doSubstr with startIdx: " + startIdx);
                }
            }
        }
    }

    /** Calls substring method for each element if the element is at least
     *that long.
     */
    public static void doSubstr( String[] list, int startIdx) {
        for (int i=0;i<list.length;i++) {
            if ( list[i] != null && list[i].length() > startIdx ) {
                list[i]=list[i].substring(startIdx);
            } else {
                if ( list[i] != null ) {
                    General.showWarning("String: " + list[i] + " is not long enough for doSubstr with startIdx: " + startIdx);
                }
            }
        }
    }

    /** Changes a '.' value to Defs.NULL_STRING_NULL
     */
    public static void dot2Null( String[] list ) {
        for (int i=list.length-1;i>=0;i--) {
            String v = list[i];
            if ( v == null ) {
                continue;
            }
            /**
            if ( (v.length() == 0 ) ||
                ((v.length() == 1) && (v.charAt(0) == '.') )) {
                list[i] = Defs.NULL_STRING_NULL;
            }
             */
            // faster method and may be optimized by jvm implementation.
            if ( v.equals(".")) {
                list[i] = Defs.NULL_STRING_NULL;
            }
        }
    }

    /** Changes a '.' value to Defs.NULL_STRING_NULL
     */
    public static void dot2Null( String[][] lol ) {
        for (int i=lol.length-1;i>=0;i--) {
            dot2Null( lol[i] );
        }
    }


    /** Very simplistic wrapper. Doesn't consider spaces to be special. Doesn't
     *account for tabs, eols etc. Use freely for that's what it's worth
     */
    public static String wrapToMarginSimple( String input, int margin ) {
        if ( input == null ) {
            return EOL;
        }
        if ( input.length() <= margin ) {
            return input + EOL;
        }

        StringBuffer sb = new StringBuffer();
        int startIndex = 0;
        int endIndex = 0;
        while ( startIndex < input.length()) {
            endIndex = startIndex + margin;
            if ( endIndex > (input.length() -1) ) {
                sb.append( input.substring( startIndex ) );
            } else {
                sb.append( input.substring( startIndex, endIndex ) );
            }
            sb.append( EOL );
            startIndex += margin;
        }
        return sb.toString();
    }
    /** From textual representation of single to multiple lines.
    public static ArrayList getLines( String txt ) {

        RE re = null;
        try {
            re = new RE("\r\n|\n|\r");
        } catch ( RESyntaxException e) {
            General.showThrowable(e);
            General.showOutput("Code error: getLines" + e.toString() );
            System.exit(1);
        }
        String[] lines = re.split( txt );

        ArrayList al = convertRegularArrayToArrayList( lines );
        return al;
    }
     */

    /** Concatenate an array of strings to one string.
     * If the input is empty; an empty string will be returned and a warning will be
     * issued. Now uses the stringbuffer for speed...
     * @param str input
     * @param delim delimiter
     * @return Concatenation of input.
     */
    public static String concatenate( Object str[], String delim ) {
        if ( str == null ) {
            return null;
        }
        StringBuffer sb = new StringBuffer( EMPTY_STRING );
        if ( str.length == 0 ) {
            General.showWarning("concatenate got empty input");
        }
        for ( int i=0;i<str.length;i++ ) {
            String next = (String) str[i];
            //General.showOutput("Concatenate ["+next+"]");
            sb.append(next);
            if ( i!=str.length-1 ) {
                sb.append(delim);
            }
        }
        return ( sb.toString() );
    }

    /** see similar method for description
     */
    public static String toWord( String input ) {
        return toWord( input, '_' );
    }

    /** Replace any white space with another character turning the string into a word (
     *if used right). Use an underscore character as the standard
     *replacement.
     */
    public static String toWord( String input, char replacementForWhiteSpace ) {
        if ( (input == null) || (input.length() == 0)) {
            return input;
        }

        String result = input;
        result = p_whitespace.matcher(input).replaceAll(String.valueOf(replacementForWhiteSpace));
        return result;
    }

    /** Delete any white space.
     */
    public static String deleteAllWhiteSpace( String input ) {
        if ( (input == null) || (input.length() == 0)) {
            return input;
        }

        String result = p_whitespace.matcher(input).replaceAll("");
        return result;
    }

    /** Operate on a native array of strings. */
    public static String[] deleteAllWhiteSpace( String[] input ) {
        for (int i=0;i<input.length;i++ ) {
            input[i] = deleteAllWhiteSpace(input[i]);
        }
        return input;
    }

    /** Operate on a native array of array of strings. */
    public static String[][] deleteAllWhiteSpace( String[][] input ) {
        for (int i=0;i<input.length;i++ ) {
            input[i] = deleteAllWhiteSpace(input[i]);
        }
        return input;
    }

   /** see similar method for description
     */
    public static String breakWord( String input ) {
        return breakWord( input, '_' );
    }

    /** Replace character with space turning the string into a bunch of words (
     *if used right). Look for the underscore character as the standard
     *replacement.
     */
    public static String breakWord( String input, char replacementForWhiteSpace ) {
        if ( (input == null) || (input.length() == 0)) {
            return input;
        }

        return input.replace(replacementForWhiteSpace, ' ');
    }

    /** Generates attributes for the properties like needed in a table
     For all attributes it holds that the value must be quoted in the string
     itself if needed. E.g. Use "2" and "\"two\"".
     */
    public static String toHtml( Properties p ) {
        StringBuffer sb = new StringBuffer();
        Set keys = p.keySet();
        for (Iterator i=keys.iterator();i.hasNext();) {
            String key = i.next().toString();
            sb.append( key + "=" + p.getProperty(key) );
            if ( i.hasNext() )
                sb.append(" ");
        }
        return(sb.toString());
    }

    /** Assumes very little quoting needs to be done;
     *for now NONE. Assumes the average word length is 10 for efficiency.
     */
    public static String toCsv( String[][] in ) {
        int rows = in.length;
        int columns = in[0].length;
        StringBuffer sb = new StringBuffer(rows*columns*10);
        for (int r=0;r<rows;r++) {
            sb.append( in[r][0] );
            for (int c=1;c<columns;c++) {
                sb.append( ',' );
                sb.append( in[r][c] );
            }
            sb.append( '\n' );
        }
        String result = sb.toString();
        int expectedCount = rows*(columns-1);
        int count = countChars(result, ',');
        if ( expectedCount != count ) {
            General.showError("expectedCountComma ("+expectedCount+") != countComma ("+count+")");
            General.showError("Improve routine Wattos.Utils.Strings.toCsv");
        return null;
        }
        //General.showDebug("expectedCountComma ("+expectedCount+") == countComma ("+count+")");
        expectedCount = rows; // no eol before eof
        count = countChars(result, '\n');
        if ( expectedCount != count ) {
            General.showError("expectedCountEol ("+expectedCount+") != countEol ("+count+")");
            General.showError("Improve routine Wattos.Utils.Strings.toCsv");
        return null;
        }
        //General.showDebug("expectedCountEol ("+expectedCount+") == countEol ("+count+")");
        return sb.toString();
    }


    /** Reinterprets a string from 16 bit to 16 bit but doing the
     *conversion on each 2 bytes using the US-ASCII encoding scheme.
     */
    public static String toASCII( String input ) {
        //General.showWarning("converting to basic ASCII encoding");
        String enc = "US-ASCII";
        String output = null;
        //enc = "ISO-8859-1";
        try {
            output = new String( input.getBytes(enc) );
        } catch ( UnsupportedEncodingException e ) {
            General.showThrowable(e);
        }

        return output;
    }


    /** Compares two strings in the ASCII encoding. E.g.
     *<PRE>
     * "u",      "?" : false
     *</PRE>
     *Note that the question mark is special because any non ASCII character
     *will be mapped to it and then the comparison is done.
     */
    public static boolean areASCIISame( String one, String two ) {
        String one_ascii = toASCII( one );
        String two_ascii = toASCII( two );
        if ( one_ascii.equals(two_ascii) ) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean writeToFile( String text, String filename ) {
        if ( text == null )
            return false;
        try {
            File f = new File( filename );
            if ( f.exists() ) {
                //General.showOutput("WARNING: overwriting original file:" + f.getCanonicalFile());
                f.delete();
            }
            PrintWriter out = new PrintWriter( new FileWriter(f) );
            out.print( text );
            out.close();
        } catch (IOException e) {
            General.showThrowable(e);
            return false;
        }
        return true;
    }


    /** Looks to see if the string contains only digits.     */
    public static boolean areDigits( String chk_string  ) {
        return re_areDigits.match( chk_string );
    }


    /** Checks if the string could be a pdb id like "1brv".
     * See regular expression in the code.
     * @param chk_string input
     * @return <CODE>true</CODE> if it is valid pdb code.
     */
    public static boolean is_pdb_code( String chk_string ) {
        boolean matched = re_is_pdb_code.match( chk_string );
        if ( matched )
            return true;
        else
            return false;
    }

    /** Checks if the string could be a pdb id like "1brv".
     * See regular expression in the code.
     * @return <CODE>true</CODE> if it is valid pdb code.
     */
    public static boolean is_bmrb_code( String bmrb_id_str ) {
        int bmrb_id = -1;
        try {
            bmrb_id = Integer.parseInt( bmrb_id_str );
        } catch ( Throwable t ) {
        }
// f&*k!!!
//        if ( (bmrb_id < 1 ) || ( bmrb_id > 9999) ) {
        if ( bmrb_id < 1 ) {
            return false;
        }
        return true;
    }

    public static boolean isPdbEntryLoL( String[][] lol ) {
        for (int r=lol.length-1;r>=0;r--) {
            String[] list = lol[r];
            if ( list == null ) {
                continue;
            }
            for (int c=list.length-1;c>=0;c--) {
                if ( ! is_pdb_code( list[c])) {
                    General.showDebug("Isn't a pdb code: " + list[c]);
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean isPdbEntryList( String[] list) {
        for (int i=list.length-1;i>=0;i--) {
            if ( ! is_pdb_code( list[i])) {
                General.showDebug("Isn't a pdb code: " + list[i]);
                return false;
            }
        }
        return true;
    }


    /** Returns null if not a valid pdb code can be returned
     * Accepted formats are:
     * 1brv.pdb
     * /s/1brv.ent
     * S:\\1brv.PDB
     * pdb1brv.ent
     * etc.
     */
    public static String getPDBEntryCodeFromFileName( String fn ) {
        if ( fn == null ) {
            return null;
        }

        String regExp = ".*?(.{4})\\.(ent|pdb|txt|)";
        Pattern p = null;
        try {
            p = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return null;
        }
        Matcher m = p.matcher(fn);

        if ( ! (m.matches())) {
            General.showWarning("Failed to get a valid pdb id from file name because missmatch with expression: " + regExp);
            return null;
        }
        String result = m.group(1);
        result = result.toLowerCase();
        if ( is_pdb_code( result ) ) {
            return result;
        }
        General.showWarning("Failed to get a valid pdb id from file name because match isn't a real pdb code: " + result);
        return null;
    }

    /** Get a line of input from System.in after asking the prompt as given.
     * Error status is indicated by return value being null
     * @param prompt Prompt the user with this string.
     * @return The string that the user gave.
     */
    public static String getInputString( String prompt )
    {
        if ( prompt == null ) {
            prompt = "";
        }
        //General.showOutput("Now in: getInputString( String prompt )");
        String reply;

        // This bufferedreader is set to read Standard Input (keyboard)
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        General.showOutputNoEol(prompt );
        try {
            reply = in.readLine();
            // DON'T close the stream even though you think you're done with
            // it. This causes errors of unknown nature to JFD.
            //in.close();
        } catch (IOException e) {
            General.showThrowable(e);
            reply = null;
        }
        if ( reply == null ) {
            General.showError( "Reading input from console.");
            General.showError( "Prompt was: [" + prompt +"]");
        } else {
            reply = reply.replaceAll("^\\s+","");
            reply = reply.replaceAll("\\s+$","");
        }
//        General.showDebug("Read a reply: [" + reply + "]");
        return reply;
    }

    /** Convert a properties class to a string by calling it's store method.
     *Will return null on error. Will return "{}" string if no properties
     *have been defined.
     *Counterpart of next method.
     */
    public static String getProperties( Properties p )
    {
        String result = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            p.store(baos, null);
            baos.close();
            result = baos.toString();
        } catch ( IOException e ) {
            General.showThrowable(e);
        }
        return result;
    }

    /** Convert a properties class to a string by calling it's toString method.
     *Will return null on error. Will return empty string if no properties
     *have been defined. Because the values will be used in a csv formatted
     *file the comma's will be replaced by a semi colon.
     *Counterpart of next method.
     */
    public static String getPropertiesNoBrackets( Properties p )
    {
        String result = p.toString();
        /** Strip brackets */
        result = result.substring(1,result.length()-1);
        result = result.replace(',',';');
        return result;
    }

    /** Convert a properties class to a string by calling it's toString method.
     *Counterpart of get method. Note that it's allowed to pass a null or empty
     *String.
     */
    public static Properties setProperties( String input_properties )
    {
        Properties result = new Properties();

        if ( input_properties == null || input_properties.length() == 0 ) {
            return result;
        }

        byte[] ba = input_properties.getBytes();
        ByteArrayInputStream bais = new ByteArrayInputStream( ba );

        try {
            result.load(bais);
        } catch ( IOException e ) {
            General.showThrowable(e);
        }
        return result;
    }

    /** Get a line of input from System.in after asking the prompt as given.
     * Error status is indicated by return value being null. Note that
     * the InOut.END_OF_FILE_ENCOUNTERED value will be returned if that's the case.
     * Will remove leading and trailing whitespace.
     * @param prompt Prompt the user with this string.
     * @param ui TODO
     * @return The string that the user gave.
     */
    public static String getInputString( BufferedReader in, String prompt, UserInterface ui )
    {
        if ( prompt == null ) {
            prompt = "";
        }
        if ( in == null ) {
            General.showError("Given BufferedReader in getInputString was null");
            return null;
        }
        //General.showOutput("Now in: getInputString( BufferedReader in, String prompt )");
        String reply;
    // This bufferedreader is set to read Standard Input (keyboard)
        // There is a problem with reading multiple lines
        // Prompt
//        if ( ui == null || ui.interactiveSession ) {
//            General.showOutput( prompt );
//        }
        try {
            reply = in.readLine();
            if ( reply == null ) {
                reply = InOut.END_OF_FILE_ENCOUNTERED;
            }
        } catch (IOException e) {
            General.showThrowable(e);
            reply = null;
        }
        if ( reply == null ) {
            General.showError( "Reading input from BufferedReader.");
            General.showError( "Prompt was: [" + prompt +"]");
            return null;
        }
        reply = reply.trim();
//        General.showDebug("getInputString read: " + reply);
        return reply;
    }


    /** Get a line of input from System.in
     * Error status is indicated by return value being null
     * Only values that will be allowed as input are present in the array
     * of possible values. The routine will repeat until one of the expected
     * values has been entered.
     * @param prompt Prompt the user with this string. The prompt will be appended
     * with the possible answers.
     * @param allowed_strings Allowed answers.
     * @param ui TODO
     * @return The string that the user gave and exists in the
     * allowed_strings.
     */
    public static String getInputString( String prompt, ArrayList allowed_strings )
    {
        if ( prompt == null ) {
            prompt = "";
        }
        //General.showOutput("Now in: getInputString( String prompt, ArrayList allowed_strings )");
        // Prevent an infinite loop
        int max_prompts = MAX_PROMPTS;
        int prompt_id = 0;

        // The following should not be one of the allowed strings
        String result="bogus_very_unlikely_to_be_entered";
        Object[] strings = allowed_strings.toArray();
        String allows=concatenate( strings, "/");

        while ( (prompt_id < max_prompts ) && ! allowed_strings.contains(result) ) {
            result = getInputString( prompt + " ("+allows+"): " );
            prompt_id++;
        }
        General.showDebug("getInputString read: " + result);

        return result;
    }


    /** Get a line of input from System.in
     * Error status is indicated by return value being null
     * Only values that will be allowed as input are present in the array
     * of possible values. The routine will repeat until one of the expected
     * values has been entered.
     * @param prompt Prompt the user with this string. The prompt will be appended
     * with the possible answers.
     * @param allowed_strings Allowed answers.
     * @param ui TODO
     * @return The string that the user gave and exists in the
     * allowed_strings.
     */
    public static String getInputString( BufferedReader in, String prompt, ArrayList allowed_strings, UserInterface ui )
    {
        if ( prompt == null ) {
            prompt = "";
        }
        // Prevent an infinite loop
        int max_prompts = MAX_PROMPTS;
        int prompt_id = 0;
        // The following should not be one of the allowed strings
        String reply="bogus";
        Object[] strings = allowed_strings.toArray();
        String allows=concatenate( strings, "/");
        while ( (prompt_id < max_prompts ) && ! allowed_strings.contains(reply)) {
            reply = getInputString( in, prompt + " ("+allows+"): ", ui );
            prompt_id++;
        }
        return reply;
    }

    /** Get a boolean input from System.in
     * There is no way to catch errors.
     * In case of error the value returned is false, so schedule action safely
     * to that specification.
     * @param prompt Prompt the user with this string. The prompt will be appended
     * with the possible answers (y/n).
     * @return <CODE>true</CODE> if user answered 'y'.
     */
    public static boolean getInputBoolean( String prompt ) {
        if ( prompt == null ) {
            prompt = "";
        }
        //General.showOutput("Now in: getInputBoolean( String prompt )");
        ArrayList allows = new ArrayList();
        allows.add("y");
        allows.add("n");
        String reply = getInputString( prompt, allows );
        //General.showOutput("Reply :[" + reply + "]");

        if (( reply == null ) || (reply == InOut.END_OF_FILE_ENCOUNTERED)) {
            General.showError( "in: getInputBoolean\nAssuming answer was false");
            return false;
        }

        boolean result = true;
        if ( ! reply.equals("y") ) {
            result = false;
        }
        General.showDebug("getInputChar read: " + result);
        return result;
    }

    /** Get a boolean input from System.in
     * There is no way to catch errors.
     * In case of error the value returned is false, so schedule action safely
     * to that specification.
     * @param prompt Prompt the user with this string. The prompt will be appended
     * with the possible answers (y/n).
     * @return <CODE>true</CODE> if user answered 'y'.
     */
    public static boolean getInputBoolean( BufferedReader in, String prompt ) {
        if ( prompt == null ) {
            prompt = "";
        }
        //General.showOutput("Now in: getInputBoolean( BufferedReader in, String prompt )");
        ArrayList allows = new ArrayList();
        allows.add("y");
        allows.add("n");
        String reply = getInputString( in, prompt, allows, null );

        if (( reply == null ) || (reply == InOut.END_OF_FILE_ENCOUNTERED)) {
            General.showError( "in: getInputBoolean\nAssuming answer was false");
            General.showError( "prompt was: [" + prompt + "]");
            return false;
        }
        boolean result = true;
        if ( ! reply.equals("y") ) {
            result = false;
        }
        General.showDebug("getInputChar read: " + result);
        return result;
    }

    /** Get a boolean input from System.in
     * There is no way to catch errors.
     * In case of error the value returned is false, so schedule action safely
     * to that specification.
     * @param prompt Prompt the user with this string. The prompt will be appended
     * with the possible answers (y/n).
     * @param ui TODO
     * @return <CODE>true</CODE> if user answered 'y'.
     */
    public static char getInputChar( BufferedReader in, String prompt, UserInterface ui ) {
        if ( prompt == null ) {
            prompt = "";
        }
        String reply = getInputString( in, prompt, ui );

        if (( reply == null ) || (reply == InOut.END_OF_FILE_ENCOUNTERED)) {
            General.showError( "in: getInputChar\nAssuming answer was a INVALID_CHAR_FOR_ANSWER");
            return INVALID_CHAR_FOR_ANSWER;
        }

        if ( reply.length() == 0 ) {
            General.showError( "in: getInputChar; empty input.\nAssuming answer was a INVALID_CHAR_FOR_ANSWER");
            return INVALID_CHAR_FOR_ANSWER;
        }

        char result = reply.charAt(0);
        if ( reply.length() > 1 ) {
            General.showWarning("in: getInputChar; input was longer than 1\nTruncated answer to: " + result);
        }
        General.showDebug("getInputChar read: " + result);
        return result;
    }


    /** Get a int input from System.in
     * @param prompt Prompt the user with this string.
     * @param ui TODO
     */
    public static int getInputInt( BufferedReader in, String prompt, UserInterface ui ) {

        if ( prompt == null ) {
            prompt = "";
        }
        // Prevent an infinite loop
        int max_prompts = MAX_PROMPTS;
        int prompt_id = 0;

        int result = 999;

        boolean done = false;
        while ( (prompt_id < max_prompts ) && ! done ) {
            String reply = getInputString( in, prompt, ui );
            if (( reply == null ) || (reply == InOut.END_OF_FILE_ENCOUNTERED)) {
                continue;
            }
            try {
                result = Integer.parseInt(reply);
                done = true;
            } catch ( NumberFormatException e ) {
                General.showOutput("NumberFormatException\n" + e.toString() );
            }
            prompt_id++;
        }
        return (result);
    }

    /** Get a float input.
     * @param prompt Prompt the user with this string.
     * @param ui TODO
     */
    public static float getInputFloat( BufferedReader in, String prompt, UserInterface ui ) {

        if ( prompt == null ) {
            prompt = "";
        }
        // Prevent an infinite loop
        int max_prompts = MAX_PROMPTS;
        int prompt_id = 0;

        float result = 999;

        boolean done = false;
        while ( (prompt_id < max_prompts ) && ! done ) {
            String reply = getInputString( in, prompt, ui );
            if (( reply == null ) || (reply == InOut.END_OF_FILE_ENCOUNTERED)) {
                continue;
            }
            try {
                result = Float.parseFloat(reply);
                done = true;
            } catch ( NumberFormatException e ) {
                General.showOutput("NumberFormatException\n" + e.toString() );
            }
            prompt_id++;
        }
        General.showDebug("getInputFloat read: " + result);
        return result;
    }


    /** Replaces text with other text based on a Properties mapping.
     *The strings may include regular expressions.
     *Order of substitutions shouldn't matter.
     */
    public static String replaceMulti( String input, Properties subs ) {

        String result = input;
        Set keys = subs.keySet();
        for (Iterator i=keys.iterator();i.hasNext();) {
            String key = (String) i.next();
            result = replace(result, key, subs.getProperty(key));
        }
        return(result);
    }

    /** Replaces a match of a regular expression with a given string.
     If no matches of the regular expression then the string will be
     returned unchanged. If the regular expression doesn't compile
     *null is returned.
     */
    public static String replace( String input, String in, String out ) {
        RE re=null;
        try {
            re = new RE(in);
        } catch ( RESyntaxException e) {
            General.showOutput("Code error: RESyntaxException" + e.toString() );
            return null;
        }
        String result = re.subst(input,out);
        return(result);
    }

    /** Replaces a match of a regular expression with a given string.
     If no matches of the regular expression then the string will be
     returned unchanged. If the regular expression doesn't compile
     *null is returned.
     */
    public static boolean replace( String[] list, String in, String out ) {
        Pattern p= null;
        Matcher m= null;
        try {
            p = Pattern.compile(in);
        } catch ( PatternSyntaxException e) {
            General.showOutput("Code error: PatternSyntaxException" + e.toString() );
            return false;
        }
        for (int i=list.length-1;i>=0;i--) {
            //General.showDebug("Seeing if needing to replace on input: " + list[i]);
            if ( list[i] == null ) {
                continue;
            }
            m=p.matcher(list[i]);
            list[i] = m.replaceAll(out);
        }
        return true;
    }


    /** Returns a string without html tags and trimmed for white space chars.
     */
    public static String stripHtml( String input ) {
        // Do multiple substitutions if matches present
        String result = Strings.re_stripHtml.subst(input, EMPTY_STRING);
        result = result.trim();
        return(result);
    }

    /* Setting a new or old parameter pair and removing all pairs with the
     *same parameter name that might have existed.
    public static String setUrlQueryParameterPair( String query,
        String parameter_name, String parameter_value ) {

        String result = query;

        String parameter_pair_expression = "(?|&)" + parameter_name + "=[\s";
        base_query_url = Strings.replace(query, , EMPTY_STRING);
        base_query_url = base_query_url + "&request_type=\"file_set\"";

        return ( result );
    }
     */


    /**Format method is called to output decimal numbers
     * with specified precision for reals (float & double). Need to implement
     *float alternative still.
     */
    public static String formatReal(double d, int precision) {
        Strings.nf.setMaximumFractionDigits(precision);
        Strings.nf.setMinimumFractionDigits(precision);
        return Strings.nf.format(d);
    }

    /** Convenience function that reuses objects.
     */
    public static String sprintf(int v, String format) {
        return Format.sprintf( format, p.add( v ));
    }
    /** Convenience function that reuses objects.
     */
    public static String sprintf(double v, String format) {
        return Format.sprintf( format, p.add( v ));
    }
    /** Convenience function that reuses objects.
     */
    public static String sprintf(float v, String format) {
        return Format.sprintf( format, p.add( v ));
    }
    /** Convenience function that reuses objects.
     */
    public static String sprintf(String v, String format) {
        return Format.sprintf( format, p.add( v ));
    }

    /** Will convert a string to double, even for the case were a d or f
     *was used as the exponent indicator. This routine might need some speed up
     *as regular expressions are slow if many are needed. Alternatively the re
     *could be stored as a class object.
     */
    public static double parseDouble( String svalue ) throws NumberFormatException {
        String new_str = "E";
        // Old style using Apache
        if ( Strings.re_parseDouble.match(svalue) ) {
            svalue = Strings.re_parseDouble.subst(svalue,new_str);
        }
        /** New style
        if ( svalue.matches( Strings.p_parseDouble..match(svalue) ) {
            svalue = Strings.re_parseDouble.subst(svalue,new_str);
        }
         */

        double dvalue = Double.parseDouble(svalue);
        return dvalue;
    }

    /**
     * Turns array of bytes into string representing each byte as
     * a two digit unsigned hex number.
     *
     * @param hash Array of bytes to convert to hex-string
     * @return  Generated hex string
     * Originally authored by Santeri Paavolainen, Helsinki Finland 1996
     */
    public static String toHex(byte hash[]){
        if ( hash == null ) {
            return "null";
        }
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i=0; i<hash.length; i++){
            int intVal = hash[i] & 0xff;
            if (intVal < 0x10){
                // append a zero before a one digit hex
                // number to make it two digits.
                buf.append("0");
            }
            buf.append(Integer.toHexString(intVal));
        }
        return buf.toString();
    }

    /** slower method
    public static int countChars( String line, char c ) {
        int count = 0;
        if ( line == null ) {
            return 0;
        }
        for (int i=0;i<line.length();i++) {
            char current_char = line.charAt(i);
            if ( c == current_char ) {
                count++;
            }
        }
        return count;
    }
     */

    /** How often does the char occur in the line? REturn -1 on error.
     */
    public static int countChars( String in, char c ) {
        if ( in == null ) {
            return 0;
        }
        int count = 0;
        int idx = in.indexOf(c, 0 );
        while ( idx >= 0 ) {
            count++;
            idx = in.indexOf(c, idx+1 );
        }
        return count;
    }

    /** How often does the regexp match the line? REturn -1 on error.
     */
    public static int countStrings( String line, String regexp_sub_seq ) {
        Pattern p_sub_seq;
        Matcher m_sub_seq;

        try {
            p_sub_seq = Pattern.compile(regexp_sub_seq, Pattern.COMMENTS);
            m_sub_seq = p_sub_seq.matcher("");
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);
            return -1;
        }

        return countStrings( line, m_sub_seq );
    }

    /** How often does the regexp match the line? REturn -1 on error.
     */
    public static int countStrings( String line, Matcher m_sub_seq ) {
        if (line == null) {
            return -1;
        }
        if ( line.length()== 0 ) {
            General.showWarning("Senseless to count from an empty string");
            return 0;
        }

        m_sub_seq.reset(line);
        int count = 0;
        int posStart = 0;
        while ( m_sub_seq.find( posStart ) ) {
            count++;
            posStart = m_sub_seq.end();
            //General.showDebug("Matched group: " + m_sub_seq.group());
        }
        return count;
    }

    /** Count the length of a string if the tabs are expanded as defined.
     *E.g. "a\tbbb\tc\td" will be of length 13 when tabwidth is 4 because the subsequent
     *tabs expand to 3, 1, and 3 characters; + the 6 regular ones makes
     *13.
     */
    public static int countStringLengthWithTabs( String line, int tabwidth ) {
        int cur_pos_with_tabs       = 0;
        int line_length             = line.length();

        if ( line == null || line_length < 1 ) {
            return 0;
        }
        for (int cur_pos=0;cur_pos<line_length;cur_pos++) {
            if ( line.charAt(cur_pos) == '\t' ) {
                cur_pos_with_tabs += tabwidth - ( cur_pos_with_tabs % tabwidth );
            } else {
                cur_pos_with_tabs++;
            }
        }
        return cur_pos_with_tabs;
    }

    /**Same as String.subString but doesn't throw the exceptions when
     *the end index is out of bound.
     */
    public static String substringCertainEnd( String line, int start, int end ) {
        if ( line == null ) {
            return line;
        }
        if ( start >= (line.length() - 1 )) {
            return line;
        }
        if ( end > line.length()) {
            end = line.length();
        }
        return line.substring(start, end);
    }

    /**Assumes the tab width
     *is set to 8 characters. See method of same name.
     */
    public static String substringInterpetTabs( String line, int start, int end ) {
        int tabwidth = 8;
        return substringInterpetTabs( line, start, end, tabwidth );
    }

    /** Detect tab inside the line and get the correct content. Uses String.substring to do
     * the final cut so: the character at the start is included but the character at the
     * end is not. The first column is numbered zero. If a tab is found on the left
     * border then it is excluded from the returned string. A tab on the right border is included.
     *The tab is either repeated or not but not expanded to any number of spaces.
     * Code adapted from Lei Yin.
     * E.g.:
     * <PRE>
        ("1\t2", 0, 2, 4) -> "1\t"
     *  ("1\t2", 1, 2, 4) -> "\t"
     *  ("\n1\t2\t3", 1, 6, 4) -> "\n1\t"
     *</PRE>
     * A selection within a tab region returns the empty string.
     * @param line Input string which can't contain end of line characters.
     * @param start_pos_with_tabs Use 0 for start.
     * @param end_pos_with_tabs Use length of the string or -1 for all characters.
     * @param tabwidth Should usually be 8 (perhaps 4).
     * @return A substring in which tabs might still exist but they
     * have been accounted for by matter of counting.
     */
    public static String substringInterpetTabs( String line, int start_pos_with_tabs, int end_pos_with_tabs, int tabwidth ) {
        int cur_pos_with_tabs       = 0;
        if ( line == null ) {
            return line;
        }

        int line_length             = line.length();
        /** Set the defaults to be matching from beginning to end
         */
        int start_pos               = 0;
        int end_pos                 = line_length;
        boolean foundStart          = false;
        boolean foundEnd            = false;


        if ( line_length < 1 ) {
            return line;
        }

        /** Speed things up by early return */
        if ( start_pos_with_tabs == 0 ) {
            foundStart = true;
        }

        /** Speed things up by early return */
        if ( end_pos_with_tabs == -1 ) {
            // Old code was stupid. No need to count...
            //end_pos_with_tabs = countStringLengthWithTabs(line, tabwidth);
            foundEnd = true;
            // Just to not fail the next test.
            end_pos_with_tabs = start_pos_with_tabs;
        }

        if ( start_pos_with_tabs > end_pos_with_tabs ) {
            General.showError("start > end for pos with tabs: " +
                start_pos_with_tabs + ", " + end_pos_with_tabs);
            General.showError("line in:["+line+"]");
            Error e =new Error();
            General.showThrowable(e);
            return null;
        }

        for (int cur_pos=0;cur_pos<line_length;cur_pos++) {
            /** No need to look for something already found
             */
            if ( foundStart && foundEnd )  {
                break;
            }
            if ( ( ! foundStart ) && ( start_pos_with_tabs <= cur_pos_with_tabs ) ) {
                start_pos = cur_pos;
                foundStart = true;
            }
            if ( ( ! foundEnd )   && ( end_pos_with_tabs   <= cur_pos_with_tabs ) ) {
                end_pos = cur_pos;
                foundEnd = true;
            }
            if ( line.charAt(cur_pos) == '\t' ) {
                cur_pos_with_tabs += tabwidth - ( cur_pos_with_tabs % tabwidth );
            } else {
                cur_pos_with_tabs++;
            }
        }
        // Sanity checks:
        if (    start_pos   < 0 ||
                start_pos   >= line_length ||
                end_pos     < 0 ||
                end_pos     > line_length ||
                start_pos > end_pos ) {
            General.showError("some code bug in routine: substringInterpetTabs");
            General.showError("start_pos_with_tabs      :"+start_pos_with_tabs);
            General.showError("end_pos_with_tabs        :"+end_pos_with_tabs);
            General.showError("start_pos  :"+start_pos);
            General.showError("end_pos    :"+end_pos);
            General.showError("line_length:"+line_length);
            General.showError("line in:["+line+"]");
            Error e =new Error();
            General.showThrowable(e);
            return null;
        }
        // Now take the substring with the real character positions:
        return line.substring( start_pos, end_pos );
    }

    /** Issues an error message saying this class can not be initiated */
    public Strings() {
        General.showWarning("Don't try to initiate Strings class");
    }


    /** Finds the string position or returns -1. Not used; not tested.
     */
    public static int indexOf(String[] values, String value) {
        for (int i=0;i<values.length;i++) {
            if ( values[i].equals( value ) ) {
                return i;
            }
        }
        return -1;
    }


    /** Finds the highest precision from an array of string values by
     *doing a lookup of the index of the first found dot.
     */
    public static int getHighestPrecision(String[] values) {

        int precision = 0;
        int max_precision = 0;

        for (int i=0;i<values.length;i++ ) {
            String value = values[i];
            try{
                int dot_index = value.indexOf(".");
                // No decimal present.
                if ( dot_index == -1 ) {
                    precision = 0;
                } else {
                    precision = value.length() - dot_index - 1;
                }
            } catch (NumberFormatException e) {
                ;// PASS
            }
            if ( precision > max_precision ) {
                max_precision = precision;
            }
        }
        return max_precision;
    }

    public static String[] toUpperCase(String[] strs ) {
        String[] result = new String[strs.length];
        for (int i=0;i<strs.length;i++) {
            if ( strs[i] != null ) {
                result[i] = strs[i].toUpperCase();
            }
        }
        return result;
    }

    public static String[] toLowerCase(String[] strs ) {
        String[] result = new String[strs.length];
        for (int i=0;i<strs.length;i++) {
            if ( strs[i] != null ) {
                result[i] = strs[i].toLowerCase();
            }
        }
        return result;
    }

    public static void toLowerCase(ArrayList strs ) {
    int length = strs.size();
        for (int i=0;i<length;i++) {
            if ( strs.get(i) != null ) {
                strs.set(i, ((String)strs.get(i)).toLowerCase());
            }
        }
    }

    public static String[] trim(String[] strs ) {
        String[] result = new String[strs.length];
        for (int i=0;i<strs.length;i++) {
            if ( strs[i] != null ) {
                result[i] = strs[i].trim();
            }
        }
        return result;
    }

    /** Routine to right align the strings preserving the length of
     *the string when including white spaces.
     *This is a slow routine. Not suitable for 10**6 strings.
     */
    public static String[] toRightAlign(String[] strs ) {
        String[] result = new String[strs.length];
        for (int i=0;i<strs.length;i++) {
            if ( strs[i] != null ) {
                result[i] = toRightAlign( strs[i] );
            }
        }
        return result;
    }

    public static String toRightAlign(String str ) {

        int str_length = str.length();
        if ( str_length == 0 ) {
            return str;
        }

        // Count the numbe of whitespace on the left which
        // will be the required shift.
        int shift = 0;
        int idx = str_length - 1;
        while ( ( idx > 0 ) && Character.isWhitespace( str.charAt( idx ))) {
            shift++;
            idx--;
        }

        if ( shift == 0 ) {
            return str;
        }
        char[] result = new char[str_length];
        Arrays.fill( result, ' ');
        for (int i=str_length-shift-1;i>-1;i--) {
            result[ i + shift ] = str.charAt( i );
        }
        return new String(result);
    }





    public static String[] translateValuesToLowerAndUpper(String[] distances_in) {

        String[] distances_out = { EMPTY_STRING, EMPTY_STRING };
        String d       = distances_in[0];
        String dminus  = distances_in[1];
        String dplus   = distances_in[2];
        int precision = Wattos.Utils.Strings.getHighestPrecision( distances_in );

        //Handling dollar character '$' prepended to a distance value
        if (d.startsWith("$") || dminus.startsWith("$") || dplus.startsWith("$")) {
            if (d.startsWith("$")) {
                distances_out[0] = d + "-" + dminus;
                distances_out[1] = d + "+" + dplus;
            } else {
                try{
                    double dist = Strings.parseDouble(d);
                    if (!dminus.startsWith("$")) {
                        double dmin = Strings.parseDouble(dminus);
                        double dLower = dist - dmin;
                        distances_out[0] = Strings.formatReal(dLower, precision);
                        distances_out[1] = d + "+" + dplus;
                    } else if (!dplus.startsWith("$")) {
                        double dmax = Strings.parseDouble(dplus);
                        double dUpper = dist + dmax;
                        distances_out[0] =  d + "-" + dminus;
                        distances_out[1] =  Strings.formatReal(dUpper, precision);
                    } else {
                        distances_out[0] =  d + "-" + dminus;
                        distances_out[1] =  d + "+" + dplus;
                    }
                } catch (NumberFormatException e) {
                    General.showWarning("NumberFormatException converting distance info -1-.");
                    General.showThrowable(e);
                }
            }
        } else{
            try{
                double dist = Strings.parseDouble(d);
                double dmin = Strings.parseDouble(dminus);
                double dmax = Strings.parseDouble(dplus);
                double dLower = dist - dmin;
                double dUpper = dist + dmax;
                distances_out[0] =  Strings.formatReal(dLower, precision);
                distances_out[1] =  Strings.formatReal(dUpper, precision);
            } catch (NumberFormatException e) {
                General.showWarning("NumberFormatException converting distance info -2-.");
                General.showThrowable(e);
            }
        }
        return distances_out;
    }

    /** Replace all null references to a reference to an empty string.
     */
    public static void fillStringNullReferencesWithEmptyString(String[] a) {
        if ( a == null ) {
            return;
        }
        for (int i=0;i<a.length;i++ ) {
            if ( a[i] == null ) {
                a[i] = EMPTY_STRING;
            }
        }
    }


    /** Append the string representation to the stringbuffer prepending it with
     *spaces if needed to make the buffer 'stringSize' longer.
     */
    public static void appendRightAlign(long l, StringBuffer sb, int stringSize ) {
       int length_before = sb.length();
       sb.append(l);
       rightAllignInStringBuffer( sb, length_before, stringSize);
    }


    /**
     *WARNING: caller has to make sure that the stringSize isn't smaller than
     *the text representation.
     */
    public static void appendRightAlign(double d, StringBuffer sb, int stringSize, int fractionSize ) {
       int length_before = sb.length();
       doubleToString.appendFormatted(sb, d, fractionSize, '.', ',', 3, '-', '\uffff');
       rightAllignInStringBuffer( sb, length_before, stringSize);
    }


    private static void rightAllignInStringBuffer(StringBuffer sb, int length_before, int stringSize ) {
       int length_after = sb.length();
       //byte length_diff = (byte) (length_after - length_before);
       int length_toAdd = stringSize - length_after + length_before;
       switch ( length_toAdd ) {
           case 0:
               break;
           case 1:
               sb.insert(length_before, ' ');
               break;
           case 2:
               sb.insert(length_before, "  ");
               break;
           case 3:
               sb.insert(length_before, "   ");
               break;
           case 4:
               sb.insert(length_before, "    ");
               break;
           case 5:
               sb.insert(length_before, "     ");
               break;
           case 6:
               sb.insert(length_before, "      ");
               break;
           case 7:
               sb.insert(length_before, "       ");
               break;
           case 8:
               sb.insert(length_before, "        ");
               break;
           case 9:
               sb.insert(length_before, "         ");
               break;
           default:
               break;
       }
       return;
    }


    public static String toString(Method m ) {
        StringBuffer sb = new StringBuffer();
        String methodString = m.getName();
        sb.append("Name: " + methodString+General.eol);
        String returnString = m.getReturnType().getName();
        sb.append("   Return Type: " + returnString+General.eol);
        Class[] parameterTypes = m.getParameterTypes();
        sb.append("   Parameter Types:");
        for (int k = 0; k < parameterTypes.length; k ++) {
            sb.append(" " + parameterTypes[k].getName());
        }
        return sb.toString();
    }


    public static String toString(Object[] a ) {
        return toString(a, false, true );
    }

    /** For rectangular */
    public static String toString(String[][] a ) {
        StringBuffer result = new StringBuffer();
        for (int i=0;i<a.length;i++) {
            result.append(toString(a[i]));
            result.append('\n');
        }
        return result.toString();
    }

    /** Will even work when objects are null */
    public static String toString(Object[] a, boolean printEOLAfterEach ) {
        boolean useBrackets = true;
        return toString( a, printEOLAfterEach, useBrackets );
    }

    /** Will even work when objects are null */
    public static String toString(Object[] a, boolean printEOLAfterEach,
        boolean useBrackets ) {
        return toString( a, printEOLAfterEach, useBrackets, ";" );
    }

    /** Will even work when objects are null */
    public static String toString(Object[] a, boolean printEOLAfterEach,
        boolean useBrackets, String separator ) {
        if ( a == null ) {
            return null;
        }
        if ( a.length == 0 ) {
            if ( useBrackets ) {
                return "[empty]";
            } else {
                return "empty";
            }
        }

        StringBuffer result = new StringBuffer();
        if ( useBrackets ) {
            result.append('[');
        }
        for (int i=0;i<a.length;i++) {
            if ( i != 0 ) {
                if ( printEOLAfterEach ) {
                    result.append('\n');
                } else {
                    //result.append(',');
                    result.append(separator);
                }
            }
            result.append( a[i] );
        }
        if ( useBrackets ) {
            result.append(']');
        }
        return result.toString();
    }

    public static String toString(HashMap m ) {
        return toString(m, false );
    }

    public static String toString(HashMap m, boolean showHashes ) {
        if ( m == null ) {
            return null;
        }
        Object key;
        int hash;
        String value;
        int size_print_max = 100;

        List keys = new ArrayList(Arrays.asList( m.keySet().toArray() ));
        Collections.sort(keys);

        StringBuffer sb = new StringBuffer();
        sb.append( Format.sprintf( "There are %d keys:\n", p.add( keys.size() )) );
        Iterator i = keys.iterator();
        while ( i.hasNext() ) {
            key = i.next();
            hash = key.hashCode();
            Object o = m.get( key );
            value = EMPTY_STRING;
            if ( o != null ) {
                value = o.toString();
            }
            // Truncate values longer than specified size.
            if ( value.length() > size_print_max ) {
                value = value.substring(0,size_print_max);
            }
            // Don't show more than 1 line
            if ( value.indexOf('\n') != -1 ) {
                value = value.substring(0,value.indexOf('\n')) + "[...]";
            }
            if ( showHashes ) {
                sb.append( Format.sprintf( "%-25s (%11d): %-s\n", p.add( key ).add( hash ).add( value )) );
            } else {
                sb.append( Format.sprintf( "%-25s: %-s\n",        p.add( key ).add(             value )) );
            }
        }
        return sb.toString();
    }

    public static String toString(Collection c ) {
        if ( c == null ) {
            return null;
        }
        return toString(c,false);
    }


    public static String toString(Enumeration e ) {
        return toString( PrimitiveArray.toArrayList(e) );
    }

    public static String toString(ArrayList c ) {
        if ( c == null ) {
            return null;
        }
        Collection cl = (Collection) c;
        return toString(cl,false);
    }

    /** Simply calls another toString method */
    public static String toString(Collection c, boolean printEOLAfterEach  ) {
        if ( c == null ) {
            return null;
        }
        Object[] l = c.toArray();
        return toString(l,printEOLAfterEach);
    }

    /** Dumb method */
    public static String[] toStringArray( Collection c ) {
        // If the collection makes any guarantees as to what order its elements are
        //returned by its iterator, this method must return the elements in the same order.
        if ( c == null ) {
            return null;
        }
        Object[] a = c.toArray();
        String[] r = new String[a.length];
        for (int i=0;i<a.length;i++) {
            r[i] = a[i].toString();
        }
        return r;
    }

    /** dumb method */
    public static String[] toStringArray( Object[] c ) {
        // If the collection makes any guarantees as to what order its elements are
        //returned by its iterator, this method must return the elements in the same order.
        if ( c == null ) {
            return null;
        }
        String[] r = new String[c.length];
        for (int i=0;i<c.length;i++) {
            if ( c[i] != null ) {
                r[i] = c[i].toString();
            } else {
                r[i] = null;
            }
        }
        return r;
    }

    /** Returns the longest string in the array or null if no valid string was encountered. */
    public static String longestString( String[] a ) {
        if ( a == null ) {
            return null;
        }
        String result = null;
        int maxLenght = 0;
        for (int i=a.length-1;i>=0;i--) {
            String tmp = a[i];
            if ( ( tmp != null ) && ( tmp.length() > maxLenght) ) {
                maxLenght = tmp.length(); // slow but infrequent to double call
                result = tmp;
            }
        }
        return result;
    }

    /** Returns the length of the longest string in the array. Null references for
     *strings will cause a core.
     */
    public static int getMaxSizeStrings( String[] s ) {
        int longestLength = 0;
        for (int i=s.length-1;i>=0;i--) {
            if ( s[i].length() > longestLength ) {
                longestLength = s[i].length();
            }
        }
        return longestLength;
    }

    /** From an array of strings take those whose bit is set in selected variable
     *but only those who haven't been taken already; i.e. are distinct.
     *If the assumption holds that the string pointers in the array are the
     *same for the same strings, then this routine returns pretty efficiently.
     *This method can easily be generalized to Object[] and should probably be
     *done so.
     *Returns null on failure.
     *Should be efficient for arrays of large sizes like 10**6.
     *But could be more efficient if scan is eliminated; see Database.SQLSelect code.
     *Make it faster by using same distinct algo as for int index.
     */

    public static String[] getDistinctSorted( String[] stringList, BitSet selected ) {
        if ( stringList == null ) {
            General.showCodeBug("Input string list should not be null in Strings.getDistinct");
            return null;
        }
        if ( selected == null ) {
            General.showCodeBug("Selection BitSet should not be null in Strings.getDistinct");
            return null;
        }
        int selectedCardinality = selected.cardinality() ;
//        General.showDebug("Selection BitSet has cardinality in getDistinctSorted: " + selectedCardinality);
        if ( selectedCardinality == 0 ) {
            General.showWarning("Selection BitSet has zero cardinality in getDistinctSorted");
            return new String[0];
        }
        HashMap map = new HashMap();
        for (int i=selected.nextSetBit(0); i>=0; i=selected.nextSetBit(i+1))  {
            map.put( stringList[i], null );
        }
        Set keys = map.keySet();
        Object[] resultObjectList = keys.toArray();
        Arrays.sort( resultObjectList );
        String[] result = Strings.toStringArray( resultObjectList );
        return result;
    }

    public static String toString( boolean b ) {
        if ( b ) {
            return Relation.TRUE;
        }
        return Relation.FALSE;
    }

        /** Show a little debug info on environment     */
    public static String toString( Map m ) {
        StringBuffer sb = new StringBuffer();
        Set k = m.keySet();
        ArrayList al = new ArrayList();
        al.addAll(k);
        Collections.sort(al);

        for ( Iterator i=al.iterator(); i.hasNext();) {
            String key = (String) i.next();
            Parameters p = new Parameters(); // Printf parameters
            p.add( key );
            p.add( m.get(key) );
            sb.append( Format.sprintf("%-50s %-50s", p));
            sb.append( General.eol );
        }
        return sb.toString();
    }

    /** Fast convenience method. Callers assures the item is not null and has
     *at least 1 character */
    public static boolean startLooksLikeANumber(String item) {
        char ch = item.charAt(0);
        // inlined isAsciiDigit here for speed.
        int c = (int)ch;
        if ( ( (c >= 48) && (c <= 57) ) || (ch=='-') || (ch=='+') || (ch=='.') ) {
            return true;
        }
        return false;
    }

    public static boolean isAsciiDigit(char ch) {
        int c = (int)ch;
        if ( c >= 48 && c <= 57 ) {
            return true;
        }
        return false;
    }
    /** Transfers the lines to content variable.
     */
    public static byte[] toByteArray(String text) {
        if ( text == null ) {
            General.showError("Cannot use null ref in Strings.toByteArray");
            return null;
        }
        /** Code could be optimized a bit by not materializing over and over...
         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream ();
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(baos);
            //General.showDebug("Encoding: " + out.getEncoding());
            // web app failed here when encoding "Cp850" was given.
            out.write( text );
            out.close(); // weird that this method is necessary. apparently baos doesn't get all data yet.
            return baos.toByteArray();
        } catch ( Exception e ) {
            General.showThrowable(e);
        }
        return null;
    }
    /**
     *
     * @param o
     * @return
     */
    public static String toString(Object o) {
        Class cl = o.getClass();
        Package pa = cl.getPackage();
        String name = cl.getName();
        String msg = "Object from class " + name + " from package " + pa;
        return msg;
    }
    public static String removeEols(String value) {
        return EOL_ANY.matcher(value).replaceAll("");
    }
    public static void deriveUniqueNames(String[] columnString) {
        for (int i=1;i<columnString.length;i++) {
            if ( columnString[i] == null ) {
                continue;
            }
            int count = 1;
            int strLenght = columnString[i].length();
//            boolean done = false;
            for (int j=0;j<i;j++) {
                if ( columnString[j] == null ) {
                    continue;
                }
                if ( columnString[j].equals(columnString[i])) {
                    count++;
                    columnString[i] = columnString[i].substring(0,strLenght) + " " + count;
                    j--; // check again
                }
            }
        }

    }
}
