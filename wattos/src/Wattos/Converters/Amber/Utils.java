/*
 * Utils.java
 *
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Amber;


import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.util.*;
import java.io.*;

/** Common routines for parsing Amber constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Utils {

    /** Shorthand for AmberParserAllConstants namesake.
     */    
    static final int POUNDCOMMENT    = AmberParserAllConstants.POUNDCOMMENT;
    /** Shorthand for AmberParserAllConstants namesake.
     */    
    static final int EOF            = AmberParserAllConstants.EOF;
    /** Number of atoms that define a distance restraint */
    static int membersCountDistance = 2;

    /** Number of atoms that define a dihedral angle restraint */
    static int membersCountDihedral = 4;
    /** Number of atoms that define a dipolar coupling restraint */
    static int membersCountDipolarCoupling = 2;

    
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
        if (token.kind ==POUNDCOMMENT) {
            content = content.substring(1).trim();
        } else {
            // Might have been possible to get descriptive name but that takes
            // too much reflection.
            General.showError("code bug in getTextInCommentToken");
            General.showError( "Unknown token.kind:" + token.kind);
            General.showError( "See values defined for them in class AmberParserAllConstants" );
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
        t = AmberParserAll.getToken(1);
        //Store the last comment before EOF if error is due to EOF
        if (t.kind == EOF) {
            t = AmberParserAll.getNextToken(); 
            saveComment(t, cmtStack);
            done = true;
        }
        do{
            t = AmberParserAll.getToken(1);

            // Consume the token only if it is NOT the
            // one we are looking for, or if the flag   
            // was set to consume the last token:
            if( (consume || t.kind != kind) && t.kind != EOF) {
                tmp = AmberParserAll.getNextToken();
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


    /** Returns null if the key has not at least a String attribute
     */
    public static String getPossibleSingleAtr( HashMap attrSet, String key ) {
        ArrayList tmp = (ArrayList) attrSet.get( key );
        if ( (tmp == null) || (tmp.size()!=1) ) {
            return null;
        }
        Object o = tmp.get(0);
        if ( o instanceof String ) {
            return (String) o;           
        }        
        return null;        
    }
    
    /** Sometimes extra bogus (-1) atoms are given as in entry 1buf for the
     *hydrogen bonds
     */
    public static void removeNegativeOnesBeyondFirstTwo( ArrayList iatList ) {
        for (int i=2;i<iatList.size();i++ ) {
            Object o = iatList.get(i);
            if ( o.equals( "-1" )) {
                iatList.remove(i);
                i--;
            }
        }
    }
    /** Put the constraints in there native data structure. In attrSet there should be
     *a HashMap with String keys and ArrayList values.
     */    
    public static void saveAssiStateDistance( Vector stack,
        HashMap attrSet) {

        ArrayList iatList = (ArrayList) attrSet.get( "iat" );
        if ( (iatList==null)) {
            General.showError("Didn't find any elements in iat list as expected, not saving restraint.");
            General.showError("Attributes: " + Strings.toString(attrSet));
            return;
        }        
        removeNegativeOnesBeyondFirstTwo( iatList );
        // Let's expand the atoms from the list if need be.
        if ( iatList.size() != 2 ) {
            General.showError("Didn't find exactly two elements in iat list as expected, not saving restraint.");
            General.showError("Attributes: " + Strings.toString(attrSet));
            return;
        }        
        LogicalNode root = new LogicalNode();
        int[]atomL = null;
        
        int iresid = 0;
        String iresidStr = getPossibleSingleAtr( attrSet, "iresid" );
        if ( ( iresidStr!=null ) && iresidStr.equals("1") ) {
            iresid = 1;
        }
        
        try {
            // Do both sides
            if ( iresid == 0 ) { // Normal operation according to Amber manual.
                for (int member=0;member<2;member++) {
                    ArrayList igrList = (ArrayList) attrSet.get( "igr" + Integer.toString(member+1) );
                    ArrayList select = new ArrayList();
                    root.atoms.add( select );
                    int iat = Integer.parseInt( (String) iatList.get(member) );
                    if ( iat < 0 ) {
                        if ( (igrList == null) || ( igrList.size() < 1)) {
                            General.showError("iat for atom was defined below zero but didn't find a valid igr " + (member+1) + " list. Not saving restraint.");
                            return;
                        }
                        atomL = new int[igrList.size()];
                        for (int i=0;i<igrList.size();i++) {
                            atomL[i] = Integer.parseInt( (String) igrList.get(i) );
                        }
                    } else {
                        atomL = new int[1];
                        atomL[0] = iat;
                    }
                    for (int i=0;i<atomL.length;i++) {
                        AtomNode atomNode = new AtomNode();
                        //General.showDebug("Putting atom # in: " + Integer.toString( atomL[i] ));
                        atomNode.info.putValue("id", Integer.toString( atomL[i] ) );
                        select.add(atomNode);
                    }
                }
            } else { // Mode in which the iat info encodes residue numbers in stead of atom ids
                     // Even though groups could be used with attribute grnam they never are in the mr files at 2004-11-04
                for (int member=0;member<2;member++) {               
                    ArrayList select = new ArrayList();
                    root.atoms.add( select );
                    int iat = Integer.parseInt( (String) iatList.get(member) );
                    if ( iat < 0 ) {
                        General.showError("iat for atom was defined below zero but iresid is 1. Not saving restraint.");
                        return;
                    }
                    String name = getPossibleSingleAtr( attrSet, ("atnam"+(member+1)) );
                    if ( name==null ) {
                        General.showError("Can't find a single valid atom name. Not saving restraint.");
                        General.showError("Attributes are: " + Strings.toString(attrSet));
                        return;                        
                    }
                    name = Strings.stripSingleQuotes( name );
                    name = name.trim();
                    AtomNode atomNode = new AtomNode();
                    select.add(atomNode);

                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.SEGI_POS], Wattos.Utils.NmrStar.STAR_EMPTY);
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.RESI_POS], (String) iatList.get(member));
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.RESN_POS], Wattos.Utils.NmrStar.STAR_EMPTY);
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.NAME_POS], name);
                }
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return;
        }
        
        
        // Storing all values into logical node starting at 1
        root.entry.putValue("treeId",       (new Integer(stack.size() + 1)).toString() );
        root.entry.putValue("treeNodeId",   (new Integer(1)).toString() );
        String ixpk = Varia.getPossibleStringFromHashMap(attrSet, "ixpk" );
        if ( ! ixpk.equalsIgnoreCase("0") ) {
            root.entry.putValue("peak",ixpk );
        }
        root.entry.putValue("lower", Varia.getPossibleStringFromHashMap(attrSet, "r2" ));
        root.entry.putValue("value", Varia.getPossibleStringFromHashMap(attrSet, "r3" ));        
        root.entry.putValue("upper", Varia.getPossibleStringFromHashMap(attrSet, "r4" ));
        
        root.entry.putValue("weight",Varia.getPossibleStringFromHashMap(attrSet, "rk2" ));
        // Parse the remainder out in the class StarOutAll where the molecule info is known.
        
        root.opt_info = attrSet; // not really used now but we'll pass it anyway for consistency.
        stack.add(root);
    }
    
    /** Put the constraints in there native data structure.
     */

    public static void saveAssiStateDihedral( Vector stack,
        HashMap attrSet) {

        LogicalNode root = new LogicalNode();
        // Storing all values into logical node starting at 1
        root.entry.putValue("lower", Varia.getPossibleStringFromHashMap(attrSet, "r2" ) );
        root.entry.putValue("upper", Varia.getPossibleStringFromHashMap(attrSet, "r3" ) );
        root.entry.putValue("force_constant_value",         Varia.getPossibleStringFromHashMap(attrSet, "rk2" ) );
        //root.entry.putValue("potential_function_exponent",  exponent );

        ArrayList iatList = (ArrayList) attrSet.get( "iat" );
        
        // Let's expand the atoms from the list if need be.
        if ( (iatList==null) || (iatList.size() != 4 )) {
            General.showError("Didn't find exactly four elements in iat list as expected, not saving restraint.");
            General.showError("Attributes: " + Strings.toString(attrSet));
            return;
        }        
        int[]atomL = null;
        
        int iresid = 0;
        String iresidStr = getPossibleSingleAtr( attrSet, "iresid" );
        if ( ( iresidStr!=null ) && iresidStr.equals("1") ) {
            iresid = 1;
        }
        
        try {
            // Do both sides
            if ( iresid == 0 ) { // Normal operation according to Amber manual.
                for (int member=0;member<membersCountDihedral;member++) {
                    ArrayList igrList = (ArrayList) attrSet.get( "igr" + Integer.toString(member+1) );
                    ArrayList select = new ArrayList();
                    root.atoms.add( select );
                    int iat = Integer.parseInt( (String) iatList.get(member) );
                    if ( iat < 0 ) {
                        if ( (igrList == null) || ( igrList.size() < 1)) {
                            General.showError("iat for atom was defined below zero but didn't find a valid igr " + (member+1) + " list. Not saving restraint.");
                            return;
                        }
                        atomL = new int[igrList.size()];
                        for (int i=0;i<igrList.size();i++) {
                            atomL[i] = Integer.parseInt( (String) igrList.get(i) );
                        }
                    } else {
                        atomL = new int[1];
                        atomL[0] = iat;
                    }
                    for (int i=0;i<atomL.length;i++) {
                        AtomNode atomNode = new AtomNode();
                        //General.showDebug("Putting atom # in: " + Integer.toString( atomL[i] ));
                        atomNode.info.putValue("id", Integer.toString( atomL[i] ) );
                        select.add(atomNode);
                    }
                }
            } else { // Mode in which the iat info encodes residue numbers in stead of atom ids
                     // Even though groups could be used with attribute grnam they never are in the mr files at 2004-11-04
                for (int member=0;member<membersCountDihedral;member++) {               
                    ArrayList select = new ArrayList();
                    root.atoms.add( select );
                    int iat = Integer.parseInt( (String) iatList.get(member) );
                    if ( iat < 0 ) {
                        General.showError("iat for atom was defined below zero but iresid is 1. Not saving restraint.");
                        return;
                    }
                    String name = getPossibleSingleAtr( attrSet, ("atnam"+(member+1)) );
                    if ( name==null ) {
                        General.showError("Can't find a single valid atom name for member: " + member + ". Not saving restraint.");
                        General.showError("Attributes are: " + Strings.toString(attrSet));
                        return;                        
                    }
                    name = Strings.stripSingleQuotes( name );                                        
                    name = name.trim();
                    AtomNode atomNode = new AtomNode();
                    select.add(atomNode); 
                    
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.SEGI_POS], Wattos.Utils.NmrStar.STAR_EMPTY);
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.RESI_POS], (String) iatList.get(member));
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.RESN_POS], Wattos.Utils.NmrStar.STAR_EMPTY);
                    atomNode.info.putValue(AtomNode.variable_names_atom[AtomNode.NAME_POS], name);
                    //General.showDebug("Putting atom name in: " + name);
                }
            }
        } catch ( Exception e ) {
            General.showThrowable(e);
            return;
        }
        stack.add(root); // don't add it until valid.
    }
    
    
    /** Put the constraints in there native data structure.
     */    
    public static void saveAssiStateDipolarCoupling( Vector stack,
        HashMap attrSet) {

        LogicalNode root = new LogicalNode();
        root.entry.putValue("lower", Varia.getPossibleStringFromHashMap(attrSet, "dobslxx" ) );
        root.entry.putValue("upper", Varia.getPossibleStringFromHashMap(attrSet, "dobsuxx" ) );
        root.entry.putValue("value", Varia.getPossibleStringFromHashMap(attrSet, "dobsxx" ) );
            
        for (int member=0;member<membersCountDipolarCoupling;member++) {
            ArrayList select = new ArrayList();
            root.atoms.add( select );
            String id = Varia.getPossibleStringFromHashMap(attrSet, "idxx" );
            if ( member == 1 ) {
                id = Varia.getPossibleStringFromHashMap(attrSet, "jdxx" );
            }
            General.showDebug("Putting atom # in: " + id);
            AtomNode atomNode = new AtomNode();
            atomNode.info.putValue("id", id );
            select.add(atomNode);
        }        
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
} 
