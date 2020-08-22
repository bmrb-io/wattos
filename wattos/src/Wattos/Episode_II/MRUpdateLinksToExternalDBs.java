package Wattos.Episode_II;

import java.io.*;
import java.util.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/**
 *Main program for updating the linkages from PDB to BMRB entries in the
 * database.
 *
 * @author  Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRUpdateLinksToExternalDBs {
    
    static SQL_Episode_II sql_epiII = null;
    
    static public void showUsage() {
        General.showOutput("Usage: java -Xmx128m Wattos.Episode_II.MRUpdateLinksToExternalDBs <resultDir>");
        System.exit(1);
    }
    
    
    
    public static void main(String[] args) {
        // Change some of the standard settings defined in the Globals class
        General.verbosity = General.verbosityOutput;
        
        if ( args.length != 1 ) {
            General.showError("Number of arguments should be 1; the results dir.");
            System.exit(1);
        }
        
        String resultDir = args[0];
        File resultDirF = new File(resultDir);
        if ( (! resultDirF.exists()) || !resultDirF.isDirectory() ) {
            General.showError("Given argument isn't a valid directory");
            System.exit(1);
        }
        Globals g = new Globals();
        //g.showMap();
        // Open Episode_II database connection
        Properties db_properties = new Properties();
        db_properties.setProperty( "db_conn_string",g.getValueString( "db_conn_string" ));
        db_properties.setProperty( "db_username",   g.getValueString( "db_username" ));
        db_properties.setProperty( "db_driver",     g.getValueString( "db_driver" ));
        db_properties.setProperty( "db_password",   g.getValueString( "db_password" ));
        sql_epiII = new SQL_Episode_II( db_properties );
        
        General.showOutput("Opened sql connection:" + sql_epiII );
        
        DBMS dbms = new DBMS();
        String[] relationNames = new String[] {
            "bmrb_main",
            "pdb_main",
            "score_many2one",
            "recoord",
            "dress"
        };
        File projectDir = resultDirF.getParentFile();
        File listDir = new File( projectDir, "lists");
        File dtdDir = new File( listDir, "dtd");
//         boolean checkConsistency = true;
        boolean checkConsistency = false;
        boolean showChecks = true;
        boolean status = dbms.readCsvRelationList( relationNames, resultDirF.toString(),
            dtdDir.toString(), checkConsistency, showChecks );
        if ( ! status ) {
            General.showError(      "For relations: " + relationNames);
            General.doCodeBugExit(  "couln't readRelationList from input dir: " + resultDirF.toString());
        }
        
        Relation score = dbms.getRelation("score_many2one");
        String[] columnsToUpdate = new String[] { "bmrb_id", "in_recoord", "in_dress" };
        for ( int i=0;i<columnsToUpdate.length;i++ ) {
            if ( sql_epiII.nullColumn("entry", columnsToUpdate[i])) {
                General.showOutput("Cleared column: "       + columnsToUpdate[i] + " in entry table");
            } else {
                General.showError("Failed to clear column: " + columnsToUpdate[i] + " in entry table");
                System.exit(1);
            }
        }
        
        int[] bmrb_id_list           = (int[])         score.getColumn("bmrb_id");
        String[] pdb_id_list         = (String[])      score.getColumn("pdb_id");
        General.showOutput("Updating BMRB links numbered: " + score.used.cardinality());
        String columnToUpdate = "bmrb_id";
        for (int score_rid=score.used.nextSetBit(0); score_rid>=0; score_rid=score.used.nextSetBit(score_rid+1)) {
            String pdb_id = pdb_id_list[score_rid];
            int bmrb_id = bmrb_id_list[score_rid];
            if ( ! sql_epiII.setValue("entry", columnToUpdate, "pdb_id", pdb_id, new Integer(bmrb_id)) ) {
                General.showError("Failed to update match for pdb_id: " + pdb_id + " to bmrb_id: " + bmrb_id);
                System.exit(1);
            }
        }

        Relation recoord = dbms.getRelation("recoord");
        Relation dress   = dbms.getRelation("dress");
        Relation[] relationToUpdate = new Relation[] {recoord,dress};        
        for ( int i=0;i<relationToUpdate.length;i++ ) {
            Relation relation = relationToUpdate[i];            
            String[] pdb_id_list2         = (String[]) relation.getColumn("pdb_id");
            General.showOutput("Updating links from relation " + relation.name + " numbered: " + relation.used.cardinality());
            columnToUpdate = "in_" + relation.name;
            for (int rid=relation.used.nextSetBit(0); rid>=0; rid=relation.used.nextSetBit(rid+1)) {
                String pdb_id = pdb_id_list2[rid];
                if ( ! sql_epiII.setValue("entry", columnToUpdate, "pdb_id", pdb_id, Boolean.TRUE) ) {
                    General.showError("Failed to do sql_epiII.setBoolean for pdb_id: " + pdb_id );
                    System.exit(1);
                }
            }
        }        
        General.showOutput("All done");
    }
}
