/*
 * STARFilter.java
 *
 * Created on March 1, 2006, 10:39 AM
 *
 */

package Wattos.Star;

import Wattos.Database.*;
import Wattos.Utils.*;
import java.lang.reflect.Method;
import java.net.URL;

/**
 *Reads and writes after filtering the input.
 *Possible filters intended are reordering and removing tags, tagtables, and
 *saveframes.
 *The third argument may be a dot so it will not filter but just pass thru which
 *is useful for reformatting.
 * @author jurgen
 */
public class STARFilter {

    public static String SAVEFRAME_RULE_ORDER                           = "rule_order";
    public static String TAGNAME_RULE                                   = "_Rule";
    public static String SAVEFRAME_CATEGORY                             = "_Saveframe_category";
    public static String SAVEFRAME_CATEGORY_NAME                        = "_Saveframe_category_name";
    public static String TAG_NAME                                       = "_Tag_name";
    public static String TAG_NAME_IDENTIFYING                           = "_Tag_name_identifying";
    public static String VALUE                                          = "_Value";
    public static String LOCATION                                       = "_Location";

    StarNode db = null;

    public STARFilter() {
    }


    /**
     * This will be a list of commands to do
     */
    public boolean invoke(SaveFrame sFOneRule) {
        String ruleTypeName = sFOneRule.getCategory();
        try {
            General.showDebug( "Executing: " + ruleTypeName);
            Class thisClass = this.getClass();
            Method m = thisClass.getMethod( ruleTypeName, new Class[]{ SaveFrame.class });
            Object result = m.invoke( this, new Object[] {sFOneRule});
            return ((Boolean) result).booleanValue();
        } catch ( NoSuchMethodException nsme ) {
            General.showError("No method with command name: " + ruleTypeName);
            General.showThrowable(nsme);
        // Catch any exception
        } catch ( Exception e ) {
            General.showError("Error invoking reflected command: " + ruleTypeName);
            General.showThrowable(e);
        }
        return false;
    }



    /**
     * STARFilter using the rules given in the starnode.
     */
    public boolean filter(DataBlock dbRules) {
        TagTable tTRules = dbRules.getTagTable(SAVEFRAME_RULE_ORDER,TAGNAME_RULE,true);
        if ( tTRules == null ) {
            General.showError("Failed to find tagTable sfRules by category and tag name");
            return false;
        }
        if ( tTRules.used.cardinality() == 0 ) {
            General.showWarning("Empty rule file found.");
            return true;
        }
        for ( int ruleIdx =tTRules.used.nextSetBit(0);ruleIdx>=0;ruleIdx=tTRules.used.nextSetBit(ruleIdx+1)) {
            String ruleFrameCodeName = tTRules.getValueString(ruleIdx,TAGNAME_RULE);
            SaveFrame sFOneRule = dbRules.getSaveFrameByName(ruleFrameCodeName,true);
            if ( sFOneRule == null ) {
                General.showError("Failed to find rule SaveFrame: " + ruleFrameCodeName);
                return false;
            }
            if ( ! invoke(sFOneRule)) {
                General.showError("Failed to invoke rule from: "+ruleFrameCodeName);
                return false;
            }
        }
        return true;
    }

    /**
     * STARFilter using the given files.
     */
    public boolean filter(String[] args) {
        DataBlock dbRules =null;
        if ( args == null ) {
            General.doErrorExit("Expected three arguments but got null");
        }
        if ( args.length != 3 ) {
            General.showError("Expected three arguments but got: " + args.length + General.eol +
                    ". They were: " + Strings.toString(args));
            return false;
        }
        StarFileReader sfr = new StarFileReader(new DBMS());
        if ( ! InOut.filesExist(new String[] {args[0], args[2] })) {
            General.showError("Input file or rules file does not exist.");
            return false;
        }

        // Parse rules
        boolean skipFiltering = false;
        if ( args[2].equalsIgnoreCase(".") ) {
            // skip filtering itself.
            skipFiltering = true;
        } else {
            URL urlRules = InOut.getUrlFileFromName(args[2]);
            long start = System.currentTimeMillis();
            StarNode snRules = sfr.parse(urlRules);
            long taken = System.currentTimeMillis() - start;
            General.showDebug("Parse rules  : " + taken + "(" + (taken/1000.0) + " sec)" );
            if ( snRules == null ) {
                General.showError("Failed to parse rules file.");
                return false;
            }
            dbRules = (DataBlock) snRules.get(0);
        }

        // Parse input
        sfr = new StarFileReader(new DBMS());     //to a roam in a new dbms
        URL url = InOut.getUrlFileFromName(args[0]);
//        long start = System.currentTimeMillis();
        StarNode sn = sfr.parse(url);
//        long taken = System.currentTimeMillis() - start;
//        General.showDebug("Parse took   : " + taken + "(" + (taken/1000.0) + " sec)" );
        if ( sn == null ) {
            General.showError("Failed to parse input file.");
            return false;
        }
        db = (DataBlock) sn.get(0);


        // STARFilter
        if ( ! skipFiltering ) {
//            start = System.currentTimeMillis();
            if ( ! filter(dbRules)) {
                General.showError("Failed to filter.");
                return false;
            }
//            taken = System.currentTimeMillis() - start;
//            General.showDebug("Filter took  : " + taken + "(" + (taken/1000.0) + " sec)" );
        }

        // Unparse filtered results.
//        start = System.currentTimeMillis();
        if ( ! db.toSTAR(args[1])) {
            General.showError("Failed to unparse to outputfile.");
            return false;
        }
//        taken = System.currentTimeMillis() - start;
//        General.showDebug("Unparse took : " + taken + "(" + (taken/1000.0) + " sec)" );

        return true;
    }



    /**E.g.
save_insertTag_1
   _Saveframe_category          insertTag
   loop_
      _Saveframe_category_name
      _Tag_name_identifying
      _Tag_name
      _Value
      _Location # Use 0 or . to put the tag at the begin or end.

      distance_constraints  "_Dist_constraint.Auth_segment_code" "_Entry_id" ENTRY_ID 0
   stop_
save_
     *The SF category can be a wildcard in which case SF of all categories will get the new
     *tag. The identifying tag can also be a wildcard.
     */
     public boolean insertTag( SaveFrame sFRule ) {
        TagTable tTSFCategoryName = sFRule.getTagTable(SAVEFRAME_CATEGORY_NAME,true);
        if ( tTSFCategoryName == null ) {
            General.showError("Failed to get TagTable with tag name: " + SAVEFRAME_CATEGORY_NAME);
            return false;
        }
        String[] sFCategoryList = tTSFCategoryName.getColumnString(SAVEFRAME_CATEGORY_NAME);
        String[] tagNameIdList  = tTSFCategoryName.getColumnString(TAG_NAME_IDENTIFYING);
        String[] tagNameList    = tTSFCategoryName.getColumnString(TAG_NAME);
        String[] valueList      = tTSFCategoryName.getColumnString(VALUE);
        String[] locList        = tTSFCategoryName.getColumnString(LOCATION);


        if ( ! db.insertTag( sFCategoryList,
                             tagNameIdList,
                             tagNameList,
                             valueList,
                             locList,
                             tTSFCategoryName.sizeRows
                )) {
            General.showError("Failed to insertTag for tagNameList: " + Strings.toString(tagNameList));
            return false;
        }
        return true;
    }


    /**E.g.
save_removeAllButTagNames_1
   _Saveframe_category          removeAllButTagNames
   _Saveframe_category_name     *
   loop_
      _Tag_name
       "_Dist_constraint.Auth_segment_code"
       "_Dist_constraint.Auth_seq_ID"
       "_Dist_constraint.Auth_comp_ID"
       "_Dist_constraint.Auth_atom_ID"
   stop_
save_
     *The SF category can be a wildcard. Will remove all tags in given SF category except for the
     *tags given. If SF category is a wildcard remove all tags even if the tags to keep don't
     *occur, effectively removing all data but the specified. Specify a SF category if you don't
     *want that behaviour.
     */
     public boolean removeAllButTagNames( SaveFrame sFRule ) {
        TagTable tTSFCategoryName = sFRule.getTagTable(SAVEFRAME_CATEGORY_NAME,true);
        if ( tTSFCategoryName == null ) {
            General.showError("Failed to get TagTable with tag name: " + SAVEFRAME_CATEGORY_NAME);
            return false;
        }
        String SFCatToKeep      = tTSFCategoryName.getValueString(0,SAVEFRAME_CATEGORY_NAME);

        TagTable tTTagNameList = sFRule.getTagTable(TAG_NAME,true);
        if ( tTTagNameList == null ) {
            General.showError("Failed to get TagTable with tag name list: " + TAG_NAME);
            return false;
        }
        StringSet sS = tTTagNameList.getColumnStringSet(TAG_NAME);
        StringArrayList TtTagNameListToKeep = new StringArrayList(sS.hm.keySet());

        if ( ! db.removeAllButTagNames( SFCatToKeep, TtTagNameListToKeep )) {
            General.showError("Failed to removeAllButTagNames for categoriy to keep: " + SFCatToKeep +
                    " and tag names: " + Strings.toString(TtTagNameListToKeep));
            return false;
        }
        return true;
    }

/**
 *E.g.
save_removeAllButTagTableWithTagName_1

   _Saveframe_category          removeAllButTagTableWithTagName
   _Saveframe_category_name     *
   _Tag_name                    "_Dist_constraint.Auth_segment_code"

save_
 *SF can be a wild card.
 */
    public boolean removeAllButTagTableWithTagName( SaveFrame sFRule ) {
        TagTable tTSFCategoryName = sFRule.getTagTable(SAVEFRAME_CATEGORY_NAME,true);
        if ( tTSFCategoryName == null ) {
            General.showError("Failed to get TagTable with tag name: " + SAVEFRAME_CATEGORY_NAME);
            return false;
        }
        String SFCatToKeep      = tTSFCategoryName.getValueString(0,SAVEFRAME_CATEGORY_NAME);
        String TtTagNameToKeep  = tTSFCategoryName.getValueString(0,TAG_NAME);

        if ( ! db.removeAllButTagTableWithTagName( SFCatToKeep, TtTagNameToKeep )) {
            General.showError("Failed to removeAllButTagTableWithTagName for SFCatToKeep: " + SFCatToKeep+
                    " and TtTagNameToKeep to keep: " + TtTagNameToKeep);
            return false;
        }
        return true;
    }

    public boolean removeAllButSaveframeCategories( SaveFrame sFRule ) {
        TagTable tTSFCategoryName = sFRule.getTagTable(SAVEFRAME_CATEGORY_NAME,true);
        if ( tTSFCategoryName == null ) {
            General.showError("Failed to get TagTable with tag name: " + SAVEFRAME_CATEGORY_NAME);
            return false;
        }
        StringSet sS = tTSFCategoryName.getColumnStringSet(SAVEFRAME_CATEGORY_NAME);
        StringArrayList SFCatToKeep = new StringArrayList(sS.hm.keySet());
        if ( ! db.removeAllButSaveframeCategories( SFCatToKeep )) {
            General.showError("Failed to removeAllButSaveframeCategories for categories to keep: " + Strings.toString(SFCatToKeep));
            return false;
        }
        return true;
    }

    public boolean removeSaveframeCategories( SaveFrame sFRule ) {
        TagTable tTSFCategoryName = sFRule.getTagTable(SAVEFRAME_CATEGORY_NAME,true);
        if ( tTSFCategoryName == null ) {
            General.showError("Failed to get TagTable with tag name: " + SAVEFRAME_CATEGORY_NAME);
            return false;
        }
        StringSet sS = tTSFCategoryName.getColumnStringSet(SAVEFRAME_CATEGORY_NAME);
        StringArrayList SFCatToRemove = new StringArrayList(sS.hm.keySet());
        if ( ! db.removeSaveframeCategories( SFCatToRemove )) {
            General.showError("Failed to removeSaveframeCategories for categories to keep: " + Strings.toString(SFCatToRemove));
            return false;
        }
        return true;
    }

    public static void showUsage() {
        General.showOutput("Usage: java Wattos.Star.STARFilter inputFile outputFile ruleFile");
        General.showOutput("    ruleFile may be a '.' to denote no rules to be done; just a pass thru.");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        STARFilter Sfilter = new STARFilter();
//        General.setVerbosityToDebug();
//        General.showDebug("test");
        if ( ! Sfilter.filter(args)) {
            showUsage();
            General.doErrorExit("Failed to STARFilter"); // give a error status code exit.
        }
    }
}
