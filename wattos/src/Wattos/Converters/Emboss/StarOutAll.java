/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */ 
package Wattos.Converters.Emboss;


import EDU.bmrb.starlibj.*;

import com.braju.format.*;              // printf equivalent
import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.io.*; 
import java.util.*;
import java.util.jar.Attributes;


/** StarOutAll class is used to produce Star file output from Emboss input
 * files.
 */

public class StarOutAll{
    
    /** Using starlibj to produce star format of a saveframe.
     */
    static StarFileNode sfn = null;
    /** StarlibJ unparser.
     */    
    static StarUnparser myUnparser = null;

    /** Input file name.
     */    
    static String inputFile;
    /** Output file name.
     */    
    static String outFile;
        
    /** Main data resides in this vector. Vectors passed from EmbossParserAll by calling corresponding pop method
     */    
    static Vector stack;
    /** All comments are collected in this vector.
     */
    static Vector cmtStack;
    /** All parse errors are collected in this vector.
     */
    static Vector errStack;
    
    /** Which type of data should be parsed? Default is distance. 
     */
    public static int data_type  = Varia.DATA_TYPE_DISTANCE;
    
    public static final String STAR_EMPTY = Wattos.Utils.NmrStar.STAR_EMPTY;
    /** Set to produce very verbose output.
     */    
    static boolean DEBUG = true;
    
    /** The parser as defined by the JavaCC .jj file. Dummy stream.
     */    
    static EmbossParserAll parser = new EmbossParserAll(System.in);    
    
    /** Empty constructor.     */
    public StarOutAll() {
        init();
    }
    

    /** Initialize this class global variables. Needs to be executed before
     *each run.
     */
    public static void init() {
        sfn = null;
        myUnparser = null;

        inputFile = null;
        outFile = null;

        //Vectors passed from EmbossParserAll by calling corresponding pop method
        stack = null;
        cmtStack = null;
        errStack = null;
        
        data_type  = Varia.DATA_TYPE_DISTANCE;
                
        parser.init();
        EmbossParserAll.ReInit( System.in );        
    }
    
    /** Does the conversion from a single text String to a StarFileNode.
     * Returns null if an error occurred. If input is a null reference the given
     * input file will be tried. If the input text is given a temporary file
     * will be materialized for the sake of this class.
     * Speed this up by using in memory representation.
     * @param input A textual representation of the input. If a null reference the input file
     * as defined by the class variable will used.
     * @param type Type of data, e.g. distance, dihedral, etc...
     * @return <CODE>true</CODE> for success.
     */
    static public StarFileNode convertToStarFileNode( String input,  int type, int star_version ) {
        
        data_type =  type;
        File temp_file = null;
        if ( input != null ) {
            if ( inputFile == null ) {
                try {
                    temp_file = File.createTempFile("input", ".tmp" );
                    temp_file.deleteOnExit();
                    inputFile = temp_file.getCanonicalPath();
                } catch ( IOException e ) {
                    General.showThrowable(e);
                    return null;
                }
            }
            // Materialize the input as a real file. 
            Strings.writeToFile( input, inputFile );
        }
        
        // Check input file's existence
        File inputf = null;
        if ( temp_file == null ) {
            inputf = new File( inputFile );
        } else {
            inputf = temp_file;
        }
        
        if ( ! inputf.exists() ) {
            General.showError("The input file: " + inputFile + " does not exist. ");
            General.showError( "Make sure it does. The current working directory is: " +
            System. getProperty("user.dir") );
            showUsage();
        }
        inputf = null; // Not used anymore.
        
        //General.showOutput("Starting parse");
        parser.parse(inputFile, data_type);
        //pop result from EmbossParserAll
        //General.showOutput("Pop stacks");
        stack    = parser.popResult();
        cmtStack = parser.popComment();
        errStack = parser.popError();
        //General.showOutput("Finished parsing");

        /** Gets results from EmbossParserAll generate the STAR structure in memory and
         * then unparses this out to a STAR file using starlibj.
         */ 
        //Create Saveframe template first
        sfn = new StarFileNode();
        //initialize a BlockNode (for adding to StarFileNode)
        BlockNode emboss_block       = new BlockNode("data_emboss");
        //initialize a SaveFrameNode (for adding to BlockNode)
        SaveFrameNode constraints   = new SaveFrameNode("save_constraints");

        // Create saveframenode with data
        saveData(constraints, star_version);

        emboss_block.addElement(constraints);
        sfn.addElement(emboss_block);
        // Some debug statement:
        //General.showOutput("Different method shows:\n" + Wattos.Utils.NmrStar.toString( sfn ));        
        
        return sfn;
    }
    
    
    /** Does the conversion from file to file.
     * If input is a null reference the input file from inputFile will be tried.
     * @param text A textual representation of the input. If a null reference the input file
     * as defined by the class variable will used.
     * @param type Type of data, e.g. distance, dihedral, etc...
     * @return Returns <CODE>false</CODE> if an error occurred.
     */
    public static boolean convertStar( String text, int type, int star_version  ) {        
        
        File outputf = new File( outFile );
        if ( outputf.exists() ) {
            General.showWarning("The file will be deleted.");
            outputf.delete();
        }
        outputf = null; // Not used anymore.

        StarFileNode sfn = convertToStarFileNode( text, type, star_version );
        try{            
            // Now output the result to standard output:
            myUnparser = new StarUnparser(new java.io.FileOutputStream(outFile));
            myUnparser.setFormatting( true );
            myUnparser.writeOut( sfn, 0 );
        }//try
        catch( FileNotFoundException e){
            General.showError("File Not found exception in writeStar" );
            General.showThrowable(e);
        }
        catch( Exception e){
            General.showThrowable(e);
        }

        return true;
    }
        
    
    /** Prints a message showing how to use the main of this program.
     */
    public static void showUsage() {
        General.showError("Usage:java Wattos.Converters.Emboss.StarOut <input_file> <output_file> <data type id> <nmrstar version id>");
        General.showError( "DATA_TYPE_DISTANCE: " + Varia.DATA_TYPE_DISTANCE);
        General.showError( "DATA_TYPE_DIHEDRAL: " + Varia.DATA_TYPE_DIHEDRAL);
        General.showError( "NMR-STAR version 2.1.1: " + NmrStar.STAR_VERSION_2_1_1);
        General.showError( "NMR-STAR version 3.0  : " + NmrStar.STAR_VERSION_3_0);
        System.exit(1);
    }
                   
    
    /** Save all the data in an in-memory structure of the STAR file to be generated.
     * @param save_frame_node_constraint A reference to the saveframe node of the constraints to which the data should
     * be added.
     * @return <CODE>true</CODE> for success.
     */
    public static boolean saveData(SaveFrameNode save_frame_node_constraint, int star_version ) {
        
        Parameters p = new Parameters(); // Printf parameters
        // Print the filename without path and the characteristics of the parse
        File iFile = new File( inputFile );
        p.add( iFile.getName() );
        p.add( stack.size());
        p.add( cmtStack.size());
        p.add( errStack.size());
        String ls = System.getProperty("line.separator");
        General.showOutput( Format.sprintf("%-20s %4d %4d %4d" + ls, p));
        if ( errStack.size() > 0 ) {
            General.showError("please check the input. An error was recorded.");
        }        
        

        if (stack.size() != 0) {
            switch ( data_type ) {
                case Varia.DATA_TYPE_DISTANCE:
                    NmrStar.addConstraintDistance(  save_frame_node_constraint, star_version );
                    saveDataDistance(               save_frame_node_constraint, star_version );
                    break;
                case Varia.DATA_TYPE_DIHEDRAL:
                    NmrStar.addConstraintDihedral(  save_frame_node_constraint, star_version );
                    saveDataDihedral(               save_frame_node_constraint, star_version );
                    break;
                case Varia.DATA_TYPE_DIPOLAR_COUPLING:
                    NmrStar.addConstraintDipolarCoupling(  save_frame_node_constraint, star_version );
                    saveDataDipolarCoupling(               save_frame_node_constraint, star_version );
                    break;
                default:
                    General.showError("none existing data type entered (code bug?):" + data_type);
            }
        }

        String tableNamePrefix = NmrStar.getMRDataType2TableNamePrefix( Varia.DATA_TYPE_NAMES[data_type]);
        // Add comments
        if (cmtStack.size() != 0) {
            NmrStar.addConstraintComments(      save_frame_node_constraint, star_version, tableNamePrefix);
            DataLoopNode dataloopnode_comment       = (DataLoopNode) save_frame_node_constraint.lastElement();
            String[] variable_names_comment = new String[6];
            int id = 0;
            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    variable_names_comment[id++] = "commentId";
                    variable_names_comment[id++] = "comment";
                    variable_names_comment[id++] = "beginLine";
                    variable_names_comment[id++] = "beginColumn"; 
                    variable_names_comment[id++] = "endLine";
                    variable_names_comment[id++] = "endColumn";
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: 
                case NmrStar.STAR_VERSION_3_1: {
                    variable_names_comment[id++] = "commentId";
                    variable_names_comment[id++] = "beginLine";
                    variable_names_comment[id++] = "beginColumn"; 
                    variable_names_comment[id++] = "endLine";
                    variable_names_comment[id++] = "endColumn";
                    variable_names_comment[id++] = "comment";
                    break;
                }
                default: {
                    General.showError("code bug Converters.Emboss.saveData Unknown NMR-STAR format: " + star_version );
                }                            
            }                                                            
            LoopTableNode loop_table_node_comment = dataloopnode_comment.getVals();
            for (int i = 0; i < cmtStack.size(); i++) {
                Comment cmtTemp = (Comment) (cmtStack.elementAt(i));
                LoopRowNode looprownode_comment = new LoopRowNode();
                for (int j = 0; j < variable_names_comment.length; j++) {
//                    String name = variable_names_comment[j];
                    String value = (String) cmtTemp.entry.getValue( variable_names_comment[j] );
                    looprownode_comment.addElement(new DataValueNode( value ));
                }
                loop_table_node_comment.addElement( looprownode_comment );        
            }            
        }
        
        // Add parse errors
        if (errStack.size() != 0) {
            NmrStar.addConstraintParseErrors(   save_frame_node_constraint, star_version, tableNamePrefix);
            DataLoopNode dataloopnode_parse_error   = (DataLoopNode) save_frame_node_constraint.lastElement();
            String[] variable_names_parse_error = new String[6];
            int id = 0;
            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    variable_names_parse_error[id++] = "errorId";
                    variable_names_parse_error[id++] = "error";
                    variable_names_parse_error[id++] = "beginLine";
                    variable_names_parse_error[id++] = "beginColumn"; 
                    variable_names_parse_error[id++] = "endLine";
                    variable_names_parse_error[id++] = "endColumn";
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: 
                case NmrStar.STAR_VERSION_3_1: {
                    variable_names_parse_error[id++] = "errorId";
                    variable_names_parse_error[id++] = "beginLine";
                    variable_names_parse_error[id++] = "beginColumn"; 
                    variable_names_parse_error[id++] = "endLine";
                    variable_names_parse_error[id++] = "endColumn";
                    variable_names_parse_error[id++] = "error";
                    break;
                }
                default: {
                    General.showError("code bug Converters.Emboss.saveData Unknown NMR-STAR format: " + star_version );
                }                            
            }                                                            
            LoopTableNode loop_table_node_parse_error = dataloopnode_parse_error.getVals();
            for (int i = 0; i < errStack.size(); i++) {
                ParseError errTemp = (ParseError) (errStack.elementAt(i));
                LoopRowNode looprownode_parse_error = new LoopRowNode();
                for (int j = 0; j < variable_names_parse_error.length; j++) {
//                    String name = variable_names_parse_error[j];
                    String value = (String) errTemp.entry.getValue( variable_names_parse_error[j] );
                    looprownode_parse_error.addElement(new DataValueNode( value ));
                }
                loop_table_node_parse_error.addElement( looprownode_parse_error );        
            }
        }     
        
        // Fill all tables with a dummy row just to show the tags. Is that ok to do?
        boolean doEmptyTablesOnly = true;
        NmrStar.addEmptyRowToAllTables( save_frame_node_constraint, doEmptyTablesOnly );
        return true;
    }
    
    
    /** Save the dihedral angle data in an in-memory structure of the STAR file to be generated.
     * @param save_frame_node_constraint A reference to the saveframe node of the constraints to which the data should
     * be added.
     */
    public static void saveDataDihedral(SaveFrameNode save_frame_node_constraint, int star_version) {
        
        VectorCheckType vct = save_frame_node_constraint.searchForType(NmrStar.dataLoopNodeClass, DataValueNode.DONT_CARE);
        if ( vct.size() !=  1 ) {
            General.showError("didn't find exactly 1 table in saveframe node. Found: " + vct.size() );
            General.showError( NmrStar.toString( save_frame_node_constraint) );
            return;
        }
        DataLoopNode dataloopnode_constraint     = (DataLoopNode) vct.elementAt(0);

        LoopTableNode loop_table_node_constraint = dataloopnode_constraint.getVals();
        // Four atoms define a dihedral angle; no more, no less
        int atom_count = 4;
        for (int i = 0; i < stack.size(); i++) {
            LogicalNode treeNode = (LogicalNode)(stack.elementAt(i));
            LoopRowNode looprownode_constraint = new LoopRowNode();
            
            looprownode_constraint.addElement(new DataValueNode( Integer.toString(i+1)));
            looprownode_constraint.addElement(new DataValueNode( Wattos.Utils.NmrStar.STAR_EMPTY ));
            // Only the middle elements are different between the 2 versions right now.
            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: {
                    int values_per_atom = 5;
                    int empties = atom_count*values_per_atom;
                    for (int j = 0; j < empties; j++) {                
                        looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    }
                    break;
                }
                case NmrStar.STAR_VERSION_3_1: {
                    int values_per_atom = 8;
                    int empties = atom_count*values_per_atom;
                    for (int j = 0; j < empties; j++) {                
                        looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    }
                    break;
                }
                
                default: {
                    General.showError("code bug saveDataDihedral. Unknown NMR-STAR format: " + star_version );
                    return;
                }
            }

            if ( star_version >= NmrStar.STAR_VERSION_3_1 ) {
                looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
            }
            
            // Store info on all 4 atoms
            for (int j = 0; j < atom_count; j++) {                
                ArrayList atom_list = (ArrayList) treeNode.atoms.get(j);
                AtomNode atom = (AtomNode) atom_list.get(0);
                for (int k = 0; k < AtomNode.variable_names_atom.length; k++) {                
                    String value = NmrStar.getPossibleValue( atom.info.getValue(AtomNode.variable_names_atom[k]) );
                    looprownode_constraint.addElement(new DataValueNode( value ));
                }
            }

//            looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
//            looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
//            looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
//            looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
            
            loop_table_node_constraint.addElement( looprownode_constraint );        
        }        
    }
    
    /** Residual dipolar coupling section.
     * @param save_frame_node_constraint A reference to the saveframe node of the constraints to which the data should
     * be added.
     * 
     * Not implemented really as not observed anywhere for emboss
     */    
    public static void saveDataDipolarCoupling(SaveFrameNode save_frame_node_constraint, int star_version) {
        
        VectorCheckType vct = save_frame_node_constraint.searchForType(NmrStar.dataLoopNodeClass, DataValueNode.DONT_CARE);
        if ( vct.size() !=  1 ) {
            General.showError("didn't find exactly 1 table in saveframe node. Found: " + vct.size() );
            General.showError( NmrStar.toString( save_frame_node_constraint) );
            return;
        }
        DataLoopNode dataloopnode_constraint     = (DataLoopNode) vct.elementAt(0);

        LoopTableNode loop_table_node_constraint = dataloopnode_constraint.getVals();
        // Two atoms define a coupling; no more, no less
        int atom_count = 2;
        for (int i = 0; i < stack.size(); i++) {
            LogicalNode treeNode = (LogicalNode)(stack.elementAt(i));
            LoopRowNode looprownode_constraint = new LoopRowNode();
            looprownode_constraint.addElement(new DataValueNode( Integer.toString(i+1)));
            
            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));  // coupling code
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: {
                    int values_per_atom = 5;
                    int empties = atom_count*values_per_atom;
                    for (int j = 0; j < empties; j++) {                
                        looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    }
                    break;
                }
                default: {
                    General.showError("code bug saveDataDipolarCoupling. Unknown NMR-STAR format: " + star_version );
                    return;
                }
            }
            
            // Store info on both atoms
            for (int j = 0; j < atom_count; j++) {                
                AtomNode atom = (AtomNode) ((ArrayList) treeNode.atoms.get(j)).get(0);
                for (int k = 0; k < AtomNode.variable_names_atom.length; k++) {                
                    String value = NmrStar.getPossibleValue( atom.info.getValue(AtomNode.variable_names_atom[k]) );
                    looprownode_constraint.addElement(new DataValueNode( value ));
                }
            }

            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "value" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "error" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: {
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "value" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "error" ) ));
                    break;
                }
                default: {
                    General.showError("code bug saveDataDipolarCoupling. Unknown NMR-STAR format: " + star_version );
                    return;
                }
            }

            loop_table_node_constraint.addElement( looprownode_constraint );        
        }        
    }
    

    /** Save the distance data in an in-memory structure of the STAR file to be generated. 
     *Admittedly , this code is a bit of a mess.
     * @param save_frame_node_constraint A reference to the saveframe node of the constraints to which the data should
     * be added.
     */
    public static void saveDataDistance(SaveFrameNode save_frame_node_constraint, int star_version) {
        
        
//        String peak, volume, weight, ppm1, ppm2;
        String peak, volume;
        int atom_count, atom_list_count, number_nodes;
        LoopRowNode looprownode_logic;

        int pos = 0;
            
        VectorCheckType vct = save_frame_node_constraint.searchForType(NmrStar.dataLoopNodeClass, DataValueNode.DONT_CARE);
        if ( vct.size() !=  3 ) {
            General.showError("didn't find exactly 3 tables in saveframe node. Found: " + vct.size() );
            General.showError( NmrStar.toString( save_frame_node_constraint) );
            return;
        }
        DataLoopNode dataloopnode_logic     = (DataLoopNode) vct.elementAt(pos++);
        DataLoopNode dataloopnode_atom      = (DataLoopNode) vct.elementAt(pos++);
        DataLoopNode dataloopnode_distance  = (DataLoopNode) vct.elementAt(pos++);

        LoopTableNode loop_table_node_logic     = dataloopnode_logic.getVals();
        LoopTableNode loop_table_node_atom      = dataloopnode_atom.getVals();
        LoopTableNode loop_table_node_distance  = dataloopnode_distance.getVals();
                            
        for (int i = 0; i < stack.size(); i++) {
            
            // LOGIC LOOP
            LogicalNode treeNode = (LogicalNode)(stack.elementAt(i));
            Attributes opt_info = (Attributes) treeNode.opt_info.get( "opt_info" );            
            if ( opt_info != null ) {
                peak     = NmrStar.getPossibleValue( opt_info.getValue( "peak" ) );
//                weight   = NmrStar.getPossibleValue( opt_info.getValue( "weight" ) );
                volume   = NmrStar.getPossibleValue( opt_info.getValue( "volume" ) );
//                ppm1     = NmrStar.getPossibleValue( opt_info.getValue( "ppm1" ) );
//                ppm2     = NmrStar.getPossibleValue( opt_info.getValue( "ppm2" ) );
            } else {
                peak     = STAR_EMPTY;
//                weight   = STAR_EMPTY;
                volume   = STAR_EMPTY;
//                ppm1     = STAR_EMPTY;
//                ppm2     = STAR_EMPTY;
            }
            // Number of nodes is 1 for logic only node (1) and 1 for first
            // atom pair (2).
            // This is only valid if there are ARIA type of restraints.
            number_nodes = treeNode.atomsOR.size() / 2 + 2; 
            //General.showOutput("number_nodes: " + number_nodes);

            // There was an ARIA type restraint.
            if ( treeNode.atomsOR.size() != 0 ) {
                
                for (int j=1; j<=number_nodes; j++ ) {
                    looprownode_logic       = new LoopRowNode();            
                    looprownode_logic.addElement(   new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                    looprownode_logic.addElement(   new DataValueNode( Integer.toString(        j) ));
                    String sibling = STAR_EMPTY;
                    String child   = STAR_EMPTY;
                    String logic   = STAR_EMPTY;
                    if ( j == 1 ) {
                        child = Integer.toString( 2 );
                        logic = "OR";
                    }                        
                    if ( j != 1 && j < number_nodes ) {
                        sibling = Integer.toString( j + 1);
                    }                        
                    looprownode_logic.addElement(   new DataValueNode( child ));
                    looprownode_logic.addElement(   new DataValueNode( sibling ));
                    looprownode_logic.addElement(   new DataValueNode( logic ));
                    loop_table_node_logic.addElement(       looprownode_logic );
                }                    
            } else {
                looprownode_logic       = new LoopRowNode();            
                looprownode_logic.addElement(   new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                looprownode_logic.addElement(   new DataValueNode( treeNode.entry.getValue( "treeNodeId" )));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                loop_table_node_logic.addElement(       looprownode_logic );
            }

            // ATOM LOOP
            // There was an ARIA type restraint.
            //General.showOutput("i =" + i );
            if ( treeNode.atomsOR.size() != 0 ) {
                // Do node -2-
                atom_list_count = treeNode.atoms.size();
                // General.showOutput("Node 2 has number of atoms: " + atom_count);
                // Store info on all atoms from first pair (might be variable too)
                for (int j = 0; j < atom_list_count; j++) {
                    //General.showOutput("i =" + i + " j =" + j );
                    ArrayList atom_list = (ArrayList) treeNode.atoms.get(j);
                    atom_count = atom_list.size();
                    for (int k = 0; k < atom_count; k++) {                
                        //General.showOutput("i =" + i + " j =" + j + " k =" + k);
                        
                        LoopRowNode looprownode_atom        = new LoopRowNode();
                        AtomNode atom = (AtomNode) (atom_list.get(k));                
                        String segi = NmrStar.getPossibleValue( atom.info.getValue( "segi" ) );
                        String resi = NmrStar.getPossibleValue( atom.info.getValue( "resi" ) );
                        String resn = NmrStar.getPossibleValue( atom.info.getValue( "resn" ) );
                        String name = NmrStar.getPossibleValue( atom.info.getValue( "name" ) );
                        looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                        looprownode_atom.addElement(    new DataValueNode( "2" ));
//                        looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_atom.addElement(    new DataValueNode( Integer.toString(j+1) ));
                        switch ( star_version ) {
                            case NmrStar.STAR_VERSION_2_1_1: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_0: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_1: {
                                for (int tt=0;tt<8;tt++) {                            
                                    looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                }
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            default: {
                                General.showError("code bug Converters.Emboss.saveDataDistance. Unknown nmr-star format: " + star_version );
                            }                            
                        }                        
                        loop_table_node_atom.addElement(        looprownode_atom );
                    }                        
                }
                // Do node -3- and beyond.                
                atom_list_count = treeNode.atomsOR.size();
                // General.showOutput("Node 2 has number of atoms: " + atom_count);
                // Store info on all atoms from first pair (might be variable too)
                for (int j = 0; j < atom_list_count; j++) {
                    ArrayList atom_list = (ArrayList) treeNode.atomsOR.get(j);
                    atom_count = atom_list.size();
                    for (int k = 0; k < atom_count; k++) {                
                        LoopRowNode looprownode_atom        = new LoopRowNode();
                        AtomNode atom = (AtomNode) (atom_list.get(k));                
                        String segi = NmrStar.getPossibleValue( atom.info.getValue( "segi" ) );
                        String resi = NmrStar.getPossibleValue( atom.info.getValue( "resi" ) );
                        String resn = NmrStar.getPossibleValue( atom.info.getValue( "resn" ) );
                        String name = NmrStar.getPossibleValue( atom.info.getValue( "name" ) );
                        looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                        looprownode_atom.addElement(    new DataValueNode( Integer.toString(3+j/2) ));
//                        looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_atom.addElement(    new DataValueNode( Integer.toString(j%2+1) ));
                        switch ( star_version ) {
                            case NmrStar.STAR_VERSION_2_1_1: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_0: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_1: {
                                for (int tt=0;tt<8;tt++) {                            
                                    looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                }
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            default: {
                                General.showError("code bug Converters.Emboss.saveDataDistance. Unknown nmr-star format: " + star_version );
                            }
                        }
                        loop_table_node_atom.addElement(        looprownode_atom );
                    }                        
                }
            } else {
                atom_list_count = treeNode.atoms.size();
                // General.showOutput("Node 2 has number of atoms: " + atom_count);
                // Store info on all atoms from first pair (might be variable too)
                for (int j = 0; j < atom_list_count; j++) {
                    //General.showOutput("i =" + i + " j =" + j );
                    ArrayList atom_list = (ArrayList) treeNode.atoms.get(j);
                    atom_count = atom_list.size();
                    for (int k = 0; k < atom_count; k++) {                
                        //General.showOutput("i =" + i + " j =" + j + " k =" + k);
                        
                        LoopRowNode looprownode_atom        = new LoopRowNode();
                        AtomNode atom = (AtomNode) (atom_list.get(k));                
                        String segi = NmrStar.getPossibleValue( atom.info.getValue( "segi" ) );
                        String resi = NmrStar.getPossibleValue( atom.info.getValue( "resi" ) );
                        String resn = NmrStar.getPossibleValue( atom.info.getValue( "resn" ) );
                        String name = NmrStar.getPossibleValue( atom.info.getValue( "name" ) );
                        looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                        looprownode_atom.addElement(    new DataValueNode( "1" ));
//                        looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_atom.addElement(    new DataValueNode( Integer.toString(j+1) ));
                        switch ( star_version ) {
                            case NmrStar.STAR_VERSION_2_1_1: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_0: {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            case NmrStar.STAR_VERSION_3_1: {
                                for (int tt=0;tt<8;tt++) {                            
                                    looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                                }

                                looprownode_atom.addElement(    new DataValueNode( segi ));
                                looprownode_atom.addElement(    new DataValueNode( resi   ));
                                looprownode_atom.addElement(    new DataValueNode( resn ));
                                looprownode_atom.addElement(    new DataValueNode( name));
                                break;
                            }
                            default: {
                                General.showError("code bug Converters.Emboss.saveDataDistance. Unknown nmr-star format: " + star_version );
                            }
                        }
                        loop_table_node_atom.addElement(        looprownode_atom );
                    }                        
                }
            }
            // DISTANCE LOOP 
            // Logic might also be nice to use to prevent repetition in previous code block.
            int node_number_start_distance = 1;
            // There was an ARIA type restraint.            
            if ( treeNode.atomsOR.size() != 0 ) {
                node_number_start_distance = 2;
            } else {
                number_nodes = 1;
            }
            
            
            for (int j=node_number_start_distance; j<=number_nodes; j++ ) {                                
                LoopRowNode looprownode_distance    = new LoopRowNode();
                looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                looprownode_distance.addElement(    new DataValueNode( Integer.toString(j) ));
                looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                looprownode_distance.addElement(    new DataValueNode( peak ));
                switch ( star_version ) {
                    case NmrStar.STAR_VERSION_2_1_1: {
//                        looprownode_distance.addElement(    new DataValueNode( weight ));
                        looprownode_distance.addElement(    new DataValueNode( volume ));
//                        looprownode_distance.addElement(    new DataValueNode( ppm1 ));
//                        looprownode_distance.addElement(    new DataValueNode( ppm2 ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                        break;
                    }
                    case NmrStar.STAR_VERSION_3_0: 
                    case NmrStar.STAR_VERSION_3_1: {
                        looprownode_distance.addElement(    new DataValueNode( volume ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" ) ));
//                        looprownode_distance.addElement(    new DataValueNode( weight ));                        
//                        looprownode_distance.addElement(    new DataValueNode( ppm1 ));
//                        looprownode_distance.addElement(    new DataValueNode( ppm2 ));
                        break;
                    }
                    default: {
                        General.showError("code bug Converters.Emboss.saveDataDistance. Unknown nmr-star format: " + star_version );
                    }
                }
                loop_table_node_distance.addElement(    looprownode_distance );        
            }        
        }        
    }

    
    //main
    /** The main to call for the conversion giving input and output files on the command
     * line.
     * @param args First argument : input file (EMBOSS)
     * Second argument: output file (STAR)
     * Third argument : data type (e.g. 1 for distances)
     *
     * <PRE>Usage:java Wattos.Converters.Emboss.StarOutAll <input_file> <output_file> <[1-3]></PRE>
     */    
    public static void main(String[] args) {
                
        //check for arguments, require one Emboss template file (.config file) followed by
        //the Emboss file block to be processed
        if (args.length != 4) {
            General.showError("number of arguments should be four");
            showUsage();
        }
        
        init();
        
        inputFile  = args[0];
        outFile    = args[1]; 
        data_type  = (new Integer( args[2])).intValue();
        int star_version = (new Integer( args[3])).intValue();
        
        convertStar( null, data_type, star_version );
    }
}
