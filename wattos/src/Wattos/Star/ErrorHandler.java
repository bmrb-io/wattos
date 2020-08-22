/*
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ErrorHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Revision 1.1  2007/06/19 18:07:54  jurgenfd
/*
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ErrorHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 * Initial revision
/*
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
 *
 * This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 * All Rights Reserved.
 *
 * FILE:        $Source: /cvs_archive/cvs/Wattos/src/Wattos/Star/ErrorHandler.java,v $
 * 
 * AUTHOR:      $Author: jurgenfd $
 * DATE:        $Date: 2008-08-11 15:48:41 -0500 (Mon, 11 Aug 2008) $
 * 
 * UPDATE HISTORY:
 * ---------------
 *
/*
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * ErrorHandler.java
 *
 * Created on November 22, 2002, 5:05 PM
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
 * Interface somewhat similar to org.xml.sax.ErrorHandler.
 * Declares callbacks a class must implement to use SANS2 parser.
 * @author  dmaziuk
 * @version 1
 */
public interface ErrorHandler {
    /** Called when parser encounters an error.
     * @param line line number
     * @param col column number
     * @param msg error message
     */
    public void error( int line, int col, String msg );
    /** Called when parser encounters a possible error
     * @param line line number
     * @param col column number
     * @param msg error message
     * @return true signals parser to stop parsing
     */
    public boolean warning( int line, int col, String msg );
}

