/*
 * GetNMRSTARConstraintCount.java
 *
 * Created on May 20, 2004, 3:57 PM
 */

package Wattos.Utils.Programs;

import java.net.*;

import Wattos.Database.*;
import Wattos.Utils.*;
import Wattos.Star.*;

/**
 *Simple routine that gets the number of items in the first datanode, 
 * the second saveframe, second tagtable or the string
 *"Not a Number" if absent.
 * @author Jurgen F. Doreleijers
 */
public class GetNMRSTARConstraintCount {
    
    /** Creates a new instance of GetNMRSTARConstraintCount */
    public GetNMRSTARConstraintCount() {
    }
    
    /**
     * Read a CIF file into STAR tagtables and all; then
     * map the items in the largest table (coordinates) to native types.
     */
    public static String getCount(String fileName) {
        
        URL    url          = InOut.getUrlFileFromName(fileName);
        DBMS dbms = new DBMS();
        StarFileReader sfr = new StarFileReader(dbms);
        // parse
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            System.exit(1);
        }
        
        StarNode  dn = (StarNode) sn.get(0); //Get the first datanode (always present)
        StarNode  sf = (StarNode) dn.get(1); //Get the second saveframe (always present)
        //General.showOutput("sf node is: " + sf.toSTAR());
        
        String count = "Not a Number";
        if ( sf.size() >= 2 ) {
            TagTable tt = (TagTable) sf.get(1); // The second tagtable (not always present)
            count = tt.getValueString(tt.sizeRows-1,1); // Get the value from the last row and the first real column (0 column is order);
        }
        return count;
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String fileName =  "c:\\jurgen\\tmp_unb\\mrgridFiles\\block_10252.txt";
        if ( args.length != 0 ) {
            fileName =  args[0];
        }
        General.showOutput("Found number of constraint items: " + getCount(fileName));
    }
    
}
