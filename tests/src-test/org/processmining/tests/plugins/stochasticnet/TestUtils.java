package org.processmining.tests.plugins.stochasticnet;

import java.io.File;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * Contains shared functionality of the test cases.
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class TestUtils {

	/**
	 * 
	 * @param name String filename without the suffix ".pnml" tries to load a {@link StochasticNet} from
	 * the tests/testfiles folder of the installation.
	 *  
	 * @return size 2 Object[] containing the {@link StochasticNet} and the initial {@link Marking} of the net.  
	 * @throws Exception
	 */
	public static Object[] loadModel(String name) throws Exception {
		Serializer serializer = new Persister();
		File source = new File("tests/testfiles/"+name+".pnml");

		PNMLRoot pnml = serializer.read(PNMLRoot.class, source);

		StochasticNetDeserializer converter = new StochasticNetDeserializer();
		Object[] netAndMarking = converter.convertToNet(null, pnml, name, false);
		return netAndMarking;
	}
}
