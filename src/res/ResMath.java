package res;

import java.util.*;

/*
 * little arithmetic functions like modular arithmetic
 */
public final class ResMath
{
    private static final Map<Integer,ResMath> INSTANCES = new HashMap();
    
    private final int[] inverseTable;
    private final int[] factorialTable;

    private final int p;
    private final int q;
    
    public static ResMath getInstance(int p){
        ResMath r = INSTANCES.get(p);
        if(r==null){
            r = new ResMath(p);
            INSTANCES.put(p,r);
        }
        return r;
    }
    
    private ResMath(int p){
        this.BINOM_CACHE = new TreeMap<>();
        this.p = p;
        this.q = 2*p - 2;
        inverseTable = new int[p];
        factorialTable = new int[p];   
        initializeFactorialTable();
    }
  
    private void initializeFactorialTable(){
        factorialTable[0] = 1;
        for(int i = 1; i<p; i++){
            factorialTable[i] = (factorialTable[i-1] * i)%p;
        }
    }
    
    public static int binom_2(int a, int b){
        return ((~a) & b) == 0 ? 1 : 0;
    }
    
    public int positive_modp(int n){
        return Math.floorMod(n, p);
    }
    
    /**
     * Efficiently compute power mod p using binary exponentiation
     * @param b base
     * @param e exponent
     * @return b^e mod p
     */
    public int power_mod(int b,int e){
        int r = 1;
//      b is b^{2^i} mod p
//      if the current bit of e is odd, mutliply b^{2^i} mod p into r.
        while (e > 0){
            if ((e&1) == 1){
                r = (r*b)%p;
            }
            b = (b*b)%p; 
            e >>= 1;            
        }
        return r;
    }
    
    /**
     * Finds the inverse of k mod p.
     * Uses Fermat's little theorem: x^(p-1) = 1 mod p ==> x^(p-2) = x^(-1).
     * @param k an integer
     * @return the inverse of k mod p.
     */
    public int inverse(int k){
        k = Math.floorMod(k, p);
        if(inverseTable[k] == 0){
            inverseTable[k] = power_mod(k,p-2);
        }
        return inverseTable[k];
    }    
    
    /**
     * Compute the p-adic valuation of n! using the formula 
     * v(n!) = [n/p] + [n/p^2] + [n/p^3] + ...
     * @param n 
     * @return The p-adic valuation of n!
     */
    public int factorial_valuation(int n){
        int v = 0;
        int u = p;
        while (u <= n){
            v += n/u;
            u *= p;
        }
        return v;
    }

    private final Map<Integer,Integer> BINOM_CACHE;
    private Integer binom_cache_key(int a, int b) { return (a<<16) | b; }
    

    /**
     * For n,k <= p, compute binomial coefficient using standard formula.
     * We precompute all the factorials for efficiency reasons, and the inverses are memoized.
     * @param n <= p
     * @param k <= p
     * @return n choose k mod p
     */
    int binom_small(int n, int k){
        if(k>n){
            return 0;
        }
        return (factorialTable[n] * inverse((factorialTable[k]*factorialTable[n-k])%p))%p;
    }

    /**
     * Return n choose k. Works by expressing n and k in base p, then computing
     * ni choose ki for the ith digit of n, k  and multiplying them together.
     * Since ni and ki are less than p, the binom_small method can be used.
     * @param n
     * @param k
     * @return n choose k mod p
     */
    public int binom(int n,int k){
        if(p == 2){
            return binom_2(n,k);
        }
        if(n < 0 || k < 0 || k > n)
            return 0;
        else if(n == 0)
            return 1;
        
        int key = binom_cache_key(n,k);
        Integer ret = BINOM_CACHE.get(key);
        if(ret != null) return ret;        
        
        String n_basep = Integer.toString(n,p);
        String k_basep = Integer.toString(k,p);
        int binom = 1;
        for(int i=0;i<n_basep.length();i++){
            int ni = Character.getNumericValue(n_basep.charAt(n_basep.length() - i - 1));
            int ki = 0;
            if(i<k_basep.length()){
                ki = Character.getNumericValue(k_basep.charAt(k_basep.length() - i - 1));
            }
            binom = (binom * binom_small(ni,ki))%p;
        }
        BINOM_CACHE.put(key,binom);
        return binom;
    }

    int factorial(int n) {
        if(n>=p){
            return 0;
        } else if(n<0) {
//            throw new Exception() 
        }
        return factorialTable[n];
    }

    public static void main(String[] args){
        ResMath instance = getInstance(11);
        for(int n=1; n<11; n++){
            System.out.println(n + "^2 = " + instance.power_mod(5,n));
        }
    }

}
