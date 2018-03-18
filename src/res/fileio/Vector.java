/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.fileio;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import res.ResMath;

/**
 *
 * @author Hood
 */
class vector {
    private enum Type {
        INT,
        VECT
    }
    private static final Type INT = Type.INT;
    private static final Type VECT = Type.VECT;
    Type type;
    Map<String,Integer> vect;
    int n;
    int p;
    final ResMath resMath;
    
    vector(int p){
        this.p = p;
        resMath = ResMath.getInstance(p);
    }
    
    public static vector getScalar(int n, int p){
        vector v = new vector(p);
        v.type = INT;
        v.n = n;
        return v;
    }
    
    public static vector getVector(String var, int c,int p){
        vector v = new vector(p);
        v.type = VECT;
        v.vect = new HashMap<>();
        v.vect.put(var, c);
        return v;
    }
    
    static vector binom(vector a, vector b) {
        if(a.type != INT){
            throw new IllegalArgumentException("");
        }
        if(b.type != INT){
            throw new IllegalArgumentException("");
        }        
        return getScalar(a.resMath.binom(a.n, b.n),a.p);
    }
    
    vector fact(){
        if(this.type == VECT){
            throw new IllegalArgumentException(""); 
        }
        return getScalar(resMath.factorial(n),p);
    }
    
    vector add(vector v){
        vector result = new vector(p);
        if(this.type != v.type){
            throw new IllegalArgumentException("");
        }
        if(this.type==INT){
            result.type = INT;
            result.n = (this.n + v.n)%p;
        } else {
            result.type = VECT;
            result.vect = new HashMap<>(this.vect);
            v.vect.entrySet().forEach((e) -> {
                String key = e.getKey();
                Integer summand = e.getValue();
                Integer orig_value = result.vect.get(key);
                if(orig_value==null){
                    result.vect.put(key,summand%p);
                } else {
                    result.vect.put(key,(orig_value + summand)%p);
                }
            });
        }
        return result;
    }

    vector negate(){
        if(this.type == INT){
            this.n = -this.n;
        } else {
            this.vect.entrySet().forEach((e) -> {
                this.vect.put(e.getKey(),-e.getValue());
            });
        }
        return this;
    }    
    
    vector mult(vector v){
//      Can't multiply two vect
        if(this.type == VECT && v.type == VECT){
            throw new IllegalArgumentException("");
        }
        vector result = new vector(p);
//      Multiply ints normally
        if(this.type == INT && v.type == INT){
            result.type = INT;
            result.n = (this.n * v.n) % p;
            return result;
        } 
//      scale all coefficients of vect
        result.type = VECT;
        int factor;
        if(this.type == INT){
            factor = this.n;
            result.vect = new HashMap<>(v.vect);
        } else {
            factor = v.n;
            result.vect = new HashMap<>(this.vect);
        }
        result.vect.entrySet().forEach(e -> {
            result.vect.put(e.getKey(),(e.getValue() * factor)%p);
        });
        return result;
    }
    
    Map<String,Integer> getVector(){
        if(this.type == INT){
            throw new IllegalArgumentException("");
        }
        return this.vect;
    }
    
    int getInt(){
        if(this.type == VECT){
            throw new IllegalArgumentException("");
        }
        return this.n;
    }
    
    @Override
    public String toString(){
        if(this.type == INT){
            return String.valueOf(this.n);
        } else {
            StringBuilder builder = new StringBuilder();
            Iterator<Map.Entry<String, Integer>> iterator = vect.entrySet().iterator();
            Map.Entry<String, Integer> next = iterator.next();
            builder.append(next.getValue()).append(" ").append(next.getKey());
            while(iterator.hasNext()){
                next = iterator.next();
                builder.append(" + ");
                builder.append(next.getValue()).append(" ").append(next.getKey());
            }
            return builder.toString();
        }
    }
}
