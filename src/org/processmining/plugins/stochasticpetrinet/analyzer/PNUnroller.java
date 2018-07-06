package org.processmining.plugins.stochasticpetrinet.analyzer;

import gnu.trove.map.hash.THashMap;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.*;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.graphbased.directed.petrinet.impl.PetrinetFactory;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.models.semantics.Semantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.astar.petrinet.manifestreplay.PNManifestFlattener;
import org.processmining.plugins.connectionfactories.logpetrinet.TransEvClassMapping;
import org.processmining.plugins.manifestanalysis.visualization.performance.PerfCounter;
import org.processmining.plugins.petrinet.manifestreplayer.EvClassPattern;
import org.processmining.plugins.petrinet.manifestreplayer.PNManifestReplayerParameter;
import org.processmining.plugins.petrinet.manifestreplayer.TransClass2PatternMap;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.DefTransClassifier;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.TransClass;
import org.processmining.plugins.petrinet.manifestreplayer.transclassifier.TransClasses;
import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.AllSyncReplayResult;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.plugins.stochasticpetrinet.enricher.ReliableInvisibleTransitionPerfCounter;

import java.util.*;

public class PNUnroller {

//	private List<StepTypes> stepTypes = null;
//	private List<Object> nodeInstances = null;

    //private XEventClasses ec = null;
    private XEventClassifier classifier = null;

    public PNUnroller(XEventClassifier classifier) {
        //this.ec = ec;
        this.classifier = classifier;
    }

    public PetrinetGraph unrolPNbasedOnTrace(XLog originalTrace, TransEvClassMapping mapping, Petrinet net, Marking initialMarking, Marking finalMarking, boolean debug) throws Exception {
        // assume that event class matches with each transition.
        // first construct alignment
        long start = System.currentTimeMillis();
        Pair<SyncReplayResult, PNManifestFlattener> replayResult = StochasticNetUtils.replayTrace(originalTrace, mapping, net, initialMarking, finalMarking, classifier);
        long now = System.currentTimeMillis();
        if (debug) {
            System.out.println((now - start) + "ms for replaying the trace.");
            start = now;
        }
        Map<Transition, Transition> origTransMap = new THashMap<>();
        for (Transition t : replayResult.getSecond().getFlatTransArr()){
            origTransMap.put(t, replayResult.getSecond().getOrigTransFor(t));
        }
        PetrinetGraph constructPN = constructPN(net, initialMarking, replayResult.getFirst().getStepTypes(), replayResult.getFirst().getNodeInstance(), origTransMap);
        if (debug) System.out.println((System.currentTimeMillis() - start) + "ms for constructing the net.");
        return constructPN;
    }

	public PetrinetGraph unrollPNbasedOnAlignment(AllSyncReplayResult replayResult, int alignmentIndex, Petrinet net, Marking initialMarking) throws Exception{
		List<StepTypes> stepTypes = replayResult.getStepTypesLst().get(alignmentIndex);
		List<Object> nodeInstances = replayResult.getNodeInstanceLst().get(alignmentIndex);
		return constructPN(net, initialMarking, stepTypes, nodeInstances, null);
	}

    public Map<Transition, Double> replayTraceUnrolledPN(PetrinetGraph net, XLog originalTrace, XEventClasses ec) throws Exception {
        // parameters inits
        // initialMarking
        Marking initMarking = new Marking();
        List<Place> initialPlaces = getInitialPlaces(net);
        for (Place p : initialPlaces) {
            initMarking.add(p);
        }
        // final marking
        List<Place> finalPlaces = getFinalPlaces(net);
        Marking finalMarking = new Marking();
        for (Place p : finalPlaces) {
            finalMarking.add(p);
        }
        Marking[] finalMarkings = new Marking[1];
        finalMarkings[0] = finalMarking;
        // event classes, costs
        Map<XEventClass, Integer> mapEvClass2Cost = new HashMap<XEventClass, Integer>();
        for (XEventClass c : ec.getClasses()) {
            mapEvClass2Cost.put(c, 1);
        }
        // transition classes
        Map<TransClass, Integer> trans2Cost = new HashMap<TransClass, Integer>();
        Map<TransClass, Integer> transSync2Cost = new HashMap<TransClass, Integer>();
        TransClasses transClasses = new TransClasses(net, new DefTransClassifier());
        for (TransClass tc : transClasses.getTransClasses()) {
            // check if tc corresponds with invisible transition
            boolean check = StochasticNetUtils.getTransitionClassIsInvisible(tc, net);
            if (!check) {
                trans2Cost.put(tc, 1);
            } else {
                trans2Cost.put(tc, 0);
            }
            transSync2Cost.put(tc, 0);
        }

        Map<TransClass, Set<EvClassPattern>> mapTCtiECP = new HashMap<TransClass, Set<EvClassPattern>>();
        // fill map, for each transition there is exactly one event class (pattern)
        XEventClass evClassDummy = new XEventClass("DUMMY", -1);
        for (Transition t : net.getTransitions()) {
            boolean found = false;
            TransClass transClassT = transClasses.getClassOf(t);
            EvClassPattern patt = new EvClassPattern();
            // get the event class for the transition
            for (XEventClass c : ec.getClasses()) {
                String label = t.getLabel();
                //label = label.replace("unrol", "");
                int ind = label.lastIndexOf(StochasticNetUtils.SEPARATOR_STRING);
                label = label.substring(0, ind);
                label = label.trim();
                if (c.getId().startsWith(label)) {
                    patt.add(c);
                    Set<EvClassPattern> setPatt = new HashSet<EvClassPattern>();
                    setPatt.add(patt);
                    mapTCtiECP.put(transClassT, setPatt);
                    found = true;
                    break;
                }
            }
            // if not found than assign empty one or dummy one?
            if (!found) {
                Set<EvClassPattern> setPatt = new HashSet<EvClassPattern>();
                EvClassPattern pattDummy = new EvClassPattern();
                pattDummy.add(evClassDummy);
                setPatt.add(pattDummy);
                //mapTCtiECP.put(transClassT, setPatt);
            }
        }

        TransClass2PatternMap mapping = new TransClass2PatternMap(originalTrace, net, this.classifier, transClasses, mapTCtiECP);

        PNManifestReplayerParameter parameters = new PNManifestReplayerParameter();
        parameters.setInitMarking(initMarking);
        parameters.setFinalMarkings(finalMarkings);
        parameters.setGUIMode(false);
        parameters.setMapEvClass2Cost(mapEvClass2Cost);
        parameters.setMaxNumOfStates(50000);
        parameters.setTrans2Cost(trans2Cost);
        parameters.setTransSync2Cost(transSync2Cost);
        parameters.setMapping(mapping);

        ManifestEvClassPattern manifest = (ManifestEvClassPattern) StochasticNetUtils.replayLog(null, net, originalTrace, parameters, true);

        PerfCounter counter = new ReliableInvisibleTransitionPerfCounter(StochasticNetUtils.getMinimalDate(originalTrace.get(0)).getTime());
        final boolean[] caseFilter = new boolean[originalTrace.size()];
        Arrays.fill(caseFilter, true);
        counter.init(manifest, "time:timestamp", Date.class, caseFilter);
        //double[] transStat = counter.getTransStats(manifest, counter.getEncOfTrans(t));

        double fitness = manifest.getTraceFitness(0);
        if (fitness != 1.0) {
//			System.out.println("fitness is not 1.0 for trace " + XConceptExtension.instance().extractName(originalTrace));
        }
        // get performance values
        Map<Transition, Double> mappingTransitionWT = new HashMap<Transition, Double>();
        for (Transition t : net.getTransitions()) {
            if (!t.isInvisible()) {
                double[] transStat = counter.getTransStats(manifest, counter.getEncOfTrans(t));
                if (transStat != null) {
                    // waiting time is position 23
                    double wt = transStat[23];
                    mappingTransitionWT.put(t, wt);
                } else {
                    mappingTransitionWT.put(t, Double.NaN);
                }
            }
        }
        return mappingTransitionWT;
    }


    /**
     * @param net
     * @param stepTypes
     * @param nodeInstances
     * @param origTransMap maps from new transitions in the efficient net to the original transitions
     * @return
     * @throws Exception
     */
    private PetrinetGraph constructPN(Petrinet net, Marking initialMarking, List<StepTypes> stepTypes, List<Object> nodeInstances, Map<Transition,Transition> origTransMap) {
        Map<Place, Place> mappingPlaces = new HashMap<Place, Place>();
        Map<Transition, Transition> mappingTransitions = new HashMap<Transition, Transition>();
//		List<Place> incomingPlaces = new ArrayList<Place>();
        PetrinetGraph unrolPN;
        if (net instanceof StochasticNet) {
            unrolPN = new StochasticNetImpl(net.getLabel() + " unrolled");
        } else {
            unrolPN = PetrinetFactory.newPetrinet(net.getLabel() + " unrolled");
        }
        // first get initial places via initial marking
        Iterator<Place> iterator = initialMarking.iterator();
        while (iterator.hasNext()) {
            Place place = iterator.next();
            // clone the place
            Place newPlace = unrolPN.addPlace("start unrol");
//			incomingPlaces.add(newPlace);
            mappingPlaces.put(newPlace, place);
        }
        // initialize
        Semantics<Marking, Transition> semantics = StochasticNetUtils.getSemantics(net);
        semantics.initialize(net.getTransitions(), initialMarking);
        // replay the net according to alignment
        Map<String, Integer> transitionCounter = new HashMap<String, Integer>();
        Map<String, Integer> placeCounter = new HashMap<String, Integer>();
        // initalize the transitionCounter
        for (Transition t : net.getTransitions()) {
            transitionCounter.put(t.getLabel(), 0);
        }
        // initialize the placeCounter
        for (Place p : net.getPlaces()) {
            placeCounter.put(p.getLabel(), 0);
        }
        for (int i = 0; i < stepTypes.size(); i++) {
            StepTypes stepType = stepTypes.get(i);
            if (stepType.equals(StepTypes.L)) {
                // ignore log only moves when unrolling
            } else {
                Transition nodeInstance = (Transition) nodeInstances.get(i);
                Collection<Transition> transitions = semantics.getExecutableTransitions();
                Transition selectedTrans = selectTransition(transitions, origTransMap, nodeInstance);
                if ((stepType.equals(StepTypes.LMGOOD) || stepType.equals(StepTypes.MINVI) || stepType.equals(StepTypes.MREAL)) && selectedTrans != null) {
                    // find out how often the transition has been used
                    int countTrans = transitionCounter.get(selectedTrans.getLabel());
                    String labelName = selectedTrans.getLabel() + StochasticNetUtils.SEPARATOR_STRING + countTrans;
                    Transition addTransition;
                    if (net instanceof StochasticNet) {
                        TimedTransition tt = (TimedTransition) selectedTrans;
                        addTransition = ((StochasticNet) unrolPN).addTimedTransition(labelName, tt.getWeight(), tt.getDistributionType(), tt.getDistributionParameters());
                    } else {
                        addTransition = unrolPN.addTransition(labelName);
                    }
                    addTransition.setInvisible(selectedTrans.isInvisible());
                    countTrans = countTrans + 1;
                    transitionCounter.put(selectedTrans.getLabel(), countTrans);
                    mappingTransitions.put(addTransition, selectedTrans);
                    List<Place> finalPlaces = getFinalPlaces(unrolPN);
                    Map<Place, Place> origPlaceToUnrollPlace = new HashMap<Place, Place>();
                    // get corresponding places in orig net
                    for (Place place : finalPlaces) {
                        Place mappedPlace = mappingPlaces.get(place);
                        origPlaceToUnrollPlace.put(mappedPlace, place);
                    }
                    // now check the corresponding places for the transition
                    Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = net.getInEdges(selectedTrans);
                    for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> inEdge : inEdges) {
                        Place sourceOriginal = (Place) inEdge.getSource();
                        // find corresponding place in unrolled net
                        Place sourceUnrol = origPlaceToUnrollPlace.get(sourceOriginal);
                        // add arc in net
                        unrolPN.addArc(sourceUnrol, addTransition);
                    }
                    // for outgoing arcs of selectedTrans, create new place
                    Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdgesOrig = net.getOutEdges(selectedTrans);
                    for (PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode> outEdgeOrig : outEdgesOrig) {
                        Place outPlaceOrig = (Place) outEdgeOrig.getTarget();
                        int countPlace = placeCounter.get(outPlaceOrig.getLabel());
                        Place addPlace = unrolPN.addPlace(outPlaceOrig.getLabel() + StochasticNetUtils.SEPARATOR_STRING + countPlace);
                        placeCounter.put(outPlaceOrig.getLabel(), countPlace + 1);
                        mappingPlaces.put(addPlace, outPlaceOrig);
                        unrolPN.addArc(addTransition, addPlace);
                    }
                    try {
                        semantics.executeExecutableTransition(selectedTrans);
                    } catch (IllegalTransitionException e) {
                        throw new IllegalArgumentException(e);
                    }
                } else {
                    // not expected
                    throw new IllegalArgumentException("step type not expected or node not instance of Transition or alignment not in line with semantics!, alignment type:" + stepType);
                }
            }
        }
        return unrolPN;
    }

    private Transition selectTransition(Collection<Transition> transitions, Map<Transition, Transition> origTransMap, Transition nodeInstance) {
        Transition trans;
        if (origTransMap != null && origTransMap.containsKey(nodeInstance)){
            trans = origTransMap.get(nodeInstance);
        } else {
            trans = getNameEqualTransition(transitions, nodeInstance);
        }
        return trans;
    }

    private Transition getNameEqualTransition(Collection<Transition> transitions, Transition nodeInstance) {
        for (Transition t : transitions) {
            if (t.getLabel().equals(nodeInstance.getLabel())) {
                return t;
            }
        }
        return null;
    }

    public static List<Place> getFinalPlaces(PetrinetGraph petriNet) {
        List<Place> places = new ArrayList<Place>();
        for (Place p : petriNet.getPlaces()) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> outEdges = petriNet.getOutEdges(p);
            if (outEdges == null || outEdges.size() == 0) {
                places.add(p);
            }
        }
        return places;
    }

    public static List<Place> getInitialPlaces(PetrinetGraph petriNet) {
        List<Place> places = new ArrayList<Place>();
        for (Place p : petriNet.getPlaces()) {
            Collection<PetrinetEdge<? extends PetrinetNode, ? extends PetrinetNode>> inEdges = petriNet.getInEdges(p);
            if (inEdges == null || inEdges.size() == 0) {
                places.add(p);
            }
        }
        return places;
    }

    public void setEventClassifier(XEventClassifier eventClassifier) {
        this.classifier = eventClassifier;
    }

}
