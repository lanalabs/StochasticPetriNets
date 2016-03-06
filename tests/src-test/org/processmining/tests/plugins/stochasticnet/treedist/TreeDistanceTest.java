//package org.processmining.tests.plugins.stochasticnet.treedist;
//
//import java.awt.BorderLayout;
//import java.util.List;
//
//import javax.swing.JComponent;
//import javax.swing.JFrame;
//
//import org.deckfour.xes.model.XLog;
//import org.junit.Test;
//import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
//import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMi;
//import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
//import org.processmining.plugins.logmodeltrust.GeneralizedMinerPlugin;
//import org.processmining.plugins.logmodeltrust.converter.ProcessTreeConverter;
//import org.processmining.plugins.logmodeltrust.miner.GeneralizedMiner;
//import org.processmining.plugins.logmodeltrust.mover.BehaviorScore;
//import org.processmining.plugins.logmodeltrust.mover.TreeMover;
//import org.processmining.plugins.logmodeltrust.visualizer.GeneralizedMinerVisualizer;
//import org.processmining.processtree.Block;
//import org.processmining.processtree.Edge;
//import org.processmining.processtree.ProcessTree;
//import org.processmining.processtree.Task;
//import org.processmining.processtree.impl.AbstractBlock;
//import org.processmining.processtree.impl.AbstractTask;
//import org.processmining.processtree.impl.ProcessTreeImpl;
//import org.processmining.tests.plugins.stochasticnet.TestUtils;
//
//import treedist.EditScore;
//import treedist.LabeledTree;
//import treedist.Mapping;
//import treedist.TreeEditDistance;
//
//public class TreeDistanceTest {
//	class ScoreImpl implements EditScore {
//		private final LabeledTree tree1, tree2;
//
//		public ScoreImpl(LabeledTree tree1, LabeledTree tree2) {
//			this.tree1 = tree1;
//			this.tree2 = tree2;
//		}
//
//		@Override
//		public double replace(int node1, int node2) {
//			if (tree1.getLabel(node1) == tree2.getLabel(node2)) {
//				return 0;
//			} else {
//				return 4;
//			}
//		}
//
//		@Override
//		public double insert(int node2) {
//			return 3;
//		}
//
//		@Override
//		public double delete(int node1) {
//			return 2;
//		}
//	}
//	
//	@Test
//	public void testConversion() throws Exception {
//		ProcessTreeConverter converter = new ProcessTreeConverter();
//		ProcessTree processTree = getTestVisTree();
//		LabeledTree lTree = converter.getLabeledTree(processTree);
//		System.out.println(lTree);
//		System.out.println(converter.getMapping());
//	}
//	
//	@Test
//	public void testDistance() throws Exception {
//		ProcessTreeConverter converter = new ProcessTreeConverter();
//		ProcessTree processTree = getTestVisTree();
//		LabeledTree t1 = converter.getLabeledTree(processTree);
//		ProcessTree processTreeB = getTestVisTree2();
//		LabeledTree t2 = converter.getLabeledTree(processTreeB);
//		
//		Mapping map = new Mapping(t1, t2);
//		TreeEditDistance dist = new TreeEditDistance(new ScoreImpl(t1, t2));
//		System.out.println(dist.calc(t1, t2, map));
//		debugTrace(converter, t1, t2, map);
//	}
//	
//	@Test
//	public void testBehaviorDistance() throws Exception {
//		ProcessTreeConverter converter = new ProcessTreeConverter();
//		ProcessTree processTree = getTestVisTree();
//		LabeledTree t1 = converter.getLabeledTree(processTree);
//		ProcessTree processTreeB = getTestVisTree2();
//		LabeledTree t2 = converter.getLabeledTree(processTreeB);
//		System.out.println(t1);
//		System.out.println(t2);
//		Mapping map = new Mapping(t1, t2);
//		TreeEditDistance dist = new TreeEditDistance(new BehaviorScore(t1, t2));
//		System.out.println(dist.calc(t1, t2, map));
//		debugTrace(converter, t1, t2, map);
//	}
//	
//	@Test
//	public void testSimpleMoveFullTrust() throws Exception {
//		ProcessTree processTree = getTestVisTree();
//		ProcessTree processTreeB = getTestVisTree2();
//		TreeMover mover = new TreeMover(processTree, processTreeB);
//		ProcessTree resultTree = mover.getProcessTreeBasedOnTrust(1);
//		System.out.println(resultTree);
//		System.out.println(processTree);
//	}
//	
//	@Test
//	public void testSimpleMoveZeroTrust() throws Exception {
//		ProcessTree processTree = getTestVisTree();
//		System.out.println("Tree 1: "+processTree);
//		ProcessTree processTreeB = getTestVisTree2();
//		System.out.println("Tree 2: "+processTreeB);
//		TreeMover mover = new TreeMover(processTree, processTreeB);
//		ProcessTree resultTree = mover.getProcessTreeBasedOnTrust(0);
//		System.out.println("Result: "+resultTree);
//	}
//	
//	@Test
//	public void testSimpleMoveHalfTrust() throws Exception {
//		ProcessTree processTree = getTestVisTree();
//		System.out.println("Tree 1: "+processTree);
//		ProcessTree processTreeB = getTestVisTree2();
//		System.out.println("Tree 2: "+processTreeB);
//		TreeMover mover = new TreeMover(processTree, processTreeB);
//		ProcessTree resultTree = mover.getProcessTreeBasedOnTrust(0.5);
//		System.out.println("Result: "+resultTree);
//	}
//
//	@Test
//	public void testMinerHalfTrust() throws Exception {
//		XLog log = TestUtils.loadLog("generated_log_1000.xes");
//		XLog noisyLog = TestUtils.loadLog("generated_simple_log_1000.xes"); 
//		MiningParameters parameters = new MiningParametersIMi();
//		parameters.setNoiseThreshold(0.001f);
//		ProcessTree tree = IMProcessTree.mineProcessTree(log, parameters);  
//		
//		GeneralizedMiner miner = GeneralizedMinerPlugin.mineGeneralLogModel(null, noisyLog, tree);
//		ProcessTree newTree = miner.getProcessTreeBasedOnTrust(0.5);
//		System.out.println(newTree);
//		
//		JFrame frame = new JFrame("Visual comparison");
//		frame.getContentPane().setLayout(new BorderLayout());
//		JComponent comp = GeneralizedMinerVisualizer.visualize(null, miner);
//		frame.getContentPane().add(comp, BorderLayout.CENTER);
//		
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		frame.pack();
//		frame.setVisible(true);
//		Thread.sleep(100000);
//	}
//	
//	
//	public static void debugTrace(ProcessTreeConverter converter, LabeledTree t1, LabeledTree t2, Mapping map) {
//		System.out.println(map);
//		List<Integer> inserts = map.getAllInsertion();
//		System.out.println("inserted:");
//		for (Integer ins : inserts){
//			System.out.println(converter.getLabel(t2.getLabel(ins)));
//		}
//		
//		List<Integer> deletions = map.getAllDeletion();
//		System.out.println("deleted:");
//		for (Integer del : deletions){
//			System.out.println(converter.getLabel(t1.getLabel(del)));
//		}
//		
//		List<int[]> replacements = map.getAllReplacement();
//		System.out.println("replaced:");
//		for (int[] replaced : replacements){
//			System.out.println(converter.getLabel(t1.getLabel(replaced[0]))+" -> "+ converter.getLabel(t2.getLabel(replaced[1])));
//		}
//	}
//	
//	public static ProcessTree getTestVisTree() {
//		ProcessTree tree = new ProcessTreeImpl();
//		Edge edge;
//
//		// And( Xor( F+complete ,  ) , B+complete , A+complete )
//
//		Block and = new AbstractBlock.And("AND");
//		and.setProcessTree(tree);
//		tree.addNode(and);
//		tree.setRoot(and);
//
//		Block xor = new AbstractBlock.Xor("XOR");
//		xor.setProcessTree(tree);
//		tree.addNode(xor);
//
//		edge = and.addChild(xor);
//		xor.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task f = new AbstractTask.Manual("F");
//		f.setProcessTree(tree);
//		tree.addNode(f);
//
//		edge = xor.addChild(f);
//		f.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task tau = new AbstractTask.Automatic("G");
//		tau.setProcessTree(tree);
//		tree.addNode(tau);
//
//		edge = xor.addChild(tau);
//		tau.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task a = new AbstractTask.Manual("B");
//		a.setProcessTree(tree);
//		tree.addNode(a);
//
//		edge = and.addChild(a);
//		a.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task b = new AbstractTask.Manual("A");
//		b.setProcessTree(tree);
//		tree.addNode(b);
//
//		edge = and.addChild(b);
//		b.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		return tree;
//	}
//	public static ProcessTree getTestVisTree2() {
//		ProcessTree tree = new ProcessTreeImpl();
//		Edge edge;
//
//		// Or ( Xor( F+complete ,  ) , B+complete , A+complete )
//
//		Block or = new AbstractBlock.Or("AND");
//		or.setProcessTree(tree);
//		tree.addNode(or);
//		tree.setRoot(or);
//
//		Block xor = new AbstractBlock.Xor("XOR");
//		xor.setProcessTree(tree);
//		tree.addNode(xor);
//
//		edge = or.addChild(xor);
//		xor.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task f = new AbstractTask.Manual("F");
//		f.setProcessTree(tree);
//		tree.addNode(f);
//
//		edge = xor.addChild(f);
//		f.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task tau = new AbstractTask.Automatic("C");
//		tau.setProcessTree(tree);
//		tree.addNode(tau);
//
//		edge = xor.addChild(tau);
//		tau.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task a = new AbstractTask.Manual("A");
//		a.setProcessTree(tree);
//		tree.addNode(a);
//
//		edge = or.addChild(a);
//		a.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		Task b = new AbstractTask.Manual("B");
//		b.setProcessTree(tree);
//		tree.addNode(b);
//
//		edge = or.addChild(b);
//		b.addIncomingEdge(edge);
//		tree.addEdge(edge);
//		
//		Task c = new AbstractTask.Manual("D");
//		c.setProcessTree(tree);
//		tree.addNode(c);
//
//		edge = or.addChild(c);
//		c.addIncomingEdge(edge);
//		tree.addEdge(edge);
//
//		return tree;
//	}
//}
