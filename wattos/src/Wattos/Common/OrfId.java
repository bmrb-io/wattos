/*
 * OrfId.java
 *
 * Created on December 19, 2002, 11:27 AM
 */

package Wattos.Common;
 
import java.io.*;
import Wattos.Utils.*;

/**
 *Open reading frame/protein or nucleic acid id parameters.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class OrfId implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    
    /** How many attributes need to be serialized and written to csv file. This is
     *the number of non-static attributes.
     */
    public static final int COLUMN_COUNT = 4;

    public static String[] parts = {
        "orf_db_name",
        "orf_db_id",
        "orf_db_subid",
        "molecule_name"        
    };

    /** Name of the database for the query. E.g. BMRB, pir, gb, ref, etc.
     *A nice overview of databases and what abbreviation they use is
     *available at http://srs.ebi.ac.uk/srs6bin/cgi-bin/wgetz?-page+databanks+-newId
     */
    public String orf_db_name;
    /** Id withing the FIRST database for the subject  e.g. NP_125847.1, 9999.*/
    public String orf_db_id;
    /** To get complete locus in database for a single sequence, e.g.: A or save_polymer_1 */
    public String orf_db_subid;    
    /** Name of the subject molecule e.g. ABC transporter, ATP-binding protein [Pyrococcus abyssi]*/
    public String molecule_name;
    
    /** Creates new OrfId */
    public OrfId() {
        init();
    }

    public void init() {
        orf_db_name     = "";
        orf_db_id       = "";
        orf_db_subid    = "";
        molecule_name   = "";                
    }
    
    /** If the orf_db_id contains a dot. Split the string and overwrite the orf_id with the first
     and the orf_db_sub_id with the second part. If it doesn't contain a dot; do nothing.
     */
    public void splitIdOnDot () {
        int idx = orf_db_id.indexOf('.');
        if ( idx == -1 ) {
            return;
        }
        if ( idx == (orf_db_id.length() - 1)) {
            orf_db_subid = ""; // do the overwrite because there was a dot.
        } else {
            orf_db_subid = orf_db_id.substring(idx+1);
        }
            
        orf_db_id = orf_db_id.substring(0,idx);
    }
    /** Fill (part of) an array with the String values starting at position offset.
     */
    public void toStringArray(String[] a, int i) {
        /**
        General.showOutput("in toStringArray orf_db_name:"+orf_db_name);
        if ( orf_db_name == null ) {
            General.showOutput("it's really a NULL reference");
        }
         */
        a[ i++] = orf_db_name;
        a[ i++] = orf_db_id;
        a[ i++] = orf_db_subid;
        a[ i++] = molecule_name;
    }    

    /** See specs for Fasta above.
     */
    public String toFasta() {
        String result = orf_db_name +       FastaDefinitions.FASTA_DELIM +
                        orf_db_id +         FastaDefinitions.FASTA_DELIM +
                        orf_db_subid +      FastaDefinitions.FASTA_DELIM +
                        molecule_name;
        result = Strings.toWord( result );
        // Just do a simple delete afterwards. It might be faster to do afterwards.
        result = result.replaceAll("null",""); 
        return result;
    }    

    public String toString() {
        String result = 
        "orf_db_name    : " + orf_db_name +     General.eol +
        "orf_db_id      : " + orf_db_id +       General.eol +
        "orf_db_subid   : " + orf_db_subid +    General.eol +
        "molecule_name  : " + molecule_name +   General.eol;
        return result;
    }    

    public void toBmrbStyle() {
        orf_db_name = DatabaseDefinitions.getBmrbFromNrDb( orf_db_name );
    }    
    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        if ( true ) {
            OrfId o = new OrfId();
            o.orf_db_name   = "my db";
            o.orf_db_id     = "12.34";
            o.orf_db_subid  = "A";
            o.molecule_name = "my little peppie";
            o.splitIdOnDot();
            String orf_in_fasta = o.toFasta();
            General.showOutput("orf_in_fasta: [" + orf_in_fasta + "]");
        }
        General.showOutput("Done with all tests in OrfId");        
    }     
}
