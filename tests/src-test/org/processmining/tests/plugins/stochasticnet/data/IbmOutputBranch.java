package org.processmining.tests.plugins.stochasticnet.data;

import java.util.List;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Transient;

@Root(strict=false)
@Element
public class IbmOutputBranch extends IbmNode{
	
	@ElementList(entry="output", inline=true)
    private List<IbmOutput> outputs;
	
	@Element(required=false,name="literalValue")
	@Path("operationalData/probability")
	private String probability;

	public List<IbmOutput> getOutputs() {
		return outputs;
	}

	public void setOutputs(List<IbmOutput> outputs) {
		this.outputs = outputs;
	}

	public String getProbability() {
		return probability;
	}

	public void setProbability(String probability) {
		this.probability = probability;
	}
	
	@Transient
	public String getNodeName(){
		return "outputBranch";
	}
}
