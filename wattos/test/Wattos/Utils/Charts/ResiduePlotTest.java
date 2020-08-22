/*
 * ResiduePlotTest.java
 * JUnit based test
 *
 * Created on March 16, 2006, 3:39 PM
 */

package Wattos.Utils.Charts;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;

import Wattos.Database.DBMS;
import Wattos.Database.Relation;
import Wattos.Utils.General;
import Wattos.Utils.InOut;
import Wattos.Utils.StringArrayList;

/**
 *
 * @author jurgen
 */
public class ResiduePlotTest extends TestCase {

    String resNumbColName       = "Residue Number";
    String complColName         = "Completeness";
    String totalViolColName     = "Total distance violation (A)";
    String restrCountColName    = "Restraint Count";

    static String columnNameMolNumb = "molNumb";
    static String columnNameResName = "resName";
    static String columnNameResNumb = "resNumb";

    public ResiduePlotTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ResiduePlotTest.class);
        return suite;
    }


    public static Relation getTestRelation() {
        String complColName         = "Completeness";
        String totalViolColName     = "Total distance violation (A)";
        String restrCountColName    = "Restraint Count";

        int numbResidues = 100;
        DBMS dbms = new DBMS();
        Relation r = null;
        try {
            r = new Relation("completeness", dbms);
        } catch (Exception ex) {
            ex.printStackTrace();
            General.doCodeBugExit("Failed to get new relation");
        }
        r.addColumnForOverallOrder();
        r.insertColumn(columnNameMolNumb,Relation.DATA_TYPE_INT,null);
        r.insertColumn(columnNameResName,Relation.DATA_TYPE_STRINGNR,null);
        r.insertColumn(columnNameResNumb,Relation.DATA_TYPE_INT,null);

        r.insertColumn(complColName,Relation.DATA_TYPE_FLOAT,null);
        r.insertColumn(restrCountColName,Relation.DATA_TYPE_INT,null);
        r.insertColumn(totalViolColName,Relation.DATA_TYPE_FLOAT,null);
        int[] rids = r.getNewRowIdList(numbResidues);
        for (int i=0;i<numbResidues;i++) {
            int rid = rids[i];
            int molNumber = 1;
            int resNumber = 1+i;
            String resName = "ALA";
            if ( i > (numbResidues/2) ) {
                molNumber = 2;
                resNumber = i-50;
            }
            if ( i %2 == 0 ) {
                resName = "TRP";
            }
            if ( i %3 == 0 ) {
                resName = "GLY";
            }
            r.setValue(rid,Relation.DEFAULT_ATTRIBUTE_ORDER_ID,i);
            r.setValue(rid,columnNameMolNumb,molNumber);
            r.setValue(rid,columnNameResName,resName);
            r.setValue(rid,columnNameResNumb,resNumber);
            r.setValue(rid,complColName,(float)i+40+3*i);
            r.setValue(rid,restrCountColName,14+i);
            r.setValue(rid,totalViolColName,(float) 20-i);
        }
        return r;
    }
    /**
     * Test of main method, of class Wattos.Utils.Charts.ResiduePlot.
     */
    public void testMain() {
        String file_name_base_dc = "test";
//        General.verbosity = General.verbosityDebug;
        General.verbosity = General.verbosityNothing;
        //General.showEnvironment();
        System.out.println("main");
        Relation r = getTestRelation();
        StringArrayList columnNameListValue = new StringArrayList();
        columnNameListValue.add(restrCountColName);
        columnNameListValue.add(complColName);
        columnNameListValue.add(totalViolColName);
        StringArrayList seriesNameList = columnNameListValue;

        DefaultTableXYDataset dataSet = ResiduePlot.createDatasetFromRelation(r,
                columnNameListValue, seriesNameList );
        //ResiduePlot.run(dataSet);

        JFreeChart chart = ResiduePlot.createChart(dataSet,r,
                columnNameMolNumb,
                columnNameResNumb,
                columnNameResName
                );
        XYPlot plot = (XYPlot) chart.getPlot();
        int seriesCount = dataSet.getSeriesCount();
        for (int i=0;i<seriesCount;i++) {
            XYSeries series = dataSet.getSeries(i);
            String key = (String) series.getKey();
            General.showDebug("Changing series: " + key);
            if ( key.equals(complColName)) {
                General.showDebug("Changing axis");
                NumberAxis axis = (NumberAxis) plot.getRangeAxisForDataset(i);
                axis.setRange(0,100);
                axis.setTickUnit(new NumberTickUnit(20));
            } else if ( key.equals(complColName)) {
                ;
            }
        }
        //chart.setBackgroundPaint(Color.BLACK);
//        if ( false ) {
//            ResiduePlot.show(chart);
//        }

        if ( true ) {
            ChartPanel chartPanel = new ChartPanel(chart);
            String fileName = file_name_base_dc+".bin";
            if ( ! InOut.writeObject(chartPanel,fileName)) {
                fail("Failed InOut.writeObject(chartPanel,fileName)");
            }
//            chartPanel = (ChartPanel) InOut.readObject(fileName);
//            if ( chartPanel == null ) {
//                fail("Failed InOut.readObject(fileName)");
//            }
            ResiduePlot.showPanel(chartPanel);
            General.sleep(1100);
        }

//        if ( false ) {
//            int width = 1024;
//            int height = 768;
//
//            String fileName = file_name_base_dc+".jpg";
//            try {
//                General.showOutput("Saving chart as JPEG");
//                ChartUtilities.saveChartAsJPEG(new File(fileName), chart, width, height);
//                //            General.showOutput("Saving chart as PDF");
//                //            fileName = file_name_base_dc+".pdf";
//                //            PdfGeneration.convertToPdf(chart, width, height, fileName);
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
//        General.sleep(111000);
    }

}
