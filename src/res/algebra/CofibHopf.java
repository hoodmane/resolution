package res.algebra;

import res.*;
import java.util.*;

public class CofibHopf extends GradedModule<Sq>
{
    private final int p;
    private final AlgebraFactory factory; 
    private final int i;
    private final Dot<Sq> d1, d2;

    
    public CofibHopf(int p, int i)
    {
        this.p = p;
        factory = AlgebraFactory.getInstance(p);
        this.i = i;
        /* XXX should follow the number of extra gradings on alg */
        Generator<Sq> g = new Generator<>(p,new int[] {-1,0,0}, 0);
        d1 = new Dot<>(g, factory.UNIT);
        d2 = new Dot<>(g, factory.HOPF[i]);
    }

    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        if(deg == 0) return Collections.singleton(d1);
        if(deg == d2.deg[1]) return Collections.singleton(d2);
        return Collections.emptySet();
    }

    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        DModSet<Sq> ret = new DModSet<>(p);
        if(o.deg[1] == d1.deg[1] && sq.equals(factory.UNIT))
            ret.add(d1,1);
        if(o.deg[1] == d2.deg[1] && sq.equals(factory.UNIT))
            ret.add(d2,1);
        if(o.deg[1] == d1.deg[1] && sq.equals(factory.HOPF[i]))
            ret.add(d2,1);
        return ret;
    }

    @Override
    public int getP() {
        return p;
    }
}

