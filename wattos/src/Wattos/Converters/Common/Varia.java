/*
 * Created on June 5, 2002, 11:02 AM
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Common;


import Wattos.Utils.*;
import java.util.*;

/** Common routines for parsing Xplor constraints.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Varia {

    /** Distance
     */    
    static final public int DATA_TYPE_DISTANCE                  = 1;
    /** Dihedral angle
     */    
    static final public int DATA_TYPE_DIHEDRAL                  = 2;
    /** Residual dipolar couplings
     */    
    static final public int DATA_TYPE_DIPOLAR_COUPLING          = 3;
    /** Chemical shift assignments
     */    
    static final public int DATA_TYPE_CHEMICAL_SHIFT            = 4;
    /** Coupling constants
     */    
    static final public int DATA_TYPE_COUPLING_CONSTANT         = 5;
    /** Planarity restraints
     */    
    static final public int DATA_TYPE_PLANARITY                 = 6;

    /** String representation that needs to map exactly to how the
     *names are listed in the classification and the above int defs.
     *It should also map with the defs given in Wattos.Utils.NmrStar.
     */
    static final public String[] DATA_TYPE_NAMES = {
        "invalid data type", "distance", "dihedral angle", "dipolar coupling" };
    
    /** AND operation id
     */    
    static final public int OPERATION_TYPE_AND         = 1;
    /** OR operation id
     */    
    static final public int OPERATION_TYPE_OR          = 2;

    
    public static String getPossibleStringFromHashMap( HashMap attrSet, Object key ) {
        if ( ! attrSet.containsKey(key) ) {
            return Wattos.Utils.NmrStar.STAR_EMPTY;
        }
        Object o = attrSet.get( key );
        if ( !(o instanceof ArrayList )) {
            General.showError("Value to key : " + key + " in attr set wasn't of type ArrayList; ignored set");
            return Wattos.Utils.NmrStar.STAR_EMPTY;
        }
        ArrayList l = (ArrayList) o;
        o = l.get( 0 );
        if ( !(o instanceof String )) {
            General.showError("First value in attr set wasn't of type String; ignored set");
            return Wattos.Utils.NmrStar.STAR_EMPTY;
        }
        return (String) o;
    }
    
    
    
    
    
    /** Obligatory constructor; don't use.
     */
    public Varia() {
        General.showError("Don't try to initiate Varia class");
    }
} 
