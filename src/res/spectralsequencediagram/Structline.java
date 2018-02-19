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
public interface Structline {
    SseqClass getSource();
    SseqClass getTarget();
    Shape getShape();
    Color getColor();
}
