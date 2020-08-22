/*
 * TypeDesc.java
 *
 * Created on March 28, 2006, 1:17 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Utils;

/**
 *
 * @author jurgen
 */
public class TypeDesc {
    
    /** Creates a new instance of TypeDesc */
    public TypeDesc() {
    }
    
    private static String[] basic   = {"class",     "interface" },
                            supercl = {"extends",   "implements" },
                            iFace   = {null,        "extends"};
        
    private static void printType(Class type, int depth, String[] labels) {
        if (type==null) {
            return;            
        }
        for (int i=0;i<depth;i++) {
            General.showOutputNoEOL("  ");
        }
        General.showOutputNoEOL(labels[type.isInterface() ? 1 : 0] + " ");
        General.showOutput(type.getName());
        Class[] interfaces = type.getInterfaces();
        for (int i=0;i<interfaces.length;i++) {
            printType(interfaces[i], depth+1,
                    type.isInterface() ? iFace : supercl);
        }
        printType(type.getSuperclass(), depth +1, supercl);
    }
    
    
    public static void main (String[] args) {
        //TypeDesc desc = new TypeDesc();
        for (int i=0;i<args.length;i++) {
            try {
                Class startClass = Class.forName(args[i]);
                TypeDesc.printType(startClass,0,basic);
            } catch ( ClassNotFoundException t) {
                General.showThrowable(t);
            }
        }
    }

    public static void printType(Class type) {
        printType(type,0,basic);
    }
    
}
