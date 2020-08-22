/*
 * Created on February 4, 2002, 4:56 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Utils;

import java.io.*;
import java.util.*;

import com.Ostermiller.util.CSVPrinter;
import Wattos.Database.*;

/**
 * Table concept used in the NMR restraint Grid. A richer api is provided by
 * the {@link Wattos.Database.Relation } class.
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class Table implements Cloneable, Serializable {
    private static final long serialVersionUID = 7688526320482104073L;
    /** Default value of a cell */
    static final String DEFAULT_CELL_VALUE      = "";
    /**In case no label for a column is given this will be used. */
    static final String DEFAULT_LABEL_PREFIX    = "Label_";

    /** Only strings are allowed. A null reference is NOT allowed.*/
    public ArrayList labels = new ArrayList();
    /** Values by column. In each column the type is the same.
     *The values can be any object class that has a toString method. A null
     reference is allowed.
     */
    public ArrayList values_by_col = new ArrayList();

    /** Initializes a Table with 0 rows and 0 columns.
     */
    public Table() {
        this(0,0);
    }

    public Table( Table table ) {
        this( table.sizeRows(), table.sizeColumns());

        int nrows = table.sizeRows();
        int ncols = table.sizeColumns();

        // Do simple deep clone.
        for (int c=0; c < ncols; c++) {
            setLabel( c, table.getLabel(c) );
            for (int r=0; r < nrows; r++)
                setValue(r,c, table.getValue(r,c));
        }
    }

    /**
     * @param nrows
     * @param ncols
     */
    public Table( int nrows, int ncols) {
        init();
        // This might need some optimalization for large tables.
        for (int c=0; c < ncols; c++) {
            labels.add( DEFAULT_LABEL_PREFIX + c );
            ArrayList column_values = new ArrayList();
            for (int r=0; r < nrows; r++) {
                column_values.add(DEFAULT_CELL_VALUE);
            }
            values_by_col.add( column_values );
        }
    }

    public void init() {
        labels = new ArrayList();
        values_by_col = new ArrayList();
    }


    /** First attempt at writing some cloning method */
    public Object clone() {
        try {
            // This call has to be first command
            Table ntable = (Table) super.clone();
            /** Can't use the clone method of ArrayList because they are shallow.
            ntable.labels = (ArrayList) labels.clone();
            ntable.values_by_col = (ArrayList) values_by_col.clone();
             */
            int nrows = sizeRows();
            int ncols = sizeColumns();

            ntable.labels = new ArrayList();
            ntable.values_by_col = new ArrayList();

            // Do simple deep clone.
            for (int c=0; c < ncols; c++) {
                ntable.labels.add( getLabel(c) );
                ntable.values_by_col.add( new ArrayList() );
                ArrayList column_values = (ArrayList) ntable.values_by_col.get(c);
                for (int r=0; r < nrows; r++)
                    column_values.add( getValue(r,c) );
            }

            return ntable;
        } catch (CloneNotSupportedException e) {
            // Cannot happen -- we support clone, and so do the other
            // classes.
            throw new InternalError(e.toString());
        }
    }

    /** Sorts the table rows given the columns to sort on in descending
     *importance; e.g. column_priorities = {0,1} then the rows will be
     *sorted first looking at column 0 and then column 1 if the
     *values in column 0 are the same.
     *A full list of columns needs to be given...
     *Since no actual data is being copied; all operations are by reference;
     *it shouldn't be that slow.
     */
    public boolean sortRowsAscii( int[] column_priorities ) {

        /** Check and correct the argument for validity and completeness
         */
        int ncols = sizeColumns();
        int[] column_priorities_new;
        if ( ncols < column_priorities.length ) {
            General.showWarning("column priorities given will be truncated from: "
                + column_priorities.length  + " to the correct number: " + ncols);
            General.showOutput("This is usually the wrong thing to do.");
            column_priorities_new = new int[ncols];
            System.arraycopy(column_priorities, 0, column_priorities_new, 0, ncols);
            column_priorities = column_priorities_new;
        } else if ( ncols > column_priorities.length ) {
            //General.showDebug(" column priorities given will be expanded from: "
            //    + column_priorities.length  + " to the correct number: " + ncols);

            column_priorities_new = new int[ncols];
            System.arraycopy(column_priorities, 0, column_priorities_new, 0, column_priorities.length);
            ArrayList column_priorities_array = PrimitiveArray.asList( column_priorities );
            int idx_new = column_priorities.length;
            for (int c=0; c < ncols; c++) {
                if ( ! column_priorities_array.contains( new Integer( c ) )) {
                    // add to list and increment new position.
                    //General.showDebug(" added at position: " + idx_new + " column id: " + c);
                    column_priorities_new[ idx_new ] = c;
                    idx_new++;
                } else {
                    // skip it; it was already in the priority list
                }
            }
            if ( idx_new != ncols ) {
                General.showError("Code bug or input contains non unique ids or out of range ids or so...;-)");
                return false;
            }
            column_priorities = column_priorities_new;
        }
        // Just checking to see if all columns occur exactly once.
        int[] column_priorities_copy = new int[ncols];
        System.arraycopy( column_priorities, 0, column_priorities_copy, 0, ncols);
        Arrays.sort( column_priorities_copy );
        for (int c=0; c < ncols; c++) {
            if ( column_priorities_copy[c] != c ) {
                General.showError("column: " + c + " doesn't occur in corrected priority list.");
                General.showError( "List reads (corrected): " + PrimitiveArray.toString( column_priorities));
                General.showError( "List reads (sorted)   : " + PrimitiveArray.toString( column_priorities_copy));
                return false;
            }
        }
        General.showError( "code incomplete in: Table.sortRowsAscii" );
        General.showError( "No actual sorting done now." );
        // Deleted code here.
        return true;
    }

    /** Strip a number of characters from the end of each value in a given
     *column. Be very carefull because no checks are performed.
     */
    public boolean stripValueByColumn( int c, boolean fromEnd, int count ) {

        if ( ! fromEnd ) {
            General.showError("code feature to not start from beginning in Table.stripValueByColumn;");
            return false;
        }

        int nrows = sizeRows();
        for (int r=0; r < nrows; r++) {
            String s = (String) getValue(r,c);
            s = s.substring(0,s.length()-count);
            setValue( r, c, s);
        }
        return true;
    }

    /** Append/prepend a number of characters to the end/begin of each value in a given
     *column. Be very carefull because no checks are performed.
     */
    public boolean apendToValueByColumn( int c, boolean atEnd, String str ) {

        if ( ! atEnd ) {
            General.showError("code feature to not start from end in Table.apendToValueByColumn;");
            return false;
        }

        int nrows = sizeRows();
        for (int r=0; r < nrows; r++) {
            String s = (String) getValue(r,c);
            s += str;
            setValue( r, c, s);
        }
        return true;
    }

    /** Append/prepend a number of characters to the end/begin of value in a given
     *cell. */
    public boolean apendToValue( int r, int c, String str ) {
        String s = (String) getValue(r,c);
        s += str;
        setValue( r, c, s);
        return true;
    }

    /** Sorts the table rows given the column based on the
     percentage in the rows. */
    public boolean sortRowsPercentage( int column, boolean ascending ) {

        stripValueByColumn(     column, true, 1);
        sortRowsNumber(         column, ascending );
        apendToValueByColumn(  column, true, "%" );
        return true;
    }


    /** Sorts the table rows given the column based on the
     number in the rows. */
    public boolean sortRowsNumber( int column, boolean ascending ) {

        int ncols = sizeColumns();
        int nrows = sizeRows();
        // Create a kind of table as a list of tablerownumber objects.
        // And sort that based on the column requested.
        ArrayList tmp_table = new ArrayList();
        for (int r=0; r < nrows; r++) {
            TableRowNumbers trn = new TableRowNumbers();
            trn.column_values.add( new Double( (String) getValue(r,column) ) );
            trn.column_values.add( new Double( r )); // original row number used for later.
            tmp_table.add(trn);
        }
        Collections.sort( tmp_table ); // Uses methods in TableRowNumbers
        if ( ! ascending ) {
            Collections.reverse( tmp_table );
        }

        // Create the new table values
        // Get dimensions and labels the expensive way.
        Table new_table = (Table) this.clone();
        new_table.setValueByTable(DEFAULT_CELL_VALUE); // nil
        for (int r=0; r < nrows; r++) {
            TableRowNumbers trn = (TableRowNumbers) tmp_table.get(r);
            int old_idx = (int) ((Double) trn.column_values.get( 1 )).intValue();
            for (int c=0; c < ncols; c++) {
                new_table.setValue( r, c, getValue(old_idx,c));
            }
        }
        values_by_col = new_table.values_by_col;

        return true;
    }

    /** Done using references for elements so watch out.
     */
    public void getTableByColumn( ArrayList tableRowList ) {
        int ncols = sizeColumns();
        int nrows = sizeRows();
        ArrayList values_by_col_new = new ArrayList();
        for (int c=0; c < ncols; c++) {
            ArrayList values = new ArrayList();
            for (int r=0; r < nrows; r++) {
                TableRow tableRow = (TableRow) tableRowList.get(r);
                values.add( tableRow.column_values.get(c));
            }
            values_by_col_new.add( values );
        }
        values_by_col = values_by_col_new;
    }

    /** Done using references for elements so watch out.
     */
    public ArrayList getTableByRow() {
        int ncols = sizeColumns();
        int nrows = sizeRows();
        ArrayList tableRowList = new ArrayList();
        for (int r=0; r < nrows; r++) {
            TableRow tableRow = new TableRow();
            for (int c=0; c < ncols; c++) {
                tableRow.column_values.add( ((ArrayList) values_by_col.get(c)).get(r));
            }
            tableRowList.add( tableRow );
        }
        return tableRowList;
    }


    /** Does not do anything yet */
    public void setCommonDefaults() {}

    /** No extra range checking done for efficiency     */
    public void setValue( int row, int column, Object o) {
        ((ArrayList) values_by_col.get(column)).set(row,o);
    }

    /** The object can be null, a Boolean, or an Integer. In
     *all other cases it will return false.
     */
    public boolean isEmptyValue( Object value ) {
        if ( value == null ) {
            return true;
        }


        if ( value instanceof Boolean) {
            if ( value.equals(Boolean.FALSE)) {
                return true;
            }
            return false;
        }

        if ( value instanceof Integer ) {
            if ( ((Integer)value).intValue() == 0 ) {
                return true;
            }
            return false;
        }

        /** Most expensive checks first */
        if ( value instanceof String ) {
            String valueStr = (String) value;
            if (valueStr.equalsIgnoreCase("") ||
                valueStr.equalsIgnoreCase(Defs.STRING_TRUE) ||
                valueStr.equalsIgnoreCase("t") ) {
                return true;
            }
            return false;
        }

        return false;
    }

    /** No extra range checking done for efficiency     */
    public Object getValue( int row, int column) {
        return ( ((ArrayList) values_by_col.get(column)).get(row));
    }

    /** Convenience class
     */
    public String getValueString( int row, int column) {
        Object o = getValue( row, column );
        if ( o == null ) {
            return Defs.NULL_STRING_NULL;
        }
        return o.toString();
    }

    /** No extra range checking done for efficiency     */
    public void setLabel( int column, Object o) {
        labels.set(column, o);
    }

    /** No extra range checking done for efficiency     */
    public Object getLabel( int column) {
        return ( labels.get(column) );
    }

    /** No extra range checking done for efficiency     */
    public int getColumnIdx( String label) {
        return labels.indexOf(label);
    }

    /** Some range checking done. No error message printed though.
     *Inserts an extra row before the given position, so the new
     *row inserted has that position. E.g. insert at 0 is an insert
     *at the beginning. insert at n with n being the number of
     *rows appends a row. Appending is cheapest with the implementations
     *used!
     */
    public boolean insertRow( int row ) {

        General.showDebug(" routine not tested yet");
        int rowCount = sizeRows();
        int columnCount = sizeColumns();

        if ( row > rowCount ) {
            General.showError("insertRow row > rowCount: "+
                row + " " + rowCount );
            return false;
        }
        if ( row < 0 ) {
            General.showError("insertRow row < 0: "+
                row + " " + rowCount );
            return false;
        }


        for (int c=0; c < columnCount; c++) {
            ((ArrayList) values_by_col.get(c)).add(row, DEFAULT_CELL_VALUE );
        }

        return true;
    }

    /** Appending is reasonably fast.
     */
    public boolean addRow( String value ) {
        int columnCount = sizeColumns();
        for (int c=0; c < columnCount; c++) {
            ((ArrayList) values_by_col.get(c)).add(value);
        }
        return true;
    }

    /** Appending is reasonably fast.
     */
    public boolean addRow() {
        return addRow( DEFAULT_CELL_VALUE );
    }
    /** Some range checking done.
     *Inserts an extra row before the given position, so the new
     *row inserted has that position. E.g. insert at 0 is an insert
     *at the beginning. insert at n with n being the number of
     *rows appends a row. Appending is cheapest with the implementations
     *used!
     */
    public boolean moveRow( int row_org, int row_new ) {

        int rowCount = sizeRows();
        int columnCount = sizeColumns();

        if ( row_org > rowCount ) {
            General.showError("moveRow row > rowCount: "+
                row_org + " " + rowCount );
            return false;
        }
        if ( row_org < 0 ) {
            General.showError("moveRow row < 0: "+
                row_org + " " + rowCount );
            return false;
        }

        if ( row_new > rowCount ) {
            General.showError("moveRow to row > rowCount: "+
                row_new + " " + rowCount );
            return false;
        }
        if ( row_new < 0 ) {
            General.showError("moveRow to row < 0: "+
                row_new + " " + rowCount );
            return false;
        }

        for (int c=0; c < columnCount; c++) {
            swapValues( row_org, c, row_new, c );
        }

        return true;
    }

    /** Simple swap
     */
    public void swapValues( int row_org, int col_org, int row_new, int col_new ) {
        Object tmp = getValue( row_org, col_org );
        setValue( row_org, col_org, getValue( row_new, col_new ));
        setValue( row_new, col_new, tmp);
    }

    /** Some range checking done. No error message printed though.
     *Inserts an extra column before the given position.
     */
    public boolean insertColumn( int column ) {

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

        int rowCount = sizeRows();

        ArrayList al = new ArrayList();
        for (int r=0; r < rowCount; r++)
            al.add( DEFAULT_CELL_VALUE );

        labels.add( column, DEFAULT_LABEL_PREFIX + column );
        values_by_col.add(column, al);

        setLabel( column, DEFAULT_LABEL_PREFIX + column);

        /** Relabel consecutive columns if they have the standard prefix. */
        columnCount++;
        for (int r=column+1;r<columnCount;r++) {
            String label = (String) getLabel( r );
            if ( label.regionMatches(0, DEFAULT_LABEL_PREFIX,
                                     0, DEFAULT_LABEL_PREFIX.length() ) ||
                 label.equals("") ) {
                setLabel( r, DEFAULT_LABEL_PREFIX + r );
            }
        }

        return true;
    }

    /**
     *Deletes a column at given position but only if all rows are empty.
     */
    public boolean removeAnyEmptyColumn() {
        int columnCount = sizeColumns();
        //General.showOutput("Checking to remove any empty column of the available: " + columnCount);
        for (int c=columnCount-1;c>0;c--) {
            if ( ! removeColumnIfEmpty(c)) {
                General.showError("Failed to removeColumnIfEmpty on column: " + c);
                return false;
            }
        }
        return true;
    }

    /**
     *Deletes a column at given position but only if all rows are empty.
     */
    public boolean removeColumnIfEmpty( int column ) {
        //General.showOutput("Checking to remove if empty column: " + column);
        int columnCount = sizeColumns();
        if ( column < 0 || column >= columnCount ) {
            General.showError("In removeColumnIfEmpty: column doesn't exist: " + column);
            return false;
        }
        boolean isEmpty = true;
        int rowCount = sizeRows();
        for (int r=0;r<rowCount;r++) {
            Object value = getValue( r, column );
            if ( !isEmptyValue(value)) {
                isEmpty = false;
                break;
            }
        }
        if ( isEmpty ) {
            return removeColumn( column );
        }

        return true;
    }


    /** Some range checking done. No error message printed though.
     *Deletes a column at given position.
     */
    public boolean removeColumn( int column ) {

        //General.showOutput("Removing column: " + column);
        int columnCount = sizeColumns();
        if ( column > ( columnCount - 1 ) ) {
            General.showError("deleteColumn column > (columnCount -1): "+
                column + " " + columnCount );
            return false;
        }
        if ( column < 0 ) {
            General.showError("deleteColumn column < 0: "+
                column + " " + columnCount );
            return false;
        }

//        int rowCount = sizeRows();

        labels.remove( column );
        // Remove by column is very fast in this data model!
        values_by_col.remove( column );

        return true;
    }

    /** Inclusive start row and exclusive endRow. Rows are numbered from zero and run
     *to size n -1. Some range checking done.
     */
    public boolean removeRowsFromTo( int startRow, int endRow ) {
        int rowCount = sizeRows();
        if ( startRow >= endRow ) {
            General.showError("startRow >= endRow: "+
                startRow + " " + endRow );
            return false;
        }
        if ( startRow < 0 ) {
            General.showError("removeRowsFromTo startRow < 0: "+
                startRow );
            return false;
        }
        if ( endRow > rowCount ) {
            General.showError("removeRowsFromTo endRow > rowCount: "+
                endRow + " " + rowCount );
            return false;
        }
        for (int r=endRow-1; r>=startRow; r--) {
            if ( ! removeRow( r )) {
                return false;
            }
        }
        return true;
    }

    /** Keep only those rows listed.
     *Start is inclusive, end is exclusive
     */
    public boolean keepRowsFromTo( int startRow, int endRow ) {
        int rowCount = sizeRows();
        if ( startRow >= endRow ) {
            General.showError("startRow >= endRow: "+
                startRow + " " + endRow );
            return false;
        }
        if ( startRow < 0 ) {
            General.showError("removeRowsFromTo startRow < 0: "+
                startRow );
            return false;
        }
        if ( endRow > rowCount ) {
            General.showError("removeRowsFromTo endRow > rowCount: "+
                endRow + " " + rowCount );
            return false;
        }
        for (int r=rowCount-1; r>=0; r--) {
            if ( (r < startRow) || (r >= endRow) ) {
                if ( ! removeRow( r )) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Some range checking done. No error message printed though.
     *Deletes a column at given position.
     */
    public boolean removeRow( int row ) {

        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( row > ( rowCount - 1 ) ) {
            General.showError("removeRow row > (rowCount -1): "+
                row + " " + rowCount );
            return false;
        }
        if ( row < 0 ) {
            General.showError("removeRow row < 0: "+
                row + " " + rowCount );
            return false;
        }

        // Remove by row is not that fast in this model
        for (int c=0;c<columnCount;c++) {
            ArrayList column = (ArrayList) values_by_col.get(c);
            column.remove( row );
        }

        return true;
    }

    /** No error message printed.*/
    public void replaceStringByTable( String str_old, String str_new) {
        int columnCount = sizeColumns();
        for (int c=0;c<columnCount;c++) {
            replaceStringByColumn( c, str_old, str_new);
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void replaceStringByColumn( int column, String str_old, String str_new) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( column < 0 || column >= columnCount )
            return;
        for (int r=0;r<rowCount;r++) {
            replaceString( r, column, str_old, str_new);
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void replaceString( int row, int column, String str_old, String str_new) {
        String value = getValueString(row, column);
        if ( value == null ) {
            return;
        }
        if ( value.equals(str_old)) {
            setValue(row,column,str_new);
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void setValueByColumn( int column, Object o) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( column < 0 || column >= columnCount )
            return;
        for (int r=0;r<rowCount;r++)
            setValue( r, column, o);
    }

    /** Some range checking done. No error message printed though.*/
    public void setValueByRow( int row, Object o) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( row < 0 || row >= rowCount )
            return;
        for (int c=0;c<columnCount;c++)
            setValue( row, c, o);
    }

    /** Some range checking done. No error message printed though.*/
    public void setValueByTable( Object o) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( columnCount < 0 )
            return;
        for (int r=0;r<rowCount;r++)
            setValueByRow( r, o);
    }

    /**
     * @return  size of the table in rows */
    public int sizeRows() {
        if ( sizeColumns() > 0 )
            return ( (ArrayList) values_by_col.get(0)).size();
        else
            return 0;
    }

    /**
     * @return  size of the table in columns*/
    public int sizeColumns() {
        return ( values_by_col.size() );
    }


    /** Simple comparison method based not on comparing individual elements but
     *comparing the string representations of the whole tables. Including the
     *labels of the columns.
     */
    public boolean equalsByStringRepresentation( Table other ) {
        if ( other == null ) {
            return false;
        }
        String this_string = this.toString();
        String other_string = other.toString();

        if ( this_string.equals(other_string) ) {
            return true;
        }
        //showDifferences(other);
        return false;
    }

    /**Compares two tables cell by cell and shows any differences. The labels of
     *the columns are checked too. No range checking is done either. The number
     *of differences are printed if any are present.
     */
    public void showDifferences( Table other ) {
        int rowCount = sizeRows();
        int columnCount = sizeColumns();
        int diffs = 0;

        // check labels
        for (int c=0;c<columnCount;c++) {
            if ( ! getLabel(c).equals(other.getLabel(c) ) ) {
                General.showOutput("For column " + c + " labels diff: [" +
                    getLabel(c) + "] and [" + other.getLabel(c) + "]");
                diffs++;
            }
        }

        // check values
        for (int r=0;r<rowCount;r++) {
            for (int c=0;c<columnCount;c++) {
                if ( ! getValueString(r,c).equals(other.getValueString(r,c) ) ) {
                    General.showOutput("For " + r + ", " + c + " values diff: [" +
                        getValueString(r,c) + "] and [" + other.getValueString(r,c) + "]");
                    diffs++;
                }
            }
        }
        if ( diffs == 0 ) {
            General.showOutput("Found no differences between the two tables." );
        } else {
            General.showOutput("Found number of differences between the two tables: " + diffs);
        }
    }

    /**Default is to show header.
     */
    public String toString( ) {
        return( toString( true ) );
    }

    /**
     * @param show_header
     * @return  string representation of the header.*/
    public String toString( boolean show_header ) {

        int rowCount = sizeRows();
        int columnCount = sizeColumns();
        if ( columnCount < 1 )
            return ("--No columns--");

        StringBuffer sb = new StringBuffer();
        // Header
        if (show_header) {
            sb.append("---  Table View ---\n");
            for (int i=0;i<columnCount;i++) {
                sb.append(labels.get(i));
                if ( i < ( columnCount - 1 ) )
                    sb.append(",");
                else
                    sb.append(General.eol);
            }
        }

        if ( rowCount < 1 ) {
            sb.append("--Empty table-- (" + columnCount + " columns but no rows)\n");
            return (sb.toString() );
        }

        // Rows
        for (int r=0;r<rowCount;r++) {
            for (int i=0;i<columnCount;i++) {
                Object o = getValue(r,i);
                if ( o != null )
                    sb.append( o.toString() );
                if ( i < ( columnCount - 1 ) )
                    sb.append(",");
                else
                    sb.append(General.eol);
            }
        }

        return (sb.toString() );
    }



    /**
     * Get the html code back overriding the superclasses' method*/
    public String toHtml() {
        return( toHtml(true) );
    }

    /**
     *Get the html code back overriding the superclasses' method
     * @param show_header
     */
    public String toHtml( boolean show_header ) {

        /** Table attributes */
        Properties table_attributes = new Properties();

        table_attributes.setProperty( "cellpadding", "2");
        table_attributes.setProperty( "cellspacing", "2");
        table_attributes.setProperty( "border", "1");

        int columnCount = sizeColumns();
        int rowCount = sizeRows();

        // Table
        if ( columnCount < 1 ) {
            return null;
        }

        StringBuffer sb = new StringBuffer("<TABLE ");
        sb.append( Strings.toHtml( table_attributes ) );
        sb.append( ">\n" );

        if (show_header) {
        // Header row
        // No row attributes
            sb.append( "<TR>\n" );
            for (int i=0;i<columnCount;i++) {
                sb.append( "<TD>\n" );
                sb.append(labels.get(i));
                sb.append( "</TD>\n" );
            }
            sb.append( "</TR>\n" );
        }

        if ( rowCount < 1 ) {
            sb.append( "</TABLE>\n" );
            return (sb.toString() );
        }

        // Rows
        for (int r=0;r<rowCount;r++) {
            sb.append( "<TR>\n" );
            for (int i=0;i<columnCount;i++) {
                sb.append( "<TD>\n" );
                Object o = getValue(r,i);
                if ( o != null )
                    sb.append( o.toString() );
                sb.append( "</TD>\n" );
            }
            sb.append( "</TR>\n" );
        }

        sb.append( "</TABLE>\n" );
        return (sb.toString() );
    }



    /**
     *Routine that becomes interesting when overriden. E.g. in the htmltable
     *variant the html code gets filtered out.
     */
    public String toCsv( String value ) {
        return( value );
    }

    /**
     *Assume we want a header row.
     */
    public String toCsv() {
        return( toCsv( true ) );
    }

    /** Uses third-party code for rendering.
     * @see com.Ostermiller.util.CSVPrinter
     */
    public String toCsv( boolean show_header ) {

        // String the writers together.
        StringWriter sw = new StringWriter();
        CSVPrinter csvOut = new CSVPrinter(sw);

        int columnCount = sizeColumns();
        int rowCount = sizeRows();

        // Table
        if ( columnCount < 1 )
            return null;

        // Header row
        if (show_header) {
            String[] cella = new String[columnCount];
            for (int c=0;c<columnCount;c++)
                cella[c] = toCsv(labels.get(c).toString());
            csvOut.println( cella );
        }

        // Rows
        if ( rowCount > 0 ) {
            String[] cella = new String[columnCount];
            for (int r=0;r<rowCount;r++) {
                for (int c=0;c<columnCount;c++) {
                    Object o = getValue(r,c);
                    String value = "";
                    if ( o != null ) {
                        value = o.toString();
                    }
                    cella[c] = toCsv( value );
                }
                csvOut.println( cella );
                if ( ((r %10000) == 0) && ( r != 0) ) {
                    General.showDebug("CSV Printed rows: " + (r+1));
                }
            }
        }
        return sw.toString();
    }


    /** Self test;
     * @param args Ignored.
     */
    public static void main (String[] args) {
        // constructor
//        if ( false ) {
//            Table t = new Table(3,3);
//            t.setValueByTable("z");
//            t.setValue( 0,0, "H<B>2\n\nx");
//            t.setValue( 2,2, new Integer(1) );
//            General.showOutput(t.toCsv());
//        }
//        if ( false ) {
//            Table t = new Table(3,3);
//            t.setValue( 0,0, "test");
//            Table s = (Table) t.clone();
//            s.setValue( 0,0, null);
//
//            General.showOutput(t.toString());
//            General.showOutput(s.toString());
//        }
//        if ( false ) {
//            Table t = new Table(2,2);
//            t.setValueByColumn(0,"zero");
//            t.setValueByColumn(1,"one");
//            t.setLabel(1, "MyLabel_99");
//            t.insertColumn(0);
//            General.showOutput(t.toString());
//            t.removeColumn(0);
//            General.showOutput(t.toString());
//        }
//        if ( false ) {
//            Table t1 = new Table(2,2);
//            Table t2 = new Table(0,0);
//            t1.setValueByColumn(0,"zero");
//            t1.setValueByColumn(1,"one");
//            /**
//            t2.setValueByColumn(0,"zero");
//            t2.setValueByColumn(1,"one");
//             */
//            //t2.setValue(0,1,"testing");
//            General.showOutput("Table t1: " + t1);
//            General.showOutput("Table t2: " + t2);
//            General.showOutput("Tables by equalsByStringRepresentation: " +
//                t1.equalsByStringRepresentation( t2 ) );
//        }
//        if ( false ) {
//            Table t1 = new Table(2,3);
//            t1.setValue(0,0,"a");
//            t1.setValue(0,1,"x");
//            t1.setValue(0,2,"c");
//            t1.setValue(1,0,"d");
//            t1.setValue(1,1,"e");
//            t1.setValue(1,2,"f");
//            General.showOutput("Table t1: " + t1);
//            int[] column_priorities = {1,0 };
//            t1.sortRowsAscii(column_priorities);
//            //t1.swapValues( 0,0,1,1 );
//            General.showOutput("Table t1: " + t1);
//        }
//        if ( false ) {
//            Table t1 = new Table(2,3);
//            t1.setValue(0,0,"a");
//            t1.setValue(0,1,"9");
//            t1.setValue(0,2,"c");
//            t1.setValue(1,0,"d");
//            t1.setValue(1,1,"2");
//            t1.setValue(1,2,"f");
//            General.showOutput("Table t1: " + t1);
//            //t1.sortRowsPercentage(1);
//            boolean ascending = false;
//            t1.sortRowsNumber(1, ascending);
//            General.showOutput("Table t1: " + t1);
//        }
//        if ( false ) {
//            Table t1 = new Table(2,3);
//            Table t2 = new Table(2,3);
//            t1.setValue(0,0,"a");
//            t2.setValue(0,0,"b");
//            General.showOutput("Table t1: " + t1);
//            General.showOutput("Table t2: " + t2);
//            General.showOutput("Table are the same is: " + t1.equalsByStringRepresentation(t2));
//        }
//        if ( false ) {
//            Table t1 = new Table(2,3);
//            t1.setValue(0,0,"n/a");
//            t1.setValue(0,1,"n/a");
//            t1.setValue(0,2,"n/a");
//            t1.setValue(1,0,"n/a");
//            t1.setValue(1,1,"n/a");
//            t1.setValue(1,2,"n/a");
//            General.showOutput("Table t1: " + t1);
//            t1.replaceStringByTable("n/a","");
//            General.showOutput("Table t1: " + t1);
//        }
        if ( true ) {
            Table t1 = new Table(2,3);
            t1.setValue(0,0,"1");
            t1.setValue(0,1,"n/a");
            t1.setValue(0,2,"n/a");
            t1.setValue(1,0,"2");
            t1.setValue(1,1,"n/a");
            t1.setValue(1,2,"n/a");
            General.showOutput("Table t1: " + t1);
            t1.keepRowsFromTo(1,2);
            General.showOutput("Table t1: " + t1);
        }

        General.showOutput("Done all selected tests!" );
    }
}
