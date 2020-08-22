package Wattos.Utils;

import java.io.*;
import java.net.*;

/**Optimized reader used for e.g. reading PDB files.
 * @author unknown
 */
public class FastFileReader
{
    public int BUFFER_SIZE = 32 * 1024;
    public int nlines;
    public int tchars;
    public static final char MAP3[] = {
        '\200', '\201', '\202', '\203', '\204', '\205', '\206', '\207', '\210', '\211', 
        '\212', '\213', '\214', '\215', '\216', '\217', '\220', '\221', '\222', '\223', 
        '\224', '\225', '\226', '\227', '\230', '\231', '\232', '\233', '\234', '\235', 
        '\236', '\237', '\240', '\241', '\242', '\243', '\244', '\245', '\246', '\247', 
        '\250', '\251', '\252', '\253', '\254', '\255', '\256', '\257', '\260', '\261', 
        '\262', '\263', '\264', '\265', '\266', '\267', '\270', '\271', '\272', '\273', 
        '\274', '\275', '\276', '\277', '\300', '\301', '\302', '\303', '\304', '\305', 
        '\306', '\307', '\310', '\311', '\312', '\313', '\314', '\315', '\316', '\317', 
        '\320', '\321', '\322', '\323', '\324', '\325', '\326', '\327', '\330', '\331', 
        '\332', '\333', '\334', '\335', '\336', '\337', '\340', '\341', '\342', '\343', 
        '\344', '\345', '\346', '\347', '\350', '\351', '\352', '\353', '\354', '\355', 
        '\356', '\357', '\360', '\361', '\362', '\363', '\364', '\365', '\366', '\367', 
        '\370', '\371', '\372', '\373', '\374', '\375', '\376', '\377', 0, '\001', 
        '\002', '\003', '\004', '\005', '\006', '\007', '\b', '\t', '\n', '\013', 
        '\f', '\r', '\016', '\017', '\020', '\021', '\022', '\023', '\024', '\025', 
        '\026', '\027', '\030', '\031', '\032', '\033', '\034', '\035', '\036', '\037', 
        ' ', '!', '"', '#', '$', '%', '&', '\'', '(', ')', 
        '*', '+', ',', '-', '.', '/', '0', '1', '2', '3', 
        '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', 
        '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 
        'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 
        'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[', 
        '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 
        'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 
        'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 
        'z', '{', '|', '}', '~', '\177'
    };

    /** Creates new PdbFile */
    public FastFileReader() {
        init();
    }
    
    public boolean preprocess( URL url ) {
        General.showError("preprocessForFileCharacteristics in FastFileReader should have been overriden.");
        return false;
    }
    
    public boolean postProcess( boolean status ) {
        General.showError("postProcess in FastFileReader should have been overriden.");
        return false;
    }
    
    
    public void init()
    {
        nlines = 0;
        tchars = 0;
    }

    /** Method to process a single line and return true in case
     *processing should continue.
     */
    public boolean doSomethingWith(char ac[], int i, int j)
    {
        General.showWarning("Should be overrriden method doSomethingWith in FastFileReader." );
        nlines++;
        tchars += j;
        String line = new String( ac, i, j);
        General.showDebug(" Line: [" + line + "]" );
        return true;
    }


    public boolean myReader2(URL url) {
        boolean status = preprocess(url); // Some things are better done in specialized class.
        if ( ! status ) {
            General.showError("Failed to do preprocessing.");
            return false;            
        }
        
        try {
            //Do the processing myself, directly from a FileReader
            //But don't create strings for each line, just leave it
            //as a char array
            //    FileReader in = new FileReader(string);
            //this last line becomes
            //FileInputStream in = new FileInputStream(string);
            BufferedInputStream in = InOut.getBufferedInputStream(url);
            int defaultBufferSize = BUFFER_SIZE;
            //and add the byte array buffer
            byte[] byte_buffer = new byte[defaultBufferSize];
            int nextChar = 0;
            char[] buffer = new char[defaultBufferSize];
            
            char c;
            int leftover;
            int length_read;
            int length_line;
            int startLineIdx = 0;
            
            //First fill the buffer once before we start
            //  this next line becomes a byte read followed by convert() call
            //  int nChars = in.read(buffer, 0, defaultBufferSize);
            int nChars = in.read(byte_buffer, 0, defaultBufferSize);
            convert(byte_buffer, 0, nChars, buffer, 0, nChars, MAP3);
            boolean checkFirstOfChunk = false;
            
            for(;;) {
                //Work through the buffer looking for end of line (eol) characters.
                //Note that the JDK does the eol search as follows:
                //It hard codes both of the characters \r and \n as end
                //of line characters, and considers either to signify the
                //end of the line. In addition, if the end of line character
                //is determined to be \r, and the next character is \n,
                //it winds past the \n. This way it allows the reading of
                //lines from files written on any of the three systems
                //currently supported (Unix with \n, Windows with \r\n,
                //and Mac with \r), even if you are not running on any of these.
                for (; nextChar < nChars; nextChar++) {
                    if (((c = buffer[nextChar]) == '\n') || (c == '\r')) {
                        //We found a line, so pass it for processing
                        length_line = nextChar - startLineIdx;
                        boolean status_temp = doSomethingWith(buffer, startLineIdx, length_line);
                        if ( ! status_temp ) {
                            status = false;
                            String line = new String(buffer,startLineIdx,length_line);
                            General.showError( "In FastFileReader.myReader2: reading line: [" + line + "]");
                            return false;
                        }
                        
                        //And then increment the cursors. nextChar is
                        //automatically incremented by the loop,
                        //so only need to worry if c is \r
                        if (c == '\r') {
                            //need to consider if we are at end of buffer
                            if (nextChar == (nChars - 1) )
                                checkFirstOfChunk = true;
                            else if (buffer[nextChar+1] == '\n')
                                nextChar++;
                        }
                        startLineIdx = nextChar + 1;
                    }
                }
                
                leftover = 0;
                if (startLineIdx < nChars) {
                    //We have some characters left over at the end of the chunk.
                    //So carry them over to the beginning of the next chunk.
                    leftover = nChars - startLineIdx;
                    System.arraycopy(buffer, startLineIdx, buffer, 0, leftover);
                }
                do {
                    //      length_read = in.read(buffer, leftover,
                    //              buffer.length-leftover );
                    //becomes
                    length_read = in.read(byte_buffer, leftover,
                    buffer.length-leftover);
                } while (length_read == 0);
                if (length_read > 0) {
                    nextChar -= nChars;
                    nChars = leftover + length_read;
                    startLineIdx = nextChar;
                    //And add the conversion here
                    convert(byte_buffer, leftover, nChars, buffer,
                    leftover, nChars, MAP3);
                    if (checkFirstOfChunk) {
                        checkFirstOfChunk = false;
                        if (buffer[0] == '\n') {
                            nextChar++;
                            startLineIdx = nextChar;
                        }
                    }
                }
                else { /* EOF */
                    in.close();
                    // Why was this set to return FALSE?
                    break;
                    //return false;
                }
            }
        } catch(Exception exception) {
            exception.printStackTrace();
            status = false;
        }            
        status = postProcess(status);
        if ( ! status ) {
            General.showError("Failed to do postprocessing");
            return false;            
        }
        return status;
    }


    public static int convert(byte abyte0[], int i, int j, char ac[], int k, int l, char ac1[])
        throws Exception
    {
        int i1 = j;
        boolean flag = false;
        if(j - i > l - k)
        {
            i1 = i + (l - k);
            flag = true;
        }
        int j1 = k;
        if(i1 - i > 10)
        {
            i1 -= 10;
            int k1;
            for(k1 = i; k1 < i1;)
            {
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
                ac[j1++] = ac1[abyte0[k1++] + 128];
            }

            for(i1 += 10; k1 < i1;)
                ac[j1++] = ac1[abyte0[k1++] + 128];

        }
        else
        {
            for(int l1 = i; l1 < i1;)
                ac[j1++] = ac1[abyte0[l1++] + 128];
 
        }
        if(flag)
            throw new Exception();
        else
            return j1 - k;
    }
}
