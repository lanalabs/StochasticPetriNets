package org.processmining.plugins.logmodeltrust.heuristic.bp;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.jbpt.bp.BehaviouralProfile;
import org.jbpt.bp.RelSetType;
import org.jbpt.hypergraph.abs.IEntity;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;


public class BehaviourJaccardSimilarity<M, N extends IEntity> {

	private double overlap = 0;
	private double union = 0;
	
	public BehaviourJaccardSimilarity(BehaviouralProfile<M, N> profileA, BehaviouralProfile<M, N> profileB){
		BiMap<Integer, String> aEntities = HashBiMap.create();
		BiMap<Integer, String> bEntities = HashBiMap.create();
		
		List<String> unionEntities = new ArrayList<>();
		addProfileEntities(profileA, aEntities, unionEntities);
		addProfileEntities(profileB, bEntities, unionEntities);
		
		union = FastMath.pow(unionEntities.size(),2); 
		
		overlap = 0;
		
		// go through the matrix:
		for (int i = 0; i < unionEntities.size(); i++){
			for (int j = 0; j < unionEntities.size(); j++){
				String fromString = unionEntities.get(i); 
				String toString = unionEntities.get(j);

				Integer fromA = aEntities.inverse().get(fromString);
				Integer toA = aEntities.inverse().get(toString);
				Integer fromB = bEntities.inverse().get(fromString);
				Integer toB = bEntities.inverse().get(toString);
				if (fromA != null && toA != null && fromB != null && toB != null){
					// if both exist in both sides, we can compare them:
					RelSetType aType = profileA.getRelationForIndex(fromA, toA);
					RelSetType bType = profileB.getRelationForIndex(fromB, toB);
					
					if (aType.equals(bType)){
						overlap += 1; // real match
					} else if (aType.equals(RelSetType.Interleaving) && !bType.equals(RelSetType.Exclusive) || 
							bType.equals(RelSetType.Interleaving) && !aType.equals(RelSetType.Exclusive)){
						// one says sequence, the other allows both directions, that's not so bad.
						overlap += 0.5;
					}
				}
			}
		}
	}

	public double getSimilarity(){
		return overlap / union;
	}
	
	protected void addProfileEntities(BehaviouralProfile<M, N> profileA, BiMap<Integer, String> aEntities,
			List<String> unionEntities) {
		for (N entity : profileA.getEntities()){
			String name = entity.getLabel();
			if (!aEntities.containsValue(name)){
				aEntities.put(aEntities.size(), name);
			}
			if (!unionEntities.contains(name)){
				unionEntities.add(name);
			}
		}
	}
	
}
