/*
 * BlastMatchLoL.java
 *
 * Created on December 12, 2002, 10:45 AM
 */

package Wattos.Gobbler;

import java.util.*;
import java.io.*;  
import com.Ostermiller.util.CSVPrinter;
import Wattos.Common.*;
import Wattos.Utils.*;

/**
 *A list of BlastMatch objects.
 * @author Jurgen F. Doreleijers
 * @version 1
 *@see BlastMatch
 *@see Wattos.Gobbler
 */
public class BlastMatchList implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    
//    private static boolean debug = false;
    
    /** A list of instances in class: Wattos.Common.OrfId for which the exact same sequence
     *occurs. Needs at least 1 match. First 1 is the primary. E.g. an AT number from
     Tigr coming in through Sesame would have 2
     */
    public OrfIdList query_orf_id_list;
    
    /** Length of the query sequence: // newly introduced!
     *         (297 letters)
     */
    public int query_orf_length;
    
    /** The list contains for 1 query the matches with subjects.
     */
    public ArrayList match_list;
    
    public BlastMatchList() {
        init();
    }

    public void init() {
        query_orf_id_list = new OrfIdList();
        query_orf_length = 0;
        match_list = new ArrayList();
    }
    
    
    public int getMaximumNumberOfSubjectOrfIds() {
        int result = 0;
        //General.showOutput("Number of matches in list: " + match_list.size());
        for (Iterator it=match_list.iterator();it.hasNext();) {
            BlastMatch bm = (BlastMatch) it.next();
            int temp = bm.subject_orf_id_list.orfIdList.size();
            //General.showOutput("Number of orf ids in list of this match: " + temp);
            if ( temp > result ) {
                result = temp;
            }
        }
        return result;
    }
    
    
    public boolean sortByExpectationValue() {
        // Use a fast comparator
        Comparator comparator = new ComparatorBlastMatchEvalue();
        // Sorts on values column only.
        try {
            Collections.sort(match_list, comparator);
        } catch ( ClassCastException e ) {
            General.showThrowable(e);
            return false;
        }
        return true;
    }
    
    public boolean sortByWorkDoneValueStructuralGenomics() {
        // Use a fast comparator
        Comparator comparator = new ComparatorBlastMatchTargetDB();
        // Sorts on values column only.
        try {
            Collections.sort(match_list, comparator);
        } catch ( ClassCastException e ) {
            General.showThrowable(e);
            return false;
        }
        return true;
    }
    
    /** Converts the blast output to csv format.
 * @see Wattos.Gobbler
     */
    public boolean toCsv(CSVPrinter out) {
        
        int max_orf_id_count = query_orf_id_list.orfIdList.size() + getMaximumNumberOfSubjectOrfIds();
        int max_column_count = BlastMatch.COLUMN_COUNT + max_orf_id_count * OrfId.COLUMN_COUNT;
        /**
        General.showOutput("query_orf_id_list.orfIdList.size()  :"+query_orf_id_list.orfIdList.size());
        General.showOutput("getMaximumNumberOfSubjectOrfIds     :"+getMaximumNumberOfSubjectOrfIds());
        General.showOutput("max_column_count                    :"+max_column_count);
        */
        
        String[] a = new String[max_column_count];
        
        int offset = 0;
//        boolean status;
        
        
        int i = 0;
        for (Iterator it=query_orf_id_list.orfIdList.iterator();it.hasNext();) {                
            //General.showOutput("offset 1              :"+offset);
            OrfId orfId = (OrfId) it.next();
            //General.showError( "---In blastmatch_list toCsv "+ i + " orf_db_id: " + orfId.orf_db_id);
            orfId.toStringArray( a, offset );
            offset += OrfId.COLUMN_COUNT;
            i++;
        }
            
        i = 0;
        for (Iterator it=match_list.iterator();it.hasNext();) {
            //General.showOutput("offset 2              :"+offset);
            BlastMatch bm = (BlastMatch) it.next();
            bm.toStringArray( a, offset, query_orf_length  );
            Strings.fillStringNullReferencesWithEmptyString(a);
            /** Always quote an empty token that is the first on the line, as it may be the only thing on the 
             *line.  If it were not quoted in that case, an empty line has no tokens. */
            out.println( a );
            i++; 
            // don't increase the offset here!
        }
        return true;
    }    

    
    public String toString() {
        
        int max_orf_id_count = query_orf_id_list.orfIdList.size() + getMaximumNumberOfSubjectOrfIds();
        int max_column_count = BlastMatch.COLUMN_COUNT + max_orf_id_count * OrfId.COLUMN_COUNT;
        String[] a = new String[max_column_count];
        StringBuffer sb = new StringBuffer();
        
        int offset = 0;
//        boolean status;
        
        
        int i = 0;
        for (Iterator it=query_orf_id_list.orfIdList.iterator();it.hasNext();) {                
            //General.showOutput("offset 1              :"+offset);
            OrfId orfId = (OrfId) it.next();
            //General.showError( "---In blastmatch_list toCsv "+ i + " orf_db_id: " + orfId.orf_db_id);
            orfId.toStringArray( a, offset);
            offset += OrfId.COLUMN_COUNT;
            i++;
        }
            
        i = 0;
        for (Iterator it=match_list.iterator();it.hasNext();) {
            //General.showOutput("offset 2              :"+offset);
            BlastMatch bm = (BlastMatch) it.next();
            bm.toStringArray( a, offset, query_orf_length  );
            Strings.fillStringNullReferencesWithEmptyString(a);
            /** Always quote an empty token that is the first on the line, as it may be the only thing on the 
             *line.  If it were not quoted in that case, an empty line has no tokens. */
            for (int j=0;j<a.length;j++) {
                sb.append( a[j] );
                sb.append( ',' );
            }
            sb.append( "\n\n" );
            i++;
            // don't increase the offset here!
        }
        return sb.toString();
    }    

    

    public boolean toBmrbStyle() {
        boolean status;
        
        for (Iterator it=query_orf_id_list.orfIdList.iterator();it.hasNext();) {            
            OrfId o = (OrfId) it.next();
            o.toBmrbStyle();
       }
        for (Iterator it=match_list.iterator();it.hasNext();) {
            BlastMatch bm = (BlastMatch) it.next();
            status = bm.toBmrbStyle();
            if ( ! status ) {
                General.showError("using blast match toBmrbStyle");
                return false;
            }
            NMRSTAREntry.reformatOrfIdForBMRB( bm.subject_orf_id_list );
            if ( ! status ) {
                General.showError("using reformatOrfIdForBMRB on subject orf id list: " + 
                    bm.subject_orf_id_list.toString());
                return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) throws Exception {

        General.showOutput("Self test");
        BlastMatchList bml = new BlastMatchList();
        bml.query_orf_length = 99;

        BlastMatch bm = new BlastMatch();
        OrfId orfId = new OrfId();
        orfId.orf_db_name = "GenBank";
        orfId.orf_db_id   = "a";
        bm.match_size = 100;
        bm.number_identities = 10;
        bm.subject_orf_id_list.orfIdList.add( orfId );
        bml.match_list.add( bm );
        
        bm = new BlastMatch();
        orfId = new OrfId();
        orfId.orf_db_name = "pdb";
        orfId.orf_db_id   = "b";
        orfId.molecule_name = "XXA";
        bm.match_size = 100;
        bm.number_identities = 10;
        bm.subject_orf_id_list.orfIdList.add( orfId );
        bml.match_list.add( bm );
                
        bm = new BlastMatch();
        orfId = new OrfId();
        orfId.orf_db_name = "pdb";
        orfId.orf_db_id   = "b";
        orfId.molecule_name = "BBB";
        bm.match_size = 100;
        bm.number_identities = 10;
        bm.subject_orf_id_list.orfIdList.add( orfId );
        bml.match_list.add( bm );
                
        if ( true ) {
            General.showOutput("List reads: " + bml.toString() );
            Collections.sort( bml.match_list );
            General.showOutput("List reads: " + bml.toString() );
            /**
            Collections.reverse( bml.match_list );
            General.showOutput("List reads: " + bml.toString() );
             */
        }
        General.showOutput("Done with Self test");
    }
        
}
