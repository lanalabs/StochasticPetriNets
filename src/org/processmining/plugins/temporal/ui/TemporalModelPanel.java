package org.processmining.plugins.temporal.ui;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.temporal.model.TemporalConnection;
import org.processmining.plugins.temporal.model.TemporalModel;
import org.processmining.plugins.temporal.model.TemporalNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.*;
import java.util.List;

public class TemporalModelPanel extends JPanel {
    private static final long serialVersionUID = -5125356835996951952L;

    private static final int NODE_SIZE = 20;
    private Map<TemporalNode, Point2D.Double> nodePositions;
    private Map<TemporalNode, Point2D.Double> nodeInertias;

    private Map<TemporalNode, List<Point2D.Double>> nodeForces;

    private static final double NORMAL_DISTANCE = 2 * NODE_SIZE;

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

    Polygon arrowHead;

    private MouseAdapter mouseListener;

    public TemporalModelPanel(TemporalModel model) {
        this.setLayout(new BorderLayout());

        this.model = model;
        this.random = new Random();
        this.nodePositions = new HashMap<TemporalNode, Point2D.Double>();
        this.nodeInertias = new HashMap<TemporalNode, Point2D.Double>();
        this.nodeForces = new HashMap<TemporalNode, List<Double>>();
        this.setPreferredSize(new Dimension(700, 500));

        arrowHead = new Polygon();
        arrowHead.addPoint(0, 5);
        arrowHead.addPoint(-5, -5);
        arrowHead.addPoint(5, -5);

        position = new HashMap<>();

        initForceMatrix();

        initPositions();
        this.mouseListener = new MouseListener(this);

        this.addMouseListener(mouseListener);
        this.addMouseMotionListener(mouseListener);
        this.addMouseWheelListener(mouseListener);
    }

    private class MouseListener extends MouseAdapter {

        private TemporalModelPanel panel;

        private TemporalNode selectedNode;

        public MouseListener(TemporalModelPanel panel) {
            this.panel = panel;
        }

        public void mousePressed(MouseEvent e) {
            selectedNode = panel.getClickedNode(e.getX(), e.getY());
        }

        public void mouseReleased(MouseEvent e) {
            selectedNode = null;
        }

        public void mouseDragged(MouseEvent e) {
            if (selectedNode != null) {
                panel.updateNodePosition(selectedNode, e.getX(), e.getY());
            }
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() < 0) {
                panel.zoom(true);
            } else if (e.getWheelRotation() > 0) {
                panel.zoom(false);
            }
        }

    }

    private class UpdateThread implements Runnable {

        private TemporalModelPanel panel;

        private int counter = 0;

        public UpdateThread(TemporalModelPanel panel) {
            this.panel = panel;
        }

        public void run() {
            boolean changed = true;
            while (panel.isVisible() && counter++ < 1000 && changed) {
                try {
                    if (panel.isPaused()) {
                        Thread.sleep(10000);
                    } else {
                        changed = panel.updatePositions();
                        Thread.sleep(5);
                    }
                } catch (InterruptedException e) {
                }
            }
            System.out.println("finished Layouting after " + counter + " iterations.");
        }
    }

    private void initForceMatrix() {
        this.maxDist = 0;
        int size = model.getNodes().size();
        this.forceMatrix = new double[size][];
        this.weightMatrix = new double[size][];

        Iterator<XEventClass> iter = model.getNodes().keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            XEventClass eClass = iter.next();
            TemporalNode node = model.getNodes().get(eClass);
            position.put(node, i);
        }

        double sumDist = 0, maxDist = 0;
        double maxWeight = 0;

        iter = model.getNodes().keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            XEventClass eClass = iter.next();
            TemporalNode node = model.getNodes().get(eClass);
            this.forceMatrix[i] = new double[size];
            this.weightMatrix[i] = new double[size];
            Arrays.fill(this.forceMatrix[i], 0);
            Arrays.fill(this.weightMatrix[i], 0);
            if (model.getConnections().containsKey(node)) {
                for (TemporalConnection conn : model.getConnections().get(node)) {
                    if (conn.getStats().getN() > 0) {
                        int j = position.get(conn.getAfter());
                        if (java.lang.Double.isNaN(conn.getStats().getMean())) {
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

    public void updateNodePosition(TemporalNode selectedNode, int x, int y) {
        nodePositions.get(selectedNode).setLocation(x, y);
        revalidate();
        repaint();
    }

    public TemporalNode getClickedNode(int x, int y) {
        TemporalNode nearestNode = null;
        double minDist = java.lang.Double.POSITIVE_INFINITY;
        Double pointClicked = new Double(x, y);
        for (TemporalNode node : nodePositions.keySet()) {
            double nodeDist = pointClicked.distance(nodePositions.get(node));
            if (nodeDist < minDist) {
                minDist = nodeDist;
                nearestNode = node;
            }
        }
        if (minDist <= NODE_SIZE / 2) {
            return nearestNode;
        }
        return null;
    }

    private void normalizeForceMatrix(double maxDist, double sumDist, double maxWeight) {
        this.maxDist = 0;
        for (int i = 0; i < this.forceMatrix.length; i++) {
            for (int j = 0; j < this.forceMatrix[i].length; j++) {
                if (i == position.get(model.getStartNode())) {
                    this.forceMatrix[i][j] = this.forceMatrix[i][j] / maxDist;
                } else {
                    this.forceMatrix[i][j] = this.forceMatrix[i][j] / sumDist;
                }
                this.maxDist = Math.max(this.maxDist, this.forceMatrix[i][j]);

                this.weightMatrix[i][j] = this.weightMatrix[i][j] / maxWeight;
            }
        }
    }

    public void initPositions() {
        // assign positions randomly to nodes:
        for (TemporalNode node : model.getNodes().values()) {
            if (node.getEventClass().equals(TemporalModel.START_CLASS)) {
                // the start node is fixed.
                nodePositions.put(node, new Point2D.Double(NODE_SIZE, HEIGHT / 2));
            } else {
                nodePositions.put(node, new Point2D.Double(random.nextDouble() * WIDTH, random.nextDouble() * HEIGHT));
            }
            nodeInertias.put(node, new Point2D.Double(0, 0));

        }
        revalidate();
        repaint();
    }

    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        // draw nodes:
        for (TemporalNode node : nodePositions.keySet()) {
            int i = position.get(node);
            for (TemporalNode target : nodePositions.keySet()) {
                int j = position.get(target);
                if (i < j && forceMatrix[i][j] > 0) {
                    drawArrow(g2, node, target, weightMatrix[i][j], 1);
                }
                if (i > j && forceMatrix[i][j] < 0) {
                    drawArrow(g2, target, node, weightMatrix[i][j], 1);
                }
            }

            drawNode(g2, node);
        }
    }

    private void drawArrow(Graphics2D g2, TemporalNode node, TemporalNode target, double weight, double alpha) {
        Double position = nodePositions.get(node);
        Double targetPosition = nodePositions.get(target);
        drawArrow(g2, Color.getHSBColor(0f, 0f, (float) (1 - weight)), new BasicStroke((float) (weight * 2)), position, targetPosition, alpha);
    }

    protected void drawArrow(Graphics2D g2, Color color, Stroke stroke, Double position, Double targetPosition, double alpha) {
        g2.setStroke(stroke);
        g2.setColor(color);
        g2.drawLine((int) position.x, (int) position.y, (int) targetPosition.x, (int) targetPosition.y);
        //g2.drawOval((int)(targetPosition.x-(targetPosition.x-position.x)*0.05), (int)(targetPosition.y-(targetPosition.y-position.y)*0.05), 3, 3);
        g2.setStroke(new BasicStroke(1));

        double angle = Math.atan2(targetPosition.y - position.y, targetPosition.x - position.x);

        AffineTransform tx = new AffineTransform();
        tx.translate(targetPosition.x, targetPosition.y);
        tx.rotate((angle - Math.PI / 2d));

        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 250)));
        g.setTransform(tx);
        g.fill(arrowHead);
        g.dispose();
    }

    /**
     * Updates all positions of the nodes according to the temporal forces
     *
     * @return whether the positions changed
     */
    public synchronized boolean updatePositions() {
        double scale = (WIDTH * zoom) / maxDist;
        double dampening = 0.1;

        int startNodeIndex = position.get(model.getStartNode());

        boolean changed = false;

        for (TemporalNode node : model.getNodes().values()) {
            int i = position.get(node);
            Point2D.Double pos = nodePositions.get(node);
            Point2D.Double oldPos = new Point2D.Double(pos.x, pos.y);
            Point2D.Double force = new Point2D.Double(0, 0);

            List<Point2D.Double> forces = new ArrayList<>();
            nodeForces.put(node, forces);

            // fix start node in the left
            if (node.getEventClass().equals(TemporalModel.START_CLASS)) {
                pos.setLocation(NODE_SIZE, HEIGHT / 2); // the start node is fixed.
            } else {
//				// add force to relative position on the x-axis
//				double idealXPos = model.getRelativePositionInTrace(node)*WIDTH;
//				
//				force.setLocation(idealXPos - pos.getX(), 0);
//				forces.add(new Point2D.Double(idealXPos - pos.getX(),0));

                // add force from neighboring nodes
                for (TemporalNode previousNode : model.getNodes().values()) {
                    int j = position.get(previousNode);
                    if (i != j) {
                        Point2D.Double predecessorPos = nodePositions.get(previousNode);
                        double dist = predecessorPos.distance(pos);

                        double rawForce = (forceMatrix[j][i] * weightMatrix[j][i] - forceMatrix[i][j] * weightMatrix[i][j]);

                        double idealDist = dist;
                        if (Math.abs(rawForce) < 0.01) {
                            // no force to be applied, unless...
                            if (dist < NORMAL_DISTANCE) {
                                // push away a bit
                                idealDist = NORMAL_DISTANCE;
                            }
                        } else {
                            idealDist = Math.max(NORMAL_DISTANCE, scale * rawForce);
                        }
                        //					System.out.println("idealDist of node "+node.getName()+" to predecessor node "+previousNode.getName()+": "+idealDist+" (dist: "+dist+")");

                        double newForce = (idealDist - dist) / idealDist;
                        if (newForce < -2) {
                            newForce = -2;
                        }
                        if (newForce > 2) {
                            newForce = 2;
                        }
                        Point2D.Double vector = new Point2D.Double(pos.x - predecessorPos.x, pos.y - predecessorPos.y);

                        //
                        //double adjustment = (dist - idealDist) * dampening;

                        force.setLocation(force.x + (newForce * vector.x), force.y + (newForce * vector.y));
                        forces.add(new Point2D.Double(newForce * vector.x, newForce * vector.y));
                    }
                }

                // pull towards point P at P(width, 1/2 height) with force (1-x) where x is forceMatrix[0][i]
                double relativePointInTime = forceMatrix[startNodeIndex][i];
                Point2D.Double targetPosition = new Point2D.Double(relativePointInTime * WIDTH, 0.5 * HEIGHT);

                Point2D.Double timeVector = new Point2D.Double(targetPosition.x - pos.x, targetPosition.y - pos.y);
                force.setLocation(force.x + timeVector.x * 0.2, force.y + timeVector.y * 0.2);
                forces.add(new Point2D.Double(timeVector.x * 0.2, timeVector.y * 0.2));

                // update inertia by adding the force
                Point2D.Double inertia = nodeInertias.get(node);
                inertia.setLocation((inertia.x + force.x) * dampening, (inertia.y + force.y) * dampening);

                pos.setLocation(pos.x + inertia.x, pos.y + inertia.y);
//				if (model.getLatestNode().equals(node)){
//					pos.setLocation(idealXPos, pos.y);
//				}
            }
            if (oldPos.distance(pos) > 0.001) {
                changed = true;
            }

        }
        this.invalidate();
        this.repaint();
        return changed;
    }

    private synchronized void drawNode(Graphics2D g2, TemporalNode node) {
        Double position = nodePositions.get(node);
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(Color.red);
        g2.drawOval((int) position.x - NODE_SIZE / 2, (int) position.y - NODE_SIZE / 2, NODE_SIZE, NODE_SIZE);
        g2.setColor(Color.black);
        g2.drawString(node.getName(), (int) position.x + NODE_SIZE / 2 + 2, (int) position.y + NODE_SIZE / 4);

        if (nodeForces.containsKey(node)) {
            int i = 0;
            float forces = 5;
            for (Point2D.Double force : nodeForces.get(node)) {
                drawArrow(g2, Color.getHSBColor(i++ / forces, 1f, 0.7f), new BasicStroke(1f), position, new Point2D.Double(position.x + force.x, position.y + force.y), 0.5);
            }
        }
    }

    public void zoom(boolean in) {
        if (in) {
            this.zoom *= 1.2;
        } else {
            this.zoom *= 0.85;
        }
        System.out.println("Zoom: " + zoom);
        updatePositions();
    }

    public boolean isPaused() {
        return paused;
    }

    public void runLayout() {
        UpdateThread updater = new UpdateThread(this);
        updateThread = new Thread(updater);
        updateThread.start();
    }

}
