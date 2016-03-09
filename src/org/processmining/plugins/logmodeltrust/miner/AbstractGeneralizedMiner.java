package org.processmining.plugins.logmodeltrust.miner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttributeDiscrete;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.processmining.framework.util.Pair;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.plugins.logmodeltrust.converter.RelaxedPT2PetrinetConverter;
import org.processmining.plugins.petrinet.replayresult.PNRepResult;
import org.processmining.plugins.petrinet.replayresult.StepTypes;
import org.processmining.plugins.replayer.replayresult.SyncReplayResult;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;
import org.processmining.processtree.ProcessTree;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.ptconversions.pn.ProcessTree2Petrinet.PetrinetWithMarkings;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

public abstract class AbstractGeneralizedMiner implements GeneralizedMiner {

	private static final String IMPUTED_POS = "trustBasedRepair:imputedPos";
	private static final String ALIGNED_POS = "trustBasedRepair:alignedPos";

	private boolean initialized = false;
	
	protected ProcessTree tree;
	protected XLog log;

	/**
	 * current Petri net of the best model. 
	 */
	protected PetrinetWithMarkings pnWithMarkings;
	
	public void init(ProcessTree inputTree, XLog eventLog) {
		this.tree = inputTree;
		this.log = eventLog;
		doInit();
		this.initialized = true;
	}
	
	/**
	 * Template method for initialization in sub classes. 
	 */
	protected abstract void doInit();

	/**
	 * 
	 */
	public abstract ProcessTree getProcessTreeBasedOnTrust(double trust);

	/**
	 * Decompose the problem:
	 * First "mine" a fitting model based on trust.
	 * Then, align the input log to the new model and remove infrequent log moves, until the trust level is reached.
	 */
	public Pair<XLog, ProcessTree> getFittingPair(double trustLog, double trustModel) {
		if (!initialized){
			throw new IllegalArgumentException("You first need to call init() with inputs!");
		}
		ProcessTree bestTree = getProcessTreeBasedOnTrust(trustModel);
		
		// repair events in the log, until we reach the repaired trust level
		XLog repairedLog = StochasticNetUtils.cloneLog(log);
		int alreadyRepairedEvents = 0;
		try {
			// convert tree to Petri net
			pnWithMarkings = RelaxedPT2PetrinetConverter.convert(bestTree);

			// align petri net with log:
			PNRepResult result = (PNRepResult) StochasticNetUtils.replayLog(null, pnWithMarkings.petrinet, log, false, true);
			// go through events and mark their positions in the alignments
			List<SyncReplayResult> resultList = new ArrayList<>();
			int adjustedEvents = 0;
			for (SyncReplayResult repResult : result){
				resultList.add(repResult);
				int tracePos = 0;
				int alignmentPos = 0;
				for(StepTypes stepType : repResult.getStepTypes()){
					if (stepType.equals(StepTypes.LMNOGOOD)){
						System.out.println("Debug me!");
					}
					if (stepType.equals(StepTypes.L) || stepType.equals(StepTypes.LMGOOD) || stepType.equals(StepTypes.LMNOGOOD) || stepType.equals(StepTypes.LMREPLACED)){
						for (Integer traceIndex : repResult.getTraceIndex()){
							XTrace trace = repairedLog.get(traceIndex);
							trace.get(tracePos).getAttributes().put(ALIGNED_POS, new XAttributeDiscreteImpl(ALIGNED_POS, alignmentPos));
							adjustedEvents++;
						}
						tracePos++;
					}
					alignmentPos++;
				}
			}
//			System.out.println("adjusted "+adjustedEvents+" events!");
			
			// gather frequencies of misalignments
			Table<String, Boolean, Pair<Integer,Set<int[]>>> eventAddedRemovedCounts = extractCountsOfEventsToBeRemovedOrAdded(pnWithMarkings, result);
			List<RepairEntry> repairEntries = getRepairEntries(eventAddedRemovedCounts);
			
			int alignmentMismatchCount = getAlignmentMismatchCount(repairEntries);
			int numToRepair = (int) Math.floor((1.0-trustLog) * alignmentMismatchCount);
			for (RepairEntry entry : repairEntries){
				if (alreadyRepairedEvents >= numToRepair){
					break;
				}
				Set<int[]> positionsRemoved = new HashSet<>();
				for (int[] pos : entry.getPositionsInAlignment()){
					SyncReplayResult repResult = resultList.get(pos[0]);
					SortedSet<Integer> traceIndexes = repResult.getTraceIndex();
					for (Integer trIndex : traceIndexes){
						XTrace trace = repairedLog.get(trIndex);
						String traceId = XConceptExtension.instance().extractName(trace);
//						List<StepTypes> sTypes = repResult.getStepTypes();
//						List<Object> objects = repResult.getNodeInstance();
						// traverse trace and find position where the new event belongs to, or which event needs to be removed.
//						int alignmentPos = 0;
						int tracePos = 0;
						
						for (; tracePos < trace.size(); tracePos++){
							XEvent e = trace.get(tracePos);
							XAttributeDiscrete attribute = (XAttributeDiscrete) (e.getAttributes().containsKey(ALIGNED_POS)?e.getAttributes().get(ALIGNED_POS) : e.getAttributes().get(IMPUTED_POS));
							if (attribute.getValue() >= pos[1]){
								break;
							}
						}
						
//						for (; alignmentPos < pos[1]; alignmentPos++){ // go from 0 to the current pos in the alignment and find the position in the trace
//							XEvent e = trace.get(tracePos);
//							StepTypes sType = sTypes.get(alignmentPos);
//							Object object = objects.get(alignmentPos);
//							long alignedPos = ((XAttributeDiscrete)e.getAttributes().get(ALIGNED_POS)).getValue();
//							switch (sType){
//								case MREAL:
//									// real model move 
//									// check if event is there already:
//									Transition t = (Transition) object;
//									XAttribute attr = e.getAttributes().get(IMPUTED_POS);
//									if (XConceptExtension.instance().extractName(e).equals(t.getLabel()) && attr != null && ((XAttributeDiscrete)attr).getValue() <= alignmentPos ){
//										// we synch these, as it is possible that they match!
//										tracePos++;
//									}
//									break;
//								case L:
//									// real log move
//									// check if event was removed already:
//									XEventClass eClass = (XEventClass) object;
//									if (XConceptExtension.instance().extractName(e).equals(eClass.getId())){
//										tracePos++;
//									}
//									break;
//								case LMGOOD:
//									tracePos++;
//									break;
//								case MINVI: // don't care about these
//								default:
//									break;
//							}
//						}
						if (entry.isAdd()){
							XEvent newEvent = XFactoryRegistry.instance().currentDefault().createEvent();
							XConceptExtension.instance().assignName(newEvent, entry.getEventType());
							XConceptExtension.instance().assignInstance(newEvent, traceId);
							XLifecycleExtension.instance().assignStandardTransition(newEvent, StandardModel.COMPLETE);
							newEvent.getAttributes().put(IMPUTED_POS, new XAttributeDiscreteImpl(IMPUTED_POS, pos[1]));
							trace.add(tracePos, newEvent);
						} else { // remove it!
							trace.remove(tracePos);
						}
						alreadyRepairedEvents++;
					}
					positionsRemoved.add(pos);
					if (alreadyRepairedEvents >= numToRepair){
						break;
					}
				}
				entry.getPositionsInAlignment().removeAll(positionsRemoved);
			}
		} catch (NotYetImplementedException e) {
			e.printStackTrace();
		} catch (InvalidProcessTreeException e) {
			e.printStackTrace();
		}
		System.out.println("Repaired "+alreadyRepairedEvents+" events!");		
		return new Pair<XLog, ProcessTree>(repairedLog, bestTree);
	}

	private int getAlignmentMismatchCount(List<RepairEntry> repairEntries) {
		int count = 0;
		for (RepairEntry entry : repairEntries){
			count += entry.getWeight();
		}
		return count;
	}

	/**
	 * Used to find which misalignments to repair/remove first.
	 * @param eventAddedRemovedCounts
	 * @return a sorted list of {@link RepairEntry} elements sorted by their weight in ascending order. 
	 */
	private List<RepairEntry> getRepairEntries(Table<String, Boolean, Pair<Integer, Set<int[]>>> eventAddedRemovedCounts) {
		List<RepairEntry> repairEntries = new ArrayList<>();
		for (Cell<String, Boolean, Pair<Integer,Set<int[]>>> cell : eventAddedRemovedCounts.cellSet()){
			repairEntries.add(new RepairEntry(cell.getRowKey(), cell.getColumnKey(), cell.getValue().getFirst(), cell.getValue().getSecond()));
		}
		Collections.sort(repairEntries);
		return repairEntries;
	}

//	private void hideInvisibleTransitions(PetrinetWithMarkings pnWithMarkings) {
//		for (Transition t : pnWithMarkings.petrinet.getTransitions()){
//			if (t.getLabel().startsWith("tau ")){
//				t.setInvisible(true);
//			}
//		}
//	}

	protected Table<String, Boolean, Pair<Integer, Set<int[]>>> extractCountsOfEventsToBeRemovedOrAdded(
			PetrinetWithMarkings pnWithMarkings, PNRepResult result) {
		Table<String, Boolean, Pair<Integer, Set<int[]>>> eventAddedRemovedCounts = HashBasedTable.create(); // <- from the log perspective!
		int resId = 0;
		for (SyncReplayResult repResult : result){
			int multiplier = repResult.getTraceIndex().size();
			List<Object> nodeInstances = repResult.getNodeInstance(); // assume array list for O(1) access
			List<StepTypes> stepTypes = repResult.getStepTypes();
			for (int i = 0; i < stepTypes.size(); i++ ){
				StepTypes stepType = stepTypes.get(i);
				Object nodeInstance = nodeInstances.get(i);
				
				String key = null;
				Pair<Integer, Set<int[]>> current = null;
				Boolean add = false; // whether to add a transition with respect to the log!!
				switch (stepType){
				case LMGOOD: // synch, replaced, or swapped
				case MINVI: // invisible model move <- that's fine	
					// fine
					break;
				case L: // log move
					// bad! <- excess entry in the log?
					if (nodeInstance instanceof XEventClass){
						XEventClass eClass = (XEventClass) nodeInstance;
						String id = eClass.getId(); // TODO: extract base label in a more general way!
						if (id.contains("+")){
							key = id.substring(0, id.indexOf("+"));
						}
						current = eventAddedRemovedCounts.get(key, add);
					}
//						nodeInstance;
					break;
				case MREAL: // model move
					add = true;
					// bad! <- missing entry in the log?
					if (nodeInstance instanceof Transition){
						Transition t = (Transition) nodeInstance;
						if (!t.isInvisible()){
							key = t.getLabel();
							current = eventAddedRemovedCounts.get(key, add);
						} else {
							System.out.println("Debug this!");
						}
					} else {
						System.out.println("Debug me!");
					}
					break;
				}
				if (key != null){
					Set<int[]> set = new HashSet<>();
					set.add(new int[]{resId, i});
					
					if (current == null){
						current = new Pair<Integer, Set<int[]>>(multiplier, set);
					} else {
						set.addAll(current.getSecond());
						current = new Pair<>(current.getFirst() + multiplier, set);
					}
					eventAddedRemovedCounts.put(key, add, current);
				}
			}
			resId++;
		}
		return eventAddedRemovedCounts;
	}

	public PetrinetWithMarkings getLastModelPetriNet() {
		return pnWithMarkings;
	}

}
