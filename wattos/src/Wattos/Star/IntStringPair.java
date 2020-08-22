/*
 * IntStringPair.java
 *
 * Created on November 22, 2002, 5:47 PM
 */

package Wattos.Star;

/**
 * Object to hold text string and its line number.
 * @author  dmaziuk
 * @version 1
 */
public class IntStringPair {
    /** line number */
    private int fLine = -1;
    /** text */
    private String fTxt = null;
    /** Creates new IntStringPair.
     * @param line line number
     * @param txt text
     */
    public IntStringPair( int line, String txt ) {
        fLine = line;
        fTxt = txt;
    } //*************************************************************************
    /** Returns line number.
     * @return line number
     */
    public int getLineNumber() {
        return fLine;
    } //*************************************************************************
    /** Returns text.
     * @return text
     */
    public String getText() {
        return fTxt;
    } //*************************************************************************
}
