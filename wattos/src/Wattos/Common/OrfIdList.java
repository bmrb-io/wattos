/*
 *
 * Created on December 19, 2002, 11:27 AM
 */

package Wattos.Common;

import java.io.*;
import java.util.*;

import Wattos.Utils.*;

/**
 *Open reading frame/protein or nucleic acid sequence and id parameter list.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class OrfIdList implements Cloneable, Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    
    public ArrayList orfIdList = new ArrayList();

    /** Creates new OrfId */
    public OrfIdList() {
    }

    /** First attempt at writing some cloning method. Taken from:
     * "The Java Programming Launguage, Third Edition" by Arnold, Gosling,
     * and Holmes.
     * @return  cloned object */
    public Object clone() {
        try {
            // This call has to be first command
            OrfIdList nObj = (OrfIdList) super.clone();
            return nObj;
        } catch (CloneNotSupportedException e) {
            // Cannot happen -- we support clone, and so do Attributes
            throw new InternalError(e.toString());
        }
    }
    
    public void init() {
        orfIdList.clear();
    }    
    
    public boolean toBmrbStyle() {
        for (Iterator it=orfIdList.iterator();it.hasNext();) {
            OrfId o = (OrfId) it.next();
            o.toBmrbStyle();
        }
        return true;
    }    
        
    public String toFasta() {
        
        StringBuffer sb = new StringBuffer();
        for (int i=0;i<orfIdList.size();i++) {
            //General.showOutput("Now at i: " + i);
            OrfId o = (OrfId) orfIdList.get(i);
            if ( i != 0 ) {
                sb.append( FastaDefinitions.FASTA_DELIM );
            }
            sb.append( o.toFasta() );            
        }
        /** Print at least an empty one 
         */
        if ( orfIdList.size() == 0 ) {
            OrfId o = new OrfId();
            sb.append( o.toFasta() );
        }
        return sb.toString();
    }    
    
    /** Reads the header line of standard fasta file to a list of orf ids.
     *Well documented in file: FastaSpecs.txt
     */
    public boolean readFasta( String line ) {
        init();
        line = line.trim();
        if ( ! ((line.charAt(0) == FastaDefinitions.FASTA_START_CHAR ) &&
                 line.endsWith( FastaDefinitions.FASTA_FORMAT_ID_GENERAL ))) {
            General.showError("Ids didn't start with: [" +
                FastaDefinitions.FASTA_START_CHAR + "]");
            General.showError("or didn't end with   : [" +
                FastaDefinitions.FASTA_FORMAT_ID_GENERAL + "]");
            General.showError("Reading line: [" + line + "]");
            return false;
        }
        // Chop the > off and hash by pipe
        String str_todo = line.substring(1);
        String[] part_list = Strings.splitWithAllReturned( str_todo, FastaDefinitions.FASTA_DELIM );
        
        for (int i=0;(i+3)<part_list.length;i=i+4) {
            //General.showDebug("parsing set: " + i/4 );
            OrfId orf_id = new OrfId();
            orf_id.orf_db_name      = part_list[i];
            orf_id.orf_db_id        = part_list[i+1];
            orf_id.orf_db_subid     = part_list[i+2];
            orf_id.molecule_name    = part_list[i+3];
            orfIdList.add( orf_id );
        }

        int set_count = orfIdList.size();
        //General.showDebug("Found number of sets  : " + set_count);
        //General.showDebug("Found number of tokens: " + token_count);

        if ( set_count == 0 ) {
            General.showError("Found number of sets: " + set_count);
            return false;
        }
            
        return true;
    }
    
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        for (Iterator it=orfIdList.iterator();it.hasNext();) {
            OrfId o = (OrfId) it.next();
            sb.append( "OrfId #: " + i + General.eol);            
            sb.append( o.toString() );
            i++;
        }
        return sb.toString();
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        //String line = ">bmrb|4020|brsv-g||cesg|orfie123|or_5_actually||FASTA_FORMAT_ID_GENERAL";
        String line = ">sesame|GO.1||,At1g75770|FASTA_FORMAT_ID_GENERAL";
        Orf orf = new Orf();
        boolean status = orf.orf_id_list.readFasta( line);
        if ( ! status ) {
            General.showError( "in readFasta.");
        }
        General.showOutput("line reads: [" + line + "]");
        General.showOutput("Orf reads : [" + orf.toFasta() + "]");
        General.showOutput("Done successfully.");
    }
}
