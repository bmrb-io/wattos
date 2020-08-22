/*
 * GoId.java
 *
 * Created on March 7, 2005, 11:50 AM
 */

package Wattos.Gobbler.TargetScore;

import Wattos.Utils.*;

/**
 *Simple class for comparing GO ids.
 * @author Jurgen F. Doreleijers
 */
public class GoId {
    
    /** Creates a new instance of GoId */
    public GoId() {
    }
    
    /** Funny thing about Go ids is that sometimes they contain the GO. prefix.
     */
    public static boolean equalGoId ( String i1, String i2 ) {
        if ( i1.equals(i2) ) {
            return true;
        }
        String goid_1 = i1;
        String goid_2 = i2;
        if (goid_1.startsWith("GO.")) {
            goid_1 = goid_1.substring(3);
        }
        if (goid_2.startsWith("GO.")) {
            goid_2 = goid_2.substring(3);
        }
        return goid_1.equals(goid_2);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String oi_1 = "GO.1234";
        String oi_2 = "1234";
        General.showOutput("Same orf id: " + equalGoId(oi_1, oi_2));
    }
    
}
