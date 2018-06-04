/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram.nodes;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Hood
 */

public class RectangleNode extends Node {
    double width;
    double height;
    final Shape shape;
    int fill;
    
    public static final int FILL = 0;
    public static final int NO_FILL = 1;
    public static final int LOWER_LEFT_FILL = 2;
    public static final int LOWER_RIGHT_FILL = 3;
    public static final int UPPER_LEFT_FILL = 4;
    public static final int UPPER_RIGHT_FILL = 5;
    
    public RectangleNode(){
        this(10, 10);
    }

    public RectangleNode(double size){
        this(size, size);
    }
    
    public RectangleNode(double width, double height ){
        this.width = width;
        this.height = height;
        shape = new Rectangle2D.Double(0, 0, width, height);
        this.fill = FILL;
    }
    
    public RectangleNode(double width, double height, int fill ){
        this.width = width;
        this.height = height;
        xoffset = width/2;
        yoffset = height/2;
        this.shape = new Rectangle2D.Double(0, 0, width, height);
        this.fill = fill;
    }

    @Override
    Shape baseShape() {
        return shape;
    }
        
    
    @Override
    public Point2D getBoundaryPoint(AffineTransform t, double x1, double y1) {
        double dx = x1 - x;
        double dy = y1 - y;
        double scaled_height = t.getScaleY() * height;
        double scaled_width  = t.getScaleX() * width;
        double px, py;
        if(dy - dx <= 0){
            if(dy + dx <= 0){
                py = -scaled_height/2;
                px = dx/dy * py;
            } else {
                px = scaled_width/2;
                py = dy/dx * px;
            }
        } else {
            if(dy + dx <= 0){
                px = -scaled_width/2;
                py = dy/dx * px;                
            } else {
                py = scaled_height/2;
                px = dx/dy * py;
            }            
        }
        return new Point2D.Double(px + x, py + y);
    }
    
    @Override
    public void draw(Graphics2D g, AffineTransform t){
        g.setColor(color);
        Shape transformedShape = this.getShape(t);
        if(fill == FILL){
            g.fill(transformedShape);
        } else {
            g.draw(transformedShape);
            if(fill != NO_FILL){
                double xsgn, ysgn;
                switch(fill){
                    case LOWER_LEFT_FILL:
                        xsgn = width/2;
                        ysgn = height/2;
                        break;
                    case LOWER_RIGHT_FILL:
                        xsgn = - width/2;
                        ysgn =   height/2;                    
                        break;
                    case UPPER_LEFT_FILL:
                        xsgn =   width/2;
                        ysgn = - height/2;                    
                        break;
                    case UPPER_RIGHT_FILL:
                        xsgn = - width/2;
                        ysgn = - height/2;                    
                        break;    
                    default:
                        return;
                }

                GeneralPath path = new GeneralPath();
                path.moveTo(x - xsgn, y + ysgn);
                path.lineTo(x + xsgn, y + ysgn);
                path.lineTo(x - xsgn, y - ysgn);
                path.closePath();
                g.fill(t.createTransformedShape(path));            
            }
        }
    }
       

    @Override
    public Node copy() {
        return new RectangleNode(width, height, fill);
    }

    public static class Bar extends RectangleNode {
        @Override 
        public void draw(Graphics2D g, AffineTransform t){
            super.draw(g, t);
            GeneralPath path = new GeneralPath();
            path.moveTo(x - width/2 - 2, y - height/2 - 3);
            path.lineTo(x + width/2 + 2, y - height/2 - 3);
            g.draw(t.createTransformedShape(path));            
        }
        
        public Bar(){
            super(10, 10);
        }

        public Bar(double width, double height ){
            super(width, height);
        }

        public Bar(double width, double height, int fill ){
            super(width, height, fill);
        }
        
        @Override
        public Point2D getBoundaryPoint(AffineTransform t, double x1, double y1) {
            double scaled_height = t.getScaleY() * height;
            double dx = x1 - x;
            double dy = y1 - y;
            if(dy + dx <= 0 && dy - dx <= 0 ){
                double py = -scaled_height/2 - 3;
                double px = dx/dy * py;
                return new Point2D.Double(px + x, py + y);
            } else {
                return super.getBoundaryPoint(t, x1, y1);
            }
        }
        
        @Override
        public Node copy() {
            return new Bar(width, height, fill);
        }        
    }
    
    public static class Dot extends RectangleNode {
        @Override 
        public void draw(Graphics2D g, AffineTransform t){
            super.draw(g, t);
            g.fill(t.createTransformedShape(new Ellipse2D.Double(x - 1.5, y - height/2 - 6, 4, 4)));            
        }
        
        public Dot(){
            super(10, 10);
        }

        public Dot(double width, double height ){
            super(width, height);
        }

        public Dot(double width, double height, int fill ){
            super(width, height, fill);
        }
        
        @Override
        public Node copy() {
            return new Dot(width, height, fill);
        }                
    }    
}    
