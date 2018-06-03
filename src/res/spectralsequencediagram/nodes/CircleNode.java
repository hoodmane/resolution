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
        this.fillQ = fillQ;
        this.shape = new Ellipse2D.Double(0,0,radius,radius);
    }

    @Override
    Shape getShape() {
        return AffineTransform.getTranslateInstance(x - diameter/2, y - diameter/2).createTransformedShape(shape);
    }
   

    @Override
    public Point2D getBoundaryPoint(double x1, double y1) {
        double dx = x1 - x;
        double dy = y1 - y;
        double magnitude = Math.sqrt(dx*dx + dy*dy);
        return new Point2D.Double(diameter/2*dx/magnitude + x,diameter/2*dy/magnitude + y);
    }

    @Override
    public Node copy() {
        return new CircleNode(diameter, fillQ);
    }
   
}
