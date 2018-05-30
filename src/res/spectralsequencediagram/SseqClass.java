/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates   
 * and open the template in the editor.
 */
package res.spectralsequencediagram;
import java.awt.Color;
import java.awt.Shape;
import java.util.Collection;

/**
 *
 * @author Hood
 */
public interface SseqClass {
    int[] getDegree();
    
    Collection<Structline> getStructlines();
    Collection<Differential> getOutgoingDifferentials();
    int getPage();
    boolean drawOnPageQ(int page);
    
    Shape getShape(int page);
    Color getColor(int page);
    void setColor(int page,Color color);
    
    Style getStyle(int page);
    SseqClass setStyle(Style s);
    
    String extraInfo();
    String getName();
    
    
}
