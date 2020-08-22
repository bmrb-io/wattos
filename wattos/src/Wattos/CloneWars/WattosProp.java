/*
 * Globals.java
 *
 * Created on November 27, 2001, 6:05 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.CloneWars;

import java.util.*;
import java.io.*;
import Wattos.Utils.*;
import Wattos.Database.*;

/** Holds the global settings like directory locations and preferred text editors.
 * The nice thing is that most things can be modified after instantiating.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 */
public class WattosProp extends Properties {

    private static final long serialVersionUID = -1207795172754062330L;    

    public static final String DEFAULT_PROP_KEY_VERBOSITY               = "verbosity";
    public static final String DEFAULT_PROP_KEY_WRITE_SESSION_ON_EXIT   = "writeSessionOnExit";
    public static final String DEFAULT_PROP_KEY_INTERACTIVE_SESSION     = "interactiveSession";
    public static final String DEFAULT_PROP_KEY_STOP_ON_ERROR           = "stopOnError";

    public static final int DEFAULT_PROP_VALUE_VERBOSITY = General.verbosityOutput;
    public static final String DEFAULT_PROP_VALUE_STR_VERBOSITY = new Integer( DEFAULT_PROP_VALUE_VERBOSITY ).toString();
    
    /** If resource is null it will load from the default location in Data dir. */
    public WattosProp() {
        super();
    }
        
    /** Get a system independent temp dir. That can later be user defined. */
    public static String getTmpDir() {
        /** Initializing some variables
         *  Use unix notation as a standard for directories
         */
        // Set the root
        String fs = File.separator;        
        String osName       = System.getProperty("os.name" );        
        /** Only used for testing purposes or maintenance */
        String rootDir = null; // directories will always be ended by a slash.
        if ( osName.startsWith("Windows") ) {
            rootDir             = "C:" + fs; 
        } else if ( osName.equals("Linux") || osName.equals("SunOS") || osName.startsWith("Mac OS") ) {
            rootDir             = fs; 
        } else {
            General.showOutput("Any Operating System that is not:");
            General.showOutput("- Windows");
            General.showOutput("- Linux, SunOS, or Mac OS");
            General.showOutput("is currently not supported by Wattos.CloneWars.WattosProp.");
            General.showOutput("The OS of this machine is determined by Java as: " 
                + osName);
            return null; 
        }                                  
        return rootDir + "tmp" + fs + "wattos";
    }

    /** Get a system independent bin dir. That can later be user defined. */
    public static String getBinDir() {
        String osName       = System.getProperty("os.name" );        
        if ( osName.startsWith("Windows") ) {
            return "C:\\cygwin\\bin";
        } else if ( osName.equals("Linux") || osName.equals("SunOS")|| osName.startsWith("Mac OS")) {
            return "/bin";            
        } else {
            General.showOutput("Any Operating System that is not:");
            General.showOutput("- Windows");
            General.showOutput("- Linux or SunOS");
            General.showOutput("is currently not supported by Wattos.CloneWars.WattosProp.");
            General.showOutput("The OS of this machine is determined by Java as: " 
                + osName);
            return null; 
        }                                  
    }

    /** Transfer some values in the property settings to internal values for ui and General.
     */
    public boolean setProperties(UserInterface ui) {

        if ( ! containsKey( WattosProp.DEFAULT_PROP_KEY_VERBOSITY )) {
            setProperty( WattosProp.DEFAULT_PROP_KEY_VERBOSITY, WattosProp.DEFAULT_PROP_VALUE_STR_VERBOSITY);
        }
        int intValue = Defs.getInt( getProperty( DEFAULT_PROP_KEY_VERBOSITY )); 
        if ( Defs.isNull( intValue )) {
            General.showError("Failed to parse int number from string: " + getProperty( DEFAULT_PROP_KEY_VERBOSITY ));
            return false;
        }
        if ( intValue > 9 ) {
            intValue = 9;
        } else if ( intValue < 0 ) {
            intValue = 0;
        } 
        General.verbosity = intValue;
        if ( containsKey( DEFAULT_PROP_KEY_WRITE_SESSION_ON_EXIT )) {
            ui.writeSessionOnExit = Defs.getBoolean( getProperty( DEFAULT_PROP_KEY_WRITE_SESSION_ON_EXIT ));
        }
        if ( containsKey( DEFAULT_PROP_KEY_INTERACTIVE_SESSION )) {
            //General.showDebug("found interactive session key");
            ui.interactiveSession = Defs.getBoolean( getProperty( DEFAULT_PROP_KEY_INTERACTIVE_SESSION ));
        }            
        if ( containsKey( DEFAULT_PROP_KEY_STOP_ON_ERROR )) {
            //General.showDebug("found interactive session key");
            ui.stopOnError = Defs.getBoolean( getProperty( DEFAULT_PROP_KEY_STOP_ON_ERROR ));
        }  
        return true;
    }
    
    
    
   /** Self test; tests the function <CODE>showmap</CODE>.
     * @param args Ignored.
     */
    public static void main (String[] args) 
    {
        WattosProp map = new WattosProp();
        map.list( System.out );
    }            
}    
