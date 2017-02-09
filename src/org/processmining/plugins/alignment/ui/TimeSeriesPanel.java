package org.processmining.plugins.alignment.ui;

import com.timeseries.TimeSeries;

import javax.swing.*;
import java.awt.*;

public class TimeSeriesPanel extends JPanel {
    private static final long serialVersionUID = 9154472783164778952L;
    private TimeSeries<? extends Number> timeSeries;

    private Color[] colors;

    public TimeSeriesPanel(TimeSeries<? extends Number> timeSeries) {
        this.timeSeries = timeSeries;
        this.setPreferredSize(new Dimension(timeSeries.size() * 2, 30));
        this.colors = new Color[timeSeries.numOfDimensions()];
        for (int i = 0; i < this.colors.length; i++) {
            colors[i] = Color.getHSBColor((float) (i / (double) this.colors.length), 1f, 0.8f);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        Rectangle bounds = this.getBounds();
        int width = Math.max(bounds.width, 100);
        int height = Math.max(bounds.height, 20);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, bounds.width, bounds.height);

        Number[] lastValues = timeSeries.getMeasurementVector(0);
        int lastXPos = 0;
        for (int i = 0; i < timeSeries.size(); i++) {
            Number[] measurements = timeSeries.getMeasurementVector(i);
            int xPos = (int) (Math.max(bounds.getWidth(), 100) * ((double) i / timeSeries.size()));
            for (int color = 0; color < measurements.length; color++) {
                Color c = colors[color];
                g2.setColor(c);
                int lastYPos = i > 0 ? ((timeSeries.getMeasurementVector(i - 1)[color].intValue() == 0) ? height - 2 : 2) : height - 2;
                int yPos = (measurements[color].intValue() == 0) ? height - 2 : 2;

                g2.drawLine(lastXPos, lastYPos, xPos, yPos);
            }

            lastValues = measurements;
            lastXPos = xPos;
        }

    }


}
