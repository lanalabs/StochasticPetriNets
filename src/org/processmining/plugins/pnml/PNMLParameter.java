package org.processmining.plugins.pnml;

/**
 * The default way of Prom to import PNML files is to scale them by a factor of {@value PnmlPosition#SCALE} (see {@link PnmlPosition#SCALE}) 
 * We do not want to change this, so that we are compatible with importing and exporting PNML files
 * 
 * @author Andreas Rogge-Solti
 *
 */
public class PNMLParameter {

	public static double getScaleForViewInProM(){
		return 2.0D;
	}
}
