package res;

import java.util.*;

/*
 * little arithmetic functions like modular arithmetic
 */
public final class ResMath
{
    private static final Map<Integer,ResMath> instances = new HashMap();
    
    public int[] inverse;

    private final int p;
    private final int q;
    
    public static ResMath getInstance(int p){
        ResMath r = instances.get(p);
        if(r==null){
            r = new ResMath(p);
            instances.put(p,r);
        }
        return r;
    }
    
    private ResMath(int p){
        this.BINOM_CACHE = new TreeMap<>();
        this.p = p;
        this.q = 2*p - 2;
        this.calcInverses();
    }
    
    public static boolean binom_2(int a, int b)
    {
        return ((~a) & b) == 0;
    }

    private final Map<Integer,Integer> BINOM_CACHE;
    private Integer binom_cache_key(int a, int b) { return (a<<16) | b; }
    public int binom_p(int a, int b)
    {
        Integer s = binom_cache_key(a,b);
        Integer i = BINOM_CACHE.get(s);
        if(i != null) return i;

        int ret;
        if(a < 0 || b < 0 || b > a)
            ret = 0;
        else if(a == 0)
            ret = 1;
        else ret = dmod(binom_p(a-1,b) + binom_p(a-1,b-1));

        BINOM_CACHE.put(s,ret);
        return ret;
    }

    public int dmod(int n)
    {
        return (n + (p << 16)) % p;
    }

    public int floorstep(int n, int m)
    {
        return (n / m) * m;
    }

    void calcInverses()
    {
        inverse = new int[p];
        for(int i = 1; i < p; i++) {
            for(int j = 1; j < p; j++) {
                if((i * j) % p == 1) {
                    inverse[i] = j;
                    break;
                }
            }
        }
    }

}
