/*
 * StarFilter.java
 *
 * Created on February 20, 2006, 3:55 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.CloneWars.Gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author jurgen
 */
public class StarFilter extends FileFilter {
    
    /** Creates a new instance of StarFilter */
    public StarFilter() {
    }
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }        
        String extension = Wattos.Utils.InOut.getFilenameExtension(f);
        if (extension != null) {
            if (extension.equals("str")) {
                    return true;
            }
        }
        return false;
    }    
    public String getDescription() {
        return "Filter STAR files (.str).";
    }
    
}
