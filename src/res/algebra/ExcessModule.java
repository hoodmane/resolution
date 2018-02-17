package res.algebra;

import res.*;
import java.util.*;

/* The Steenrod algebra, terms of excess at most K. */
public class ExcessModule extends GradedModule<Sq>
{
    private final int K;

    GradedAlgebra<Sq> alg;
    Generator<Sq> g;

    public ExcessModule(int k, GradedAlgebra<Sq> alg)
    {
        /* XXX the extra-grading behavior is probably very broken */ 
        g = new Generator<>(new int[] {-1,0,k}, 0);
        this.alg = alg;
        this.K = k;
    }

    @Override public Iterable<Dot<Sq>> basis(int n)
    {
        Collection<Dot<Sq>> ret = new ArrayList<>();
        for(Sq s : alg.basis(n)) {
            if(s.excess() <= K)
                ret.add(new Dot<>(g, s)); 
        }

        return ret;
    } 

    @Override public DModSet<Sq> act(Dot<Sq> a, Sq b)
    {
        ModSet<Sq> prelim = alg.times(b, a.sq); /* left module */
        DModSet<Sq> ret = new DModSet<>();
        for(Map.Entry<Sq,Integer> ent : prelim.entrySet()) 
            if(ent.getKey().excess() <= K)
                ret.add(new Dot<>(g, ent.getKey()), ent.getValue());
        return ret;
    }

}
