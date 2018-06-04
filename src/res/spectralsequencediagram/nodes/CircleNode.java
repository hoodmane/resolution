/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram.nodes;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;

/**
 *
 * @author Hood
 */
public class CircleNode extends Node {
    double diameter;
    Shape shape;
    
    public CircleNode(){
        this(6);
    }
    
    public CircleNode(double radius){
        this(radius, true);
    }
    
//    public CircleNode(double width, double height){
//        this(width,height, true);
//    }    
    
    public CircleNode(double radius, boolean fillQ){
        this.diameter = radius;
        this.xoffset = radius/2;
        this.yoffset = radius/2;
        this.fillQ = fillQ;
        this.shape = new Ellipse2D.Double(0,0,radius,radius);
    }

    @Override
    Shape baseShape() {
        return shape;
    }
   

    @Override
    public Point2D getBoundaryPoint(AffineTransform t, double x1, double y1) {
        double scaled_diameter = t.getScaleY() * diameter;
        double dx = x1 - x;
        double dy = y1 - y;
        double magnitude = Math.sqrt(dx*dx + dy*dy);
        return new Point2D.Double(scaled_diameter/2 * dx/magnitude + x, scaled_diameter/2 * dy/magnitude + y);
    }

    @Override
    public Node copy() {
        return new CircleNode(diameter, fillQ);
    }
   
}
