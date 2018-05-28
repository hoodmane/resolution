/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.awt.Color;

/**
 *
 * @author Hood
 */
public interface SseqEdge {
    SseqClass getSource();
    SseqClass getTarget();
    boolean drawOnPageQ(int page);
    Color getColor(int page);
}
