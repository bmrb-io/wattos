/*
 * FastResultList.java
 * Created on January 26, 2005, 11:18 AM
 */

package Wattos.Common;

import Wattos.Utils.*;
import java.io.*;
import java.util.*;

/**
 *A data structure that is a fasta list plus a list of added strings per sequence.
 * @see <a href="FastrSpecs.html">Data on a sequence from a fasta file following the definitions</a>
 * @see FastResult
 * @author Jurgen F. Doreleijers
 */
public class FastResultList extends ArrayList {
    
    private static final long serialVersionUID = 5938860206303971655L;

    /** Creates a new instance of FastResult */
    public FastResultList() {
        super();
    }
        

    /**
     * Read the file into memory after init.
     * @param input_filename
     * 
     */
    public boolean readFastrFile( String input_filename ) {
        clear();
        int count_read=0;        
        try {
            FileReader fr = new FileReader( input_filename );
            LineNumberReader inputReader = new LineNumberReader( fr );
            if ( inputReader == null ) {
                General.showError("initializing LineNumberReader");
                return false;
            }
            if ( ! inputReader.ready() ) {
                General.showDebug("input not ready or just empty..");
                return false; 
            }
            
            String line = inputReader.readLine();
            while ( line != null ) {
                //General.showDebug("Processing line number: " + (inputReader.getLineNumber()-1));            
                /**Does the line start a second new query output? */                
                if ( ( line.length() == 0 ) ||
                     ( line.charAt(0) != FastaDefinitions.FASTA_START_CHAR )) {
                    line = inputReader.readLine();
                    continue;
                }
                count_read++;
		FastResult fastr = new FastResult();
                if ( ! fastr.parse( line ) ) {
                    General.showError("Failed to read fastr record from line: [" + line + "]");
                    return false;
                }
                add( fastr );
                //OrfId oi = (OrfId) fastr.oiList.orfIdList.get(0);                
                //General.showOutput("fastr: [" + oi.orf_db_name + "]");
                line = inputReader.readLine();
            }
            inputReader.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Read records: " + count_read);
        FastResult fr = (FastResult) get(0);
        int sizeResult = fr.result.size();        
        General.showOutput("First record contained number of results: " + sizeResult);
        
        return true;        
    }
    
    /**
     *
     * @param output_filename
     */    
    public boolean writeFastrFile( String output_filename ) {
        int count_written=0;
        try {
            BufferedWriter bw = new BufferedWriter( new FileWriter( output_filename));
            PrintWriter outputWriter = new PrintWriter( bw );
            if ( outputWriter == null ) {
                General.showError("initializing PrintWriter");
                return false;
            }
            if ( outputWriter.checkError() ) {
                General.showError("output got an error before starting.");
                return false; 
            }

            for (int i=0;i<size();i++) {
                //General.showDebug("Processing fast result number: " + 1);            
                /**Does the line start a second new query output? */   
                FastResult fastr = (FastResult) get(i);
                outputWriter.write( fastr.toFastr());
                count_written++;
            }
            outputWriter.close();
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }      
        General.showOutput("Wrote records                             : " + count_written);
        if ( size() >0 ) {
            FastResult fr = (FastResult) get(0);
            int sizeResult = fr.result.size();        
            General.showOutput("First record contained number of results  : " + sizeResult);
        }
        return true;        
    }

    /** Splits input on ',' and converts to integer values.
     *If first element returned is -1 then all columns are to be kept.
     *Returns null on error.
     */
    public int[] parseColumnsToKeep( String columnsToKeep ) {
        if ( columnsToKeep.equals(".")) {
            int[] map = new int[1];
            map[0]=-1; // denotes no map.
            return map;
        }
        if ( columnsToKeep.equals("-2")) {
            int[] map = new int[1];
            map[0]=-2; // denotes none to keep.
            return map;
        }
        int[] map = Strings.splitWithAllReturnedIntegers(columnsToKeep, ',');
        if ( map == null ) {
            General.showError("Failed to parse string into comma seperated int values" );
            General.showError("Input: ["+columnsToKeep+"]");
            return null;
        }
        Arrays.sort( map );
        FastResult fr = (FastResult) get(0);
        int sizeResult = fr.result.size();
        for (int i=0;i<map.length;i++ ) {
            map[i]--;  // switch from human to computer numbering.
            if ( map[i] < -2 ) {
                General.showError("Can't keep a column with a number smaller than -2  : " + ++map[i]);
                General.showError("Number of columns in first record of file          : " + sizeResult);
                return null;
            }
            if ( map[i] >= sizeResult ) {
                General.showError("Can't keep a column with a number larger than the number of columns present: " + ++map[i]);
                General.showError("Number of columns in first record of file          : " + sizeResult);
                return null;
            }
        } 
        return map;
    }
    
    /** Only keeps those columns listed by the argument.
     *routine assumes that the column ids are sorted in ascending order and all are valid.
     *if the first element in the map parameter is negative one all columns will
     *be maintained. If it is -2 then none will be kept.
     */
    public boolean keepColumns(int[] map) {
        if ( map[0]==-1 ) {
            return true;
        }
        if ( map[0]==-2 ) {
            for (int i=0;i<size();i++) {
                FastResult fr = (FastResult) get(i);
		fr.result.clear();
            }
            return true;
        }
        int idxMap = map.length-1;
        FastResult fr = (FastResult) get(0);
        int sizeResult = fr.result.size();        
        General.showDebug("First record contained number of results: " + sizeResult);
        
        for (int idxCurrent=sizeResult-1;idxCurrent>=0;idxCurrent-- ) {
            if ( (idxMap>=0) && (map[idxMap] == idxCurrent) ) {
                // Lets keep the column
                idxMap--;
            } else {
                // Remove the column
                removeRecordColumn(idxCurrent);
            }
        }
        return true;
    }
    
    /** From all records remove the column given
     */
    public boolean removeRecordColumn(int index) {
        General.showDebug("Removing column from all records: " + index);
        int size = size();    
        for (int i=0;i<size;i++) {
            FastResult fr = (FastResult) get(i);
            fr.result.remove(index);
        }
        return true;
    }

    /** From all records remove the column given
     */
    public boolean addRecordColumnSet(int count, String value, int position) {
        General.showDebug("Adding count of columns to all records: " + count);
        int size = size();    
        for (int i=0;i<size;i++) {
            FastResult fr = (FastResult) get(i);
            for (int c=0;c<count;c++) {
                fr.result.add(position,value);
            }
        }
        return true;
    }
    
    /**
     * Note that after this routine other has been mutilated and shouldn't be
     * used. If
     * @param other
     * @param skipNoMatch If set to true the results from the other list will not be added if there is no match.
     */
    public boolean combineResultsById( FastResultList other, boolean skipNoMatch ) {
        FastResult fr_this_first  = (FastResult) get(0);
        FastResult fr_other_firsr = (FastResult) other.get(0);
        int thisResultListSize = fr_this_first.result.size();
        int otherResultListSize = fr_other_firsr.result.size();
        int combinedResultListSize = thisResultListSize + otherResultListSize;        
        General.showDebug("Combining counts of " + thisResultListSize + " and " +
            otherResultListSize + " to " + combinedResultListSize);
        
        // Anything needed to be done?
        if ( otherResultListSize == 0 ) {
            return true;
        }
        
        OrfId oi_this  = (OrfId) fr_this_first.oiList.orfIdList.get(0);
        OrfId oi_other = (OrfId) fr_this_first.oiList.orfIdList.get(0);
        if ( ! oi_this.orf_db_name.equals(oi_other.orf_db_name) ) {
            General.showError("Can't combine results for lists where the first records don't have the same orf db names");
            General.showError("file 1 record 1 orf db name:" + oi_this.orf_db_name);
            General.showError("file 2 record 1 orf db name:" + oi_other.orf_db_name);
            return false;
        }
        /** Maps the id to the location in the array */
        HashMap map_id      = createHashMap( true,   0);
//        HashMap map_sub_id  = createHashMap( false,  0);
        //General.showDebug("Map of ids: " + Strings.toString( map_id ));
        //General.showDebug("Map of subids: " + Strings.toString( map_sub_id ));
        // Increase the width of the results already
        int count = otherResultListSize;
        String value = "";
        int position = thisResultListSize;
        addRecordColumnSet( count, value, position );
        
        // Add the info from other to this even if this didn't have any to
        // begin with.
        General.showDebug("Trying to match the orfs");
        for (int otherIdx=0;otherIdx<other.size();otherIdx++) {
            FastResult otherFr = (FastResult) other.get(otherIdx);
            OrfId otherOi = (OrfId) otherFr.oiList.orfIdList.get(0);
            Integer idxThisDbId    = (Integer) map_id.get( otherOi.orf_db_id );
            Integer idxThisDbSubId = (Integer) map_id.get( otherOi.orf_db_subid );
            // Make it a match if no sub id is given in other.
            if ( (otherOi.orf_db_subid == null ) || ( otherOi.orf_db_subid.equals(""))) {
                idxThisDbSubId = idxThisDbId;
            }
            //General.showDebug("Trying to look up the other orf:" + otherOi);
            if ( (idxThisDbId==null) || (idxThisDbSubId==null) || (idxThisDbId!=idxThisDbSubId)) {
                // It's not in this so prepare a new entry
                //General.showDebug("No match");
                if ( ! skipNoMatch ) {
                    add(otherFr);
                    for (int i=0;i<thisResultListSize;i++) {
                        otherFr.result.add(0, ""); 
                    }
                }
            } else {
                // It's already in this
                FastResult thisFr = (FastResult) get(idxThisDbId.intValue());
                //General.showDebug("Matched with: " + thisFr.oiList.orfIdList.get(0));
                int j=thisResultListSize;
                for (int i=0;i<otherResultListSize;i++) {
                    thisFr.result.set(j, otherFr.result.get(i));
                    j++;
                }              
            }
        }
        other.clear();
        
        return true;
    }
    
    /** Empty elements will not be mapped. 
     *If useId is true the orf_db_id's will be hashed otherwise
     *the orf_db_subid's will be hashed.
     *Make sure that if orf_id_index is set to non-zero that
     *there are non-zero elements in the orf_id_list!
     */
    private HashMap createHashMap( boolean useId, int orf_id_index ) {
        HashMap hm = new HashMap( size() );
        int size = size();    
        for (int i=0;i<size;i++) {
            FastResult fr = (FastResult) get(i);
            if ( orf_id_index >= fr.oiList.orfIdList.size() ) {
                General.showError("orf_id_index used             : " + orf_id_index);
                General.showError("but fr.oiList.orfIdList.size(): " + fr.oiList.orfIdList.size());
                return null;
            }
            OrfId oi = (OrfId) fr.oiList.orfIdList.get(orf_id_index);
            String id = null;
            if ( useId ) {
                id = oi.orf_db_id;
            } else {
                id = oi.orf_db_subid;
            }            
            if ( !(( id == null ) || id.equals(""))) {
                hm.put( id, new Integer(i));
            }
        }
        return hm;
    }
    
    /** After this function the argument is worthless; actually nilled
     */
    public boolean replaceOrfIdFromFastaMapping( FastaList other ) {
        // Get the map from the first orf id to the position based only
        // on the orf_db_id.
        HashMap hmOther = other.createHashMap( true, 0);
        //General.showOutput( "FastaList other hm: " + Strings.toString( hmOther ));
        int countMatched = 0;
        int countSkipped = 0;
        for (int i=0;i<size();i++) {
            FastResult fr = (FastResult) get(i);
            OrfId oi = (OrfId) fr.oiList.orfIdList.get(0);
            String id = oi.orf_db_id;
            if ( !(( id == null ) || id.equals("") || (!hmOther.containsKey(id)))) {
                int idx = ((Integer)hmOther.get(id)).intValue();
                Orf orfOther = (Orf) other.get(idx);
                OrfId orfIdOther = (OrfId) orfOther.orf_id_list.orfIdList.get(1);
                fr.oiList.orfIdList.clear();
                fr.oiList.orfIdList.add( orfIdOther );
                countMatched++;
            } else {
                countSkipped++;
                remove(i);
                i--; // do it again sam.
            }
        }
        other.clear(); 
        General.showOutput("Total of records in fastr file : " + (countMatched+countSkipped));
        General.showOutput("Records skipped in fastr file  : " + countSkipped);
        General.showOutput("Records matched with fasta file: " + countMatched);
        return true; 
    }
    

    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Common.FastResultList -skipNoMatch file_1_name file_2_name file_out_name");
        General.showOutput("       -skipNoMatch   :   skip results from second list if there's no match on id with the first.");
        General.showOutput("       file_1_name    :   name of the first file to read.");
        General.showOutput("       columnsToKeep_1:   comma separated list of columns to keep from first file. Use -2 to keep none.");
        General.showOutput("       file_2_name    :   name of the second file to read or . for none.");
        General.showOutput("       columnsToKeep_2:   comma separated list of columns to keep from second file.");
        General.showOutput("       file_out_name  :   name of the file to write.");
        General.showOutput("NOTE: give '.' for all columns to keep.");        
        General.showOutput("      First column is numbered 1.");        
        System.exit(1);
    }
    
    /**
     * Main of class can be used to combine two fastr files. The resulting file will have
     * a fixed number of columns for the result section which is equal to the sum of the number of
     * columns in the first record of each file.
     * The results will be matched on the basis of the first orf id comparing only the fields:
     * orf_db_id and orf_db_sub_id assuming that the orf_db_names are already the same. 
     * The code will check that the the orf_db_names are the same for the first record.
     * If the second filename is given as a . then a selection on the first will be made.
     * <BR>
     * Notes:<BR>
     * <PRE>
     * This works well for under a million records. E.g.:
     * -rw-r--r--    1 jurgen   None     20,724,080 Jan 27 17:01 u.fastr
     * S:\jurgen\BioInf\tmp_unb_>java -Xmx256m Wattos.Common.FastResultList u.fastr d.fastr
     * Starting
     * Read records: 279580
     * Wrote records: 279580
     * Done
     * </PRE>
     * @param args Two file names with qualifying paths.
     *
USAGE: java -Xmx256m Wattos.Common.FastResultList [-skipNoMatch] file_1_name file_2_name file_out_name
       -skipNoMatch   :   skip results from second list if there's no match on id with the first.
       file_1_name    :   name of the first file to read.
       columnsToKeep_1:   comma separated list of columns to keep from first file. Use -2 to keep none..
       file_2_name    :   name of the second file to read or . for none.
       columnsToKeep_2:   comma separated list of columns to keep from second file.
       file_out_name  :   name of the file to write.
NOTE: give '.' for all columns to keep.        
      First column is numbered 1.        
     */
    public static void main(String[] args) {
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;
        boolean skipNoMatch = false;
        int i = 0;
        if ( args[0].equals("-skipNoMatch") ) {
            skipNoMatch = true;
            i++;
            /** Checks of input */
            if ( args.length != 6 ) {
                General.showError("Need 6 arguments.");
                show_usage();
            }        
        } else {
            /** Checks of input */
            if ( args.length != 5 ) {
                General.showError("Need 5 arguments.");
                show_usage();
            }                    
        }

        String list_1_name      = args[i++];
        String columnsToKeep_1  = args[i++];
        String list_2_name      = args[i++];
        String columnsToKeep_2  = args[i++];
        String list_o_name      = args[i++];
        FastResultList frl_1 = new FastResultList();
        FastResultList frl_2 = new FastResultList();
        
        General.showOutput("Reading file: " + list_1_name );
        if ( ! frl_1.readFastrFile(  list_1_name )) {
            General.doCodeBugExit("Failed to read first fastr file: " + list_1_name);
        }
        int[] map_1 = frl_1.parseColumnsToKeep(columnsToKeep_1);
        if ( map_1 == null ) {
            General.doCodeBugExit("Failed to get mapping for columns to keep from first file from string: [" + columnsToKeep_1 + "]");
        }
        General.showDebug("Removing any unwanted columns from file");
        frl_1.keepColumns(map_1);

        
        if ( ! list_2_name.equals(".")) {
            General.showOutput("Reading file: " + list_2_name );
            if ( ! frl_2.readFastrFile(  list_2_name )) {
                General.doCodeBugExit("Failed to read second fastr file: " + list_2_name);
            }
            int[] map_2 = frl_2.parseColumnsToKeep(columnsToKeep_2);
            if ( map_2 == null ) {
                General.doCodeBugExit("Failed to get mapping for columns to keep from second file from string: [" + columnsToKeep_1 + "]");
            }
            General.showDebug("Removing any unwanted columns from file");
            frl_2.keepColumns(map_2);

            General.showDebug("Copying the wanted columns from second to first.");
            if ( ! frl_1.combineResultsById( frl_2, skipNoMatch )) {
                General.doCodeBugExit("Failed to combineResultsById");
            }
            // Allow gc on second list before write.
            frl_2.clear();
        } else {
            General.showOutput("Skipping read of second file because given as ." );
        }
         
        General.showOutput("Writing file: " + list_o_name );
        frl_1.writeFastrFile( list_o_name );
    }
}
