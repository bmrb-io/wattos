/*
 * DatabaseDefinitions.java
 *
 * Created on December 18, 2002, 2:38 PM
 */

package Wattos.Common;

import java.util.*;

import Wattos.Utils.*;
/**
 * Contains a mapping between database names as used in the NR and BMRB databases.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class DatabaseDefinitions {

    static final HashMap bmrb2NrDb = new HashMap();
    static final HashMap nrDb2Bmrb;

    /** Names of the databases. E.g. BMRB, pir, gb, ref, etc.
     *A nice overview of databases and what abbreviation they use is
     *available at http://srs.ebi.ac.uk/srs6bin/cgi-bin/wgetz?-page+databanks+-newId
     */    
    static {
        bmrb2NrDb.put("PDB",        "pdb");
        bmrb2NrDb.put("DBJ",        "dbj");
        bmrb2NrDb.put("EMBL",       "emb");
        bmrb2NrDb.put("GenBank",    "gb");
        bmrb2NrDb.put("GI",         "gi");  // GenBank identifier
        bmrb2NrDb.put("PIR",        "pir"); // Protein Identification Resource 
        bmrb2NrDb.put("PRF",        "prf");
        bmrb2NrDb.put("REF",        "ref");
        bmrb2NrDb.put("SWISS-PROT", "sp");
        /** Next ones commented out because they map to the same
         */
        //bmrb2NrDb.put("",        "tpg"); // don't know this one??? todo; find out.
        //bmrb2NrDb.put("Sesame",     "");
        bmrb2NrDb.put("BMRB",     "bmrb");
        
        /** Possible extensions take from http://pir.georgetown.edu/pirwww/search/batch.html.
         *<option value="ALLIDS"  >Any Unique ID
<option value="CSQID"  >iProclass ID
<option value="SWIDS"  >SwissProt ID
<option value="TIGRID"  >TIGR ID
<option value="TAXGRPID"  >Taxon Group ID
<option value="TRIDS"  >TrEMBL ID
<option value="UWGPID"  >UWGP ID
<option value="HDOMAIN"  >PIR HD ID
<option value="PIIDS"  >PIR ID
<option value="PRINTS"  >PRINTS ID
<option value="BINDID"  >BIND ID
<option value="BLOCKS"  >BLOCKS ID
<option value="COG"  >COG ID
<option value="ECNUM"  >EC #
<option value="FLYID"  >FLY ID
<option value="MIPSFAM"  >Family #
<option value="GDBID"  >GDB ID
<option value="GO"  >GO ID
<option value="GBEMBLDDBJ"  >GenBank ID
<option value="GEACCS"  >Genpept Accession #
<option value="GEIDS"  >Genpept ID
<option value="PATHWAY"  >KEGG Pathway ID
<option value="LOCUSID"  >LOCUS ID
<option value="MGIID"  >MGI ID
<option value="GIIDS"  >NCBI GI #
<option value="TAXID"  >NCBI Taxon ID
<option value="NFID"  >NREF ID
<option value="OMMIMID"  >OMMIM ID
<option value="PCMOTIF"  >PC Motif ID
<option value="PDACCS"  >PDB Accession #
<option value="PDIDS"  >PDB ID
<option value="PIACCS"  >PIR Accession #
<option value="PFAM"  >Pfam ID
<option value="BIBLIO"  >PubMed ID
<option value="RESID"  >RESID ID
<option value="REACCS"  >Refseq Accession #
<option value="REIDS"  >Refseq ID
<option value="SGDID"  >SGD ID
<option value="SUPFAM"  >Superfamily #

         */
        
        nrDb2Bmrb = MapSpecific.invertHashMap( bmrb2NrDb );
    }
    /** Creates new DatabaseDefinitions */
    public DatabaseDefinitions() {
    }

    /** Gives GenBank for gb */
    static public String getBmrbFromNrDb( String q ) {
        String result = (String) nrDb2Bmrb.get( q );
        if ( result == null ) {
            result = q;
        }
        return result;
    }

    /** Gives gb for GenBank */
    static public String geNrDbFromBmrb( String q ) {
        String result = (String) bmrb2NrDb.get( q );
        if ( result == null ) {
            result = q;
        }
        return result;
    }
}
