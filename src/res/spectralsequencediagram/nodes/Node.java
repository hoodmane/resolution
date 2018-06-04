/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram.nodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Hood
 */
public abstract class Node {
    double x, y;
    double xoffset, yoffset;
    boolean fillQ;
    public boolean visibleQ;
    Color color;
    
    public Node setColor(Color c){
        color = c;
        return this;
    }
    
    public Color getColor(){
        return color;
    }
    
    public static void drawLine(Graphics2D g, AffineTransform t,Node src, Node dest){
        Point2D p1 = src.getBoundaryPoint(t, dest.getCenter());
        Point2D p2 = dest.getBoundaryPoint(t, src.getCenter());
        g.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
    }
    
    public void draw(Graphics2D g, AffineTransform t){
        g.setColor(color);
        Shape shape = this.getShape(t);
        if(fillQ){
            g.fill(shape);
        } else {
            g.draw(shape);
        }
    }
    
    Shape getShape(AffineTransform t){
        Shape baseShape = baseShape();
        Point2D pt = getCenter();
        pt.setLocation(pt.getX(), pt.getY());
        try {
            t.inverseTransform(pt, pt);
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(RectangleNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        AffineTransform tempTransform = new AffineTransform();
        tempTransform.setToTranslation(pt.getX(), pt.getY());
        tempTransform.preConcatenate(t);
        tempTransform.translate(-xoffset, -yoffset);        
        return tempTransform.createTransformedShape(baseShape);
    }
    
    abstract Shape baseShape();
    
    public Node setPosition(double x, double y){
        this.x = x;
        this.y = y;
        return this;
    }
    
    public Point2D getCenter(){
        return new Point2D.Double(x, y);
    }
    
    public Point2D getBoundaryPoint(AffineTransform t, Point2D p){
        return getBoundaryPoint(t, p.getX(), p.getY());
    }
    public abstract Point2D getBoundaryPoint(AffineTransform t, double x, double y);
    
    public abstract Node copy();
    
}
