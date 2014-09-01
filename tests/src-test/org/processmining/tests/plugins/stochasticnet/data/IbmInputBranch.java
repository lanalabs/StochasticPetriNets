package org.processmining.tests.plugins.stochasticnet.data;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Transient;

@Element(name="inputBranch")
public class IbmInputBranch extends IbmNode{

	@ElementList(entry="input", inline=true, required=false)
    private List<IbmInput> inputs;
	
	@Transient
	public String getNodeName(){
		return "inputBranch";
	}
}
