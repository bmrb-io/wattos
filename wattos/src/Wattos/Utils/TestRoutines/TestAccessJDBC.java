/*
 * TestAccessJDBC.java
 *
 * Created on June 12, 2004, 4:51 PM
 */

package Wattos.Utils.TestRoutines;

import java.sql.*;

/**
 *
 * @author Jurgen F. Doreleijers
 */
public class TestAccessJDBC {
    
    /** Creates a new instance of TestAccessJDBC */
    public TestAccessJDBC() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        {
            // change this to whatever your DSN is
            String dataSourceName = "mrgrid"; // A datasource that was enabled through Windows
            // from windows control panel
            //      from data source
            //          from mrgrid mdb microsoft database file mrgrid.mdb
            //
            String dbURL = "jdbc:odbc:" + dataSourceName;
            try {
                Class.forName("sun.jdbc.odbc.JdbcOdbcDriver");
                Connection con = DriverManager.getConnection(dbURL, "","");
               Statement s = con.createStatement();
               
                s.execute("create table TEST12345 ( column_name integer )"); // create a table
                s.execute("insert into TEST12345 values(1)"); // insert some data into the table 
                s.execute("select column_name from TEST12345"); // select the data from the table
                ResultSet rs = s.getResultSet(); // get any ResultSet that came from our query
                if (rs != null) // if rs == null, then there is no ResultSet to view
                while ( rs.next() ) // this will step through our data row-by-row
                    {
                    /* the next line will get the first column in our current row's ResultSet 
                    as a String ( getString( columnNumber) ) and output it to the screen 
                     */
                    System.out.println("Data from column_name: " + rs.getString(1) );
                }
                s.execute("drop table TEST12345");
                
                s.close(); // close the Statement to let the database know we're done with it
                con.close(); // close the Connection to let the database know we're done with it
            }
            catch (Exception err) {
                System.out.println( "Error: " + err );
            }            
        }
    }
}
