package org.processmining.plugins.stochasticpetrinet.ui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.processmining.plugins.stochasticpetrinet.enricher.StochasticManifestCollector;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlotPanelFreeChart extends JPanel {
    private static final long serialVersionUID = -4121860809081058096L;

    public static final int POINTS_TO_DRAW_LINE = 500;

    private List<Plot> plots;

    /**
     * Draw a vertical
     */
    private List<PointOfInterest> pointsOfInterest;

    private JComponent currentChart = null;

    private String unit = "s";

    private String scatterData;
    private JComponent secondChart = null, thirdChart = null;

    public PlotPanelFreeChart() {
        super();
        plots = new LinkedList<Plot>();
        pointsOfInterest = new ArrayList<PointOfInterest>();
        this.setLayout(new BorderLayout());
    }

    public void setPlots(List<Plot> plots) {
        this.plots = plots;
        updateChart();
    }

    public void setTrainingData(String trainingData) {
        this.scatterData = trainingData;
        if (secondChart != null) {
            this.remove(secondChart);
        }
        this.secondChart = null;

        if (thirdChart != null) {
            this.remove(thirdChart);
        }
        this.thirdChart = null;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public void addPlot(Plot plot) {
        this.plots.add(plot);
        updateChart();
    }

    public void clearPointsOfInterest() {
        this.pointsOfInterest.clear();
        updateChart();
    }

    public void setPointsOfInterest(List<PointOfInterest> pointsOfInterest) {
        this.pointsOfInterest = pointsOfInterest;
        updateChart();
    }

    public void addPointOfInterest(PointOfInterest poi) {
        this.pointsOfInterest.add(poi);
        updateChart();
    }

    public void addPointOfInterest(String label, Double xValue, Color color, Float linewidth) {
        PointOfInterest pointOfInterest = new PointOfInterest();
        pointOfInterest.label = label;
        pointOfInterest.value = xValue;
        pointOfInterest.color = color;
        pointOfInterest.width = linewidth;
        this.addPointOfInterest(pointOfInterest);
    }

    public void displayMessage(String message) {
        if (currentChart != null) {
            this.remove(currentChart);
        }
        currentChart = new JLabel(message);
        add(currentChart, BorderLayout.CENTER);
        this.revalidate();
        this.repaint();
    }

    private void updateChart() {
        if (currentChart != null) {
            this.remove(currentChart);
        }

        double xMin = 0;
        double xMax = 1;
        XYSeries[] seriesArray = new XYSeries[plots.size()];

        for (int i = 0; i < plots.size(); i++) {
            Plot plot = plots.get(i);
            seriesArray[i] = new XYSeries(plot.getName());
            xMin = Math.min(xMin, plot.getXMin());
            xMax = Math.max(xMax, plot.getXMax());
        }

        double unit = (xMax - xMin) / POINTS_TO_DRAW_LINE;

        for (int i = 0; i < plots.size(); i++) {
            Plot plot = plots.get(i);
            // create a dataset...
            for (double x = xMin; x < xMax; x += unit) {
                seriesArray[i].add(x, plot.getVal(x));
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries series : seriesArray) {
            dataset.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(
                "",
                "transition duration",
                "probability density",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = (XYPlot) chart.getPlot();

        // draw a horizontal line across the chart at y == 0
        plot.addDomainMarker(new ValueMarker(0, Color.DARK_GRAY, new BasicStroke(1)));

        int i = 0;
        for (PointOfInterest poi : pointsOfInterest) {
            if (poi.drawLine) {
                plot.addDomainMarker(new ValueMarker(poi.value, poi.color, new BasicStroke(poi.width)));
            }
            if (poi.showLabel) {
                XYTextAnnotation textAnnotation = new XYTextAnnotation(poi.label, poi.value, plot.getRangeAxis().getUpperBound() * (0.9 - i++ * 0.2));
                textAnnotation.setPaint(poi.color);
                plot.addAnnotation(textAnnotation);
            }
        }

        currentChart = new ChartPanel(chart);


        if (secondChart == null && scatterData != null) {
            // TODO parse scatter data
            XYSeries loadScatterSeries = new XYSeries("Load");
            double[][] data = parse(scatterData, StochasticManifestCollector.SYSTEM_LOAD, StochasticManifestCollector.RELATIVE_DURATION);
            for (int j = 0; j < data.length; j++) {
                loadScatterSeries.add(data[j][0], data[j][1]);
            }
            JFreeChart loadScatterChart = ChartFactory.createScatterPlot
                    ("duration by system load", "load (no. of active instances)", "duration (in " + this.unit + ")",
                            new XYSeriesCollection(loadScatterSeries), PlotOrientation.VERTICAL, true, true, false);

            secondChart = new ChartPanel(loadScatterChart);
            XYItemRenderer renderer = ((XYPlot) loadScatterChart.getPlot()).getRenderer();
            renderer.setSeriesShape(0, new Rectangle2D.Float(-0.5f, -0.5f, 1f, 1f));


            XYSeries timeScatterSeries = new XYSeries("Time");
            double[][] timeData = parse(scatterData, StochasticManifestCollector.TIMESTAMP, StochasticManifestCollector.RELATIVE_DURATION);
            for (int j = 0; j < timeData.length; j++) {
                timeScatterSeries.add(timeData[j][0], timeData[j][1]);
            }
            JFreeChart timeScatterChart = ChartFactory.createScatterPlot
                    ("duration by time", "timestamp", "duration (in " + this.unit + ")",
                            new XYSeriesCollection(timeScatterSeries), PlotOrientation.VERTICAL, true, true, false);
            thirdChart = new ChartPanel(timeScatterChart);
            renderer = ((XYPlot) timeScatterChart.getPlot()).getRenderer();
            renderer.setSeriesShape(0, new Rectangle2D.Float(-0.5f, -0.5f, 1f, 1f));
        }


        add(currentChart, BorderLayout.CENTER);

        if (secondChart != null) {
            add(secondChart, BorderLayout.EAST);
        }
        if (thirdChart != null) {
            add(thirdChart, BorderLayout.WEST);
        }

        this.revalidate();
        this.repaint();
    }

    /**
     * @param scatterData    a table containing training data in a {@link StochasticManifestCollector#DELIMITER} separated table.
     * @param independentVar the name of the header
     * @param dependentVar
     * @return
     */
    private double[][] parse(String scatterData, String independentVar, String dependentVar) {
        // ignore first row
        String[] lines = scatterData.split("\n");
        double[][] data = new double[lines.length - 1][];

        String[] headerParts = lines[0].split(StochasticManifestCollector.DELIMITER);

        int indexOfDependentVar = getPosInArray(headerParts, dependentVar, 0); // e.g., the duration
        int indexOfIndependendVar = getPosInArray(headerParts, independentVar, 1); // e.g., the system load

        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(StochasticManifestCollector.DELIMITER);
            data[i - 1] = new double[]{Double.valueOf(parts[indexOfIndependendVar]), Double.valueOf(parts[indexOfDependentVar])};
        }
        return data;
    }

    private int getPosInArray(String[] array, Object objectToFind, int defaultValue) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(objectToFind)) {
                return i;
            }
        }
        return defaultValue;
    }

    public class PointOfInterest {
        public boolean drawLine = true;
        public boolean showLabel = true;

        public String label = "";
        /**
         * The value of the point.
         */
        public Double value = 0.0;
        /**
         * Color for the point of interes
         */
        public Color color = Color.BLACK;
        /**
         * should be around 1-4 pixels
         */
        public Float width = 1f;
    }
}
