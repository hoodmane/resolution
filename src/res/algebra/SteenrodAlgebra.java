package res.algebra;

import java.util.*;

/* The Steenrod algebra. */
public class SteenrodAlgebra implements GradedAlgebra<Sq>
{
    private final int p, q;
    private final AlgebraFactory factory;
    private final boolean MICHAEL_MODE, MOTIVIC_GRADING;
    public SteenrodAlgebra(int p) {
        this(p,false,false);
    }
    
    public SteenrodAlgebra(int p,boolean MICHAEL_MODE,boolean MOTIVIC_GRADING) {
        this.p = p;
        this.q = 2*p-2;
        this.MICHAEL_MODE = MICHAEL_MODE;
        this.MOTIVIC_GRADING = MOTIVIC_GRADING;
        factory = AlgebraFactory.getInstance(p,MICHAEL_MODE,MOTIVIC_GRADING);
    }    

    @Override public Iterable<Sq> basis(int n)
    {
        Collection<Sq> ret = new ArrayList<>();
        for(int[] q : part_p(n,n))
            ret.add(factory.Sq(q));

        return ret;
    }

    @Override public ModSet<Sq> times(Sq a, Sq b)
    {
        return a.times(b);
    }

    @Override public Sq unit()
    {
        return factory.UNIT;
    }

    @Override public List<Sq> distinguished()
    {
        ArrayList<Sq> ret = new ArrayList<>();
        ret.add(factory.HOPF[0]);
        ret.add(factory.HOPF[1]);
        ret.add(factory.HOPF[2]);
        return ret;
    }

    @Override public int extraDegrees()
    {
        if(MICHAEL_MODE && p == 2) return 1;
        if(MOTIVIC_GRADING) return 1;
        return 0;
    }

    static final Map<Integer,Iterable<int[]>> PART_CACHE = new TreeMap<Integer,Iterable<int[]>>();
    Integer part_cache_key(int n, int max) {
        return (p << 28) ^ (n << 14) ^ max;
    }
    private static final Iterable<int[]> ZERO = Collections.emptyList(); /* no solutions */
    private static final Iterable<int[]> ONE = Collections.singleton(new int[] {}); /* the trivial partition of zero */

    /*
     * Returns all partitions of <n> into P-admissible sequences of largest entry at most <max>.
     */
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
            if(i+1 > max) break;
            for(int[] q0 : part_p(n-(i+1), (i+1)/p)) {
                int[] q1 = new int[q0.length + 1];
                q1[0] = i+1;
                for(int j = 0; j < q0.length; j++)
                    q1[j+1] = q0[j];
                ret.add(q1);
            }
        }

        PART_CACHE.put(part_cache_key(n,max), ret);

        return ret;
    }

}
