package res.algebra;

import res.*;
import java.util.*;

public class Sq implements GradedElement<Sq>
{
    private final AlgebraFactory factory;
    public final int p,q;
    private final ResMath resmath;
    
    private final boolean MICHAEL_MODE, MOTIVIC_GRADING;



    public int[] indices; /* Indices of the power operations.
                Mod 2, i indicates Sq^i.
                Mod p>2, 2i(p-1) indicates P^i, 2i(p-1)+1 indicates B P^i. */


    public Sq(int p,int qq) { 
        this(p,new int[] {qq});
    }
        
    public Sq(int p,int[] indices){
        this(p,indices,false,false);
    }
    
    public Sq(int p,int[] indices,boolean MICHAEL_MODE, boolean MOTIVIC_GRADING) {
        this.p = p;
        this.q = 2*p-2;
        this.indices = indices; 
        this.MICHAEL_MODE = MICHAEL_MODE;
        this.MOTIVIC_GRADING = MOTIVIC_GRADING;
        this.resmath = ResMath.getInstance(p);
        factory = AlgebraFactory.getInstance(p,MICHAEL_MODE,MOTIVIC_GRADING);
    }

    
    private static final int[] EMPTY = new int[] {};
    private static final int[] ZERO = new int[] {0};
    private static final int[] ONE = new int[] {1};
    private static final int[][] SINGLETONS = new int[10000][1];
    static {
        for(int i = 0; i < 10000; i++) { SINGLETONS[i][0] = i; }
    }

    /* novikov filtration is 1 if there are no betas. this returns 1 for the
     * identity operation; is this okay? */
    @Override public int[] extraGrading()
    {
        if(MICHAEL_MODE) {
            for(int i : indices)
                if(i % p != 0)
                    return ZERO;
            return ONE;
        } else if(MOTIVIC_GRADING) {
            int tot = 0;
            for(int a : indices) tot += a/2;
            return SINGLETONS[tot];
        } else 
        return EMPTY;
    }

    @Override public int deg()
    {
        int deg = 0;
        for(int i : indices)
            deg += i;
        return deg;
    }

    public int excess()
    {
        if(indices.length == 0) return 0;
        int exc = indices[indices.length-1];
        for(int i = 1; i < indices.length; i++)
            exc += indices[i-1] - p * indices[i];
        return exc;
    }

    public ModSet<Sq> times(Sq o)
    {
        int[] ret = new int[indices.length + o.indices.length];
        System.arraycopy(indices, 0, ret, 0, indices.length);
        System.arraycopy(o.indices, 0, ret, indices.length, o.indices.length);

        if(p == 2 && !MICHAEL_MODE)
            return factory.Sq(ret).resolve_2();
        else
            return factory.Sq(ret).resolve_p();
    }

    private ModSet<Sq> resolve_2()
    {
        ModSet<Sq> ret;

        ret = factory.ModSet();

        for(int i = indices.length - 2; i >= 0; i--) {
            int a = indices[i];
            int b = indices[i+1];

            if(a >= 2 * b)
                continue;

            /* apply Adem relation */
            for(int c = 0; c <= a/2; c++) {

                if(resmath.binom(b - c - 1, a - 2*c) == 0)
                    continue;

                int[] t;
                if(c == 0) {
                    t = Arrays.copyOf(indices, indices.length - 1);
                    for(int k = i+2; k < indices.length; k++)
                        t[k-1] = indices[k];
                    t[i] = a+b-c;
                } else {
                    t = Arrays.copyOf(indices, indices.length);
                    t[i] = a+b-c;
                    t[i+1] = c;
                }

                /* recurse */
                factory.Sq(t).resolve_2().entrySet().forEach((sub) -> {
                    ret.add(sub.getKey(), sub.getValue());
                });
            }

            return ret;
        }

        /* all clear */
        ret.add(this, 1);
        return ret;
    }

    private ModSet<Sq> resolve_p()
    {
        ModSet<Sq> ret;

        ret = factory.ModSet();
        
        /* convenience */
        final int P = p;
        final int Q = 2 * (p - 1);
        final int R = p - 1;

        for(int i = indices.length - 2; i >= 0; i--) {
            int x = indices[i];
            int y = indices[i+1];

            if(x >= p * y)
                continue;

            /* apply Adem relation */
            int a = x / Q;
            int b = y / Q;
            int rx = x % Q;
            int ry = y % Q;

            for(int c = 0; c <= a/p; c++) {

                int sign = ((a ^ c) & 1) == 0  ?  1  :  -1;

//                System.out.printf("adem: x=%d y=%d a=%d b=%d sign=%d\n", x, y, a, b, sign);

                if(ry == 0)
                    resolve_p_add_term(sign*resmath.binom(R*(b-c)-1,a-c*P  ), (a+b-c)*Q+rx, c*Q  , i, ret);
                else {
                    if(rx == 0) {
                        resolve_p_add_term(sign*resmath.binom(R*(b-c)  ,a-c*P  ), (a+b-c)*Q+1, c*Q  , i, ret);
                        resolve_p_add_term(-sign*resmath.binom(R*(b-c)-1,a-c*P-1), (a+b-c)*Q  , c*Q+1, i, ret);
                    } else
                        resolve_p_add_term(-sign*resmath.binom(R*(b-c)-1,a-c*P-1), (a+b-c)*Q+1, c*Q+1, i, ret);
                }
            }

            return ret;
        }

        /* all clear */
        ret.add(this, 1);
        return ret;
    }

    private void resolve_p_add_term(int coeff, int a, int b, int i, ModSet<Sq> ret)
    {
//        System.out.printf("adem_term: coeff=%d a=%d b=%d\n", coeff, a, b);

        coeff = resmath.positive_modp(coeff);
        if(coeff == 0) return; /* save some work... */

        int[] t;
        if(b == 0) {
            t = Arrays.copyOf(indices, indices.length - 1);
            for(int k = i+2; k < indices.length; k++)
                t[k-1] = indices[k];
            t[i] = a;
        } else {
            t = Arrays.copyOf(indices, indices.length);
            t[i] = a;
            t[i+1] = b;
        }

        /* recurse */
        for(Map.Entry<Sq,Integer> sub : factory.Sq(t).resolve_p().entrySet())
            ret.add(sub.getKey(), sub.getValue() * coeff);
    }

    @Override public String toString()
    {
        if(indices.length == 0) return "1";
        String s = "";
        if(p == 2 && ! MICHAEL_MODE) {
            for(int i : indices) s += "Sq"+i;
        } else {
            for(int i : indices) {
                if(i == 1)
                    s += "\u03b2"; /* beta */
                else if(i % q == 0)
                    s += "P"+(i/q);
                else if(i % q == 1)
                    s += "\u03b2P"+(i/q);
                else
                    Main.die_if(true, "bad A_"+p+" element: Sq"+i);
            }
        }
        return s;
    }

    @Override public int hashCode()
    {
        int hash = 0;
        for(int i : indices)
            hash = hash * 27863521 ^ i;
        return hash;
    }

    @Override public boolean equals(Object o)
    {
        Sq s = (Sq)o;
        if(indices.length != s.indices.length)
            return false;
        for(int i = 0; i < indices.length; i++)
            if(indices[i] != s.indices[i])
                return false;
        return true;
    }

    @Override public int compareTo(Sq o)
    {
        if(indices.length != o.indices.length)
            return indices.length - o.indices.length;
        for(int i = 0; i < indices.length; i++)
            if(indices[i] != o.indices[i])
                return indices[i] - o.indices[i];
        return 0;
    }

    @Override
    public int getP() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}

