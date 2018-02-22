/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.algebra;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hood
 */
public class AlgebraFactory {
    private static final Map<Integer,AlgebraFactory> cache = new HashMap();
    private final int p;
    private final int q;
    
    public Sq UNIT;
    /**
     *
     */
    public Sq[] HOPF;
    
    public static AlgebraFactory get(int p){
        AlgebraFactory factory = cache.get(p);
        if(factory == null){
            factory = new AlgebraFactory(p);
            cache.put(p,factory);
            int q = factory.q;
            factory.UNIT = new Sq(p,new int[] {});
            factory.HOPF = new Sq[] {
                factory.Sq(1),
                factory.Sq(q),
                factory.Sq(p*q),
                factory.Sq(p*p*q)
            };                    
        }
        return factory;
    }
    
    private AlgebraFactory(int p){
        this.p = p;
        this.q = 2*p-2;
    }

    public <T> ModSet<T> ModSet(){
        return new ModSet<>(p);
    }
    
    public <T> ModSet<T> ModSet(int p, T t){
        return new ModSet<>(p,t);
    }
   
    public Sq Sq(int[] q) {
        return new Sq(p,q);
    }
    public Sq Sq(int q) { 
        return new Sq(p,q);
    }
}
