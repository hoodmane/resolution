package res.algebra;

import res.*;
import java.util.Map;

/* to work around generic array restrictions */
public class DModSet<T extends GradedElement<T>> extends ModSet<Dot<T>>
{

    /**
     *
     * @param p
     */
    public DModSet(int p) {
        super(p);
    }
    
    public DModSet(int p,Dot<T> d) {
        super(p,d);
    }

    public DModSet<T> times(T sq, GradedAlgebra<T> alg)
    {
        DModSet<T> ret = new DModSet<>(p);
        for(Map.Entry<Dot<T>,Integer> e1 : entrySet()) {
            Dot<T> d = e1.getKey();
            ModSet<T> prod = alg.times(sq, d.sq);
            for(Map.Entry<T,Integer> e2 : prod.entrySet())
                ret.add(new Dot<>(d.base, e2.getKey()), e1.getValue() * e2.getValue());
        }
        return ret;
    }
    
    public DModSet<T> times(T sq, GradedModule<T> module)
    {
        DModSet<T> ret = new DModSet<>(p);
        for(Map.Entry<Dot<T>,Integer> e1 : entrySet()) {
            Dot<T> d = e1.getKey();
            DModSet<T> prod = module.act(d, sq);
            for(Map.Entry<Dot<T>,Integer> e2 : prod.entrySet())
                ret.add(e2.getKey(), e1.getValue() * e2.getValue());
        }
        return ret;
    }

    public DModSet<T> dscaled(int scale)
    {
        DModSet<T> ret = new DModSet<>(p);
        if(resmath.positive_modp(scale) == 0)
            return ret; // scaling by 0
        for(Map.Entry<Dot<T>,Integer> e1 : entrySet())
            ret.put(e1.getKey(), resmath.positive_modp(e1.getValue() * scale));
        return ret;

    }
}
