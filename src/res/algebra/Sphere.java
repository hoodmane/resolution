package res.algebra;

import java.util.*;

public class Sphere<T extends GradedElement<T>> extends GradedModule<T>
{
    private final Dot<T> d;
    private final T unit;
    private final int p;

    public Sphere(int p, T unit)
    {
        this.p = p;
        this.unit = unit;
        /* XXX should follow the number of extra gradings on alg */
        Generator<T> g = new Generator<>(p,new int[] {-1,0,0}, 0);
        d = new Dot<>(g, unit);
    }

    @Override public Iterable<Dot<T>> basis(int deg) {
        if(deg != 0) return Collections.emptyList();
        else return Collections.singleton(d);
    }

    @Override public DModSet<T> act(Dot<T> o, T sq)
    {
        DModSet<T> ret = new DModSet<>(p);
        if(sq.equals(unit))
            ret.add(d,1);
        return ret;
    }

    @Override
    public int getP() {
        return p;
    }
}
