package org.processmining.plugins.stochasticpetrinet.external.sensor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

public class SensorIntervalVisualization {
	@Plugin(name = "Sensor Interval Visualizer", returnLabels = { "Visualized Sensor Intervals" }, returnTypes = { JComponent.class }, parameterLabels = { SortedSensorIntervals.PARAMETER_LABEL }, userAccessible = false)
	@Visualizer
	public static JComponent visualize(PluginContext context, SortedSensorIntervals sensorIntervals) {
		// collect locations and assign lanes
		SortedSet<String> locations = new TreeSet<String>();
		
		Map<String, Color> resourceColors = new HashMap<String, Color>();
		
		for (SensorInterval interval : sensorIntervals){
			locations.add(interval.locationKey);
			Color resourceColor = new Color(interval.resourceKey.hashCode());
			resourceColors.put(interval.resourceKey, resourceColor);
		}
		
		return new IntervalVisualizer(sensorIntervals, locations, resourceColors);
	}
	
	public static class IntervalVisualizer extends JPanel {

		private SortedSensorIntervals sensorIntervals;
		private SortedSet<String> locations;
		private Map<String, Color> resourceColors;
		private Map<String, Integer> locationPositions;
		private long maxTime;
		
		public static final int LOCATION_HEIGHT = 20;
		public static final int WIDTH = 1000;

		public IntervalVisualizer(SortedSensorIntervals sensorIntervals, SortedSet<String> locations,
				Map<String, Color> resourceColors) {
			this.sensorIntervals = sensorIntervals;
			this.locations = locations;
			locationPositions = new HashMap<String, Integer>();
			int i = 0;
			for (String location : locations){
				// draw lane:
				int y = (int)((i+0.8)*LOCATION_HEIGHT);
				locationPositions.put(location, y);
			}
			this.resourceColors = resourceColors;
			
			this.maxTime = sensorIntervals.last().endTime;
			
			this.setPreferredSize(new Dimension(WIDTH,locations.size()*LOCATION_HEIGHT));
		}

		public void paint(Graphics g) {
			super.paint(g);
			
			// draw background lanes:
			for (String location : locations){
				int y = locationPositions.get(location);
				g.drawString(location, 5, y);
				g.setColor(Color.GRAY);
				g.drawLine(0, y+2, WIDTH, y+2);
			}
			
			// draw intervals:
			for (SensorInterval interval : sensorIntervals){
				int xStart = (int)(interval.startTime/this.maxTime * WIDTH);
				int xEnd = (int)(interval.endTime/this.maxTime * WIDTH);
				int locationPosition = locationPositions.get(interval.locationKey);
				g.setColor(resourceColors.get(interval.resourceKey));
				g.drawRect(xStart, locationPosition, (xEnd - xStart), 2);
			}
		}
	}
}
