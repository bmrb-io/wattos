package Wattos.Star;

import Wattos.Utils.*;
import java.io.File;

/**
 *Simply joins two files together stripping the data_xxx line on the second file.
 *Both input files need to exist and the output file will automatically be overwritten
 *if it does exist already.
 * @author jurgen
 */
public class STARJoin {
    
    public STARJoin() {        
    }
    
    /**
     *Simply joins two files together stripping the data_xxx line on the second file.
     *Both input files need to exist and the output file will automatically be overwritten
     *if it does exist already.
     *The method wastes some memory by reading the whole files before writing.
     */
    public boolean join(String[] args) {
        if ( args == null ) {
            General.doErrorExit("Expected three arguments but got null");
        }
        if ( args.length != 3 ) {
            General.doErrorExit("Expected three arguments but got: " + args.length + General.eol +
                    ". They were: " + Strings.toString(args));            
        }
        if ( ! InOut.filesExist(new String[] {args[0], args[1] })) {
            General.showError("At least on of the input files does not exist.");
            return false;
        }
        
        
        long start = System.currentTimeMillis();        
        String f1_txt = InOut.readTextFromUrl(InOut.getUrlFileFromName(args[0]));
        if ( f1_txt == null ) {
            General.showError("Failed to read from: " + args[0]);
        }
        String f2_txt = InOut.readTextFromUrl(InOut.getUrlFileFromName(args[1]));
        if ( f2_txt == null ) {
            General.showError("Failed to read from: " + args[0]);
        }
        long taken = System.currentTimeMillis() - start;
        General.showDebug("Read took : " + taken + "(" + (taken/1000.0) + " sec)" );        
        int endIdx = f2_txt.indexOf('\n');
        if ( endIdx < 0 ) {
            endIdx=f2_txt.length();
        }
        String beginLine = f2_txt.substring(0,endIdx);
        if ( ! f2_txt.startsWith("data_") ) {
            General.showError("Second file does not start with data_ as expected but with: " + beginLine);
            return false;            
        }
        String secondFileTxtToKeep = "";
        if ( endIdx!=f2_txt.length()) {
            secondFileTxtToKeep = f2_txt.substring(endIdx+1);
        }
        
        String totalTxt = f1_txt + secondFileTxtToKeep;
        InOut.writeTextToFile(new File(args[2]),totalTxt,true,false);
        return true;
    }
    
        
    public static void showUsage() {
        General.showOutput("Usage: java Wattos.Star.STARJoin inputFile_1 inputFile_2 outputFile");        
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        STARJoin sj = new STARJoin();
        if ( ! sj.join(args)) {
            showUsage();
        }
    }    
}
