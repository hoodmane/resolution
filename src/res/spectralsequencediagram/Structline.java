/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.awt.Color;
import java.awt.Shape;

/**
 *
 * @author Hood
 */
public interface Structline extends SseqEdge {
    SseqClass getSource();
    SseqClass getTarget();
    Shape getShape(int page);
    Color getColor(int page);
    
    int getPage();
    Structline setPage(int page);
    
    default boolean drawOnPageQ(int page){
        return (this.getPage()<=page);
    }
}
