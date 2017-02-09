package org.processmining.plugins.pnml;

import org.processmining.plugins.pnml.elements.graphics.PnmlPosition;

/**
 * The default way of Prom to import PNML files is to scale them by a factor of
 * {@value PnmlPosition#SCALE} (see {@link PnmlPosition#SCALE}) We do not want
 * to change this, so that we are compatible with importing and exporting PNML
 * files
 *
 * @author Andreas Rogge-Solti
 */
public class PNMLParameter {

    /**
     * BVD: Added local constant here as the other constant is no longer visible
     */
    private static final double SCALE = 2.0;

    public static double getScaleForViewInProM() {
        return SCALE;
    }
}
