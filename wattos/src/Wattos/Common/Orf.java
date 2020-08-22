/*
 * Orf.java
 *
 * Created on December 10, 2002, 4:48 PM
 */

package Wattos.Common;
import java.io.*;

import Wattos.Utils.*;
/**
 *Open reading frame/protein or nucleic acid sequence and id parameters.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Orf  implements Serializable {

    /** Faking this variable makes the serializing not worry
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;

    public OrfIdList orf_id_list = new OrfIdList();

    /** Sequence should not contain invalid characters like end of lines.
     */
    public String sequence = "";

    /** For speed:
    private static BlastDefinitions blastdefinitions = new BlastDefinitions();
    private static FastaDefinitions fastadefinitions = new FastaDefinitions();
     */

    /** Creates new Orf */
    public Orf() {
    }


    public void init() {
        orf_id_list.init();
        sequence = "";
    }

    public String toFasta() {
        StringBuffer sb = new StringBuffer();
        sb.append( FastaDefinitions.FASTA_START_CHAR );
        sb.append( FastaDefinitions.replaceInvalidCharsFasta( orf_id_list.toFasta() ) );
        sb.append( FastaDefinitions.FASTA_DELIM + FastaDefinitions.FASTA_FORMAT_ID_GENERAL );
        sb.append( Strings.EOL );
        sb.append( Strings.wrapToMarginSimple( sequence, 80 ) );
        return sb.toString();
    }


    public boolean hasValidSequenceForBlastDb() {
        return BlastDefinitions.hasValidSequenceForBlastDb( sequence, true);
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
        General.verbosity = General.verbosityDebug;
        Orf orf = new Orf();
        orf.sequence = "ACCA";
        OrfId orf_id = new OrfId();
        orf.orf_id_list.orfIdList.add( orf_id );
        orf_id.orf_db_name  = "sesame";
        orf_id.orf_db_id    = "123";
        orf_id.orf_db_subid = "abc";
        orf_id.molecule_name= "test";

        General.showOutput("Orf is:\n" + orf.toFasta() );
//        if ( false ) {
//            General.showOutput("Orf sequence is valid for blast: " + orf.hasValidSequenceForBlastDb() );
//        }
    }
}

