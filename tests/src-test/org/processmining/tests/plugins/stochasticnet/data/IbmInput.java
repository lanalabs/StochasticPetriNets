package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

@Element(name="input")
public class IbmInput extends IbmNode{

	@Attribute(required=false)
	private String associatedData;
	
	@Attribute(required=false)
	private String isOrdered;
	
	@Attribute(required=false)
	private String isUnique;
	
	@Attribute(required=false)
	private String maximum;
	
	@Attribute(required=false)
	private String minimum;
}
