package org.processmining.plugins.pnml.simple;

import java.util.List;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

@Root(name="net")
public class PNMLNet extends AbstractPNMLElement{
	public static final String PT_NET_CLASS = "http://www.pnml.org/version-2009/grammar/ptnet";
	
	@Attribute(name="type",required=false)
	private String type;
	
	@Attribute(name="id")
	private String id;
	
	@ElementList(inline=true)
	private List<PNMLPage> page;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<PNMLPage> getPage() {
		return page;
	}

	public void setPage(List<PNMLPage> page) {
		this.page = page;
	}
}
