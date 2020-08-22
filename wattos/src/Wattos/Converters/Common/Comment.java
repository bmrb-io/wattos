/*
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Converters.Common;

import java.util.jar.Attributes;

/**Comment class is used for storing comments
 * Attributes entry stores mapping for comments
 * such as commentId, comment, beginLine, beginColumn
 * endLine, endColumn
 */ 

public class Comment{

    //datafield
    /** Room for attributes.
     */    
    public Attributes entry;

    /**Construct an empty Comment
     */
    public Comment() {
	//initialize empty entry
	entry = new Attributes();
    }
}
