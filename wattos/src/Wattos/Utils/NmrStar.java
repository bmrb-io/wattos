/* 
 * NmrStar.java
 *
 * Created on August 2, 2002, 11:46 AM
 */

package Wattos.Utils;
 
import EDU.bmrb.starlibj.*;

import java.io.*;
import java.util.*;

/** Code for generating NMR-STAR using the EDU.bmrb.starlibj API
 *mostly used for the NMR Restraint Grid.
 * List of tag names in common NMRSTAR files as used in wattos.
 * All tags used should be defined here so there's one easy location to change
 *them if needed.
 *And actually, this code ought to be merged with the completely dictionary
 *based code in Wattos.Star.NMRStar.File31
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class NmrStar {
 
    //Default value used in Star files for an empty value
    /** Default value for whenever data is missing.
     */    
    public static final String STAR_EMPTY = ".";
    public static final String STAR_QUESTION = "?";
    
    /** ints for easy switch on versioning */
    public static final int STAR_VERSION_INVALID    = -1;
    public static final int STAR_VERSION_2_1_1      = 0;
    public static final int STAR_VERSION_3_0        = 1;
    public static final int STAR_VERSION_3_1        = 2;
    public static final int STAR_VERSION_DEFAULT    = STAR_VERSION_3_1; 

    public static final String[] STAR_VERSION_NAMES = { 
        "STAR_VERSION_2_1_1", 
        "STAR_VERSION_3_0",
        "STAR_VERSION_3_1",
        };
    public static Class dataLoopNodeClass = null;

    /** Definitions for saveframe categories */
    public static HashMap mRDataType2SfCategory = null;
    
    public static HashMap mRDataType2SfCategoryInvert = null;
    public static HashMap mRDataType2TableNamePrefixInvert = null;
    /** Definitions for table names; not used in star version 2.1.1 and below*/
    public static HashMap mRDataType2TableName  = null;
    /** Definitions for prefix of tag names; not used in star version 3.0 and below*/
    public static HashMap mRDataType2TagNamePrefix  = null;
    
    // The prefix of the table name containing comments and the like.
    public static HashMap mRDataType2TableNamePrefix  = null;

    /** Used in multiple getItemCount routines */
    public static String[] checkTagsCount = new String[] {
            "_Torsion_angle_constraint.ID",             
            "_RDC_constraint.ID",
            "_Dist_constraint.Tree_node_member_constraint_ID"
        };
    
    static {
        try {
            dataLoopNodeClass = Class.forName("EDU.bmrb.starlibj.DataLoopNode");
            
            mRDataType2SfCategory = new HashMap();
            mRDataType2SfCategory.put("distance",           "distance_constraints"); 
            mRDataType2SfCategory.put("dihedral angle",     "torsion_angle_constraints"); 
            mRDataType2SfCategory.put("dipolar coupling",   "RDC_constraints"); 
            mRDataType2SfCategory.put("comment",            "org_constr_file_comment"); 
            mRDataType2TableName = new HashMap();
            mRDataType2TableName.put("distance",           "_Distance_constraint_list"); 
            mRDataType2TableName.put("dihedral angle",     "_Torsion_angle_constraint_list"); 
            mRDataType2TableName.put("dipolar coupling",   "_RDC_constraint_list"); 
            mRDataType2TableName.put("comment",            "_Org_constr_file_comment"); 
            mRDataType2TagNamePrefix = new HashMap();
            mRDataType2TagNamePrefix.put("distance",           "Distance_constraint"); 
            mRDataType2TagNamePrefix.put("dihedral angle",     "Torsion_angle_constraint"); 
            mRDataType2TagNamePrefix.put("dipolar coupling",   "RDC_constraint"); 
            mRDataType2TagNamePrefix.put("comment",            "MR_file_comment"); 
            mRDataType2TableNamePrefix = new HashMap();
            mRDataType2TableNamePrefix.put("distance",           "_Dist"); 
            mRDataType2TableNamePrefix.put("dihedral angle",     "_TA"); 
            mRDataType2TableNamePrefix.put("dipolar coupling",   "_RDC"); 
            mRDataType2TableNamePrefix.put("comment",            null);  // Not applicable.
            
            mRDataType2SfCategoryInvert    = MapSpecific.invertHashMap( mRDataType2SfCategory );
            mRDataType2TableNamePrefixInvert = MapSpecific.invertHashMap( mRDataType2TableNamePrefix );
        } catch ( Throwable t ) {
            General.showThrowable(t);
        }
    }
    
    /** Creates new NmrStar */
    public NmrStar() {
    }


    
    public static String getMRDataType2SfCategory( String type) {
        Object o = mRDataType2SfCategory.get(type);
        if ( o == null ) {
            General.showWarning("saveframe category not found for type: " + type );
            return null;
        }   
        return (String) o;        
    }
    
    public static String getMRDataType2TableName( String type) {
        Object o = mRDataType2TableName.get(type);
        if ( o == null ) {
            General.showWarning("table name not found for type: " + type );
            return null;
        }   
        return (String) o;        
    }
    
    public static String getMRDataType2TableNamePrefix( String type) {
        Object o = mRDataType2TableNamePrefix.get(type);
        if ( o == null ) {
            General.showWarning("table name not found for type: " + type );
            return null;
        }   
        return (String) o;        
    }
    
    public static String getDataTypeForSaveFrameCategoryName( String sf_category) {
        Object o = mRDataType2SfCategoryInvert.get(sf_category);
        if ( o == null ) {
            General.showWarning("sf_category not found: " + sf_category );
            return null;
        }   
        return (String) o;        
    }
        

    /** Derives the saveframe category from the first tag in the saveframe
     *which needs to be the save_frame specifying tag.
     *Returns null and a verbose message, otherwise.
     */
    public static String getSaveFrameCategoryName( SaveFrameNode save_frame_node, int star_version ) {

        DataItemNode data_item_node_sf_category = null;
        StarNode sn = save_frame_node.firstElement();                
        if ( !(sn instanceof DataItemNode) ) {
            General.showError("expected the first tag in the saveframe to be the saveframe category but type didn't match.");
            General.showError( NmrStar.toString( save_frame_node) );
            return null;
        }                
        // Let's make it a candidate.
        data_item_node_sf_category = (DataItemNode) sn;
        // See if it's name is valid.
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                String tag_name = "_Saveframe_category";
                if ( ! data_item_node_sf_category.getLabel().equals(tag_name)) {
                    General.showError("expected the first tag in the saveframe to be the saveframe category but label isn't right.");
                    General.showError( NmrStar.toString( save_frame_node) );
                    return null;
                }                    
                break;
            }
            case STAR_VERSION_3_1: // so it defaults to next block right?
            case STAR_VERSION_3_0: {
                String tag_name_end = ".Sf_category";
                if ( ! data_item_node_sf_category.getLabel().endsWith(tag_name_end)) {
                    General.showError("expected the first tag in the saveframe to be the saveframe category but label isn't right.");
                    General.showError( NmrStar.toString( save_frame_node) );
                    return null;
                }                    
                break;
            }
            default: {
                General.showError("code bug getSaveFrameCategoryName. Unknown nmr-star format: " + star_version );
                return null;
            }
        }
        // Ok we trust the tag.        
        return data_item_node_sf_category.getValue();
    }
    
    /** Adds a bunch of free tags usually found at the beginning of a constraints saveframe
     * @return <CODE>true</CODE> for success.
     * @param sf_category name of the saveframe category
     * @param tag_prefix table name like _Distance_constraint
     * @param id The id of the list in it's category.
     * Example 2 if it's the second of the distance constraints
     * @param block_position Position of the block in the mr file.
     * @param program string
     * @param type string
     * @param subtype string
     * @param format string
     * @param star_version enumerated value.
     * @param save_frame_node_constraint Node to append to.
     */    
    public static boolean addConstraintsHeader( SaveFrameNode save_frame_node_constraint, String sf_category, 
        String tag_prefix, String id, String block_position, String program, String type, String subtype, String format,
        int star_version, String fileName ) {

        // exit status
        boolean status = true;
        int pos = 0;
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                save_frame_node_constraint.insertElementAt( new DataItemNode( "_Saveframe_category", sf_category ),pos++ );
                save_frame_node_constraint.insertElementAt( new DataItemNode( "_BMRB_dev_Position_block_in_MR_file", block_position ),pos++ );
                break;
            }
            case STAR_VERSION_3_0: {
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Sf_category",       sf_category ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".ID",                id ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".MR_file_block_position",    block_position ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Program",           program ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Type",              type ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Subtype",           subtype ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Format",            format ),pos++  );
                break;
            }
            case STAR_VERSION_3_1: {
                int Constraint_file_content_ID = Integer.parseInt(block_position) + 0;
                String block_number_IDStr = new Integer(Constraint_file_content_ID).toString();
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Sf_category",       sf_category ),pos++  );
//                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Entry_ID",          "1" ),pos++  );
//                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".ID",                "1" ),pos++  );
//                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Data_file_name",    fileName ),pos++  );
                if ( type!=null ) {                    
                    if ( type.equals("comment") || type.equals("dihedral angle") || type.equals("dipolar coupling")) {
//                        save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Type",              type ),pos++  );
                    } else { 
                        save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Constraint_type",   subtype ),pos++  );
                    }
                }
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Constraint_file_ID","1" ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Block_ID",  block_number_IDStr ),pos++  );
                save_frame_node_constraint.insertElementAt( new DataItemNode( tag_prefix+".Details",            "Generated by Wattos" ),pos++  );
                break;
            }
            default: {
                General.showError("code bug addConstraintsHeader. Unknown nmr-star format: " + star_version );
                status = false;
            }
        }
        return status;
    }
    
    /** Adds a bunch of free tags usually found at the beginning of a constraints saveframe 
     * 
     * @return <CODE>true</CODE> for success.
     */    
    public static SaveFrameNode addConstraintsOverallHeader( BlockNode block_node, String pdb_id, int star_version ) {

        SaveFrameNode save_frame_node_chars                 = new SaveFrameNode("save_global_Org_file_characteristics");
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_chars    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_chars            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_chars                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_chars                      = new LoopRowNode( );
      
        switch ( star_version ) {
            case NmrStar.STAR_VERSION_2_1_1: {
                save_frame_node_chars.addElement( new DataItemNode( "_Saveframe_category",      "MR_file_characteristics" ) );
                save_frame_node_chars.addElement( new DataItemNode( "_NMR_STAR_version",        "developmental" ) );
                save_frame_node_chars.addElement( new DataItemNode( "_BMRB_dev_PDB_id",         pdb_id.toUpperCase() ) );
                save_frame_node_chars.addElement( new DataItemNode( "_BMRB_dev_MR_file_name",   pdb_id + ".mr" ) );
                save_frame_node_chars.addElement( new DataItemNode( "_BMRB_dev_conversion_date", Dates.getDateBMRBStyle() ) );        

                // Create header of table.
                loopnamelistnode_chars.addElement( new DataNameNode( "_BMRB_dev_Position_block_in_MR_file" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_BMRB_dev_Program" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_BMRB_dev_Type" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_BMRB_dev_Subtype" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_BMRB_dev_Format" ));
                break;
            }
            case NmrStar.STAR_VERSION_3_0: {
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.Sf_category",        "file_characteristics")); 
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.ID",                 "1"));
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.NMR_STAR_version",   "3.0"));
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.PDB_ID",             pdb_id.toUpperCase() ) );
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.MR_file_name",       pdb_id + ".mr" ) );
                save_frame_node_chars.addElement( new DataItemNode( "_File_characteristics.Conversion_date",    Dates.getDateBMRBStyle() ) );      

                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.File_characteristics_ID" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.MR_file_block_position" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.Program" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.Type" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.Subtype" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_File_characteristic.Format" ));                
                break;
            }
            case NmrStar.STAR_VERSION_3_1: {
                
                save_frame_node_chars.addElement( new DataItemNode( "_Constraint_stat_list.Sf_category",        "constraint_statistics")); 
//                save_frame_node_chars.addElement( new DataItemNode( "_Constraint_stat_list.Entry_ID",           "?"));
//                save_frame_node_chars.addElement( new DataItemNode( "_Constraint_stat_list.ID",                 "1"));

                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.ID" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_filename" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Software_ID" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Software_label" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Software_name" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Block_ID" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_type" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_subtype" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_subsubtype" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_number" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Entry_ID" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Constraint_file.Constraint_stat_list_ID" ));
                break;
            }
            default: {
                General.showError("code bug MRSTARFile. Unknown nmr-star format: " + star_version );
                return null;
            }
        }
        
        dataloopnamelistnode_chars.addElement(loopnamelistnode_chars);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_chars.setTabFlag( true );
        DataLoopNode dataloopnode_chars = new DataLoopNode( dataloopnamelistnode_chars, looptablenode_chars );                
        save_frame_node_chars.addElement( dataloopnode_chars );
        
        return save_frame_node_chars;
    }

    /** Adds a bunch of free tags usually found at the beginning of a constraints saveframe 
     * 
     * @return <CODE>true</CODE> for success.
     */    
    public static SaveFrameNode addConstraintsStudyHeader( BlockNode block_node, String pdb_id, int star_version ) {

        SaveFrameNode save_frame_node_chars                 = new SaveFrameNode("save_Conversion_project");
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_chars    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_chars            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_chars                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_chars                      = new LoopRowNode( );
      
        switch ( star_version ) {
            case NmrStar.STAR_VERSION_3_1: {
                
                save_frame_node_chars.addElement( new DataItemNode( "_Study_list.Sf_category",        "study_list")); 
//                save_frame_node_chars.addElement( new DataItemNode( "_Study_list.Entry_ID",           "."));
//                save_frame_node_chars.addElement( new DataItemNode( "_Study_list.ID",                 "1"));

                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.ID" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.Name" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.Type" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.Details" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.Entry_ID" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Study.Study_list_ID" ));
                break;
            }
            default: {
                General.showError("code bug MRSTARFile. Unknown nmr-star format: " + star_version );
                return null;
            }
        }
        
        dataloopnamelistnode_chars.addElement(loopnamelistnode_chars);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_chars.setTabFlag( true );
        DataLoopNode dataloopnode_chars = new DataLoopNode( dataloopnamelistnode_chars, looptablenode_chars );                
        save_frame_node_chars.addElement( dataloopnode_chars );
        
        return save_frame_node_chars;
    }
    
    public static SaveFrameNode addConstraintsEntryHeader( BlockNode block_node, String pdb_id, int star_version ) {

        SaveFrameNode save_frame_node_chars                 = new SaveFrameNode("save_entry_information");
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_chars    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_chars            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_chars                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_chars                      = new LoopRowNode( );

        switch ( star_version ) {
            case NmrStar.STAR_VERSION_3_1: {                
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Sf_category",        "entry_information")); 
//                save_frame_node_chars.addElement( new DataItemNode( "_Entry.ID",                 "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Title",                 "Original constraint list(s)"));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Version_type",          "original"));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Submission_date",       "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Accession_date",        "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Last_release_date",     "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Original_release_date", "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Origination",           "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.NMR_STAR_version",      "3.1"));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Original_NMR_STAR_version",   "."));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Experimental_method",         "NMR"));
                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Experimental_method_subtype", "."));
//                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Assigned_BMRB_ID", "."));
//                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Assigned_BMRB_deposition_code", "."));                 
//                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Assigned_PDB_ID", pdb_id));
//                save_frame_node_chars.addElement( new DataItemNode( "_Entry.Assigned_PDB_deposition_code", "."));
                
                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Database_name" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Database_accession_code" ));
                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Relationship" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Entry_ID" ));
                
                break;
            }
            default: {
                General.showError("code bug MRSTARFile. Unknown nmr-star format: " + star_version );
                return null;
            }
        }        
        dataloopnamelistnode_chars.addElement(loopnamelistnode_chars);
        looptablenode_chars.setTabFlag( true );
        DataLoopNode dataloopnode_chars = new DataLoopNode( dataloopnamelistnode_chars, looptablenode_chars );                
        save_frame_node_chars.addElement( dataloopnode_chars );        

        return save_frame_node_chars;
    }
    
    /** 
     *Only for the star_version 3.0.
     *In the case of constraints:
     *Adds to each table a column as the first column. The column will be labeled with a tag
     *having the same table name as the now second column label and post fixed with the 
     *id given to the method as a parameter.
     *In the case of a comment block:
     *Adds a free tag in front of the comment tag: _MR_file_comment.Comment.
     */    
    public static boolean addConstraintsIDs( SaveFrameNode save_frame_node_constraint, int star_version, String id ) {

        if ( star_version == STAR_VERSION_2_1_1 ) {
            return true;
        }
        if ( star_version == STAR_VERSION_3_1 ) {
            return true;
        }

        String saveframe_category = getSaveFrameCategoryName( save_frame_node_constraint, star_version );
        // Prefer to do the check on the data type defined in the program i.s.o. in dictionary.
        String data_type = getDataTypeForSaveFrameCategoryName( saveframe_category );

        //General.showDebug("data type is: [" + data_type + "]");
        
        // Do something special for comment block.
        if ( data_type.equals( "comment" ) ) {
            // Find the tag before which the new tag will be inserted.
            VectorCheckType vct = save_frame_node_constraint.searchByName( "_MR_file_comment.Comment" );            
            if ( vct.size() != 1 ) {
                General.showError("didn't find exactly 1 tag with name: _MR_file_comment.Comment in saveframe node. Found: " + vct.size() );
                General.showError( NmrStar.toString( save_frame_node_constraint) );
                return false;
            }                
            DataItemNode data_item_node_comment = (DataItemNode) vct.firstElement();
            int pos_comment = save_frame_node_constraint.indexOf(data_item_node_comment);           
            DataItemNode data_item_node_id = new DataItemNode( "_MR_file_comment.Comment_ID", id );            
            save_frame_node_constraint.insertElementAt(data_item_node_id,pos_comment);                        
            return true;
        }
        
        
        // Regular stuff for constraint blocks.
        VectorCheckType vct = save_frame_node_constraint.searchForType(dataLoopNodeClass, DataValueNode.DONT_CARE);
        if ( vct.size() < 1 ) {
            General.showError("didn't find at least 1 tables in saveframe node. Found: " + vct.size() );
            General.showError( NmrStar.toString( save_frame_node_constraint) );
            return false;
        }
        
                
        for (int pos=0;pos<vct.size();pos++) {
            DataLoopNode dataloopnode = (DataLoopNode) vct.elementAt(pos);            
            
//            LoopTableNode loop_table_node = dataloopnode.getVals();
            DataLoopNameListNode data_loop_name_list_node = dataloopnode.getNames();
            LoopNameListNode loop_name_list_node = data_loop_name_list_node.elementAt(0);
            DataNameNode data_name_node = (DataNameNode) loop_name_list_node.elementAt(0);
            String data_name_node_value = data_name_node.getValue();
            String tableName = getTableNameFromFullName( data_name_node_value );
            String tagName = tableName + ".Constraints_ID";
            DataNameNode dataNameNode = new DataNameNode(tagName);
            
            // Needs to be a new DataValueNode instance because parent will be different?
            loop_name_list_node.insertElementAt( dataNameNode, 0, new DataValueNode( id ));                        
        }
         
        
        return true;
    }
    

    /** Adds to each table an empty row as the last row. No checking done.
     *Empty is STAR_EMPTY.
     *If tables contain rows then the row append will only be done if
     *doEmptyTablesOnly is given as false.
     */    
    public static boolean addEmptyRowToAllTables( SaveFrameNode save_frame_node, boolean doEmptyTablesOnly ) {
        VectorCheckType vct = save_frame_node.searchForType(dataLoopNodeClass, DataValueNode.DONT_CARE);
        
        DataValueNode data_value_node = new DataValueNode( STAR_EMPTY );
        for (int pos=0;pos<vct.size();pos++) {
            //General.showDebug("Doing table: " + pos );
            DataLoopNode dataloopnode = (DataLoopNode) vct.elementAt(pos);
            LoopTableNode loop_table_node = dataloopnode.getVals();            
            int rowCount = loop_table_node.size();
            if ( (rowCount == 0 ) || (!doEmptyTablesOnly)) {
                DataLoopNameListNode data_loop_name_list_node = dataloopnode.getNames();
                LoopNameListNode loop_name_list_node = data_loop_name_list_node.elementAt(0);
                int columnCount = loop_name_list_node.size();

                // Don't know if this is the fastest way but it's not a performance issue.
                LoopRowNode looprownode       = new LoopRowNode();            
                for (int c=0;c<columnCount;c++) {            
                    looprownode.addElement( data_value_node );
                }
                loop_table_node.addElement( looprownode );                            
            }
        }
        
        return true;
    }
    

    /** Given _Distance_constraints.Sf_category this routine
     *returns _Distance_constraints. If the string doesn't contain a dot
     *the input will be returned.
     */
    public static String getTableNameFromFullName( String fullName ) {
        int dot_pos = fullName.indexOf('.');
        if ( dot_pos < 0 ) {
            return fullName;
        }
        String result = fullName.substring(0,dot_pos);
        return result;
    }

    /** Adds a saveframe category and empty loop(s) for this data type.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintDistance( SaveFrameNode save_frame_node_constraint, int star_version ) {

        // exit status
        boolean status = true;
        
        // Loop for logic, atom, and distance
        DataLoopNode dataloopnode_logic                         =  new DataLoopNode( true );
        DataLoopNode dataloopnode_atom                          =  new DataLoopNode( true );
        DataLoopNode dataloopnode_distance                      =  new DataLoopNode( true );

        // List of data names for the logic, atom, and distance loops
        LoopNameListNode loopnamelistnode_logic                 = new LoopNameListNode();
        LoopNameListNode loopnamelistnode_atom                  = new LoopNameListNode();
        LoopNameListNode loopnamelistnode_distance              = new LoopNameListNode();

        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                // 5 values
                loopnamelistnode_logic.addElement( new DataNameNode( "_Constraint_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Constraint_tree_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Constraint_tree_down_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Constraint_tree_right_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Constraint_tree_logic_operation" ));

                // 13 values
                loopnamelistnode_atom.addElement( new DataNameNode( "_Constraint_tree_node_member_constraint_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Constraint_tree_node_member_node_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Contribution_fractional_value" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Constraint_tree_node_member_ID" ));
                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Mol_system_atom_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Mol_system_component_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Mol_system_component_code" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Chemical_comp_index_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Chemical_comp_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Residue_seq_code" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Residue_label" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Atom_name" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Atom_type" ));

                // 11 values
                loopnamelistnode_distance.addElement( new DataNameNode( "_Distance_constraint_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Distance_constraint_tree_node_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Source_experiment_code" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Spectral_peak_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Spectral_peak_volume" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Spectral_peak_weight" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Spectral_peak_ppm1" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Spectral_peak_ppm2" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Distance_value" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Distance_lower_bound_value" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Distance_upper_bound_value" ));

                // Fill the table with an empty looptablenode, the elements to be appended later.
                dataloopnode_logic.getNames().addElement(       loopnamelistnode_logic );
                dataloopnode_atom.getNames().addElement(        loopnamelistnode_atom );
                dataloopnode_distance.getNames().addElement(    loopnamelistnode_distance );
                save_frame_node_constraint.addElement( dataloopnode_logic );        
                save_frame_node_constraint.addElement( dataloopnode_atom );        
                save_frame_node_constraint.addElement( dataloopnode_distance );
                break;
            }
            case STAR_VERSION_3_0: {
                // 5 values as in 2.1.1
                //not done: loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Constraints_ID" ));
                // this will be done later for all constraint data types in a similar way and when all info is
                // present too.
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Down_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Right_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Logic_operation" ));
                
                // 13 values
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Dist_constraint_tree_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Tree_node_member_node_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Contribution_fractional_val" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Constraint_tree_node_member_ID" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Entity_assembly_ID" ));     
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Label_entity_ID" ));              
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Label_comp_index_ID" ));               
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Label_comp_ID" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Label_atom_ID" ));                                 
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_segment_code" )); // Next group will be filled.
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_seq_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_comp_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_atom_ID" ));                //
                
                // 13 values
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Constraint_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Tree_node_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Source_experiment_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ID" ));        
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_val" ));                   
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_lower_val_err" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_upper_val_err" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_val" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_lower_bound_val" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_upper_bound_val" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Weight" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ppm_1" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ppm_2" ));
                
                // Fill the table with an empty looptablenode, the elements to be appended later.
                dataloopnode_logic.getNames().addElement(       loopnamelistnode_logic );
                dataloopnode_atom.getNames().addElement(        loopnamelistnode_atom );
                dataloopnode_distance.getNames().addElement(    loopnamelistnode_distance );
                save_frame_node_constraint.addElement( dataloopnode_logic );        
                save_frame_node_constraint.addElement( dataloopnode_atom );        
                save_frame_node_constraint.addElement( dataloopnode_distance );
                break;
            }
            case STAR_VERSION_3_1: {
                // 5 values
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Constraint_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Down_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Right_node_ID" ));
                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Logic_operation" ));
//                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Entry_ID" ));
//                loopnamelistnode_logic.addElement( new DataNameNode( "_Dist_constraint_tree.Distance_constraint_list_ID" ));
                
                // 15 values
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Tree_node_member_constraint_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Tree_node_member_node_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Constraint_tree_node_member_ID" ));

                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Assembly_atom_ID" ));     
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Entity_assembly_ID" ));     
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Entity_ID" ));              
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Comp_index_ID" ));               
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Seq_ID" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Comp_ID" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Atom_ID" ));                
//                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Atom_type" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Resonance_ID" ));                
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_asym_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_seq_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_comp_ID" ));
                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Auth_atom_ID" ));
//                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Entry_ID" ));
//                loopnamelistnode_atom.addElement( new DataNameNode( "_Dist_constraint.Distance_constraint_list_ID" ));
                                
                // 10 values
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Constraint_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Tree_node_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Source_experiment_ID" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ID" ));        
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_val" ));                   
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_lower_val_err" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Intensity_upper_val_err" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_val" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_lower_bound_val" ));
                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_upper_bound_val" ));
//                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Weight" ));
//                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ppm_1" ));
//                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Spectral_peak_ppm_2" ));
//                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Entry_ID" ));
//                loopnamelistnode_distance.addElement( new DataNameNode( "_Dist_constraint_value.Distance_constraint_list_ID" ));
                
                // Fill the table with an empty looptablenode, the elements to be appended later.
                dataloopnode_logic.getNames().addElement(       loopnamelistnode_logic );
                dataloopnode_atom.getNames().addElement(        loopnamelistnode_atom );
                dataloopnode_distance.getNames().addElement(    loopnamelistnode_distance );
                save_frame_node_constraint.addElement( dataloopnode_logic );        
                save_frame_node_constraint.addElement( dataloopnode_atom );        
                save_frame_node_constraint.addElement( dataloopnode_distance );
                break;
            }
            default: {
                General.showError("code bug addConstraintDistance. Unknown nmr-star format: " + star_version );
                status = false;
            }
        }
        
        return status;
    }
    
    
    /** Adds a saveframe category and empty loop(s) for this data type.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintDihedral( SaveFrameNode save_frame_node_constraint, int star_version ) {

        // Loop for constraints
        DataLoopNode dataloopnode_constraint                    = null;
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_constraint    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_constraint            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_constraint                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_constraint                      = new LoopRowNode( );

        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                //save_frame_node_constraint.addElement( new DataItemNode( "_Saveframe_category", "torsion_angle_constraints" ) );

                // Create header of table.
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Constraint_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Angle_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_three_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_three_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_three_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_three_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_four_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_four_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_four_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_four_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Angle_upper_bound_value" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Angle_lower_bound_value" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Force_constant_value" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Potential_function_exponent" ));
                break;
            }
            case STAR_VERSION_3_0: {
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.ID" ));         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Torsion_angle_name" ));         
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_assembly_ID_1" )); 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_ID_1" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_index_ID_1" )); 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_ID_1" ));     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_atom_ID_1" ));        
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_assembly_ID_2" ));  
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_index_ID_2" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_ID_2" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_atom_ID_2" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_assembly_ID_3" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_index_ID_3" ));            
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_ID_3" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_atom_ID_3" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_assembly_ID_4" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_entity_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_index_ID_4" ));             
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_comp_ID_4" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Label_atom_ID_4" ));              
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_segment_code_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_segment_code_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_segment_code_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_segment_code_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_4" ));              
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Angle_upper_bound_val" ));     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Angle_lower_bound_val" ));                 
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Force_constant_value" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Potential_function_exponent" ));
                break;
            }
            case STAR_VERSION_3_1: {
                // 55 columns
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.ID" ));         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Torsion_angle_name" ));                         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Assembly_atom_ID_1" ));  
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_assembly_ID_1" )); 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_ID_1" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_index_ID_1" )); 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Seq_ID_1" ));     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_ID_1" ));     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_ID_1" ));        
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_type_1" ));        
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Resonance_ID_1" ));        
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Assembly_atom_ID_2" ));  
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_assembly_ID_2" ));  
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_index_ID_2" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Seq_ID_2" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_ID_2" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_ID_2" ));              
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_type_2" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Resonance_ID_2" ));                              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Assembly_atom_ID_3" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_assembly_ID_3" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_index_ID_3" ));            
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Seq_ID_3" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_ID_3" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_ID_3" ));              
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_type_3" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Resonance_ID_3" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Assembly_atom_ID_4" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_assembly_ID_4" ));   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entity_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_index_ID_4" ));             
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Seq_ID_4" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Comp_ID_4" ));                   
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_ID_4" ));              
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Atom_type_4" ));              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Resonance_ID_4" ));                              
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Angle_lower_bound_val" ));                 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Angle_upper_bound_val" ));     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Source_experiment_ID" ));                     
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_asym_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_asym_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_asym_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_3" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_asym_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_seq_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_comp_ID_4" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Auth_atom_ID_4" ));                              
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Entry_ID" ));              
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_Torsion_angle_constraint.Torsion_angle_constraint_list_ID" ));              
                break;
            }
            default: {
                General.showError("code bug . Unknown nmr-star format: " + star_version );
                return false;
            }
        }

        dataloopnamelistnode_constraint.addElement(loopnamelistnode_constraint);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_constraint.setTabFlag( true );
        dataloopnode_constraint = new DataLoopNode( dataloopnamelistnode_constraint, looptablenode_constraint );                
        save_frame_node_constraint.addElement( dataloopnode_constraint );        
        
        return true;
                        
    }

        
    /** Adds a saveframe category and empty loop(s) for this datatype.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintDipolarCoupling( SaveFrameNode save_frame_node_constraint, int star_version ) {

        // Loop for constraints
        DataLoopNode dataloopnode_constraint                    = null;
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_constraint    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_constraint            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_constraint                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_constraint                      = new LoopRowNode( );

        //save_frame_node_constraint.addElement( new DataItemNode( "_Saveframe_category", "residual_dipolar_couplings" ) );
        
        // Create header of table.
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_one_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_mol_system_component_ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_residue_seq_code" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_residue_label" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Atom_two_atom_name" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_value" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_value_error" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_lower_bound_value" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_Residual_dipolar_coupling_upper_bound_value" ));
                break;
            }
            case STAR_VERSION_3_0: {
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.ID" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_entity_assembly_ID_1" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_entity_ID_1" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_comp_index_ID_1" ));                         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_atom_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_entity_assembly_ID_2" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_entity_ID_2" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_comp_index_ID_2" ));                         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_comp_ID_2" ));                        
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Label_atom_ID_2" ));    
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_segment_code_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_seq_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_atom_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_segment_code_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_seq_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_comp_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_atom_ID_2" ));                 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_val" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_lower_bound" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_upper_bound" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_val_err" ));
                break;
            }
            case STAR_VERSION_3_1: { 
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.ID" ));
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Assembly_atom_ID_1" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Entity_assembly_ID_1" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Entity_ID_1" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Comp_index_ID_1" ));                         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Seq_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Atom_ID_1" ));
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Assembly_atom_ID_2" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Entity_assembly_ID_2" ));               
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Entity_ID_2" ));          
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Comp_index_ID_2" ));                         
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Seq_ID_2" ));                        
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Comp_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Atom_ID_2" ));   
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_val" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_lower_bound" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_upper_bound" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_val_err" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Source_experiment_ID" ));
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Resonance_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Resonance_ID_2" ));
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_asym_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_seq_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_comp_ID_1" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_atom_ID_1" ));
                
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_asym_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_seq_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_comp_ID_2" ));
                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Auth_atom_ID_2" ));  
                
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.Entry_ID" ));  
//                loopnamelistnode_constraint.addElement( new DataNameNode( "_RDC_constraint.RDC_constraint_list_ID" ));  
                break;
            }
            default: {
                General.showError("code bug . Unknown nmr-star format: " + star_version );
                return false;
            }
        }
        

        dataloopnamelistnode_constraint.addElement(loopnamelistnode_constraint);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_constraint.setTabFlag( true );
        dataloopnode_constraint = new DataLoopNode( dataloopnamelistnode_constraint, looptablenode_constraint );                
        save_frame_node_constraint.addElement( dataloopnode_constraint );        
         
        return true;
                        
    }

    
    /** Adds a saveframe category and empty loop(s) for this datatype.
     * @return <CODE>true</CODE> for success.
     * @param star_version One of the specified versions.
     * @param tableNamePrefix E.g. _Dist to get tables
     * that are labeled: _Dist_constraint_comment_org
     * @param save_frame_node_constraint Node to append to.
     */    
    public static boolean addConstraintComments( SaveFrameNode save_frame_node_constraint, 
        int star_version, String tableNamePrefix ) {
        
        // Loop for comments
        DataLoopNode dataloopnode_comment                      = null;
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_comment    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_comment            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_comment                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_comment                      = new LoopRowNode( );
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                // Create header of table.
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment_ID" ));
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment" ));
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment_begin_line" ));
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment_begin_column" ));
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment_end_line" ));
                loopnamelistnode_comment.addElement( new DataNameNode( "_Constraint_comment_end_column" ));
                break;
            }
            case STAR_VERSION_3_0: {
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_comment_org";
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_begin_line" )));            
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_begin_column" )));          
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_end_line" )));              
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_end_column" )));                            
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment" )));       
                break;
            }
            case STAR_VERSION_3_1: {
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_comment_org";
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_begin_line" )));            
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_begin_column" )));          
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_end_line" )));              
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_end_column" )));                            
//                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Entry_ID" )));
//                String tagNamePrefix = (String) mRDataType2TagNamePrefix.get(
//                        mRDataType2TableNamePrefixInvert.get(tableNamePrefix));
//                loopnamelistnode_comment.addElement( new DataNameNode( tableName+"."+tagNamePrefix+"_list_ID" ));       
                loopnamelistnode_comment.addElement( new DataNameNode( tableName.concat(".Comment_text" )));       
                break;
            }
            default: {
                General.showError("code bug addConstraintComments. Unknown nmr-star format: " + star_version );
                return false;
            }
        }
        dataloopnamelistnode_comment.addElement(loopnamelistnode_comment);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_comment.setTabFlag( true );
        dataloopnode_comment = new DataLoopNode( dataloopnamelistnode_comment, looptablenode_comment );                
        save_frame_node_constraint.addElement( dataloopnode_comment );        
        
        return true;                        
    }
   

    /** Adds a saveframe category and empty loop(s) for this datatype.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintParseErrors( SaveFrameNode save_frame_node_constraint, int star_version,
            String tableNamePrefix) {

        // Loop for comments
        DataLoopNode dataloopnode_parse_error                    = null;
        // List of Lists of data names.
        DataLoopNameListNode dataloopnamelistnode_parse_error    = new DataLoopNameListNode();
        // List of data names.
        LoopNameListNode loopnamelistnode_parse_error            = new LoopNameListNode();
        // Table
        LoopTableNode looptablenode_parse_error                  = new LoopTableNode();
        // Row
//        LoopRowNode looprownode_parse_error                      = new LoopRowNode( );
        // Create header of table.
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_ID" ));
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_content" ));
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_begin_line" ));
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_begin_column" ));
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_end_line" ));
                loopnamelistnode_parse_error.addElement( new DataNameNode( "_Org_file_parse_err_end_column" ));
                break;
            }
            case STAR_VERSION_3_0: {
                String tableName = tableNamePrefix + "_constraint_org_file_parse_err";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Begin_line" )));            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Begin_column" )));          
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".End_line" )));              
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".End_column" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Content" )));       
                break;
            }
            case STAR_VERSION_3_1: {
                String tableName = tableNamePrefix + "_constraint_parse_err";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Begin_line" )));            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Begin_column" )));          
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".End_line" )));              
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".End_column" )));                            
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Entry_ID" )));
//                String tagNamePrefix = (String) mRDataType2TagNamePrefix.get(
//                        mRDataType2TableNamePrefixInvert.get(tableNamePrefix));
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName+"."+tagNamePrefix+"_list_ID" ));       
                
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Content" )));       
                break;
            }
            default: {
                General.showError("code bug addConstraintParseErrors. Unknown nmr-star format: " + star_version );
                return false;
            }
        }

        dataloopnamelistnode_parse_error.addElement(loopnamelistnode_parse_error);
        
        // Fill the table with an empty looptablenode, the elements to be appended later.
        looptablenode_parse_error.setTabFlag( true );
        dataloopnode_parse_error = new DataLoopNode( dataloopnamelistnode_parse_error, looptablenode_parse_error );                
        save_frame_node_constraint.addElement( dataloopnode_parse_error );        
        
        return true;                        
    }

    /** Adds empty loop(s) for this datatype.
     * @param save_frame_node_comment Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addComment( SaveFrameNode save_frame_node_comment, int star_version,
            String tableNamePrefix, String text) {
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                save_frame_node_comment.addElement( new DataItemNode( "_BMRB_dev_Comment", text, DataValueNode.SEMICOLON) );                
                return true; // nothing needed in 2.1.1
            }
            case STAR_VERSION_3_0: {
                save_frame_node_comment.addElement( new DataItemNode( "_MR_file_comment.Comment", text, DataValueNode.SEMICOLON) );                
                break;
            }
            case STAR_VERSION_3_1: {
                save_frame_node_comment.addElement( new DataItemNode( "_Org_constr_file_comment.Comment", text, DataValueNode.SEMICOLON) );                
                break;
            }
            default: {
                General.showError("code bug addConstraintParseErrors. Unknown nmr-star format: " + star_version );
                return false;
            }
        }
        
        return true;                        
    }
    
    
    /** Adds empty loop(s) for this datatype.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintConversionErrors( SaveFrameNode save_frame_node_constraint, int star_version,
            String tableNamePrefix) {
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                return true; // nothing needed in 2.1.1
            }
            case STAR_VERSION_3_0: {
                // Loop for comments
                DataLoopNode dataloopnode_parse_error                    = null;
                // List of Lists of data names.
                DataLoopNameListNode dataloopnamelistnode_parse_error    = new DataLoopNameListNode();
                // List of data names.
                LoopNameListNode loopnamelistnode_parse_error            = new LoopNameListNode();
                // Table
                LoopTableNode looptablenode_parse_error                  = new LoopTableNode();
                // Row
//                LoopRowNode looprownode_parse_error                      = new LoopRowNode( );
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_parse_file_conv_err";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Parse_file_ID" )));       
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Parse_file_sf_label" )));            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Parse_file_constraint_ID" )));          
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Conv_error_type" )));              
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Conv_error_note" )));                            

                
                dataloopnamelistnode_parse_error.addElement(loopnamelistnode_parse_error);

                // Fill the table with an empty looptablenode, the elements to be appended later.
                looptablenode_parse_error.setTabFlag( true );
                dataloopnode_parse_error = new DataLoopNode( dataloopnamelistnode_parse_error, looptablenode_parse_error );                
                save_frame_node_constraint.addElement( dataloopnode_parse_error );        
                break;
            }
            case STAR_VERSION_3_1: {
                // Loop for comments
                DataLoopNode dataloopnode_parse_error                    = null;
                // List of Lists of data names.
                DataLoopNameListNode dataloopnamelistnode_parse_error    = new DataLoopNameListNode();
                // List of data names.
                LoopNameListNode loopnamelistnode_parse_error            = new LoopNameListNode();
                // Table
                LoopTableNode looptablenode_parse_error                  = new LoopTableNode();
                // Row
//                LoopRowNode looprownode_parse_error                      = new LoopRowNode( );
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_conv_err";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName+"."+tableNamePrefix.substring(1)+"_constr_parse_file_ID" ));       
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Parse_file_constraint_ID" )));          
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Conv_error_type" )));              
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Entry_ID" )));
//                String tagNamePrefix = (String) mRDataType2TagNamePrefix.get(
//                        mRDataType2TableNamePrefixInvert.get(tableNamePrefix));
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName+"."+tagNamePrefix+"_list_ID" ));       
                
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Conv_error_note" )));                            

                
                dataloopnamelistnode_parse_error.addElement(loopnamelistnode_parse_error);

                // Fill the table with an empty looptablenode, the elements to be appended later.
                looptablenode_parse_error.setTabFlag( true );
                dataloopnode_parse_error = new DataLoopNode( dataloopnamelistnode_parse_error, looptablenode_parse_error );                
                save_frame_node_constraint.addElement( dataloopnode_parse_error );        
                break;
            }
            default: {
                General.showError("code bug addConstraintParseErrors. Unknown nmr-star format: " + star_version );
                return false;
            }
        }
        
        return true;                        
    }
    
    /** Adds empty loop(s) for this datatype.
     * @param save_frame_node_constraint Node to append to.
     * @return <CODE>true</CODE> for success.
     */    
    public static boolean addConstraintParseFile( SaveFrameNode save_frame_node_constraint, int star_version,
            String tableNamePrefix) {
        
        switch ( star_version ) {
            case STAR_VERSION_2_1_1: {
                return true; // nothing needed in 2.1.1
            }
            case STAR_VERSION_3_0: {
                // Loop for comments
                DataLoopNode dataloopnode_parse_error                    = null;
                // List of Lists of data names.
                DataLoopNameListNode dataloopnamelistnode_parse_error    = new DataLoopNameListNode();
                // List of data names.
                LoopNameListNode loopnamelistnode_parse_error            = new LoopNameListNode();
                // Table
                LoopTableNode looptablenode_parse_error                  = new LoopTableNode();
                // Row
//                LoopRowNode looprownode_parse_error                      = new LoopRowNode( );
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_parse_file";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Name" )));       

                
                dataloopnamelistnode_parse_error.addElement(loopnamelistnode_parse_error);

                // Fill the table with an empty looptablenode, the elements to be appended later.
                looptablenode_parse_error.setTabFlag( true );
                dataloopnode_parse_error = new DataLoopNode( dataloopnamelistnode_parse_error, looptablenode_parse_error );                
                save_frame_node_constraint.addElement( dataloopnode_parse_error );        
                break;
            }
            case STAR_VERSION_3_1: {
                // Loop for comments
                DataLoopNode dataloopnode_parse_error                    = null;
                // List of Lists of data names.
                DataLoopNameListNode dataloopnamelistnode_parse_error    = new DataLoopNameListNode();
                // List of data names.
                LoopNameListNode loopnamelistnode_parse_error            = new LoopNameListNode();
                // Table
                LoopTableNode looptablenode_parse_error                  = new LoopTableNode();
                // Row
//                LoopRowNode looprownode_parse_error                      = new LoopRowNode( );
                // Create header of table.
                String tableName = tableNamePrefix + "_constraint_parse_file";
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".ID" )));                            
                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Name" )));       
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName.concat(".Entry_ID" )));       
//                loopnamelistnode_parse_error.addElement( new DataNameNode( tableName+"."+tableNamePrefix.substring(1)+"_constraint_list_ID" ));       

                
                dataloopnamelistnode_parse_error.addElement(loopnamelistnode_parse_error);

                // Fill the table with an empty looptablenode, the elements to be appended later.
                looptablenode_parse_error.setTabFlag( true );
                dataloopnode_parse_error = new DataLoopNode( dataloopnamelistnode_parse_error, looptablenode_parse_error );                
                save_frame_node_constraint.addElement( dataloopnode_parse_error );        
                break;
            }
            default: {
                General.showError("code bug addConstraintParseErrors. Unknown nmr-star format: " + star_version );
                return false;
            }
        }
        
        return true;                        
    }    
    
    /** Get the textual representation in STAR. Other representations might be
     * possible in the future.
     * @return The STAR content.
     * @param sn Input starnode.
     */    
    public static String toString( StarNode sn ) {
        // Now output the result
        ByteArrayOutputStream os = new ByteArrayOutputStream();        
        StarUnparser myUnparser = new StarUnparser( os );
        myUnparser.setFormatting( true );
        myUnparser.writeOut( sn, 0 );        
        String result = os.toString();
        return result;
    }
    
    
    /** See if the value is null or empty in which case we would like to return the
     * STAR null value as defined elsewhere.
     * @param value Input
     * @return Always a valid STAR value.
     */
    public static String getPossibleValue(Object value) {
        if ( value == null || (!(value instanceof String))) {
            return( STAR_EMPTY );
        }
        String result = (String) value;
        if ( result.length() == 0 ) {
            return( STAR_EMPTY );
        }
        return( result );      
    }

    /** Generic routine to translate any table to an unnested STAR loop.
     */
    public static DataLoopNode toSTAR( Table table ) {

        // Encapsulates the labels
        DataLoopNameListNode data_loop_name_list_node = new DataLoopNameListNode();        
        // Labels
        LoopNameListNode loop_name_list_node = new LoopNameListNode();
        // Values
        LoopTableNode loop_table_node = new LoopTableNode( true );
        
        int ncols = table.sizeColumns();
        int nrows = table.sizeRows();
        for (int c=0;c<ncols;c++) {
            String label = "_" + table.getLabel(c);
            loop_name_list_node.addElement( new DataNameNode( label ));
        }
        
        for (int r=0;r<nrows;r++) {
            LoopRowNode loop_row_node = new LoopRowNode();
            for (int c=0;c<ncols;c++) {
                loop_row_node.addElement(new DataValueNode( (String) table.getValue(r,c) ));
            }
            
            loop_table_node.addElement( loop_row_node );
        }
        data_loop_name_list_node.addElement( loop_name_list_node );
        
        return new DataLoopNode( data_loop_name_list_node, loop_table_node );
    }


}
