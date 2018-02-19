package res.transform;

import java.awt.Color;
import java.awt.Shape;
import res.spectralsequencediagram.SseqClass;
import res.spectralsequencediagram.Structline;


public class BasedLineDecoration<T extends SseqClass> implements Structline {
    public T src;
    public T dest;
    public Color color;

    public BasedLineDecoration(T src, T dest, Color color) {
        this.src = src;
        this.dest = dest;
        this.color = color;
    }

    @Override
    public SseqClass getSource() {
        return src;
    }

    @Override
    public SseqClass getTarget() {
        return dest;
    }

    @Override
    public Shape getShape() {
        //new Line;
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Color getColor() {
        return color;
    }
}

