/*
 * BlastMatch.java
 *
 * Created on December 10, 2002, 12:59 PM
 */

package Wattos.Gobbler;

import java.util.*;
import java.io.*;

import Wattos.Common.*;

/** Contains exactly one match between two orfs. The information in here is aimed
 * to capture enough information for both:
<PRE>
-1- Update the BMRB entries
-2- Use for scoring targets for the CESG project.

The class is serializable so it can easily be stored in a small binary format on disk.
The sequence info itself is optionally saved in this class see documentation below.

Example of the info as it resides in the Blast output:

Query= BSGCAIR30310|2002-07-24
         (297 letters)

>ref|NP_125847.1| ABC transporter, ATP-binding protein [Pyrococcus abyssi]
 pir||G75203 abc transporter, ATP-binding protein PAB0103 - Pyrococcus abyssi
           (strain Orsay)
 emb|CAB49078.1| ABC transporter, ATP-binding protein, substrate unknown [Pyrococcus
           abyssi]
          Length = 344

 Score =  248 bits (633), Expect = 1e-64
 Identities = 140/331 (42%), Positives = 206/331 (61%), Gaps = 42/331 (12%)

Query: 1   MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN 59
           ML+V ++SK +K+FKL+++SF++ +E + +ILGPSGAGK+VL++ IAGI++PD GRIILN
Sbjct: 1   MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN 60

Query: 60  GEDITNLPPEKRNVGYVPQNYALFPNKNVYKNIAYGLIIKKVNKLEIDRKVKEIAEFLNI 119
           G D+T+ PPEKRN+ Y+PQ+YALFP+  VY NIA+GL ++++++ EIDRKVKEI++ L I
Sbjct: 61  GVDVTSYPPEKRNLAYIPQDYALFPHMTVYDNIAFGLKLRRISRQEIDRKVKEISKVLGI 120

Query: 120 SHLLNRDVKTLSGGEQQRVALARALILNPSILLLDEPTSAVDXXXXXXXXXXX---XXXX 176
            HLL+R  +TLSGGE+QRVA+ARAL++ P +LLLDEP + +D                  
Sbjct: 121 EHLLHRKPRTLSGGEKQRVAIARALVIEPELLLLDEPFANLDVQTKSRFMTEMKVWRKEL 180

Query: 177 HIPVLHITHDLAEARTLGEKVGIFMNGELIAFGD-KSILKKPKNKKVAEFLGF-NIIDDK 234
               LH+TH   EA +LG++VG+ + G L+  GD K +   P ++ VA FLGF NII+  
Sbjct: 181 GFTSLHVTHSFEEAISLGDRVGVMLRGRLVQVGDVKEVFSNPVDEGVARFLGFENIIEGV 240

Query: 235 A-------------------------IAPEDVII---------KDGNGGEVVNIIDYGKY 260
           A                         + PED+I+         ++    EV+ I + G  
Sbjct: 241 AKGNILEANGVKITLPISVEGKVRIGVRPEDIILSTEPVKTSARNEFRAEVIGIEELGPL 300

Query: 261 KKVFVKYNGYIIKAFTERD--LNIGDNVGLE 289
            +V +K  G  +KAF  R   + +G + G E
Sbjct: 301 VRVNLKIGGITLKAFITRSSLIELGISEGRE 331
</PRE>

 * @author Jurgen F. Doreleijers
 * @version 1
 *@see Wattos.Gobbler
 */
public class BlastMatch implements Cloneable, Serializable, Comparable {

    /** Files should be named like: xxx.blastout */
    public static String EXTENSION_BLAST_OUTPUT_TXT = "blast";
    /** Files should be named like: xxx.bin */
    public static String EXTENSION_BLAST_OUTPUT_BIN = "bin";
    /** How many attributes need to be serialized and written to csv file. This is
     *the number of non-static attributes excluding the list as it will be dealt
     *with separately.
     */
    public static final int COLUMN_COUNT = 15;

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    
    /** The aligned sequence of the query. This is an optional field which might be
     *set to null to indicate it's absence. 
     *It has "-" for inserted gaps and X for "Low complexity" residues.
     *e.g.: 
MLKVNNLSKIWKDFKLKNVSFEIDRE-YCVILGPSGAGKSVLIKCIAGILKPDSGRIILN
GEDITNLPPEKRNVGYVPQNYALFPNKNVYKNIAYGLIIKKVNKLEIDRKVKEIAEFLNI
SHLLNRDVKTLSGGEQQRVALARALILNPSILLLDEPTSAVDXXXXXXXXXXX---XXXX
HIPVLHITHDLAEARTLGEKVGIFMNGELIAFGD-KSILKKPKNKKVAEFLGF-NIIDDK
A-------------------------IAPEDVII---------KDGNGGEVVNIIDYGKY
KKVFVKYNGYIIKAFTERD--LNIGDNVGLE
     */
    public String query_orf_sequence;    

    /** Amino acid number where the match started, e.g. 1. 
     *Not filled at the moment?
     */
    public int query_orf_match_start;                  

    /** A list of instances in class: Wattos.Common.OrfId for which the exact same sequence
     *occurs. Needs at least 1 match. First 1 is the primary.
     */
    public OrfIdList subject_orf_id_list = new OrfIdList();

    /** The aligned sequence of the subject. This is an optional field which might be
     *set to null to indicate it's absence. 
     *It does not have "-" for inserted gaps and also no X for "Low complexity" residues.
     *e.g.:
MLRVESVSKDYKEFKLRDISFDVKKEEHFIILGPSGAGKTVLLEIIAGIIEPDEGRIILN
GVDVTSYPPEKRNLAYIPQDYALFPHMTVYDNIAFGLKLRRISRQEIDRKVKEISKVLGI
EHLLHRKPRTLSGGEKQRVAIARALVIEPELLLLDEPFANLDVQTKSRFMTEMKVWRKEL
GFTSLHVTHSFEEAISLGDRVGVMLRGRLVQVGDVKEVFSNPVDEGVARFLGFENIIEGV
AKGNILEANGVKITLPISVEGKVRIGVRPEDIILSTEPVKTSARNEFRAEVIGIEELGPL
VRVNLKIGGITLKAFITRSSLIELGISEGRE
     */
    public String subject_orf_sequence;    

    /** Amino acid sequence length of subject e.g. 344
     */
    public int subject_orf_length;

    /** Amino acid number where the match started, e.g. 1
     */
    public int subject_orf_match_start;            

    /** Score in bits of the sequence. It is a partially normalized score one still needs
     *to normalize by the sequence length. Bit scores are related to the E value by a simple
     *formula. It is documented at:
     *http://www.ncbi.nlm.nih.gov/BLAST/tutorial/Altschul-1.html#head3
     *e.g. 248 bits
     */
    public float bit_score;

    /** Maximum score obtainable in bits used for normalizing the score obtained e.g. 331.
     */
    public int bit_score_maximum;

    /** The expectation value; a measure of how unusual the match is e.g. 1e-64
     *For e-104 one needs a double.
     */
    public double expectation_value;

    /** The size of the match which is usually smaller than the query and the subject but
     * can be bigger due to gaps. e.g. 331
     */
    public int match_size;

    /** The number of identities over the matched sequence e.g. 140
     */
    public int number_identities;

    /** The number of residue that are of comparable type; like Leucine and Isoleucine. This 
     *includes the number of identities, e.g. 206
     */
    public int number_positives;

    /** The number of residues that needed to be inserted into the query in order to come
     up with the total residues in the match, e.g. 42 (The number of dashes in the
     *query as shown in the alignment.
     */
    public int number_gaps;    
    
    /** for nucleotide matches
     */
    public boolean query_is_positive_strand;
    
    /** for nucleotide matches
     */
    public boolean subject_is_positive_strand;
    
    /** Creates new BlastMatch */
    public BlastMatch() {
    }

    
    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
    }
    
    
    /** It could be optimized for the number of objects created.
     */
    public boolean toStringArray(String[] a, int i, int query_orf_length ) {
        
        a[ i++] = String.valueOf(         bit_score);
        a[ i++] = String.valueOf(         bit_score_maximum);
        a[ i++] = String.valueOf(         expectation_value);
        a[ i++] = String.valueOf(         match_size);
        a[ i++] = String.valueOf(         number_gaps);
        a[ i++] = String.valueOf(         number_identities);
        a[ i++] = String.valueOf(         number_positives);
        a[ i++] = String.valueOf(         query_orf_length); // newly added because is in fact blast match list attribute
        a[ i++] = String.valueOf(         query_orf_match_start);
        a[ i++] =                         query_orf_sequence;
        a[ i++] = String.valueOf(         query_is_positive_strand);
        a[ i++] = String.valueOf(         subject_orf_length);
        a[ i++] = String.valueOf(         subject_orf_match_start);
        a[ i++] =                         subject_orf_sequence;
        a[ i++] = String.valueOf(         subject_is_positive_strand);
        for (Iterator it=subject_orf_id_list.orfIdList.iterator();it.hasNext();) {
            OrfId orfId = (OrfId) it.next();
            orfId.toStringArray( a, i );
            i += OrfId.COLUMN_COUNT;
        }
        
        return true;
    }
    
    public boolean toBmrbStyle() {
        subject_orf_id_list.toBmrbStyle();
        return true;
    }    

    /** First attempt at writing some cloning method. Taken from:
     * "The Java Programming Launguage, Third Edition" by Arnold, Gosling,
     * and Holmes.
     * @return  cloned object */
    public Object clone() {
        try {
            // This call has to be first command
            BlastMatch nObj = (BlastMatch) super.clone();
            nObj.query_orf_sequence         = query_orf_sequence;
            nObj.query_is_positive_strand   = query_is_positive_strand;
            nObj.query_orf_match_start      = query_orf_match_start;
            nObj.subject_orf_id_list        = (OrfIdList) subject_orf_id_list.clone();
            nObj.subject_orf_sequence       = subject_orf_sequence;
            nObj.subject_is_positive_strand = subject_is_positive_strand;
            nObj.subject_orf_length         = subject_orf_length;
            nObj.subject_orf_match_start    = subject_orf_match_start;
            nObj.bit_score                  = bit_score;
            nObj.bit_score_maximum          = bit_score_maximum;
            nObj.expectation_value          = expectation_value;
            nObj.match_size                 = match_size;
            nObj.number_identities          = number_identities;
            nObj.number_positives           = number_positives;
            nObj.number_gaps                = number_gaps;
            return nObj;
        } catch (CloneNotSupportedException e) {
            // Cannot happen -- we support clone, and so do Attributes
            throw new InternalError(e.toString());
        }
    }

    /** To sort results for BMRB in a really funky way
     */
    public int compareTo(Object obj) {
        
        BlastMatch other = (BlastMatch) obj;
        
        // Ignore other orf ids than first.
        OrfId orfId       = (OrfId)       subject_orf_id_list.orfIdList.get(0);
        OrfId other_orfId = (OrfId) other.subject_orf_id_list.orfIdList.get(0);
        //General.showDebug("comparing: " + orfId.toString() + " to: " + other_orfId.toString() );

        // Put BMRB matches first (return -1 for them)
        if ( orfId.orf_db_name.equalsIgnoreCase("bmrb") || other_orfId.orf_db_name.equalsIgnoreCase("bmrb") ) { 
            if ( ! orfId.orf_db_name.equalsIgnoreCase( other_orfId.orf_db_name )) {
                //General.showDebug("sorting by difference in being BMRB.");
                if ( orfId.orf_db_name.equalsIgnoreCase("bmrb") ) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }

        // Put PDB matches first (return -1 for them)
        if ( orfId.orf_db_name.equalsIgnoreCase("pdb") || other_orfId.orf_db_name.equalsIgnoreCase("pdb") ) { 
            if ( ! orfId.orf_db_name.equalsIgnoreCase( other_orfId.orf_db_name )) {
                //General.showDebug("sorting by difference in being pdb.");
                if ( orfId.orf_db_name.equalsIgnoreCase("pdb") ) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }
                
                
        // sort by db name       
        int status = orfId.orf_db_name.compareTo( other_orfId.orf_db_name );
        if ( status != 0 ) {
            //General.showDebug("sorting by difference in db name.");
            return status;
        }

        // sort by sequence query to submitted_percentage
        // Implemented by comparing the subject orf length assuming the matches are from
        // the same blast match list the query length (property of blast match list) are
        // all the same.
        if ( subject_orf_length != other.subject_orf_length ) {
            if ( subject_orf_length < other.subject_orf_length ) {
                return -1;
            } else {
                return 1;
            }
        }

        // sort by rounded sequence identity percentage
        if ((match_size != 0 ) &&
            (other.match_size != 0 ) ){
            int id        = (int) ((100.0f*number_identities)/match_size);
            int other_id  = (int) ((100.0f*other.number_identities)/other.match_size);        
            //General.showDebug("checking sequence identity percentage.");
            //General.showDebug("id: " + id + " and other id: " + other_id);
            if ( id < other_id ) {
                return 1;
            }
            if ( id > other_id ) {
                return -1;
            }
        }
        
        // sort by db id
        status = orfId.orf_db_id.compareTo( other_orfId.orf_db_id );
        if ( status != 0 ) {
            //General.showDebug("sorting by difference in db name.");
            return status;
        }

        // sort by molecule name
        status = orfId.molecule_name.compareTo( other_orfId.molecule_name );
        if ( status != 0 ) {
            //General.showDebug("sorting by difference in molecule name.");
            return status;
        }

        // Room for additional criteria .e.g strandness in case of nucleic acid sequences
        return 0;
    }    
}

