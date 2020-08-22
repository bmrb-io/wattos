/*
 * MRSTARFile.java
 *
 * Created on June 25, 2002, 4:30 PM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import EDU.bmrb.starlibj.BlockNode;
import EDU.bmrb.starlibj.DataLoopNameListNode;
import EDU.bmrb.starlibj.DataLoopNode;
import EDU.bmrb.starlibj.DataValueNode;
import EDU.bmrb.starlibj.LoopRowNode;
import EDU.bmrb.starlibj.LoopTableNode;
import EDU.bmrb.starlibj.RemoteInt;
import EDU.bmrb.starlibj.SaveFrameNode;
import EDU.bmrb.starlibj.StarFileNode;
import EDU.bmrb.starlibj.StarParser;
import EDU.bmrb.starlibj.StarUnparser;
import EDU.bmrb.starlibj.StarValidity;
import EDU.bmrb.starlibj.VectorCheckType;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Star.StarFileReader;
import Wattos.Star.NMRStar.File31;
import Wattos.Star.NMRStar.ValidatorDictionary;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.NmrStar;

/** Contains some methods to deal with STAR files following the standards for
 * a STAR file generated from the MR files. Just for convenience.
 *No actual star tag names are allowed here.
 *
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRSTARFile {    
    
    /** Content of complete file. */
    public StarFileNode star_file_node                         = null;
    /** Everything in one data block (for BMRB entries that is all) */
    public BlockNode block_node_mrfile                         = null;
    /** The saveframe level */
    public SaveFrameNode save_frame_node_chars                 = null;
    /** loop_ t/m stop_ (Table including the header)
     */
    public DataLoopNode dataloopnode_chars                     = null; 

    /** Keep track of the number of occurrences of each data type 
     */
    public HashMap dataOccurences                               = null;
    /** StarParser is only allowed to be invoke as a static class 
     */
    static StarParser aParser = null;
    
    /** Map for subtypes to STAR enumerations */
    public static HashMap subTypeMap2STAR = new HashMap();
    /** Map for subtypes to STAR enumerations */
    public static HashMap subsubTypeMap2STAR = new HashMap();
        
    public static ValidatorDictionary validDictionary = new ValidatorDictionary();
    static {
        subTypeMap2STAR.put("n/a", "Not applicable");
        subsubTypeMap2STAR.put("n/a", "Not applicable");
        validDictionary.readFile(null);                
    }
    public MRSTARFile() {
    }
    
    /** Creates new MRSTARFile
     * @param block_position position of the block to include. Special value BLOCK_ID_INDICATING_ALL 
     *indicates all blocks should be added.
     * @param mrf File to be converted.
     * @param classification
     */
    public boolean init( DBMRFile mrf, Classification classification, int block_position, int star_version ) {
        
        boolean status;
        boolean converted_something = false;
        
        String pdb_id = null;
        if ( mrf.pdb_id == null ) {
            pdb_id = NmrStar.STAR_EMPTY;
        } else {
            pdb_id =  mrf.pdb_id;
        }
        
        // Initialize these global variables.
        dataOccurences                        = new HashMap();
        star_file_node                        = new StarFileNode();
        block_node_mrfile                     = new BlockNode("data_" + pdb_id + "_MR_file_constraints");
        star_file_node.addElement(block_node_mrfile);

        save_frame_node_chars = NmrStar.addConstraintsStudyHeader( block_node_mrfile, pdb_id, star_version );
        if ( save_frame_node_chars != null ) {
            // Save the reference for global use.
            dataloopnode_chars = (DataLoopNode) save_frame_node_chars.lastElement();
            // Add the saveframe.
            block_node_mrfile.addElement(save_frame_node_chars);
        }
        addStudy(star_version);
        save_frame_node_chars = NmrStar.addConstraintsEntryHeader( block_node_mrfile, pdb_id, star_version );
        if ( save_frame_node_chars != null ) {
            // Save the reference for global use.
            dataloopnode_chars = (DataLoopNode) save_frame_node_chars.lastElement();
            // Add the saveframe.
            block_node_mrfile.addElement(save_frame_node_chars);
        }
        addRelatedEntry(star_version,pdb_id);
        
        save_frame_node_chars = NmrStar.addConstraintsOverallHeader( block_node_mrfile, pdb_id, star_version );
        if ( save_frame_node_chars != null ) {
            // Save the reference for global use.
            dataloopnode_chars = (DataLoopNode) save_frame_node_chars.lastElement();
            // Add the saveframe.
            block_node_mrfile.addElement(save_frame_node_chars);
        }
        
        // CHECK next two blocks of code were swapped in order; CHECK
        // Add the blocks needed. This is just the one given by the position or all if
        // the position is set to the sentinel value.
        int pos = 0;
        for (Iterator i=mrf.blocks.iterator();i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            if ( ( block_position == DBMRFile.BLOCK_ID_INDICATING_ALL ) || ( pos == block_position ) ){
                status = addData( mrb, classification, pos, star_version, pdb_id ); 
                if ( status ) {
                    converted_something = true;
                }
            }
            pos++;                 
        }
        
        // Add a row into the first table in the first saveframe for each block
        pos = 0;
        for (Iterator i=mrf.blocks.iterator();i.hasNext();) {
            status = addDataOverview( (DBMRBlock) i.next(), pos, star_version );
            if ( ! status ) {
                General.showError("Failed to MRSTARFile.addDataOverview for block:" + pos);
                return false;
            }
            pos++;
        }
        
        
        
        // Do more post processing using Wattos API to STAR files because it's
        // easier to extend. Takes a back and forth trip but that's ok.
        //TO WATTOS API
        String buf = toString();
        DBMS dbms = new DBMS();
        StarFileReader sfr = new StarFileReader(dbms); 
        Wattos.Star.StarNode sn = sfr.parse( buf );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        
        
        // Post processing
        General.showDebug("Before post processing: "+sn.toSTAR());
        File31.enterStandardIDs(   sn, "parsed_" + pdb_id);
        validDictionary.sortTagNames( sn );
 
        //FROM WATTOS API
        buf = sn.toSTAR();
        General.showDebug("Parsed: "+sn.toSTAR());
        BufferedReader br = new BufferedReader(new StringReader(buf));
        try {
            StarParser.ReInit(br); //not needed unless called again.
            StarParser.StarFileNodeParse(Wattos.Star.StarGeneral.sp);
            // perhaps better to call from STATIC class but this works.
        } catch (Throwable t) { 
            General.showThrowable(t);
            return false;
        }
        star_file_node = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();
        
                
        if ( ! converted_something ) {
            //General.showDebug("didn't convert data in block: " + pos);
            //General.showDebug("Returning false in init");
            return false;
        }            
        return true;
    }

    /** Get the number of restraints or other items.
     *the only countable items are:
     *distances,
     *dipolar couplings,
     *dihedral angles.
         *Returns zero in case no items classified as restraints.
     *Duplication of same routine in DBMRBlock
     *@see DBMRBlock
     */
    public int getItemCount() {
        int sum = 0;
        try {
            String[] checkTagsCount = NmrStar.checkTagsCount;
            for (int i=0;i<checkTagsCount.length;i++) {
//                General.showDebug("Looking for tag: " + checkTagsCount[i]);
                VectorCheckType vct = star_file_node.searchForTypeByName(
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
        
        
    
    /** Convenience method. See namesake method.
     *Returns Defs.NULL_INT on error.
     */
    public static int getItemCount(String fileName) {
        /** Content of complete file. */
        StarFileNode star_file_node = null;
        try {
            //General.showDebug("reinit star parser");
            FileInputStream inStream = new FileInputStream(fileName);
            
            if( Wattos.Star.StarGeneral.sp == null ) {
                General.showCodeBug("Should have initiated aParser at: .");
                return Defs.NULL_INT;
            }
            StarParser.ReInit( inStream );
            StarParser.StarFileNodeParse( Wattos.Star.StarGeneral.sp );
            star_file_node = (StarFileNode) Wattos.Star.StarGeneral.sp.popResult();
        } catch ( Throwable t) {
            General.showThrowable(t);
            return Defs.NULL_INT;
        }
        MRSTARFile mrsf = new MRSTARFile();
        mrsf.star_file_node = star_file_node;        
        return mrsf.getItemCount();
    }
    
    /** Add the actual data from one block by adding another saveframe.
     * @return true for success
     * @param classification
     * @param pos Position of the block in the file.
     * @param mrb block
     */
    public boolean addData( DBMRBlock mrb, Classification classification, int pos, int star_version, String pdb_id ) {
        
        SaveFrameNode save_frame_node_constr = mrb.convert( classification, pos, star_version, pdb_id );
        if ( save_frame_node_constr == null ) {
            General.showOutput("Skipping block: " + pos );
            return false;
        }
        //General.showOutput("Adding to MRSTARFile converted block at position: " + pos );
        block_node_mrfile.addElement(save_frame_node_constr);
        
        
        String program  = mrb.type[0];
        String type     = mrb.type[1];
        String subtype  = mrb.type[2];
        String format   = mrb.type[3];
        
        String sf_category  = NmrStar.getMRDataType2SfCategory(type);
        if ( sf_category == null ) {
            General.showError("no sf category for type thus not adding data");
            return false;
        }
        String table_name   = NmrStar.getMRDataType2TableName(type);
        if ( table_name == null ) {
            General.showError("no sf category for type thus not adding data");
            return false;
        }
        
        int currentOccurenceCount = 0;
        if ( dataOccurences.containsKey( type ) ) {
            currentOccurenceCount = ((Integer) dataOccurences.get( type )).intValue();
        }
        currentOccurenceCount++;
        dataOccurences.put( type, new Integer( currentOccurenceCount )); // overwriting if needed.
        String currentOccurenceCountStr = Integer.toString( currentOccurenceCount );
        
        NmrStar.addConstraintsHeader(save_frame_node_constr, sf_category, table_name, 
            currentOccurenceCountStr, 
            Integer.toString( pos+1), 
            program, type, subtype, format, star_version, mrb.fileName );
        NmrStar.addConstraintsIDs( save_frame_node_constr, star_version, currentOccurenceCountStr);
        
        return true;
    }
    

    /** Add the data from one block to the
     * overview chars table (characteristics) in the first sf of the file.
     * @return true for success
     * @param pos Position of the block in the file.
     * @param mrb
     */
    public boolean addDataOverview( DBMRBlock mrb, int pos, int star_version ) {
                
        int block_id = dataloopnode_chars.getVals().size();
        // Fill the table with the original values.
        LoopRowNode looprownode_chars = new LoopRowNode();
        // Some STAR versions need extra ids.
        switch ( star_version ) {
            case NmrStar.STAR_VERSION_2_1_1: { 
                looprownode_chars.addElement(new DataValueNode( Integer.toString( block_id + 1)));        
                looprownode_chars.addElement(new DataValueNode( mrb.type[0]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[1]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[2]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[3]));
                break;
            }
            case NmrStar.STAR_VERSION_3_0: {
                looprownode_chars.addElement(new DataValueNode( "1" ));        
                looprownode_chars.addElement(new DataValueNode( Integer.toString( block_id + 1)));        
                looprownode_chars.addElement(new DataValueNode( mrb.type[0]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[1]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[2]));
                looprownode_chars.addElement(new DataValueNode( mrb.type[3]));
                break;
            }
            case NmrStar.STAR_VERSION_3_1: {
//                int itemCount = getItemCount();
                int itemCount = mrb.item_count; // fails when called from MRSTARFileTest.java                
                String itemCountStr = new Integer(itemCount).toString();
                if ( Defs.isNull(itemCount)) {
                    itemCountStr = "0";
                }
                String subType = mrb.type[2];
                if ( subTypeMap2STAR.containsKey(subType)) {
                    subType = (String) subTypeMap2STAR.get(subType);
                }
                String subsubType = mrb.type[3];
                if ( subsubTypeMap2STAR.containsKey(subsubType)) {
                    subsubType = (String) subTypeMap2STAR.get(subsubType);
                }
                
                looprownode_chars.addElement(new DataValueNode( "1" ));        
                looprownode_chars.addElement(new DataValueNode( mrb.fileName ));        
                looprownode_chars.addElement(new DataValueNode( "?" ));        
                looprownode_chars.addElement(new DataValueNode( "?" ));        
                looprownode_chars.addElement(new DataValueNode( mrb.type[0] ));    // program     
                looprownode_chars.addElement(new DataValueNode( Integer.toString( block_id + 1)));        
                looprownode_chars.addElement(new DataValueNode( mrb.type[1]));  // type
                looprownode_chars.addElement(new DataValueNode( subType ));         // subtype
                looprownode_chars.addElement(new DataValueNode( subsubType));      // format
                looprownode_chars.addElement(new DataValueNode( itemCountStr ));    // TODO fix for individual blocks.
//                looprownode_chars.addElement(new DataValueNode( "?" ));        
//                looprownode_chars.addElement(new DataValueNode( "1" ));        
                break;
            }
            default: {
                General.showError("code bug addDataOverview Unknown nmr-star format: " + star_version );
            }                            
        }                                                
        
        dataloopnode_chars.getVals().addElement( looprownode_chars );

        return true;
    }
    
    public boolean addStudy( int star_version ) {
        LoopRowNode looprownode_chars = new LoopRowNode();
        switch ( star_version ) {
            case NmrStar.STAR_VERSION_3_1: {
                looprownode_chars.addElement(new DataValueNode( "1" ));        
                looprownode_chars.addElement(new DataValueNode( "Conversion project" ));        
                looprownode_chars.addElement(new DataValueNode( "NMR" ));        
                looprownode_chars.addElement(new DataValueNode( "." ));        
//                looprownode_chars.addElement(new DataValueNode( "." ));        
//                looprownode_chars.addElement(new DataValueNode( "1" ));        
                break;
            }
            default: {
                General.showError("code bug addStudy Unknown nmr-star format: " + star_version );
            }                            
        }                                                        
        dataloopnode_chars.getVals().addElement( looprownode_chars );
        return true;
    }

    public boolean addRelatedEntry( int star_version, String pdb_id ) {
        LoopRowNode looprownode_chars = new LoopRowNode();
        switch ( star_version ) {
            case NmrStar.STAR_VERSION_3_1: {
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Database_name" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Database_accession_code" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Relationship" ));
//                loopnamelistnode_chars.addElement( new DataNameNode( "_Related_entries.Entry_ID" ));
                looprownode_chars.addElement(new DataValueNode( "PDB" ));        
                looprownode_chars.addElement(new DataValueNode( pdb_id ));        
                looprownode_chars.addElement(new DataValueNode( "Master copy" ));        
//                looprownode_chars.addElement(new DataValueNode( "." ));        
                break;
            }
            default: {
                General.showError("code bug addStudy Unknown nmr-star format: " + star_version );
            }                            
        }                                                        
        dataloopnode_chars.getVals().addElement( looprownode_chars );
        return true;
    }
    
    /** Get the textual representation in STAR. Other representations might be
     * possible in the future.
     * @return The STAR content.
     */    
    public String toString() {
        // Now output the result
        ByteArrayOutputStream os = new ByteArrayOutputStream();        
        StarUnparser myUnparser = new StarUnparser( os );
        myUnparser.setFormatting( true );
        myUnparser.writeOut( star_file_node, 0 );        
        String result = os.toString();
        return result;
    }
    
    /** Parses the output string like
     *...
<PRE>
Reading restraints file: C:\Documents and Settings\jurgen\Desktop\test_data\1brv_dih.txt
input61200.tmp         29    1    0

Parsed number of restraints:29
</PRE>
     *returning the number of parsed restraints, comments and errors.
     *Note that it will take the first such occurance.
     */
    public static int[] getCountsFromString( String txt ) {
        Pattern p = null;
        Matcher m = null;
        // New style using standard Java API
        try {
            // capture the 3 ints
            p = Pattern.compile("input[0-9]+\\.tmp +([0-9]+) +([0-9]+) +([0-9]+)");
            m = p.matcher(txt);                        
        } catch ( PatternSyntaxException e ) {
            General.showThrowable(e);  
            return null;
        }
        
        if ( ! m.find() ) {
            General.showWarning("No match found for string: [" + txt + "]");
            return null;
        }
        int[] results = new int[3];
        for ( int i=1; i<=3; i++) {        
            results[i-1] = (new Integer(m.group(i))).intValue();
        }
        return results;
    }
    
    /** Reads restraints to NMR-STAR format.
     * @param args E.g. "XPLOR/CNS" "distance" "NOE" "simple" filename
     *In the future it might contain more than 1 exit status.
     *Right now the designation is:
     *<PRE>
     *0 0 success 1 failure
     *1 parsed restraint count
     *2 comment count
     *3 errors count
     *</PRE>
     */
    public int[] read(String args[]) {
        //int star_version = NmrStar.STAR_VERSION_2_1_1;
//        int star_version = NmrStar.STAR_VERSION_3_0;
        int star_version = NmrStar.STAR_VERSION_3_1;

        String program  = args[0];
        String type     = args[1];
        String subtype  = args[2];
        String format   = args[3];
        String pdb_id   = args[4];
        String fi       = args[5];
        String[] typeList = { program, type, subtype, format };

        int[] status = new int[4];
        status[0] = 1;
        status[1] = Defs.NULL_INT;
        status[2] = Defs.NULL_INT;
        status[3] = Defs.NULL_INT;
        
        Classification classi = new Classification();
        String location = "Data/classification.csv";            
        classi.readFromCsvFile( location );        
        DBMRFile mrf = new DBMRFile();
        mrf.pdb_id = pdb_id;
        DBMRBlock mrb = new DBMRBlock();

        mrb.setType( typeList );
        mrb.fileName = InOut.getFileName(fi); // strip off the path part just keeping base name and extension.
        General.showOutput("Reading restraints file: " + fi);
        ArrayList lines = InOut.getLineList(fi,-1,-1);
        if ( lines == null || (lines.size()==0)) {
            General.showError("Failed to read text from file.");
            return status;
        }
        mrb.fillContent(lines);     
        //General.showDebug("lines: " + Strings.toString( lines));
        mrf.blocks.add( mrb );

        
        if ( ! init( mrf, classi, DBMRFile.BLOCK_ID_INDICATING_ALL, star_version )) {
        	String fname = "not set";
        	if ( mrf != null && mrf.file != null ) {
        		fname = mrf.file.toString();
        	}
            General.showError("Failed to read STAR text from file: " + fname);
            return status;            
        }
        status[0] = 0; // success.
        status[1] = getItemCount(); // restraint count.
        return status;
    }    
    
    
    /** Writes restraints to NMR-STAR format.
     */
    public boolean write(String fo) {
        General.showOutput("Writing NMR-STAR file: " + fo);
        if ( ! InOut.writeTextToFile(new File(fo),toString(),true,false)) {
            General.showError("Failed to write STAR text to file.");
            return false;            
        }      
        return true;
    }    
    /**
     * @param args the input file name
     *java Wattos.Episode_II.MRSTARFile input.str
     */
    public static void main(String[] args) {
        General.setVerbosityToDebug();
        General.showOutput("counted items: " + getItemCount(args[0]));
    }
}
