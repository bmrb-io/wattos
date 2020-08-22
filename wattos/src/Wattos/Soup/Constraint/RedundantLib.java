/*
 * AtomMap.java
 *
 * Created on August 27, 2003, 3:04 PM
 */

package Wattos.Soup.Constraint;

import java.util.*;
import java.io.*;
import java.net.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Star.*;
/**
 * Encodes redunancy distances for all known residues (just 20 AA now) from
 *an external file.
 *Changes all lower bounds read to a minimum of 1.8 Ang.
 *
 * @author Jurgen F. Doreleijers
 */
public class RedundantLib implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    public static float LOWER_DISTANCE_MINIMUM_CORRECTION = 0.2f; // To get from 2.0 to 1.8 Ang. like in Aqua
    public static float LOWER_DISTANCE_MINIMUM = 2f-LOWER_DISTANCE_MINIMUM_CORRECTION;

    /** Local resource */
    static final String STR_FILE_LOCATION = "Data/redundant.str";

    public static String saveframeNodeCategoryName      = "redundant_distance_info";
    public static String tagNameCompID                  = "_Comp_ID";
    public static String tagNameAtomID                  = "_Atom_ID";
    public static String tagNameDistanceType            = "_Distance_type";
    public static String tagNameDistanceAtomBase        = "_Distance_atom_";
    /** How many atoms per residue appear in the star file maximum? */
    public static int atomPerResidueCountMax            = 30;

    public static int DEFAULT_IDX_LOW_BOUND = 0;
    public static int DEFAULT_IDX_UPP_BOUND = 1;
    /** In case the bound has no known upper limit; use this value.*/
    //public static float DEFAULT_UNKNOWN_BOUND_UPP = 100.0f;
    /** In case the bound has no known lower limit; use this value.
     *Currently set to two times the radius of a hydrogen atom
     *but can be changed.
     */
    //public static float DEFAULT_UNKNOWN_BOUND_LOW = 1.8f;

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
    public HashOfHashesOfHashes bounds;

    /** Creates a new instance of AtomMap */
    public RedundantLib() {
        init();
    }

    public boolean init() {
        bounds  = new HashOfHashesOfHashes();
        return true;
    }

    public boolean readStarFile( URL url) {
        if ( url == null ) {
            url = getClass().getResource(STR_FILE_LOCATION);
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

        TagTable tT = sn.getTagTable( saveframeNodeCategoryName, tagNameCompID, true);
        if ( tT == null ) {
            General.showError("Expected to find the appropriate tagtable but apparently not." );
            return false;
        }

        String[] varCompID      = tT.getColumnString(tagNameCompID);
        String[] varAtomID      = tT.getColumnString(tagNameAtomID);
        String[] varDistanceType= tT.getColumnString(tagNameDistanceType);
        float[][]varBounds      = new float[atomPerResidueCountMax][];

        for (int c=1;c<=atomPerResidueCountMax;c++ ) {
            String label = tagNameDistanceAtomBase + c;
            if ( ! tT.containsColumn( label )) {
                General.showError("Failed to get column for tag name: " + label);
                return false;
            }
            if ( ! tT.convertDataTypeColumn(label,Relation.DATA_TYPE_FLOAT,null) ) {
                General.showError("Failed to convert column to float from string for tag name: " + label);
                return false;
            }
            varBounds[c-1] = tT.getColumnFloat(label);
            if ( varBounds[c-1] == null ) {
                General.showError("Failed to get float column for tag name: " + label);
                return false;
            }
        }

        //Wattos.Utils.General.showDebug("DBMS: " + dbms_local.toString( true ));

        String currentCompId = Defs.NULL_STRING_NULL;
        for (int r=0;r<tT.sizeRows;) {
            ArrayList alAtomNames = new ArrayList();
            // Find the rows associated with one residue. Afterwards reset r to point to the next atom of a new residue or to tT.size()
            currentCompId = varCompID[r];
            //General.showDebug("Looking for atoms for residue: " + currentCompId);
            int s=r;
            for (;s<tT.sizeRows;s++ ) {
                if ( ! currentCompId.equals( varCompID[s] )) {
                    break; // all atoms have been collected.
                }
                if ( varDistanceType[s].equals("L")) {
                    continue; // only count an atom once
                }
                //General.showDebug("Found atom: " + varAtomID[s] + " at row: " + s);
                alAtomNames.add( varAtomID[s] );
            }
            //s--; // rewind one so s points to
            // at this point r points to beginning and s points to end of atoms in residue.
            //General.showDebug("Found atoms in rows: " + r + " to: " + s );
            for (int t=r;t<s;t++ ) { // loop over atoms
                int idx = -1;
                if ( varDistanceType[t].equals("L")) {
                    idx = DEFAULT_IDX_LOW_BOUND;
                } else if (varDistanceType[t].equals("U") ) {
                    idx = DEFAULT_IDX_UPP_BOUND;
                } else {
                    General.showError("Distance type should be L or U but found: " + varDistanceType[t] + " on line: " +  t);
                    return false;
                }
                String fromAtom = varAtomID[t];
                for (int c=1;c<=atomPerResidueCountMax;c++ ) { // skip 0 because self contacts aren't interesting.
                    float b = varBounds[c-1][t];
                    if ( Defs.isNull(b) ) {
                        //General.showDebug("Not storing null bound");
                        continue; // don't store nulls
                    }
                    //General.showDebug("Looking at row: " + t + " at bound: " + c + " finding bound: " + b);
                    String toAtom = (String) alAtomNames.get(c-1);
                    float[] bs = null;
                    //General.showDebug("Storing from atom: " + fromAtom + " to: " + toAtom);

                    /** symmetric -1- */
                    if ( bounds.get(currentCompId,fromAtom,toAtom) == null ) {
                        bounds.put(currentCompId,fromAtom,toAtom,new float[2]);
                    }
                    bs = (float[]) bounds.get(currentCompId,fromAtom,toAtom);
                    if ( bs == null ) {
                        General.showCodeBug("Error in code in RedundantLib -1-");
                        return false;
                    }
                    bs[ idx ] = b;
                    if ( (idx == DEFAULT_IDX_LOW_BOUND) && ( b < LOWER_DISTANCE_MINIMUM )) {
                        bs[ idx ] = LOWER_DISTANCE_MINIMUM;
                    }
                    /** symmetric -2- */
                    if ( bounds.get(currentCompId,toAtom,fromAtom) == null ) {
                        bounds.put(currentCompId,toAtom,fromAtom,new float[2]);
                    }
                    bs = (float[]) bounds.get(currentCompId,toAtom,fromAtom);
                    if ( bs == null ) {
                        General.showCodeBug("Error in code in RedundantLib -2-");
                        return false;
                    }
                    bs[ idx ] = b;
                    if ( (idx == DEFAULT_IDX_LOW_BOUND) && ( b < LOWER_DISTANCE_MINIMUM )) {
                        bs[ idx ] = LOWER_DISTANCE_MINIMUM;
                    }
                }
            }
            r = s;
        }
        //General.showDebug("Found number of elements in bounds : " + bounds.cardinality());
        return true;
    }



    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        RedundantLib rl = new RedundantLib();
        boolean status = rl.readStarFile( null );
        if (! status) {
            General.showError(" in RedundantLib.main found:");
            General.showError(" reading RedundantLib star file.");
            System.exit(1);
        }
        General.showDebug(" read RedundantLib star file.");

//        if ( false ) {
//            General.showOutput("RedundantLib:\n" + rl.bounds);
//        }
        if ( true ) {
            String resName   = "VAL";
            String atomName1 = "H";
            String atomName2 = "HG11";
            General.showOutput("Found for "+resName+", "+atomName1+", "+atomName2+": " +
                PrimitiveArray.toString( rl.bounds.get(resName, atomName1, atomName2)));
        }
    }
}
