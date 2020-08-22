/*
 * SaveFrame.java
 *
 * Created on April 25, 2003, 1:23 PM
 */

package Wattos.Star;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;

import Wattos.Utils.General;


/**
 *A list of looped or free value tables.
 * @author Jurgen F. Doreleijers
 */
public class SaveFrame extends StarNode implements Serializable {
    private static final long serialVersionUID = -4004261938981413501L;
    public static final String STANDARD_TITLE = "standard_"+
        StarGeneral.DATA_NODE_TYPE_DESCRIPTION[StarGeneral.DATA_NODE_TYPE_SAVEFRAME]+"_title";
    
    /** Creates a new instance of SaveFrame */
    public SaveFrame() {
        super.init();
        init();
    }

    public void init() {
        title                   = STANDARD_TITLE;
        parent                  = null;        
        dataNodeType            = StarGeneral.DATA_NODE_TYPE_SAVEFRAME;        
    }        
    
    /** Specify a valid title without the save_ part */
    public boolean setTitle( String titleNew ) {
        String titleMod = StarGeneral.toValidSaveFrameName( titleNew );
        if ( ! titleMod.equals( titleNew ) ) {
            General.showWarning("Failed to set valid saveframe title with input: [" + titleNew + "]");
            return false;
        }
        title = titleMod;
        //General.showDebug("Set saveframe title to: [" + title + "]");
        return true;
    }
        
    public boolean toSTAR( Writer w) {
        StringBuffer sb = new StringBuffer();
        try {             
            sb.append('\n'); // Just like Steve's starlib
            sb.append("save_");
            sb.append(title);
            sb.append('\n');
            w.write(sb.toString());
            for (Iterator it=datanodes.iterator();it.hasNext();) {
                Object o = it.next();
                if ( o instanceof StarNode ) {
                    StarNode sN = (StarNode) o;
                    sN.toSTAR(w);
                } else if ( o instanceof TagTable ) {
                    TagTable tT = (TagTable) o;
                    tT.toSTAR(w);
                }
            }
            w.write("save_\n\n");
        } catch ( IOException e_io ) {
            General.showError("I/O error: " +  e_io.getMessage() );
            return false;
        }        
        return true;
    }    

    /** Find the correct tagtables in the save frame/
     * One can use the wild card '*' for the any of the arguments. Returns null if no tagtable is 
     *present in the tree.
     */
    public ArrayList getTagTableList(String columnName ) {

        /** Determine if we need to continue or can return right away.*/
        if ( columnName.equals(StarGeneral.WILDCARD) ) {
            if ( datanodes.size() > 0 ) {
                return datanodes;
            } else {
                return null;
            }
        }
        
        ArrayList result = new ArrayList();        
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
                // determine if the column exists or matches *
                tT = tT.getTagTable(columnName); // recycle the variable
                if ( tT != null ) {
                    result.add( tT );
                }
            }
        }
        if ( result.size() == 0 ) {
            return null;
        }
        return result;
    }
    
    /** See namesake but now we'll use regexp force.
     */
    public ArrayList getTagTableListRegExp(String columnNameExp ) {

        /** Determine if we need to continue or can return right away.*/
        if ( columnNameExp.equals(StarGeneral.WILDCARD) ) {
            if ( datanodes.size() > 0 ) {
                return datanodes;
            } else {
                return null;
            }
        }
        
        ArrayList result = new ArrayList();        
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
                // determine if the column exists or matches *
                tT = tT.getTagTableRegExp(columnNameExp); // recycle the variable
                if ( tT != null ) {
                    result.add( tT );
                }
            }
        }
        if ( result.size() == 0 ) {
            return null;
        }
        return result;
    }

    /** Works for NMRSTAR versions 2.1.1 and 3.0. Needs updating in the future probably.
     *Can easily be made faster by not checking each column label.
     */
    public String getCategory() {
        //General.showDebug("Getting sf category for sf with title: " + title);
        String regExp = "(_Saveframe_category)|(_.+\\.Sf_category)";
        ArrayList list = getTagTableListRegExp(regExp);
        if ( list == null || list.size() == 0 ) {
            General.showWarning("Failed to find any match for reg exp: " + regExp + "in saveframe: " + toString(false));
            try {
                throw new Exception("dummy");
            } catch ( Exception e ) {
                General.showThrowable(e);
            }            
            return null;
        }
        if ( list.size() > 1 ) {
            General.showCodeBug("Found more than one tag table that matches which is not good. : " + list.size() + " taking the first match.");
            return null;
        }                
        TagTable tT = (TagTable) list.get(0);            
        int columnId = tT.getColumnIdForRegExp(regExp);
        if ( columnId < 0 ) {
            General.showCodeBug("Didn't find column in table for regexp: " + regExp);
            return null;
        }                        
        String result = tT.getValueString(0,columnId);
        return result;
    }
    
    
    /** Find the correct saveframe in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no save frame is 
     *present in the tree.
     */
    public ArrayList getSaveFrameListByCategory( String saveframeNodeCategoryName ) {                
        String cat = getCategory();
        if ( cat == null ) {
            General.showDebug("Failed to get category for saveframe with title: " + title);
            return null;
        }
        
        if ( saveframeNodeCategoryName.equals( StarGeneral.WILDCARD) ||
                                   cat.equals( saveframeNodeCategoryName ) ) {
            ArrayList result = new ArrayList(1);
            result.add(this);
            return result;
        }
        return null;
    }    
    
    /** Comparison will be made by string representation; don't use it if that doesn't
     *work or would be slow. It assumes only one is found. Returns first even if
     *more could be found.
     */
    public static SaveFrame selectSaveFrameByTagNameAndFreeValue( ArrayList sFList, String tagName, Object value ) {
        for (int i=0;i<sFList.size();i++) {
            SaveFrame sF = (SaveFrame) sFList.get(i);
            //General.showDebug("Name looking at sf: " + sF.title);
            for (Iterator it=sF.datanodes.iterator();it.hasNext();) {
                Object o = it.next();
                if ( o instanceof TagTable ) {
                    TagTable tT = (TagTable) o;
                    //General.showDebug("Name looking at tT: " + tT.name);
                    if ( ! tT.isFree ) {
                        continue;
                    }
                    if ( ! tT.containsColumn( tagName ) ) {
                        continue;
                    }
                    //General.showDebug("Found tag name: " + tagName);
                    String valueFound = tT.getValueString(0, tT.getColumnIdx(tagName));
                    if ( value.toString().equals(valueFound) ) {
                        return sF;
                    }
                }
            }
        }
        return null;
    }
}
