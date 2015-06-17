package org.processmining.plugins.temporal.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

public class VisualControls extends JPanel implements ActionListener{
	private static final long serialVersionUID = -1867934291671926306L;
	private TemporalModelPanel panel;
	
	private JButton updateButton;
	private JButton pauseButton;
	private JButton zInButton;
	private JButton zOutButton;
	
	private boolean paused = false;
	
	public VisualControls(TemporalModelPanel panel){
		this.panel = panel;
		
		zInButton = new JButton("Zoom in");
		zInButton.addActionListener(this);
		zOutButton = new JButton("Zoom out");
		zOutButton.addActionListener(this);
		updateButton = new JButton("update");
		updateButton.addActionListener(this);
		pauseButton = new JButton("pause");
		pauseButton.addActionListener(this);
		
		this.add(zInButton);
		this.add(zOutButton);
		this.add(updateButton);
		this.add(pauseButton);
		
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(zInButton)){
			panel.zoom(true);
		} else if (e.getSource().equals(zOutButton)){
			panel.zoom(false);
		} else if (e.getSource().equals(updateButton)){
			panel.updatePositions();
		} else if (e.getSource().equals(pauseButton)){
			panel.setPaused(!panel.isPaused());
			if (panel.isPaused()){
				pauseButton.setText("continue");	
			} else {
				pauseButton.setText("pause");
			}	
		}
	}

}
