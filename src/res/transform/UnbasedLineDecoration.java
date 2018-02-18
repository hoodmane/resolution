package res.transform;

import java.awt.Color;

public class UnbasedLineDecoration<T> {
    public T src;
    public double[] dest;
    public Color color;

    public UnbasedLineDecoration(T src, double[] dest, Color color) {
        this.src = src;
        this.dest = dest;
        this.color = color;
    }
}
