package res.algebra;

import java.util.*;

public class Dot<T extends GradedElement<T>> implements Comparable<Dot<T>>
{
    /* kernel basis vector */
    public T sq;
    public Generator<T> base;
    public int[] deg;

    public Dot(Generator<T> base, T sq) { /* square dot */
        this.base = base;
        this.sq = sq;
        deg = Arrays.copyOf(base.deg, base.deg.length);
        deg[1] += sq.deg();
        int[] ex = sq.extraGrading();
        for(int i = 0; i < ex.length; i++)
            deg[i+2] += ex[i];
    }
    
    @Override public String toString()
    {
        String ret = sq.toString() + "(" +(base.deg[1] - base.deg[0])+ "," +  base.deg[0]  + ";" + base.idx + ")";
        if(deg.length > 2) {
            ret += "(";
            for(int g = 2; g < deg.length; g++) {
                if(g != 2) ret += ",";
                ret += deg[g];
            }
            ret += ")";
        }
        return ret;
    }
    
    @Override public boolean equals(Object o)
    {
        Dot<?> d = (Dot<?>) o;
        return (base.equals(d.base) && d.sq.equals(sq));
    }

    @Override public int compareTo(Dot<T> o)
    {
        /* XXX tweak this for performance */
        int c = base.compareTo(o.base);
        if(c != 0) return c;
        return sq.compareTo(o.sq);
    }
}

