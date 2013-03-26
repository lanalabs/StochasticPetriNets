package org.processmining.tests.plugins.stochasticnet;

import java.io.File;

import junit.framework.Assert;

import org.junit.Test;
import org.processmining.plugins.pnml.simple.PNMLMarking;
import org.processmining.plugins.pnml.simple.PNMLModule;
import org.processmining.plugins.pnml.simple.PNMLNet;
import org.processmining.plugins.pnml.simple.PNMLPage;
import org.processmining.plugins.pnml.simple.PNMLPlace;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.plugins.pnml.simple.PNMLToolSpecific;
import org.processmining.plugins.pnml.simple.PNMLTransition;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

public class PNMLImportTest {

	@Test
	public void testImport() throws Exception{
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/parallel2.pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);
		PNMLNet net = pnml.getNet().get(0);
		Assert.assertEquals("Name should be dc1", "dc1", net.getId());
	}
	
	@Test
	public void testImportSignavio() throws Exception{
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/loopy_free_choice.pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);
		PNMLModule pnmlModule = pnml.getModule().get(0);
		PNMLNet net = pnmlModule.getNets().get(0);
		PNMLMarking finalMarking = pnmlModule.getFinalmarkings().get(0);
		Assert.assertEquals("petrinet", net.getId());
		Assert.assertEquals(1, finalMarking.getPlaces().get(0).getTokens());
		
	}
	
	@Test
	public void testImportPage() throws Exception{
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/page.pnml");

		PNMLPage pnml = serializer.read(PNMLPage.class, source);
		String pageId = pnml.getId();
		Assert.assertEquals("pg1", pageId);
	}
	@Test 
	public void testImportNet() throws Exception{
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/net.pnml");

		PNMLNet pnml = serializer.read(PNMLNet.class, source);
		String netId = pnml.getId();
		Assert.assertEquals("dc1", netId);
	}
	
	
	@Test
	public void testImportToolspecific() throws Exception {
		String ts = "<toolspecific tool=\""+PNMLToolSpecific.STOCHASTIC_ANNOTATION+"\" version=\"0.1\">" +
				"<property key=\"priority\">1</property>" +
				"<property key=\"weight\">1</property>" +
				"<property key=\"distributionType\">IMMEDIATE</property>" +
				"<property key=\"distributionParameters\"></property>" +
				"</toolspecific>";
		Serializer serializer = new Persister();
		PNMLToolSpecific pnmlTs = serializer.read(PNMLToolSpecific.class, ts);
		Assert.assertEquals(PNMLToolSpecific.STOCHASTIC_ANNOTATION,pnmlTs.getTool());
		Assert.assertEquals("1", pnmlTs.getProperties().get("priority"));
	}
	@Test
	public void testImportTransition() throws Exception {
		String transition = "<transition id=\"tr4\">" +
				"<name>" +
				" <text>t_start</text>" +
				"</name>" +
				"<graphics>" +
				" <position x=\"59\" y=\"92\" />" +
				" <dimension x=\"10\" y=\"32\" />" +
				"</graphics>" +
				"<toolspecific tool=\""+PNMLToolSpecific.STOCHASTIC_ANNOTATION+"\" version=\"0.1\">" +
				" <property key=\"priority\">1</property>" +
				" <property key=\"weight\">1</property>" +
				" <property key=\"distributionType\">IMMEDIATE</property>" +
				" <property key=\"distributionParameters\"></property>" +
				"</toolspecific>" +
				"</transition>";
		Serializer serializer = new Persister();
		PNMLTransition pnmlTransition = serializer.read(PNMLTransition.class, transition);
		Assert.assertEquals("t_start",pnmlTransition.getName().getValue());
		Assert.assertEquals(10.0, pnmlTransition.getGraphics().getDimension().getX());
		Assert.assertEquals(92.0, pnmlTransition.getGraphics().getPosition().get(0).getY());
		PNMLToolSpecific ts = pnmlTransition.getToolspecific().get(0);
		Assert.assertEquals("1",ts.getProperties().get("weight"));
	}
	
	@Test
	public void testPlace() throws Exception{
		String place = "<place id=\"pl28\">" +
				"        <graphics>" +
				"          <position x=\"109\" y=\"118\" />" +
				"          <dimension x=\"20\" y=\"20\" />" +
				"        </graphics>" +
				"        <toolspecific tool=\"Yasper\" version=\"2.0.4491.32151\">" +
				"          <tokenCaseSensitive xmlns=\"http://www.yasper.org/specs/toolspec-2.0\">" +
				"            <text>true</text>" +
				"          </tokenCaseSensitive>" +
				"        </toolspecific>" +
				"        <initialMarking>" +
				"          <text>0</text>" +
				"        </initialMarking>" +
				"      </place>";
		Serializer serializer = new Persister();
		PNMLPlace pnmlTransition = serializer.read(PNMLPlace.class, place);
		Assert.assertEquals("pl28",pnmlTransition.getId());
		Assert.assertEquals(new Integer(0), pnmlTransition.getInitialMarking());
	}
}