/*
 * Calculation.java
 *
 * Created on August 11, 2005, 2:03 PM
 */

package Wattos.Soup;

import Wattos.Database.*;
import Wattos.Utils.*;
import Wattos.Utils.Wiskunde.*;

/**
 *Code for calculating hydrogen bond energy. More to come.
 * @author  jurgen
 */
public class Calculation {
    
    /** For hydrogen bond calculation */
    public static final float HBq1 = 0.42f; // units are the unit electron charge
    /** For hydrogen bond calculation */
    public static final float HBq2 = 0.2f;  
    /** For hydrogen bond calculation */
    public static final float HBf  = 332f;  // unit is dimensionless
    /** For hydrogen bond calculation */
    public static final float HBfactor = HBf*HBq1*HBq2;  // unit is dimensionless

    /** Standard definitions used in dimplot that calls hbplus */
    public static final float hbHADistance = 2.7f;
    /** Standard definitions used in dimplot that calls hbplus  */
    public static final float hbDADistance = 3.35f;
    /** Standard definitions used in hbplus  */
    public static final float hbDHAAngle   = 90f;
    
    /** A very small value for a dimension in Angstrom. Smaller than would be significant
     *to say a bond length. The precision in PDB files is 0.001 Angstrom.
     */    
    public static final float distanceEps = 0.00001f;  // unit is dimensionless
    
    /** Creates a new instance of Calculation */
    public Calculation() {
    }
    
    /**
     * HB according to some of HBPlus definitions 
     * returns true in case of a bond.
     * returns false if not and when there's an error.
     *@see Atom#calcHydrogenBond
     */
    public static boolean isHydrogenBond(Atom atom,
            int ridD, int ridH, int ridA, 
            float hbHADistance, float hbDADistance, float hbDHAAngle ) { 
                
        if ( atom.calcDistanceFast( ridH, ridA) > hbHADistance) {
            return false;
        }
        if ( atom.calcDistanceFast( ridD, ridA) > hbDADistance) {
            return false;
        }
        
        if ( (atom.calcAngle( ridD, ridH, ridA )*Geometry.CF) < hbDHAAngle ) {
            return false;            
        }
        return true;
    }

    
    
    /** HB energy according to Kabsch and Sander DSSP paper.
     *returns a zero in case the energy is less favourable than -0.5 kcal/mol or
     *if there was an error.
     *by a quick distance test. A cutoff of 5.2 Angstrom between N and O (or other
     *acceptor) is used.
     *Returns a float null in case of an error, e.g. when atoms coincide.
     */
    public static float calculateHydrogenBondEnergyKS(Atom atom,
            int ridN, int ridH, int ridO, int ridC) { 
        float distanceNOMax = 5.2f;  // see paper.
        // Do a quick check for closeness so no multiplication is wasted.
        if ( Math.abs( atom.xList[ridN] - atom.xList[ridO]) > distanceNOMax) {
            return 0f;
        }
        float d3 = atom.calcDistanceFast( ridN, ridO);
        // Next check still saves a factor of 4 easily.
        if ( d3 > distanceNOMax) {
            return 0f;
        }
        float d1 = atom.calcDistanceFast( ridO, ridN);
        float d2 = atom.calcDistanceFast( ridC, ridH);
        float d4 = atom.calcDistanceFast( ridC, ridN);
        // Check nulls
        if ( Defs.isNull( d1 )) {
            General.showError("Failed to calculate a distance between atoms: " +
                atom.toString(ridO) + General.eol +
                atom.toString(ridN));
            return Defs.NULL_FLOAT;            
        }
        if ( Defs.isNull( d2 )) {
            General.showError("Failed to calculate a distance between atoms: " +
                atom.toString(ridC) + General.eol +
                atom.toString(ridH));
            return Defs.NULL_FLOAT;            
        }
        if ( Defs.isNull( d3 )) {
            General.showError("Failed to calculate a distance between atoms: " +
                atom.toString(ridO) + General.eol +
                atom.toString(ridH));
            return Defs.NULL_FLOAT;            
        }
        if ( Defs.isNull( d4 )) {
            General.showError("Failed to calculate a distance between atoms: " +
                atom.toString(ridC) + General.eol +
                atom.toString(ridN));
            return Defs.NULL_FLOAT;            
        }
        
        // check for too small distances.
        if ( d1 < distanceEps ) {
            General.showError("Distance between different atoms is too small: " +
                "(" + distanceEps + ")\n" +
                atom.toString(ridO) + General.eol +
                atom.toString(ridN));
            return Defs.NULL_FLOAT;            
        }
        if ( d2 < distanceEps ) {
            General.showError("Distance between different atoms is too small: " +
                "(" + distanceEps + ")\n" +
                atom.toString(ridC) + General.eol +
                atom.toString(ridH));
            return Defs.NULL_FLOAT;            
        }
        if ( d3 < distanceEps ) {
            General.showError("Distance between different atoms is too small: " +
                "(" + distanceEps + ")\n" +
                atom.toString(ridO) + General.eol +
                atom.toString(ridH));
            return Defs.NULL_FLOAT;            
        }
        if ( d4 < distanceEps ) {
            General.showError("Distance between different atoms is too small: " +
                "(" + distanceEps + ")\n" +
                atom.toString(ridC) + General.eol +
                atom.toString(ridN));
            return Defs.NULL_FLOAT;            
        }
        
        return HBfactor * (1/d1 + 1/d2 - 1/d3 - 1/d4);
    }    
}
