/*
 * StringsTest.java
 * JUnit based test
 *
 * Created on January 19, 2006, 1:50 PM
 */

package Wattos.Utils;

import java.util.ArrayList;
import java.util.Collection;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 *
 * @author jurgen
 */
public class StringsTest extends TestCase {

    public StringsTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(StringsTest.class);

        return suite;
    }

    public void testToStringArray() {
        Collection c = null;

        String[] expResult = null;
        String[] result = Strings.toStringArray(c);
        assertEquals(expResult, result);
    }

    public void testStringArrayListDuplicates() {
    	General.setVerbosityToDebug();
    	StringArrayList in = new StringArrayList();
    	StringArrayList expResult = new StringArrayList();
    	in.add("1brv");
    	in.add("1brv");
    	expResult.add("1brv");
    	StringArrayList result = in.duplicates();
    	General.showDebug("in:     " +           in.toString());
    	General.showDebug("result: "    +    result.toString());
    	General.showDebug("expResult: " + expResult.toString());
        assertEquals(expResult, result);
    }

    public void testGrowToSize() {
        boolean leftAlign = false;
        boolean colContainsAQuotedValue = true;
        String[] l = new String[] {
            "\"bla die bla\"",
             "real value",
            "'bla bla'",
            "\"1.0 e06\"",
            "2.0e07"
            };
        char[] charsReusable = new char[l[0].length()];
        for ( int i=0;i<l.length;i++) {
            Strings.growToSize(l[i],leftAlign,charsReusable,colContainsAQuotedValue);
            General.showDebug("Aligned value is: [" + new String(charsReusable) + "]");
        }
    }

    public void testGetEndPosition() {
        String txt = "123\n45\n";
        int[] result = Strings.getEndPosition( txt );
        General.showDebug("Result: " + PrimitiveArray.toString(result));
        if ( result[0] != 1 || result[1] != 2 ) {
            fail( "Expected: 1,2 but got: " + PrimitiveArray.toString(result));
        }
    }

    public void testGetBlock() {
        String txt = "123\n45\n6\n";
        int[] positionBegin = { 0, 1 };
        int[] positionEnd   = { 1, 1 };
        String block = Strings.getBlock(txt, positionBegin,positionEnd);
        General.showDebug("txt block is: [" + block + "]");
        assertEquals( "23" + General.eol + "4", block);
    }


    public void testDos2unix() {
        String in = "a test\r\nwhat else";
        String exp = "a test\nwhat else";
        String out = Strings.dos2unix(in);
        assertEquals(exp,out);
    }

    public void testUnix2dos() {
        String in = "a test\nwhat else";
        String exp = "a test\r\nwhat else";
        String out = Strings.unix2dos(in);
        assertEquals(exp,out);

    }

    public void testEqualsIgnoreWhiteSpace() {
        String s1 = " what a\n hoot ";
        String s2 = "whata hoot";
        //General.verbosity = General.verbosityDebug;
        boolean expResult = true;
        boolean result = Strings.equalsIgnoreWhiteSpace(s1, s2);
        assertEquals(expResult, result);
    }

    /**
    public void testGetClassName() {
        Object o = "Test";
        String expResult = "";
        String result = Strings.getClassName(o);
        assertEquals(expResult, result);
    }

    /**
    public void testJoin() {
        String[] a = null;
        String[] b = null;

        String[] expResult = null;
        String[] result = Strings.join(a, b);
        assertEquals(expResult, result);
    }
     */

    public void testMakeStringOfLength() {
        String in = "";
        int length = 0;

        String expResult = "";
        String result = Strings.makeStringOfLength(in, length);
        assertEquals(expResult, result);
    }

    public void testParseBoolean() {
        String in = "true";

        boolean expResult = true;
        boolean result = Strings.parseBoolean(in);
        assertEquals(expResult, result);
    }

    /**
    public void testChangeNullsToEmpties() {
        String[][] in = null;

        boolean expResult = true;
        boolean result = Strings.changeNullsToEmpties(in);
        assertEquals(expResult, result);
    }

    public void testStripSingleQuotes() {
        String in = "";

        String expResult = "";
        String result = Strings.stripSingleQuotes(in);
        assertEquals(expResult, result);
    }
     */

    public void testCreateStringOfXTimesTheCharacter() {
        char c = ' ';
        int count = 0;

        String expResult = "";
        String result = Strings.createStringOfXTimesTheCharacter(c, count);
        assertEquals(expResult, result);
    }

    public void testGetLines() {
        String txt = "1\n2";
        ArrayList expResult = new ArrayList();
        expResult.add("1");
        expResult.add("2");
        ArrayList result = Strings.getLines(txt);
        assertEquals(expResult, result);
    }
/**
    public void testGetFirstWord() {
        String line = "";

        String expResult = "";
        String result = Strings.getFirstWord(line);
        assertEquals(expResult, result);
    }

    public void testStripFirstWord() {
        String line = "";

        String expResult = "";
        String result = Strings.stripFirstWord(line);
        assertEquals(expResult, result);
    }

    public void testGetSecondWord() {
        String line = "";

        String expResult = "";
        String result = Strings.getSecondWord(line);
        assertEquals(expResult, result);
    }

    public void testSplitWithAllReturnedIntegers() {
        String txt = "";
        char delim = ' ';

        int[] expResult = null;
        int[] result = Strings.splitWithAllReturnedIntegers(txt, delim);
        assertEquals(expResult, result);
    }

    public void testSplitAllNoEmpties() {
        String[] txt = null;
        String regexp = "";

        String[] expResult = null;
        String[] result = Strings.splitAllNoEmpties(txt, regexp);
        assertEquals(expResult, result);
    }

    public void testSplitWithAllReturned() {
        String txt = "";
        char delim = ' ';

        String[] expResult = null;
        String[] result = Strings.splitWithAllReturned(txt, delim);
        assertEquals(expResult, result);
    }

    public void testDoSubstr() {
        String[] list = null;
        int startIdx = 0;
        int endIdx = 0;

        Strings.doSubstr(list, startIdx, endIdx);
    }

    public void testDot2Null() {
        String[] list = null;

        Strings.dot2Null(list);
    }

    public void testWrapToMarginSimple() {
        String input = "";
        int margin = 0;

        String expResult = "";
        String result = Strings.wrapToMarginSimple(input, margin);
        assertEquals(expResult, result);
    }

    public void testConcatenate() {
        Object[] str = null;
        String delim = "";

        String expResult = "";
        String result = Strings.concatenate(str, delim);
        assertEquals(expResult, result);
    }

    public void testToWord() {
        String input = "";

        String expResult = "";
        String result = Strings.toWord(input);
        assertEquals(expResult, result);
    }

    public void testDeleteAllWhiteSpace() {
        String input = "";

        String expResult = "";
        String result = Strings.deleteAllWhiteSpace(input);
        assertEquals(expResult, result);
    }

    public void testBreakWord() {
        String input = "";

        String expResult = "";
        String result = Strings.breakWord(input);
        assertEquals(expResult, result);
    }

    public void testToHtml() {
        Properties p = null;

        String expResult = "";
        String result = Strings.toHtml(p);
        assertEquals(expResult, result);
    }

    public void testToCsv() {
        String[][] in = null;

        String expResult = "";
        String result = Strings.toCsv(in);
        assertEquals(expResult, result);
    }

    public void testToASCII() {
        String input = "";

        String expResult = "";
        String result = Strings.toASCII(input);
        assertEquals(expResult, result);
    }

    public void testAreASCIISame() {
        String one = "";
        String two = "";

        boolean expResult = true;
        boolean result = Strings.areASCIISame(one, two);
        assertEquals(expResult, result);
    }

    public void testWriteToFile() {
        String text = "";
        String filename = "";

        boolean expResult = true;
        boolean result = Strings.writeToFile(text, filename);
        assertEquals(expResult, result);
    }

    public void testAreDigits() {
        String chk_string = "";

        boolean expResult = true;
        boolean result = Strings.areDigits(chk_string);
        assertEquals(expResult, result);
    }

    public void testIs_pdb_code() {
        String chk_string = "";

        boolean expResult = true;
        boolean result = Strings.is_pdb_code(chk_string);
        assertEquals(expResult, result);
    }

    public void testIs_bmrb_code() {
        String bmrb_id_str = "";

        boolean expResult = true;
        boolean result = Strings.is_bmrb_code(bmrb_id_str);
        assertEquals(expResult, result);
    }

    public void testIsPdbEntryLoL() {
        String[][] lol = null;

        boolean expResult = true;
        boolean result = Strings.isPdbEntryLoL(lol);
        assertEquals(expResult, result);
    }

    public void testIsPdbEntryList() {
        String[] list = null;

        boolean expResult = true;
        boolean result = Strings.isPdbEntryList(list);
        assertEquals(expResult, result);
    }

    public void testGetPDBEntryCodeFromFileName() {
        String fn = "";

        String expResult = "";
        String result = Strings.getPDBEntryCodeFromFileName(fn);
        assertEquals(expResult, result);
    }

    public void testGetInputString() {
        String prompt = "";

        String expResult = "";
        String result = Strings.getInputString(prompt);
        assertEquals(expResult, result);
    }

    public void testGetProperties() {
        Properties p = null;

        String expResult = "";
        String result = Strings.getProperties(p);
        assertEquals(expResult, result);
    }

    public void testGetPropertiesNoBrackets() {
        Properties p = null;

        String expResult = "";
        String result = Strings.getPropertiesNoBrackets(p);
        assertEquals(expResult, result);
    }

    public void testSetProperties() {
        String input_properties = "";

        Properties expResult = null;
        Properties result = Strings.setProperties(input_properties);
        assertEquals(expResult, result);
    }

    /**
    public void testGetInputBoolean() {
        String prompt = "";

        boolean expResult = true;
        boolean result = Strings.getInputBoolean(prompt);
        assertEquals(expResult, result);
    }

    public void testGetInputChar() {
        BufferedReader in = null;
        String prompt = "";

        char expResult = ' ';
        char result = Strings.getInputChar(in, prompt);
        assertEquals(expResult, result);
    }

    public void testGetInputInt() {
        BufferedReader in = null;
        String prompt = "";

        int expResult = 0;
        int result = Strings.getInputInt(in, prompt);
        assertEquals(expResult, result);
    }

    public void testGetInputFloat() {
        BufferedReader in = null;
        String prompt = "";

        float expResult = 0.0F;
        float result = Strings.getInputFloat(in, prompt);
        assertEquals(expResult, result);
    }

    public void testReplaceMulti() {
        String input = "";
        Properties subs = null;

        String expResult = "";
        String result = Strings.replaceMulti(input, subs);
        assertEquals(expResult, result);
    }

    public void testReplace() {
        String input = "";
        String in = "";
        String out = "";

        String expResult = "";
        String result = Strings.replace(input, in, out);
        assertEquals(expResult, result);
    }

    public void testStripHtml() {
        String input = "";

        String expResult = "";
        String result = Strings.stripHtml(input);
        assertEquals(expResult, result);
    }

    public void testFormatReal() {
        double d = 0.0;
        int precision = 0;

        String expResult = "";
        String result = Strings.formatReal(d, precision);
        assertEquals(expResult, result);
    }

    public void testParseDouble() {
        String svalue = "";

        double expResult = 0.0;
        double result = Strings.parseDouble(svalue);
        assertEquals(expResult, result, 0.00001);
    }

    public void testToHex() {
        byte[] hash = null;

        String expResult = "";
        String result = Strings.toHex(hash);
        assertEquals(expResult, result);
    }

    public void testCountChars() {
        String in = "";
        char c = ' ';

        int expResult = 0;
        int result = Strings.countChars(in, c);
        assertEquals(expResult, result);
    }


    public void testCountStringLengthWithTabs() {
        String line = "";
        int tabwidth = 0;

        int expResult = 0;
        int result = Strings.countStringLengthWithTabs(line, tabwidth);
        assertEquals(expResult, result);
    }

    public void testSubstringCertainEnd() {
        String line = "";
        int start = 0;
        int end = 0;

        String expResult = "";
        String result = Strings.substringCertainEnd(line, start, end);
        assertEquals(expResult, result);
    }

    public void testSubstringInterpetTabs() {
        String line = "";
        int start = 0;
        int end = 0;

        String expResult = "";
        String result = Strings.substringInterpetTabs(line, start, end);
        assertEquals(expResult, result);
    }

    public void testIndexOf() {
        String[] values = null;
        String value = "";

        int expResult = 0;
        int result = Strings.indexOf(values, value);
        assertEquals(expResult, result);
    }

    public void testGetHighestPrecision() {
        String[] values = null;

        int expResult = 0;
        int result = Strings.getHighestPrecision(values);
        assertEquals(expResult, result);
    }

    public void testToUpperCase() {
        String[] strs = null;

        String[] expResult = null;
        String[] result = Strings.toUpperCase(strs);
        assertEquals(expResult, result);
    }

    public void testToLowerCase() {
        String[] strs = null;

        String[] expResult = null;
        String[] result = Strings.toLowerCase(strs);
        assertEquals(expResult, result);
    }

    public void testTrim() {
        String[] strs = null;

        String[] expResult = null;
        String[] result = Strings.trim(strs);
        assertEquals(expResult, result);
    }

    public void testToRightAlign() {
        String[] strs = null;

        String[] expResult = null;
        String[] result = Strings.toRightAlign(strs);
        assertEquals(expResult, result);
    }

    public void testTranslateValuesToLowerAndUpper() {
        String[] distances_in = null;

        String[] expResult = null;
        String[] result = Strings.translateValuesToLowerAndUpper(distances_in);
        assertEquals(expResult, result);
    }

    public void testFillStringNullReferencesWithEmptyString() {
        String[] a = null;

        Strings.fillStringNullReferencesWithEmptyString(a);
    }

    public void testAppendRightAlign() {
        long l = 0L;
        StringBuffer sb = null;
        int stringSize = 0;

        Strings.appendRightAlign(l, sb, stringSize);
    }

    public void testToString() {
        Object[] a = null;

        String expResult = "";
        String result = Strings.toString(a);
        assertEquals(expResult, result);
    }


    public void testLongestString() {
        String[] a = null;

        String expResult = "";
        String result = Strings.longestString(a);
        assertEquals(expResult, result);
    }

    public void testGetMaxSizeStrings() {
        String[] s = null;

        int expResult = 0;
        int result = Strings.getMaxSizeStrings(s);
        assertEquals(expResult, result);
    }

    public void testGetDistinctSorted() {
        String[] stringList = null;
        BitSet selected = null;

        String[] expResult = null;
        String[] result = Strings.getDistinctSorted(stringList, selected);
        assertEquals(expResult, result);
    }

     */
    /** Self test; tests the methods:
     *is_pdb_code and concatenate. The other methods are interactive and
     *are disabled as tests for automatic testing.
     * @param args Command line arguments; ignored
     */
    public static void testMain (String[] args) {
        // is_pdb_code test
//        if ( false ) {
//            String code = "1brv";
//            General.showOutput("PDB code: " + code );
//            boolean matched = Strings.is_pdb_code( code );
//            if ( matched )
//                General.showOutput("Result matched PDB pattern" );
//            else
//                General.showOutput("Result did NOT matched  PDB pattern" );
//        }
//        if ( false ) {
//            ArrayList cmd = new ArrayList();
//            cmd.add("1brv");
//            cmd.add("1hue");
//            cmd.add("1aub");
//            General.showOutput("strings: " + cmd.toString() );
//            General.showOutput("string : " + Strings.concatenate(cmd.toArray(), "/") );
//        }
//        if ( false ) {
//            boolean status = Strings.getInputBoolean("say yes");
//
//            General.showOutputNoEol("you said: " );
//            if ( status )
//                General.showOutput("yes");
//            else
//                General.showOutput("no");
//        }
//        if ( false ) {
//            Wattos.Episode_II.Globals g = new Wattos.Episode_II.Globals();
//            String html_header_text = g.getValueString("html_header_text");
//            General.showOutput("BEFORE\n"+html_header_text);
//
//            Properties subs = new Properties();
//            subs.setProperty( "<!-- INSERT A TITLE HERE -->",   "XXXXXXXXXXXXXX" );
//            subs.setProperty( "<!-- INSERT AN IMAGE HERE -->",  "YYYYYYYYYYYYYYYYY" );
//            subs.setProperty( "<!-- INSERT DATE HERE -->",      "ZZZZZZZZZZZZZZZZZZ" );
//            html_header_text = Strings.replaceMulti(html_header_text, subs);
//            General.showOutput("AFTER\n"+html_header_text);
//        }
//        if ( false ) {
//            Properties subs = new Properties();
//            subs.setProperty( "a",  "\"XXXXXXXXXXXXXX\"" );
//            subs.setProperty( "b",  "1" );
//            subs.setProperty( "c",  "-99x0" );
//            General.showOutput("HTML:\n"+Strings.toHtml(subs));
//        }
//        if ( false ) {
//            String t = "\na<B>b</b>c\td\r";
//            General.showOutput("before:["+t+"]");
//            General.showOutput("after:["+Strings.stripHtml(t)+"]");
//        }
//        if ( false ) {
//            String url = "request_type=file_set";
//            General.showOutput("before:["+url+"]");
//            String request_type_pair = "request_type=(grid|block_set|file_set|)";
//            url = Strings.replace(url, request_type_pair, "request_type=NaN");
//            General.showOutput("after :["+url+"]");
//        }
//        if ( false ) {
//            String value = "abc def/&'\";'+";
//            General.showOutput("org    :[" + value + "]");
//            //General.showOutput("after  :["+encodeUrlParameterValue(value)+"]");
//            try {
//                value = URLEncoder.encode(value, null);
//                General.showOutput("after e:[" + value + "]");
//                value = URLDecoder.decode(value, null);
//                General.showOutput("after d:[" + value + "]");
//            } catch (UnsupportedEncodingException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//        if ( false ) {
//            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
//
//            boolean stop = false;
//            while ( stop == false ) {
//                String value = Strings.getInputString( in, "prompt:");
//                General.showOutput("Read  :[" + value + "]");
//                if ( value.equals("stop") ) {
//                    stop = true;
//                }
//            }
//        }
//        if ( false ) {
//            boolean stop = false;
//            while ( stop == false ) {
//                boolean value = Strings.getInputBoolean( "prompt (enter 'n' to stop)");
//                General.showOutput("Read  :[" + value + "]");
//                if ( ! value ) {
//                    stop = true;
//                }
//            }
//        }
//
//        if ( false ) {
//            // Check which encodings are supported on the current jvm.
//            String input = "ab\tcd\u05D0ef";
//            String output = Strings.toASCII( input );
//            // The funny char is printed as a question mark.
//            General.showOutput("Input : [" + input + "]");
//            General.showOutput("Output: [" + output + "]");
//        }
//
//        if ( false ) {
//            // Check which encodings are supported on the current jvm.
//            String input1 = "ab\tcd?ef";
//            String input2 = "ab\tcd\u0081ef";
//            // The funny char is printed as a question mark.
//            General.showOutput("Input 1: [" + input1 + "]");
//            General.showOutput("Input 2: [" + input2 + "]");
//            General.showOutput("ASCII equal:: [" + Strings.areASCIISame( input1, input2) + "]");
//        }
//        if ( false ) {
//            double numb = 1.234567689;
//            int precision = 3;
//            General.showOutput("numb 1: [" + numb + "]");
//            General.showOutput("numb 2: [" + Strings.formatReal(numb,precision)+ "]" +
//                " with precision: " + precision );
//        }
//        if ( false ) {
//            Strings.writeToFile("testing\n1\n2", "test.txt");
//        }
//        if ( false ) {
//            ArrayList lines = Strings.getLines( "1\n2\n3\n\n4\n\n" );
//            General.showOutput("lines: [" + lines.size() + "]");
//            for (int i=0;i<lines.size();i++) {
//                General.showOutput("line: [" + lines.get(i) + "]");
//            }
//        }
//        if ( false ) {
//            String svalue = "-1.0d+01";
//            //String svalue = "-1e5";
//            double dvalue = Strings.parseDouble(svalue);
//            General.showOutput("dvalue is ["+dvalue+"]");
//        }
//        if ( false ) {
//            double dvalue = -1000;
//            General.showOutput("value is ["+Strings.formatReal(dvalue,2)+"]");
//        }
//        if ( false ) {
//            String[] distances_in = { "3e3", "2.1", "1.01"};
//            String[] distances_out = Strings.translateValuesToLowerAndUpper( distances_in);
//            General.showOutput("[" + distances_out[0] + "]");
//            General.showOutput("[" + distances_out[1] + "]");
//        }
//        if ( false ) {
//            String line = "1\t2\t3";
//            General.showOutput("[" + line + "]");
//            General.showOutput("Number of tabs: " + Strings.countChars( line, '\t' ));
//        }
//       if ( false ) {
//           Properties p = new Properties();
//           p.setProperty("test",    "ok");
//           p.setProperty("t2",      "better");
//           General.showOutput( Strings.getProperties(p) );
//       }
//       if ( false ) {
//            String test = "a\tbbb\tc\td";
//            int start       = 5;
//            int end         = 8;
//            int tabWidth    = 4;
//            General.showOutput("test is         : [" + test + "]");
//            General.showOutput("start is        : " + start );
//            General.showOutput("end is          : " + end );
//            General.showOutput("tabwidth is     : " + tabWidth );
//            General.showOutput("substringInterpetTabs: [" +
//                Strings.substringInterpetTabs(test, start, end, tabWidth) + "]" );
//        }
//        if ( false ) {
//            Properties p = new Properties();
//            p.setProperty("te te", "1 2 3");
//            p.setProperty("a ja", "YES");
//            General.showOutput("without brackets: [" + Strings.getPropertiesNoBrackets(p) + "]");
//        }
//        if ( false ) {
//            float f = -123.456789f;
//            StringBuffer sb = new StringBuffer(1000);
//            Strings.appendRightAlign( f, sb, 8, 3 );
//            General.showOutput("sb: [" + sb.toString() + "]");
//            General.showOutput("f:  [" + f + "]");
//        }
//        if ( false ) {
//            String line         = "this\tis\r\na test";
//            String line_2       = Strings.toWord(line);
//            General.showOutput("line: [" + line + "]");
//            General.showOutput("line: [" + line_2 + "]");
//            General.showOutput("line: [" + Strings.breakWord(line_2) + "]");
//        }
//        if ( false ) {
//            //String line         = "012345678 \n\t901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678";
//            String line         = "01234567890";
//            line = line.replaceAll("\\s","");
//            General.showOutput("line: [" + line + "]");
//            General.showOutput("line: [\n" + Strings.wrapToMarginSimple(line,10) + "]");
//        }
//        if ( false ) {
//            String line         = "";
//            //String line         = "a|b|c|d";
//            General.showOutput("line: [" + line + "]");
//            String[] word_list = Strings.splitWithAllReturned(line,'|');
//            General.showOutput(word_list.length + " words: [" + Strings.concatenate( word_list, "|") + "]");
//        }
//        if ( false ) {
//            String line         = "first word";
//            General.showOutput("Line      : [" + line + "]");
//            General.showOutput("First word: [" + Strings.substringCertainEnd(line,0,99) + "]");
//            // doesn't work:
//            //General.showOutput("First word: [" + line.substring(0,99) + "]");
//        }
//        if ( false ) {
//            General.showOutput("String      : [" + Strings.createStringOfXTimesTheCharacter ( ' ', 5 ) + "]");
//        }
//        if ( false ) {
//            String[] s = { "a", "bc", "bcd" };
//            General.showOutput("Longest string lenght is: [" + Strings.getMaxSizeStrings( s ) + "]");
//
//        }
//        if ( false ) {
//            String s = " a ";
//            General.showOutput("String is:              [" + s + "]");
//            General.showOutput("RightAlligned it is:    [" + Strings.toRightAlign(s) + "]");
//
//        }
//        if ( false ) {
//            StringSet ss = new StringSet();
//            String[] test = {"a", "b", "a", "a", "c", "d" };
//            ss.intern(test);
//            General.showOutput("String set is: " + ss.toString());
//            BitSet bs = new BitSet();
//            bs.set(0,test.length,true);
//            bs.clear(4); // clear the one with "c"
//            String[] result = Strings.getDistinctSorted( test, bs );
//            General.showOutput("Selection is: " + PrimitiveArray.toString( bs ));
//            General.showOutput("Strings selected and unique are: " + Strings.toString( result ));
//        }
//        if ( false ) {
//            String input = "test in";
//            General.showOutput("Input is :["+ input +"]");
//            General.showOutput("Output is:["+ Strings.chomp(input) +"]");
//        }
//        if ( false ) {
//            HashMap map = new HashMap();
//
//            int hash1 = 999;
//            int hash2 = 9;
//            int rid1 = 0;
//            int rid2 = 1;
//            map.put(new Integer(hash1), new Integer(rid1));
//            map.put(new Integer(hash2), new Integer(rid2));
//            General.showOutput("Map is :["+ Strings.toString( map, true ) +"]");
//        }
//       if ( false ) {
//           while (true) {
//                String prompt = "type a float value please (use ctrl-c to quit)";
//                BufferedReader in = new BufferedReader(new InputStreamReader( System.in ));
//                float answer = Strings.getInputFloat(in, prompt);
//                General.showOutput("Input read :["+ answer +"]");
//           }
//       }
//       if ( false ) {
//           String fn = "S:\\test\\1bRv.PDB";
//           General.showOutput("fn    : " + fn);
//           General.showOutput("entry : " + Strings.getPDBEntryCodeFromFileName(fn));
//       }
//       if ( false ) {
//           String line = "XYZ";
//           String regexp_sub_seq = "[A-z]";
//           General.showOutput("counted: " + Strings.countStrings( line, regexp_sub_seq ));
//       }
//       if ( false ) {
//           String[] txt = { "XYZ", "a,b,c" };
//           General.showOutput("txt: " + Strings.toString( Strings.splitAllNoEmpties(txt,",")));
//       }
       if ( true ) {
           String txt = "100";
           General.showOutput("txt: " + txt + " and is valid bmrb code: " + Strings.is_bmrb_code(txt));
       }

        General.showOutput("Done with all tests in Strings");
    }


    public void testCountStrings() {
        String line = "[ + * - ]";
        String regexp_sub_seq = "[\\*\\-\\+]";

        int expResult = 3;
        int result = Strings.countStrings(line, regexp_sub_seq);
        //General.showOutput("result: " + result);
        assertEquals(expResult, result);
    }

    public void testToByteArray() {
        String text = "abc\ndef";
        //String text = null;
        byte[] ba = Strings.toByteArray(text);
        assertNotNull(ba);
    }
    public void testReplace() {
        String[] text = { "ab'c\"d\te,f" };
        //String text = null;
        Strings.replace(text,"['\"\\s,]"," ");
        assertEquals("ab c d e f", text[0]);
        String[] splitText = Strings.splitAllNoEmpties(text, " ");
        assertEquals(5,splitText.length);
//        General.showOutput("split Text: "+PrimitiveArray.toString(splitText));
    }

    public void testToString() {
//        General.setVerbosityToDebug();
        Object o = new String[3];
        General.showDebug("testToString: " + Strings.toString(o));
    }
    public void testToString2() {
//        General.setVerbosityToDebug();
        int i = 123;
        General.showDebug("sprintf: [" + Strings.sprintf(i," %-8s")+"]");
        General.showDebug("sprintf: [" + Strings.sprintf(i,"%9.3f")+"]");
        General.showDebug("sprintf: [" + Strings.sprintf(i,"%9.3e")+"]");
    }
    public void testAreASCIISame(  ) {
        String one = "ABC\uFFFF";
        String two = "ABC?";
        assertEquals(true, Strings.areASCIISame(one, two));
    }
}
