/*
 * DataItemNode.java
 *
 * Created on November 22, 2002, 4:41 PM
 */

package Wattos.Star;

/**
 * DataItemNode is similar to the one in bmrb.EDU.starlibj, only simpler.
 * Tag and value are stored as strings rather that Data[Name|Value]Node objects.
 * @author  dmaziuk
 * @version 1
 */
public class DataItemNode {
    /** tag */
    private String fTag = null;
    /** tag line number */
    private int fTagLine = -1;
    /** value */
    private String fVal = null;
    /** value line number */
    private int fValLine = -1;
    /** value delimiter type */
    private int fDel = -1;
    /** is tag in loop? */
    private boolean fInLoop = false;
//*******************************************************************************
    /** Creates new DataItemNode */
    public DataItemNode() {
    } //*************************************************************************
    public DataItemNode( IntStringPair tag ) {
        fTagLine = tag.getLineNumber();
        fTag = tag.getText();
    } //*************************************************************************
    public DataItemNode( int tagline, String tag ) {
        fTagLine = tagline;
        fTag = tag;
    } //*************************************************************************
    /** Sets tag.
     * @param tag tag
     */
    public void setTag( String tag ) {
        fTag = tag;
    } //*************************************************************************
    /** Sets tag line number.
     * @param line line number
     */
    public void setTagLine( int line ) {
        fTagLine = line;
    } //*************************************************************************
    /** Returns tag name.
     * @return tag
     */
    public String getName() {
        return fTag;
    } //*************************************************************************
    /** Returns tag name.
     * Synonym to getName()
     * @return tag
     */
    public String getLabel() {
        return fTag;
    } //*************************************************************************
    /** Returns tag name.
     * Synonym to getName()
     * @return tag
     */
    public String getTag() {
        return fTag;
    } //*************************************************************************
    /** Returns line number where tag was found.
     * @return line number
     */
    public int getTagLine() {
        return fTagLine;
    } //*************************************************************************
    /** Sets value.
     * @param val value
     */
    public void setValue( String val ) {
        fVal = val;
    } //*************************************************************************
    /** Sets value line number.
     * @param line line number
     */
    public void setValueLine( int line ) {
        fValLine = line;
    } //*************************************************************************
    /** Returns value.
     * @return value
     */
    public String getValue() {
        return fVal;
    } //*************************************************************************
    /** Returns line number where value was found.
     * @return line number
     */
    public int getValueLine() {
        return fValLine;
    } //*************************************************************************
    /** Sets value delimiter type
     * @param delim delimiter type constant
     */
    public void setDelimType( int delim ) {
        fDel = delim;
    } //*************************************************************************
    /** Returns value delimiter type
     * @return delimiter type
     */
    public int getDelimType() {
        return fDel;
    } //*************************************************************************
    /** Sets "in loop" flag.
     * @param flag true or false
     */
    public void setLoopFlag( boolean flag ) {
        fInLoop = flag;
    } //*************************************************************************
    /** Returns true if this node is in loop, false otherwise
     * @return true or false
     */
    public boolean isInLoop() {
        return fInLoop;
    } //*************************************************************************
    /** Returns this object as string
     * @return string
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append( fTag + " (" + fTagLine + "): " );
        buf.append( fVal + " (" + fValLine + ")" );
        return buf.toString();
    } //*************************************************************************
}
