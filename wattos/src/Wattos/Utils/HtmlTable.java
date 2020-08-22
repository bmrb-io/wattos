/*
 * Created on February 4, 2002, 4:56 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Utils;

import java.util.*;

/** Using the HtmlTable class allows for easier conversions to html,
 * text, and csv for instance. In addition some common operations are
 * more logically supported.
 * Too lazy to program all set/getters.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class HtmlTable extends Table {
    private static final long serialVersionUID = 2347130337008786118L;

    /** Table attributes
     For all attributes it holds that the value must be quoted in the string
     itself if needed. E.g. Use "2" and "\"two\"".
     Only string classes allowed, don't use null references.
     */
    public Properties table_attributes = new Properties();

    /** Header cell attributes, only 1 header row possible and obligatory,
     make sure to add the second dimension when adding columns
     Only string classes allowed, don't use null references.
     */
    public ArrayList header_cell_attributes = new ArrayList();

    /** Cell attributes, make sure to add the second dimension when
     adding rows, columns.
     *     Only string classes allowed, don't use null references.
     */
    public ArrayList cell_attributes = new ArrayList();

    /**Using this signals the toHtml code not to display this cell.*/
    static final public String NOT_PRINTABLE_CELL_HTML_TABLE = "<NOT_PRINTABLE_CELL_HTML_TABLE>";
    // Using this instead of an empty string makes the cell show up filled.
    static final public String EMPTY_CELL_HTML_TABLE = "<BR>";

    public void init() {
        super.init();
        table_attributes = new Properties();
        header_cell_attributes = new ArrayList();
        cell_attributes = new ArrayList();
    }


    /** Creates new DbTable */
    public HtmlTable() {
        this(0,0);
    }

    public HtmlTable( int nrows, int ncols) {
        super(nrows, ncols);

        table_attributes = new Properties();
        header_cell_attributes = new ArrayList();
        cell_attributes = new ArrayList();

        for (int c=0; c < ncols; c++) {
            header_cell_attributes.add( new Properties() );
            ArrayList column_cell_attributes = new ArrayList();
            for (int r=0; r < nrows; r++) {
                column_cell_attributes.add(new Properties());
            }
            cell_attributes.add( column_cell_attributes );
        }
    }

    public HtmlTable( Table table ) {

        super(table);

        int nrows = table.sizeRows();
        int ncols = table.sizeColumns();

        table_attributes = new Properties();
        header_cell_attributes = new ArrayList();
        cell_attributes = new ArrayList();

        for (int c=0; c < ncols; c++) {
            header_cell_attributes.add( new Properties() );
            ArrayList column_cell_attributes = new ArrayList();
            for (int r=0; r < nrows; r++) {
                column_cell_attributes.add(new Properties());
            }
            cell_attributes.add( column_cell_attributes );
        }
    }

    /** OK they're not common enough to warrant inclusion in constructor
     */
    public void setCommonDefaults() {
        super.setCommonDefaults();
        table_attributes.setProperty( "cellpadding", "2");
        table_attributes.setProperty( "cellspacing", "2");
        table_attributes.setProperty( "border", "1");
    }



    /** No extra range checking done for efficiency
     plural
     */
    public Properties getCellAttributes( int row, int column) {
        return ( (Properties) ((ArrayList) cell_attributes.get(column)).get(row));
    }

    /** No extra range checking done for efficiency
     singular
     */
    public void setCellAttribute( int row, int column, String key, String value) {
        ArrayList al = (ArrayList) cell_attributes.get(column);
        Properties p = (Properties) al.get(row);
        p.setProperty(key, value);
    }

    /** No extra range checking done for efficiency
     plural
     */
    public Properties getHeaderCellAttributes(int column) {
        return ( (Properties) header_cell_attributes.get(column) );
    }

    /** No extra range checking done for efficiency
     singular
     */
    public void setHeaderCellAttribute( int column, String key, String value) {
        ( (Properties) header_cell_attributes.get(column)).setProperty(key, value);
    }



    /** Some range checking done. No error message printed though.
     *Use -1 for column to act on header.
     */
    public void setValueHtmlCode( int row, int column,
    String start_code, String end_code ) {
        if ( row < -1 ) {
            return;
        }
        if ( row == -1 ) {
            setLabel( column,
                start_code +
                getLabel( column ) +
                end_code
            );
        } else {
            setValue( row, column,
                start_code +
                getValue( row, column ) +
                end_code
            );
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void setValueHtmlCodeByColumn( int column,
        String start_code, String end_code ) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( column < 0 || column >= columnCount )
            return;
        for (int r=0;r<rowCount;r++) {
            setValueHtmlCode( r, column, start_code, end_code);
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void setValueHtmlCodeByRow( int row,
        String start_code, String end_code ) {
        int columnCount = sizeColumns();
//        int rowCount = sizeRows();
        if ( row < -1 ) {
            return;
        }
        for (int c=0;c<columnCount;c++) {
            setValueHtmlCode( row, c, start_code, end_code);
        }
    }



    /** Some range checking done. No error message printed though.*/
    public void setCellAttributeByColumn( int column, String key, String value) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( column < 0 || column >= columnCount )
            return;
        for (int r=0;r<rowCount;r++) {
            setCellAttribute( r, column, key, value);
        }
    }

    /** Some range checking done. No error message printed though.*/
    public void setCellAttributeByRow( int row, String key, String value) {
        int columnCount = sizeColumns();
//        int rowCount = sizeRows();
        if ( row < 0 )
            return;
        for (int c=0;c<columnCount;c++)
            setCellAttribute( row, c, key, value);
    }

    /** Some range checking done. No error message printed though.*/
    public void setCellAttributeByTable( String key, String value) {
        int columnCount = sizeColumns();
        int rowCount = sizeRows();
        if ( rowCount < 1 )
            return;
        for (int c=0;c<columnCount;c++)
            setCellAttributeByColumn( c, key, value);
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

        int rowCount = sizeRows();

        ArrayList cell_att = new ArrayList();
        for (int r=0; r < rowCount; r++)
            cell_att.add( new Properties() );
        cell_attributes.add( column, cell_att);

        header_cell_attributes.add( column, new Properties() );
        return true;
    }


    /** Some range checking done. No error message printed though.
     *Inserts an extra column before the given position.
     */
    public boolean removeColumn( int column ) {

        if ( ! super.removeColumn( column ) ) {
            return false;
        }

        cell_attributes.remove( column );
        header_cell_attributes.remove( column );
        return true;
    }


    // Get the html code back overriding the superclasses' method
    public String toHtml( ) {
        return toHtml( true );
    }

    // Get the html code back overriding the superclasses' method
    public String toHtml( boolean show_header ) {

        int columnCount = sizeColumns();
        int rowCount = sizeRows();

        if ( columnCount < 1 ) {
            return( "Empty HtmlTable" );
        }

        // Table
        StringBuffer sb = new StringBuffer("<TABLE ");
        sb.append( Strings.toHtml( table_attributes ) );
        sb.append( ">\n" );

        // Header row
        // No row attributes
        if ( show_header ) {
            sb.append( "<TR>\n" );
            for (int i=0;i<columnCount;i++) {
                sb.append( "<TD " );
                sb.append( Strings.toHtml( getHeaderCellAttributes(i) ));
                sb.append( ">\n" );
                String value_str = (String) labels.get(i);
                if ( value_str.equals("") ) {
                    value_str = EMPTY_CELL_HTML_TABLE;
                }
                sb.append(value_str);
                sb.append( "</TD>\n" );
            }
            sb.append( "</TR>\n\n" );
        }

        if ( rowCount < 1 ) {
            sb.append( "</TABLE>\n\n" );
            return (sb.toString() );
        }

        // Rows
        for (int r=0;r<rowCount;r++) {
            sb.append( "<TR>\n" );
            for (int i=0;i<columnCount;i++) {
                Object o = getValue(r,i);
                // Special case to enabling to have multiple cells rendered nicely.
                if ( o == null )
                    continue;
                String value_str = o.toString();
                if ( value_str.equals(NOT_PRINTABLE_CELL_HTML_TABLE) )
                    continue;
                if ( value_str.equals("") ) {
                    value_str = EMPTY_CELL_HTML_TABLE;
                }
                sb.append( "<TD " );
                sb.append( Strings.toHtml( getCellAttributes(r,i)) );
                sb.append( ">\n" );
                sb.append( value_str);
                sb.append( "</TD>\n" );
            }
            sb.append( "</TR>\n\n" );
        }

        sb.append( "</TABLE>\n\n\n" );
        return (sb.toString() );
    }


     /** Strips the html code from a string, overrides the super class method
      which doesn't do a thing.*/
    public String toCsv( String value ) {
        return( Strings.stripHtml(value) );
    }

    /** Self test;
     * @param args Ignored.
     */
    public static void main (String[] args) {
        // constructor
//        if ( false ) {
//            HtmlTable htmltable = new HtmlTable(3,3);
//            htmltable.setCommonDefaults();
//            htmltable.labels.set(1, "Testing label 1");
//            htmltable.table_attributes.setProperty( "width", "700" );
//            htmltable.setCellAttributeByColumn(2, "align", "\"Right\"" );
//
//            htmltable.setCellAttribute(0, 1, "rowspan", String.valueOf(htmltable.sizeRows()) );
//
//            htmltable.setValueByTable( EMPTY_CELL_HTML_TABLE );
//            htmltable.setValueByColumn(1, NOT_PRINTABLE_CELL_HTML_TABLE );
//            htmltable.setValue(0, 1, "Now we can write some very long message in multiple cells\n without getting clobbered all over the place.");
//            htmltable.setValue(2, 0, null);
//            htmltable.setValue(1, 0, "BMRB code");
//            String null_str = null;
//            htmltable.setValue(1, 1, null_str);
//            htmltable.setValueHtmlCode( 0, 0, "<A HREF=x>", "<A>");
//            htmltable.setValueHtmlCode( 0, 0, "<B>", "</B>");
//            General.showOutput( htmltable.toHtml() );
//            General.showOutput("[" + htmltable.toCsv() + "]");
//        }
//        if ( false ) {
//            Table table = new Table(2,2);
//            table.setValue(0, 0, "testt");
//
//            HtmlTable htmltable = new HtmlTable(table);
//            //htmltable.setCommonDefaults();
//            htmltable.setValue(0, 0, null);
//            //htmltable.setValueHtmlCode( 0, 0, "<A HREF=x>", "<A>");
//
//            General.showOutput( table.toHtml() );
//            General.showOutput( htmltable.toHtml() );
//
//        }
         if ( true ) {
            HtmlTable t = new HtmlTable(2,2);
            t.setValueByTable("TEST");
            t.setValue(0,0,"");
            t.setCellAttributeByTable("width", "99");
            t.insertColumn(0);
            General.showOutput(t.toHtml());
            t.removeColumn(0);
            General.showOutput(t.toHtml());
        }
       General.showOutput("Done all selected tests!" );
    }
}
