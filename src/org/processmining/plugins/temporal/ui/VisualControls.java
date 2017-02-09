package org.processmining.plugins.temporal.ui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VisualControls extends JPanel implements ActionListener {
    private static final long serialVersionUID = -1867934291671926306L;
    private TemporalModelPanel panel;

    private JButton resetButton;
    private JButton layoutButton;
    private JButton stepButton;
    private JButton zInButton;
    private JButton zOutButton;

    private boolean paused = false;

    public VisualControls(TemporalModelPanel panel) {
        this.panel = panel;

        zInButton = new JButton("Zoom in");
        zInButton.addActionListener(this);
        zOutButton = new JButton("Zoom out");
        zOutButton.addActionListener(this);
        resetButton = new JButton("reset");
        resetButton.addActionListener(this);
        layoutButton = new JButton("layout");
        layoutButton.addActionListener(this);
        stepButton = new JButton("step");
        stepButton.addActionListener(this);

        this.add(zInButton);
        this.add(zOutButton);
        this.add(resetButton);
        this.add(layoutButton);
        this.add(stepButton);

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(zInButton)) {
            panel.zoom(true);
        } else if (e.getSource().equals(zOutButton)) {
            panel.zoom(false);
        } else if (e.getSource().equals(resetButton)) {
            panel.initPositions();
        } else if (e.getSource().equals(layoutButton)) {
            panel.runLayout();
        } else if (e.getSource().equals(stepButton)) {
            panel.updatePositions();
        }
    }

}
