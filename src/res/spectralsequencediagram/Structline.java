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
    Shape getShape(int page);
    
    int getPage();
//    default 
    int getPageMin();
    
    Structline setPage(int page);
    
    default boolean drawOnPageQ(int page){
        return (page <= this.getPage()) && this.getPageMin()<= page;
    }
}
