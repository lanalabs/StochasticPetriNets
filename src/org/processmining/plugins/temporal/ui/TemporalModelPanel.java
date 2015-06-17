package org.processmining.plugins.temporal.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.swing.JPanel;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.temporal.model.TemporalConnection;
import org.processmining.plugins.temporal.model.TemporalModel;
import org.processmining.plugins.temporal.model.TemporalNode;

public class TemporalModelPanel extends JPanel{
	
	private static final int NODE_SIZE = 20;
	private Map<TemporalNode, Point2D.Double> nodePositions;
	private Map<TemporalNode, Point2D.Double> nodeInertias;
	
	private static final double NORMAL_DISTANCE = 30;
	
	private double maxDist;
	
	private TemporalModel model;
	
	private Random random;

	private static int WIDTH = 700;
	private static int HEIGHT = 500;
	
	private Map<TemporalNode, Integer> position;
	
	double[][] forceMatrix;
	double[][] weightMatrix;

	private double zoom = 1.0;
	
	private Thread updateThread;
	private boolean paused = false;
	
	public TemporalModelPanel(TemporalModel model){
		this.setLayout(new BorderLayout());
		
		this.model = model;
		this.random = new Random();
		this.nodePositions = new HashMap<TemporalNode, Point2D.Double>();
		this.nodeInertias = new HashMap<TemporalNode, Point2D.Double>();
		this.setPreferredSize(new Dimension(700, 500));
		
		position = new HashMap<>();
		
		initForceMatrix();
		
		initPositions();
	}
	
	private class UpdateThread implements Runnable{

		private TemporalModelPanel panel;
		
		private int counter = 0;

		public UpdateThread(TemporalModelPanel panel){
			this.panel = panel;
		}
			
		public void run() {
			while(panel.isVisible() && counter++ < 1000){
				try {
					if (panel.isPaused()){
						Thread.sleep(10000);
					} else {
						panel.updatePositions();
						Thread.sleep(5);
					}
				} catch (InterruptedException e) {
				}
			}
			panel.setPaused(true);
		}
	}
	
	private void initForceMatrix() {
		this.maxDist = 0;
		int size = model.getNodes().size();
		this.forceMatrix = new double[size][];
		this.weightMatrix = new double[size][];
		
		Iterator<XEventClass> iter = model.getNodes().keySet().iterator();
		for (int i = 0; iter.hasNext(); i++ ){
			XEventClass eClass = iter.next();
			TemporalNode node = model.getNodes().get(eClass);
			position.put(node, i);
		}
		
		double sumDist = 0, maxDist = 0;
		double maxWeight = 0;
		
		iter = model.getNodes().keySet().iterator();
		for (int i = 0; iter.hasNext(); i++ ){
			XEventClass eClass = iter.next();
			TemporalNode node = model.getNodes().get(eClass);
			this.forceMatrix[i] = new double[size];
			this.weightMatrix[i] = new double[size];
			Arrays.fill(this.forceMatrix[i], 0);
			Arrays.fill(this.weightMatrix[i], 0);
			if (model.getConnections().containsKey(node)){
				for (TemporalConnection conn : model.getConnections().get(node)){
					if (conn.getStats().getN()>0){
						int j = position.get(conn.getAfter());
						if (java.lang.Double.isNaN(conn.getStats().getMean())){
							System.out.println("Debug me!");
						}
						this.forceMatrix[i][j] = conn.getStats().getMean();
						sumDist += this.forceMatrix[i][j];
						maxDist = Math.max(maxDist, this.forceMatrix[i][j]);
						
						this.weightMatrix[i][j] = conn.getStats().getN();
						maxWeight = Math.max(maxWeight, this.weightMatrix[i][j]);
					}
				}
			}
		}
		normalizeForceMatrix(maxDist, sumDist, maxWeight);
	}

	private void normalizeForceMatrix(double maxDist, double sumDist, double maxWeight) {
		for (int i = 0; i < this.forceMatrix.length; i++){
			for (int j = 0; j < this.forceMatrix[i].length; j++){
				if (i == position.get(model.getStartNode())){
					this.forceMatrix[i][j] = this.forceMatrix[i][j] / maxDist;
				} else {
					this.forceMatrix[i][j] = this.forceMatrix[i][j] / sumDist;
				}
				this.maxDist = Math.max(maxDist, this.forceMatrix[i][j]);
			
				this.weightMatrix[i][j] = this.weightMatrix[i][j] / maxWeight;
			}
		}
	}

	private void initPositions(){
		// assign positions randomly to nodes:
		for (TemporalNode node : model.getNodes().values()){
			nodePositions.put(node, new Point2D.Double(random.nextDouble()*WIDTH, random.nextDouble()*HEIGHT));
			nodeInertias.put(node, new Point2D.Double(0,0));
		}
	}

	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		// draw nodes:
		for (TemporalNode node : nodePositions.keySet()){
			int i = position.get(node);
			for (TemporalNode target : nodePositions.keySet()){
				int j = position.get(target);
				if (forceMatrix[i][j] != 0){
					drawArrow(g2, node, target, weightMatrix[i][j]);
				}
			}
				
			drawNode(g2, node);
		}
	}
	
	private void drawArrow(Graphics2D g2, TemporalNode node, TemporalNode target, double d) {
		Double position = nodePositions.get(node);
		Double targetPosition = nodePositions.get(target);
		g2.setStroke(new BasicStroke((float)(d*2)));
		g2.setColor(Color.gray);
		g2.drawLine((int)position.x, (int)position.y, (int)targetPosition.x, (int)targetPosition.y);
		g2.drawOval((int)(targetPosition.x-(targetPosition.x-position.x)*0.05), (int)(targetPosition.y-(targetPosition.y-position.y)*0.05), 3, 3);
		g2.setStroke(new BasicStroke(1));
	}

	public void updatePositions(){
		double scale = (NORMAL_DISTANCE * zoom) / maxDist ;
		double dampening = 0.01;
		
		int startNodeIndex = position.get(model.getStartNode());
		
		for (TemporalNode node : model.getNodes().values()){
			int i = position.get(node);
			Point2D.Double pos = nodePositions.get(node);
			
			Point2D.Double force = new Point2D.Double(0, 0);
			
			
			// fix start node in the left 
			if (node.getEventClass().equals(TemporalModel.START_CLASS)){
				pos.setLocation(NODE_SIZE, HEIGHT/2); // the start node is fixed.
			} else {
				// add force from neighboring nodes
				for (TemporalNode previousNode : model.getNodes().values()){
					int j = position.get(previousNode);
					if (i!=j){
						Point2D.Double predecessorPos = nodePositions.get(previousNode);
						double dist = predecessorPos.distance(pos);
						
						double rawForce = (forceMatrix[j][i] * weightMatrix[j][i] - forceMatrix[i][j] * weightMatrix[i][j]) ;
						
						double idealDist = Math.max(NORMAL_DISTANCE, scale * rawForce);
						
	//					System.out.println("idealDist of node "+node.getName()+" to predecessor node "+previousNode.getName()+": "+idealDist+" (dist: "+dist+")");
						
						double newForce = (idealDist - dist)/idealDist;
						if (newForce <-0.5){
							newForce = -0.5;
						}
						if (newForce > 2){
								newForce = 2;
						}
						Point2D.Double vector = new Point2D.Double(pos.x-predecessorPos.x, pos.y-predecessorPos.y);
						
						//
						//double adjustment = (dist - idealDist) * dampening;
						
						force.setLocation(force.x+(newForce*vector.x), force.y+(newForce*vector.y));
					}
				}
				
				// pull towards point P at P(width, 1/2 height) with force (1-x) where x is forceMatrix[0][i]
				double relativePointInTime = forceMatrix[startNodeIndex][i];
				Point2D.Double targetPosition = new Point2D.Double(relativePointInTime*WIDTH, 0.5*HEIGHT);
				
				Point2D.Double timeVector = new Point2D.Double(targetPosition.x-pos.x, targetPosition.y -pos.y);
				force.setLocation(force.x+timeVector.x*0.1, force.y+timeVector.y*0.1);
				
				
				// update inertia by adding the force
				Point2D.Double inertia = nodeInertias.get(node);
				inertia.setLocation((inertia.x+force.x) * dampening, (inertia.y+force.y)*dampening);
				
				pos.setLocation(pos.x+inertia.x, pos.y+inertia.y);
			}
			
		}
		this.invalidate();
		this.repaint();
	}

	private void drawNode(Graphics2D g2, TemporalNode node) {
		Double position = nodePositions.get(node); 
		g2.setStroke(new BasicStroke(1.0f));
		g2.setColor(Color.red);
		g2.drawOval((int)position.x-NODE_SIZE/2, (int)position.y-NODE_SIZE/2, NODE_SIZE, NODE_SIZE);
		g2.setColor(Color.black);
		g2.drawString(node.getName(), (int)position.x + NODE_SIZE/2 +2, (int)position.y+NODE_SIZE/4);
	}
	
	public void zoom(boolean in){
		if (in){
			this.zoom  *= 1.2;
		} else {
			this.zoom *= 0.85;
		}
		updatePositions();
	}

	public boolean isPaused() {
		return paused;
	}
	
	public void setPaused(boolean paused) {
		if (!paused){
			UpdateThread updater = new UpdateThread(this);
			updateThread = new Thread(updater);
			updateThread.start();
		}
		this.paused = paused;
	}

}
