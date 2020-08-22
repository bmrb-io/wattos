/*
 * Created on April 25, 2003, 2:35 PM
 */

package Wattos.Star.NMRStar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import Wattos.Database.Defs;
import Wattos.Database.Relation;
import Wattos.Utils.General;
import Wattos.Utils.HashOfHashes;
import Wattos.Utils.Strings;

import com.Ostermiller.util.ExcelCSVParser;

/**
 * Contains mapping for different formats of NMR-STAR, starting with 3.0 to read to WattosComponents.
 * Much of the logic will still reside in code but what's attempted here is to allow a versatile
 * mapping from the column names in the relations in Wattos to the tag names in the saveframes of star
 * files and vice versa. This should result in a code base that's void of any star tag names at all.
 * The organization is:
 * <PRE>
 * STAR version by string e.g.:      "3.0"
 * Saveframe category         :      "entity"
 * Tag name                   :      "_Entity_comp_index.Comp_ID"
 * mmCIF Tag Name             :      "_Entity_comp_index.Comp_ID" TODO...
 * Text format                :      "%.3f"
 * Wattos relation            :      "res_main"
 * Wattos name                :      "name"
 * Wattos data type           :      "STRINGNR"
 * </PRE>
 * Even though many tags in STAR do not have a direct representative in Wattos there will always be
 * a name. This surrogate name will be used for temporary use of the data e.g. with reading in and
 * writing out the data. These surrogate names will be names in upper case.
 * The tag names are unique over a whole entry. The names in Wattos relation are only unique within
 * that relation.
 * Maps FROM nmr-star to wattos:
 * <PRE> HashOfHashes fromStar2D -> ArrayList
 *      "Saveframe category" -> "Tag name" -> ("Wattos relation", "Wattos name", "Wattos data type")
 * HashMap fromStar -> ArrayList
 *      "Tag name" -> ("Wattos relation", "Wattos name", "Wattos data type")
 * Maps TO nmr-star from wattos:
 * HashOfHashes toStar2D -> ArrayList
 *      "Wattos relation" -> "Wattos name" -> ("Saveframe category", "Tag name", "Text format")
 * </PRE>The maps are not supposed to be modified after loading.
 * The maps are intended to be extended in the future when other info might be usefull too.
 *@see <a href="Data/WattosDictionary_STAR30.csv">WattosDictionary_STAR30.csv</a>
 * @author Jurgen F. Doreleijers
 */
public class StarDictionary implements Serializable {

    private static final long serialVersionUID = -1207795172754062330L;
    /** Definitions for a table. */
    static final String DEFAULT_TABLE_BEGIN     = "TBL_BEGIN";
    static final String DEFAULT_TABLE_END       = "TBL_END";

    static final String COLUMN_NAME_RELATION_NAME   = "WattosRelationName";
    static final String COLUMN_NAME_ATTR_NAME       = "WattosAttributeName";
    static final String COLUMN_NAME_DATA_TYPE       = "WattosDataType";
    static final String COLUMN_NAME_TEXT_FORMAT     = "TextFormat";
    static final String COLUMN_NAME_SF_CATEGORY     = "SFCategory";
    static final String COLUMN_NAME_TAG_NAME        = "TagName";
    static final String COLUMN_NAME_TAG_NAME_CIF    = "TagNameCIF";
    static final String COLUMN_NAME_COMMENTS        = "Comments";

    static final String[] columnList = {
        COLUMN_NAME_RELATION_NAME,
        COLUMN_NAME_ATTR_NAME,
        COLUMN_NAME_DATA_TYPE,
        COLUMN_NAME_TEXT_FORMAT,
        COLUMN_NAME_SF_CATEGORY,
        COLUMN_NAME_TAG_NAME,
        COLUMN_NAME_TAG_NAME_CIF,
        COLUMN_NAME_COMMENTS
    };

    // Positions in ("Wattos relation", "Wattos name", "Wattos data type")
    public static final int POSITION_WATTOS_RELATION              = 0;
    public static final int POSITION_WATTOS_NAME                  = 1;
    public static final int POSITION_WATTOS_DATATYPE              = 2;

    // Positons in ("Saveframe category", "Tag name", "Text format")
    public static final int POSITION_STAR_CATEGORY              = 0;
    public static final int POSITION_STAR_TAG_NAME              = 1;
    public static final int POSITION_STAR_TAG_FORMAT            = 2;

    // Positons in ("Tag name", "Text format")
    public static final int POSITION_CIF_TAG_NAME              = 0;
    public static final int POSITION_CIF_TAG_FORMAT            = 1;

    /**Keys SF_CATEGORY, TAG_NAME. Values: RELATION_NAME, ATTR_NAME, DATA_TYPE */
    public HashOfHashes  fromStar2D;
    /**Key TAG_NAME. Values: RELATION_NAME, ATTR_NAME, DATA_TYPE*/
    public HashMap       fromStar;
    /**Keys RELATION_NAME, ATTR_NAME. Values: SF_CATEGORY, TAG_NAME, TEXT_FORMAT*/
    public HashOfHashes  toStar2D;
    /**Keys RELATION_NAME, ATTR_NAME. Values: TAG_NAME, TEXT_FORMAT*/
    public HashOfHashes  toCIF2D;
    /**Key TAG_NAME_CIF. Values: RELATION_NAME, ATTR_NAME, DATA_TYPE */
    public HashMap  fromCIF;

    /** Local resource */
    static final String CSV_FILE_LOCATION = "Data/WattosDictionary_STAR31.csv";

    /** Settings within for a change */
    public StarDictionary() {
        fromStar2D = new HashOfHashes();
        fromStar   = new HashMap();
        fromCIF    = new HashMap();
        toStar2D   = new HashOfHashes();
        toCIF2D    = new HashOfHashes();
    }

    /** Given category and key with possible whitespace added
     *return the tag name.
     */
    public String getTagName(String cat, String key) {
        String keyStr1 = cat.trim();
        String keyStr2 = key.trim();
        ArrayList info = (ArrayList) toStar2D.get( keyStr1,keyStr2);
        if ( info == null ) {
            General.showError("Failed to find key: " + key + " in category: " + cat);
            return null;
        }
        return (String) info.get(POSITION_STAR_TAG_NAME);
    }

    /** Given category and key with possible whitespace added
     *return the tag name.
     */
    public String getTagNameCIF(String cat, String key) {
        String keyStr1 = cat.trim();
        String keyStr2 = key.trim();
        ArrayList info = (ArrayList) toCIF2D.get( keyStr1,keyStr2);
        if ( info == null ) {
            General.showError("Failed to find key: " + key + " in category: " + cat);
            return null;
        }
        return (String) info.get(POSITION_CIF_TAG_NAME);
    }

    /** Returns data type as defined by dictionary or -1 for failure.
     */
    public int getDataType(String tagName, boolean isMMCIF) {
        ArrayList info = null;

        if ( isMMCIF ) {
            info = (ArrayList) fromCIF.get(tagName);
        } else {
            info = (ArrayList) fromStar.get(tagName);
        }
        if ( info == null ) {
            General.showError("Tag not found (1): " + tagName);
            return -1;
        }
        String dataTypeString = (String) info.get(POSITION_WATTOS_DATATYPE);
        if ( dataTypeString == null ) {
            General.showCodeBug("Tag not found (2): " + tagName);
            return -1;
        }
        // Reuse variable for new data type
        int result = Relation.dataTypeArrayList.indexOf( dataTypeString );
        if ( result < 0 ) {
            General.showError("Tag has a data type listed in dictionary that is not supported: [" + dataTypeString +"]");
            General.showError("Known are: " + Strings.concatenate( Relation.dataTypeList, "," ) );
            return -1;
        }
        return result;
    }

    public boolean putFromDict( HashMap namesAndTypes, ArrayList order,
            String tagName ) {
        int result = getDataType(tagName, false);
        if ( result < 0 ) {
            General.showWarning("Failed to get datatype for tagName: " + tagName);
            return false;
        }
        Object v = namesAndTypes.put( tagName, new Integer(result));
        if ( v != null ) {
            General.showWarning("Failed to put tagName: " + tagName +
                        " with type: " + result + " because there already was such a key.");
            General.showWarning("Value to the key: " + v);
            return false;
        }
        if ( ! order.add(tagName)) {
            General.showWarning("Failed to add in order, tagName: " + tagName +
                        " with type: " + result);
            return false;
        }

        return true;
    }

    /** Read the possible star names.
     *Uses standard location if argument is null;
     * @param csv_file_location Url for file with the comma separated info.
     * @return <CODE>true</CODE> if all operations are successful.
     */
    public boolean readCsvFile(String csv_file_location) {
        if ( csv_file_location == null ) {
//            General.showDebug("Reading Star dictionary from local resource : " + CSV_FILE_LOCATION);
        }
        String[][] values = null;
        int FIRST_ROW_WITH_DATA = 1;
        int LAST_ROW_WITH_DATA = -1;
        try {
            Reader reader = null;
            if ( csv_file_location == null ) {
                InputStream csv_file_is = getClass().getResourceAsStream(CSV_FILE_LOCATION);
                reader = new InputStreamReader( csv_file_is );
            } else {
                reader = new FileReader( csv_file_location );
            }
            if ( reader == null ) {
                General.showWarning("Failed to open resource as a stream from location: " + csv_file_location);
                return false;
            }

            BufferedReader br = new BufferedReader( reader );
            ExcelCSVParser parser = new ExcelCSVParser(br);
            if ( parser == null ) {
                General.showWarning("Failed to open Excel CSV parser from location: " + csv_file_location);
                return false;
            }
            // Parse the data
            values = parser.getAllValues();
            if ( values == null ) {
                General.showError("no data read from csv file");
                return false;
            }
            if ( values.length <= 0 ) {
                General.showError("number of rows found: " + values.length);
                General.showError("but expected at least      : " + 1);
                return false;
            }
            values = Strings.deleteAllWhiteSpace(values);
            ArrayList columnLabels = new ArrayList( Arrays.asList( values[0] ) );

            /** Get the interesting column indexes */
            int[] columnIdx = new int[ columnList.length ];
            for (int c=0;c<columnList.length;c++) {
                columnIdx[c] = columnLabels.indexOf( columnList[c] );
                if ( columnIdx[c] == -1 ) {
                    General.showError("failed to find a column with label: " + columnList[c]);
                    General.showError("Found column labels: " + columnLabels );
                    return false;
                }
                //General.showDebug(" found column name: " + columnList[c] + " in column number: " + columnIdx[c] );
            }

            FIRST_ROW_WITH_DATA = 1;
            LAST_ROW_WITH_DATA = values.length - 1;
//            General.showDebug("Data starts at row:  "+FIRST_ROW_WITH_DATA);
//            General.showDebug("Data ends after row: "+LAST_ROW_WITH_DATA);


            /** Create bogus items to start */
            String[] v = new String[columnList.length];
            for (int r=FIRST_ROW_WITH_DATA;r<=LAST_ROW_WITH_DATA;r++) {
                //General.showDebug(" doing row: " + r + " which reads (csv) with: " + Strings.toString( values[r] ));
                for (int c=0;c<columnList.length;c++ ) {
                    v[c]    = values[r][ columnIdx[c] ];
                }
                //General.showDebug("v: [" + Strings.toString(v));
                // Name them.
                String RELATION_NAME   = v[0];
                String ATTR_NAME       = v[1];
                String DATA_TYPE       = v[2];
                String TEXT_FORMAT     = v[3];
                String SF_CATEGORY     = v[4];
                String TAG_NAME        = v[5];
                String TAG_NAME_CIF    = v[6];
//                String COMMENTS        = v[7];

                ArrayList alWattos = new ArrayList();
                alWattos.add( RELATION_NAME );
                alWattos.add( ATTR_NAME );
                alWattos.add( DATA_TYPE );
                ArrayList alStar = new ArrayList();
                alStar.add( SF_CATEGORY );
                alStar.add( TAG_NAME );
                alStar.add( TEXT_FORMAT );
                ArrayList alCif = new ArrayList();
                alCif.add( TAG_NAME_CIF );
                alCif.add( TEXT_FORMAT );

                fromStar2D.put( SF_CATEGORY, TAG_NAME, alWattos );
                toStar2D.put( RELATION_NAME, ATTR_NAME, alStar);
                fromStar.put( TAG_NAME, alWattos );
                if ( ! Defs.isNullString(TAG_NAME_CIF)) {
                    fromCIF.put( TAG_NAME_CIF, alWattos );
                    toCIF2D.put( RELATION_NAME, ATTR_NAME, alCif);
                }

            }
        } catch ( Throwable t ) {
            General.showThrowable(t);
            return false;
        }

        /** Some debugging: */
//        General.showDebug("Read number of star tag names from STAR dictionary: " + (LAST_ROW_WITH_DATA-FIRST_ROW_WITH_DATA+1));
//        General.showDebug("Dictionary looks like: ");
//        General.showDebug(toString());
        return checkConsistency();
    }

    private boolean checkConsistency() {
        Object[] sfCategoryList = fromStar2D.keySet().toArray();
        for (int i=0;i<sfCategoryList.length;i++) {
            HashMap tagNameMap = (HashMap) fromStar2D.get(sfCategoryList[i]);
            String[] tagNameList = Strings.toStringArray(tagNameMap.keySet().toArray());
            for (int j=0;j<tagNameList.length;j++) {
                String tagName = tagNameList[j];
                if ( ! niceTagName( tagName )) {
                    return false;
                }
//                ArrayList alWattos = (ArrayList) tagNameMap.get( tagNameList[j]);
            }
        }
        return true;
    }

    private boolean niceTagName(String tagName) {
        int idxDot = tagName.indexOf(".");
        int lengthPartAfterDot = tagName.length() - idxDot - 1;
        if ( lengthPartAfterDot > 31 ) {
            General.showError("Found too long ("+lengthPartAfterDot+">31) part after dot in tag: ["+tagName+"]");
            return false;
        }
        return true;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("fromCIF\n");
        sb.append(Strings.toString(fromCIF));
        sb.append("fromStar\n");
        sb.append(Strings.toString(fromStar));
        return sb.toString();
    }
}
