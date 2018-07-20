package res.transform;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Line2D;
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
    public Shape getShape(int page) {
        return new Line2D.Double(0,0,1,0);        
    }

    @Override
    public Color getColor(int page) {
        return color;
    }

    @Override
    public int getPage() {
        return 0;
    }

    @Override
    public Structline setPage(int page) {
        return this;
    }

    @Override
    public boolean drawOnPageQ(int page) {
        return true;
    }

    @Override
    public int getPageMin() {
        return 0;
    }
}

