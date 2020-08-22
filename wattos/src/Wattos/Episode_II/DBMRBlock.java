/*
 * Episode_II.java
 *
 * Created on December 4, 2001, 11:46 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 *
 */

package Wattos.Episode_II;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

import EDU.bmrb.starlibj.BlockNode;
import EDU.bmrb.starlibj.DataLoopNameListNode;
import EDU.bmrb.starlibj.DataLoopNode;
import EDU.bmrb.starlibj.DataValueNode;
import EDU.bmrb.starlibj.LoopTableNode;
import EDU.bmrb.starlibj.RemoteInt;
import EDU.bmrb.starlibj.SaveFrameNode;
import EDU.bmrb.starlibj.StarFileNode;
import EDU.bmrb.starlibj.StarNode;
import EDU.bmrb.starlibj.StarValidity;
import EDU.bmrb.starlibj.VectorCheckType;
import Wattos.Converters.Amber.StarOutAll;
import Wattos.Converters.Common.Varia;
import Wattos.Database.Defs;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.NmrStar;
import Wattos.Utils.PrimitiveArray;
import Wattos.Utils.Strings;

import com.braju.format.Format;
import com.braju.format.Parameters;

/**
 * One block of lines as separated by annotation.
 * A small class just holding the content of one block in the DBMRFile.
 * The block data itself can now also contain non-text data in which case
 * the line count will be null.
 *
 * The block files are stored in a filesystem now whereas before they went into
 *CLOBs in the database. This allows me to be completely dbms implementation
 *independent.
 *
 * @author  Jurgen F. Doreleijers
 * @version 1.0
 */
public class DBMRBlock {
    public static final String[] level = { "PROGRAM", "TYPE", "SUBTYPE", "FORMAT"};
    public static final int PROGRAM_ID   = 0;
    public static final int TYPE_ID      = 1;
    public static final int SUBTYPE_ID   = 2;
    public static final int FORMAT_ID    = 3;

    public static final Properties other_prop_anno_allowed_types;
    public static final String other_prop_anno_id = "OTHER_PROP";
    public static final String OTHER_PROP_LOWER_ONLY = "LOWER_ONLY";
    public static final String[] FULL_ENTRY_TYPES = new String[] {"n/a", "entry", "full", "n/a"};


    static {
        other_prop_anno_allowed_types = new Properties();

        other_prop_anno_allowed_types.setProperty(OTHER_PROP_LOWER_ONLY,   "boolean");
        /** Examples of other types. */
        other_prop_anno_allowed_types.setProperty("BOGUS_S",      "string");
        other_prop_anno_allowed_types.setProperty("BOGUS_I",      "int");
        other_prop_anno_allowed_types.setProperty("BOGUS_D",      "double");
        other_prop_anno_allowed_types.setProperty("BOGUS_C",      "char");
    }

    /** The id in the db */
    public int mrblock_id;
    /** The id in the db */
    public int mrfile_id;
    /** The position in the file */
    public int position;

    /** The type of block as specified at four levels:
     *PROGRAM
     *TYPE
     *SUBTYPE
     *FORMAT
     *OTHER_PROP
     */
    public String[] type = {"n/a","n/a","n/a","n/a"};

    /** Intended to include a slew of additional info that varies from
     *type to type. E.g. for DYANA distance restraints, it needs to be noted
     *whether it's upper or lower bound being defined.
     */
    public Properties other_prop;

    /** Indicates the state of the text it is currently in. E.g.:
     *"Original", "First pass", "NMR-STAR version 3.0", etc.
     *It defaults to "test".*/
    public String text_type;
    /** Any modification noted or assumed */
    public Date date_modified;

    /** The data making up a block.
     * Use the method isText to figure out what the data actually is.
     *Use the method getLines to get: ArrayList lines from content.
     *They will be materialized but not kept. Content is the master.
     */
    public byte[] content;

    /** The name of the file without the extension and path */
    public int dbfs_id;
    /** The name of the file; excluding path but INCLUDING extension.
     * This will be the original filename in the beginning or e.g. 12345.str
     * where 12345 is the block id.
     * The extension is important to get right. Otherwise the released files
     * will get a bad extensions from the NMR Restraints Grid.
     * */
    public String fileName;
    /** Number of restraints or other items in block.*/
    public int item_count ;

    public String md5Sum;

    public void init( ) {
        mrblock_id          = Wattos.Utils.General.NULL_FOR_INTS;
        mrfile_id           = Wattos.Utils.General.NULL_FOR_INTS;
        position            = Wattos.Utils.General.NULL_FOR_INTS;
//        String[] type       = {"n/a","n/a","n/a","n/a"};
        other_prop          = new Properties();
        text_type           = "default";
        date_modified       = new Date();
        content             = null;             // should be created when put in or retrieved from db.
        dbfs_id             = Defs.NULL_INT;    // should be changed when put in or retrieved from db.
        fileName            = "text.txt";       // should be changed at creation of class instance.
        item_count          = Defs.NULL_INT;    // should be changed at creation of class instance.
        md5Sum              = null;             // should be changed when put in or retrieved from db.
    }

    /** Set type and the text lines of the block.
     * @param type The block type.
     */
    public DBMRBlock(String[] type, Properties other_prop,
        byte[] content, int dbfs_id, String fileName, String md5Sum ) {
        init();
        this.type                   = type;
        this.other_prop             = other_prop;
        this.content                = content;
        this.dbfs_id                = dbfs_id;
        this.fileName               = fileName;
        this.md5Sum                 = md5Sum;
    }


    /** Simple initialization*/
    public DBMRBlock() {
        init();
    }

    /** convert from the original data to text lines.
     */
    public ArrayList getLines() {
	if ( ! isTextBlock() ) {
            General.showError("Can't materialize the content of dbmrblock as text because it isn't");
            return null;
        }
        return PrimitiveArray.getLines( content );
    }

    /** Content of the file should be considered as text.
     */
    public boolean isTextBlock() {
        String extension = InOut.getFilenameExtension(fileName);
        return InOut.fileNameExtensionCanBeText( extension );
    }

    /** Textual representation of the block type.
     * @return The concatenation of the block type identifiers (format, subtype, type, and
     * program).
     */
    public String getBlockType()
    {
        return Strings.concatenate(type,", ");
    }


    /** Textual representation of the annotation. Uses sprintf java equivalent
     * from com.braju.Format.
     * @return The four lines which show the annotation as an annotator might have put in.
     */
    public String getAnnotationString()
    {
        String line;
        StringBuffer annotation = new StringBuffer();
        Parameters p = new Parameters(); // Printf parameters
        for (int i=0;i<level.length;i++) {
            line = Format.sprintf("%s%-15s%s\n",
                p.add( DBMRFile.PREFIX ).add( level[i] ).add(type[i]));
            annotation.append(line);
        }
        for (Enumeration e = other_prop.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            String value = other_prop.getProperty(key);
            line = Format.sprintf("%s%-15s%-15s = %s\n",
                p.add( DBMRFile.PREFIX ).add( other_prop_anno_id ).add(key).add(value));
            annotation.append(line);
        }
        return annotation.toString();
    }


    /** Textual representation.
     * @return The concatenation of the lines
     */
    public String toString() {
        if ( ! isTextBlock() ) {
            return "DBMRBlock data can't currently be represented as text.";
        }
        if ( isEmpty() ) {
            return "DBMRBlock currently empty.";
        }

        // Use a buffer for efficiency, the string can be ~1Mb.
        StringBuffer sb = new StringBuffer();

        ArrayList lines = getLines();
        if ( lines != null ) {
            for (Iterator i=lines.iterator();i.hasNext();)
                sb.append(i.next()+General.eol);
        } else {
            String extension = InOut.getFilenameExtension(fileName);
            sb.append("No textual representation possible for data with file extension: " + extension );
        }
        return(sb.toString());
    }

    /** From textual representation of single String to multiple lines (Strings).
     */
    public void setStrings( String txt ) {
        ArrayList lines = Strings.getLines( txt );
        content = PrimitiveArray.fillContent(lines);
        if ( content == null ) {
            General.showError("Failed to transfer strings to content -0-");
        }
    }

    /** Textual representation of type.
     * @return The concatenation of the four types
     */
    public String getType()
    {
        // Use a buffer for efficiency, the string can be ~1Mb.
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<type.length;i++) {
            sb.append(type[i]+",");
        }
        // Remove the last comma in the string buffer
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }


    /** Traditional setter.
     */
    public void setType( String[] new_type)
    {
        date_modified = new Date();
        for (int i=0; i<type.length; i++) {
            type[i] = new_type[i];
        }
    }

    /** Traditional setter ignoring null values in new type if present.
     */
    public void setTypeWithoutNulls( String[] new_type)
    {
        date_modified = new Date();
        for (int i=0; i<4; i++) {
            if ( new_type[i] != null ) {
                //General.showOutput(i+" is reset");
                type[i] = new_type[i];
            }
        }
    }

    /** Removes white space from both ends of all type levels.
     * @return <CODE>true</CODE> always for now.
     */
    public boolean trimTypeValues() {
        for (int i=0;i<type.length;i++) {
            type[i].trim();
        }
        return true;
    }


    /** Checks whether this mrb has the same type as given. Special case of a
     *null for a string matches every type, including null.
     */
    public boolean hasBlockType(String[] type_cmp) {
        if ( type_cmp.length != type.length ) {
            General.showError("in DBMRBlock.hasBlockType found:");
            General.showError("type input does not have of the correct size (4):"+
                type.length );
            return false;
        }
        if ( type.length != type.length ) {
            General.showError("in DBMRBlock.hasBlockType found:");
            General.showError("type self does not have of the correct size (4):"+
                type.length );
            return false;
        }

        for (int i=0;i<type.length;i++) {
            if ( type_cmp[i] ==  null || type[i] == null ) {
                // This is allowed and considered a match.
                continue;
            }
            if ( ! type_cmp[i].equals( type[i] ) ) {
                return false;
            }
        }
        return true;
    }

    /** Checks whether the other mrb has the same type.
     * @param mrb The other DBMRBlock object to compare with.
     * @return <CODE>true</CODE> if the block is the same.
     */
    public boolean hasEqualType(DBMRBlock mrb) {
        // Use this algorithm to speed things up in case of frequent mismatches
        //General.showOutput("getAnnotationString: " + mrb.getAnnotationString());
        if ( mrb.type.length != type.length ) {
            General.showError("in DBMRBlock.hasEqualType found:");
            General.showError("mrb.type does not have of the correct size (" + type.length + "):"+
                type.length );
            return false;
        }

        for (int i=0;i<type.length;i++) {
            if ( type[i] ==  null ) {
                General.showError("in DBMRBlock.hasEqualType found:");
                General.showError("type["+i+"] is null.");
                return false;
            }
            if ( mrb.type[i] == null ) {
                General.showError("in DBMRBlock.hasEqualType found:");
                General.showError("mrb.type["+i+"] is null.");
                return false;
            }
            if ( ! type[i].equals( mrb.type[i] ) ) {
                return false;
            }
        }
        return true;
    }


    /** Checks whether the other mrb has the same type
     * without comparing the program part.
     * @param mrb The other DBMRBlock object to compare with.
     * @return <CODE>true</CODE> if the block has the same types.
     */
    public boolean hasEqualTypeButProgram(DBMRBlock mrb) {
        // Use this algorithm to speed things up in case of frequent mismatches
        //General.showOutput("getAnnotationString: " + mrb.getAnnotationString());
        if ( mrb.type.length != type.length ) {
            General.showError("in DBMRBlock.hasEqualTypeButProgram found:");
            General.showError("mrb.type does not have of the correct size (" + type.length + "):"+
                mrb.type.length );
            return false;
        }

        // Now compare only last three types in array.
        for (int i=1;i<type.length;i++) {
            if ( type[i] ==  null ) {
                General.showError("in DBMRBlock.hasEqualTypeButProgram found:");
                General.showError("type["+i+"] is null.");
                return false;
            }
            if ( mrb.type[i] == null ) {
                General.showError("in DBMRBlock.hasEqualTypeButProgram found:");
                General.showError("mrb.type["+i+"] is null.");
                return false;
            }
            if ( ! type[i].equals( mrb.type[i] ) ) {
                return false;
            }
        }
        return true;
    }

    /** Returns true if the block type is any of the known ones.
     * @param classification The allowed types of blocks.
     * @return <CODE>true</CODE> if the block type matches any in the classification list.
     */
    public boolean hasValidBlockType(Classification classification) {
        for (Iterator i=classification.mrb_list.iterator();i.hasNext();)
        {
            DBMRBlock mrb = (DBMRBlock) i.next();
            /**
            General.showOutput("Comparing: " + this.getType());
            General.showOutput("To       : " + mrb.getType());
             */

            if ( hasEqualType( mrb )) {
                return true;
            }
        }
        return false;
    }

    /** Returns true if the block's other props are all of allowed status.
     * @return <CODE>true</CODE> if the block props matches all as defined.
     */
    public boolean hasValidBlockOtherProp() {

        if ( other_prop == null ) {
            return true;
        }
        String other_prop_str = Strings.getProperties( other_prop );
        if ( other_prop_str.length() > SQL_Generic.MAX_CHARS_VARCHAR2 ) {
            General.showError("(hasValidBlockOtherProp) too many chars for a varchar2");
            return false;
        }

        for (Enumeration e = other_prop.propertyNames(); e.hasMoreElements();) {
            String key      = (String) e.nextElement();
            String value    = (String) other_prop.getProperty(key);

            boolean element_allowed = true;

            if ( other_prop_anno_allowed_types.containsKey( key ) ) {

                String allowed_type = other_prop_anno_allowed_types.getProperty(key);

                /** BOOLEAN checks here */
                if ( allowed_type.equals("boolean" ) ) {
                    if  ( ! ( value.equals(Defs.STRING_TRUE) | value.equals(Defs.STRING_FALSE) ) ) {
                        element_allowed = false;
                        General.showError("key was allowed but allowed type didn't match observed value.");
                        General.showError("value was not true or false but: [" + value + "]");
                    }
                /** CHAR checks here */
                } else if ( allowed_type.equals("char" ) ) {
                    if  ( value.length() != 1 ) {
                        element_allowed = false;
                        General.showError("key was allowed but allowed type didn't match observed value.");
                        General.showError("value length was not 1 as expected but: " + value.length());
                    }
                }
            } else {
                element_allowed = false;
            }

            if ( ! element_allowed ) {
                General.showError("element was not allowed");
                return false;
            }
        }
        return true; // no keys or all keys allowed.
    }


    /**
     * Converts this block into STAR if a converter is available.
     * If not available or an error occurs, a null will be returned.
     * @param scheme
     * @param pos
     * @param star_version
     * @param pdb_id Only used for Amber conversions
     */
    public SaveFrameNode convert(Classification scheme, int pos, int star_version, String pdb_id  ) {

        int program_id = getConverterProgramId(scheme);

        SaveFrameNode save_frame_node  = null;

        /** Catch the exceptions and errors that may arise from the parsers. The only
         *infrequently expected error is the TokenManagerErrors but all should be
         *caught to ensure proper handling.
         */
        try {
            //General.showOutput("program_id is : " + program_id);
            switch ( program_id ) {
                case Classification.CONVERSION_CNS_DISTANCE_NOE_NA:
                    save_frame_node = convertCNSDistanceNOE( pos, star_version );
                    break;
                case Classification.CONVERSION_CNS_DIHEDRAL_NA_NA:
                    save_frame_node = convertCNSDihedralNaNa( pos, star_version );
                    break;
                case Classification.CONVERSION_CNS_DIPOLAR_NA_NA:
                    save_frame_node = convertCNSDipolarNaNa( pos, star_version  );
                    break;
                case Classification.CONVERSION_DYANA_DISTANCE_NOE_NA:
                    save_frame_node = convertDYANADistanceNOE( pos, star_version  );
                    break;
                case Classification.CONVERSION_DYANA_DIHEDRAL_NOE_NA:
                    save_frame_node = convertDYANADihedralNaNa( pos, star_version );
                    break;
                case Classification.CONVERSION_DYANA_DIPOLAR_NOE_NA:
                    save_frame_node = convertDYANADipolarNaNa( pos, star_version );
                    break;
                case Classification.CONVERSION_DISCOVER_DISTANCE_NOE_NA:
                    save_frame_node = convertDiscoverDistanceNOE( pos, star_version  );
                    break;
                case Classification.CONVERSION_DISCOVER_DIHEDRAL_NOE_NA:
                    save_frame_node = convertDiscoverDihedralNaNa( pos, star_version  );
                    break;
                case Classification.CONVERSION_EMBOSS_DISTANCE_NOE_NA:
                    save_frame_node = convertEmbossDistanceNaNa( pos, star_version );
                    break;
                case Classification.CONVERSION_EMBOSS_DIHEDRAL_NOE_NA:
                    save_frame_node = convertEmbossDihedralNaNa( pos, star_version  );
                    break;
                    /** comment out the next block when no parsing of amber files should be
                     *done*/
                case Classification.CONVERSION_AMBER_DISTANCE_NOE_NA:
                    if ( ! amberPDBFilePresent(pdb_id)) {
                        return null;
                    }
                    save_frame_node = convertAMBERDistanceNOE( pos, star_version, pdb_id );
                    break;
                case Classification.CONVERSION_AMBER_DIHEDRAL_NOE_NA:
                    if ( ! amberPDBFilePresent(pdb_id)) {
                        return null;
                    }
                    save_frame_node = convertAMBERDihedralNaNa( pos, star_version, pdb_id );
                    break;
                case Classification.CONVERSION_AMBER_DIPOLAR_NOE_NA:
                    if ( ! amberPDBFilePresent(pdb_id)) {
                        return null;
                    }
                    save_frame_node = convertAMBERDipolarNaNa( pos, star_version, pdb_id );
                    break;

                case Classification.CONVERSION_ANY_COMMENT_NA_NA:
                    save_frame_node = convertAnyCommentNaNa( pos, star_version );
                    break;
                case Classification.NO_CONVERSION_PROGRAM_AVAILABLE_ID:
                    return null;
                default:
                    General.showError("code bug: wrong program_id (or amber file): " + program_id);
                    return null;
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            save_frame_node  = null;
        }
        item_count = getItemCount(save_frame_node);

        return save_frame_node;
    }


    private boolean amberPDBFilePresent(String pdb_id) {
        String amberUrl = StarOutAll.getAmberUrl(pdb_id);
        if ( amberUrl == null ) {
            General.showError("Failed to create amberUrl");
            return false;
        }
        File f = new File(amberUrl);
        if ( f == null  ) {
            General.showError("Failed to create amberUrl file");
            return false;
        }
        return f.exists();
    }

    /** Converts this block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertAnyCommentNaNa( int pos, int star_version  ) {

        SaveFrameNode save_frame_node_comments = new SaveFrameNode("save_MR_file_comment_" + (pos+1));
        String text =  toString();
        if ( text.length() > 300000 ) { //
            General.showError("Found a comment block larger than 10^5 characters, it's of size: " + text.length());
            General.showError("This will likely lead to an error as JavaCC parser is not set up to read it.");
            General.showError("The largest known comment block is 162,000 characters in entry 1zaq");
            return null;
        }
        String dummy_string_tableNamePrefix = null;
        NmrStar.addComment(save_frame_node_comments, star_version, dummy_string_tableNamePrefix, text);

        return save_frame_node_comments;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertCNSDistanceNOE( int pos, int star_version) {


        // Initialize the class
        Wattos.Converters.Xplor.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Xplor.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DISTANCE, star_version );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_CNS/XPLOR_distance_constraints_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertCNSDihedralNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Xplor.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Xplor.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIHEDRAL, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_CNS/XPLOR_dihedral_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertCNSDipolarNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Xplor.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Xplor.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIPOLAR_COUPLING, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_CNS/XPLOR_dipolar_coupling_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertDYANADistanceNOE( int pos, int star_version  ) {


         /** Should the distances be interpreted as lower bounds?
          */
         boolean lower_only = false;
         if ( other_prop.containsKey("LOWER_ONLY") ) {
             String value = other_prop.getProperty( "LOWER_ONLY" );
             if ( value.equals(Defs.STRING_TRUE) ) {
                 lower_only = true;
             }
         }

        // Initialize the class
        Wattos.Converters.Dyana.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Dyana.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DISTANCE, lower_only, star_version );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_DYANA/DIANA_distance_constraints_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertDYANADihedralNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Dyana.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Dyana.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIHEDRAL, false, star_version );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_DYANA/DIANA_dihedral_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertDiscoverDistanceNOE( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Discover.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Discover.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DISTANCE, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_Discover_distance_constraints_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertDiscoverDihedralNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Discover.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Discover.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIHEDRAL, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_Discover_dihedral_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertEmbossDistanceNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Emboss.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Emboss.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DISTANCE, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_Emboss_distance_constraints_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertEmbossDihedralNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Emboss.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Emboss.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIHEDRAL, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_Emboss_dihedral_" + (pos+1));

        return save_frame_node_constraints;
     }

    /** Converts this type of block to STAR.
     *If not available or an error occurs, a null will be returned.
     */
     public SaveFrameNode convertDYANADipolarNaNa( int pos, int star_version  ) {


        // Initialize the class
        Wattos.Converters.Dyana.StarOutAll.init();
        StarFileNode star_file_node_constraints         = Wattos.Converters.Dyana.StarOutAll.convertToStarFileNode( toString(),
            Varia.DATA_TYPE_DIPOLAR_COUPLING, false, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_DYANA/DIANA_dipolar_coupling_" + (pos+1));

        return save_frame_node_constraints;
     }

     /** Returns program id or -1 to signal none is available.
     */
    public int getConverterProgramId(Classification classification) {


        int index = 0;

        for (Iterator i=classification.mrb_list_converters.iterator();i.hasNext();)
        {
            DBMRBlock mrb = (DBMRBlock) i.next();
            if ( hasEqualType( mrb )) {
                int program_id = ((Integer) classification.mrb_list_converters_map.get(index)).intValue();
                return program_id;
            }
            index++;
        }
        return -1;
    }

    /** Put text from lines into content */
    public boolean fillContent( ArrayList lines ) {
        content = PrimitiveArray.fillContent(lines);
        if ( content == null ) {
            General.showError("Failed to transfer strings to content -1-");
            return false;
        }
        return true;
    }
    /** Checks if the block contains only white space characters.
     * @return <CODE>false</CODE> if the block has at least one line that contains more
     * than just white space or the content as bytes isn't the null ref.
     */
    public boolean isEmpty() {

        //General.showOutput("Checking emptyness of dbmrblock");

        if ( (content == null) || (content.length == 0) ) {
            General.showOutput("dbmrblock content is null or has zero length");
            return true;
        }

        if ( isTextBlock() ) {
            ArrayList lines = getLines();
            if ( lines == null ) {
                General.showError("Failed to get text even though the block is marked by fileExtension as text");
                return true;
            }
            Iterator i=lines.iterator();
            //General.showOutput("Check: isEmpty");
            while (i.hasNext()) {
                String line = (String) i.next();
                if ( ! line.trim().equals("") ) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }



    /** Looks at one line of annotation and sets the type level as specified in the
     * annotation.
     * @param line The text line with the annotation.
     * @return <CODE>true</CODE> if the line contains a valid annotation or if
     *the type level is other prop then it will return true even if the annotation
     *was not used. A warning will be issued about it.
     */
    public boolean setSpecifier( String line ) {

        int level_id = -1;
        String key, value;
        int beginIndex = DBMRFile.PREFIX.length();
        String info = line.substring(beginIndex).trim();
        StringTokenizer st = new StringTokenizer(info);
        String level_string = st.nextToken();
        // Abbreviate the logic here....
        if      ( level_string.equals( level[0] ) )
            level_id = 0;
        else if ( level_string.equals( level[1]) )
            level_id = 1;
        else if ( level_string.equals( level[2]) )
            level_id = 2;
        else if ( level_string.equals( level[3]) )
            level_id = 3;
        else if ( level_string.equals( other_prop_anno_id ) )
            ;
        else {
            General.showError("in DBMRBlock.setSpecifier found:");
            General.showError("got unknown specifier: "+level_string);
            return false;
        }

        /** It was a regular level thingie */
        if ( level_id != -1 ) {
            String info_string = st.nextToken();
            while (st.hasMoreTokens()) {
                info_string = info_string + " " + st.nextToken();
            }
            // The leading space resulting from the lousy algorithm above needs trimming
            info_string.trim();
            type[level_id] = info_string;
            return true;
        }

        /** Some other prop was defined */
        /** Strip the level_string and look at the assignment string only */
        String assignment = info.substring(level_string.length()).trim();
        //General.showDebug("assignment is: [" + assignment + "]");
        st = new StringTokenizer(assignment, "=");

        if ( st.hasMoreTokens() ) {
            key = st.nextToken();
        } else {
            General.showWarning("assignment has no key that could be interpreted: [" + assignment + "]");
            General.showWarning("assignment ignored");
            return true;
        }

        if ( st.hasMoreTokens() ) {
            value = st.nextToken();
        } else {
            General.showWarning("assignment has no value that could be interpreted: [" + assignment + "]");
            General.showWarning("assignment ignored");
            return true;
        }
        if ( ! setOtherProp( key, value )) {
            General.showError("Failed to set valid key,value pair for other_prop and value: " + value + " and key: " + key);
            return false;
        }
        //General.showDebug("other_prop now: [" + other_prop.toString() + "]");
        return true;
    }

    /** Sets the key, value pair and checks if it's valid. Returns validity
     */
    public boolean setOtherProp( String key, String value ) {
        if ( other_prop == null ) {
            other_prop = new Properties();
        }
        other_prop.put(key.trim(), value.trim());
        if ( ! hasValidBlockOtherProp() ) {
            General.showError("other_prop set to invalid item; up to caller of routine: mrb.setOtherProp to fix this.");
            return false;
        }
        return true;
    }

    /**
     * Converts this type of block to STAR.
     * If not available or an error occurs, a null will be returned.
     */
    public SaveFrameNode convertAMBERDihedralNaNa(int pos, int star_version, String pdb_id) {


        // Initialize the class
        Wattos.Converters.Amber.StarOutAll.init(pdb_id);
        StarFileNode star_file_node_constraints         = Wattos.Converters.Amber.StarOutAll.convertToStarFileNode( toString(),
        Varia.DATA_TYPE_DIHEDRAL, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_AMBER_dihedral_" + (pos+1));

        return save_frame_node_constraints;
    }

    /**
     * Converts this type of block to STAR.
     * If not available or an error occurs, a null will be returned.
     */
    public SaveFrameNode convertAMBERDipolarNaNa(int pos, int star_version, String pdb_id) {
        // Initialize the class
        Wattos.Converters.Amber.StarOutAll.init(pdb_id);
        StarFileNode star_file_node_constraints         = Wattos.Converters.Amber.StarOutAll.convertToStarFileNode( toString(),
        Varia.DATA_TYPE_DIPOLAR_COUPLING, star_version   );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_AMBER_dipolar_coupling_" + (pos+1));

        return save_frame_node_constraints;
    }

    /**
     * Converts this type of block to STAR.
     * If not available or an error occurs, a null will be returned.
     */
    public SaveFrameNode convertAMBERDistanceNOE(int pos, int star_version, String pdb_id ) {

        // Initialize the class
        Wattos.Converters.Amber.StarOutAll.init(pdb_id);
        StarFileNode star_file_node_constraints         = Wattos.Converters.Amber.StarOutAll.convertToStarFileNode( toString(),
        Varia.DATA_TYPE_DISTANCE, star_version );
        BlockNode block_node_constraints                = star_file_node_constraints.firstElement();
        SaveFrameNode save_frame_node_constraints       = (SaveFrameNode) block_node_constraints.firstElement();
        save_frame_node_constraints.setLabel( "save_AMBER_distance_constraints_" + (pos+1));

        return save_frame_node_constraints;
    }
    /** Get the number of restraints or other items.
         *the only countable items are:
         *distances,
         *dipolar couplings,
         *dihedral angles.
         *Returns zero in case no items classified as restraints.
         */
        public int getItemCount(StarNode star_node) {
            int sum = 0;

            try {
                String[] checkTagsCount = NmrStar.checkTagsCount;
                for (int i=0;i<checkTagsCount.length;i++) {
    //                General.showDebug("Looking for tag: " + checkTags[i]);
                    VectorCheckType vct = star_node.searchForTypeByName(
                        Class.forName(StarValidity.clsNameDataLoopNode),
                        checkTagsCount[i]);
                    for (int j=0;j<vct.size();j++) { // for each DataLoopNode
    //                    General.showDebug("Looking at table (numbered in order): " + j);
                        DataLoopNode dln = (DataLoopNode) vct.elementAt(j);
                        RemoteInt column = new RemoteInt();
                        DataLoopNameListNode names = dln.getNames();
                        names.tagPositionDeep(checkTagsCount[i], new RemoteInt(), column);
                        int columnId = column.num;
                        if ( columnId < 0 ) {
                            General.showError("Failed to get column for tag name: " +  checkTagsCount[i]);
                            return Defs.NULL_INT;
                        }
                        LoopTableNode ltn = dln.getVals();
                        int lastRow = ltn.size()-1;
                        if ( lastRow < 0 ) {
                            General.showError("Failed to get row for tag name: " +  checkTagsCount[i]);
                            return Defs.NULL_INT;
                        }
    //                    General.showDebug("lastRow : " + lastRow);
    //                    General.showDebug("columnId: " + columnId);
                        DataValueNode dvn = ltn.elementAt(lastRow).elementAt(columnId);
                        String value = dvn.getValue();
                        int count = Integer.parseInt(value);
    //                    General.showDebug("Adding number of items: " + count);
                        sum += count;
                     }
                }
            } catch ( Throwable t ) {
                General.showThrowable(t);
                return Defs.NULL_INT;
            }
            //General.showDebug("Sum getItemCount is: " + sum);
            return sum;
        }

    /** Self test; doesn't do anything.
     * @param args Ignored.
     */
    public static void main (String[] args)
    {
       General.verbosity = General.verbosityDebug;
       General.showOutput("Starting test of check routine." );
        if ( true ) {
            String[] s1 = {"a",".","c","x"};
            String[] s2 = {"a","b","c","x"};
            Properties p = new Properties();

            DBMRBlock mrb1 = new DBMRBlock( s1, p, null, Defs.NULL_INT, "txt", null);
            DBMRBlock mrb2 = new DBMRBlock( s2, p, null, Defs.NULL_INT, "txt", null);
            General.showOutput("mrb 1 looks like this:["+mrb1.getType()+"]");
            General.showOutput("mrb 2 looks like this:["+mrb2.getType()+"]");
            General.showOutput("mrb 1 has same type but program as 2: " +
                mrb1.hasEqualTypeButProgram(mrb2) );
            General.showOutput("mrb 1 has same type as 2: " +
                mrb1.hasBlockType(s2) );
            mrb1.setTypeWithoutNulls(s2);
            General.showOutput("After reassignment mrb 1 looks like this:["+mrb1.getType()+"]");
        }

//        if ( false ) {
//            String[] s1 = {"a","b","c","d"};
//            Properties p = new Properties();
//            p.setProperty("test", "nothing");
//            DBMRBlock mrb1 = new DBMRBlock( s1, p, null, Defs.NULL_INT, "txt", null);
//            String t = "X\nY\n\n";
//            General.showOutput("mrb looks like this:["+ mrb1 +"]");
//            mrb1.setStrings( t );
//            General.showOutput("mrb looks like this:["+ mrb1 +"]");
//            General.showOutput("mrb props look like this:["+ mrb1.other_prop +"]");
//        }
        General.showOutput("Finished all selected check routines." );
    }

}
