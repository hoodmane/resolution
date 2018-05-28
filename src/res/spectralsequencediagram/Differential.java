/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

/**
 *
 * @author Hood
 */
public interface Differential extends SseqEdge {
    SseqClass getSource();
    SseqClass getTarget();
    int getPage();
    
    default boolean drawOnPageQ(int page){
        return (page == 0 || this.getPage()==page);
    }
}
