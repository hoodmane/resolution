package res.algebra;

import res.*;
import java.util.*;

/* The Even Steenrod algebra, as requested by Eva. */
public class EvenSteenrodAlgebra extends SteenrodAlgebra
{

    private final int p;
    private final int q;
    public EvenSteenrodAlgebra(int p) {
        super(p);
        this.p = p;
        this.q = 2*p-2;
    }
    /*
     * Returns all partitions of <n> into P-admissible sequences of largest entry at most <max>.
     * We commented out the part to deal with betas because we are the EvenSteenrodAlgebra.
     */
    @Override
    Iterable<int[]> part_p(int n, int max)
    {
        /* base cases */
        if(n == 0) return Collections.singleton(new int[] {}); /* the trivial partition */
        if(max == 0) return Collections.emptyList(); /* no solutions */

        /* cache */
        Iterable<int[]> ret0 = PART_CACHE.get(part_cache_key(n,max));
        if(ret0 != null) return ret0;

        Collection<int[]> ret = new ArrayList<>();

        for(int i = n * (p-1) / (p * q) * q; i <= max; i += q) {
            /* try P^i */
            for(int[] q0 : part_p(n-i, i/p)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
            /* try BP^i */
            /* We are even steenrod so no betas
            if(i+1 > max) break;
            for(int[] q0 : part_p(n-(i+1), (i+1)/p)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i+1;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
            */
        }

        PART_CACHE.put(part_cache_key(n,max), ret);

        return ret;
    }

}
