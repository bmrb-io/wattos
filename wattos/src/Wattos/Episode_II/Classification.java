/*
 * Classification.java
 *
 * Created on January 14, 2002, 11:44 AM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import Wattos.Utils.*;
import java.util.*; 
import java.io.*;

/**
 * See below links for 
 * data files for the possible classifications for annotating blocks of textual 
 * information of PDB MR files.
 * @author  Jurgen F. Doreleijers
 * @version 0.1  
 * @see <a href="Data/classification.csv">CSV data files</a> 
 * @see <a href="Data/classification.xls">MS Excel data files</a>
 */
public class Classification {

    /**An array of DBMRBlock info     */
    ArrayList mrb_list = new ArrayList();
    /**This will hold the data; an array of DBMRBlock info
     *not the lines themselves. */
    ArrayList mrb_list_converters = new ArrayList();
    /** Contains a parallel list of Integers which correspond to the programs
     *as defined in DBMRBlock code taken from the classification csv file.
     */
    ArrayList mrb_list_converters_map = new ArrayList();
     
    /** A map from mrb type (key) to sf_category (value) */
    HashMap sf_category_map = new HashMap();
    
    HashOfHashesOfHashes conversionPossibilities = new HashOfHashesOfHashes();
    
    /** Minimum number of characters for a line in a csv file to be considered.
     */
    static final int LINES_MIN_CHARS = 14;
    
    /* Number of columns after the types columns that contain dummy info */
    static final int DUMMY_COLUMNS = 3;

    static final int NO_CONVERSION_PROGRAM_AVAILABLE_ID = -1;
    /* Naming scheme is PROGRAM, TYPE, SUBTYPE, FORMAT
     Replacing where one id doesn't matter: ANY
    Denote n/a by NA.
     *The codes used here should also occur in the classification.csv file
     *if the conversion actually exists.
     *JFD notes: why was NOE used for many more than logically expected.
     */
    static final int CONVERSION_CNS_DISTANCE_NOE_NA         = 1;
    static final int CONVERSION_CNS_DIHEDRAL_NA_NA          = 2;
    static final int CONVERSION_CNS_DIPOLAR_NA_NA           = 3;
    static final int CONVERSION_DYANA_DISTANCE_NOE_NA       = 101;
    static final int CONVERSION_DYANA_DIHEDRAL_NOE_NA       = 102;
    static final int CONVERSION_DYANA_DIPOLAR_NOE_NA        = 103;
    static final int CONVERSION_DISCOVER_DISTANCE_NOE_NA    = 201;
    static final int CONVERSION_DISCOVER_DIHEDRAL_NOE_NA    = 202;
    static final int CONVERSION_DISCOVER_DIPOLAR_NOE_NA     = 203;
    static final int CONVERSION_EMBOSS_DISTANCE_NOE_NA      = 301;
    static final int CONVERSION_EMBOSS_DIHEDRAL_NOE_NA      = 302;
    static final int CONVERSION_AMBER_DISTANCE_NOE_NA       = 401;
    static final int CONVERSION_AMBER_DIHEDRAL_NOE_NA       = 402;
    static final int CONVERSION_AMBER_DIPOLAR_NOE_NA        = 403;
    static final int CONVERSION_ANY_COMMENT_NA_NA           = 99;
    //etc.

    static final String ALLOWED_TYPE       = "X";
    static final String NOT_ALLOWED_TYPE   = "-";
    
    /** Creates new Classification */
    public Classification() {
    }

    /** Returns the concatenated textual representations of the blocks in this instance
     * of classification. I.e. the allowed combinations.
     * @return The concatenated string.
     */
    public String toString()
    {
        String value = "";
        value = value + "\n\nmrbs available:\n";
        for (Iterator i=mrb_list.iterator();i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            value = value + mrb.getType() + General.eol;
        }            
        value = value + "\n\nmrb_list_converters available:\n";
        int index = 0;
        for (Iterator i=mrb_list_converters.iterator();i.hasNext();) {
            DBMRBlock mrb = (DBMRBlock) i.next();
            int program_id = ((Integer) mrb_list_converters_map.get(index++)).intValue();
            value = value + mrb.getType() + " " + program_id + General.eol;
        }            

        value = value + "\n\nsf category map items:\n";
        Set keys = sf_category_map.keySet();
        for (Iterator i=keys.iterator();i.hasNext();) {
            DBMRBlock key = (DBMRBlock) i.next();
            Object item_value = sf_category_map.get(key);
            value = value + key.getBlockType() + ":" + item_value + General.eol;
        }      
        
        value = value + "\n\n"+ conversionPossibilities.toString();
        
        return value;
    }

    /** Read the possible block types from a comma delimited file.
     * Since the standard tokenizer is used, values can not be empty strings.
     * For programs, a 'X' or 'Y' denotes a possibility. 
     * A warning will be issued if
     * something else than these or '-' is encountered.
     * The csv file should be formatted as follows:
     * <pre>
,,,,In schema:,,,,,,,,,,,,,,,,,,,,,,,,,,,
Type,Subtype,Format,Saveframe Category,2.1.1,3.0,dev,n/a,STAR,MR format,XPLOR/CNS,AMBER,AQUA,DISCOVER,CONGEN,DYANA/DIANA,DISGEO,DISTGEO,DISMAN,GROMOS,DSPACE,DGII,EMBOSS,SYBYL,TINKER,TRIPOS,XEASY,PIPP,PDB,NMRCOMPASS,SHIFTY,MARDIGRAS/CORMA
angle,n/a,n/a,MR_file_angle,,,Y,X,X,X,X,X,-,X,X,X,X,X,X,X,X,X,-,X,-,-,-,-,-,-,-,-
distance,disulfide bond,ambi,distance_constraints,,Y,,X,X,X,X,X,-,X,X,X,-,-,-,-,-,-,-,-,-,-,-,-,-,-,-,-
distance,disulfide bond,simple,distance_constraints,,Y,,X,X,X,Y,X,X,X,X,X,X,X,X,X,X,X,X,X,X,X,-,-,-,-,-,X
     * </pre>
     * The only variation is in the number of additional types and the number of
     * programs listed on each file.
     * @param csv_file_location Url for file with the comma separated info.
     * @return <CODE>true</CODE> if all operations are successful.
     */
    public boolean readFromCsvFile(String csv_file_location)
    {
        StringTokenizer st;
        BufferedReader in=null;
        InputStream is=null;
        try {
            is = Classification.class.getResourceAsStream(csv_file_location);
            InputStreamReader isr = new InputStreamReader(is);
            in = new BufferedReader(isr);
        } catch ( Exception e ) {
            General.showWarning("Failed to read the csv file from location: ["+csv_file_location+"]");
            General.showThrowable(e);
            return false;
        }

        //General.showDebug("Reading file from location: " + csv_file_location);
        try { 
            String line;
            // Read away the additional header line
            line = in.readLine();
            // Read the header line and determine how many programs we have            
            // ignoring content from columns: 
            line = in.readLine();
            //General.showOutput("Read line: [" + line + "]");
            st = new StringTokenizer(line);
            String type         = st.nextToken(",");
            String subtype      = st.nextToken(",");
            String format       = st.nextToken(",");
            String sf_category  = st.nextToken(",");  // Ignored from here.
            
            for ( int i=0; i < DUMMY_COLUMNS; i++ ) { // Ignored from here.
                st.nextToken(",");
            }
            ArrayList programs = new ArrayList();
            while (st.hasMoreTokens()) {
                programs.add(st.nextToken(","));                
            }
            
            // Read types per line
            while((line = in.readLine()) != null ) {
                // Skipp lines shorter than 10 characters (that will
                // take care of empty lines.
                if (line.length()<LINES_MIN_CHARS) {
                    General.showWarning("Skipped too short line.. (less than "+
                        LINES_MIN_CHARS + " characters.)");
                    continue; 
                }
                st = new StringTokenizer(line);
                type        = st.nextToken(",");
                subtype     = st.nextToken(",");
                format      = st.nextToken(",");
                sf_category = st.nextToken(",");
                int index = 0;

                // Read away dummy columns
                for ( int i=0; i < DUMMY_COLUMNS; i++ ) {
                    st.nextToken(",");
                }
                
                DBMRBlock mrb_map = new DBMRBlock();
                mrb_map.type[0] = "."; // Not used.
                mrb_map.type[1] = type;
                mrb_map.type[2] = subtype;
                mrb_map.type[3] = format;
                mrb_map.trimTypeValues();
                sf_category_map.put(mrb_map, sf_category);

                while (st.hasMoreTokens()) {
                    String prog = st.nextToken(",");
                    //General.showOutput("Reading token: [" + prog + "]");
                    boolean are_digits = Strings.areDigits( prog );
                    if ( prog.equals( ALLOWED_TYPE ) || are_digits ) {
                        DBMRBlock mrb = new DBMRBlock();
                        if ( index > ( programs.size() -1 ) ) {
                            General.showError("in Classification.readFromCsvFile found: index out of bounds:"+index);
                            General.showError( programs.get(index).toString());
                            General.showError( type);
                            General.showError( subtype);
                            General.showError( format);
                        }
                        mrb.type[0] = programs.get(index).toString();
                        mrb.type[1] = type;
                        mrb.type[2] = subtype;
                        mrb.type[3] = format;
                        mrb.trimTypeValues();
                        mrb_list.add(mrb);
                        if ( are_digits ) {
                            // No need to check parse as the areDigits function is good enough a check.
                            mrb_list_converters_map.add( new Integer( Integer.parseInt( prog ) ) );
                            mrb_list_converters.add(mrb);
                            if ( ! prog.equals("99")) { // ignore comments
                                Object v = conversionPossibilities.get(
                                   mrb.type[0],
                                   mrb.type[1],
                                   mrb.type[2]);
                                if ( v instanceof ArrayList ) {
                                    ArrayList va = (ArrayList) v;
                                    va.add( mrb.type[3]);
                                } else {
                                    ArrayList va = new ArrayList();
                                    va.add( mrb.type[3]);
                                    conversionPossibilities.put( 
                                        mrb.type[0],
                                        mrb.type[1],
                                        mrb.type[2],
                                        va
                                    );
                                }
                            }
                        }                            
                    } else if ( ! prog.equals("-") ) {
                        General.showWarning("found: "+prog);
                        General.showWarning("but expected '" + ALLOWED_TYPE + "', digit, or '"+
                                            NOT_ALLOWED_TYPE + "'; ignoring line");
                        General.showWarning("ignoring line:\n" + line);
                    }
                    index++;
                }
            }
            in.close();
        } catch (IOException e) {
            General.showError("in Classification.readFromCsvFile found:");
            General.showError("error reading file from location: "+csv_file_location);
            General.showError("make sure that all program fields have - or + values");
            General.showError("and there are no empty cells generated; check csv file");
            return false;
        }
        //General.showDebug("Read: ["+mrb_list.size()+"] block types");
        return true;
    }
    
    
    /** Self test; Reads in the classification file and shows content.
     * @param args Ignored.
     */
    public static void main (String args[]) {
        General.showOutput("Starting test of check routine." );
        if ( true ) {
//            Globals g = new Globals();

            Classification classi = new Classification();        
            // Note the system independent regular slash. This also works on Windows!
            String location = "Data/classification.csv";            
            boolean status = classi.readFromCsvFile( location );
            if (! status) {
                General.showError("in Classification.main found:");
                General.showError("reading classification file.");
                System.exit(1);
            }
            General.showOutput("Class looks like:\n"+classi.toString());
        }
        General.showOutput("Finished all selected check routines." );
    }
}
