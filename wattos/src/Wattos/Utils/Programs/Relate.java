/*
 * Relate.java
 *
 * Created on November 12, 2004, 1:49 PM
 */

package Wattos.Utils.Programs;

import Wattos.Utils.*;

/**
 *Main program allows set operations on Strings as union, intersection, or difference.
 *
 * @see Wattos.Utils.StringArrayList
 * @author Jurgen F. Doreleijers
 */
public class Relate {

    /**
     * Number of arguments needed when using this class as a main for doing operations
     */
    public static final int NUMBER_ARGS_NEEDED = 4;

    /** Creates a new instance of Relate */
    public Relate() {
    }

    public static void show_usage() {
        General
                .showOutput("USAGE: java Wattos.Utils.Programs.Relate file_1_name set_operation file_2_name file_out_name [-maintainCase] ");
        General.showOutput("       file_1_name  :   name of the first file to read.");
        General.showOutput("       set_operation:   union, intersection, or difference");
        General.showOutput("       file_2_name  :   name of the second file to read.");
        General.showOutput("       file_out_name:   name of the output file.");

        System.exit(1);
    }

    public static void main(String[] args) {

        // test_code();

        /** Checks of input */
        if (args.length < NUMBER_ARGS_NEEDED) {
            General.showError("need " + NUMBER_ARGS_NEEDED + " arguments.");
            show_usage();
        }

        boolean useCase = false;
        String list_1_name = args[0];
        String set_operation = args[1];
        String list_2_name = args[2];
        String list_out_name = args[3];
        if (args.length == (NUMBER_ARGS_NEEDED + 1)) {
            String caseOption = args[4];
            if (!caseOption.equals("-maintainCase")) {
                General.showError("last argument should read '-maintainCase' (without quotes).");
                show_usage();
            }
            useCase = true;
            General.showOutput("Using case sensitivity");
        }

        if (!StringArrayList.ALLOWED_SET_OPERATIONS.contains(set_operation)) {
            General.showError("set operation " + set_operation + " is not implemented.");
            show_usage();
        }

        /**
         * Read in and check sizes
         */
        StringArrayList list_1 = new StringArrayList();
        StringArrayList list_2 = new StringArrayList();
        boolean status = list_1.read(list_1_name);
        if (!status) {
            General.showError("Error reading file 1");
            System.exit(1);
        }
        status = list_2.read(list_2_name);
        if (!status) {
            General.showError("Error reading file 2");
            System.exit(1);
        }

        General.showOutput("Read elements in file 1: " + list_1.size());
        General.showOutput("Read elements in file 2: " + list_2.size());

        int list_1_size = list_1.size();
        int list_2_size = list_2.size();
        list_1.make_unique();
        list_2.make_unique();
        if (list_1_size != list_1.size()) {
            General.showWarning("changed number of elements in list 1 by: " + (list_1_size - list_1.size()));
        }
        if (list_2_size != list_2.size()) {
            General.showWarning("changed number of elements in list 2 by: " + (list_2_size - list_2.size()));
        }

        if (! useCase) {
            list_1.toLower();
            list_2.toLower();
        }
        /**
         * Check the type of items. Comment this out if not PDB entries. if ( ! list_1.isPdbEntryList() ) {
         * General.showError("list 1 is not a list of pdb entries"); show_usage(); } if ( ! list_2.isPdbEntryList() ) {
         * General.showError("list 2 is not a list of pdb entries"); show_usage(); }
         */

        StringArrayList list_out = new StringArrayList();
        ;
        if (set_operation.equals("union")) {
            list_out = list_1.union(list_2);
        } else if (set_operation.equals("intersection")) {
            list_out = list_1.intersection(list_2);
        } else if (set_operation.equals("difference")) {
            list_out = list_1.difference(list_2);
        } else {
            General.showError("code bug.");
            show_usage();
        }

        list_out.make_unique();
        General.showOutput("Output number of elements   : " + list_out.size());

        list_out.write(list_out_name);
        General.showOutput("Written output to file      : " + list_out_name);
    }

}
