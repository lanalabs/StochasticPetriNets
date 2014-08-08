package org.processmining.tests.plugins.stochasticnet;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.processmining.models.semantics.petrinet.impl.EfficientTimedMarking;
import org.processmining.models.semantics.petrinet.impl.NormalizedMarkingCache;

public class SemanticsTest {

	@Test
	public void testEfficientMarking() throws Exception {
		ArrayList<Integer>[] timedMarking = new ArrayList[4];
		for (int i = 0; i < timedMarking.length; i++){
			timedMarking[i] = new ArrayList<>();	
		}
		timedMarking[0].add(0);
		timedMarking[1].add(2);
		
		EfficientTimedMarking marking = new EfficientTimedMarking(timedMarking);
		System.out.println(marking.toString());
		marking.pack();
		
		
		NormalizedMarkingCache.getInstance().clearCache();
		
		marking.unpack();
		Assert.assertTrue(marking.equalsMarking(new short[]{1,1,0,0}));
		
		System.out.println(marking.toString());
		
	}
}
