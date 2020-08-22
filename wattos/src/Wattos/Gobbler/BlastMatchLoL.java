/*
 * BlastMatchLoL.java
 *
 * Created on December 12, 2002, 10:45 AM
 */

package Wattos.Gobbler;

import java.util.*;
import java.io.*;
import com.Ostermiller.util.CSVPrinter;
import Wattos.Utils.*;

/**
 * This container class contains a List of Lists of BlastMatch objects. This class
 *also has methods for reading/writing these objects. LoL is funny because in
 *slang it also means lot's of laughter;_)
 *It contains no serializable attributes except the blastmatch objects.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class BlastMatchLoL implements Serializable {

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;

    /** The list contains zero or more queries and in the deeper level the 
     *matches with subjects.
     */
    public ArrayList LoL_matches;
    
    /** Creates new BlastMatchLoL */
    public BlastMatchLoL() {
        init();
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
    }
 
    public void init() {
        if ( LoL_matches != null ) {
            for (Iterator it=LoL_matches.iterator();it.hasNext();) {
               BlastMatchList bml = (BlastMatchList) it.next();
               bml.init();
            }
        }
        LoL_matches = new ArrayList();
    }
    
    public boolean toCsv(CSVPrinter out) {
        int n = 0;
        for (Iterator it=LoL_matches.iterator();it.hasNext();) {
           BlastMatchList bml = (BlastMatchList) it.next();
           boolean status = bml.toCsv( out );
           if ( ! status ) {
               General.showError("converting to Csv BlastMatchList instance:" + n);
               return false;
           }
           n++;
        }
        return true;
    }    
    
    public boolean toBmrbStyle() {
        int i = 0;
        for (Iterator it=LoL_matches.iterator();it.hasNext();) {
           BlastMatchList bml = (BlastMatchList) it.next();
           boolean status = bml.toBmrbStyle();
           if ( ! status ) {
               General.showError("converting to Csv BlastMatchList instance:" + i);
               return false;
           }
           i++;
        }
        return true;
    }    
}
