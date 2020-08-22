/*
 * MRAnnoDiff.java
 *
 * Created on January 15, 2002, 2:42 PM
 *
 *This software is copyright (c) 2002 Board of Regents, University of Wisconsin.
 *All Rights Reserved. No warranty implied or expressed.
 */

package Wattos.Episode_II;

import Wattos.Utils.*;

/** Compares the contents of two files and notes the differences that are not
 * allowed as annotations for MR files.
 * @author Jurgen F. Doreleijers
 * @version 0.1
 */
public class MRAnnoDiff extends DiffPrint.Base {

    /** Compares two files (list of lines actually) to see if the differences
     * are allowed as mr file annotations.
     * @param a Original file.
     * @param b New file.
     */
    public MRAnnoDiff(Object[] a, Object[] b) {
      super(a,b);
    }

    /** Returns a boolean denoting whether or not all the observed changes
     * are conform the rules for annotation. This function is a slightly modified copy of
     * <code>Wattos.Utils.DiffPrint.NormalPrint</code>.
     * @param script Changes in a type of ed script.
     * @return <CODE>true</CODE> if all the observed changes are conform the rules for
     * annotation.
     */
    public boolean hasOnlyChangesAnnotationAllowed(Diff.change script) {

      Diff.change next = script;
      // Not guilty until proven otherwise.
      boolean status = true;
      
      while (next != null)
	{
	  Diff.change t, end;

	  /* Find a set of changes that belong together.  */
	  t = next;
	  end = hunkfun(next);

	  /* Disconnect them from the rest of the changes,
	     making them a hunk, and remember the rest for next iteration.  */
	  next = end.link;
	  end.link = null;
	  //if (DEBUG)
	  //  debug_script(t);

	  /* Check this hunk.  */
          if ( ! check_hunk(t) ) {
              status = false;
          }

	  /* Reconnect the script so it will all be freed properly.  */
	  end.link = next;
	}
	outfile.flush();
      return(status);
    }

    /** Check if a hunk of a diff conforms to annotation changes allowed*/
    protected boolean allowed_hunk(Diff.change hunk) {        
        /* Allow changes to lines in encoding by doing a diff on the lines
         *after the lines have been encoded in ASCII only
         */
        if ( (deletes > 0) && ( deletes == inserts) ){
            int j = first1;
            for (int i = first0; i <= last0; i++) {
                String line_0 = (String) file0[i];
                String line_1 = (String) file1[j];              
                j++;
                //outfile.println("WARNING: comparing lines\n" + line_0 + General.eol + line_1);
                if ( ! Strings.areASCIISame( line_0, line_1 ) ) {
                  outfile.println("WARNING: the lines differ when compared in ASCII encoding");
                  return false;
                }
            }
            return true;
        }

        /* Disallow deletions other than empty lines in the first file.*/
        if (deletes != 0) {
            for (int i = first0; i <= last0; i++) {
              String line = (String) file0[i];
              if ( ! line.trim().equals("") )
                  return false;
            }
        }
        
        /* Only allow lines in the second file that are empty or start with the 
         * known prefix */
        if (inserts != 0)
            for (int i = first1; i <= last1; i++) {
              String line = (String) file1[i];
              if ( line.trim().equals("") )
                  continue;
              if ( ! line.startsWith(DBMRFile.PREFIX))
                  return false;
            }
              
        return true;
    }
    
    /** Check a hunk of a diff */
    protected boolean check_hunk(Diff.change hunk) {

      /* Determine range of line numbers involved in each file.  */
      analyze_hunk (hunk);
      if (deletes == 0 && inserts == 0)
	return true;

      // This is the essential modification. Don't print it if the hunk
      // is an allowed change. After these 2 statements the code is unchanged.
      boolean status = allowed_hunk (hunk);
      if ( status )
          return true;      
      
      //outfile.println("WARNING: deletes and inserts: " + deletes + " " + inserts);

      outfile.println("ERROR: in MRAnnoDiff.check_hunk found:");
      outfile.println("ERROR: The following modification is not allowed as annotation");
      /* Print out the line number header for this hunk */
      print_number_range (',', first0, last0);
      outfile.print(change_letter(inserts, deletes));
      print_number_range (',', first1, last1);
      outfile.println();

      /* Print the lines that the first file has.  */
      if (deletes != 0)
	for (int i = first0; i <= last0; i++)
	  print_1_line ("< ", file0[i]);

      if (inserts != 0 && deletes != 0)
	outfile.println("---");

      /* Print the lines that the second file has.  */
      if (inserts != 0)
	for (int i = first1; i <= last1; i++)
	  print_1_line ("> ", file1[i]);

      return false;            
    }
    
    /** Print a hunk of an ed diff */
    protected void print_hunk(Diff.change hunk) {

      /* Determine range of line numbers involved in each file.  */
      analyze_hunk (hunk);
      if (deletes == 0 && inserts == 0)
	return;

      /* Print out the line number header for this hunk */
      print_number_range (',', first0, last0);
      outfile.println(change_letter(inserts, deletes));

      /* Print new/changed lines from second file, if needed */
      if (inserts != 0)
	{
	  boolean inserting = true;
	  for (int i = first1; i <= last1; i++)
	    {
	      /* Resume the insert, if we stopped.  */
	      if (! inserting)
		outfile.println(i - first1 + first0 + "a");
	      inserting = true;

	      /* If the file's line is just a dot, it would confuse `ed'.
		 So output it with a double dot, and set the flag LEADING_DOT
		 so that we will output another ed-command later
		 to change the double dot into a single dot.  */

	      if (".".equals(file1[i]))
		{
		  outfile.println("..");
		  outfile.println(".");
		  /* Now change that double dot to the desired single dot.  */
		  outfile.println(i - first1 + first0 + 1 + "s/^\\.\\././");
		  inserting = false;
		}
	      else
		/* Line is not `.', so output it unmodified.  */
		print_1_line ("", file1[i]);
	    }

	  /* End insert mode, if we are still in it.  */
	  if (inserting)
	    outfile.println(".");
	}
    }

    
    /** Self test; simple test that should show a difference between two simple
     * arrays of strings.
     * @param args Ignored.
     */
    public static void main (String args[]) {
        General.showOutput("Starting test of check routine." );
        if ( true ) {
            /*
            String[] a = { "?" };      
            String[] b = { "\u0081" };                             
            */
            String[] a = { "x", "\u0080", "z" };      
            String[] b = { "x", "\u0081", "z" };                             
            Diff d = new Diff(a,b);
            boolean edstyle = true;

            Diff.change script = d.diff_2(edstyle);

            MRAnnoDiff p = new MRAnnoDiff(a,b);
            p.hasOnlyChangesAnnotationAllowed(script);

            General.showOutput("Finished all selected check routines." );
        }
    }
}
