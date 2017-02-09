package org.processmining.plugins.stochasticpetrinet.simulator;

import cern.colt.Arrays;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import edu.uci.ics.jung.algorithms.layout.DAGLayout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.VisualizationImageServer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import edu.uci.ics.jung.visualization.picking.ShapePickSupport;
import intervalTree.Interval;
import intervalTree.IntervalTree;
import org.apache.commons.collections15.Transformer;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.extension.std.XTimeExtension;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.external.Allocation;
import org.processmining.plugins.stochasticpetrinet.external.anomaly.GreedyAnomalyResolver;
import org.processmining.plugins.stochasticpetrinet.external.interaction.Activity;
import org.processmining.plugins.stochasticpetrinet.external.interaction.LocationChange;
import org.processmining.plugins.stochasticpetrinet.external.interaction.Record;
import org.utils.datastructures.graph.BellmanFordDistance;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * This class takes a location unaware log and transforms it into a location
 * aware log. The effect is that we create delays between activities due to
 * resources moving around on a map. We take a graph containing the connections
 * between different areas and the distances between these areas as edge
 * weights.
 * <p>
 * 1. We make one pass through the log to decide which activities are performed
 * where. This is given by a probabilistic mapping of location options per
 * activity.
 * <p>
 * 2. We check for each trace and every activity: where have the resources been
 * before, and how much time has passed since? -> if the resources were seen
 * somewhere else, and the time that passed since is big enough to include a
 * passage time, then it is fine. -> otherwise, we need to shift the activity by
 * the passage time of the latest arriving resource. -> This shift is transitive
 * to all activities having some transitive relation to the current one.
 *
 * @author Andreas Rogge-Solti
 */
public class LogLocationDelayInducer {

    private static final String ARTIFICIAL_START_EVENT = "artificialStartEvent";
    private Map<String, Double> resourceSpeedMap;
    private Random random;

    public LogLocationDelayInducer() {
        this.resourceSpeedMap = new HashMap<String, Double>();
        this.random = new Random(1);
    }

    public XLog induceLocationDelay(final XLog log, WorldConfiguration wc, StochasticNet net) {
        System.out.println("original log:");
        for (XTrace trace : log) {
            System.out.println(StochasticNetUtils.debugTrace(trace));
        }
        System.out.println("------------------");

        Map<Transition, Set<Allocation>> mapping = wc.getTransitionLocationMapping();
        Map<String, Transition> transitionsByName = extractTransitions(net);
        IntervalTree<Activity> logActivities = new IntervalTree<Activity>();

        Map<String, SortedSet<Activity>> resourceViews = new HashMap<String, SortedSet<Activity>>();

        XLog newLog = StochasticNetUtils.cloneLog(log);

        XEvent artificialStartEvent = XFactoryRegistry.instance().currentDefault().createEvent();
        XTimeExtension.instance().assignTimestamp(artificialStartEvent, 0);

        Activity startActivity = new Activity(artificialStartEvent, artificialStartEvent, ARTIFICIAL_START_EVENT);

        Graph<Activity, LocationChange> dependencyGraph = new DirectedSparseMultigraph<Activity, LocationChange>();

        dependencyGraph.addVertex(startActivity);

        //Assume start and end events!
        for (XTrace trace : newLog) {
            Map<String, XEvent> startEvents = new HashMap<String, XEvent>();
            for (XEvent event : trace) {
                String name = XConceptExtension.instance().extractName(event) + ","
                        + XConceptExtension.instance().extractInstance(event);
                StandardModel lifecycle = XLifecycleExtension.instance().extractStandardTransition(event);
                if (StandardModel.START.equals(lifecycle)) {
                    startEvents.put(name, event);
                }
                if (lifecycle.equals(StandardModel.COMPLETE)) {
                    XEvent startEvent = null;
                    if (!startEvents.containsKey(name)) {
                        // last event is either arrival event, or we don't have start events at all!
                        startEvent = event;
                    } else {
                        startEvent = startEvents.remove(name);
                    }
                    long startTime = XTimeExtension.instance().extractTimestamp(startEvent).getTime();
                    long endTime = XTimeExtension.instance().extractTimestamp(event).getTime();

                    Activity activity = new Activity(startEvent, event, XConceptExtension.instance().extractName(event));

                    String[] resources = activity.getResources();
                    for (int i = 0; i < resources.length; i++) {
                        String resource = resources[i];

                        if (i < resources.length - 1) {
                            // associated resource: (patients are there full time)
                            double ratioSpentTogether = 1;
                            if (wc.isResourceTimeAsynchronous()) {
                                Double factor = wc.getResourceSpeedFactor(resource);
                                ratioSpentTogether = (0.5 / factor);
                            }
                            activity.setRealDuration(resource, (long) (activity.getDuration() * ratioSpentTogether));
                        }
                        if (!resourceViews.containsKey(resource)) {
                            TreeSet<Activity> activitiesOfResource = new TreeSet<Activity>();
                            activitiesOfResource.add(startActivity);
                            resourceViews.put(resource, activitiesOfResource);
                        }
                        SortedSet<Activity> resourceView = resourceViews.get(resource);
                        resourceView.add(activity);
                    }
                    logActivities.addInterval(new Interval<Activity>(startTime, endTime, activity));
                }
            }
        }

        for (String resource : resourceViews.keySet()) {
            SortedSet<Activity> resourceView = resourceViews.get(resource);
            Activity last = null;
            for (Activity activity : resourceView) {
                if (last == null) {
                    last = activity;
                } else {
                    LocationChange lc = getLocationChange(last, activity, resource, wc);
                    if (!dependencyGraph.containsVertex(activity)) {
                        System.out.println("Creating activity: " + activity.toString());
                        dependencyGraph.addVertex(activity);
                    }
                    dependencyGraph.addEdge(lc, last, activity);
                    last = activity;
                }
            }
        }

        BellmanFordDistance<Activity, LocationChange> shortestPaths = new BellmanFordDistance<Activity, LocationChange>(dependencyGraph, new Transformer<LocationChange, Number>() {
            public Number transform(LocationChange lc) {
                return -lc.getFullDuration();
            }
        });

//		final DijkstraDistance<Activity, LocationChange> shortestPaths = new ReverseDijkstraShortestPath<Activity, LocationChange>(
//				dependencyGraph, new Transformer<LocationChange, Number>() {
//					public Number transform(LocationChange lc) {
//						return -lc.getFullDuration();
//					}
//				});

        Map<Activity, Number> earliestStartTimes = shortestPaths.getDistanceMap(startActivity);

//		visualizeGraph(dependencyGraph, false, earliestStartTimes, net.getTimeUnit(), startActivity);


        // update start times of activities, such that they are consistent with the new earliest start times
        for (Activity activity : earliestStartTimes.keySet()) {
            long earliestStartTime = -earliestStartTimes.get(activity).longValue();

            if (activity.getStartTime() < earliestStartTime) {
                long timeDiff = earliestStartTime - activity.getStartTime();
                System.out.println("activity " + activity + " is shifted by " + timeDiff + " ms.");
                activity.shift(timeDiff);
            }
        }
        SortedMultiset<Record> sortedRecords = TreeMultiset.create();
        for (Activity a : dependencyGraph.getVertices()) {
            if (a != startActivity) {
                String[] resources = a.getResources();
                for (String resource : resources) {
                    long startTime = a.getEndTime() - a.getRealDuration(resource);
                    sortedRecords.add(new Record(resource, startTime, a.getEndTime(), a.getName(), a
                            .getLocation()));
                }
            }
        }

        for (LocationChange lc : dependencyGraph.getEdges()) {
            if (lc.getDuration() > 0) {
                Activity sourceActivity = dependencyGraph.getSource(lc);
                sortedRecords.add(new Record(lc.getResource(), sourceActivity.getEndTime(), sourceActivity.getEndTime()
                        + lc.getDuration(), Record.IDLE, lc.getAreasPassed().get(0)));
            }
        }
//		writeRecordsToFile(sortedRecords, "results.csv");

        writeRecordsToFile(sortedRecords, wc.getFilePrefix() + "results_good.csv");

        introduceMissingValues(sortedRecords, wc, Record.ANOMALY);

        writeRecordsToFile(sortedRecords, wc.getFilePrefix() + "results_anomalous.csv");

        GreedyAnomalyResolver resolver = new GreedyAnomalyResolver();
        SortedMultiset<Record> imputedRecords = resolver.getImputedRecords(sortedRecords);
        writeRecordsToFile(imputedRecords, wc.getFilePrefix() + "results_anomalous_imputed.csv");


//		SortedSensorIntervals intervals = LogToSensorIntervalConverter.convertLog(log, net.getTimeUnit(), false, 1);
//		JComponent orig = SensorIntervalVisualization.visualize(null, intervals);
//
//		SortedSensorIntervals intervalsAdjusted = LogToSensorIntervalConverter.convertLog(newLog, net.getTimeUnit(),
//				false, 1);
//		JComponent adjusted = SensorIntervalVisualization.visualize(null, intervalsAdjusted);
//
//		JComponent comparison = new JPanel();
//		comparison.setLayout(new BorderLayout());
//		comparison.add(orig, BorderLayout.NORTH);
//		comparison.add(adjusted, BorderLayout.SOUTH);
//
//		TestUtils.showComponent(comparison);

        return newLog;
    }

    protected void writeRecordsToFile(SortedMultiset<Record> sortedRecords, String fileName) {
        StringBuffer result = new StringBuffer();
        result.append(Record.getHeader());
        result.append("\n");
        for (Record r : sortedRecords) {
            result.append(r.getRow());
            result.append("\n");
        }
        try {
            FileWriter writer = new FileWriter(new File(fileName));
            writer.write(result.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * The trick is to erase consecutive records from a resource (according to a geometric distribution).
     * The erased records are substituted with records covering the same time span and the label "anomaly"(ie., the content of the missingRecordName param).
     * This trick is repeated until we have a certain percentage of missing entries.
     *
     * @param sortedRecords     the records that will be changed.
     * @param wc                WorldConfiguration containing the ratio of missingness
     * @param missingRecordName String label of the record that will cover the missing entries
     */
    private void introduceMissingValues(SortedMultiset<Record> sortedRecords, WorldConfiguration wc, String missingRecordName) {
        double currentMissingRatio = 0.0;
        int missingRecords = 0;
        final int totalRecords = countValidRecords(sortedRecords);
        while (currentMissingRatio < wc.getMissingRatio()) {
            missingRecords += removeSomeRecords(sortedRecords, missingRecordName);
            currentMissingRatio = missingRecords / (double) totalRecords;
        }
    }

    private int countValidRecords(SortedMultiset<Record> sortedRecords) {
        int count = 0;
        for (Record r : sortedRecords) {
            if (r.getLocation() != null && !r.getLocation().isEmpty() && !r.getLocation().equals(Record.ARRIVING)) {
                count++;
            }
        }
        return count;
    }

    private int removeSomeRecords(SortedMultiset<Record> sortedRecords, String missingRecordName) {
        // replace consecutive records of a resource with one "anomaly" record
        int position = (int) (Math.random() * sortedRecords.size());
        int pos = 0;
        long startTime = -1;
        long endTime = -1;
        String resource = "";
        int removed = 0;

        Iterator<Record> recordIter = sortedRecords.iterator();
        Record r = null;
        do {
            r = recordIter.next();
            pos++;
        } while (recordIter.hasNext() && pos < position);

        if (!r.getLocation().equals(Record.ARRIVING) && !r.getLocation().equals(missingRecordName) && !GreedyAnomalyResolver.isPatient(r.getId())) {
            // start erasing records here!
            startTime = r.getStartTime();
            endTime = r.getEndTime();
            resource = r.getId();
            recordIter.remove();
            removed++;
            // let's remove 1-10 records at random:
            int recordsToRemove = (int) (Math.random() * 9);
            while (removed < recordsToRemove && recordIter.hasNext()) {
                r = recordIter.next();
                if (r.getId().equals(resource)) {
                    endTime = r.getEndTime();
                    recordIter.remove();
                    removed++;
                }
            }
            Record newRecord = new Record(resource, startTime, endTime, missingRecordName, missingRecordName);
            sortedRecords.add(newRecord);
        }
        return removed;
    }

    protected void visualizeGraph(Graph<Activity, LocationChange> dependencyGraph, boolean show, final Map<Activity, Number> earliestStartTimes, final TimeUnit unit, Activity root) {
        //		Layout<Activity, LocationChange> layout = new TopologyLayout<Activity, LocationChange>();
        //		Layout<Activity, LocationChange> layout = new FRLayout<Activity, LocationChange>(dependencyGraph);
        DAGLayout<Activity, LocationChange> layout = new DAGLayout<Activity, LocationChange>(dependencyGraph);
        layout.setRoot(root);
        layout.setSize(new Dimension(500, 2000));
        VisualizationViewer<Activity, LocationChange> vv = new VisualizationViewer<Activity, LocationChange>(layout);
//		vv.scaleToLayout(new ViewScalingControl());


        Transformer<Activity, String> vertexLabelTransformer = new Transformer<Activity, String>() {
            public String transform(Activity arg0) {
                return arg0.getName() + "\n " + (int) (arg0.getStartTime() / unit.getUnitFactorToMillis()) + "/" + (int) (earliestStartTimes.get(arg0).longValue() / unit.getUnitFactorToMillis());
            }
        };
        Transformer<LocationChange, String> edgeLabelTransformer = new Transformer<LocationChange, String>() {
            public String transform(LocationChange arg0) {
                return String.valueOf((int) (arg0.getFullDuration() / unit.getUnitFactorToMillis()) + "," + arg0.getResource());
            }
        };

        vv.getRenderContext().setVertexLabelTransformer(vertexLabelTransformer);
        vv.getRenderContext().setEdgeLabelTransformer(edgeLabelTransformer);
        vv.getRenderContext().setArrowFillPaintTransformer(new Transformer<LocationChange, Paint>() {
            public Paint transform(LocationChange arg0) {
                return arg0.getColor();
            }
        });
        vv.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<LocationChange, Paint>() {
            public Paint transform(LocationChange arg0) {
                return arg0.getColor();
            }
        });
        /* allow clickable stuff */
        vv.setPickSupport(new ShapePickSupport<Activity, LocationChange>(vv));
		/* allow us to define mouse stuff in the graph and respond... */
        DefaultModalGraphMouse<Activity, LocationChange> graphMouse = new DefaultModalGraphMouse<Activity, LocationChange>();
        graphMouse.setMode(ModalGraphMouse.Mode.PICKING);
        vv.setGraphMouse(graphMouse);
		
		/* this will show up when mouse moves over...feel free to change */
        vv.setVertexToolTipTransformer(new Transformer<Activity, String>() {
            public String transform(Activity a) {
                return a.getName() + "\n " + Arrays.toString(a.getResources()) + "\n " + "start: " + a.getStartTime()
                        + "\n " + "end: " + a.getEndTime() + "\n " + "loc: " + a.getLocation();
            }
        });
        if (show) {
            JFrame jf = new JFrame();
            final GraphZoomScrollPane panel = new GraphZoomScrollPane(vv);
            jf.getContentPane().add(panel);
            jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            jf.pack();
            jf.setVisible(true);

            try {
                Thread.sleep(1000000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }

        // Create the VisualizationImageServer
        // vv is the VisualizationViewer containing my graph
        VisualizationImageServer<Activity, LocationChange> vis = new VisualizationImageServer<Activity, LocationChange>(
                vv.getGraphLayout(), vv.getGraphLayout().getSize());

        // Configure the VisualizationImageServer the same way
        // you did your VisualizationViewer. In my case e.g.

        vis.setBackground(Color.WHITE);
        vis.getRenderContext().setEdgeLabelTransformer(edgeLabelTransformer);
        vis.getRenderContext().setVertexLabelTransformer(vertexLabelTransformer);
        vis.getRenderContext().setEdgeDrawPaintTransformer(new Transformer<LocationChange, Paint>() {
            public Paint transform(LocationChange arg0) {
                return arg0.getColor();
            }
        });
        Dimension size = new Dimension(vv.getGraphLayout().getSize());
        size.setSize(size.width + 200, size.height + 200);

        vis.setSize(size.width - 50, size.height - 50);

        // Create the buffered image
        BufferedImage image = (BufferedImage) vis.getImage(new Point2D.Double(
                        vv.getGraphLayout().getSize().getWidth() / 2 + 10, vv.getGraphLayout().getSize().getHeight() / 2 + 10),
                size);

        // Write image to a png file
        File outputfile = new File("graph.png");

        try {
            ImageIO.write(image, "png", outputfile);
        } catch (IOException e) {
            // Exception handling
        }

    }

    private LocationChange getLocationChange(Activity last, Activity activity, String resource, WorldConfiguration wc) {
        LocationChange lc = new LocationChange(resource);
        lc.setDuration(0);

        List<String> areasPassed = new LinkedList<String>();
        if (!last.getStartEvent().equals(last.getEndEvent())) {
            lc.setLastActivityDuration(last.getRealDuration(resource));
            // TODO: use a more sophisticated approach to infer the travel time (shortest path on a graph of locations) and then use waiting buffer at the end!
            areasPassed.add("hallway");
            double distance = getDistance(last, activity, wc);
            lc.setDuration(getDuration(distance, resource));
        } else {
            if (last.getName().equals(ARTIFICIAL_START_EVENT)) {
                lc.setDuration(activity.getStartTime());
                areasPassed.add("arriving");
            }
        }
        lc.setAreasPassed(areasPassed);
        return lc;
    }

    protected double getDistance(Activity last, Activity activity, WorldConfiguration wc) {
        double distance = 0;
        String from = last.getLocation();
        String to = activity.getLocation();

        if (from != null && !from.isEmpty()) {
            int fromId = wc.getLocationId(from);
            int toId = wc.getLocationId(to);
            distance = wc.getDistances()[fromId][toId];
        }
        return distance;
    }

    /**
     * Given distance in meters, it returns the time in ms it takes to walk it
     * with a random speed.
     *
     * @param distance
     * @param resource
     * @return
     */
    private long getDuration(double distance, String resource) {
        if (!resourceSpeedMap.containsKey(resource)) {
            resourceSpeedMap.put(resource, getRandomSpeed());
        }
        return (long) ((distance / resourceSpeedMap.get(resource)) * 1000);
    }

    /**
     * Returns a random human walking speed in m/s (5 km/h +- a normal noise
     * with 0.5 standard deviation)
     *
     * @return
     */
    private Double getRandomSpeed() {
        return (5. + random.nextGaussian() / 2.) / 3.6;
    }

    private Map<String, Transition> extractTransitions(Petrinet net) {
        Map<String, Transition> transitionsByName = new HashMap<String, Transition>();
        for (Transition transition : net.getTransitions()) {
            transitionsByName.put(transition.getLabel(), transition);
        }
        return transitionsByName;
    }
}
