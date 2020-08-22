/*
 * WattosCommand.java
 *
 * Created on August 4, 2003, 4:01 PM
 */

package Wattos.CloneWars.WattosMenu;

import java.io.*;

/**
 *Simple command in Wattos with attributes like help text.
 *@see WattosMenu
 * @author Jurgen F. Doreleijers
 */
public class WattosCommand extends WattosMenuItem implements Serializable {
    
    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    

    /** Top level menu has null for parentMenu */
    public String keyboardShortCut;
    public String description;
    public String helpHtml;
                
    /** Creates a new instance of WattosCommand */
    public WattosCommand(    WattosMenu parentMenu,
                             String name,
                             String keyboardShortCut,
                             String description,
                             String helpHtml
                            ) {
         super(parentMenu, name);                                
         this.keyboardShortCut  = keyboardShortCut; 
         this.description       = description;
         this.helpHtml          = helpHtml; 
    }    
    
    public boolean isTopLevelMenu() {
        if ( parentMenu == null ) {
            return true;
        }
        return false;
    }

    public String toHtml(int identLevel) {
        StringBuffer result = new StringBuffer();
        //String identString = Strings.createStringOfXTimesTheCharacter(' ', identLevel*4);
        //result.append( "<PRE>" );
        //result.append( identString );
        result.append( name );
        //result.append( General.eol );
        //result.append( "</PRE>" );
        return result.toString();
    }
    
}
