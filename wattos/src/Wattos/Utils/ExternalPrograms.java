package Wattos.Utils;

import java.io.*;
import Wattos.CloneWars.*;

/**
 *System dependent programs external to Java such as formatNMRStar and convert_star2pdb.
 *
 * @author Jurgen F. Doreleijers
 */
public class ExternalPrograms {

    /** Creates a new instance of FormatNMRStarExternal */
    public ExternalPrograms() {
    }

    /** It assumes the runtime environment is setup to
     *include that program in it's path. The file names should be given
     *with respect to working directory in a system dependent naming.
     *The error output will be echoed to System.err so intercept it
     *if needed. The exit code of the program will be returned.
     *
     * This converter uses the starlib C library compiled program FormatNMRStar
     *to achieve it's goal.
     *Does not run on Windows.
     */
    public static int formatNMRStar( String fi, String fo ) {
        if ( System.getProperty("os.name" ).startsWith("Windows")) {
            General.showCodeBug("formatNMRStar doesn't run under windows yet");
            return 1;
        }
        final String PROGRAM = "formatNMRSTAR";
        String cmd = PROGRAM + " < " + fi + " > " + fo;
        int status = OSExec.exec( cmd );
        return status;
    }


    /** It does not assume that the runtime environment is setup to
     *include that the program gawk is it's path. The file names should be given
     *with respect to working directory in a system dependent naming.
     *The error output will be echoed to System.err so intercept it
     *if needed. The exit code of the program will be returned.
     *
     *Runs under Windows with Cygwin's gawk and on Linux.
     */
    public static int convert_star2pdb( String fi, String fo ) {

        if ( UserInterface.wattosRootDir == null ) {
            General.showError("Without set WATTOSROOT variable can't convert_star2pdb");
            return 1;
        }

        General.showDebug("Converting from STAR file: " + fi);
        General.showDebug("           to PDB file   : " + fo);
        File wattosScriptsDir = new File( UserInterface.wattosRootDir, "scripts" );
        File programFile = new File( wattosScriptsDir, "convert_star2pdb");
        String PROGRAM = programFile.toString();
        /** make sure gawk is in your path */
        String path = "";
        if ( true ) {
            path = "c:\\cygwin\\bin\\";
        }
        String cmd = path + "gawk -f " + PROGRAM + " \"" + fi + "\" \"" + fo + "\"";
        int status = OSExec.exec( cmd );
        return status;
    }

    /**
     *Use as main only to test. Much quicker to use program directly!
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            General.showOutput("USAGE: java ExternalPrograms file_in file_out");
            System.exit(1);
        }

//        if ( false ) {
//            // The following command exists on Windows and my Linux box
//            //String infile =
//            //exec("format ");
//            //exec("java -version");
//            //exec("\"C:\\Progra~1\\jEdit4~1.1pr\\jedit.exe i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr\"");
//            //"C:\Progra~1\jEdit4~1.1pr\jedit.exe i:\pdbmirror2\nozip\data\structures\all\nmr_restraints\1brv.mr
//            //exec("\"dir i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr\"");
//            /// i:\\pdbmirror2\\nozip\\data\\structures\\all\\nmr_restraints\\1brv.mr");
//            //exec("\"C:\\Program Files\\Microsoft Office\\Office\\Winword.exe\"");
//            //exec("java -Xmx96m -Xms24m -jar S:\\linux\\src\\jedit\\4.0.3\\jedit.jar");
//            int status = formatNMRStar( args[0], args[1]);
//            if ( status == 0 ) {
//                General.showOutput("Finished FormatNMRStarExternal successfully with exit code 0");
//            } else {
//                General.showError("Found an error exit status: " + status);
//                System.exit(status);
//            }
//        }
    }
}
