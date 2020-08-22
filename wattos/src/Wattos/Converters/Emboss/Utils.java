/*
 * Utils.java
 *
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Emboss;


import org.apache.regexp.*;
import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.util.*;
import java.io.*;

/** Common routines for parsing Emboss constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Utils {

    public static RE re_getTextInCommentToken_remark;
    public static RE re_getTextInCommentToken_set;
    public static RE re_getTextInCommentToken_end;
    
    static final int SEMICOLONCOMMENT    = EmbossParserAllConstants.SEMICOLONCOMMENT;
    static final int EOF            = EmbossParserAllConstants.EOF;

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
        if (token.kind == SEMICOLONCOMMENT) {
            content = content.substring(1).trim();
        } else {
            General.showError("code bug in getTextInCommentToken");
            General.showError( "Unknown token.kind:" + EmbossParserAllConstants.tokenImage[token.kind] );
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
        Vector errLineStack, Vector cmtStack, 
        int lastAssiLine, int lastAssiCol
        ) throws ParserDoneException {
            
        boolean consume = false;
        int errEndLine = lastAssiLine, errEndCol = lastAssiCol;
        Token t, tmp;
        //Flag to indicate if parser is at EOF
        boolean done = false;
        //Flag to indicate if error is at end of an Assi statement
        boolean last = false;
        t = EmbossParserAll.getToken(1);
        //Store the last comment before EOF if error is due to EOF
        if (t.kind == EOF) {
            t = EmbossParserAll.getNextToken(); 
            saveComment(t, cmtStack);
            done = true;
        }
        do{
            t = EmbossParserAll.getToken(1);

            // Consume the token only if it is NOT the
            // one we are looking for, or if the flag   
            // was set to consume the last token:
            if( (consume || t.kind != kind) && t.kind != EOF) {
                tmp = EmbossParserAll.getNextToken();
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
     * @param stack Target location
     * @param select1 First atom's selection characteristics
     * @param select2 Second...
     * @param dminus Lower bound correction from target distance.
     * @param dplus Upper bound correction from target distance. */
    
    public static void saveAssiStateDistance( Vector stack,
    ArrayList select1, ArrayList select2, 
    String dminus, String dplus, 
    ArrayList select_list ) {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("treeId",       (new Integer(stack.size() + 1)).toString() );
        root.entry.putValue("treeNodeId",   (new Integer(1)).toString() );

        //root.entry.putValue("value",         null );
        root.entry.putValue("lower", dminus);
        root.entry.putValue("upper", dplus);
        
        //Store separate values for atoms
        root.atoms.add(select1);
        root.atoms.add(select2);
        stack.add(root);
    }
    
    
    /** Put the constraints in there native data structure.
     * @param stack Data vector.
     * @param select1 Selected atom 1
     * @param select2 Selected atom 2
     * @param select3 Selected atom 3
     * @param select4 Selected atom 4
     */

    public static void saveAssiStateDihedral( Vector stack,
    ArrayList select1, ArrayList select2, ArrayList select3, ArrayList select4, 
    String angle_low, String angle_high ) {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("lower",        angle_low);
        root.entry.putValue("upper",        angle_high);
        
        //Store separate values for atoms
        root.atoms.add(select1);
        root.atoms.add(select2);
        root.atoms.add(select3);
        root.atoms.add(select4);
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
            if (special_token.kind == prevKind  ) {
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
            token.kind = SEMICOLONCOMMENT;
            
            if ( token.kind == SEMICOLONCOMMENT ) {
                token.image = "; here is {a} remark\n";
            }                
            General.showOutput("token is            : [" + token.image + "]");            
            General.showOutput("token without quotes: [" +  getTextInCommentToken(token) + "]");        
        }
    } 
    
} 
