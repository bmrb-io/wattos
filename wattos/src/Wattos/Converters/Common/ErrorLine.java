/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Converters.Common;

/**ErrorLine class used to store errors' 
 * positions according to XplorParser
 * used later for second pass of storing error lines
 */

public class ErrorLine{
    /**line[0] as start line number
    line[1] as end line number
     */
    public int[] line = new int[2];
    /**col[0] as start column number
    col[1] as end column number
     */
    public int[] col = new int[2];

    /** Fills this small class with the correct line and column numbers of the found parse error.
     * @param startLine .
     * @param endLine .
     * @param startCol .
     * @param endCol .
     */    
    public ErrorLine(int startLine, int endLine, int startCol, int endCol) {
	line[0] = startLine;
	line[1] = endLine;
	col[0] = startCol;
	col[1] = endCol;
    }
}
