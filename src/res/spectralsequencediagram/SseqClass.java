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
    Collection<Structline> getDifferentials();
    int getDeathPage();
    
    Shape getShape(int page);
    Color getColor(int page);
    void setColor(int page,Color color);
    String extraInfo();
    String getName();
    
    
}
