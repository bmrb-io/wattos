/*
 * MolImage.java
 *
 * Created on December 11, 2001, 11:45 AM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import java.util.*;
import Wattos.Utils.*;
/** Container for relational database scheme DBEntry entity.
 * Contains an inner class: <CODE>Retval</CODE> which is defined within for array
 * of different data types that is to be returned. See
 * {@link <a href="http://developer.java.sun.com/developer/TechTips/2000/tt1205.html">here<a>}
 * for the example where it was adapted from.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class DBEntry {

    // It's important that unknown values are null and not say 0
    /** Database id for the entry.
     */    
    int     entry_id    = Wattos.Utils.General.NULL_FOR_INTS; 
    /** BMRB id for the entry.
     */    
    int     bmrb_id     = Wattos.Utils.General.NULL_FOR_INTS;
    /** PDB entry code.
     */    
    String  pdb_id      = null; 

    /** See documentation for enclosing class
     */
    static class Retval {
        /** Database id for the entry.
         */        
        int       entry_id;
        /** BMRB id for the entry.
         */        
        int       bmrb_id;
        /** PDB entry code.
         */        
        String    pdb_id;
    }
    
    /** Creates new DBEntry.
     * Fetching only a new entry if DB didn't have an entry where pdb_id
     * is as given.
     * @param sql_epiII The database connection.
     * @param p_id The PDB entry code.
     */
    public DBEntry( SQL_Episode_II sql_epiII, String p_id ) {
        if ( ! Wattos.Utils.Strings.is_pdb_code( p_id ) ) {
            General.showError("in Classification.main found:");
            General.showError("Given id ["+p_id+
                "] doesn't look like a valid pdb id.");
            System.exit(1);
        }
            
        Retval ret = sql_epiII.getEntryByPDBId( p_id );
        if ( ret == null ) {
            General.showOutput("Failed to instantiate an instance of DBEntry from DB.");
            System.exit(1);
        }
        entry_id    = ret.entry_id;
        bmrb_id     = ret.bmrb_id;
        pdb_id      = ret.pdb_id;
    }    
    
    
    /** Returns one line describing the content of this instance.
     * @return Description of the dbentry.
     */
    public String toString() {
        return( "entry_id:" + entry_id + ",bmrb_id:" + bmrb_id + 
                ",pdb_id:" + pdb_id + General.eol );
    }
    
    
    /* Creating one anew with only entry_id filled in.
    public DBEntry( SQL_Episode_II sql_epiII  ) {        
        entry_id = sql_epiII.getEntryByPDBId( );
        if ( entry_id == Wattos.Utils.General.NULL_FOR_INTS )
            General.showOutput("Failed to instantiate an instance from DB." );
    }    
    */

    
    /** Self test. All disabled.
     * What is can do:
     * Opens db connection and checks to see if there already is an dbentry with
     * the same pdb_id as hard coded: "1brv". If not, it will create one.
     * @param args the command line arguments; ignored
     */
    public static void main (String args[]) {
        // Usually needed for all tests
        Globals globals = new Globals();

        // Open Episode_II database connection
        Properties db_properties = new Properties();        
        db_properties.setProperty( "db_conn_string",globals.getValueString( "db_conn_string" ));
        db_properties.setProperty( "db_username",   globals.getValueString( "db_username" ));
        db_properties.setProperty( "db_driver",     globals.getValueString( "db_driver" ));
        db_properties.setProperty( "db_password",   globals.getValueString( "db_password" ));                
        SQL_Episode_II sql_epiII = new SQL_Episode_II( db_properties );

        if ( true ) {
            General.showOutput("Starting test of constructor(s): DBEntry( sql_epiII, args[0] )" );
            DBEntry de = new DBEntry( sql_epiII, "1brv" );
            General.showOutputNoEol("The dbe looks like this:  "+de );
        } 
        General.showOutput("Done all selected tests!" );            
    }
}
