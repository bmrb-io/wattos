 /*
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Utils;

import java.util.*;
import java.util.Date;
import java.text.*;

/**
 * Utilities for dealing with dates.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class Dates {

    /** Returns a string in a formatting used in BMRB entries
     */
    public static String getDateBMRBStyle( )
    {
        Date d = new java.util.Date();
        return getDateBMRBStyle( d );
    }

    /** Returns a string in a formatting used in BMRB entries
     */
    public static String getEpochTime( )
    {
        Date d = new Date();
        String e = Long.toString( d.getTime() );
        return e;
    }

    /** Returns a string in a formatting used in BMRB entries
     * (ISO 8601 formatted)
     */
    public static String getDateBMRBStyle( Date d )
    {
        DateFormat bmrbformat = new SimpleDateFormat("yyyy-MM-dd");
        String date_str = bmrbformat.format(d);
        return date_str;
    }

    /** Returns a string in a formatting used in python time.asctime()
     */
    public static String getDateLikePythonAscTime( Date d )
    {
        DateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy");
        String date_str = format.format(d);
        return date_str;
    }


    /** Returns a string without html tags and trimmed for white space chars.
     */
    public static String getDateWithoutFunnyChars() {
        String date_str = getDate( new java.util.Date() );

        // Remove funny chars.
        Properties s = new Properties();
        s.put(" ", "_");
        s.put(",", "_");
        s.put(":", "_");
        date_str = Strings.replaceMulti( date_str, s );

        // Avoid repetition
        s = new Properties();
        s.put("_+", "_");
        date_str = Strings.replaceMulti( date_str, s );
        //General.showOutput("date :["+date_str+"]");

        return(date_str);
    }

    /** Normal formatted time stamp.
     */
    public static String getDate( Date date ) {
        String date_str = java.text.DateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.LONG).format(date);
        return date_str;
    }

    /** See book Java Enterprise in a nutshell from O'Reilly, page 26. These are ISO date
        escape sequences.
        Returns something like: {ts '2002-05-10 16:02:00'}
     */
    public static String getDateIsoEscaped( Date date ) {
        java.text.DateFormat format = new java.text.SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
        String result = "{ts '" + format.format(date) + "'}";
        return( result );
    }

      /** Issues an error message saying this class can not be initiated */
    public Dates() {
        General.showError("Don't try to initiate Dates class");
    }


    /** Self test; tests the methods:
     *is_pdb_code and concatenate. The other methods are interactive and
     *are disabled as tests for automatic testing.
     * @param args Command line arguments; ignored
     */
    public static void main (String[] args) {
//        if ( false ) {
//            String date_str = getDateWithoutFunnyChars();
//            General.showOutput("date :["+date_str+"]");
//            Date now = new Date();
//            General.showOutput("iso :["+getDateIsoEscaped(now)+"]");
//        }
        if ( true ) {
            General.showOutput("date in BMRB format :[" + getDateBMRBStyle() + "]");
        }
        if ( true ) {
            General.showOutput("date in regular format :[" + getDate( new Date() ) + "]");
        }
    }
}
