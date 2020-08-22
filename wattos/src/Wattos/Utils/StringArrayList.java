/*
 * StringArrayList.java
 *
 * Created on October 31, 2002, 3:59 PM
 */

package Wattos.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import Wattos.Utils.Comparators.ComparatorStringArray;

/**
 * Methods for dealing with lists of PDB entry ids or a list of atom or residues names. Reading/writing a simple file
 * with them. Doing logical operations using multi-set semantics (e.g. allowing for multiple elements with the same
 * value) such as difference, union, and intersection.
 *
 * @author Jurgen F. Doreleijers
 * @version 1
 *@see Wattos.Utils.Programs.Relate
 */
public class StringArrayList extends ArrayList {

    private static final long serialVersionUID = -4443152614175859065L;
    public static final ArrayList ALLOWED_SET_OPERATIONS = new ArrayList();

    static {
        ALLOWED_SET_OPERATIONS.add("union");
        ALLOWED_SET_OPERATIONS.add("intersection");
        ALLOWED_SET_OPERATIONS.add("difference");
    }

    public StringArrayList() {
        super();
    }

    public StringArrayList(Collection in) {
        super(in);
    }

    public StringArrayList(String[] in) {
        super();
        addAll(in);
    }


    public boolean read(String filename) {

        // Zip previous content.
        clear();

        // Read the sucker.
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = in.readLine()) != null) {
                this.add(line.trim());
            }
            in.close();
        } catch (IOException e) {
            General.showError("in StringArrayList.read found:");
            General.showError("error reading file: " + filename);
            General.showThrowable(e);
            return false;
        }
        return true;
    }

    public boolean write(String filename) {
        // write the sucker.
        try {
            PrintWriter out = new PrintWriter(new FileWriter(filename));
            for (Iterator i = this.iterator(); i.hasNext();) {
                String element = (String) i.next();
                out.println(element);
            }
            out.close();
        } catch (IOException e) {
            General.showError("in StringArrayList.write found:");
            General.showError("error writing file: " + filename);
            return false;
        }
        return true;
    }

    public String getString(int i) {
        return (String) super.get(i);
    }

    public StringArrayList union(StringArrayList list_b) {
        StringArrayList result = new StringArrayList();
        result.addAll(this);
        result.addAll(list_b);
        return result;
    }

    /**
     * Multiset semantics. Now stating that the order of elements will not be altered by this algorithm.
     */
    public StringArrayList intersection(StringArrayList list_b) {
        StringArrayList result = new StringArrayList();
        for (Iterator i = this.iterator(); i.hasNext();) {
            String element = (String) i.next();
            if (list_b.contains(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Multiset semantics. Now stating that the order of elements will not be altered by this algorithm.
     */
    public StringArrayList difference(StringArrayList list_b) {

        StringArrayList result = new StringArrayList();
        result.addAll(this);

        for (Iterator i = list_b.iterator(); i.hasNext();) {
            String element = (String) i.next();
            int index = result.indexOf(element);
            if (index > -1) {
                result.remove(index);
            }
        }

        return result;
    }

    public void make_unique() {
        /**
         * TreeSet is a standard implementation of a Set which means that only unique elements may occur.
         */
        TreeSet s = new TreeSet(this);
        /**
         * Put them back into native data structure
         */
        this.clear();
        this.addAll(s);
    }

    /**
     * Returns duplicates (multiple ones possibly). Rather inefficient algorithm.
     */
    public StringArrayList duplicates() {
        StringArrayList result = new StringArrayList();
        // TreeSet s = new TreeSet( this );
        for (int i = this.size() - 1; i >= 0; i--) {
            String e = this.getString(i);
            int idx = this.indexOf(e);

            if (idx >= 0 && idx != i) { // matches earlier element
                result.add(e);
//				General.showDebug("i=" + i + " idx=" + idx + " element is duplicate.");
            } else {
//				General.showDebug("i=" + i + " idx=" + idx);
                // s.add(e);
            }
        }
        result.sort();
        return result;

    }

    /**
     * Checks the pdb codes. Empty list is ok.
     */
    public boolean isPdbEntryList() {
        final int MAX_TO_REPORT = 10;
        boolean status = true;
        int j = 0;
        int reported = 0;
        for (Iterator i = this.iterator(); i.hasNext();) {
            String pdb_id = (String) i.next();
            if (!Strings.is_pdb_code(pdb_id) && (reported < MAX_TO_REPORT)) {
                General.showError("string number: " + j + " is not a valid pdb code: [" + pdb_id + "]");
                status = false;
                reported++;
            }
            j++;
        }
        return status;
    }

    public String[] toStringArray() {
        int s = size();
        String[] result = new String[s];
        for (int i = 0; i < s; i++) {
            result[i] = (String) get(i);
        }
        return result;
    }

    public String toString() {
        return Strings.toString(this);
    }

    public void toLower() {
        for (int i = 0; i < this.size(); i++) {
            String element = (String) this.get(i);
            this.set(i, element.toLowerCase());
        }
    }

    public void sort() {
        ComparatorStringArray csa = new ComparatorStringArray();
        Collections.sort(this, csa);
    }

    public static void main(String[] args) {
        General.verbosity = General.verbosityDebug;
        General.showDebug("Now in test_code");
        StringArrayList list_1 = new StringArrayList();
        StringArrayList list_2 = new StringArrayList();
        StringArrayList list_3 = new StringArrayList();
        list_1.add("1brv");
        list_1.add("1aps");
        list_1.add("1Brv");
        list_2.add("1brv");
        /* list_2.add( "1brv" ); */
        General.showOutput("list 1 is: " + list_1.toString());
        General.showOutput("list 2 is: " + list_2);

        if (!list_1.isPdbEntryList()) {
            General.showError("list 1 is not a list of pdb entries");
            System.exit(1); // testing isn't a normal exit.
        }
        list_1.sort();
        General.showOutput("list 1 is: " + list_1);
        list_1.make_unique();
        General.showOutput("list 1 is: " + list_1);

        list_3 = list_1.difference(list_2);
        General.showOutput("list 3 is: " + list_3);
        System.exit(1); // testing isn't a normal exit.
    }

    public void addAll(String[] list) {
        for (int i = 0; i < list.length; i++) {
            add(list[i]);
        }
    }
}
