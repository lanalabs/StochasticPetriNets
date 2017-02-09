package org.processmining.plugins.pnml.simple;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name = "position")
public class PNMLPoint {

    @Attribute
    private double x;
    @Attribute
    private double y;

    public PNMLPoint() {
    }

    public PNMLPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }


}
