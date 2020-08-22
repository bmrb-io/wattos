/*
 * WattosSwingWorker.java
 *
 * Created on April 7, 2006, 2:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.CloneWars.Gui;

import Wattos.Utils.General;
import Wattos.Utils.Strings;
import edu.oswego.cs.dl.util.concurrent.misc.SwingWorker;
import java.lang.reflect.Method;

/**
 *
 * @author jurgen
 */
public class WattosSwingWorker extends SwingWorker {

    Method commandMethod = null;
    Object commandHub = null;
    Object[] args = null;
    WattosGui gui = null;
    String parentMethodName = null;
    
    public WattosSwingWorker(Method commandMethod, 
            Object commandHub, Object[] args, WattosGui gui, String parentMethodName ) {
        this.commandMethod = commandMethod;
        this.commandHub = commandHub;
        this.args = args;
        this.gui = gui;        
    }
    
    
    protected Object construct() {
        try {
            Object result = commandMethod.invoke( commandHub, args );
            boolean status = ((Boolean) result).booleanValue();
            if ( ! status ) {
                General.showError("in executing command commandHub."+commandMethod.getName()+" with parameters: " +
                        Strings.toString((Object[])args[0]));
            }
            gui.afterEachCommandExecuted(parentMethodName, (Object[]) args[0]);
        } catch (Throwable t) {
        // event-dispatch thread won't be interrupted 
            General.showThrowable(t);
        }
        return null;
    }
 }
