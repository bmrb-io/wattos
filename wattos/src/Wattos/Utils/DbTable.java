/*
 * DbTable.java
 * Created on February 4, 2002, 4:56 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Utils;

import java.util.*;

/**
 * Table concept used in the NMR restraint Grid. A richer api is provided by
 * the {@link Wattos.Database.Relation } class.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */

public class DbTable extends Table {
    private static final long serialVersionUID = 2933534095581892992L;
    static final public String DEFAULT_TYPE      = "Type";
    static final public String DEFAULT_TYPENAME  = "Typename";
    
    /**Type of column in a db independent manner. E.g. 12 for "STRING" and 2 for "INT" */
    public ArrayList types = new ArrayList();
    /**Type of column in a db dependent manner. E.g. "VARCHAR2 and NUMBER*/
    public ArrayList typenames = new ArrayList();
    /** Values by column. In each column the type is the same.
     */
    
    /** Creates new DbTable */
    public DbTable() {
        this(0,0);
    }

    /** Creates new DbTable */
    public DbTable(int nrows, int ncols) {
        super(nrows,ncols);
        
        types = new ArrayList();
        typenames = new ArrayList();
        
        for (int c=0; c < ncols; c++) {
            types.add(DEFAULT_TYPE);
            typenames.add(DEFAULT_TYPENAME);
        }
    }
    
    public void init() {
        super.init();
        types = new ArrayList();
        typenames = new ArrayList();
    }
    
    /** Some range checking done. No error message printed though.
     *Inserts an extra column before the given position.
     */
    public boolean insertColumn( int column ) {

        super.insertColumn( column );
        
        int columnCount = sizeColumns();
        if ( column > columnCount ) {
            General.showError("insertColumn column > columnCount: "+
                column + " " + columnCount );
            return false;
        }
        if ( column < 0 ) {
            General.showError("insertColumn column < 0: "+
                column + " " + columnCount );
            return false;
        }
        
        types.add(      column, DEFAULT_TYPE);
        typenames.add(  column, DEFAULT_TYPENAME);

        return true;
    }
    
    // Default representation.
    public String toString() {
        return( toString(true, true, false) );
    }

    // Overrides the superclasses' method
    public String toString(boolean show_header, boolean show_types, boolean show_typenames ) {
        

        int columnCount = sizeColumns();
//        int rowCount = sizeRows();

        if ( columnCount < 1 ) {
            return ( "Empty DbTable object." );
        }
        
        StringBuffer sb = new StringBuffer();

        // Header
        if ( show_header ) {
            sb.append("---  Table View ---\n");

            for (int i=0;i<columnCount;i++) {
                sb.append(labels.get(i));
                if ( show_types || show_typenames )
                    sb.append(" (");
                if ( show_types )
                    sb.append(types.get(i));
                if ( show_types && show_typenames )
                    sb.append(", ");
                if ( show_typenames )
                    sb.append(typenames.get(i));
                if ( show_types || show_typenames )
                    sb.append(")");

                if ( i < ( columnCount - 1 ) )
                    sb.append(", ");  
                else
                    sb.append(General.eol);  
            }
        }
        /**
        Table table = (Table) this;
         */
        String main_table_str = super.toString(false);
         
        if (main_table_str!=null) {
            sb.append(main_table_str);
        } else {
            return( null );
        }
        return (sb.toString() );
    }
    

    /** Self test;
     * @param args Ignored.
     */
    public static void main (String[] args) {
        // constructor
        if ( true ) {
            DbTable dbt = new DbTable(1,2);
            dbt.setValueByTable("TEST");
            dbt.types.set(0, "my_type");
            dbt.insertColumn(0);
            General.showOutput(dbt.toString(true, true, true));            
        }
        General.showOutput("Done all selected tests!" );            
    }     
}
