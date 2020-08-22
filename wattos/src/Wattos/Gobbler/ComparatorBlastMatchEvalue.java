package Wattos.Gobbler;

import java.util.Comparator;

public class ComparatorBlastMatchEvalue implements Comparator {
    
    public ComparatorBlastMatchEvalue() {
    }
    
    /** Just do comparison on first object */
    public int compare(Object o1, Object o2) {       
        BlastMatch oip1= (BlastMatch) o1;        
        BlastMatch oip2= (BlastMatch) o2; 
        if ( oip1.expectation_value == oip2.expectation_value) return 0;
        if ( oip1.expectation_value < oip2.expectation_value) {
            return -1;
        } 
        return 1;
    }            
}
 
