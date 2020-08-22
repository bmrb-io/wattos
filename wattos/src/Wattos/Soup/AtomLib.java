/*
 * AtomLib.java
 */

package Wattos.Soup;

import java.util.*;
import java.io.*;
import java.net.*;
import Wattos.Utils.*;
import Wattos.Database.*;
import Wattos.Star.*;
/**
 * Contains atom info for dihedrals etc. 
 *
 * @author Jurgen F. Doreleijers
 */
public class AtomLib implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Local resource */
    static final String FILE_LOCATION = "Data/AtomLib.str";
    
    /** A map like: na py chi  O4' C1' N1  C2' . . . .     */   
    public String[][] DIHEDRAL_LIST = null;

//    public StringArrayList DIHEDRAL_LIST_ORDER;
    /** Reset later */
    static int IDX_POL_TYPE       = 0;  
    static int IDX_COMP_ID        = 0;
    static int IDX_ANGLE_NAME     = 0;
    static int IDX_ATOM_ID_1      = 0;
    static int IDX_ATOM_ID_2      = 0;
    static int IDX_ATOM_ID_3      = 0;
    static int IDX_ATOM_ID_4      = 0;
    static int IDX_COMP_SEQ_ID_1  = 0;
    static int IDX_COMP_SEQ_ID_2  = 0;
    static int IDX_COMP_SEQ_ID_3  = 0;
    static int IDX_COMP_SEQ_ID_4  = 0;
    
    /** Creates a new instance of AtomLib */
    public AtomLib() {                
        init();
    }
    
    public boolean init() {
        int i=0;
        IDX_POL_TYPE       = i++;  
        IDX_COMP_ID        = i++;
        IDX_ANGLE_NAME     = i++;
        IDX_ATOM_ID_1      = i++;
        IDX_ATOM_ID_2      = i++;
        IDX_ATOM_ID_3      = i++;
        IDX_ATOM_ID_4      = i++;
        IDX_COMP_SEQ_ID_1  = i++;
        IDX_COMP_SEQ_ID_2  = i++;
        IDX_COMP_SEQ_ID_3  = i++;
        IDX_COMP_SEQ_ID_4  = i++;
        return true;
    }
    
    public boolean readStarFile( URL url) {
        if ( url == null ) {
            url = getClass().getResource(FILE_LOCATION);
        }
        DBMS dbms_local = new DBMS(); // Create a local copy so anything can be read in.
        StarFileReader sfr = new StarFileReader(dbms_local);        
        StarNode sn = sfr.parse( url );
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
        // Iterate over the saveframes
        Object o_tmp_1 = sn.get(0);
        if ( !(o_tmp_1 instanceof DataBlock)) {
            General.showError("Expected top level object of type DataBlock but got: " + o_tmp_1.getClass().getName());
            return false;
        }
        DataBlock db = (DataBlock) o_tmp_1;
        
        int sf_id=1;

        Object o_tmp_2 = db.get(sf_id);
        if ( !(o_tmp_2 instanceof SaveFrame)) {
            General.showError("Expected object of type SaveFrame but got: " + o_tmp_2.getClass().getName());
            return false;
        }
        SaveFrame sf = (SaveFrame) o_tmp_2;
//        String name = sf.title;
//        General.showDebug("Found saveframe with name: " + name);

        ArrayList result = sf.getTagTableList("_Angle_name");
        if ( result == null || result.size() < 1 ) {
            General.showError("Found no or empty loop");
            return false;
        }
        Object o_tmp_3 = result.get(0);
            
        if ( !(o_tmp_3 instanceof TagTable)) {
            General.showError("Expected object of type TagTable as second node in saveframe but got type: " + o_tmp_2.getClass().getName());
            return false;
        }
        TagTable tT = (TagTable) o_tmp_3;
        DIHEDRAL_LIST = tT.toStringOfString();

//        DIHEDRAL_LIST_ORDER = new StringArrayList();
//        for (int i=0;i<DIHEDRAL_LIST.length;i++) {
//            if (! DIHEDRAL_LIST_ORDER.contains(DIHEDRAL_LIST[i][IDX_ANGLE_NAME])) {
//                DIHEDRAL_LIST_ORDER.add(DIHEDRAL_LIST[i][IDX_ANGLE_NAME]);
//            }
//        }
//        General.showDebug("Read table: ["+Strings.toString(DIHEDRAL_LIST));
//        General.showDebug("Read dihedral order: ["+Strings.toString(DIHEDRAL_LIST_ORDER));
        
        return true;
    }
}
