/*
 * AtomMap.java
 *
 * Created on August 27, 2003, 3:04 PM
 */

package Wattos.Soup;

import java.util.*;
import java.io.*;
import java.net.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Star.*;
/**
 * Contains atom nomenclature (and residue nomenclature) mapping info for 
 *different programs such as XPLOR. Curiously, currently NOT in use.
 *
 * @author Jurgen F. Doreleijers
 */
public class AtomMap implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Local resource */
    static final String FILE_LOCATION = "Data/AtomMap.str";
    
    public static final String NOMENCLATURE_ID_IUPAC       = "IUPAC"; // All maps will be to/from this one.
    public static final String NOMENCLATURE_ID_PDB         = "PDB";  
    public static final String NOMENCLATURE_ID_DYANA       = "DYANA";
    public static final String NOMENCLATURE_ID_XPLOR       = "XPLOR";
    public static final String NOMENCLATURE_ID_DISCOVER    = "DISCOVER";
    public static final String NOMENCLATURE_ID_GROMOS      = "GROMOS"; 
    public static final String NOMENCLATURE_ID_XPLOR_INV   = "XPLOR_INVERTED";
    public static final String NOMENCLATURE_ID_XPLOR_IUPAC = "XPLOR_IUPAC";
    public static final String NOMENCLATURE_ID_USER        = "USER";    

    /** A map that's fast to do lookups like:<BR>
     *In nomenclature id X for residue name Y for atom name Z what's 
     *the atom name in nomenclature X**-1. 
     *E.g. to get the IUPAC name for PDB name 1HD in PHE use:
     *toIUPAC.get(NOMENCLATURE_ID_PDB, "PHE", "1HD" ) (add casts)
     *If many in the same residue and nomenclature need to be looked up it
     *might be benificial to cache the map for the residue:
     *HashMap tmpMap = toIUPAC.get(NOMENCLATURE_ID_PDB, "PHE" ) (add casts)
     *and then look in that one directly:
     *tmpMap.get("1HD");
     */
    
    public HashOfHashesOfHashes fromIUPAC;
    public HashOfHashesOfHashes toIUPAC;
    public HashOfHashes fromIUPACRes;
    public HashOfHashes toIUPACRes;
    public ArrayList NOMENCLATURE_NAMES;
    
    /** Report warnings on mappings only once */
    public static HashOfHashes reportedAtomFailures;
    /** Creates a new instance of AtomMap */
    public AtomMap() {                
        init();
    }
    
    public boolean init() {
        reportedAtomFailures = new HashOfHashes();
        fromIUPAC  = new HashOfHashesOfHashes();
        toIUPAC    = new HashOfHashesOfHashes();
        fromIUPACRes  = new HashOfHashes();
        toIUPACRes    = new HashOfHashes();
        NOMENCLATURE_NAMES = new ArrayList();
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_IUPAC     );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_PDB       );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_DYANA     );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_XPLOR     );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_DISCOVER  );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_GROMOS    );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_XPLOR_INV );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_XPLOR_IUPAC );
        NOMENCLATURE_NAMES.add( NOMENCLATURE_ID_USER      );        
        return true;
    }
    
    public boolean readStarFile( URL url) {
        if ( url == null ) {
            url = getClass().getResource(FILE_LOCATION);
        }
        DBMS dbms_local = new DBMS(); // Create a local copy so anything can be read in.
        StarFileReader sfr = new StarFileReader(dbms_local);        
//        long start = System.currentTimeMillis();
        StarNode sn = sfr.parse( url );
//        long taken = System.currentTimeMillis() - start;
        //General.showOutput("STARLexer: " + taken + "(" + (taken/1000.0) + " sec)" );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        //General.showOutput("Parse successful");
        //Wattos.Utils.General.showDebug("DBMS: " + dbms_local.toString( true ));
        // Iterate over the saveframes
        Object o_tmp_1 = sn.get(0);
        if ( !(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;
        
        for (int sf_id=0;sf_id<db.size();sf_id++ ) {
            Object o_tmp_2 = db.get(sf_id);
            if ( !(o_tmp_2 instanceof SaveFrame)) {
                General.showError("Expected object of type SaveFrame but got: " + o_tmp_2.getClass().getName());
                return false;
            }
            SaveFrame sf = (SaveFrame) o_tmp_2;
            String name = sf.title;
//            General.showDebug("Found saveframe with name: " + name);
            int idxNomenclature = NOMENCLATURE_NAMES.indexOf(name);
            if ( idxNomenclature < 0 ) {
                General.showError("Failed to find name as one of the predetermined formats; only use USER to define your own.");
                General.showError("Allowed formats are:" + Strings.toString(NOMENCLATURE_NAMES) );
                return false;
            }

            // ****************     ATOMS *****************
            ArrayList result = sf.getTagTableList("_Atom_mapping.Lib_residue_name");
            if ( result == null || (result.size() != 1) ) {
                if ( result == null ) {
                    General.showWarning("Found number of loops: null");
                } else {
                    General.showWarning("Found number of loops: " + result.size());
                }
                General.showWarning("Expected one loop defining the atom mapping but didn't found one. Skipping this saveframe.");
                continue;
            }
            Object o_tmp_3 = result.get(0);
                
            if ( !(o_tmp_3 instanceof TagTable)) {
                General.showError("Expected object of type TagTable as second node in saveframe but got type: " + o_tmp_2.getClass().getName());
                return false;
            }
            TagTable tT = (TagTable) o_tmp_3;
            String[] residue    = tT.getColumnString("_Atom_mapping.Lib_residue_name");
            String[] atom       = tT.getColumnString("_Atom_mapping.Lib_atom_name");
            String[] iupacAtom  = tT.getColumnString("_Atom_mapping.IUPAC_atom_name");
            if ( ( residue == null ) || ( atom == null ) || ( iupacAtom == null )) {
                General.showError("Failed to find 3 String[] columns in tagtable as expected.");
                return false;
            }

            HashOfHashes mapFromIupac = new HashOfHashes();
            HashOfHashes mapToIupac = new HashOfHashes();
            fromIUPAC.put(   name, mapFromIupac);
            toIUPAC.put(     name, mapToIupac);
            // Use cached nomenclature map for first level so put doesn't need
            // to lookup 3 levels but only 2.
            for (int i=tT.used.nextSetBit(0); i>=0; i=tT.used.nextSetBit(i+1)) {
                mapFromIupac.put(   residue[i],iupacAtom[i],    atom[i]);
                mapToIupac.put(     residue[i],atom[i],         iupacAtom[i]);
            }            
//            General.showDebug("Found number of elements in mapFromIupac : " + mapFromIupac.cardinality());        
//            General.showDebug("Found number of elements in mapToIupac   : " + mapToIupac.cardinality());        

                        
            // ****************     RESIDUES *****************
            result = sf.getTagTableList("_Residue_mapping.Lib_residue_name");
            if ( result == null || (result.size() != 1) ) {
                if ( result == null ) {
                    General.showWarning("Found number of loops: null");
                } else {
                    General.showWarning("Found number of loops: " + result.size());
                }
                General.showWarning("Expected one loop defining the residue mapping but didn't found one. Skipping this saveframe.");
                continue;
            }
            o_tmp_3 = result.get(0);
                
            if ( !(o_tmp_3 instanceof TagTable)) {
                General.showError("Expected object of type TagTable as second node in saveframe but got type: " + o_tmp_2.getClass().getName());
                return false;
            }
            tT = (TagTable) o_tmp_3;
            String[] LibResidue    = tT.getColumnString("_Residue_mapping.Lib_residue_name");
            String[] IUPACResidue  = tT.getColumnString("_Residue_mapping.IUPAC_residue_name");
            if ( ( LibResidue == null ) || ( IUPACResidue == null ) ) {
                General.showError("Failed to find the 2 String[] columns in tagtable as expected.");
                return false;
            }

            HashMap mapFromIUPACRes = new HashMap();
            HashMap mapToIUPACRes   = new HashMap();
            fromIUPACRes.put(   name, mapFromIUPACRes);
            toIUPACRes.put(     name, mapToIUPACRes);
            // Use cached nomenclature map for first level so put doesn't need
            // to lookup 3 levels but only 2.
            for (int i=tT.used.nextSetBit(0); i>=0; i=tT.used.nextSetBit(i+1)) {
                mapFromIUPACRes.put(IUPACResidue[i],LibResidue[i]);
                mapToIUPACRes.put(  LibResidue[i],  IUPACResidue[i]);
            }            
//            General.showDebug("Found number of elements in mapFromIUPACRes : " + mapFromIUPACRes.size());        
//            General.showDebug("Found number of elements in mapToIUPACRes   : " + mapToIUPACRes.size());        
        }
//        General.showDebug("Found number of elements in fromIUPAC : " + fromIUPACRes.cardinality());        
//        General.showDebug("Found number of elements in toIUPAC   : " + toIUPACRes.cardinality());        
        return true;
    }
    
    /** Xplor will be able to IUPAC but not for pseudos
     * 
     *
     * @param atomName
     * @param atomNomenclature Can be AtomMap.NOMENCLATURE_ID_IUPAC or
     * AtomMap.NOMENCLATURE_ID_XPLOR_IUPAC
     * 
     */
    public String atomNameToXplor( String atomName, String atomNomenclature,
            String resName) {
        String atomNameStrXplor = atomName;

        if ( ! atomNomenclature.equals(AtomMap.NOMENCLATURE_ID_IUPAC)) {             
            atomNameStrXplor = (String) fromIUPAC.get(
                    atomNomenclature, resName, atomName );
            if ( atomNameStrXplor == null ) {
                atomNameStrXplor = atomName;
                if ( ! atomName.startsWith("Q") &&
                   ( atomNomenclature.equals(AtomMap.NOMENCLATURE_ID_XPLOR_IUPAC))) {
                    ; // don't report on the incomplete lib.
                } else {
                    if ( reportedAtomFailures.get(resName,atomName) == null ) {
                        reportedAtomFailures.put(resName, atomName, atomName);
                        General.showWarning("Failed to find from IUPAC to " + 
                                atomNomenclature + " atom name: " + atomName + 
                                " for residue name: " + resName + " at least once.");
                        
                    }
                }
            }
        }
        return atomNameStrXplor;
    }
    
    public String atomNameToIupac( String atomNomenclature, String atomName, 
            String resName) {
        return (String) toIUPAC.get(atomNomenclature, resName, atomName );
    }
}
