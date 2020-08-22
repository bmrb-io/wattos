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
 * Contains completeness info for all known residues (just 20 AA, and 6 NA now) 
 *
 * @author Jurgen F. Doreleijers
 */
public class CompletenessLib implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    
    
    /** Local resource */
    public static String STR_FILE_DIR = "Data";
    public static String STR_FILE_NAME = "ob_standard.str";

    public static String saveframeNodeCategoryName      = "completeness_observable_info";
    public static String tagNameCompID                  = "_Comp_ID";
    public static String tagNameAtomID                  = "_Atom_ID";
    public String fileName = null;
    
    public HashOfLists obs;
    /** Creates a new instance of AtomMap */
    public CompletenessLib() {                
        init();
    }
    
    public boolean init() {
        obs  = new HashOfLists();
        return true;
    }
    
    public boolean readStarFile( URL url) {
        if ( url == null ) {
            String STR_FILE_LOCATION = STR_FILE_DIR + "/" + STR_FILE_NAME;
            url = getClass().getResource(STR_FILE_LOCATION);
        }
        fileName = url.toString();
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
        //General.showOutput("Parse of completeness lib is successful");
    
        TagTable tT = sn.getTagTable( saveframeNodeCategoryName, tagNameCompID, true);
        if ( tT == null ) {
            General.showError("Expected to find the appropriate tagtable but apparently not." );
            return false;
        }

        String[] varCompID      = tT.getColumnString(tagNameCompID);
        String[] varAtomID      = tT.getColumnString(tagNameAtomID);

        int atomCount = tT.sizeRows;
//        ArrayList al;
        for (int r=0;r<atomCount;r++) obs.put( varCompID[r], -1, varAtomID[r] ); // keep appending to the end.        
        //General.showDebug("Found number of elements in obs : " + obs.size());        
        return true;
    }
    
            
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        CompletenessLib cl = new CompletenessLib();    
        //cl.STR_FILE_NAME = "ob_hnha.str";
        
        boolean status = cl.readStarFile( null );
        if (! status) {
            General.showError(" in CompletenessLib.main found:");
            General.showError(" reading CompletenessLib star file.");
            System.exit(1);
        }
        General.showDebug(" read CompletenessLib star file.");
                
        if ( true ) {
            General.showOutput("CompletenessLib:\n" + Strings.toString( cl.obs));
            String resName = "PHE";
            General.showOutput("Found for "+resName+" : " + Strings.toString( (ArrayList) cl.obs.get(resName)));
        }        
    }    
}
