package res.algebra;

import java.util.*;

public class Sphere<T extends GradedElement<T>> extends GradedModule<T>
{
    private final Dot<T> d;
    T unit;

    public Sphere(T unit)
    {
        this.unit = unit;
        /* XXX should follow the number of extra gradings on alg */
        Generator<T> g = new Generator<>(new int[] {-1,0,0}, 0);
        d = new Dot<>(g, unit);
    }

    @Override public Iterable<Dot<T>> basis(int deg) {
        if(deg != 0) return Collections.emptyList();
        else return Collections.singleton(d);
    }

    @Override public DModSet<T> act(Dot<T> o, T sq)
    {
        DModSet<T> ret = new DModSet<>();
        if(sq.equals(unit))
            ret.add(d,1);
        return ret;
    }
}
