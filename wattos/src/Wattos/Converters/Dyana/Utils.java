/*
 * Utils.java
 *
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Dyana;


import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.util.*;
import java.io.*;

/** Common routines for parsing Dyana constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Utils {

    
    /** Shorthand for DyanaParserAllConstants namesake.
     */    
    static final int POUNDCOMMENT    = DyanaParserAllConstants.POUNDCOMMENT;
    /** Shorthand for DyanaParserAllConstants namesake.
     */    
    static final int EOF            = DyanaParserAllConstants.EOF;
    static final int EOL            = DyanaParserAllConstants.EOL;

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
        
        // Strip off comment style
        if (token.kind == POUNDCOMMENT) {
            content = content.substring(1).trim();
            /** Repeat once because the first char might be the optional > or < 
             */
            if ( content.length() >= 1 && content.charAt(0) == '#' ) {
                content = content.substring(1).trim();
            }
        } else {
            // Might have been possible to get descriptive name but that takes
            // too much reflection I'm not familiar with right now.
            General.showError("code bug in getTextInCommentToken");
            General.showError( "Unknown token.kind:" + token.kind);
            General.showError( "See values defined for them in class DyanaParserAllConstants" );
        }
        return content;
    }

    /** Returns the coordinates of the beginning of the current token.
     * @return The coordinates of the beginning of the current token.
     */    
    public static int[] storeBeginOfRestraint() {
        Token t     = DyanaParserAll.getToken(1);
        int[] coordinates = { t.beginLine, t.beginColumn };
        t = null;
        return coordinates;
    }

    /** Eats tokens until the next token is of kind requested or of type EOF.
     * E.g.: If input is "<WORD> <SPECIAL> <EOF>" so the returned token type from
     * getToken(0) would be <WORD> and we would look for:
     * <PRE>
     * -1- <WORD>:       input remaining would be: "<EOF>"
     * -2- <SPECIAL>:    input remaining would be: "<WORD> <SPECIAL> <EOF>"
     * -3- <WORD>: input remaining would be: "<WORD> <SPECIAL> <EOF>"
     * @param kind Kind of token to look for.
     */
    public static void skiptoToken( int kind ) {
        Token t;
        do {
            t = DyanaParserAll.getNextToken();
        } while ( t.kind != kind && 
                  t.kind != EOF );
    }

    /** Called to skip over tokens (errors) and storing the skipped tokens 
     * and the corresponding line and column of the end of the last skipped token.
     * It does not store the comments that occur between errors. It will at least eat the line
     * that contains the current token because this method looks for the kind of token
     * as the first token after an eol!
     * @param kind Kind of token to sync on after this token.
     * @param errLineStack Vector of line numbers where to add the location coordinates on to.
     * @param lastAssiLine Error coordinate for beginning of restraint.
     * @param lastAssiCol Error coordinate for beginning of restraint..
     * @throws ParserDoneException Are we done?
     */
    public static void error_skipto(int kind,
            Vector errLineStack, int lastAssiLine, int lastAssiCol
        ) throws ParserDoneException {


        Token current_token = DyanaParserAll.getToken(0);
        Token next_token    = DyanaParserAll.getToken(1);
        
//        General.showOutput("-1- error_skipto: current token is :[" + current_token.image + "]");
//        General.showOutput("-1- error_skipto: next token    is :[" + next_token.image + "]");
        if ( next_token.kind == EOF ) {
            throw ( new ParserDoneException() );
        }
        
        do {
            skiptoToken( EOL );
            current_token = DyanaParserAll.getToken(0);
            next_token    = DyanaParserAll.getToken(1);
//            General.showOutput("-2- error_skipto: current token is :[" + current_token.image + "]");
//            General.showOutput("-2- error_skipto: next token    is :[" + next_token.image + "]");
            /** Eat any consecutive EOLs if present, except last one.*/
            while ( next_token.kind == EOL && next_token.kind != EOF ) {
                current_token = DyanaParserAll.getNextToken();
                next_token = DyanaParserAll.getToken(1);
            }
//            General.showOutput("-3- error_skipto: current token is :[" + current_token.image + "]");
//            General.showOutput("-3- error_skipto: next    token is :[" + next_token.image + "]");
        } while ( next_token.kind != kind && 
                  next_token.kind != EOF );
        
        errLineStack.add(new ErrorLine( lastAssiLine, current_token.endLine, 
                                        lastAssiCol,  current_token.endColumn));

        if ( next_token.kind == EOF ) {
            throw ( new ParserDoneException() );
        }
    }  

    
    /** Put the constraints in there native data structure. At least the first
     * part. The second part and iteration thereby are done by saveDistance2.
     * @param stack
     * @param residue_number_1
     * @param residue_name_1
     * @param results  */
    public static void saveDistance( Vector stack,
            String residue_number_1, String residue_name_1, ArrayList results ) {

        for (int i=0;i<results.size();i++) {
            String[] result_one_restraint = (String[]) results.get(i);            
            
            LogicalNode root = new LogicalNode();
            // Storing all values into logical node starting at 1
            root.entry.putValue("treeId",       (new Integer(stack.size() + 1)).toString() );
            root.entry.putValue("treeNodeId",   (new Integer(1)).toString() );

            AtomNode atom_node_1 = new AtomNode();
            AtomNode atom_node_2 = new AtomNode();

            atom_node_1.info.putValue("resi", residue_number_1);
            atom_node_1.info.putValue("resn", residue_name_1);
            atom_node_1.info.putValue("name", result_one_restraint[ 0 ]);
            atom_node_2.info.putValue("resi", result_one_restraint[ 1 ]);
            atom_node_2.info.putValue("resn", result_one_restraint[ 2 ]);
            atom_node_2.info.putValue("name", result_one_restraint[ 3 ]);
            root.entry.putValue("upper",      result_one_restraint[ 4 ] );
            
            ArrayList atoms_1 = new ArrayList();
            ArrayList atoms_2 = new ArrayList();

            atoms_1.add( atom_node_1 );
            atoms_2.add( atom_node_2 );

            root.atoms.add(atoms_1);
            root.atoms.add(atoms_2);

            stack.add(root);
        }
    }
    
    
    /** Clean the stack by expanding the ambig definitions
     * @param stack
     * @param ambigStack  */
    public static void processDistanceAmig( Vector stack,  Vector ambigStack ) {
        
        for (int i = 0; i < ambigStack.size(); i++) {
            /** Check if the item is an object of type ambig definition
             *which is an object of type String[].
             */
            ArrayList ambi_atoms = (ArrayList) ambigStack.get(i);                
            String atom_name = (String) ambi_atoms.get(0); // Special item.
            /** Check real restraints for occurrences of atoms with this name.
             */
            for (int j = 0; j < stack.size(); j++) {
                LogicalNode logical_node = (LogicalNode) stack.get(j);
                ArrayList atom_nodes_list = logical_node.atoms;
                /** Check all atoms within one restraint (both ends). Assuming no
                 *more than 2 ends.
                 */
                for (int k = 0; k < 2; k++) {
                    ArrayList atom_nodes = (ArrayList) atom_nodes_list.get(k);
                    for (int m = 0; m < atom_nodes.size(); m++) {
                        AtomNode atom = (AtomNode) atom_nodes.get(m);
                        if ( atom.info.getValue("name").equalsIgnoreCase( atom_name ) ) {
                            /** Replace this atom with the atoms as defined by AMBIG lines.
                             *Remember the first element in the list is different; it was
                             *reserved for the long atom name.
                             */
                            /** Replace the atom with the first new definition
                             */
                            atom_nodes.remove(m);
                            for (int n = 1; n < ambi_atoms.size(); n++) {
                                String[] atom_info = (String[]) ambi_atoms.get(n);

                                AtomNode atom_new = new AtomNode();
                                atom_new.info.putValue("name", atom_info[0] );
                                atom_new.info.putValue("resi", atom_info[1] );
                                atom_nodes.add( m-1+n, atom_new );
                            }
                        }
                    }
                }                    
            }
        }
        /** Mark for deletion 
         */
        ambigStack.removeAllElements();
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
                    block = block + "\n" + comment_text;       
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
                    if (line.length() > 0) {
                        lastNonEmptyLineEndCol = line.length();
                    }
                }
                // Error at the same line
                if (errTemp.line[1] == errTemp.line[0]) {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] -1, errTemp.col[1] );
                } else {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] - 1, -1 ) + "\n";
                    for (int j = errTemp.line[0]; j < errTemp.line[1]; j++) {
                        line = input.readLine();
                        if (line.length() > 0)
                            lastNonEmptyLineEndCol = line.length();
                            // Check if the endColumn is 1, if so, correct position
                        if (j == errTemp.line[1] - 2 && errTemp.col[1] == 1) {
                            errTemp.col[1] = lastNonEmptyLineEndCol;
                            errTemp.line[1] = errTemp.line[1] - 1;
                        }
                        if (j == errTemp.line[1]) {
                            error = error + Strings.substringInterpetTabs( line, 0, errTemp.col[1]);
                        } else {
                            error = error + line + "\n";
                        }
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

    /** Test code for some of the routines in this class.
     * @param args Nonen to be given.
     */
    public static void main ( String[] args ) {
        if ( true ) {  
            Token token = new Token();
            // Test a few of the possible quote styles.
            token.kind = POUNDCOMMENT;
            if ( token.kind == POUNDCOMMENT ) {
                token.image = "# here is {a} pound remark\n";
            }                
            General.showOutput("token is            : [" + token.image + "]");            
            General.showOutput("token without quotes: [" +  getTextInCommentToken(token) + "]");        
        }
    }    
    
    /** Put the constraints in there native data structure.
     * @param stack Data vector.
     */
    public static void saveDihedral(Vector stack, 
        String residue_number, String residue_name, String angle_name, 
        String angle_low, String angle_high ) {
        
        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("angle_name",   angle_name );        
        root.entry.putValue("lower",        angle_low);
        root.entry.putValue("upper",        angle_high);
        /** Here only the name of the angle can lead to a correct
         *identification of the atoms e.g. which residue number through the
         *4 constituent atoms should be used. It is in general safe to assign
         *the residue to the first atom then.
         */
        for (int j = 0; j < 4; j++) {                
            ArrayList atom_list = new ArrayList();
            AtomNode atom = new AtomNode();
            if ( j == 0 ) {
                atom.info.putValue("resi", residue_number);
                atom.info.putValue("resn", residue_name);
            }
            atom_list.add(atom);
            root.atoms.add(atom_list);
        }
                                
        stack.add(root);
        //General.showOutput("ASSI target" + target + " range " + range+ " upper " + upper + " lower " + lower);
    }
    
    /** Put the constraints in there native data structure.
     * @param stack Data stack.
     */
    public static void saveDipolar(Vector stack, 
        String residue_number_1, String residue_name_1, String atom_name_1, 
        String residue_number_2, String residue_name_2, String atom_name_2, 
        String value, String value_error) {
        
        if ( value_error == null ) {
           value_error =  Wattos.Utils.NmrStar.STAR_EMPTY;
        }

        LogicalNode root = new LogicalNode();
        root.entry.putValue("value",  value   );
        root.entry.putValue("error",  value_error);
        root.entry.putValue("lower",  Wattos.Utils.NmrStar.STAR_EMPTY  );
        root.entry.putValue("upper",  Wattos.Utils.NmrStar.STAR_EMPTY);
        
        // Only 1 atom specified; the other one can be derived because
        // the requirement in DYANA is that the specified atom is bonded
        // to only 1 atom (weird..). 
        // Update: this is no longer true since I got entry 1P7E
        for (int j = 0; j < 2; j++) {                
            ArrayList atom_list = new ArrayList();
            AtomNode atom = new AtomNode();
            if ( j == 0 ) {
                atom.info.putValue("resi", residue_number_1);
                atom.info.putValue("resn", residue_name_1);
                atom.info.putValue("name", atom_name_1);
            } else {
                atom.info.putValue("resi", residue_number_2);
                atom.info.putValue("resn", residue_name_2);
                atom.info.putValue("name", atom_name_2);
            }
            atom_list.add(atom);
            root.atoms.add(atom_list);
        }
        stack.add(root);
    }
    
} 
