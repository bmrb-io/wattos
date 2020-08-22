/*
 * AtomLib.java
 */

package Wattos.Soup;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;

import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.StringArrayList;
/**
 * Contains atom type and charge info 
 *
 * @author Jurgen F. Doreleijers
 */
public class AtomLibAmber implements Serializable {
        
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Unknow type */
    public static final int DEFAULT_ATOM_TYPE_UNKNOWN_ID         = 0;
    /** Examples from AtomLibAmber is SER      OG                 OH            -0.6546 */
    public static final int DEFAULT_ATOM_TYPE_NONLINEAR_ID       = 1;
    /** Examples from AtomLibAmber is SER      CB                 CT             0.2117 */
    public static final int DEFAULT_ATOM_TYPE_TETRAHEDRAL_ID     = 2;
    /** Examples from AtomLibAmber is SER      C                  C              0.5973 */
    public static final int DEFAULT_ATOM_TYPE_PLANAR_ID          = 3;
    public static final String[] DEFAULT_ATOM_TYPE_STRING_LIST = new String[] {
        "unknonw",
        "non-linear",
        "tetrahedral",
        "planar"
    };
    public static final String[] atomTypeStringList = new String[] {
        "unknown",
        "C",  
        "C*", 
        "CA", 
        "CB", 
        "CC", 
        "CN", 
        "CR", 
        "CT", 
        "CV", 
        "CW", 
        "H",  
        "H1", 
        "H4", 
        "H5", 
        "HA", 
        "HC", 
        "HO", 
        "HP", 
        "HS", 
        "N",  
        "N2", 
        "N3", 
        "NA", 
        "NB", 
        "O",  
        "O2", 
        "OH", 
        "S",  
        "SH"        
    };
    public static final String[] atomTypeStringListNonlinear = new String[] {
        "OW",
        "OH",
        "OS",
        "S" ,
        "SH"};
    public static final String[] atomTypeStringListTetrahedral = new String[] {
        "P" , // Originally a nonlinear.   
        "CT",
        "N3",
        "NT"};   
    public static final String[] atomTypeStringListPlanar = new String[] {
        "C" ,
        "C*",
        "CA",
        "CB",
        "CC",
        "CD",
        "CK",
        "CM",
        "CN",
        "CQ",
        "CR",
        "CV",
        "CW",
        "N" ,
        "N*",
        "N2",   
        "NA",
        "NB",
        "NC",
        "O" ,
        "O2"};
    
    /** Local resource */
    static final String FILE_LOCATION = "Data/AtomLibAmber.str";

    public StringArrayList atomTypeStringArrayList;
    public StringArrayList atomTypeStringArrayListNonlinear;
    public StringArrayList atomTypeStringArrayListTetrahedral;
    public StringArrayList atomTypeStringArrayListPlanar;

    public HashOfHashes atomType;
    public HashOfHashes atomCharge;    

    
    public AtomLibAmber() {                
        init();
    }
    
    public boolean init() {
        atomTypeStringArrayList             = new StringArrayList();
        atomTypeStringArrayListNonlinear    = new StringArrayList();
        atomTypeStringArrayListTetrahedral  = new StringArrayList();
        atomTypeStringArrayListPlanar       = new StringArrayList();

        atomTypeStringArrayList.addAll(             atomTypeStringList);
        atomTypeStringArrayListNonlinear.addAll(    atomTypeStringListNonlinear );
        atomTypeStringArrayListTetrahedral.addAll(  atomTypeStringListTetrahedral);
        atomTypeStringArrayListPlanar.addAll(       atomTypeStringListPlanar);

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

        ArrayList result = sf.getTagTableList("_Comp_ID");
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
        tT.convertDataTypeColumn("_Atom_charge",   Relation.DATA_TYPE_FLOAT, null);
        //ALA,CB->CT 
        atomType = new HashOfHashes();
        atomCharge = new HashOfHashes();
        String[] resi = tT.getColumnString("_Comp_ID");
        String[] name = tT.getColumnString("_Atom_ID");
        String[] type = tT.getColumnString("_Atom_type");
        float[] charge = tT.getColumnFloat("_Atom_charge");
        for (int r=tT.used.nextSetBit(0);r>=0;r=tT.used.nextSetBit(r+1)) {
            String thisType = type[r];
            int thisTypeIdx = DEFAULT_ATOM_TYPE_UNKNOWN_ID;
            if ( atomTypeStringArrayListNonlinear.contains(thisType)) {
                thisTypeIdx = DEFAULT_ATOM_TYPE_NONLINEAR_ID;
            } else if ( atomTypeStringArrayListTetrahedral.contains(thisType)) {
                thisTypeIdx = DEFAULT_ATOM_TYPE_TETRAHEDRAL_ID;
            } else if ( atomTypeStringArrayListPlanar.contains(thisType)) {
                thisTypeIdx = DEFAULT_ATOM_TYPE_PLANAR_ID;
            }
              atomType.put(resi[r], name[r], new Integer( thisTypeIdx));                
            atomCharge.put(resi[r], name[r], new Float(charge[r]));
        }
//        General.showDebug("Loaded types: " + atomType.toString());
        return true;
    }
    
    public int getAtomTypeId( String resi, String name ) {
        int typeId = atomType.getInt(resi, name);
        if ( Defs.isNull(typeId)) {
            return DEFAULT_ATOM_TYPE_UNKNOWN_ID;
        }
        return typeId;
    }
}
