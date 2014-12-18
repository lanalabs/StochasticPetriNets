package org.processmining.plugins.stochasticpetrinet.measures;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

public class MultiSetAbstractionLevel extends AbstractionLevel {

	public String getName() {
		return "Multi set abstraction";
	}

	public int[] abstractFrom(int[] rawEncoding) {
		Bag<Integer> intBag = new HashBag<>();
		for (int i : rawEncoding){
			intBag.add(i);
		}
		int[] result = new int[intBag.size()];
		Iterator<Integer> iter = intBag.iterator();
		int index = 0;
		while(iter.hasNext()){
			int i = iter.next();
			result[index++] = i; 
		}
		Arrays.sort(result);
		return result;
	}

}
