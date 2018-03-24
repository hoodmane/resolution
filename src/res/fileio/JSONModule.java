package res.fileio;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.ArrayList;
import java.util.*;
import res.algebra.Dot;
import res.algebra.*;


import java.text.ParseException;


public class JSONModule extends GradedModule<Sq> {

   /* We make a Generator<Sq> for each generator and then embed each in a Dot<Sq>.
      A Dot<Sq> is a pair, an element of the steenrod algebra (a Sq) and a Generator<Sq> (which has an integer id and a degree).
      gens:    degree ==> list of generators in that degree.
      actions: a generator ==> a map from integers (presumably representing some P^n) ==> a DModSet which represents an Fp linear combination of 
               elements of the module.
   */
//  Just used for construction
    private final JsonSpecification spec;
    private final Map<String,Dot<Sq>> variableMap;
    
    private final int p;
    private final AlgebraFactory factory;
    private final DModSet<Sq> zero;
            
    private final Map<Integer,ArrayList<Dot<Sq>>> gens;
    private final Map<Dot<Sq>,Map<Integer,DModSet<Sq>>> actions;
    
    /**
     * Get the i-index of a map from Integers to Lists, but if the i-index does not exist, populate
     *  it with a new empty list, set the ith index equal to it, and return it 
     * @param <T> 
     * @param map A map Integer ==> List<T>
     * @param i an index
     * @return either ith index of the map or a new empty list
     */
    static <T> ArrayList<T> getAndInitializeIfNecessary(Map<Integer,ArrayList<T>> map, int i) {
        ArrayList<T> alist = map.get(i);
        if(alist == null) {
            ArrayList<T> ret = new ArrayList<>(1);
            map.put(i,ret);
            return ret;
        }
        else return alist;
    }


    // Match: x1 
    private static final Pattern VARPAT = Pattern.compile("[A-Za-z]+[\\w\\^]*");

    /**
     * Construct a GradedModule by parsing a list of generators and relations. The generators and relations are gotten directly from
     * the user from the configuration JSON file for the run, and the relations are given as a list of strings which need a fair amount of parsing.
     * 
     * @param spec
     * @throws ParseException 
     */
    public JSONModule(JsonSpecification spec) throws ParseException{
        this.spec = spec;
        this.p = spec.p;        
        if(p == 0){
            throw new ParseException("You must specify a prime.",0);
        }
        this.variableMap = new TreeMap<>();
        this.gens = new TreeMap<>();
        this.actions = new TreeMap<>();        
        this.zero = new DModSet<>(p);
        
        Map<String, Integer> generators = spec.generators;
        List<String> relationStrings = spec.relations;
        factory = AlgebraFactory.getInstance(p);
        // Default to the sphere.
        if(generators==null){
            generators = new TreeMap<>();
            generators.put("x", 0);
        }
        
        // Default to no relations (a wedge of spheres)
        if(relationStrings==null){
            relationStrings = new ArrayList<>();
        }
        
        /* Variable name ==> internal generator object */

        // Get the variable list and their degrees
        for(Map.Entry<String, Integer> entry : generators.entrySet()){
            String varName = entry.getKey();
            if(!VARPAT.matcher(varName).matches()){
               throw new ParseException("Invalid variable name \"" + varName + "\"",0);
            }  
            int deg = entry.getValue();
            ArrayList<Dot<Sq>> gensEntry = getAndInitializeIfNecessary(gens,deg);
            Generator<Sq> g = new Generator<>(p,new int[] {-1,deg,0},gensEntry.size());
            Dot<Sq> d = new Dot<>(g, factory.UNIT);
            variableMap.put(varName,d);
            gensEntry.add(d);
            actions.put(d,new TreeMap<>());
        }
        
        VectorEvaluator.VectorEvaluationContext evaluationContext = new VectorEvaluator.VectorEvaluationContext(p);
        
        VectorEvaluator evaluator = new VectorEvaluator(generators);
        for(String relStr : relationStrings){
            if(relStr == null || "".equals(relStr)){
                continue;
            }
            Collection<Relation> relations = evaluator.evaluateRelation(relStr, evaluationContext);
            for(Relation rel : relations){
                DModSet<Sq> outputSet = new DModSet<>(p);
                for(Map.Entry<String, Integer> e : rel.RHS.getVector().entrySet()){
                    outputSet.add(variableMap.get(e.getKey()),e.getValue());
                }                
                actions.get(variableMap.get(rel.inputVariable)).put(rel.operatorDegree,outputSet);   
            }
        }
                      
    }
 
    
    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        ArrayList<Dot<Sq>> alist = gens.get(deg);
        if(alist == null) return Collections.emptySet();
        else return alist;
    }

    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        if(sq.indices.length == 0)
            return new DModSet<>(p,o);
        else if(sq.indices.length == 1) {

            Map<Integer,DModSet<Sq>> map = actions.get(o);
            if(map == null) {
                System.err.println("Foreign dot detected in BrunerNotationModule");
                System.exit(1);
            }
            DModSet<Sq> ret = map.get(sq.indices[0]);
            if(ret == null) return zero; // no defined action indicates zero
            else return ret;

        } else {
            int[] sqcopy = new int[sq.indices.length-1];
            System.arraycopy(sq.indices, 0, sqcopy, 0, sq.indices.length-1);
            Sq next = factory.Sq(sqcopy);
            Sq curr = factory.Sq(sq.indices[sq.indices.length-1]);
            return act(o, curr).times(next,this);
        }
    }

    @Override
    public int getP() {
        return p;
    }

}
