package org.processmining.plugins.stochasticpetrinet.measures;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SetAbstractionLevel extends AbstractionLevel{
	
	public String getName(){
		return "Set abstraction";
	}
	
	public int[] abstractFrom(int[] rawEncoding) {
		Set<Integer> intSet = new HashSet<>();
		for (int i : rawEncoding){
			intSet.add(i);
		}
		int[] result = new int[intSet.size()];
		Iterator<Integer> iter = intSet.iterator();
		int index = 0;
		while(iter.hasNext()){
			int i = iter.next();
			result[index++] = i; 
		}
		Arrays.sort(result);
		return result;
	}
}
