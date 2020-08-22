/*
 * GetEpochTime.java
 *
 * Created on March 14, 2003, 5:13 PM
 */

package Wattos.Utils.Programs;

import java.util.Date;

/**
 * The simplest Java program I've ever written.
 * @author Jurgen F. Doreleijers
 */
class GetEpochTime {
    
    /** Creates a new instance of GetEpochTime */
    public GetEpochTime() {
    }
     
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Date d = new Date();
        String e = Long.toString( d.getTime() );
        System.out.println(e);
    }   
}
