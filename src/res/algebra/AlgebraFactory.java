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
    private static final Map<String,AlgebraFactory> cache = new HashMap();
    private final int p;
    private final int q;
    
    private final boolean MICHAEL_MODE, MOTIVIC_GRADING;
    
    public Sq UNIT;
    public AnElement AnUNIT;
    public Sq[] HOPF;
    
    public static AlgebraFactory getInstance(int p){
        return getInstance(p,false,false);
    }
    
    public static AlgebraFactory getInstance(int p,boolean MICHAEL_MODE, boolean MOTIVIC_GRADING){
        String instanceID = String.valueOf(p)+":"+String.valueOf(MICHAEL_MODE)+":"+String.valueOf(MOTIVIC_GRADING);
        AlgebraFactory factory = cache.get(instanceID);
        if(factory == null){
            factory = new AlgebraFactory(p,false,false);
            cache.put(instanceID,factory);
            int q = factory.q;
            factory.UNIT = new Sq(p,new int[] {});
            factory.HOPF = new Sq[] {
                factory.Sq(1),
                factory.Sq(q),
                factory.Sq(p*q),
                factory.Sq(p*p*q)
            };                   
            factory.AnUNIT = new AnElement(p,factory.ModSet(factory.UNIT), 0);
        }
        return factory;
    }
    
    private AlgebraFactory(int p,boolean MICHAEL_MODE, boolean MOTIVIC_GRADING){
        this.p = p;
        this.q = 2*p-2;
        this.MICHAEL_MODE = MICHAEL_MODE;
        this.MOTIVIC_GRADING = MOTIVIC_GRADING;
    }

    public <T> ModSet<T> ModSet(){
        return new ModSet<>(p);
    }
    
    public <T> ModSet<T> ModSet(T t){
        return new ModSet<>(p,t);
    }
    
    public <T  extends GradedElement<T>> DModSet<T> DModSet() {
        return new DModSet(p);
    }
    
    public <T  extends GradedElement<T>> DModSet<T> DModSet(Dot<T> d) {
        return new DModSet(p,d);
    }
   
    public Sq Sq(int[] q) {
        return new Sq(p, q, MICHAEL_MODE, MOTIVIC_GRADING);
    }
    public Sq Sq(int q) { 
        return new Sq(p,new int[] {q}, MICHAEL_MODE, MOTIVIC_GRADING);
    }
}
