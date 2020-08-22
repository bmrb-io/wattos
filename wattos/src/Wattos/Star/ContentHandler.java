/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * $Log$
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ContentHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Revision 1.1  2007/06/19 18:07:54  jurgenfd
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ContentHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Initial revision
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ContentHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 *
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Revision 1.3  2006/10/12 20:43:18  jurgen
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * All setup for work in Eclipse with new build file.
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 *
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Revision 1.2  2006/07/07 16:10:14  jurgen
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * no message
/*
 * ContentHandler.java
 *
 * Created on November 22, 2002, 3:51 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source$
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 *
 * Revision 1.1.1.1  2006/06/13 21:34:38  jurgen
 * initial
 *
 * Revision 1.1.1.1  2003/01/10 22:01:19  dmaziuk
 * initial import
 *
 */

package Wattos.Star;

/**
 * Interface similar to org.xml.sax.ContentHandler.
 * Declares callback methods that class must implement to use SANS parser.
 * @author  dmaziuk
 * @version 1
 */
public interface ContentHandler {
    /** Called when parser encounters data_ block.
     * @param line line number
     * @param id block id
     * @return true to stop parsing
     */
    boolean startData( int line, String id );
    /** Called on EOF.
     * @param line line number
     * @param id block id
     */
    boolean endData( int line, String id );
    /** Called on start of saveframe (parser encounters save_<name>).
     * @param line line number
     * @param name saveframe name
     * @return true to stop parsing
     */
    boolean startSaveFrame( int line, String name );
    /** Called on end of saveframe (parser encounters save_).
     * @param line line number
     * @param name saveframe name
     * @return true to stop parsing
     */
    boolean endSaveFrame( int line, String name );
    /** Called on start of loop (parser encounters loop_).
     * @param line line number
     * @return true to stop parsing
     */
    boolean startLoop( int line );
    /** Called on end of loop (parser encounters stop_).
     * @param line line number
     * @return true to stop parsing
     */
    boolean endLoop( int line );
    /** Called when parser encounters a comment.
     * @param line line number
     * @param text comment text
     * @return true to stop parsing
     */
    boolean comment( int line, String text );
    /** Called when parser encounters a tag-value pair.
     * Note that parser returns a "fake" DataItemNode for values in a loop.
     * @param text data
     * @return true to stop parsing
     */
    boolean data( String text);
    /** Called when parser encounters a tag-value pair.
     * Note that parser returns a "fake" DataItemNode for values in a loop.
     * @param text data
     * @return true to stop parsing
     */
    boolean tagName( int line, String text );
}

