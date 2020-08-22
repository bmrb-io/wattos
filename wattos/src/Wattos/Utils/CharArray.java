/*
 * CharArray.java
 *
 * Created on March 6, 2003, 5:42 PM
 */

package Wattos.Utils;

import java.util.*;

/**
 *Methods for string operations such as fast number parsing.
 * @author Jurgen F. Doreleijers
 */
public class CharArray {

    /** Creates a new instance of CharArray */
    public CharArray() {
    }

    /** Returns true if the buffer matches the complete match starting at an index
     *in the buffer.
     *Orignally authored by Jack Shirazi.
     */
    public static boolean matches(char[] buf, int startIdx, char[] match) {
        for (int j = match.length-1; j >= 0 ; j--) {
            if ( buf[startIdx+j] != match[j]) {
                return false;
            }
        }
        return true;
    }

    /**
     *From java.lang.Integer implementation without many security checks but
     *with space skipping. The char[] will be used from index start up to and
     *including end index. Look at the examples below to see that sometimes
     *the results are really bogus. Use at own risk. Should probably reinsert
     *a couple of safety checks but for now let's see speed!
     * <blockquote><pre>
     * parseInt("0") returns 0
     * parseInt("473") returns 473
     * parseInt("-0") returns 0
     * parseInt("$1") returns 1 (Totally nonsense of course)
     * parseInt("-") returns 0
     * parseInt("FF") returns -255 (which is totally non-sense
     * parseInt(" ") throws an NumberFormatException
     * parseInt("") throws an Exception
     * parseInt(null) throws an Exception
     *
     * </pre></blockquote>
     *
     *
     * @return     the integer represented by the string argument
     * @exception  NumberFormatException if the <code>String</code>
     * 		   does not contain a parsable <code>int</code>.
     */
    public static int parseIntSimple(char[] buf, int startIdx, int length)
		throws NumberFormatException
    {

	int result = 0;
	boolean negative = false;
	int i = startIdx;
        int endIdx = startIdx + length - 1;
        int digit;

        /** Skip some bad characters in beginning
         */
        digit = buf[i];
        while ( digit < 48 || digit > 57 ) {
            i++;
            digit = buf[i];
            if ( endIdx < i ) {
                throw new NumberFormatException( new String( buf, startIdx, endIdx) );
            }
        }

        /** Skip some at the end
         */
        digit = buf[endIdx];
        while ( digit < 48 || digit > 57 ) {
            endIdx--;
            digit = buf[endIdx];
        }

        if ( digit == '-') {
            negative = true;
            i++;
        }

        while (i <= endIdx) {
            result *= 10;
            result += buf[i] - 48;
            i++;
        }

        if (negative) {
            return -result;
	}
        return result;
    }

    /**
     *From java.lang.FloatDecimal implementation without many security checks.
     *THIS METHOD DOES NOT PARSE EXPONENT FORMATTED NUMBERS LIKE 1.0E-01!
     *The char[] will be used from index start up to and
     *including end index. Look at the examples below to see that sometimes
     *the results are really bogus. Use at own risk. Probably reinsert
     *a couple of safety checks but for now let's see speed!
     * <blockquote><pre>
     * parseInt("0") returns 0
     * parseInt("0.1") returns 0.1
     * parseInt(".1") returns 0.1
     * parseInt("-0") returns 0
     * parseInt("+0.0") returns 0
     * parseInt("0.1f") returns 0.1
     * parseInt("-") returns 0 (NONSENSE)
     * parseInt("1e1") returns 311 (NONSENSE)
     * </pre></blockquote>
     *
     *
     * @return     the float represented by the string argument
     * @exception  NumberFormatException if the <code>String</code>
     * 		   does not contain a parsable <code>int</code>.
     */
    public static float parseFloatSimple(char[] buf, int startIdx, int length)
		throws NumberFormatException
    {

	int resultInt = 0;
	boolean negative = false;
        int digit;
        int endIdx = startIdx + length - 1;

        /** Might be faster to have this code inlined than to have it call out to
         *methods with their own variables.
         */
        /** Skip some in beginning         */

        while ( buf[startIdx] < 48 || buf[startIdx] > 57 ) {
            startIdx++;
            /** Empty string? What's printed out is the white-space corrected string */
            if ( endIdx < startIdx ) {
                throw new NumberFormatException( new String( buf, startIdx, endIdx) );
            }
        }

        /** Skip some at the end         */
        while ( buf[startIdx] < 48 || buf[startIdx] > 57 ) {
            endIdx--;
            /** Can't reach the end so no clause needed for that.
             */
        }

        /** Will be reset later if dot is found.
         */
        int dotIdx = endIdx + 1;

        // Get the array element only once to save on bound checks on element.
        digit = buf[startIdx];
        if ( digit == '-') {
            negative = true;
            startIdx++;
        } else if ( digit == '+') {
            startIdx++;
        }

        int i = startIdx;
        while (i <= endIdx) {
            digit = buf[i];
            if ( digit == 46 ) { // int value of '.'
                dotIdx = i;
            } else if ( digit < 58 && digit > 47 ) {
                    resultInt *= 10;
                    resultInt += digit;
                    resultInt -= 48; // int value of '0'
            // Allow one funny character at the end (java float type)
            } else if ( digit == 'f' ||
                        digit == 'F' ||
                        digit == 'e' ||
                        digit == 'E' ) {
                if ( i == endIdx ) {
                    endIdx--;
                    // so the decimal shift gets done correctly.This screws up
                    // the string construction below so a big try around all of
                    // this is always good from outside.
                } else {
                    throw new NumberFormatException( new String( buf, startIdx, length) );
                }
            } else {
                throw new NumberFormatException( new String( buf, startIdx, length) );
            }
            i++;
        }

        /** Use floats only where needed.
         */
	float result = resultInt;
        /** Speed optimized for small floats; e.g. 0.1 or 123456789.0
         */
        int dotMultiplicationsNeeded = endIdx - dotIdx;
        /** The while is using a comparison with 0 because those are faster.
         *Hopefully offsets the use of extra temp int variable.
         */
        while ( dotMultiplicationsNeeded > 0 ) {
            result /= 10;
            dotMultiplicationsNeeded--;
        }

        if ( negative ) {
            return -result;
	}
        return result;
    }


    /** Checks to see if the character is a space.
It is '\u0009', HORIZONTAL TABULATION.
It is '\u000A', LINE FEED.
It is '\u000B', VERTICAL TABULATION.
It is '\u000C', FORM FEED.
It is '\u000D', CARRIAGE RETURN.
It is '\u001C', FILE SEPARATOR.
It is '\u001D', GROUP SEPARATOR.
It is '\u001E', RECORD SEPARATOR.
It is '\u001F', UNIT SEPARATOR.
     *in a speedy way.
     */
    public static boolean isWhitespaceCommonlyFalse( int c ) {
        // Most commonly not.
        if ( c > 32 ) {
            return false;
        }
        // Regular white space is dominate.
        if ( c == 32 ) {
            return true;
        }
        // Next common range.
        if ( c > 8 && c < 14 ) {
            return true;
        }
        // Uncommon (32 was already checked...).
        if ( c > 27 && c < 33 ) {
            return true;
        }
        return false;
    }

    /** Checks to see if the character is a space.
It is '\u0009', HORIZONTAL TABULATION.
It is '\u000A', LINE FEED.
It is '\u000B', VERTICAL TABULATION.
It is '\u000C', FORM FEED.
It is '\u000D', CARRIAGE RETURN.
It is '\u001C', FILE SEPARATOR.
It is '\u001D', GROUP SEPARATOR.
It is '\u001E', RECORD SEPARATOR.
It is '\u001F', UNIT SEPARATOR.
     *in a speedy way.
     */
    public static boolean isWhitespaceCommonlyTrue( int c ) {
        // Regular white space is dominate.
        if ( c == 32 || c == 10 || c == 13 ) {
            return true;
        }
        // Next common range.
        if ( c > 8 && c < 14 ) {
            return true;
        }
        // Uncommon (32 was already checked...).
        if ( c > 27 && c < 33 ) {
            return true;
        }
        return false;
    }

    /** Checks to see if the character is a digit [0-9].
     *in a speedy way.
     */
    public static boolean isDigitCommonlyTrue( int c ) {
        // Most commonly yes.
        return c < 58 && c > 47;
    }
    /** Checks to see if the character is a digit [0-9].
     *in a speedy way.
     */
    public static boolean isDigitCommonlyFalse( char c ) {
        // Most commonly yes.
        if ( c > 57 ) {
            return false;
        }
        if ( c < 48 ) {
            return false;
        }
        return true;
    }


    /** Insert the string representation to the stringbuffer prepending it with
     *spaces if needed. Very fast for small ints. Will overwritten preceding space
     *in buffer if int is too large to fit in. Will cause an exception to be thrown
     *if buffer is too small to the left side.
     */
    public static void insertRightAlign(int value, char[] buf, int startIdx, int stringSize )
        throws NumberFormatException {

        int temp_value;
        if ( value < 0 ) {
            temp_value = -value;
        } else {
            temp_value = value;
        }

        int endIdx = startIdx + stringSize - 1;
        do {
            // get remainder and translate into char range (48 = '0').
            buf[ endIdx ] = (char) ((temp_value % 10) + 48);
            endIdx--;
            // throws away remainder because it's integer division.
            temp_value /= 10;
            //General.showDebug(" temp_value is: " + temp_value);
        } while ( temp_value > 0 );

        if ( value < 0 ) {
            buf[ endIdx ] = '-';
            endIdx--;
        }

        /** Faster than:      Arrays.fill(buf,startIdx,endIdx+1,' '); */
        while ( endIdx >= startIdx ) {
            buf[ endIdx ] = ' ';
            endIdx--;
            //General.showDebug(" endIdx is: " + endIdx);
        }

        // Do at least one inexpensive check
        if ( endIdx != startIdx - 1) {
            throw new NumberFormatException( "String size: " + stringSize + " too small to capture value: " + value);
        }
    }

    /** Copy a complete string to a char array starting at position defined.
     *Will throw an out of bounds exception if buf array access is out of bounds.
     *E.g. "LE" needs insertion at position 8 in string (length 16) and
     *should be padded to a total string length of 3.
     [0    5  ALA    5] -> [0    5  LE     5]
     */
    public static void insertLeftAlign(String value, char[] buf, int startIdx, int stringLength ) {
        int length = value.length();
        System.arraycopy( value.toCharArray(), 0, buf, startIdx, length);
        // Then pad the remainder of the string with spaces.
        int endIdx = startIdx + stringLength;
        startIdx += length;
        if ( endIdx > startIdx ) {
            Arrays.fill( buf, startIdx, endIdx, ' ');
        }
    }


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        General.showOutput("Doing tests in CharArray - version a");
//        if ( false ) {
//            String line         = "1239";
//            char[]  buf         = line.toCharArray();
//            General.showOutput("line      : [" + line + "]");
//            General.showOutput("line value (java api): [" + Integer.parseInt(line.trim()) + "]");
//            General.showOutput("line value (mine)    : [" + parseIntSimple(buf,0,buf.length)  + "]");
//        }
//        if ( false ) {
//            String line         = " \r\n\t123\t ";
//            char[]  buf         = line.toCharArray();
//            General.showOutput("line      : [" + line + "]");
//            General.showOutput("line value (java api): [" + Float.parseFloat(line.trim()) + "]");
//            General.showOutput("line value (mine)    : [" + parseFloatSimple(buf,0,buf.length)  + "]");
//        }
//        if ( false ) {
//            String line1         = "ATOM   abcdef";
//            char[]  buf1         = line1.toCharArray();
//            String line2         = "ATOM  ";
//            //char[]  buf2         = line2.toCharArray();
//            char[] buf2          = "ATOM  ".toCharArray();
//            General.showOutput("buf1      : [" + line1+ "]");
//            General.showOutput("buf2      : [" + line2+ "]");
//            General.showOutput("matches   : [" + matches(buf1,0,buf2)  + "]");
//        }
        if ( true ) {
            int value = -123;
            String line         = "ATOM   abcdef";
            char[] buf = line.toCharArray();
            General.showOutput("line     : [" + line+ "]");
            try {
                insertRightAlign(value,buf,0,5);
            } catch ( Throwable t ) {
                General.showThrowable(t);
                System.exit(1);
            }
            General.showOutput("line     : [" + new String( buf) + "]");
        }
        General.showOutput("Done with all tests in Strings");
    }
}
