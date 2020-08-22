package Wattos.Gobbler.TargetScore;
import Wattos.Utils.*;
import Wattos.Database.*;
import hep.aida.*;
import hep.aida.ref.*;

/** CESG target selection scoring algorithm */
public class AnalyzeScore {
            

    public AnalyzeScore() {
    }
    

    
    public static void show_usage() {
        General.showOutput("USAGE: java -Xmx256m Wattos.Gobbler.TargetScore.AnalyzeScore \\");
        General.showOutput("  comparision_with_2005_02_28.csv");
        General.showOutput("Notes: File given should with 2 columns labelled with 'orgScore' and 'newScore' with only integers and a header line.");        
        System.exit(1);
    }
    
    public static void main(String[] args) {
        //General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityOutput;
        
        /** Checks of input */
        if ( args.length != 1 ) {
            General.showError("Need 1 arguments.");
            show_usage();
        }
        int i=0;
        String scoring_comp_file_name      = args[i++];

        boolean containsHeaderRow = true;

        DBMS dbms = new DBMS();
        Relation scoringComp = null;
        try {
            scoringComp = new Relation("scoringComp", dbms);
        } catch ( Exception e ) {
            General.showThrowable(e);
            System.exit(1);
        }
        
        General.showOutput("Reading file                              : " + scoring_comp_file_name );
        if ( ! scoringComp.readCsvFile(  scoring_comp_file_name, containsHeaderRow, null)) {
            General.doCodeBugExit("Failed to read scoringComp csv file: " + scoring_comp_file_name);
        }
        scoringComp.convertDataTypeColumn("orgScore", Relation.DATA_TYPE_INT, null);
        scoringComp.convertDataTypeColumn("newScore", Relation.DATA_TYPE_INT, null);
 
        IHistogram2D h2 = new Histogram2D("my histo 2D",10, 0, 10,    10, 0, 10);

//        int n = scoringComp.sizeRows;
        int r = scoringComp.used.nextSetBit(0);
        int[] score_org = scoringComp.getColumnInt("orgScore");
        int[] score_new = scoringComp.getColumnInt("newScore");
        while ( r >= 0 ) {
            h2.fill( score_org[r], score_new[r] );
            r = scoringComp.used.nextSetBit(r+1);
        }
        Converter c = new Converter();
        General.showOutput(c.toString(h2));
        
        General.showOutput("Done");
    }
}
