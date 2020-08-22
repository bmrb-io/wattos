package Wattos.Utils;

import java.io.*;

import Wattos.Utils.InOut.RegExpFilenameFilter;

import junit.framework.TestCase;

public class InOutTest extends TestCase {

    String wattosRoot = InOut.getEnvVar("WATTOSROOT");
    File inputDir = new File(wattosRoot, "data" + File.separator + "test_data");

    public void testAddFileNumberBeforeExtension() {
        General.setVerbosityToDebug();
        File f = new File("tmp", "test.str");
        String fn = f.toString();
        String actual = InOut.addFileNumberBeforeExtension(fn, 1, true, 3);
        File fExpected = new File("tmp", "test_001.str");
        String expected = fExpected.toString();
        assertEquals(expected, actual);
    }

    public void testReadTextFromUrl() {
        String baseInputName = "1brv"; // use only 1buf as example and remove other .gz s as they're big
        File input = new File(inputDir, baseInputName + ".pdb.gz");
        String text = InOut.readTextFromUrl(InOut.getUrlFileFromName(input.toString()));
        assertEquals(true, text != null);
        // General.showOutput(text);
    }

    public void testGunzipFile() {
        String baseInputName = "1brv";
        File input = new File(inputDir, baseInputName + ".pdb.gz");
        File tmpOutputFile = null;
        try {
            tmpOutputFile = File.createTempFile(baseInputName, ".mr");
            // General.showOutput("Writing to java generated temp file name: "+tmpOutputFile.toString());
            tmpOutputFile.deleteOnExit();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assertEquals(true, InOut.gunzipFile(input, tmpOutputFile));
        tmpOutputFile.delete();
    }

    public void testRegExpFileNameFilter() {
        String regexp = ".+.pdb.gz|.+.cif.gz";
        General.showOutput("Reg exp files: " + regexp);
        General.showOutput("Dir          : " + inputDir);
        assertTrue(inputDir.isDirectory());
        RegExpFilenameFilter ff = new RegExpFilenameFilter(regexp);
        String[] list = inputDir.list(ff);

        General.showOutput("Found files: " + Strings.toString(list));
        General.showOutput("Found number of files: " + list.length);
        assertTrue(list.length>0);
    }

    public void testGetstatusoutput() {
        String cmd = "ls -al *.txt"; // there is a .txt file in $WATTOSROOT
        String[] result = OSExec.getstatusoutput(cmd);
        General.showOutput("result: " + Strings.toString(result));
        assertEquals( result[0], "0"); // status ok
        assertEquals( result[2], ""); // no error message
    }


}
