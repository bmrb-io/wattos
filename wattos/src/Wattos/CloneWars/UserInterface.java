/*
 * UserInterface.java
 *
 * Created on August 4, 2003, 1:59 PM
 */

package Wattos.CloneWars;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import Wattos.CloneWars.Gui.WattosGui;
import Wattos.CloneWars.WattosMenu.WattosMenu;
import Wattos.Database.DBMS;
import Wattos.Database.Defs;
import Wattos.Soup.Gumbo;
import Wattos.Soup.Constraint.Constr;
import Wattos.Utils.FileCopy;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.Strings;
import Wattos.Utils.TypeDesc;

/**
 *A non-graphical and perhaps in the future a graphical user interface.
 *Allows execution of scripting commands such as those found in the macros
 *directory under WATTOS_ROOT.
 * @author Jurgen F. Doreleijers
 */
public class UserInterface implements Serializable, ActionListener {

    private static final long serialVersionUID = -1207795172754062330L;

    // Use a global variable for this in order not to need to create more
    // than 1 which destroys the buffer when reading commands from an
    // external file piped in. Doesn't need to be serialized either.
    public static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    // Following commands deal with the menu and need to be the same as
    // the definitions in the external xls file.
    public static final String DEFAULT_COMMAND_QUIT                 = "Quit";
    public static final String DEFAULT_COMMAND_END                  = "End";
    public static final String DEFAULT_COMMAND_COMMENT_START        = "#";

    // Argument on command line for using text version (no gui version possible yet..)
    public static final String DEFAULT_COMMAND_LINE_ARGUMENT_AT             = "-at";
    /** To be followed by a integer value in range [0-9]*/
    public static final String DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY      = "-verbosity";
    /** Don't write the ui on exit */
    public static final String DEFAULT_COMMAND_LINE_ARGUMENT_NO_WRITE       = "-noWriteSessionOnExit";
    /** Go into Wattos in a non-interactive mode.
     *Failed to find out how to have Java detect piped input
     *itself.
     */
    public static final String DEFAULT_COMMAND_LINE_ARGUMENT_NONINTERACTIVE = "-nonInteractiveSession";
    /* So with all options set the command looks like this:
     *java Wattos.CloneWars.UserInterface  -at -noWriteSessionOnExit -nonInteractiveSession -verbosity 9
     */

    // The old initialized user interface in WATTOSHOME defined dir.
    public static final String DEFAULT_LOCATION_DUMP_OLD_BIN         = "dump_old.bin";
    // The properties file in WATTOSHOME defined dir.
    public static final String DEFAULT_LOCATION_PROP_FILE            = "WattosProp.txt";
    /** May be null
     */
    public static File wattosRootDir = null;
    /** May be null
     */
    public static File wattosHomeDir = null;
    /** We trust users but when scripting a failure should be recognized after some tries */
    public static int DEFAULT_MAXIMUM_NUMBER_OF_TRIES_ON_INPUT = 5;

    /** The version will be ignored now.
     * */
// <<<<<<< .mine
//     public static String WATTOS_VERSION="20101111-0917";
// =======
// //    public static String WATTOS_VERSION=".";
// >>>>>>> .r143

    /** Write a file with the user interface on exiting. Use the command line option to
     *change this standard behavior.     */
    public boolean writeSessionOnExit = true;
    /** No need to store if a session is interactive beyond the session. So this
     *variable doesn't reside in the WattosProp class.*/
    public boolean interactiveSession = true;
    /** Stop execution on any error
     */
    public boolean stopOnError = false;

    /**
     * The graphical interface. Can be null when none is used. Use the method
     *     hasGuiAttached for telling so.
     */
    public WattosGui gui = null;
    public volatile boolean guiDone = false; // volatile because it will be updated by the EDThread

    public WattosMenu       mainMenu;
    public WattosLib        wattosLib;
    public WattosProp       wattosProp;
    public Date             startTime;
    public Date             lastTimeShown;
    public DBMS             dbms;
    public CommandHub       commandHub;
    public Gumbo            gumbo;
    public Constr           constr;

    /** Means if the Gui can be used for UI
     *This is different from the fact if a WattosGui is attached.
     */
    boolean inGuiMode = false;

    /** Creates a new instance of UserInterface */
    public UserInterface() {
    }

    /** Even if no directory was defined/present or creatable we'll still return true
     *here because we want Wattos to run everywhere.
     */
    public static boolean ensurePresenceUserDefs() {
//        General.showOutput("Now in ensurePresenceUserDefs.");
        boolean status;
        // In case the user preference file needs to be created a couple of subs are needed.
        boolean doSubs = false;
        // Is the WATTOSHOME dir defined in the shell? It's a requirement so far.
//        General.showOutput("debugging 0");
        Properties sp = InOut.getEnvVars();
        if ( sp == null ) {
            General.showDebug("Failed to get system environment settings; failing ensurePresenceUserDefs.");
            return true;
        }

        // Even if the dir exists; the files/dirs in it might not so do a copy
        // skipping files that already exist.
        if ( ! sp.containsKey("WATTOSROOT") ) {
            General.showDebug("The shell variable WATTOSROOT isn't defined.");
            General.showDebug("Consider defining it to be the directory of your Wattos installation.");
            return true;
        }
        String wattosRootDirStr = sp.getProperty( "WATTOSROOT" );
        wattosRootDir = new File(wattosRootDirStr);
//        General.showOutput("WATTOSROOT: " + wattosRootDir);
//        General.showOutput("from: " + wattosRootDirStr);

        // for debugging inside IDE
//        General.showOutput("debugging 1");
//        sp.setProperty("WATTOSHOME", "C:\\jurgen\\Wattos");
//        General.showOutput("sp: " + Strings.toString(sp));
        if ( sp.containsKey("WATTOSHOME") ) {
//            General.showOutput("debugging 1b");
            String wattosHomeDirString = sp.getProperty( "WATTOSHOME" );
            wattosHomeDir = new File( wattosHomeDirString );
//            General.showOutput("debugging 1c");
            if ( ! wattosHomeDir.exists() ) {
                General.showOutput("Creating Wattos home directory with settings at: " + wattosHomeDir);
                if ( ! wattosHomeDir.mkdir() ) {
                    General.showError("Coulnd't make Wattos home directory: " + wattosHomeDir);
                    return true;
                }
            }
        } else {
            General.showDebug("The shell variable WATTOSHOME isn't defined.");
            General.showDebug("Consider defining it to be the directory of your personal Wattos settings.");
            return true;
        }
//        General.showOutput("debugging 2");

        File wattosPropFile = new File( wattosHomeDir, DEFAULT_LOCATION_PROP_FILE);
        if ( ! wattosPropFile.exists() ) {
            doSubs = true;
        }
        // Copy all user data into the user's directory as specified by the
        // environment variable WATTOSHOME.
        boolean recursively = true;
        boolean force = false;
        boolean interactive = false;
        File wattosHomeDirIn = new File( wattosRootDir + File.separator + "data" + File.separator + "user_data");
        File[] fileList = wattosHomeDirIn.listFiles(); // no specific order
        if ( fileList == null ) {
            General.showError("Can't get files in source for wattos home dir: " + wattosHomeDirIn);
            return false;
        }
        for(int i = 0; i < fileList.length; i++) {
            File file = fileList[i];
            if ( file.isDirectory() ) {
//                Skip svn resources.
                if ( file.toString().endsWith(".svn")) {
                    continue;
                }
                status = InOut.copyFiles( file, wattosHomeDir, recursively, force, interactive );
                if ( ! status ) {
                    General.showError("Cannot copy files from: " + file + " to dir " + wattosHomeDir );
                    return false;
                }
            } else {
                status = FileCopy.copy( file, wattosHomeDir, force, interactive );
                if ( ! status ) {
                    General.showError("Cannot copy single file from: " + file + " to dir " + wattosHomeDir );
                    return false;
                }
            }
        }

        // Replace WATTOSROOT in the WattosProp.txt file with the absolute path name
        if ( doSubs ) {
            Properties subs = new Properties();
            General.showDebug("Replace WATTOSROOT in the WattosProp.txt file with the absolute path name");
            subs.setProperty( "WATTOSROOT", wattosRootDir.toString() );
            String tmpDir = WattosProp.getTmpDir();
            if ( tmpDir == null ) {
                General.showError("Cannot set a default temporary dir.");
                return false;
            }
            subs.setProperty( "TMPDIR", tmpDir);
            String binDir = WattosProp.getBinDir();
            if ( binDir == null ) {
                General.showError("Cannot set a default binary dir.");
                return false;
            }
            subs.setProperty( "BINDIR", binDir);
            InOut.replaceMultiInFile( wattosPropFile, subs );
        }
        return true;
    }

    /** Read properties if available from before.
     */
    public boolean readProperties() {
        wattosProp = new WattosProp();
        if ( wattosHomeDir == null ) {
            return true;
        }
        // Properties will always be loaded fresh from file.
        // Is the WATTOSHOME dir defined in the shell? It's a requirement so far.
        File wattosPropFile = new File( wattosHomeDir, DEFAULT_LOCATION_PROP_FILE);
        if ( !wattosPropFile.exists() ) {
            General.showWarning("Failed to find the properties file eventhough the WATTOSHOME dir is defined");
            return true;
        }
        try {
            wattosProp.load( new FileInputStream( wattosPropFile ));
        } catch ( Exception e ) {
            General.showThrowable(e);
            return false;
        }
        return true;
    }

    /** Start with the required resources.
     */
    public static UserInterface init( boolean ignoreOldDumpIfPresent) {
        UserInterface ui = null;
        if ( ! ensurePresenceUserDefs() ) {
            General.showError("Failed to ensure the presence of the user definitions");
            return null;
        }

        File oldDump = new File( wattosHomeDir, DEFAULT_LOCATION_DUMP_OLD_BIN );

        if ( (!ignoreOldDumpIfPresent) && oldDump.exists() && oldDump.isFile() && oldDump.canRead() ) {
            General.showOutput("Reading old dump: " + oldDump);
            Object object = InOut.readObject( oldDump.toString() );
            if ( object == null ) {
                General.showWarning("Failed to read object for ui.");
            } else {
                if ( ! (object instanceof UserInterface)) {
                    General.showWarning("Failed to read correct object for ui. Object is of type:");
                    TypeDesc.printType(object.getClass());
                } else {
                    ui = (UserInterface) object;
                }
            }
            if ( ui == null ) {
                General.showError("Failed to initialize user interface from previous dump.");
            } else {
////                UserInterface ui_temp = new UserInterface();
//                boolean versionsMatch = false;
//                // Do a try because parameter might not have been defined. (That would be a code bug though;-)
//                try {
//                    versionsMatch = UserInterface.WATTOS_VERSION.equals( UserInterface.WATTOS_VERSION );
//                    //General.showDebug("User interface from previous dump is of version: " + ui.WATTOS_VERSION);
//                } catch ( Exception e ) {
//                    General.showThrowable(e);
//                }
//                if ( ! versionsMatch ) {
//                    General.showError("User interface from previous dump is of incorrect version. Correct version is: " + UserInterface.WATTOS_VERSION);
//                    General.showError("Found version is: " + UserInterface.WATTOS_VERSION + " Reinitializing ui.");
//                    ui = null;
//                }
            }
        }

        if ( ui == null ) {
            ui = new UserInterface();
            if ( ! ui.initResources()) {
                General.showError("Failed to initialize resources in UserInterface.");
                return null;
            }
        }

        ui.startTime = new Date();
//        General.showOutput("Wattos ["+ WATTOS_VERSION + "]");
        General.showOutput(General.getStartMessage());
        General.showOutput("");

        // Start with fresh properties even if ui was loaded from bin file.
        if ( ! ui.readProperties() ) {
            General.showError("Couln't read Wattos properties");
            return null;
        }
        return ui;
    }



    /** Loads standard properties from the user files, loads libraries etc.
     *If not saved before. Will also be called if InitAll command is issued.
     */
    public boolean initResources() {
        if ( ! readProperties() ) {
            General.showError( "Couln't read Wattos properties" );
        }
        // Get the menu
        mainMenu = new WattosMenu( null, "Wattos Menu");
        if ( mainMenu == null ) {
            General.showCodeBug("Failed to initialize menu");
            return false;
        }
        // read from jar or filesystem
        if ( ! mainMenu.readCsvFile( null )) {
            General.showError("Can't read csv file for main menu");
            return false;
        }
        //General.showDebug("Read menu from system resource (csv file in source directory).");


        // Get the DBMS
        dbms = new DBMS();
        if ( dbms == null ) {
            General.showCodeBug("Failed to initialize dbms");
            return false;
        }
        //General.showDebug(" Initialized DBMS.");


        // Get the Gumbo. The reference will be maintained in the dbms only.
        gumbo = new Gumbo(dbms);
        if ( gumbo == null ) {
            General.showCodeBug("Failed to initialize gumbo");
            return false;
        }
        //General.showDebug("Initialized Gumbo.");

        // Get the Constr. The reference will be maintained in the dbms only.
        constr = new Constr(dbms);
        if ( constr == null ) {
            General.showCodeBug("Failed to initialize Constr");
            return false;
        }
//        General.showDebug("Initialized Constr.");

        /** Get the libs */
        wattosLib = new WattosLib(this);
        if ( wattosLib == null ) {
            General.showCodeBug("Failed to initialize wattos libraries");
            return false;
        }
        if ( ! wattosLib.initResources() ) {
            General.showCodeBug("Failed to initialize resources for wattos libraries");
            return false;
        }
        wattosLib.ui = this; // We have to reset this one because it was serialized.
        /** Register with dbms for easy access to resources in UI from constraints etc.
         */
        dbms.ui = this;
//        General.showDebug("Read libraries from system resource.");

        // Spin the wheel of fortune
        commandHub = new CommandHub( this, in );
        if ( commandHub == null ) {
            General.showCodeBug("Failed to initialize CommandHub");
            return false;
        }
        return true;
    }

    public static void showUsage(String str) {
        General.showOutput(str);
        General.showOutput(DEFAULT_COMMAND_LINE_ARGUMENT_AT                 + "\t\tstart non-gui interface");
        General.showOutput(DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY          + "\tspecifies what will be shown on the output (0 only errors, 1 and warnings, 2 and output(normal), 9 all including debugging)");
        General.showOutput(DEFAULT_COMMAND_LINE_ARGUMENT_NO_WRITE           + "\twattos last environment will not be saved for next run");
        General.showOutput(DEFAULT_COMMAND_LINE_ARGUMENT_NONINTERACTIVE     + "\twattos behaves as in scripting mode (menus aren't echoed after each command)");
    }

    /** Command-line interface
     * /** Very simple loop as a menu.
     *Start over every time a command has been done.
     *Very fast though to get the info again so not a performance issue.
     *in can be System.in or the content of a remote script file.
     *The method returns true if the end of the inputstream is met.
     */
    public boolean doNonGuiMain() {
//        General.showOutput("Now in doNonGuiMain");
        inGuiMode = false;
        //gui = null; // don't kill the gui as we might be in non-gui mode temporarily.
        String commandName = "bogusCommand";
        // Start in the main menu.
        WattosMenu currentMenu = mainMenu;
        String currentPrompt = currentMenu.getPrompt(); // Prompts are cached here and in menu instance; no speed issues.

        for (;;) {
//            try {
//                if ( !interactiveSession) {
//                    General.showDebug("Checking to see if input is ready for non-interactive session.");
//                    if ( !in.ready()) {
//                        General.showDebug("BufferedReader not ready; empty or underlying character stream is not ready.");
//                        General.showDebug("Leaving doNonGuiMain because running out of commands from input.");
//                        return true;
//                    } else {
//                        General.showDebug("BufferedReader is ready.");
//                    }
//                } else {
//                    General.showDebug("Skipping check on input ready for interactive session.");
//                }
//            } catch ( Throwable t ) {
//                General.showThrowable(t);
//                General.showError("Leaving doNonGuiMain because of an error.");
//                return false;
//            }
            if ( interactiveSession ) {
                commandName = Strings.getInputString( in, currentPrompt, null );
            } else {
                commandName = Strings.getInputString( in, null, null);
            }
            //General.showDebug("Read command: " + commandName);

            if ( commandName == InOut.END_OF_FILE_ENCOUNTERED ) {
//                General.showDebug("Leaving doNonGuiMain because of EOF.");
                return true;
            }
            if ( commandName == null ) {
                General.showError("Leaving doNonGuiMain because of error reading a new command.");
                return false;
            }

            if ( commandName.startsWith( DEFAULT_COMMAND_COMMENT_START )) {
//                General.showDebug("Comment: " + commandName.trim());
                continue;
            }
            int idxCommand = mainMenu.indexOf( commandName );
            if ( idxCommand == -1 ) {
                if ( commandName.length() > 1 ) {
                    General.showError( "Invalid command : " + commandName );
                }
                continue;
            }
            // Pick a command and preprocess some.
            // QUIT
            if ( commandName.equalsIgnoreCase( DEFAULT_COMMAND_QUIT )) {
                break;
                // STEP OUT OF MENU
            } else if ( commandName.equalsIgnoreCase( DEFAULT_COMMAND_END )) {
                if ( currentMenu.parentMenu == null ) {
                    General.showWarning("Already at the top level");
                } else {
                    currentMenu = currentMenu.parentMenu;
                    currentPrompt = currentMenu.getPrompt();
                }
                continue;
                // STEP INTO MENU
            } else if ( currentMenu.containsSubMenu( commandName ) ){
                currentMenu   = currentMenu.getSubMenu( commandName );
                currentPrompt = currentMenu.getPrompt();
                continue;
                // ALL OTHER COMMANDS
            } else {
                boolean commandExecuted = commandHub.invoke(idxCommand);
                if ( ! commandExecuted ) {
                    General.showWarning("Command could not be executed: " + commandName );
                    if ( stopOnError ) {
                        General.showWarning("Parameter set for stopping on any error: " + WattosProp.DEFAULT_PROP_KEY_STOP_ON_ERROR );
                        return false;
                    }
                }
                continue;
            }
        }
//        General.showDebug("Leaving doNonGuiMain because of Quit command.");
        return true;
    }

    /** GUI */
    public boolean doGuiMain() {
        try {
            inGuiMode = true;
            guiDone = false;
            gui = new WattosGui(this,this); // ui also implements the actionListener
            if ( gui == null ) {
                General.showCodeBug("Failed to initialize WattosGui");
                inGuiMode = false;
                return false;
            }
            General.showDebug("Gui initialized.");

            EventQueue.invokeLater(new Runnable() {
                public void run() {
                    gui.setVisible(true);
                }
            });

            // Can we make an exception for a script here? // note that the gui is up already
            // and unblocked.
            String scriptName = System.getProperty("WATTOSSCRIPT");
            if ( scriptName != null ) {
//                General.showOutput("WATTOSSCRIPT is defined so script will be executed.");
                URL scriptURl = InOut.getUrlFileFromName(scriptName);
                if ( scriptURl == null )  {
                    General.showError("Failed to get a Url from variable WATTOSSCRIPT with value: " + scriptName );
                } else {
                    General.showDebug("WATTOSSCRIPT is defined as URL: " + scriptURl);
                    Object[] methodArgs = { scriptURl };
                    commandHub.ExecuteMacroUser(methodArgs);
                }
            } else {
                General.showDebug("WATTOSSCRIPT is not defined so no script to be executed.");
//                General.showDebug("Existing props are: ");
//                General.showDebug(System.getProperties().toString());
            }

            // Don't do anything in this thread anymore. The other Thread will
            // have priority. Don't return until done though.
            while ( ! guiDone ) {
                Thread.sleep(300);
            }
            if ( gui != null ) {
                gui.dispose(); // Only the reference to gui persists. hasGuiAttached will still return true
            }
        } catch ( Throwable t ) {
            General.setOut(System.out); // might not have happened in GUI.
            General.showThrowable(t);
            inGuiMode = false;
            gui = null;
            return false;
        }
        inGuiMode = false;
        gui = null;
        General.setOut(null); // might not have happened in GUI.
        return true;
    }


    /** Is there a gui attached */
    public boolean hasGuiAttached() {
        return gui != null;
    }

    /** Is the program currently in gui mode.*/
    public boolean hasGuiMode() {
        return inGuiMode;
    }


    /**
     * @param args the command line arguments override the settings in the properties file.
     */
    public static void main(String[] args) {
        /** Important to set the following for debugging startup or use: -verbosity 9*/
//        General.verbosity = General.verbosityDebug;
        boolean ignoreOldDumpIfPresent = true; // never reload from before.
        int statusInt   = General.EXIT_STATUS_SUCCESS; // success so far
        boolean doGui = true; // will be reset if -at argument is found.

        UserInterface ui = init(ignoreOldDumpIfPresent);
        if ( ui == null ) {
            General.showError("Failed to initialize the ui in main of class UserInterface -at all-.");
            statusInt = General.EXIT_STATUS_CODE_ERROR;
        }

        if ( statusInt == General.EXIT_STATUS_SUCCESS ) {
            // Process the options. For now just see if we have the ascii terminal -at flag
            if ( args.length > 0 ) {
                Strings.toLowerCase(args);
                ArrayList argsList = new ArrayList();
                argsList.addAll( Arrays.asList( args ));   // Use faster Collection.addAll method when Java 1.5 is in production.
                General.showDebug("Program arguments [" + argsList.size() + "] :" +Strings.toString(argsList));

                if ( argsList.contains( DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY )) {
                    int pos = argsList.indexOf( DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY );
                    if ( args.length < (pos+2)) {
                        showUsage("Expected the argument after: " + DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY + " to be an integer but failed to find an argument there.");
                        statusInt = General.EXIT_STATUS_INPUT_ERROR;
                    } else {
                        int level = Defs.getInt( args[ pos + 1 ] );
                        if ( Defs.isNull( level)) {
                            showUsage("Expected the argument after: " + DEFAULT_COMMAND_LINE_ARGUMENT_VERBOSITY + " to be an integer but failed to parse it.");
                            statusInt = General.EXIT_STATUS_INPUT_ERROR;
                        } else {
                            if ( (level >= 0) && (level <= 9) ) {
                                ui.wattosProp.setProperty( WattosProp.DEFAULT_PROP_KEY_VERBOSITY, new Integer(level).toString() );
                                General.showDebug("Wattos is in verbosity mode: " + level);
                            } else {
                                showUsage("Expected the verbosity level on command line in range: [0,9] but found: " + level);
                                statusInt = General.EXIT_STATUS_INPUT_ERROR;
                            }
                        }
                    }
                }
                if ( statusInt == General.EXIT_STATUS_SUCCESS ) {
                    if ( ! ui.wattosProp.setProperties(ui)) {
                        showUsage("Failed to set properties after processing the command line arguments.");
                        statusInt = General.EXIT_STATUS_INPUT_ERROR;
                    }
                    if (  argsList.contains( DEFAULT_COMMAND_LINE_ARGUMENT_NONINTERACTIVE )) {
                        General.showDebug("Wattos in NON-interactive mode");
                        ui.interactiveSession = false;
                    } else { // default is interactive
                        General.showDebug("Wattos in interactive mode");
                    }
                    if (  argsList.contains( DEFAULT_COMMAND_LINE_ARGUMENT_NO_WRITE) ) {
                        General.showDebug("Wattos will not write on closing the session");
                        ui.writeSessionOnExit = false; // default is to do so.
                    }
                    General.showDebug("Wattos in debugging mode");
                    if ( argsList.contains( DEFAULT_COMMAND_LINE_ARGUMENT_AT )) {
                        doGui = false;
                    }
                }
            }
        }
        boolean status  = true; // overall program status
        if ( statusInt == General.EXIT_STATUS_SUCCESS ) {
            if ( doGui ) {
                General.showDebug("Starting up the shield");
                status = ui.doGuiMain();
            } else {
                status = ui.doNonGuiMain();
            }
        }
        if ( ! status ) {
            statusInt = General.EXIT_STATUS_ERROR;
        }

        // Try to save the user interface if ...
//        General.showDebug("statusInt                    : " + statusInt);
//        General.showDebug("ui.writeSessionOnExit        : " + ui.writeSessionOnExit);
//        General.showDebug("UserInterface.wattosHomeDir  : " + UserInterface.wattosHomeDir);
        if (    (statusInt==General.EXIT_STATUS_SUCCESS) &&
                ui.writeSessionOnExit &&
                (UserInterface.wattosHomeDir!=null)) {
            File newDump = new File( UserInterface.wattosHomeDir, DEFAULT_LOCATION_DUMP_OLD_BIN );
//            General.showDebug("Saving user interface with all resources, excluding the gui if present.");
//            ui.gui = null; // is already done when leaving the doGuiMain method.
            General.doFullestGarbageCollection();
            if ( ! InOut.writeObject( ui, newDump.toString() ) ) {
                General.showWarning("Failed to save the user interface.");
            }
        } else {
//            General.showDebug("Skipping save of user interface");
        }
        Date now = new java.util.Date();
        General.showOutput("");
        General.showOutput("Wattos started at: " + Wattos.Utils.Dates.getDate( ui.startTime ));
        General.showOutput("Wattos stopped at: " + Wattos.Utils.Dates.getDate( now ));
        General.showOutput("Wattos took (#ms): " + (now.getTime()-ui.startTime.getTime()));
        General.flushOutputStream(); // Exiting the program will automatically flush the stream but the system.exit is not garanteed.
//        if ( statusInt != General.EXIT_STATUS_SUCCESS) {
            System.exit( statusInt ); // Needs to be called for Java Web Start to be able to end the threads.
//        }
    }

    public void actionPerformed(ActionEvent e) {
        guiDone = true;
    }
}
