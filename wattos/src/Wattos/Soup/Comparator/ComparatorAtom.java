/*
 * ComparatorAtom.java
 *
 * Created on September 29, 2003, 2:15 PM
 */

package Wattos.Soup.Comparator;

import java.util.Comparator;

import Wattos.Soup.Gumbo;

/**
 * @author Jurgen F. Doreleijers
 */
public class ComparatorAtom implements Comparator {
    
    public Gumbo gumbo;
    /** Creates a new instance of ComparatorAtom */
    public ComparatorAtom(Gumbo gumbo) {
        this.gumbo = gumbo;        
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {                
        return gumbo.atom.compare( ((int[])  o1)[0], ((int[])  o2)[0], false, false );
    }
}
