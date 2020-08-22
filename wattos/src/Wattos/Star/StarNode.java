/*
 * StarNode.java
 *
 * Created on June 2, 2003, 4:01 PM
 */

package Wattos.Star;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import Wattos.Database.Relation;
import Wattos.Star.NMRStar.StarDictionary;
import Wattos.Utils.General;
import Wattos.Utils.StringArrayList;

/**
 * This class is a frame holder for data block, saveframe and tagtable.
 * @author Jurgen F. Doreleijers
 */
public class StarNode {
    /**  A valid title without the save_/data_/etc. part */
    public String       title; // Not set in the case of a tag table or a top node like thingie.
    /** datanodes is a list of possibly mixed saveframes and tagtables */
    public ArrayList    datanodes; // May be a general purpose top node, data node, saveframe node, or tags
    public StarNode     parent;
    public StarGeneral  general;
    public int          dataNodeType;

    public static final int BUFFER_SIZE = 32 * 1024;

    /** Creates a new instance of StarNode */
    public StarNode() {
        init();
    }

    public void init() {
        dataNodeType = StarGeneral.DATA_NODE_TYPE_DEFAULT;
        datanodes = new ArrayList();
        general = new StarGeneral();
        parent = null;
    }

    public void setStarParent(StarNode sn) {
        parent                  = sn;
    }

    /** General setter/getters to encapsulate the datanodes variable and add functionality
     *for parent relation.
     */
    public Object get( int idx ) {
        return datanodes.get(idx);
    }

    public boolean add( Object o) {
        return add( datanodes.size(), o);
    }

    public boolean add( int idx, Object o) {
        datanodes.add( idx, o );
        if ( o instanceof StarNode ) {
            StarNode sN = (StarNode) o;
            sN.parent = this;
        } else if ( o instanceof TagTable ) {
            TagTable tT = (TagTable) o;
            tT.parent = this;
        }
        return true;
    }

    /** Remove the starnode or tagtable element */
    public boolean remove( int idx ) {
        Object o = datanodes.remove(idx);
        if ( o instanceof StarNode ) {
            StarNode sN = (StarNode) o;
            sN.parent = null;
        } else if ( o instanceof TagTable ) {
            TagTable tT = (TagTable) o;
            tT.parent = null;
        }
        return true;
    }

    /** Doesn't support data outside SF yet.
     @see STARFilter#insertTag
     */
    public boolean insertTag(String[] sFCategoryList,
                             String[] tagNameIdList,
                             String[] tagNameList,
                             String[] valueList,
                             String[] locList,
                            int tagCount ) {
//        int tagCount = sFCategoryList.length;
        for (int v=0;v<tagCount;v++) {
            String sFCategory = sFCategoryList[v];
            if ( sFCategory == null ) {
                General.showError("Failed to get SF category for row rid: " + v );
                return false;
            }
            String tagNameId = tagNameIdList[v];
            if ( tagNameId == null ) {
                General.showError("Failed to get tagNameId for row rid: " + v );
                return false;
            }
            String tagName = tagNameList[v];
            if ( tagName == null ) {
                General.showError("Failed to get tagName for row rid: " + v );
                return false;
            }
            String value = valueList[v];
            if ( value == null ) {
                General.showError("Failed to get value for row rid: " + v );
                return false;
            }
            String loc = locList[v];
            if ( loc == null ) {
                General.showError("Failed to get loc for row rid: " + v );
                return false;
            }
            for (int i=datanodes.size()-1;i>=0;i--) {
                Object o = datanodes.get(i);
                if ( !(o instanceof SaveFrame) ) {
                    continue;
                }
                SaveFrame sF = (SaveFrame) o;
                String cat = sF.getCategory();
                if ( cat == null ) {
                    General.showError("Failed to get category for SF: " + sF.title );
                    return false;
                }
//                General.showDebug("Looking at SF with title: " + sF.title + " of category: " + cat);
                if ( sFCategory.equals( StarGeneral.WILDCARD ) ||
                     sFCategory.equals( cat)) {
                    for (int j=sF.datanodes.size()-1;j>=0;j--) {
                        Object p = sF.datanodes.get(j);
                        if ( ! (p instanceof TagTable) ) {
                            General.showError("Unexpected instance of non table item in star tree.");
                            return false;
                        }
                        TagTable tT = (TagTable) p;
//                        General.showDebug("Looking at tT with title: " + tT.name );
                        if ( tagNameId.equals( StarGeneral.WILDCARD ) ||
                             tT.containsColumn( tagNameId )) {
                             // find the location of the new tag name.
                             int index = 0;
                             if ( loc.equals(StarGeneral.STAR_NULL_STRING)) {
                                 index = tT.sizeColumns();
                             } else {
                                 try {
                                    index = Integer.parseInt(loc);
                                 } catch ( Throwable t ) {
                                     General.showThrowable(t);
                                 }
                             }
                             if ( ! tT.insertColumn(index,tagName,Relation.DATA_TYPE_STRINGNR,null)) {
                                 General.showError("Failed insertTag because failed tT.insertColumn");
                                 return false;
                             }
                             if ( ! tT.setValueByColumn(tagName,value)) {
                                 General.showError("Failed insertTag because failed tT.setValueByColumn");
                                 return false;
                             }
                        } // enf of block on tT
                    } // end of loop over elements in SF
                } // end of block on SF present
            }
        }
        return true;
    }



    /** Remove the starnode or tagtable element except as by specification.
     *Algorithm is less efficient than removeAllButTagTableWithTagName so use it
     *after that if it makes sense.
     *Doesn't support data outside SF yet.
     @see STARFilter#removeAllButTagNames
     */
    public boolean removeAllButTagNames( String SFCatToKeep, StringArrayList TtTagNameListToKeep ) {
        for (int i=datanodes.size()-1;i>=0;i--) {
            Object o = datanodes.get(i);
            if ( o instanceof SaveFrame ) {
                SaveFrame sF = (SaveFrame) o;
                String cat = sF.getCategory();
                if ( cat == null ) {
                    General.showError("Failed to get category for SF: " + sF.title );
                    return false;
                }
//                General.showDebug("Looking at SF with title: " + sF.title + " of category: " + cat);
                if ( SFCatToKeep.equals( StarGeneral.WILDCARD ) ||
                     SFCatToKeep.equals( cat)) {
                    // now what about if the saveframe is allowed but has tagtables to be removed.
                    for (int j=sF.datanodes.size()-1;j>=0;j--) {
                        Object p = sF.datanodes.get(j);
                        if ( ! (p instanceof TagTable) ) {
                            General.showError("Unexpected instance of non table item in star tree.");
                            return false;
                        }
                        TagTable tT = (TagTable) p;
//                        General.showDebug("Looking at tT with title: " + tT.name );
                        for (int c=tT.columnOrder.size()-1;c>=0;c--) {
                            String label = tT.getColumnLabel(c);
                            if ( label.equals(Relation.DEFAULT_ATTRIBUTE_ORDER_ID)) {
                                continue;
                            }
//                            General.showDebug("Looking at tag: " + label );
                            if ( TtTagNameListToKeep.indexOf(label) < 0 ) {
//                                General.showDebug("Removing tag: " + label);
                                tT.removeColumn(label);
                            }
                        }
                        // Remove tT without tags except the order tag.
                        if ( tT.columnOrder.size() <= 1 ) {
//                            General.showDebug("Removing tT: " + tT.name);
                            sF.datanodes.remove(j);
                        }
                    }
                    // Remove sF without tT.
                    if ( sF.datanodes.size() == 0 ) {
                        datanodes.remove(sF);
                    }
                }
            }
        }
        return true;
    }


    /** Remove the starnode or tagtable element except as by specification.
     *Doesn't support data outside SF yet.
     @see STARFilter#removeAllButTagTableWithTagName
     */
    public boolean removeAllButTagTableWithTagName( String SFCatToKeep, String TtTagNameToKeep ) {
        for (int i=datanodes.size()-1;i>=0;i--) {
            Object o = datanodes.get(i);
            if ( o instanceof SaveFrame ) {
                SaveFrame sF = (SaveFrame) o;
//                General.showDebug("Looking at SF with title: " + sF.title + " of category: " + sF.getCategory());
                if ( ! SFCatToKeep.equals( StarGeneral.WILDCARD ) &&
                     ! SFCatToKeep.equals( sF.getCategory())) {
//                    General.showDebug("Removing "+ sF.title);
                    datanodes.remove(sF);
                    continue;
                }
                // now what about if the saveframe is allowed but has tagtables to be removed.
                for (int j=sF.datanodes.size()-1;j>=0;j--) {
                    Object p = sF.datanodes.get(j);
                    if ( ! (p instanceof TagTable) ) {
                        General.showError("Unexpected instance of non table item in star tree.");
                        return false;
                    }
                    TagTable tT = (TagTable) p;
//                    General.showDebug("Looking at tT with title: " + tT.name );
                    if ( ! tT.hasColumn(TtTagNameToKeep)) {
//                        General.showDebug("Removing " + tT.name);
                        sF.remove(j);
                    }
                }
                // Remove sF without tT.
                if ( sF.datanodes.size() == 0 ) {
                    datanodes.remove(sF);
                }
            }
        }
        return true;
    }

    /** Remove the starnode or tagtable element except as by specification.
     *Doesn't support data outside SF yet.
     @see STARFilter#removeAllButSaveframeCategories
     */
    public boolean removeAllButSaveframeCategories( StringArrayList SFCatToKeep) {
        for (int i=datanodes.size()-1;i>=0;i--) {
            Object o = datanodes.get(i);
            if ( o instanceof SaveFrame ) {
                SaveFrame sF = (SaveFrame) o;
//                General.showDebug("Looking at SF with title: " + sF.title );
                if ( SFCatToKeep.indexOf( sF.getCategory() ) < 0 ) {
//                    General.showDebug("Removing");
                    datanodes.remove(sF);
                }
            }
        }
        return true;
    }

    /**
     *Doesn't support data outside SF yet.
     */
    public boolean removeSaveframeCategories( StringArrayList SFCatToRemove) {
        for (int i=datanodes.size()-1;i>=0;i--) {
            Object o = datanodes.get(i);
            if ( o instanceof SaveFrame ) {
                SaveFrame sF = (SaveFrame) o;
//                General.showDebug("Looking at SF with title: " + sF.title );
                if ( SFCatToRemove.indexOf( sF.getCategory() ) >= 0 ) {
//                    General.showDebug("Removing");
                    datanodes.remove(sF);
                }
            }
        }
        return true;
    }

    /** */
    public int size() {
        return datanodes.size();
    }


    /** Return string in stead of output to a writer. Returns null if
     * unsuccessful.
     */
    public String toSTAR() {
//        General.showDebug("Now in StarNode.toSTAR");
        StringWriter stw = new StringWriter();
        boolean status = toSTAR(stw);
        if ( ! status ) {
            return null;
        }
        return stw.toString();
    }


    /** If the file name given ends with .gz it will be gzipped.
     */
    public boolean toSTAR( String fileName ) {
        boolean status = false;
        File f = new File( fileName );
        BufferedWriter bw = null;
        try {
            FileOutputStream fos = new FileOutputStream( f );
            if ( fileName.endsWith( ".gz" ) ) {
                GZIPOutputStream gos = new GZIPOutputStream( fos );
                OutputStreamWriter osw = new OutputStreamWriter(gos);
                bw = new BufferedWriter( osw );
            } else {
                bw = new BufferedWriter( new OutputStreamWriter( fos ));
            }
            status = toSTAR( bw );
            bw.close();
        } catch ( FileNotFoundException e ) {
            General.showError("File not found: " +  e.getMessage() );
            return false;
        } catch ( IOException e_io ) {
            General.showError("I/O error: " +  e_io.getMessage() );
            return false;
        }
        return status;
    }

    public boolean toSTAR( Writer w ) {
//        General.showDebug("Doing toSTAR in starnode with name: " + title);
        try {
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
        } catch ( Exception e ) {
            General.showOutput("ERROR: error in StarNode caught: " );
            General.showThrowable(e);
            return false;
        }

        return true;
    }

    /** According to the data types specified in the dictionary
     *translate it assuming that all are strings of type non-redundant.
     *
     *The result of this function on a typical star file with coordinates was
     *really disappointing; memory usage went up with 10%.
     *Depends greatly on the types of data but could be typical.
     */
    public boolean translateToNativeTypesByDict( StarDictionary starDict, boolean isMMCIF) {
        //General.showDebug("Doing translate to native datatypes by dictionary definitions");
        TagTable tT = null;
        boolean overallStatus = true;
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            boolean status = false;
            Object o = it.next();
            if ( o instanceof TagTable ) {
                tT = (TagTable) o;
                status = tT.translateToNativeTypesByDict(starDict, isMMCIF);
            } else {
                StarNode sN = (StarNode) o;
                status = sN.translateToNativeTypesByDict(starDict, isMMCIF);
            }
            if ( ! status ) {
                overallStatus = false;
            }
        }
        return overallStatus;
    }

    /** Recursively looks for the top star node which has info on like the
     *preferred format. Usually it only takes a couple of look ups so it's
     *very fast.
     */
    public StarNode getTopStarNode() {
        if ( parent == null ) {
            return this;
        } else {
            return parent.getTopStarNode();
        }
    }

    /** Find the correct tagtable in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no tagtable is
     *present in the tree. The saveframeNodeCategoryName is intended to be used in the future
     *perhaps, currently not supported.
     */
    public ArrayList getTagTableList(String dataNodeName, String saveframeNodeCategoryName, String saveframeNodeName, String columnName ) {

        if ( dataNodeName == null || saveframeNodeCategoryName == null || saveframeNodeName == null || columnName == null ) {
            General.showError("In getTagTableList none of the String arguments can be null but found:");
            General.showError("dataNodeName,       saveframeNodeCategoryName,       saveframeNodeName,       columnName are:");
            General.showError( dataNodeName +", "+ saveframeNodeCategoryName +", "+ saveframeNodeName +", "+ columnName);
        }
        /** Determine if we need to continue or can return right away.*/
        String selectionName = null;
        if ( dataNodeType == StarGeneral.DATA_NODE_TYPE_DATABLOCK ) {
            selectionName = dataNodeName;
        } else {
            selectionName = saveframeNodeName;
        }
        if ( !(selectionName.equals(StarGeneral.WILDCARD) ||
              (selectionName.equals( title )))) {
            return null;
        }

        ArrayList result = new ArrayList(datanodes.size()); // give it the maximum number of elements possible.

        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof StarNode ) {
                StarNode sN = (StarNode) o;
                ArrayList resultTmp = sN.getTagTableList(dataNodeName, saveframeNodeCategoryName, saveframeNodeName, columnName);
                if ( resultTmp != null ) {
                    result.addAll( resultTmp );
                }
            } else if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
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

    /** In the expectation of 1 and only 1 item to be returned. showWarning if
     *not 1 is found.
     *Perfectly fine to use this to get the first tag table in a node.
     */
    public TagTable getTagTable( String columnName, boolean showWarning ) {
        return getTagTable( StarGeneral.WILDCARD, columnName, showWarning );
    }


    /** In the expectation of 1 and only 1 item to be returned. showWarning if
     *not 1 is found
     *Perfectly fine to use this to get the first tag table in a node.
     */
    public TagTable getTagTableByName( String tagTableName, boolean showError ) {
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
                Relation r = tT.dbms.getRelation(tagTableName);
                if ( r == null ) {
                    if ( showError ) {
                        General.showError("Failed to find a tagtable with the name: [" + tagTableName + "] from dbms relation: xxx");
                    }
                }
                return (TagTable) r;
            }
        }
        if ( showError ) {
            General.showError("Failed to find a tagtable with the name: [" + tagTableName + "]");
        }
        return null;
    }

    /** In the expectation of 1 and only 1 item to be returned. showError if
     *not 1 is found
     */
    public TagTable getTagTableBySaveFrameNameAndTagName( String saveFrameName,
            String columnName, boolean showError ) {
        SaveFrame sF = getSaveFrameByName(saveFrameName, showError);
        if ( sF == null ) {
            if ( showError ) {
                General.showError("Failed to find SaveFrame with the name: [" + saveFrameName + "]");
            }
            return null;
        }
        TagTable tT = sF.getTagTable(columnName,showError);
        if ( tT == null ) {
            if ( showError ) {
                General.showError("Failed to find TagTable with a column the tag name: [" + columnName + "]");
            }
            return null;
        }
        return tT;
    }


    /** In the expectation of 1 and only 1 item to be returned. showWarning if
     *not 1 is found. Note that the name should NOT include the save_ part.
     */
    public SaveFrame getSaveFrameByName( String saveFrameName, boolean showError ) {
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof SaveFrame ) {
                SaveFrame sF = (SaveFrame) o;
                String name =sF.title;
//                General.showOutput("Encountered sF: ["+name+"]");
                if ( name.equals(saveFrameName) ) {
                    return sF;
                }
            } else {
                General.showDebug("Skipping non saveFrame: " + o.toString());
            }
        }
        if ( showError ) {
            General.showError("Failed to find SaveFrame with the name: [" + saveFrameName + "]");
        }
        return null;
    }

    /** In the expectation of 1 and only 1 item to be returned. showWarning if
     *not 1 is found. Will still return 1 item if multiple are found.
     */
    public TagTable getTagTable( String saveframeNodeCategoryName, String columnName, boolean showWarning ) {
//        General.showDebug("Looking for tagtable in sf: " + saveframeNodeCategoryName +
//                " with column: " +columnName);
        ArrayList tTList = getTagTableList(StarGeneral.WILDCARD,saveframeNodeCategoryName,StarGeneral.WILDCARD,columnName);
        if ( tTList == null ) {
//            General.showDebug("Failed to find list of tag tables (in saveFrame: "+ getCategory() + ") for column name:" + columnName );
            return null;
        }
        if ( tTList.size() == 0 ) {
            if ( showWarning ) {
                General.showWarning("Find tTAssemblyList sf not once but: " + tTList.size());
                General.showWarning("(in saveFrame: "+ getCategory() + ") for column name:" + columnName );
            }
            return null;
        }
        if ( tTList.size() > 1 ) {
            if ( showWarning ) {
                General.showWarning("Find tTAssemblyList sf not once but: " + tTList.size());
                General.showWarning("(in saveFrame: "+ getCategory() + ") for column name:" + columnName );
            }
//            return null;
        }
        TagTable tT = (TagTable) tTList.get(0);
        return tT;
    }

    public String getCategory() {
        return null;
    }

    /** Find the correct saveframe in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no save frame is
     *present in the tree.
     */
    public ArrayList getSaveFrameListByCategory( String saveframeNodeCategoryName ) {

        ArrayList result = new ArrayList();
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof StarNode ) { // Skip tag tables
                StarNode sN = (StarNode) o;
                ArrayList resultTmp = sN.getSaveFrameListByCategory(saveframeNodeCategoryName);
                if ( resultTmp != null ) {
                    result.addAll( resultTmp );
                }
            }
        }
        if ( result.size() == 0 ) {
            return null;
        }
        return result;
    }


    /** Only one column per table will be changed.
     *
     * @param tagNameRegexp
     * @param value
     * @return
     */
    public boolean setAllByTagNameRegexp(String tagNameRegexp, String value) {
        ArrayList tTList = getTagTableList(
                StarGeneral.WILDCARD,
                StarGeneral.WILDCARD,
                StarGeneral.WILDCARD,
                StarGeneral.WILDCARD
                );
        for (int j=0;j<tTList.size();j++) {
            General.showDebug("working on tT: " + j);
            TagTable tT = (TagTable) tTList.get(j);
            int idxColumnID = tT.getColumnIdForRegExp(tagNameRegexp);
            if ( idxColumnID < 0 ) {
                continue;
            }
            String labelColumnIDEcho = tT.getColumnLabel(idxColumnID);
            tT.setValueByColumn(labelColumnIDEcho, value);
        }
        return true;

    }

    /** Find all saveframes in the tree under this starnode. Returns empty list if no save frame is
     *present in the tree.
     */
    public ArrayList getSaveFrameList() {
        ArrayList result = new ArrayList();
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof StarNode ) { // Skip tag tables
                StarNode sN = (StarNode) o;
                ArrayList resultTmp = sN.getSaveFrameListByCategory(StarGeneral.WILDCARD);
                if ( resultTmp != null ) {
                    result.addAll( resultTmp );
                }
            }
        }
        return result;
    }

    /** Find the correct saveframe in the tree under this starnode.
     * One can use the wild card '*' for the any of the arguments. Returns null if no save frame is
     *present in the tree.
     */
    public SaveFrame getSaveFrameByCategory( String saveframeNodeCategoryName, boolean showError ) {

        ArrayList resultTmp = getSaveFrameListByCategory( saveframeNodeCategoryName );
        if ( resultTmp == null ) {
            if ( showError ) {
                General.showError("Failed to find sf for category name: " + saveframeNodeCategoryName);
            }
            return null;
        }
        if ( resultTmp.size() != 1 ) {
            if ( showError ) {
                General.showError("Found sf not once but: " + resultTmp.size() + " for category name: " + saveframeNodeCategoryName );
            }
            return null;
        }
        SaveFrame result = (SaveFrame) resultTmp.get(0);
        return result;
    }

    /** Will use the dictionary definitions to convert tagvalues that might be in primitive types like
     *floats to textual representations where definitions exist to do something else than the default
     *to text operations Wattos does. I.e. it will not convert data for which no dictionary definitions
     *are given.
     *
     *Algoritm:
     *- for each tag (within saveframe/tagtable tree)
     *- lookup the definition in the dictionary
     *- reformat to text if definition exists according to definition
     *
     *Any failure results in a false return value.
     */
    public boolean toStarTextFormatting( StarDictionary starDict ) {
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof StarNode ) {
                StarNode sN = (StarNode) o;
                if ( ! sN.toStarTextFormatting(starDict) ) {
                    General.showError("Failed to convert with dictionary text formatting for star node with title: " + sN.title);
                    return false;
                }
            } else if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
                if ( ! tT.translateToStarFormattingByDict(starDict) ) {
                    General.showError("Failed to convert with dictionary text formatting for tag table: " + tT.toString(false,false,false,false,false,false));
                    return false;
                }
            }
        }
        return true;
    }

    /** human readible representation */
    public String toString(boolean showRows) {
        StringBuffer result = new StringBuffer();
        result.append( "StarNode of type: " + StarGeneral.DATA_NODE_TYPE_DESCRIPTION[dataNodeType] + " has number of nodes: "+datanodes.size()+General.eol);
        for (Iterator it=datanodes.iterator();it.hasNext();) {
            Object o = it.next();
            if ( o instanceof StarNode ) {
                StarNode sN = (StarNode) o;
                result.append( "StarNode of type: " + StarGeneral.DATA_NODE_TYPE_DESCRIPTION[sN.dataNodeType] + General.eol);
                result.append( sN.toString(showRows) );
            } else if ( o instanceof TagTable ) {
                TagTable tT = (TagTable) o;
                result.append( "Tagtable:\n");
                result.append( tT.toString(true,true,true,true,false,false) );
            }
        }
        return result.toString();
    }
}
