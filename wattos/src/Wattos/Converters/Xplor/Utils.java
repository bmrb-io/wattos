/*
 * Utils.java
 *
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Xplor;


import org.apache.regexp.*;
import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.util.jar.Attributes;
import java.util.*;
import java.io.*;

/** Common routines for parsing Xplor constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Utils {

    public static RE re_getTextInCommentToken_remark;
    public static RE re_getTextInCommentToken_set;
    public static RE re_getTextInCommentToken_end;
    
    static {
        try {
            /* [:alpha:]            Alphabetic characters 
             * [:space:]            Space characters (such as space, tab, and form feed, to name a few). 
             */
            re_getTextInCommentToken_remark = new RE("^REMA[:alpha:]*[:space:]*",   RE.MATCH_CASEINDEPENDENT );
            re_getTextInCommentToken_set    = new RE("^SET[:space:]+",              RE.MATCH_CASEINDEPENDENT );
            re_getTextInCommentToken_end    = new RE("[:space:]*END$",              RE.MATCH_CASEINDEPENDENT );
            
        } catch ( RESyntaxException e) {
            General.showError("Code error: Xplor.Utils" + e.toString() );
        }
    }
    /** Shorthand for XplorParserAllConstants namesake.
     */    
    static final int SETCOMMENT     = XplorParserAllConstants.SETCOMMENT;
    /** Shorthand for XplorParserAllConstants namesake.
     */    
    static final int CURLYCOMMENT   = XplorParserAllConstants.CURLYCOMMENT;
    /** Shorthand for XplorParserAllConstants namesake.
     */    
    static final int REMARKCOMMENT  = XplorParserAllConstants.REMARKCOMMENT;
    /** Shorthand for XplorParserAllConstants namesake.
     */    
    static final int BANGCOMMENT    = XplorParserAllConstants.BANGCOMMENT;
    /** Shorthand for XplorParserAllConstants namesake.
     */    
    static final int EOF            = XplorParserAllConstants.EOF;

    /** Strip the quote style from the token and combine consecutive comments
     * of the same style where needed.
     * @param token Input to be parsed.
     * @return Possibly concatenated comments without quote style.
     */
    static public String getTextInCommentToken( Token token ) {

        String content = token.image.trim();
        int content_length = content.length();

        if ( content_length < 1 ) {
            General.showError("Code error. Length smaller than 1." );
            General.showError( "token text length:" + content_length);
            return content;
        }
        
        // Strip off '!'
        if (token.kind ==BANGCOMMENT) {
            content = content.substring(1).trim();
        // Strip off "rema(rk)" by looking at first occurrence of a space or tab.
        } else if (token.kind == REMARKCOMMENT) {
            content = re_getTextInCommentToken_remark.subst(content,"",RE.REPLACE_FIRSTONLY );
        } else if (token.kind == CURLYCOMMENT) {
            content = content.substring(1, content.length() -1 ).trim();
        } else if (token.kind == SETCOMMENT) {
            content = re_getTextInCommentToken_set.subst(content,"",RE.REPLACE_FIRSTONLY );
            content = re_getTextInCommentToken_end.subst(content,"",RE.REPLACE_FIRSTONLY );
        } else {
            // Might have been possible to get descriptive name but that takes
            // too much reflection.
            General.showError("code bug in getTextInCommentToken");
            General.showError( "Unknown token.kind:" + token.kind);
            General.showError( "See values defined for them in class XplorParserAllConstants" );
        }
        return content;
    }
    
    
    /** Called to skip over errors while storing all errors and their corresponding lines and columns.
     * It also stores all the comments that occur between errors and Assi statements.
     * @param kind Kind of token to sync on after this token.
     * @param errLineStack Vector of line numbers where to add the location coordinates on to.
     * @param cmtStack Comment stack to add comments to, in case any within the error.
     * @param lastAssiLine Error coordinate.
     * @param lastAssiCol Error coordinate.
     * @throws ParserDoneException Are we done?
     */
    public static void error_skipto(int kind, 
        Vector errLineStack, Vector cmtStack, int lastAssiLine, int lastAssiCol
        ) throws ParserDoneException {
            
        boolean consume = false;
        int errEndLine = lastAssiLine, errEndCol = lastAssiCol;
        Token t, tmp;
        //Flag to indicate if parser is at EOF
        boolean done = false;
        //Flag to indicate if error is at end of an Assi statement
        boolean last = false;
        t = XplorParserAll.getToken(1);
        //Store the last comment before EOF if error is due to EOF
        if (t.kind == EOF) {
            t = XplorParserAll.getNextToken(); 
            saveComment(t, cmtStack);
            done = true;
        }
        do{
            t = XplorParserAll.getToken(1);

            // Consume the token only if it is NOT the
            // one we are looking for, or if the flag   
            // was set to consume the last token:
            if( (consume || t.kind != kind) && t.kind != EOF) {
                tmp = XplorParserAll.getNextToken();
                last = true;
                saveComment(tmp, cmtStack);
                //update error end line
                if (tmp.endLine > errEndLine) {
                        errEndLine = tmp.endLine;
                        errEndCol = tmp.endColumn;
                }
                //update error end column
                if (tmp.endLine == errEndLine && tmp.endColumn > errEndCol) {
                        errEndCol = tmp.endColumn;
                }
            }
        }  while (t.kind != EOF && t.kind != kind);

        if (t.kind != EOF) {
            //if error is at end of Assi statement, update position
            if (!last) {
                errEndLine = t.beginLine;
                errEndCol = t.beginColumn;
            }
            //save error content and line, column position
            errLineStack.add(new ErrorLine(lastAssiLine, errEndLine, lastAssiCol, errEndCol));
        }
        //store the last comment before EOF, Error until end
        if (!done && t.kind == EOF) {
            saveComment(t, cmtStack);
            errLineStack.add(new ErrorLine(lastAssiLine, errEndLine, lastAssiCol, errEndCol));
        }
        if ( t.kind == EOF ) {
            throw ( new ParserDoneException() );
        }
    }  

    
    /** Put the constraints in there native data structure.
     * @param opt_info
     * @param select_list
     * @param stack Target location
     * @param select1 First atom's selection characteristics
     * @param select2 Second...
     * @param d Target distance
     * @param dminus Lower bound correction from target distance.
     * @param dplus Upper bound correction from target distance. */

    public static void saveAssiStateDistance( Vector stack,
    ArrayList select1, ArrayList select2, 
    String d, String dminus, String dplus, Attributes opt_info,
    ArrayList select_list ) {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("treeId",       (new Integer(stack.size() + 1)).toString() );
        root.entry.putValue("treeNodeId",   (new Integer(1)).toString() );
        root.entry.putValue("value",         d );

        if ( opt_info != null ) {
            root.opt_info.put("opt_info", opt_info );
        }

        String[] values_in = { d, dminus, dplus };
        String[] values_out = translateValuesToLowerAndUpper( values_in);
        root.entry.putValue("lower", values_out[0]);
        root.entry.putValue("upper", values_out[1]);
        
        //Store separate values for atoms
        //General.showOutput("1st selection list is: [" + AtomNode.toString(select1) + "]");
        //General.showOutput("2nd selection list is: [" + AtomNode.toString(select2) + "]");
        root.atoms.add(select1);
        root.atoms.add(select2);

        if ( select_list != null ) {
            int select_list_size = select_list.size();
            if ( select_list_size % 2 != 0 ) {
                General.showWarning("Code bug or so from the number of selection expressions is odd (not even): "
                    + select_list_size);
            }
            root.atomsOR = select_list;
        }
        stack.add(root);
    }
    
    /** Put the constraints in there native data structure.
     * @param stack Data vector.
     * @param select1 Selected atom 1
     * @param select2 Selected atom 2
     * @param select3 Selected atom 3
     * @param select4 Selected atom 4
     * @param constant Force constant.
     * @param target Dihedral angle target constraint.
     * @param range Deviation possible on both sides.
     * @param exponent Potential function exponent as defined.
     */

    public static void saveAssiStateDihedral( Vector stack,
    ArrayList select1, ArrayList select2, ArrayList select3, ArrayList select4, 
    String constant, String target, String range, String exponent ) {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("force_constant_value",         constant );
        root.entry.putValue("potential_function_exponent",  exponent );

        String[] values_in = { target, range, range };
        String[] values_out = translateValuesToLowerAndUpper( values_in);
        root.entry.putValue("lower", values_out[0]);
        root.entry.putValue("upper", values_out[1]);
        
        //Store separate values for atoms
        root.atoms.add(select1);
        root.atoms.add(select2);
        root.atoms.add(select3);
        root.atoms.add(select4);
        stack.add(root);
        //General.showOutput("ASSI target" + target + " range " + range+ " upper " + upper + " lower " + lower);
    }
    
    
    /** Put the constraints in there native data structure.
     * @param stack Data stack.
     * @param select1 Selected atom 1
     * @param select2 Selected atom 2
     * @param value Observed residual dipolar coupling constant.
     * @param number2 Deviation on both sides or for lower bound.
     * @param number3 Optional third number indicates errors may be different for upper and lower side.
     */    
    public static void saveAssiStateDipolarCoupling( Vector stack,
    ArrayList select1, ArrayList select2, 
    String value, String number2, String number3 ) {

        LogicalNode root = new LogicalNode();
        root.entry.putValue("value",         value   );
        if ( number3 == null ) {
            // The two numbers case:
            root.entry.putValue("error",  number2  );
            root.entry.putValue("lower",  Wattos.Utils.NmrStar.STAR_EMPTY  );
            root.entry.putValue("upper",  Wattos.Utils.NmrStar.STAR_EMPTY);
        } else {
            String[] values_in = { value, number2, number3 };
            String[] values_out = translateValuesToLowerAndUpper( values_in);
            root.entry.putValue("error", Wattos.Utils.NmrStar.STAR_EMPTY);
            root.entry.putValue("lower", values_out[0]);
            root.entry.putValue("upper", values_out[1]);
        }            
            
        //Store seperate values for atoms
        root.atoms.add(select1);
        root.atoms.add(select2);
        stack.add(root);
    }

    
    /** Saving all special token as comments including their position.
     * @param token Input
     * @param cmtStack Target
     */
    public static void saveComment(Token token, Vector cmtStack ) {
        //If no special tokens are prepended then no action is required.
        
        if (token == null || token.specialToken == null) {
            return;
        }
        Token special_token = token.specialToken;
        //walk to the head of the special token chain
        while (special_token.specialToken != null) {
            special_token = special_token.specialToken;
        }
        //record start kind of specialToken
        int prevKind = special_token.kind;
        //used to store comment positions
        int beginLine   = special_token.beginLine;
        int endLine     = special_token.endLine;
        int beginCol    = special_token.beginColumn;
        int endCol      = special_token.endColumn;

        // Block stores all comments in one block
        // When the next token is a reference to null the special tokens are all done.
        // See the TokenManager minitutorial.
        String block = "";
        String comment_text;
        while (special_token != null) {
//            String content = special_token.image;
            // Most consecutive same style of comments should be combined.
            if (special_token.kind == prevKind && 
                special_token.kind != CURLYCOMMENT ) {
                // Combine with any previous tokens already in buffer.
                endLine = special_token.endLine;
                endCol  = special_token.endColumn;
                // Use a routine to get rid of 'quote style' of text
                comment_text = Utils.getTextInCommentToken( special_token );
                if ( block.length() == 0 ) {
                    block = comment_text;
                } else {
                    block = block + General.eol + comment_text;       
                }
            } else {
                // Write previous token(s) and start new buffer
                saveCommentBuffer( cmtStack, block, beginLine, beginCol, endLine, endCol );
                block = Utils.getTextInCommentToken( special_token );            
                //update comments positions
                beginLine   = special_token.beginLine;
                endLine     = special_token.endLine;
                beginCol    = special_token.beginColumn;
                endCol      = special_token.endColumn;
            }
            prevKind = special_token.kind;
            special_token = special_token.next;
        }
        // Last token before real token. There's always a last token
        saveCommentBuffer( cmtStack, block, beginLine, beginCol, endLine, endCol );
    }

    
    /** Saving all special token as comments including their position.
     * @param beginCol coordinate
     * @param endLine coordinate
     * @param endCol coordinate
     * @param cmtStack Target
     * @param block Text
     * @param beginLine First of the coordinates
     */
    static public void saveCommentBuffer( Vector cmtStack, String block, 
        int beginLine, int beginCol, int endLine, int endCol) {
        //if comment is not empty, add to cmtStack
        if (block.length() > 0) {
            Comment cmt = new Comment();
            cmt.entry.putValue("commentId",     (new Integer(cmtStack.size() + 1)).toString());
            cmt.entry.putValue("comment",       block);
            cmt.entry.putValue("beginLine",     (new Integer(beginLine)).toString());
            cmt.entry.putValue("beginColumn",   (new Integer(beginCol)).toString());
            cmt.entry.putValue("endLine",       (new Integer(endLine)).toString());
            cmt.entry.putValue("endColumn",     (new Integer(endCol)).toString());
            cmtStack.add(cmt);
        }
    }
    
    
    /** If there are any errors, open the input file
     * for second pass and record errors and corresponding coordinates.
     * @param inputFile Source of the errors.
     * @param errLineStack A construct with error location.
     * @param errStack A construct for error texts as target.
     */
    static public void saveErrors(String inputFile, Vector errLineStack, Vector errStack) {
        int currentLine = 0;
        //save the last non-empty line end position
        int lastNonEmptyLineEndCol = 0;
        String line = null;
        try{
            // Open input file for reading
            RandomAccessFile input = new RandomAccessFile(inputFile, "r");
            for (int i = 0; i < errLineStack.size(); i++) {
                ErrorLine errTemp = (ErrorLine) (errLineStack.elementAt(i));
                ParseError err = new ParseError();
                int errId = i + 1;
                //General.showOutput("errId is: " + errId);
                err.entry.putValue("errorId", (new Integer(errId)).toString());
                String error = "";
                //skip to error start line
                for (int j = currentLine; j < errTemp.line[0]; j++) {
                    line = input.readLine();
                    if (line.length() > 0)
                    lastNonEmptyLineEndCol = line.length();
                }
                // Error at the same line
                if (errTemp.line[1] == errTemp.line[0]) {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] -1, errTemp.col[1] );
                }
                else {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] - 1, line.length() ) + General.eol;
                    for (int j = errTemp.line[0]; j < errTemp.line[1]; j++) {
                        line = input.readLine();
                        if (line.length() > 0)
                            lastNonEmptyLineEndCol = line.length();
                            // Check if the endColumn is 1, if so, correct position
                        if (j == errTemp.line[1] - 2 && errTemp.col[1] == 1) {
                            errTemp.col[1] = lastNonEmptyLineEndCol;
                            errTemp.line[1] = errTemp.line[1] - 1;
                        }
                        if (j == errTemp.line[1])
                            // error = error + line.substring(0, errTemp.col[1]);
                            error = error + Strings.substringInterpetTabs( line, 0, errTemp.col[1]);
                        else
                            error = error + line + General.eol;
                    }
                }
                currentLine = errTemp.line[1];
                error.trim();
                if (error.length() > 0) {
                    err.entry.putValue("error", error);
                    err.entry.putValue("beginLine",  (new Integer(errTemp.line[0])).toString());
                    err.entry.putValue("beginColumn", (new Integer(errTemp.col[0])).toString());
                    err.entry.putValue("endLine", (new Integer(errTemp.line[1])).toString());
                    err.entry.putValue("endColumn", (new Integer(errTemp.col[1])).toString());
                    errStack.add(err);
                }   
            }
            input.close();
        } catch (IOException e) {
            General.showOutput("IOException in saveErrors method" );
            General.showThrowable(e);
        }
    }
    
    /** Obligatory constructor; don't use.
     */
    public Utils() {
        General.showError("Don't try to initiate Utils class");
    }

    /** Test code for some of the routines in this class. */
    public static void main ( String[] args ) {
        if ( true ) {  
            Token token = new Token();
            // Test a few of the possible quote styles.
            token.kind = CURLYCOMMENT;
            
            if ( token.kind == BANGCOMMENT ) {
                token.image = "! here is {a} remark\n";
            } else if ( token.kind == REMARKCOMMENT ) {
                token.image = "REMARKS a rem";
            } else if ( token.kind == CURLYCOMMENT ) {
                token.image = "{a {nested} comment}";
            }                
            General.showOutput("token is            : [" + token.image + "]");            
            General.showOutput("token without quotes: [" +  getTextInCommentToken(token) + "]");        
        }
        if ( true ) {  
            String[] distances_in = { "3e3", "2.1", "1.01"};
            String[] distances_out = translateValuesToLowerAndUpper( distances_in);
            General.showOutput("[" + distances_out[0] + "]");
            General.showOutput("[" + distances_out[1] + "]");
        }
        
    } 
    
    public static String[] translateValuesToLowerAndUpper(String[] distances_in) {
        
        String[] distances_out = { "", "" };
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
    
} 
