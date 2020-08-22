/*
 * File.java
 *
 * Created on April 25, 2003, 1:21 PM
 */

package Wattos.Star;

import java.io.*;

import Wattos.Utils.*;

/**
 * The top level in STAR, ignoring the block level that isn't modeled anyway.
 * @author Jurgen F. Doreleijers
 */
public class DataBlock extends StarNode {
    
    public static final String STANDARD_TITLE = "standard_"+
        StarGeneral.DATA_NODE_TYPE_DESCRIPTION[StarGeneral.DATA_NODE_TYPE_DATABLOCK]+"_title";
                                        
    /** Creates a new instance of File */
    public DataBlock() {
        super.init();
        init();
    }
    
    public void init() {
        title                   = STANDARD_TITLE;
        parent                  = null;     
        dataNodeType            = StarGeneral.DATA_NODE_TYPE_DATABLOCK;                
    }

    public boolean toSTAR( Writer w) {
        StringBuffer sb = new StringBuffer();        
        try {             
            sb.append("data_");
            sb.append(title);
            sb.append("\n\n");
            w.write(sb.toString());
        } catch ( IOException e_io ) {
            General.showError("I/O error: " +  e_io.getMessage() );
            return false;
        }
        super.toSTAR(w); // Write elements        
        return true;
    }    
}
