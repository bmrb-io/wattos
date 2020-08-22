package Wattos.Soup.Comparator;

import java.util.Comparator;

import Wattos.Soup.Gumbo;

/**
 * @author Jurgen F. Doreleijers
 */
public class ComparatorAtomWithoutEntry implements Comparator {
    
    public Gumbo gumbo;
    /** Creates a new instance of ComparatorAtom */
    public ComparatorAtomWithoutEntry(Gumbo gumbo) {
        this.gumbo = gumbo;        
    }
    
    /** Just do comparison on first object with all but entry info. */
    public int compare(Object o1, Object o2) {               
        return gumbo.atom.compare( ((int[])  o1)[0], ((int[])  o2)[0], true, false );
    }
}
