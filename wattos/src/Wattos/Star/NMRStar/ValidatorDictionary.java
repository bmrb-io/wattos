/*
 * Created on April 25, 2003, 2:35 PM
 */

package Wattos.Star.NMRStar;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;

import Wattos.Database.DBMS;
import Wattos.Database.SQLSelect;
import Wattos.Star.DataBlock;
import Wattos.Star.SaveFrame;
import Wattos.Star.StarFileReader;
import Wattos.Star.StarGeneral;
import Wattos.Star.StarNode;
import Wattos.Star.TagTable;
import Wattos.Utils.General;
import Wattos.Utils.StringArrayList;

/**
 * Contains NMR-STAR 3.x dictionary as used in Dmitri's validator
 * Used here for sorting tag names and perhaps more in the future.
 * see Data/validict.20070227.3.str or equivalent file</a>
 * @author Jurgen F. Doreleijers
 */
public class ValidatorDictionary implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;

    public DBMS dbms = new DBMS();
    // TODO remove the next dependency on STARLIB if we don't want to serialize it with the rest of UserInterface.
    public TagTable tagTt = null;
    /** Local resource */
//    static final String FILE_LOCATION = "Data/validict.3.str";
    /** From ftp://ftp.bmrb.wisc.edu/pub/software/bmrb/validator/validict.20080404.1.str */
//    static final String FILE_LOCATION = "Data/validict.20080404.1.str";
    /** From grunt:/raid/docr/workspace/validict.20100106.1.str */
    static final String FILE_LOCATION = "Data/validict.20100106.1.str";

    static final String TAGCAT  = "_TAGCAT";
    static final String TAGNAME = "_TAGNAME";
    public String NMR_STAR_version = null;
    public ValidatorDictionary() {
    }

    /** Read the possible star names.
     *Uses standard location if argument is null;
     * @param file_location Url for file with the comma separated info.
     * @return <CODE>true</CODE> if all operations are successful.
     */
    public boolean readFile(String file_location) {
        if ( file_location == null ) {
            file_location = FILE_LOCATION;
//            General.showDebug("Reading Star dictionary from local resource : " + file_location);
        }
        StarFileReader sfr = new StarFileReader(dbms);
        InputStream file_is = getClass().getResourceAsStream(file_location);
        StarNode sn = sfr.parse(new BufferedInputStream(file_is));
        if ( sn == null ) {
            General.showError("parse unsuccessful");
            return false;
        }
//        General.showDebug("Read sn: " + sn.toString());

        DataBlock db = (DataBlock) sn.datanodes.get(0);
        SaveFrame sF = db.getSaveFrameByName("TAGS", true);

        tagTt = sF.getTagTable(StarGeneral.WILDCARD, true);

//        General.showDebug("Read number of star tag names from Validator STAR dictionary: " +
//                tagTt.sizeRows);
        // Get version
        //"3.0.8.100"
        SaveFrame sFVersion = db.getSaveFrameByName("INFO", true);
        TagTable tagTtVersion = sFVersion.getTagTable(StarGeneral.WILDCARD, true);
        NMR_STAR_version = tagTtVersion.getValueString(0, "_VERSION");;
//        General.showDebug("Dictionary looks like: ");
//        General.showDebug(sn.toSTAR().substring(0,1000));
        return true;
    }

    /**
     * For each tagTable lookup order of set E of existing tagnames in dictionary.
     * Order the set W of Wattos tagnames such that:
     * Let U=W-E (tagnames in W but not in E)
     *     V=WuE (tagnames in both W and E)
     *  - order of tagnames in U unchanged.
     *  - tagnames in U precede those in V.
     * @param sn
     * @return
     */
    public boolean sortTagNames(StarNode sn) {
        ArrayList tTList = sn.getTagTableList(StarGeneral.WILDCARD,
                StarGeneral.WILDCARD,
                StarGeneral.WILDCARD,
                StarGeneral.WILDCARD);
        for (int i=0;i<tTList.size();i++) {
            TagTable tT = (TagTable) tTList.get(i);
            String tagCat = getTagCat(tT);
            if ( tagCat == null ) {
                General.showDebug("Failed to find tagCat (leaving original order) for tT: " + tT.toString(true, false, false, false, false, false));
                continue;
            }
//            General.showDebug("Sorting: tagCat: ["+tagCat+"]");
            BitSet E = SQLSelect.selectBitSet(dbms,tagTt,TAGCAT,SQLSelect.OPERATION_TYPE_EQUALS,tagCat,false);
            StringArrayList salE = new StringArrayList();
            for (int j = E.nextSetBit(0); j >= 0; j=E.nextSetBit(j+1)) {
                salE.add( tagTt.getValueString(j, TAGNAME) );
            }
            if ( ! tT.reorderSomeColumns(salE)) {
                General.showError("Failed to tT.reorderSomeColumns(salE) in ValidatorDictionary.sortTagNames");
                return false;
            }
        }
        return true;
    }

    /** Tries all tagnames (usually just 1) in table for tagcat
     * or returns null if absent.
     * This assumes all tagnames are unique.
     * @param tt
     * @return
     */
    public String getTagCat(TagTable tt) {
        for (int i=0;i<tt.sizeColumns();i++) {
            String label = tt.getColumnLabel(i);
//            General.showDebug("Trying SQL on tT:  " + tt.name);
            BitSet bs = SQLSelect.selectBitSet(dbms,tagTt,TAGNAME,SQLSelect.OPERATION_TYPE_EQUALS,label,false);
            int matchRid = bs.nextSetBit(0);
            if ( matchRid < 0 ) {
//                General.showDebug("column not present: " + label);
                continue;
            }
            return tagTt.getValueString(matchRid, TAGCAT);
        }
        return null;
    }

}
