/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.util.Collection;
import res.algebra.PingListener;

/**
 *
 * @author Hood
 */
public interface SpectralSequence {
    public int num_gradings();
    
    public int totalGens();
    
    Collection<SseqClass> getClasses();
    Collection<SseqClass> getClasses(int x, int y);
    Collection<SseqClass> getClasses(int[] p);
    
    Collection<Structline> getStructlines();
    
    int getTMax();
    
    int getState(int x, int y);
    int getState(int[] p);
     /* the ping mechanism is to receive updates when the state of a certain multi-index changes */
    public void addListener(PingListener l);
    public void removeListener(PingListener l);
}
