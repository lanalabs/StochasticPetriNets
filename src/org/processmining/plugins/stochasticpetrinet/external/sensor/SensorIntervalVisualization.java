package org.processmining.plugins.stochasticpetrinet.external.sensor;

import org.processmining.contexts.uitopia.annotations.Visualizer;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class SensorIntervalVisualization {
    @Plugin(name = "Sensor Interval Visualizer", returnLabels = {"Visualized Sensor Intervals"}, returnTypes = {JComponent.class}, parameterLabels = {SortedSensorIntervals.PARAMETER_LABEL}, userAccessible = false)
    @Visualizer
    public static JComponent visualize(PluginContext context, SortedSensorIntervals sensorIntervals) {
        // collect locations and assign lanes
        SortedSet<String> locations = new TreeSet<String>();

        Map<String, Color> resourceColors = new HashMap<String, Color>();

        for (SensorInterval interval : sensorIntervals) {
            locations.add(interval.getLocationKey());
            Color resourceColor = new Color(interval.getResourceKey().hashCode());
            resourceColors.put(interval.getResourceKey(), resourceColor);
        }

        return new IntervalVisualizer(sensorIntervals, locations, resourceColors);
    }

    public static class IntervalVisualizer extends JPanel {

        private SortedSensorIntervals sensorIntervals;
        private SortedSet<String> locations;
        private Map<String, Color> resourceColors;
        private Map<String, Integer> locationPositions;
        private long maxTime;

        public static final int LOCATION_HEIGHT = 25;
        public static final int WIDTH = 1000;

        public IntervalVisualizer(SortedSensorIntervals sensorIntervals, SortedSet<String> locations,
                                  Map<String, Color> resourceColors) {
            this.sensorIntervals = sensorIntervals;
            this.locations = locations;
            locationPositions = new HashMap<String, Integer>();
            int i = 1;
            for (String location : locations) {
                // draw lane:
                int y = (int) (((i++) + 0.8) * LOCATION_HEIGHT);
                locationPositions.put(location, y);
            }
            this.resourceColors = resourceColors;

            this.maxTime = sensorIntervals.last().getEndTime();

            this.setPreferredSize(new Dimension(WIDTH + 4, (locations.size() + 2) * LOCATION_HEIGHT));
        }

        public void paint(Graphics g) {
            super.paint(g);

            Map<String, Integer> locationCounter = new HashMap<String, Integer>(); // round robin position counter
            // draw background lanes:
            for (String location : locations) {
                locationCounter.put(location, 0);
                int y = locationPositions.get(location);
                g.drawString(location, 5, y);
                g.setColor(Color.GRAY);
                g.drawLine(2, y + 2, WIDTH + 2, y + 2);
            }


            // draw intervals:
            for (SensorInterval interval : sensorIntervals) {
                int locationPos = locationCounter.get(interval.getLocationKey());
                locationCounter.put(interval.getLocationKey(), locationPos + 1 % 5);

                int xStart = (int) ((double) interval.getStartTime() / this.maxTime * WIDTH) + 2;
                int xEnd = (int) ((double) interval.getEndTime() / this.maxTime * WIDTH) + 2;
                int locationPosition = locationPositions.get(interval.getLocationKey());
                g.setColor(resourceColors.get(interval.getResourceKey()));
                g.drawRect(xStart, locationPosition - (2 * locationPos), (xEnd - xStart), 2);
            }
        }
    }
}
