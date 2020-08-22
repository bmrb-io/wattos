/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */
package Wattos.Converters.Common;

import java.util.jar.Attributes;

/**Error class is used for storing errors after second pass
 * Attributes entry stores mapping for errors
 * such as errorId, error, beginLine, beginColumn
 * endLine, endColumn
 */

public class ParseError{

    /** datafield */
    public Attributes entry;

    /**Construct an empty Comment
     */
    public ParseError() {
	//initialize empty entry
	entry = new Attributes();
    }
}
