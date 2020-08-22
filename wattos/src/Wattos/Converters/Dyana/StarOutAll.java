/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Converters.Dyana;


import EDU.bmrb.starlibj.*;
import com.braju.format.*;              // printf equivalent
import Wattos.Utils.*;
import Wattos.Converters.Common.*;
import java.io.*;
import java.util.*;
import java.util.jar.Attributes;


/** StarOutAll class is used to produce Star file output from Dyana input
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

    /** Main data resides in this vector. Vectors passed from DyanaParserAll by calling corresponding pop method
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

    /** Is it a lower bound definitions only?
     */
    public static boolean lower_only = false;

    public static final String STAR_EMPTY = Wattos.Utils.NmrStar.STAR_EMPTY;
    /** Set to produce very verbose output.
     */
    static boolean DEBUG = false;

    /** The parser as defined by the JavaCC .jj file. Dummy stream.
     */
    static DyanaParserAll parser = new DyanaParserAll(System.in);

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

        //Vectors passed from DyanaParserAll by calling corresponding pop method
        stack = null;
        cmtStack = null;
        errStack = null;

        data_type  = Varia.DATA_TYPE_DISTANCE;

        parser.init();
        DyanaParserAll.ReInit( System.in );
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
    static public StarFileNode convertToStarFileNode( String input,  int type,
        boolean lower_only_spec, int star_version) {

        data_type =  type;
        lower_only = lower_only_spec;

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

        parser.parse(inputFile, data_type, lower_only);
        //pop result from DyanaParserAll
        stack    = parser.popResult();
        cmtStack = parser.popComment();
        errStack = parser.popError();
        //General.showOutput("Finished parsing");

        /** Gets results from DyanaParserAll generate the STAR structure in memory and
         * then unparses this out to a STAR file using starlibj.
         */
        //Create Saveframe template first
        sfn = new StarFileNode();
        //initialize a BlockNode (for adding to StarFileNode)
        BlockNode dyana_block       = new BlockNode("data_dyana/diana");
        //initialize a SaveFrameNode (for adding to BlockNode)
        SaveFrameNode constraints   = new SaveFrameNode("save_constraints");

        // Create saveframenode with data
        saveData(constraints, star_version);

        dyana_block.addElement(constraints);
        sfn.addElement(dyana_block);
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
    public static boolean convertStar( String text, int type, boolean lower_only, int star_version ) {

        File outputf = new File( outFile );
        if ( outputf.exists() ) {
            General.showWarning("The file will be deleted.");
            outputf.delete();
        }
        outputf = null; // Not used anymore.

        StarFileNode sfn = convertToStarFileNode( text, type, lower_only, star_version );
        try{
            // Now output the result to standard output:
            myUnparser = new StarUnparser(new java.io.FileOutputStream(outFile));
            myUnparser.setFormatting( true );
            myUnparser.writeOut( sfn, 0 );
        }//try
        catch( FileNotFoundException e){
            General.showError("File Not found exception in write Star" );
            General.showThrowable(e);
        }

        return true;
    }


    /** Prints a message showing how to use the main of this program.
     */
    public static void showUsage() {
        General.showError("Usage:java Wattos.Converters.Dyana.StarOut <input_file> <output_file> <data type id>");
        General.showError( "DATA_TYPE_DISTANCE: " + Varia.DATA_TYPE_DISTANCE);
        General.showError( "DATA_TYPE_DIHEDRAL: " + Varia.DATA_TYPE_DIHEDRAL);
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

        // Number of items in saveframe
        int item_count_sf = 0;
        if (stack.size() != 0) {
            switch ( data_type ) {
                case Varia.DATA_TYPE_DISTANCE:
                    NmrStar.addConstraintDistance(  save_frame_node_constraint, star_version );
                    saveDataDistance(               save_frame_node_constraint, star_version );
                    item_count_sf += 4;
                    break;
                case Varia.DATA_TYPE_DIHEDRAL:
                    NmrStar.addConstraintDihedral(  save_frame_node_constraint, star_version );
                    saveDataDihedral(               save_frame_node_constraint, star_version  );
                    item_count_sf += 2;
                    break;
                case Varia.DATA_TYPE_DIPOLAR_COUPLING:
                    NmrStar.addConstraintDipolarCoupling(  save_frame_node_constraint, star_version );
                    saveDataDipolarCoupling(               save_frame_node_constraint, star_version  );
                    item_count_sf += 2;
                    break;
                default:
                    General.showError("none existing data type entered (code bug?):" + data_type);
            }
        }

        String tableNamePrefix = NmrStar.getMRDataType2TableNamePrefix( Varia.DATA_TYPE_NAMES[data_type]);
        // Add comments
        if (cmtStack.size() != 0) {
            NmrStar.addConstraintComments(      save_frame_node_constraint, star_version, tableNamePrefix);
            item_count_sf += 1;
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
                case NmrStar.STAR_VERSION_3_0: {
                    variable_names_comment[id++] = "commentId";
                    variable_names_comment[id++] = "beginLine";
                    variable_names_comment[id++] = "beginColumn";
                    variable_names_comment[id++] = "endLine";
                    variable_names_comment[id++] = "endColumn";
                    variable_names_comment[id++] = "comment";
                    break;
                }
                case NmrStar.STAR_VERSION_3_1: {
                    variable_names_comment[id++] = "commentId";
                    variable_names_comment[id++] = "beginLine";
                    variable_names_comment[id++] = "beginColumn";
                    variable_names_comment[id++] = "endLine";
                    variable_names_comment[id++] = "endColumn";
//                    variable_names_comment[id++] = "entryId";
//                    variable_names_comment[id++] = "listId";
                    variable_names_comment[id++] = "comment";
                    break;
                }
                default: {
                    General.showError("code bug Converters.XXX.saveData Unknown NMR-STAR format: " + star_version );
                }
            }

            LoopTableNode loop_table_node_comment = dataloopnode_comment.getVals();
            for (int i = 0; i < cmtStack.size(); i++) {
                Comment cmtTemp = (Comment) (cmtStack.elementAt(i));
                LoopRowNode looprownode_comment = new LoopRowNode();
                for (int j = 0; j < variable_names_comment.length; j++) {
                    String name = variable_names_comment[j];
                    String value = (String) cmtTemp.entry.getValue( name );
                    if ( value == null ) {
                        value = STAR_EMPTY;
                    }
                    looprownode_comment.addElement(new DataValueNode( value ));
                }
                loop_table_node_comment.addElement( looprownode_comment );
            }
        }

        // Add parse errors
        if (errStack.size() != 0) {
            NmrStar.addConstraintParseErrors(   save_frame_node_constraint, star_version, tableNamePrefix);
            item_count_sf += 1;
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
                case NmrStar.STAR_VERSION_3_0: {
                    variable_names_parse_error[id++] = "errorId";
                    variable_names_parse_error[id++] = "beginLine";
                    variable_names_parse_error[id++] = "beginColumn";
                    variable_names_parse_error[id++] = "endLine";
                    variable_names_parse_error[id++] = "endColumn";
                    variable_names_parse_error[id++] = "error";
                    break;
                }
                case NmrStar.STAR_VERSION_3_1: {
                    variable_names_parse_error[id++] = "errorId";
                    variable_names_parse_error[id++] = "beginLine";
                    variable_names_parse_error[id++] = "beginColumn";
                    variable_names_parse_error[id++] = "endLine";
                    variable_names_parse_error[id++] = "endColumn";
//                    variable_names_parse_error[id++] = "entryId";
//                    variable_names_parse_error[id++] = "listId";
                    variable_names_parse_error[id++] = "error";
                    break;
                }
                default: {
                    General.showError("code bug Converters.Discover.saveData Unknown NMR-STAR format: " + star_version );
                }
            }
            LoopTableNode loop_table_node_parse_error = dataloopnode_parse_error.getVals();
            for (int i = 0; i < errStack.size(); i++) {
                ParseError errTemp = (ParseError) (errStack.elementAt(i));
                LoopRowNode looprownode_parse_error = new LoopRowNode();
                for (int j = 0; j < variable_names_parse_error.length; j++) {
//                    String name = variable_names_parse_error[j];
                    String value = (String) errTemp.entry.getValue( variable_names_parse_error[j] );
                    if ( value == null ) {
                        value = STAR_EMPTY;
                    }
                    looprownode_parse_error.addElement(new DataValueNode( value ));
                }
                loop_table_node_parse_error.addElement( looprownode_parse_error );
            }
        }
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
            String angle_name = NmrStar.getPossibleValue( treeNode.entry.getValue( "angle_name" ) );
            looprownode_constraint.addElement(new DataValueNode( angle_name ));

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
//            if ( star_version < NmrStar.STAR_VERSION_3_1 ) {
//                looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
//                looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
//                looprownode_constraint.addElement(new DataValueNode( NmrStar.getPossibleValue( treeNode.entry.getValue( "force_constant_value" ))));
//                looprownode_constraint.addElement(new DataValueNode( NmrStar.getPossibleValue( treeNode.entry.getValue( "potential_function_exponent" ))));
//                loop_table_node_constraint.addElement( looprownode_constraint );
//            } else {
//                looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
//                looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
//            }
            loop_table_node_constraint.addElement( looprownode_constraint );

        }
    }

    /** Residual dipolar coupling section.
     * @param save_frame_node_constraint A reference to the saveframe node of the constraints to which the data should
     * be added.
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
                case NmrStar.STAR_VERSION_3_1: {
                    int values_per_atom = 7;
                    int empties = atom_count*values_per_atom;
                    for (int j = 0; j < empties; j++) {
                        looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    }
                    break;
                }
                default: {
                    General.showError("code bug saveDataDipolarCoupling. Unknown nmr-star format: " + star_version );
                    return;
                }
            }

            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    // Store info on both atoms
                    for (int j = 0; j < atom_count; j++) {
                        AtomNode atom = (AtomNode) ((ArrayList) treeNode.atoms.get(j)).get(0);
                        for (int k = 0; k < AtomNode.variable_names_atom.length; k++) {
                            String value = NmrStar.getPossibleValue( atom.info.getValue(AtomNode.variable_names_atom[k]) );
                            looprownode_constraint.addElement(new DataValueNode( value ));
                        }
                    }

                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "value" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "error" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: {
                    // Store info on both atoms
                    for (int j = 0; j < atom_count; j++) {
                        AtomNode atom = (AtomNode) ((ArrayList) treeNode.atoms.get(j)).get(0);
                        for (int k = 0; k < AtomNode.variable_names_atom.length; k++) {
                            String value = NmrStar.getPossibleValue( atom.info.getValue(AtomNode.variable_names_atom[k]) );
                            looprownode_constraint.addElement(new DataValueNode( value ));
                        }
                    }

                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "value" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "error" ) ));
                    break;
                }
                case NmrStar.STAR_VERSION_3_1: {
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "value" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "lower" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "upper" ) ));
                    looprownode_constraint.addElement(new DataValueNode( treeNode.entry.getValue( "error" ) ));
                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY )); // _RDC_constraint.Source_experiment_ID
                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY )); // resonances
                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    // Store info on both atoms
                    for (int j = 0; j < atom_count; j++) {
                        AtomNode atom = (AtomNode) ((ArrayList) treeNode.atoms.get(j)).get(0);
                        for (int k = 0; k < AtomNode.variable_names_atom.length; k++) {
                            String value = NmrStar.getPossibleValue( atom.info.getValue(AtomNode.variable_names_atom[k]) );
                            looprownode_constraint.addElement(new DataValueNode( value ));
                        }
                    }
//                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY )); // tailies
//                    looprownode_constraint.addElement(new DataValueNode( STAR_EMPTY ));
                    break;
                }
                default: {
                    General.showError("code bug saveDataDipolarCoupling. Unknown nmr-star format: " + star_version );
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


        String peak, volume, weight, ppm1, ppm2;
        int atom_count, atom_list_count;
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
                weight   = NmrStar.getPossibleValue( opt_info.getValue( "weight" ) );
                volume   = NmrStar.getPossibleValue( opt_info.getValue( "volume" ) );
                ppm1     = NmrStar.getPossibleValue( opt_info.getValue( "ppm1" ) );
                ppm2     = NmrStar.getPossibleValue( opt_info.getValue( "ppm2" ) );
            } else {
                peak     = STAR_EMPTY;
                weight   = STAR_EMPTY;
                volume   = STAR_EMPTY;
                ppm1     = STAR_EMPTY;
                ppm2     = STAR_EMPTY;
            }
            // Number of nodes is 1 for logic only node (1) and 1 for first
            // atom pair (2).
            // This is only valid if there are ARIA type of restraints.
//            number_nodes = treeNode.atomsOR.size() / 2 + 2;
            //General.showOutput("number_nodes: " + number_nodes);

                looprownode_logic       = new LoopRowNode();
                looprownode_logic.addElement(   new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                looprownode_logic.addElement(   new DataValueNode( treeNode.entry.getValue( "treeNodeId" )));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
//                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
//                looprownode_logic.addElement(   new DataValueNode( STAR_EMPTY ));
                loop_table_node_logic.addElement(       looprownode_logic );

            // ATOM LOOP
            atom_list_count = treeNode.atoms.size();
            // General.showOutput("Node 2 has number of atoms: " + atom_count);
            // Store info on all atoms from first pair (might be variable too)
            for (int j = 0; j < atom_list_count; j++) {
                //General.showOutput("i =" + i + " j =" + j );
                ArrayList atom_list = (ArrayList) treeNode.atoms.get(j);
                atom_count = atom_list.size();
                for (int k = 0; k < atom_count; k++) {
//                    General.showOutput("i =" + i + " j =" + j + " k =" + k);

                    LoopRowNode looprownode_atom        = new LoopRowNode();
                    AtomNode atom = (AtomNode) (atom_list.get(k));
                    String segi = NmrStar.getPossibleValue( atom.info.getValue( "segi" ) );
                    String resi = NmrStar.getPossibleValue( atom.info.getValue( "resi" ) );
                    String resn = NmrStar.getPossibleValue( atom.info.getValue( "resn" ) );
                    String name = NmrStar.getPossibleValue( atom.info.getValue( "name" ) );
                    switch ( star_version ) {
                        case NmrStar.STAR_VERSION_2_1_1: {
                            looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                            looprownode_atom.addElement(    new DataValueNode( "1" ));
                            looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                            looprownode_atom.addElement(    new DataValueNode( Integer.toString(j+1) ));
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
                            looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                            looprownode_atom.addElement(    new DataValueNode( "1" ));
                            looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                            looprownode_atom.addElement(    new DataValueNode( Integer.toString(j+1) ));
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
                            looprownode_atom.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                            looprownode_atom.addElement(    new DataValueNode( "1" ));
                            looprownode_atom.addElement(    new DataValueNode( Integer.toString(j+1) ));
                            for (int tt=0;tt<8;tt++) {
                                looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                            }
                            looprownode_atom.addElement(    new DataValueNode( segi ));
                            looprownode_atom.addElement(    new DataValueNode( resi ));
                            looprownode_atom.addElement(    new DataValueNode( resn ));
                            looprownode_atom.addElement(    new DataValueNode( name ));
//                            looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
//                            looprownode_atom.addElement(    new DataValueNode( STAR_EMPTY ));
                        break;
                        }
                        default: {
                            General.showError("code bug Converters.dyana.saveDataDistance. Unknown nmr-star format: " + star_version );
                        }
                    }
                    loop_table_node_atom.addElement(        looprownode_atom );
                }
            }
            // DISTANCE LOOP
            LoopRowNode looprownode_distance    = new LoopRowNode();
            switch ( star_version ) {
                case NmrStar.STAR_VERSION_2_1_1: {
                    looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                    looprownode_distance.addElement(    new DataValueNode( Integer.toString(1) ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( peak ));
                    looprownode_distance.addElement(    new DataValueNode( weight ));
                    looprownode_distance.addElement(    new DataValueNode( volume ));
                    looprownode_distance.addElement(    new DataValueNode( ppm1 ));
                    looprownode_distance.addElement(    new DataValueNode( ppm2 ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    // It would be more logical if this decision was elsewhere...
                    if ( lower_only ) {
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    } else {
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                    }
                    break;
                }
                case NmrStar.STAR_VERSION_3_0: {
                    looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                    looprownode_distance.addElement(    new DataValueNode( Integer.toString(1) ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( peak ));
                    looprownode_distance.addElement(    new DataValueNode( volume ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    // It would be more logical if this decision was elsewhere...
                    if ( lower_only ) {
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    } else {
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                    }
                    looprownode_distance.addElement(    new DataValueNode( weight ));
                    looprownode_distance.addElement(    new DataValueNode( ppm1 ));
                    looprownode_distance.addElement(    new DataValueNode( ppm2 ));
                    break;
                }
                case NmrStar.STAR_VERSION_3_1: {
                    // 10 columns
                    looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "treeId" ) ));
                    looprownode_distance.addElement(    new DataValueNode( Integer.toString(1) ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( peak ));
                    looprownode_distance.addElement(    new DataValueNode( volume ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    // It would be more logical if this decision was elsewhere...
                    if ( lower_only ) {
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    } else {
                        looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                        looprownode_distance.addElement(    new DataValueNode( treeNode.entry.getValue( "upper" )  ));
                    }
//                    looprownode_distance.addElement(    new DataValueNode( weight ));
//                    looprownode_distance.addElement(    new DataValueNode( ppm1 ));
//                    looprownode_distance.addElement(    new DataValueNode( ppm2 ));
//                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
//                    looprownode_distance.addElement(    new DataValueNode( STAR_EMPTY ));
                    break;
                }
                default: {
                    General.showError("code bug Converters.. Unknown nmr-star format: " + star_version );
                }
            }

            loop_table_node_distance.addElement(    looprownode_distance );
        }
    }


    //main
    /** The main to call for the conversion giving input and output files on the command
     * line.
     * @param args First argument : input file (dyana)
     * Second argument: output file (STAR)
     * Third argument : data type (e.g. 1 for distances)
     *
     * <PRE>Usage:java Wattos.Converters.Dyana.StarOutAll <input_file> <output_file> <[1-3]></PRE>
     */
    public static void main(String[] args) {

        // Some debug info
        if ( DEBUG )
            General.showEnvironment();

        //check for arguments, require one Dyana template file (.config file) followed by
        //the Dyana file block to be processed
        if (args.length != 4) {
            showUsage();
        }

        init();
        inputFile  = args[0];
        outFile    = args[1];
        data_type  = (new Integer( args[2])).intValue();
        int star_version = (new Integer( args[3])).intValue();

        // Testing
//        if ( false ) {
//            inputFile  = "test.txt";
//            outFile    = "test.str";
//            data_type  = Varia.DATA_TYPE_DISTANCE;
//        }
        convertStar( null, data_type, false, star_version );
    }
}
