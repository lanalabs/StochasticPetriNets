package org.processmining.plugins.stochasticpetrinet.enricher;

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.processmining.plugins.petrinet.manifestreplayresult.ManifestEvClassPattern;

public class CollectorCounter extends ReliableInvisibleTransitionPerfCounter{

	protected Map<Integer, List<Double>> firingTimes;
	private ManifestEvClassPattern manifest;
	
	/** for each visited marking, collect the number of times a transition was picked.
	* the transitions are indexed by their encoded id used in the parent class's {@link #getTrans2Idx()}
	* the values in the array are the counts for the different observed next transitions.
	*/  
	protected Map<short[], int[]> markingBasedSelections;
	private short[] currentMarking;   
	
	public CollectorCounter(ManifestEvClassPattern manifest){
		super(null);
		this.manifest = manifest;
		firingTimes = new HashMap<Integer, List<Double>>();
		markingBasedSelections = new HashMap<short[], int[]>();
	}
	
	

	protected void updateMarkingMoveModel(TIntObjectMap<List<Long>> timedPlaces, short[] marking,
			int encTrans) {
		addMarkingTransitionCounter(marking,encTrans);
		
		super.updateMarkingMoveModel(timedPlaces, marking, encTrans);

		// add an entry for model move only:
		if (firingTimes.get(encTrans) == null){
			firingTimes.put(encTrans, new ArrayList<Double>());
		}
		firingTimes.get(encTrans).add(Double.NaN);
	}



	private void addMarkingTransitionCounter(short[] marking, int encTrans) {
		if (!markingBasedSelections.containsKey(marking)){
			markingBasedSelections.put(marking, new int[idx2Trans.length]);
		}
		markingBasedSelections.get(marking)[encTrans]++;
		currentMarking = null;
	}



	protected void updateManifestSojournTime(Long lastTokenTakenTime, long firingTime, int patternIDOfManifest) {
		super.updateManifestSojournTime(lastTokenTakenTime, firingTime, patternIDOfManifest);
		
		// stupid reverse engineering hack to get back the manifest id and its transition id, which we need later.
		int encodedTransitionId = -1;
		int i = 0;
		while (encodedTransitionId == -1){
			int manifestPatternId = manifest.getPatternIDOfManifest(i);
			if (manifestPatternId == patternIDOfManifest){
				encodedTransitionId = manifest.getEncTransOfManifest(i);
			}
			i++;
		} 
		
		// add an entry for synchronous move:
		if (firingTimes.get(encodedTransitionId) == null){
			firingTimes.put(encodedTransitionId, new ArrayList<Double>());
		}
		if (lastTokenTakenTime == null){
			// this happens, when transitions that are not invisible, did not move synchronously before.
			firingTimes.get(encodedTransitionId).add(0.);
		} else {
			firingTimes.get(encodedTransitionId).add((double)(firingTime - lastTokenTakenTime));
		}
		if (currentMarking != null){
			addMarkingTransitionCounter(currentMarking, encodedTransitionId);
		} 
	}

	public List<Double> getPerformanceData(int encodedTransID){
		List<Double> cleanPerformanceData = new LinkedList<Double>();
		if (firingTimes.containsKey(encodedTransID)){
			for (Double d : firingTimes.get(encodedTransID)){
				if (!Double.isNaN(d)){
					cleanPerformanceData.add(d);
				}
			}
		}
		return cleanPerformanceData;
	}
	
	public int getDataCount(int encodedTransID){
		if (firingTimes.containsKey(encodedTransID)){
			return firingTimes.get(encodedTransID).size();
		}
		return 0;
	}
	
	public Map<short[],int[]> getMarkingBasedSelections(){
		return markingBasedSelections;
	}



	protected Long takeTokens(TIntObjectMap<List<Long>> timedPlaces, short[] marking, int encTrans, long takenTime) {
		currentMarking = marking;
		return super.takeTokens(timedPlaces, marking, encTrans, takenTime);
	}
	
	
	
}
