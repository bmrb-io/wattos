/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Converters.Common;

import java.util.jar.Attributes;
import java.util.*;

/**LogicalNode class is used as nodes for logical tree
 * Attributes entry stores shared mapping for this node
 * such as distance, treeNodeId
 * Vector atoms composed of Attributes for every atom in node
 */
public class LogicalNode{
   
    /** Room for attributes.
     */    
    public Attributes entry;
    /** The list is in itself composed of a list of lists of basically attributes per
     * atom. In the usual setup item 0 in atoms is a list of atoms corresponding to
     * the left side of the constraint. This setup will work with restraints
     * between more than 2 atom lists too, e.g. 3D NOESY restraints. It will
     * not work with all other types of distances like symmetry-defining and
     * NOEs alla Habazettl et al. (example D in the documentation).
     */    
    public ArrayList atoms;
    /** Pairs of atoms that come from an ARIA OR construction.
     */
    public ArrayList atomsOR;

    /** Optional info.
     */    
    public HashMap opt_info;
    
    /**Construct an empty LogicalNode
     */
    public LogicalNode() {
	//initialize empty entries and atoms
	entry       = new Attributes();
        opt_info    = new HashMap();
	atoms       = new ArrayList();
	atomsOR     = new ArrayList();
    }

}//LogicalNode class
