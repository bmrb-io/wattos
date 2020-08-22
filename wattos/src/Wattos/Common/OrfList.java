/*
 * OrfList.java
 *
 * Created on February 10, 2003, 3:02 PM
 */

package Wattos.Common;

import java.io.*;
import java.util.*;

/**
 *Open reading frame/protein or nucleic acid sequence and id parameters.
 * Used to read FastaFiles etc.
 * @author Jurgen F. Doreleijers
 */
public class OrfList implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;
    
    public ArrayList orf_list = null;
    
    /** Creates a new instance of OrfList */
    public OrfList() {
        init();
    }
    
    public void init( ) {
        orf_list = new ArrayList();
    }

    
            
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
    }
    
}
