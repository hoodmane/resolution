/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram.nodes;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 *
 * @author Hood
 */
public abstract class Node {
    double x,y;
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
    
    public static void drawLine(Graphics2D g,Node src,Node dest){
        Point2D p1 = src.getBoundaryPoint(dest.getCenter());
        Point2D p2 = dest.getBoundaryPoint(src.getCenter());
        g.draw(new Line2D.Double(p1.getX(), p1.getY(), p2.getX(), p2.getY()));
    }
    
    public void draw(Graphics2D g){
        g.setColor(color);
        if(fillQ){
            g.fill(this.getShape());
        } else {
            g.draw(this.getShape());
        }
    }
    
    abstract Shape getShape();
    
    public Node setPosition(double x, double y){
        this.x = x;
        this.y = y;
        return this;
    }
    
    public Point2D getCenter(){
        return new Point2D.Double(x,y);
    }
    
    public Point2D getBoundaryPoint(Point2D p){
        return getBoundaryPoint(p.getX(),p.getY());
    }
    public abstract Point2D getBoundaryPoint(double x, double y);
    
    public abstract Node copy();
    
}
