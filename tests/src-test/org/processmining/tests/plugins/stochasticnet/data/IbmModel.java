package org.processmining.tests.plugins.stochasticnet.data;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Root;

@Root(name="model", strict=false)
@NamespaceList({
@Namespace(reference="http://www.ibm.com/wbim/bomSchema1.0"),
@Namespace(reference="http://www.ibm.com/wbim/bomSchema1.0", prefix="wbim")
})
//@Namespace(reference="http://www.ibm.com/wbim/bomSchema1.0", prefix="wbim")
public class IbmModel {
	
	@Element(required=false)
	private IbmCatalogs catalogs;
	
//	@Element(required=false)
//	private IbmDataModel dataModel;
	
//	@Element(required=false)
//  private IbmResourceModel resourceModel;
	
	@Element(required=false)
	private IbmProcessModel processModel;

	public IbmCatalogs getCatalogs() {
		return catalogs;
	}

	public void setCatalogs(IbmCatalogs catalogs) {
		this.catalogs = catalogs;
	}

	public IbmProcessModel getProcessModel() {
		return processModel;
	}

	public void setProcessModel(IbmProcessModel processModel) {
		this.processModel = processModel;
	}	
}
