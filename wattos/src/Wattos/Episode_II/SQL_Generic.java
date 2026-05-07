/*
 * GenericSQL.java
 *
 * Created on December 5, 2001, 1:51 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Episode_II;

import java.text.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import Wattos.Utils.*;

/** Enables generic database connection.
 * Adapted from Ian Darwin's Java Cookbook (O'Reilly).
 * @author Ian Darwin
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class SQL_Generic {

    /** Evaluates to true in SQL but can be replace by other code
     */
    public static final String STUB_SQL_STRING_TRUE = "1=1";

    /** Maximum number of characters in a varchar2 object in the database.
     */
    public static final int MAX_CHARS_VARCHAR2 = 255;
    //public static final int MAX_CHARS_VARCHAR2 = 4000; for Oracle

    /** Some differences between the 2:
     *mysql is case sensitive to identifiers on systems like Unix.
     *SQL keywords in the code should be upper case. All other identifiers for
     *tables and columns should be lower case.
     */
    public static final int DB_VENDOR_MYSQL  = 0;
    public static final int DB_VENDOR_ORACLE = 1;

    public static int DB_VENDOR_USED = DB_VENDOR_MYSQL;
    //public static int DB_VENDOR_USED = DB_VENDOR_ORACLE;

    /** The connection object.
     *Has to be visible to sub classes.
     *In servlet (pooled) mode this stays null; callers should go through
     *{@link #getConnection()}, which borrows from {@link #dataSource} per call.
     *In CLI (no-pool) mode this holds the single shared DriverManager
     *connection that the legacy CLI methods still use directly, and
     *{@link #getConnection()} returns a non-closing proxy over it so the
     *try-with-resources idiom in pool-aware methods is also safe.
     */
    public Connection           conn   = null;

    /** JNDI name the servlet looks up to get a pooled DataSource.
     *Defined in {@code wattos/wattos/NRG/META-INF/context.xml}. */
    public static final String JNDI_NAME = "java:comp/env/jdbc/wattos";

    /** Set in pooled (servlet) mode; null in CLI fallback mode. */
    protected DataSource dataSource = null;
    /*Zsolt Zolnai suggested to keep these objects definitions around but
     *the garbage collector was probably not taking them of the stack
     *soon enough because when loading ~200 files (mrfiles) the DB would
     *complain saying no more available cursors even though we're supposed
     *to have in the order of 50-100 enabled. No real benefit
     *for this alternative?
    public PreparedStatement    pstmt  = null;
    public Statement            stmt   = null;
    public ResultSet            rs     = null;
    */


    // Sometimes operations should be carried out on somewhat temporary tables.
    // This string will be prepended to most table names.
    // This excluded the sequences for instance because they should be unique
    // over both regular and temporary tables.
    // The variable will be set in the application code.
    /// E.g. see: MRConvert.main.
    public String SQL_table_prefix =  "";

    /** Creates new DbConnection
     */
    public SQL_Generic(Globals globals) {

        Properties db_properties = new Properties();
        db_properties.setProperty( "db_conn_string",globals.getValueString( "db_conn_string" ));
        db_properties.setProperty( "db_username",   globals.getValueString( "db_username" ));
        db_properties.setProperty( "db_driver",     globals.getValueString( "db_driver" ));
        db_properties.setProperty( "db_password",   globals.getValueString( "db_password" ));

        init( db_properties );
    }

    /** Creates new DbConnection
     * @param properties holds: user id, password, etc.
     */
    public SQL_Generic(Properties properties) {
        init( properties );
    }

    public void init(Globals globals) {

        Properties db_properties = new Properties();
        db_properties.setProperty( "db_conn_string",globals.getValueString( "db_conn_string" ));
        db_properties.setProperty( "db_username",   globals.getValueString( "db_username" ));
        db_properties.setProperty( "db_driver",     globals.getValueString( "db_driver" ));
        db_properties.setProperty( "db_password",   globals.getValueString( "db_password" ));

        init( db_properties );
    }

    public void init(Properties properties) {

        // Servlet path: pull a pooled DataSource from JNDI. Configured
        // in WEB-INF/context.xml; Tomcat owns the pool.
        try {
            Context ctx = new InitialContext();
            Object looked_up = ctx.lookup(JNDI_NAME);
            if (looked_up instanceof DataSource) {
                dataSource = (DataSource) looked_up;
                General.showDebug("Got pooled DataSource via JNDI: " + JNDI_NAME);
                return;
            }
            General.showDebug("JNDI " + JNDI_NAME + " resolved to unexpected type: " + looked_up);
        } catch (Throwable t) {
            // Expected in CLI mode (no servlet container, no java:comp/env).
            General.showDebug("No JNDI DataSource (CLI mode): " + t.getMessage());
        }

        // CLI fallback: open one shared Connection up front.
        String db_conn_string   = properties.getProperty( "db_conn_string" );
        String db_username      = properties.getProperty( "db_username" );
        String db_driver        = properties.getProperty( "db_driver" );
        String db_password      = properties.getProperty( "db_password" );

        try
        {
            General.showDebug("Loading Driver (with Class.forName): " + db_driver);
            Class.forName(db_driver);

            General.showDebug("Getting Connection");
            General.showDebug("db_conn_string :" + db_conn_string);
            General.showDebug("db_username    :" + db_username);
            General.showDebug("db_password    :" + db_password);

            conn = DriverManager.getConnection (
                    db_conn_string, db_username, db_password);

            checkForWarning(conn.getWarnings());
            conn.setAutoCommit( true );

        } catch (ClassNotFoundException e) {
            General.showError("Can't load driver in method: SQL_Generic.init" );
            General.showThrowable(e);
            return;
        } catch (SQLException e) {
            General.showError("Database access failed in method: SQL_Generic.init" );
            General.showThrowable(e);
            return;
        }

        General.showDebug("Done initializing ... DbConnection");
    }

    /** Borrow a Connection. Pooled callers must close it (try-with-resources)
     *to return it to the pool; the close() is a no-op for the CLI-mode shared
     *connection so the same idiom is safe in both modes.
     *@throws SQLException if no Connection is available.
     */
    public Connection getConnection() throws SQLException {
        if (dataSource != null) {
            return dataSource.getConnection();
        }
        if (conn != null) {
            return wrapNonClosing(conn);
        }
        throw new SQLException("SQL_Generic has no DataSource and no fallback Connection");
    }

    /** Returns a Connection proxy whose close() is a no-op. Used in CLI mode
     *so per-call try-with-resources blocks don't tear down the shared conn.
     */
    private static Connection wrapNonClosing(final Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[] { Connection.class },
                new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("close".equals(method.getName())) {
                            return null;
                        }
                        return method.invoke(delegate, args);
                    }
                });
    }

    /** Just a wrapper around the setAutoCommit function of the connection object
     *in this class.
     */
    public void setAutoCommit(boolean status) {

        try {
            conn.setAutoCommit( status );
        } catch (SQLException e) {
            General.showError("Database access failed in method: setAutoCommit\n" + e);
            return;
        }
    }


    /** Taken from Java programming with Oracle JDBC.
     * Great the string works with both Oracle and MySQL
     */
    public static String formatWithSql92Date( java.util.Date date ) {
        if ( date != null ) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            return"{ts '" + sdf.format(date) + "'}";
        } else {
            return "NULL";
        }
    }

    /** Close the database connection.
     *In pooled (servlet) mode the DataSource lifecycle is managed by Tomcat,
     *so we just drop our reference. In CLI mode we close the shared conn.
     */
    public void closeConnection()
    {
        if (dataSource != null) {
            General.showOutput("Releasing pooled DataSource reference for class genericSQL.");
            dataSource = null;
            return;
        }
        if (conn == null) {
            return;
        }
        General.showOutput("Closing connection for class genericSQL.");
        try  {
            conn.close();
        }
        catch (SQLException e)
        { General.showWarning("Database connection closing failed " + e); }
    }

    /** Rollback the connection in clean way. Resetting the autocommit parameter
     *to it's default value of true. Adapted from Sun's code: TransactionPairs in
     *the DB tutorial.
     */
    public boolean rollbackTransaction()
    {
        boolean status = true;
        if (conn != null)
        {
            try {
                General.showWarning("Transaction is being rolled back");
                conn.rollback();
            } catch( SQLException e) {
                General.showError("in SQL_Generic.rollbackTransaction found SQLException (1): ");
                General.showError( e.getMessage() );
                status = false;
            }
            // Be stubborn and try to restore default committing parameter in
            // any situation.
            try {
                conn.setAutoCommit(true);
            } catch(SQLException e) {
                General.showError("in SQL_Generic.rollbackTransaction found SQLException (2): ");
                General.showError( e.getMessage());
                status = false;
            }
        } else
        {
            General.showError("in SQL_Generic.rollbackTransaction found Transaction could not be rolled back.");
            General.showError("Auto commit not reset to default value.");
            status = false;
        }
        return(status);
    }


    /** Format and print any warnings from the connection.
     * @param warn The warnings.
     * @throws SQLException The exception that it will throw when additional
     *things would go wrong.
     */
    protected static void checkForWarning(SQLWarning warn) throws SQLException
    {
         // If a SQLWarning object was given, display the
         // warning messages.  Note that there could be
         // multiple warnings chained together
         if (warn != null) {
             General.showWarning(General.eol);
             while (warn != null) {
                     General.showOutput("SQLState: " +
                             warn.getSQLState()+General.eol);
                     General.showOutput("Message:  " +
                             warn.getMessage()+General.eol);
                     General.showOutput("Vendor:   " +
                             warn.getErrorCode()+General.eol);
                     General.showOutput(""+General.eol);
                     warn = warn.getNextWarning();
             }
         }
    }


    /** Test the connection by running a trivial query.
     *The pool already validates on borrow (see context.xml validationQuery),
     *so this is mostly a smoke test for init-time / CLI use.
     *Returns true for success or false for any error.
     */
    public boolean testConnection()
    {
        boolean status = true;

        General.showDebug("Testing connection for class SQL_Generic.");

        if (dataSource == null && conn == null) {
            return false;
        }

        String query;
        if ( SQL_Generic.DB_VENDOR_USED == SQL_Generic.DB_VENDOR_MYSQL ) {
            query = "SELECT sysdate()";
        } else if ( SQL_Generic.DB_VENDOR_USED == SQL_Generic.DB_VENDOR_ORACLE ) {
            query = "SELECT sysdate FROM dual";
        } else {
            General.showError("Set DB vendor isn't a supported ID: " + SQL_Generic.DB_VENDOR_USED);
            return false;
        }

        try (Connection c = getConnection();
             Statement stmt = c.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (!rs.next()) {
                General.showError("in SQL_Generic.testConnection found: no result for query\n");
                status = false;
            } else {
                java.util.Date now = rs.getDate(1);
                General.showDebug("Database system date is: " + now.toString());
            }
            checkForWarning(c.getWarnings());
        } catch (SQLException e) {
            General.showError("in SQL_Generic.testConnection found: Database access failed\n" + e);
            return false;
        }
        return status;
    }


    /** Close the database connection. Returns true for success or false for any error.
     */
    public boolean testCache()
    {
        boolean status = true;

        General.showDebug("Testing cache for class SQL_Generic.");

        if ( conn == null ) {
            return false;
        }

        try
        {
            // Gets cached
            //String query = "SELECT * FROM MRBLOCK"; // mysql
            /** Doesn't get cached because of reference to temporary table.
String query =
"SELECT t.program, t.type, t.subtype, t.format, count(*) AS count\n"+
"FROM (\n"+
"        SELECT b.program, b.type, b.subtype, b.format, b.mrfile_id\n"+
"        FROM mrfile f, mrblock b\n"+
"        WHERE f.mrfile_id=b.mrfile_id AND\n"+
"            1=1\n"+
"        GROUP BY b.program, b.type, b.subtype, b.format, b.mrfile_id\n"+
"        ) t\n"+
"GROUP BY t.program, t.type, t.subtype, t.format";
*/
            // solution gets cached nicely.
String query =
"SELECT b.program, b.type, b.subtype, b.format, count(*)\n"+
"FROM mrfile f, mrblock b\n"+
"WHERE f.mrfile_id=b.mrfile_id AND\n"+
"1=1\n"+
"GROUP BY b.program, b.type, b.subtype, b.format";


            for (int i=0;i<5;i++) {
                Statement stmt = conn.createStatement();
                long timeStart = System.currentTimeMillis();
                ResultSet rs = stmt.executeQuery( query );
                long timeTaken = System.currentTimeMillis() - timeStart;
                General.showOutput(timeTaken + " " + rs.toString() );
                // Do these need to be executed before a return, or is it just
                // nice to do so?
                rs.close();
                stmt.close();     // All done with that resultset
            }
            // Any warnings generated by the sql connection?
            checkForWarning(conn.getWarnings());
        } catch (SQLException e)
        {
            General.showError("in SQL_Episode_II.testCache found: Database access failed\n" + e);
            // Value might have been changed so we use the real thing
            // in order to signal error upon return.
            return false;
        }
        return status;
    }




    /** Self test; opens a connection and closes it again.
     * @param args Command line arguments; ignored
     */
    public static void main (String[] args) {
        General.showOutput("Starting test of check routine." );
        General.verbosity = General.verbosityDebug;

        if ( true ) {
            Globals globals = new Globals();
            // Open SQL_Generic database connection
            SQL_Generic sql_g = new SQL_Generic(globals);
            General.showOutput("Opened generic sql connection:" + sql_g );
            sql_g.testCache();
            sql_g.closeConnection();
        }
//        if ( false ) {
//            // Great the string works with both Oracle and MySQL
//            String sdf = formatWithSql92Date( new java.util.Date());
//            General.showOutput( sdf );
//        }
        General.showOutput("Finished all selected check routines." );
    }

    /**
     * Null a column for each row.
     */
    public boolean nullColumn(String tableName, String columnName) {
        String q =
        "UPDATE " + tableName + General.eol+
        "SET " + columnName + " = NULL";
        try {
            General.showOutput("Using query:["+q+"]");
            Statement stmt = conn.createStatement();
            stmt.executeUpdate(q);
            // Do these need to be executed before a return, or is it just
            // nice to do so?
            stmt.close();     // All done with that result set
            // Any warnings generated by the sql connection?
            checkForWarning(conn.getWarnings());
        } catch (SQLException e) {
            General.showError("in SQL_Generic.nullColumn found Database access failed\n" + e);
            General.showError("Full query is: \n" + q);
            // Value might have been changed so we use the real thing
            // in order to signal error upon return.
            return false;
        }

        return true;
    }

    /**
     * Clear all BMRB entry id links for each entry.
     */
    public boolean setValue(String tableName,
        String columnToUpdate, String columnToCheck, String valueToCheck,
        Object valueObject) {

        String value = null;
        if ( valueObject instanceof String ) {
            value = (String) valueObject;
        } else if ( valueObject instanceof Integer ) {
            value = ((Integer)valueObject).toString();
        } else if ( valueObject instanceof Float ) {
            value = ((Integer)valueObject).toString();
        } else if ( valueObject instanceof Boolean ) {
            boolean v = ((Boolean)valueObject).booleanValue();
            value = "0";
            if ( v ) {
                value = "1";
            }
        } else {
            General.showError("setValue only coded for values of types: String,Integer,Float, and Boolean");
            return false;
        }

        String q =
        "UPDATE " + tableName +General.eol+
        "SET   " + columnToUpdate + " = "  + value +General.eol+
        "WHERE " + columnToCheck  + " = '" + valueToCheck + "'\n";
        try {
            //General.showOutput("Using query:["+q+"]");
            Statement stmt = conn.createStatement();
            int resultCount = stmt.executeUpdate(q);
            /**if ( resultCount > 0 ) {
                General.showDebug("Updated pdb_id " + pdb_id + " to bmrb id: " + bmrb_id);
            }
             */
            if ( resultCount > 1 ) {
                General.showError("Should have updated at one entry but did more.");
                return false;
            }
            // Do these need to be executed before a return, or is it just
            // nice to do so?
            stmt.close();     // All done with that result set
            // Any warnings generated by the sql connection?
            checkForWarning(conn.getWarnings());
        } catch (SQLException e) {
            General.showError("in SQL_Generic.setBoolean found Database access failed\n" + e);
            General.showError("for query:\n" + q);
            // Value might have been changed so we use the real thing
            // in order to signal error upon return.
            return false;
        }

        return true;
    }

}
