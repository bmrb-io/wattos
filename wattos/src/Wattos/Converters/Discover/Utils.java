/*
 * Utils.java
 *
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Discover;


import org.apache.regexp.*;
import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.util.*;
import java.io.*;

/** Common routines for parsing Discover constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Utils {


    /** Shorthand for DiscoverParserAllConstants namesake.
     */
    static final int POUNDCOMMENT    = DiscoverParserAllConstants.POUNDCOMMENT;
    /** Shorthand for DiscoverParserAllConstants namesake.
     */
    static final int EOF            = DiscoverParserAllConstants.EOF;

    public static RE re_getResidueIds;

    static {
        try {
            re_getResidueIds = new RE("[:digit:]");
        } catch ( RESyntaxException e) {
            General.showError("Code error: Wattos.Converters.Discover.Utils" + e.toString() );
        }
    }

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
        } else {
            // Might have been possible to get descriptive name but that takes
            // too much reflection.
            General.showError("code bug in getTextInCommentToken");
            General.showError( "Unknown token.kind:" + token.kind);
            General.showError( "See values defined for them in class DiscoverParserAllConstants" );
        }
        return content;
    }

    /** Returns the coordinates of the beginning of the current token.
     * @return The coordinates of the beginning of the current token.
     */
    public static int[] storeBeginOfRestraint() {
        Token t     = DiscoverParserAll.getToken(1);
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
            t = DiscoverParserAll.getNextToken();
        } while ( t.kind != kind &&
                  t.kind != EOF );
    }

    /** Called to skip over tokens (errors) and storing the skipped tokens
     * and the corresponding line and column of the end of the last skipped token.
     * It does not store the comments that occur between errors. It will not eat the complete line
     * that contains the current token.
     * @param kind Kind of token to sync on after this token.
     * @param errLineStack Vector of line numbers where to add the location coordinates on to.
     * @param lastAssiLine Error coordinate for beginning of restraint.
     * @param lastAssiCol Error coordinate for beginning of restraint..
     * @throws ParserDoneException Are we done?
     */
    public static void error_skipto(int kind,
            Vector errLineStack, int lastAssiLine, int lastAssiCol
        ) throws ParserDoneException {


        Token current_token = DiscoverParserAll.getToken(0);
        Token next_token    = DiscoverParserAll.getToken(1);

//        General.showOutput("-1- error_skipto: current token is :[" + current_token.image + "]");
//        General.showOutput("-1- error_skipto: next token    is :[" + next_token.image + "]");
        if ( next_token.kind == EOF ) {
            throw ( new ParserDoneException() );
        }

        while (   next_token.kind != kind &&
                  next_token.kind != EOF ) {
            current_token   = DiscoverParserAll.getNextToken();
            next_token      = DiscoverParserAll.getToken(1);
        }
        errLineStack.add(new ErrorLine( lastAssiLine, current_token.endLine,
                                        lastAssiCol,  current_token.endColumn));

        if ( next_token.kind == EOF ) {
            throw ( new ParserDoneException() );
        }
    }


    /** Put the constraints in there native data structure. At least the first
     * part.
  */
    public static void saveDistanceNormal( Vector stack,
            String atom_id1,    String atom_id2,          String lower, String upper,
            String target,      String force_const_lower, String force_const_upper,
            String force_max ) throws ParseException {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("treeId",       (new Integer(stack.size() + 1)).toString() );
        root.entry.putValue("treeNodeId",   (new Integer(1)).toString() );

        root.entry.putValue("lower",   lower    );
        root.entry.putValue("upper",   upper    );
        if ( target != null ) {
            root.entry.putValue("dist",   target    );
        }
        /** Not really used now but maybe in future
         */
        root.entry.putValue("force_const_lower",    force_const_lower    );
        root.entry.putValue("force_const_upper",    force_const_upper    );
        root.entry.putValue("force_max",            force_max    );

        ArrayList atoms_1 = getAtomIds( atom_id1 );
        ArrayList atoms_2 = getAtomIds( atom_id2 );

        root.atoms.add(atoms_1);
        root.atoms.add(atoms_2);

        stack.add(root);
    }


    /** Put the additional constraints in there native data structure. At least the first
     * part.
 */
    public static void saveDistancePlus( Vector stack, String atom_id1, String atom_id2 )
        throws ParseException {

        ArrayList atoms_1 = getAtomIds( atom_id1 );
        ArrayList atoms_2 = getAtomIds( atom_id2 );

        LogicalNode root = (LogicalNode) stack.get(stack.size()-1);
        root.atomsOR.add(atoms_1);
        root.atomsOR.add(atoms_2);
    }

    /** Parse strings like: T13 and Thy9B to their two components:
     *<PRE>
     *Residue name
     *Residue number
     *</PRE>
     *Throws the exception in case the input is non-conforming.
     */
    public static String[] getResidueIds( String residue_id ) throws ParseException {

        /** Try to match any digit */
        if ( residue_id.length() < 2 || ! re_getResidueIds.match(residue_id) ) {
            throw new ParseException();
        }

        int position_to_cut = re_getResidueIds.getParenStart(0);
        if ( position_to_cut < 1 ) {
            throw new ParseException();
        }
        String[] residue_ids = {
            residue_id.substring(0,position_to_cut),
            residue_id.substring(position_to_cut) };
        return residue_ids;
    }


    /** Parse strings like: 1:LEU_21:HB1 and 2:T13:O3' to their four components:
     *<PRE>
     *Molecular system component id
     *Residue name
     *Residue number
     *Atom name
     *</PRE>
     *New is to allow a trailing letter in the residue number to be taken as the
     *chain id as found in some of 2p7c: 1:LEU_49B:HD11.
     */
    public static ArrayList getAtomIds( String atom_id ) throws ParseException {

        StringTokenizer st;
        boolean exception_caught = false;

        ArrayList atoms = new ArrayList();
        AtomNode atom_node = new AtomNode();
        atoms.add( atom_node );

        /** Input should at least be 6 characters long.
         */
        if ( atom_id == null || atom_id.length() < 6 ) {
            General.showWarning("atom_id is null or the length is shorter than 6 characters: ["+
                atom_id + "]");
            General.showWarning("Using empty atom identifier.");
            return atoms;
        }

        try {
            st = new StringTokenizer(atom_id, ":_");
            atom_node.info.putValue("segi", st.nextToken());
            atom_node.info.putValue("resn", st.nextToken());
            atom_node.info.putValue("resi", st.nextToken());
            atom_node.info.putValue("name", st.nextToken());
        } catch ( NoSuchElementException e ) {
            /** Try parsing into 3 initially by just using ":" as
             *separator
             */
            try {
                st = new StringTokenizer(atom_id, ":");
                atom_node.info.putValue("segi", st.nextToken());
                String[] residue_ids = getResidueIds( st.nextToken() );
                if ( residue_ids == null ) {
                    throw new NoSuchElementException();
                }
                atom_node.info.putValue("resn", residue_ids[0]);
                atom_node.info.putValue("resi", residue_ids[1]);
                atom_node.info.putValue("name", st.nextToken());

            } catch ( NoSuchElementException no_such_element_exception ) {
                exception_caught = true;
            } catch ( ParseException parse_exception ) {
                exception_caught = true;
            }

            if ( exception_caught ) {
                General.showOutput("WARNING: failed to parse atom id string: ["+atom_id+"]");
                throw new ParseException();
            }
        }

        /** A few rare cases (e.g. 1pon) have atom_id like: "1:PHE_14:HD1,HE1,HE2,HD2"
         */
        String atom_name = atom_node.info.getValue("name");

        if ( (atom_name != null) && (atom_name.indexOf(",") > 0) ) {
            // Remove the old atom node
            atoms.remove(0);
            StringTokenizer st_2 = new StringTokenizer(atom_name, ",");
            while ( st_2.hasMoreTokens() ) {
                AtomNode atom_node_2 = (AtomNode) atom_node.clone();
                atom_node_2.info.putValue("name", st_2.nextToken());
                atoms.add( atom_node_2 );
            }
        }
        // For when the last char in the residue number is the segi
        String resi = atom_node.info.getValue("resi");
        char lastChar = resi.charAt(resi.length()-1);
        if ( ! Character.isDigit(lastChar)) {
            atom_node.info.putValue("segi", Character.toString(lastChar));
            atom_node.info.putValue("resi", resi.substring(0,resi.length()-1));
        }
        return atoms;
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
                    if (line.length() > 0) {
                        lastNonEmptyLineEndCol = line.length();
                    }
                }
                // Error at the same line
                if (errTemp.line[1] == errTemp.line[0]) {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] -1, errTemp.col[1] );
                } else {
                    error = error + Strings.substringInterpetTabs( line, errTemp.col[0] - 1, -1 ) + General.eol;
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
                            error = error + line + General.eol;
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


    /** Put the constraints in there native data structure.
     * @param stack Data vector.
     */
    public static void saveDihedral(Vector stack,
        String atom_id1, String atom_id2, String atom_id3, String atom_id4,
        String angle_low, String angle_high ) throws ParseException {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("lower",        angle_low);
        root.entry.putValue("upper",        angle_high);

        ArrayList atoms_1 = getAtomIds( atom_id1 );
        ArrayList atoms_2 = getAtomIds( atom_id2 );
        ArrayList atoms_3 = getAtomIds( atom_id3 );
        ArrayList atoms_4 = getAtomIds( atom_id4 );
        root.atoms.add(atoms_1);
        root.atoms.add(atoms_2);
        root.atoms.add(atoms_3);
        root.atoms.add(atoms_4);

        stack.add(root);
    }


    /** Test code for some of the routines in this class.
     * @param args Nonen to be given.
     */
    public static void main ( String[] args ) throws ParseException {
//        if ( false ) {
//            Token token = new Token();
//            // Test a few of the possible quote styles.
//            token.kind = POUNDCOMMENT;
//            if ( token.kind == POUNDCOMMENT ) {
//                token.image = "# here is {a} pound remark\n";
//            }
//            General.showOutput("token is            : [" + token.image + "]");
//            General.showOutput("token without quotes: [" +  getTextInCommentToken(token) + "]");
//        }
        if ( true ) {
            ArrayList atom_nodes;
            //atom_nodes = getAtomIds("1:PRO_18:HG2");
            atom_nodes = getAtomIds("1:A1:H8");
            //atom_nodes = getAtomIds("1:a9A:HD1,HE1,HE2,HD2");
            General.showOutput("atom_ids " + " : [" + AtomNode.toString(atom_nodes) + "]");
        }
    }
}
