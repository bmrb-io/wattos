/*
 * Cart.java
 *
 * Created on August 4, 2003, 3:45 PM
 */

package Wattos.CloneWars.WattosMenu;

import Wattos.Utils.*;
import java.util.*;
import java.io.*;
import com.Ostermiller.util.ExcelCSVParser;

/**
 * A menu is a list of menu items. Menu items are of type menu or command.
 * Commands have attributes like name, parent Menu, Description, Keyboard shortcut, Example
 * , etc.
 * This allows for nesting although support is up to 4 levels
 * because of limitations in the definitions used in a spreadsheet used to define
 * the menu.
 * @author Jurgen F. Doreleijers
 */
public class WattosMenu extends WattosMenuItem implements Serializable  {

    /** Faking this variable makes the serializing not worry
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;

    public static final String DEFAULT_MAIN_MENU_NAME = "DEFAULT_MAIN_MENU_NAME";

    /** Definitions for a table. */
    public static final String DEFAULT_TABLE_BEGIN                  = "TBL_BEGIN";
    public static final String DEFAULT_TABLE_END                    = "TBL_END";
    public static final String COLUMN_NAME_MENUSUB0                 = "MenuSub0";
    public static final String COLUMN_NAME_MENUSUB1                 = "MenuSub1";
    public static final String COLUMN_NAME_MENUSUB2                 = "MenuSub2";
    public static final String COLUMN_NAME_MENUSUB3                 = "MenuSub3";
    public static final String COLUMN_NAME_COMMAND                  = "Command";
    public static final String COLUMN_NAME_KEYBOARD_SHORTCUT        = "Keyboard_shortcut";
    public static final String COLUMN_NAME_DESCRIPTION              = "Description";
    public static final String COLUMN_NAME_HELP_HTML                = "Help_html";

    public static final int MAX_COMMAND_NAME_LENGTH         = 16;
    public static final int MAX_MENU_NAME_LENGTH            = 8;
    public static final int MAX_COMMANDS_PER_PROMPT_LINE    = 3;
    /** Please make sure that the following holds:
     * (MAX_MENUS_PER_PROMPT_LINE+1)    * MAX_MENU_NAME_LENGTH    <
     * (MAX_COMMANDS_PER_PROMPT_LINE+1) * MAX_COMMAND_NAME_LENGTH
     */
    public static final int MAX_MENUS_PER_PROMPT_LINE       = 5;

    public static final int SUB_LEVEL_COUNT = 4;

    static final String[] columnList = {
        COLUMN_NAME_MENUSUB0,
        COLUMN_NAME_MENUSUB1,
        COLUMN_NAME_MENUSUB2,
        COLUMN_NAME_MENUSUB3,
        COLUMN_NAME_COMMAND,
        COLUMN_NAME_KEYBOARD_SHORTCUT,
        COLUMN_NAME_DESCRIPTION,
        COLUMN_NAME_HELP_HTML
    };

    /** Local resource */
    static final String CSV_FILE_LOCATION = "Data/WattosMenu.csv";

    /** A list of menu items */
    public ArrayList items;

    /** A alphabetically sorted list of items in this menu and all levels of submenus.
     Variable is only set for the main menu. Menus and submenus also become commands
     so make sure they are mutually unique overall.*/
    public ArrayList mainMenuCommandNames;

    /** Asks a user to enter a command from this menu and no submenu. With thanks
     *to Gert Vriend at EMBL/CMBI for coming up with the design as in WHAT IF's
     *menu.
     */
    private String prompt;

    /** Creates a new instance of Cart */
    public WattosMenu( WattosMenu parentMenu, String name ) {
        super( parentMenu, name );
        items = new ArrayList();
        mainMenuCommandNames = new ArrayList();
        prompt = null;
    }


    /** Should only be executed on toplevel menu because that's where it
     *will look: mainMenuCommandNames.
     *If the command is not in the list the function returns -1
     */
    public int indexOf( String commandName ) {
        for (int c=0;c<mainMenuCommandNames.size();c++) {
            if ( commandName.equalsIgnoreCase( (String) mainMenuCommandNames.get(c) ) ) {
                return c;
            }
        }
        return -1;
    }


    /**
     * @return Null or the cached string.
     */
    public String getPrompt() {
        if ( prompt == null ) {
            boolean status = setPrompt();
            if ( ! status ) {
                General.showError("failed to set prompt for menu with name: " + name);
                return null;
            }
        }
        return prompt;
    }

    /** Each line in the prompt is a fixed number of characters long.
     * The prompt might look like:
     *<PRE>
     *Submenus:------------------------------------------------------- MainMenu
     *Commands:-------------------------------------------------------
     *File          Edit        View                                 -
     *----------------------------------------------------------------
     *</PRE>
     */
    private boolean setPrompt() {

        ArrayList lines = new ArrayList();

        // Line length determined by the command length mostly.
        // and includes eol char.
        int lineLength = (MAX_COMMAND_NAME_LENGTH+1)*MAX_COMMANDS_PER_PROMPT_LINE +
                         3 + (MAX_MENU_NAME_LENGTH+1);
        int positionDashSeperation = (MAX_COMMAND_NAME_LENGTH+1) * MAX_COMMANDS_PER_PROMPT_LINE + 1;
        char[] emptyLine    = Strings.createStringOfXTimesTheCharacter(' ', lineLength).toCharArray();
        emptyLine[ lineLength-1 ] = '\n';  // todo make system independent
        emptyLine[ positionDashSeperation ] = '-';
        char[] dashLine     = Strings.createStringOfXTimesTheCharacter('-', lineLength).toCharArray();
        dashLine[ lineLength-1 ] = '\n';
        char[] submenusLine = new char[lineLength];
        char[] commandsLine = new char[lineLength];
        System.arraycopy( dashLine, 0, submenusLine, 0, lineLength);
        System.arraycopy( dashLine, 0, commandsLine, 0, lineLength);
        String submenus = "SubMenus:";
        String commands = "Commands:";
        System.arraycopy( submenus.toCharArray(),   0, submenusLine, 0, submenus.length());
        System.arraycopy( commands.toCharArray(),   0, commandsLine, 0, commands.length());

        // Find the submenus and commands
        ArrayList submenuList = new ArrayList();
        ArrayList commandList = new ArrayList();
        for (int i=0;i<items.size();i++) {
            Object o = items.get(i);
            if ( o instanceof WattosCommand ) {
                commandList.add( o );
                //General.showDebug(" Looking at item: " + i + " " + ((WattosCommand)o).name );
            } else if ( o instanceof WattosMenu ) {
                submenuList.add( o );
                //General.showDebug(" Looking at item: " + i + " " + ((WattosMenu)o).name );
            } else {
                General.showError("failed to get the correct class for menu item in menu with name: " + name);
                return false;
            }
        }
        //General.showDebug(" Number of commands found: " + commandList.size() );
        //General.showDebug(" Number of submenus found: " + submenuList.size() );


        // Add the submenus (always showing one line)
        lines.add( submenusLine );
        char[] currentLine = new char[lineLength];
        System.arraycopy( emptyLine, 0, currentLine, 0, lineLength);
        int itemIdOnLine = 0;
        for (int i=0;i<submenuList.size();i++) {
            //General.showDebug("Working on submenuList item : " + i);
            itemIdOnLine = i % MAX_MENUS_PER_PROMPT_LINE;
            // Save the previous line and start a new one.
            if ( (i!=0) && (itemIdOnLine == 0) ) {
                lines.add( currentLine );
                currentLine = new char[lineLength]; // Need a new one because of referencing.
                System.arraycopy( emptyLine, 0, currentLine, 0, lineLength);
            }
            int positionOnLine = itemIdOnLine*(MAX_MENU_NAME_LENGTH+1);
            WattosMenu item = (WattosMenu) submenuList.get(i);
            String itemName = item.name;
            System.arraycopy( itemName.toCharArray(), 0, currentLine, positionOnLine, itemName.length() );
        }
        // Grab the last line
        lines.add( currentLine );

        // Add the commands
        lines.add( commandsLine );
        currentLine = new char[lineLength];
        System.arraycopy( emptyLine, 0, currentLine, 0, lineLength);
        itemIdOnLine = 0;
        for (int i=0;i<commandList.size();i++) {
            itemIdOnLine = i % MAX_COMMANDS_PER_PROMPT_LINE;
            // Save the line and start a new one.
            if ( (i!=0) && (itemIdOnLine == 0)) {
                lines.add( currentLine );
                currentLine = new char[lineLength]; // Need a new one because of referencing.
                System.arraycopy( emptyLine, 0, currentLine, 0, lineLength);
            }
            int positionOnLine = itemIdOnLine*(MAX_COMMAND_NAME_LENGTH+1);
            WattosCommand item = (WattosCommand) commandList.get(i);
            String itemName = item.name;
            System.arraycopy( itemName.toCharArray(), 0, currentLine, positionOnLine, itemName.length() );
        }
        // Grab the last line
        lines.add( currentLine );


        // Insert path to the top menu
        submenuList = new ArrayList();
        WattosMenu currentMenu = this;
        while ( currentMenu.parentMenu != null ) {
            submenuList.add( currentMenu.name );
            currentMenu = currentMenu.parentMenu;
        }
        // Now that we know how many there are from the top; let's insert them.
        int positionOnLine = positionDashSeperation + 1;
        for (int i=0;i<submenuList.size();i++) {
            char[] menuName = ((String)submenuList.get(i)).toCharArray();
            int lineId = submenuList.size() - i - 1;
            currentLine = (char[]) lines.get(lineId);
            System.arraycopy( menuName, 0, currentLine, positionOnLine, menuName.length);
        }


        StringBuffer sb = new StringBuffer();
        for (int i=0;i<lines.size();i++) {
            sb.append((char[])lines.get(i));
        }
        prompt = sb.toString();
        return true;
    }



    public String toHtml(int level) {
        String identString = Strings.createStringOfXTimesTheCharacter(' ', level*6);
        StringBuffer sb = new StringBuffer();
        if ( level == 0 ) {
            sb.append( "<PRE>" + General.eol );
        }
        sb.append("Menu: " + name + General.eol);
        for (int i=0;i<items.size();i++) {
            WattosMenuItem menuItem = (WattosMenuItem) items.get(i);
            sb.append(identString + "Item: " + menuItem.toHtml(level+1) + General.eol);
        }
        for (int i=0;i<mainMenuCommandNames.size();i++) {
            sb.append(identString + "MainMenuCommandName: " + i + " " + mainMenuCommandNames.get(i) + General.eol);
        }
        if ( level == 0 ) {
            sb.append( "</PRE>" + General.eol );
        }
        return sb.toString();
    }

    /** Read the possible menu items. See excel file for notes on the formatting of the file.
     * Example csv file:
     * <pre>
MainMenu,MenuSub1,MenuSub2,MenuSub3,Command,Keyboard_shortcut,Description,Help_html
Notes:,All commands have a unique name that is a primary key into this table.,,,,,,
,Commands do not need to be in a submenu.,,,,,,
,"Keyboard shortcuts are meant for the GUI, not for the UI.",,,,,,
,Menus can be empty,,,,,,
,The menu are based on MOLMOL by Reto Koradi.,,,,,,
,Longest command name is ExecuteStandard with 14 chars. Tried to keep names short and abbreviated where needed.,,,,,,
,Commands are selection based for the most part. E.g. CalcNOECompl will calculated the completeness of NOEs that have been selected in selected entries.,,,,,,
,The order in which the commands are listed is the order in which they will appear in the overall menu. Don't start a mainmenu item more than once.,,,,,,

TBL_BEGIN,,,,,,,
File,Read,,,ReadEntry,,Reads a PDB formatted PDB entry or an NMR-STAR formatted BMRB entry,
TBL_END,,,,,,
blabla
     * </pre>
     * The capitalized keywords TBL_BEGIN and TBL_END denote the outline of the table.
     *The column names are hard coded but can be swapped or redefined in the code here.
     *The cells are considered empty if they only contain white space.
     * @param csv_file_location Url for file with the comma separated info.
     * @return <CODE>true</CODE> if all operations are successful.
     */
    public boolean readCsvFile(String csv_file_location) {
        String[][] values = null;
        try {
            Reader reader = null;
            if ( csv_file_location == null ) {
                //General.showDebug("Reading WattosMenu dictionary from local resource : " + CSV_FILE_LOCATION);
                InputStream is = getClass().getResourceAsStream(CSV_FILE_LOCATION);
                if ( is == null ) {
                    General.showWarning("Failed to open InputStream from location: " + CSV_FILE_LOCATION);
                    return false;
                }
                reader = new InputStreamReader(is);
            } else {
                //General.showDebug("Reading WattosMenu dictionary from local resource : " + csv_file_location);
                reader = new FileReader( csv_file_location );
            }


            if ( reader == null ) {
                General.showWarning("Failed to open reader from location: " + csv_file_location);
                return false;
            }

            BufferedReader br = new BufferedReader( reader );
            ExcelCSVParser parser = new ExcelCSVParser(br);
            if ( parser == null ) {
                General.showWarning("Failed to open Excel CSV parser from location: " + csv_file_location);
                return false;
            }
            // Parse the data
            values = parser.getAllValues();
            if ( values == null ) {
                General.showError("no data read from csv file");
                return false;
            }
            if ( values.length <= 0 ) {
                General.showError("number of rows found: " + values.length);
                General.showOutput("but expected at least      : " + 1);
                return false;
            }
            values = Strings.deleteAllWhiteSpace(values);
            ArrayList columnLabels = new ArrayList( Arrays.asList( values[0] ) );

            /** Get the interesting column indexes */
            int[] columnIdx = new int[ columnList.length ];
            for (int c=0;c<columnList.length;c++) {
                columnIdx[c] = columnLabels.indexOf( columnList[c] );
                if ( columnIdx[c] == -1 ) {
                    General.showError("failed to find a column with label: " + columnList[c]);
                    General.showOutput("Found column labels: " + columnLabels );
                    return false;
                }
                //General.showDebug(" found column name: " + columnList[c] + " in column number: " + columnIdx[c] );
            }

            int FIRST_ROW_WITH_DATA = -1;
            int LAST_ROW_WITH_DATA = -1;
            /** Find the begin and end of the table */
            for (int r=0;r<values.length;r++) {
                if ( values[r][0].equals( DEFAULT_TABLE_BEGIN ) ) {
                    FIRST_ROW_WITH_DATA = r+1;
                } else if ( values[r][0].equals( DEFAULT_TABLE_END ) ) {
                    LAST_ROW_WITH_DATA = r-1;
                }
            }
            if ( FIRST_ROW_WITH_DATA == -1 ) {
                General.showError(" did not found begin of table; should start with: " + DEFAULT_TABLE_BEGIN );
                return false;
            }

            if ( LAST_ROW_WITH_DATA == -1 ) {
                General.showError(" did not found end of table; should end with: " + DEFAULT_TABLE_END );
                return false;
            }



            /** Create bogus items to start */
            String[] menuNames = new String[SUB_LEVEL_COUNT+1];
            menuNames[0]    = "MAIN LEVEL MENU NAME";
            for (int r=FIRST_ROW_WITH_DATA;r<=LAST_ROW_WITH_DATA;r++) {
                //General.showDebug(" doing row: " + r + " which reads (csv) with: " + Strings.toString( values[r] ));
                menuNames[1]    = values[r][ columnIdx[0] ];
                menuNames[2]    = values[r][ columnIdx[1] ];
                menuNames[3]    = values[r][ columnIdx[2] ];
                menuNames[4]    = values[r][ columnIdx[3] ];
                String commandName     = values[r][ columnIdx[4] ];
                String shortcut        = values[r][ columnIdx[5] ];
                String description     = values[r][ columnIdx[6] ];
                String helpHtml        = values[r][ columnIdx[7] ];
                // Parent menu to be set later for wattos command
                WattosCommand command = new WattosCommand( null, commandName, shortcut, description, helpHtml);
                //General.showDebug(" adding to menu a command with name: " + command.name);
                addCommand( menuNames, command );
            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }
        Collections.sort( mainMenuCommandNames );

        /** Some debugging: */
        //General.showDebug("Read number of commands and menus: " + mainMenuCommandNames.size());
//        String longestString = Strings.longestString( Strings.toStringArray(mainMenuCommandNames));
        //General.showDebug("Longest commands and menus is    : " + longestString + " at length: " + longestString.length());


        return true;
    }

    /** Adds by iterating through menus to find the place.
     */
    private boolean addCommand( String[] menuNames, WattosCommand command ) {

        WattosMenu[] menus = new WattosMenu[SUB_LEVEL_COUNT+1]; // Actually 5 levels including main and 4 sublevels.
        // Set the first level.
        menus[0] = this;
        // Do the next four levels.
        for (int level=1;level<=SUB_LEVEL_COUNT;level++) {
            //General.showDebug(" doing level: " + level + " for command: " + command + " For menu names: " + Strings.toString(menuNames));
            menus[level] = menus[level-1].getSubMenu( menuNames[level] );
            // Create a new menu if it didn't exist before.
            if ( menus[level] == null ) {
                menus[level] = new WattosMenu( menus[level-1], menuNames[level] );
                menus[level-1].items.add( menus[level] );
                mainMenuCommandNames.add( menus[level].name ); // Add menu names as commands too; kind of weird but nice.
            }
            // Insert if the last one or if no sub menu defined.
            if ( (level == SUB_LEVEL_COUNT) || menuNames[level+1].equals("") ) {
                command.parentMenu = menus[level-1]; // Only now we found this info.
                menus[level].items.add( command );
                mainMenuCommandNames.add( command.name );
                return true; // Exit point!!
            }
        }
        General.showError(" failed to add the command: " + command + " For menu names: " + Strings.toString(menuNames));
        return false;
    }


    /** Does a slow scan but works and number of elements very limited
     *Case insensitive.
     */
    public WattosMenu getSubMenu( String name ) {
        for (int i=0;i<items.size();i++) {
            Object o = items.get(i);
            // Skip commands in this menu; only look for submenus.
            if ( o instanceof WattosMenu ) {
                WattosMenu menu = (WattosMenu) o;
                if ( menu.name.equalsIgnoreCase( name ) ) {
                    return menu;
                }
            }
        }
        return null;
    }

    /** Does a slow scan but works and number of elements very limited
     *Case insensitive.
     */
    public boolean containsSubMenu( String name ) {
        WattosMenu subMenu = getSubMenu( name );
        if ( subMenu == null ) {
            return false;
        }
        return true;
    }

    /** Does a slow iterative scan but works and number of elements very limited
     *Case insensitive.
     */
    public boolean containsSubMenuInAny( String name ) {
        WattosMenu subMenu = getSubMenu( name );
        if ( subMenu != null ) {
            return true;
        }
        for (int i=0;i<items.size();i++) {
            Object o = items.get(i);
            if ( o instanceof WattosMenu ) {
                WattosMenu m = (WattosMenu) o;
                //General.showDebug(" Looking at menu: " + " " + m.name );
                if ( m.name.equalsIgnoreCase( name ) ) {
                    return true;
                } else if ( m.containsSubMenuInAny(name)) {
                    return true;
                }
            }
        }
        return false;
    }



    /**
     * The main can be used to regenerated a bin file that contains the menu description.
     * It also loads it back in to check for any errors. Individual methods can
     * be used to do parts, e.g. readObject.
     */
    public static void main(String[] args) {

        WattosMenu menu = new WattosMenu(null, "Wattos Menu");
        boolean status = menu.readCsvFile(null);
        if (! status) {
            General.showError(" in WattosMenu.main found:");
            General.showError(" reading WattosMenu CSV file.");
            System.exit(1);
        } else {
            General.showDebug(" read WattosMenu CSV file.");
        }

//        if ( false ) {
//            WattosMenu fileMenu     = (WattosMenu) menu.items.get(0);
//            WattosMenu macroMenu    = (WattosMenu) fileMenu.items.get(2);
//            General.showOutput("Macro menu prompt is:\n" + macroMenu.getPrompt() );
//        }
        if ( true ) {
            General.showOutput("Menu:\n" + menu.toHtml(0));
        }
        General.showOutput("Done with menu parsing.");
    }
}
