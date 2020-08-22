/*
 * WattosMenuItem.java
 *
 * Created on August 4, 2003, 4:26 PM
 */

package Wattos.CloneWars.WattosMenu;
import java.io.*;

/**
 *Simple menu item in Wattos with attributes like name.
 *@see WattosMenu
 * @author Jurgen F. Doreleijers
 */
public class WattosMenuItem implements Serializable{

    /** Faking this variable makes the serializing not worry 
     *about potential small differences.*/
    private static final long serialVersionUID = -1207795172754062330L;    

    public WattosMenu parentMenu;
    public String name;
    
    /** Creates a new instance of WattosMenuItem */
    public WattosMenuItem( WattosMenu parentMenu, String name ) {
         this.parentMenu        = parentMenu;   
         this.name              = name;         
    }
    
    public String toHtml(int identLevel) {
        return "ERROR: in toHtml() in Wattos.Utils.WattosMenu.WattosMenuItem should be overriden";
    }    
}
