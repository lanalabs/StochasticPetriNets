package org.processmining.plugins.pnml.simple;

import java.util.Map;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementMap;
import org.simpleframework.xml.Root;

@Root(name="toolspecific", strict=false)
public class PNMLToolSpecific {
	
	public static final String STOCHASTIC_ANNOTATION = "StochasticPetriNet";
	public static final String STOCHASTIC_ANNOTATION_VERSION = "0.2";
	
	public static final String TIME_UNIT = "timeUnit";
	public static final String EXECUTION_POLICY = "executionPolicy";
	public static final String PRIORITY = "priority";
	public static final String WEIGHT = "weight";
	public static final String INVISIBLE = "invisible";
	public static final String DISTRIBUTION_TYPE = "distributionType";
	public static final String DISTRIBUTION_PARAMETERS = "distributionParameters";
	public static final String TRAINING_DATA = "trainingData";
	public static final String VALUES_SEPARATOR = ";";
	
	@Attribute
	protected String tool;
	@Attribute
	protected String version;
	
	@ElementMap(entry="property", key="key", attribute=true, inline=true, required=false)
	private Map<String, String> properties;
	
	public String getTool() {
		return tool;
	}
	public void setTool(String tool) {
		this.tool = tool;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public Map<String,String> getProperties(){
		return properties;
	}
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
}
