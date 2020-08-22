/*
 * PdfGeneration.java
 *
 * Created on March 21, 2006, 2:12 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package Wattos.Utils;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.jfree.chart.*;
import com.lowagie.text.*; 
import com.lowagie.text.pdf.*;
/**
 *
 * @author jurgen
 */
public class PdfGeneration {
    
    /** Creates a new instance of PdfGeneration */
    public PdfGeneration() {
    }
    
    
    /**
     * Converts a JFreeChart to PDF syntax.
     * @param filename	the name of the PDF file
     * @param chart		the JFreeChart
     * @param width		the width of the resulting PDF
     * @param height	the height of the resulting PDF
     */
    public static void convertToPdf(JFreeChart chart, int width, int height, String filename) {
        // step 1
        Document document = new Document(new Rectangle(width, height));
        try {
            // step 2
            PdfWriter writer;
            writer = PdfWriter.getInstance(document, new FileOutputStream(filename));
            // step 3
            document.open();
            // step 4
            PdfContentByte cb = writer.getDirectContent();
            PdfTemplate tp = cb.createTemplate(width, height);
            Graphics2D g2d = tp.createGraphics(width, height, new DefaultFontMapper());
            Rectangle2D r2d = new Rectangle2D.Double(0, 0, width, height);
            chart.draw(g2d, r2d);
            g2d.dispose();
            cb.addTemplate(tp, 0, 0);
        } catch(DocumentException de) {
            de.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // step 5
        document.close();
    }
}
