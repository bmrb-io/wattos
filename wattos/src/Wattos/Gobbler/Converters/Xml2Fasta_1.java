package Wattos.Gobbler.Converters;


import java.util.Hashtable;
import Wattos.Common.*;
import Wattos.Utils.*;
import Wattos.Utils.qdxml.*;

import java.io.*;

/** 
 *Parses PDB's target db entries from an xml file.
 *
  This class is the most basic possible
  implementation of the DocHandler class.
  It simply reports all events to System.out
  as they occur.
 * @see <A HREF="http://targetdb.pdb.org/target_files/targets.xml">example at targetdb</a>
*/
public class Xml2Fasta_1 implements DocHandler {

  // We only instantiate one copy of the DocHandler
  static Xml2Fasta_1 xml2fasta_1 = new Xml2Fasta_1();
  static BufferedWriter fw;
  static Orf orf = new Orf();
  static OrfId orfid = new OrfId(); 
  
  static String sequence;
  static String targetId;
  static String date;
  
  static private int currentItem;
  static private final int NEGLECT      = 0;
  static private final int SEQUENCE     = 1;
  static private final int TARGET_ID    = 2;
  static private final int LAB          = 3;
  static private final int STATUS       = 4;
  static private final int NAME         = 5;

    int total_sequence_count = 0;
    int total_sequence_letters_count = 0;
  
  // Implementation of DocHandler is below this line
  public void startDocument() {
      orf.init();
      orfid.init();
      orf.orf_id_list.orfIdList.add( orfid );
      //General.showOutput("  start document");
  }
  public void endDocument() {
      General.showOutput("Total number of sequences written        : " + total_sequence_count);
      General.showOutput("Total number of sequence letters written : " + total_sequence_letters_count);
  }
  public void startElement(String elem,Hashtable h) {
    //General.showOutput("    start elem: "+elem);
    if ( elem.equals("id") ) {
        currentItem = TARGET_ID;
    } else if ( elem.equals("lab") ) {
        currentItem = LAB;
    } else if ( elem.equals("status") ) {
        currentItem = STATUS;
    } else if ( elem.equals("sequence") ) {
        currentItem = SEQUENCE;
    } else if ( elem.equals("name") ) {
        currentItem = NAME;
    } else {
        currentItem = NEGLECT;
    } 
  }
  
  public void endElement(String elem) {
    //General.showOutput("    end elem: "+elem);
    try {
        if ( elem.equals("target") ) {        
            if ( doPrint( orf ) ) {
                fw.write( orf.toFasta() );
                total_sequence_count ++;
                total_sequence_letters_count += orf.sequence.length();
            }
            orfid.init();
        }
    } catch ( Throwable t ) {
        General.showThrowable(t);
    }
  }

  public void text(String text) {
    //General.showOutput("        text: "+text);
    switch ( currentItem ) {
        case NEGLECT:
            break;
        case SEQUENCE:
            /** Delete any white space within */
            orf.sequence = Strings.deleteAllWhiteSpace( text );
            currentItem = NEGLECT;
            break;
        case STATUS:
            if ( orfid.orf_db_subid.equals("") ) {                
                orfid.orf_db_subid = text;
            } else {
                orfid.orf_db_subid += ';' + text;
            }
            currentItem = NEGLECT;
            break;
        case LAB:
            orfid.orf_db_name = text.trim();
            currentItem = NEGLECT;
            break;
        case TARGET_ID:
            orfid.orf_db_id = text;
            //orfid.splitIdOnDot(); Can do this later...
            currentItem = NEGLECT;
            break;
        case NAME: // SPECIAL CASE TO CATCH CESG GO IDS which are populating the wrong tag.
            if ( orfid.orf_db_name.equalsIgnoreCase("cesg") ) {
                if ( ! text.startsWith("GO.") ) {
                    General.showError("Expected a CESG GO id but found in Name tag: [" + text + "]");
                    General.showError("Regular id will be used in stead but this will cause downstream problems.");
                    break;
                }
                orfid.orf_db_id = text;
                //orfid.splitIdOnDot(); Can do this later...
                currentItem = NEGLECT;                
            }
            break;
    }            
  }
  // implementation of DocHandler is above this line

  public static boolean convertFile( String inputFileName, String outputFileName ) {
      try {
          BufferedReader fr = new BufferedReader( new FileReader(inputFileName) );
          fw = new BufferedWriter( new FileWriter(outputFileName) );
          General.showOutput("Reading  file: "+inputFileName);
          General.showOutput("Writing file: "+outputFileName);
          QDParser.parse(xml2fasta_1,fr);
          fr.close();
          fw.close();
      } catch ( Exception e ) {
          General.showThrowable(e);
          return false;
      }
      return true;
  }
  
  
  public static void showUsage( String[] args ) {
    General.showError( "Expected 2 arguments in stead of :"+args.length+" found.");
    General.showError( "Usage: java Wattos.Gobbler.Xml2Fasta_1 file_in_name file_out_name");      
    System.exit(1);
  }
  
  /** Check if the orf has a valid sequence for the blast db.
   *was from CESG already, if so we don't put it out.
   */
  public static boolean doPrint( Orf orf ) {
//      String CESG_id_1 = "Center for Eukaryotic Structural Genomics";
//      String CESG_id_2 = "CESG";
      
      /**
      if ( orfid.orf_db_name.equalsIgnoreCase( CESG_id_1 ) || 
           orfid.orf_db_name.equalsIgnoreCase( CESG_id_2 ) ) {
          General.showOutput("Including CESG entry");
          //return false;
      }
       */

      if ( ! orf.hasValidSequenceForBlastDb() ) {
          General.showError("Invalid sequence in orf:\n" + orf.toFasta() );
          return false;
      }
      return true;
  }

  /** Usage: java Reporter [xml file(s)] */
  public static void main(String[] args) throws Exception {
    if ( args.length != 2 ) {
        showUsage(args);
    }
    String inputFileName    = args[0];
    String outputFileName   = args[1];
    boolean status = convertFile( inputFileName, outputFileName );
    if ( ! status ) {
        System.exit(1);
    }
  }
  
}
 
