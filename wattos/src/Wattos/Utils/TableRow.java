/*
 * TableRow.java
 *
 * Created on February 17, 2003, 4:49 PM
 */

package Wattos.Utils;

import java.util.*;

/**
 * Contains the data and method for sorting.
 * @author Jurgen F. Doreleijers
 */
public class TableRow implements Comparable {
    
    ArrayList column_values;
    
    public void init() {
        column_values = new ArrayList();
    }
    
    /** Creates a new instance of TableRow */
    public TableRow() {
        init();
    }
    
    public int compareTo( Object otherTableRow ) {
        
        TableRow otherRow = (TableRow) otherTableRow;
        // if they don't have equal dimensions; they can't be
        // compared.
        if ( column_values.size() != otherRow.column_values.size() ) {
            General.showError("Different dimensions for the rows: " +
                column_values.size() + " and " + otherRow.column_values.size());
            return 0;
        }
        for (int i=0;i<column_values.size();i++ ) {
            String s_1 = (String) column_values.get(i);
            String s_2 = (String) otherRow.column_values.get(i);
            //General.showDebug("comparing: [" + s_1 + "] with: [" + s_2 + "]" );
            int c = s_1.compareTo( s_2 ); 
            if ( c != 0 ) {
                return c;
            }
        }
        //General.showDebug("the rows are identical on all columns");
        return 0;            
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
    }    
}
