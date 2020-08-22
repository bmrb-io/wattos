package Wattos.CloneWars.Gui;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import javax.swing.JLabel;
import javax.swing.JTextArea;

public class WattosGuiOutputStream extends OutputStream {
    private JTextArea log;
    private JLabel    status;
    private StringBuffer line = new StringBuffer();
//    private boolean error = false; 
    
    public static final Color DEBUG_COLOR  = Color.MAGENTA;
    public static final Color WARNING_COLOR  = Color.ORANGE;
    public static final Color ERROR_COLOR  = Color.RED;
    public static final Color STDOUT_COLOR = Color.BLACK;
    public WattosGuiOutputStream( JTextArea log, JLabel status ) {
        this.log    = log;
        this.status = status;
//        this.error = error;
    }
    
    public void write( int b ) throws IOException {        
        char c = (char) b;
        log.append( String.valueOf(c) );
        line.append(c);
        if ( c == '\n' ) {
            String msg = line.toString().trim();
            if ( (msg == null) || (msg.length()==0)) {
                return;
            }
            status.setText(msg);
            if        ( msg.startsWith("ERROR") ) {
                status.setForeground(ERROR_COLOR);
            } else if ( msg.startsWith("WARNING")) {
                status.setForeground(WARNING_COLOR);
            } else if ( msg.startsWith("DEBUG")) {
                status.setForeground(DEBUG_COLOR);
            } else {
                status.setForeground(STDOUT_COLOR);                
            }
            line.setLength(0);
        }
    }  
}
