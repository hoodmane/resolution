package res.algebra;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.Collection;
import res.spectralsequencediagram.*;


public class Generator<T extends GradedElement<T>> implements MultigradedElement<Generator<T>>
{
    public DModSet<T> img;

    public int[] deg;
    public int idx;
    public String extraInfo = "";
    private Collection<Structline> structlines;
    
    public Generator(int[] deg, int idx)
    {
        this.deg = deg;
        this.idx = idx;
        img = new DModSet<>();
    }
            
    @Override public int compareTo(Generator<T> b)
    {
        if(deg.length != b.deg.length)
            return deg.length - b.deg.length;
        for(int i = 0; i < deg.length; i++)
            if(deg[i] != b.deg[i])
                return deg[i] - b.deg[i];
        return idx - b.idx;
    }

    @Override public int[] getDegree()
    {
        return deg;
    }

    @Override public boolean equals(Object o)
    {
        Generator<?> g = (Generator<?>) o;
        for(int i = 0; i < idx; i++)
            if(deg[i] != g.deg[i])
                return false;
        return (idx == g.idx);
    }

    @Override public String extraInfo()
    {
        String ret = extraInfo;
        ret += "Image: "+img;
        return ret;
    }

    public String getName() {
        return String.format("%d-%d-%d",deg[0],deg[1],idx);
    }
    
    public Generator<T> setStructlines(Collection<Structline> sls){
        this.structlines = sls;
        return this;
    }


    @Override
    public int getDeathPage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Shape getShape(int page) {
        return new Ellipse2D.Double( 0, 0, 6, 6);
    }

    @Override
    public Collection<Structline> getStructlines() {
        return structlines;
    }

    @Override
    public Collection<Structline> getDifferentials() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
