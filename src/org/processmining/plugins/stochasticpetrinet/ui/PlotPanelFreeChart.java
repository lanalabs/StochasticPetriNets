package org.processmining.plugins.stochasticpetrinet.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class PlotPanelFreeChart extends JPanel {
	private static final long serialVersionUID = -4121860809081058096L;

	public static final int POINTS_TO_DRAW_LINE = 500;
	
	
	private List<Plot> plots;
	
	private JComponent currentChart = null;
	
	public PlotPanelFreeChart() {
		super();
		plots = new LinkedList<Plot>();
		this.setLayout(new BorderLayout());
	}
	public void setPlots(List<Plot> plots) {
		this.plots = plots;
		updateChart();
	}
	public void addPlot(Plot plot){
		this.plots.add(plot);
		updateChart();
	}
	
	public void displayMessage(String message){
		if (currentChart != null){
			this.remove(currentChart);
		}
		currentChart = new JLabel(message);
		add(currentChart, BorderLayout.CENTER);
		this.revalidate();
		this.repaint();	
	}
	
	private void updateChart() {
		if (currentChart != null){
			this.remove(currentChart);
		}
		
		double xMin = 0;
		double xMax = 1;
		XYSeries[] seriesArray = new XYSeries[plots.size()];
		
		for(int i=0; i < plots.size(); i++){
			Plot plot = plots.get(i);
			seriesArray[i] = new XYSeries(plot.getName());
			xMin = Math.min(xMin, plot.getXMin());
			xMax = Math.max(xMax, plot.getXMax());	
		}
		
		double unit = (xMax-xMin)/POINTS_TO_DRAW_LINE;

		for (int i = 0; i < plots.size(); i++){
			Plot plot = plots.get(i);
			// create a dataset...
			for (double x=xMin; x < xMax; x+=unit){
				seriesArray[i].add(x, plot.getVal(x));
			}
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		for (XYSeries series : seriesArray){
			dataset.addSeries(series);
		}
		JFreeChart chart = ChartFactory.createXYLineChart(
		"",
		"time",
		"proability density",
		dataset,
		PlotOrientation.VERTICAL,
		true, true, false);

		XYPlot plot = (XYPlot)chart.getPlot();
		
		// draw a horizontal line across the chart at y == 0
		plot.addDomainMarker(new ValueMarker(0, Color.DARK_GRAY, new BasicStroke(1)));

		currentChart = new ChartPanel(chart);
		add(currentChart, BorderLayout.CENTER);
		
		this.revalidate();
		this.repaint();	
	}
}
	