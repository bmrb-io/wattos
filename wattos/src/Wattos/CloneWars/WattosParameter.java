/*
 * WattosParameter.java
 *
 * Created on April 5, 2006, 11:28 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.CloneWars;

import Wattos.Utils.StringArrayList;


/**
 *
 * @author jurgen
 */
public class WattosParameter {
    public static final String TYPE_URL = "URL";
    
    public String               value           = null;
    public String               type            = null;
    public String               prompt          = null;
    public StringArrayList      examples        = new StringArrayList();
    public String               defaultValue    = null;
    /** null for if anything goes */
    public StringArrayList      allowedValues   = null;
    public String               help            = null;
    
    /** Is the parameter retrieved successfully */
    public boolean succes = false;
    /** Creates a new instance of WattosParameter */
    public WattosParameter() {
    }
    
}
