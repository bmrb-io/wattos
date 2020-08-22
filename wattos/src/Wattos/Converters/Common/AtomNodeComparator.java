/*
 * AtomNodeComparator.java
 *
 * Created on August 22, 2002, 4:59 PM
 */

package Wattos.Converters.Common;

import java.util.*;

/**
 *
 * @author Jurgen F. Doreleijers
 * @version 1.0
 */
public class AtomNodeComparator implements Comparator {

    /** Creates new AtomNodeComparator */
    public AtomNodeComparator() {
    }

    /**
    * @param args the command line arguments
    */
    public static void main (String args[]) {
    }

    public int compare(Object atom_1, Object atom_2) {        
        return AtomNode.compare((AtomNode) atom_1, (AtomNode) atom_2);
    }
    
}
