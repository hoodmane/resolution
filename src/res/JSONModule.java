package res;

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
    private final int p;
    private final AlgebraFactory factory;
    private final DModSet<Sq> zero;
    
    private Map<Integer,ArrayList<Dot<Sq>>> gens = new TreeMap<>();
    private Map<Dot<Sq>,Map<Integer,DModSet<Sq>>> actions = new TreeMap<>();
    
    /**
     * Get the i-index of a map from Integers to Lists, but if the i-index does not exist, populate
     *  it with a new empty list, set the ith index equal to it, and return it 
     * @param <T> 
     * @param map A map Integer ==> List<T>
     * @param i an index
     * @return either ith index of the map or a new empty list
     */
    static <T> ArrayList<T> getAndInitializeIfNeeded(Map<Integer,ArrayList<T>> map, int i) {
        ArrayList<T> alist = map.get(i);
        if(alist == null) {
            ArrayList<T> ret = new ArrayList<>(1);
            map.put(i,ret);
            return ret;
        }
        else return alist;
    }


    /* Match: P10(x1) or b(y) or Sq2(x0). Group 1: "P" or "b" or "Sq", Group 2: "10" or "" or "2", Group 3: "x1" or "y" or "x0" */
    private static final Pattern LHSPAT = Pattern.compile("\\s*([A-Za-z]*)(\\d*)\\s*\\(\\s*([\\w\\^]*)\\s*\\)\\s*");
    /* Match: 2 x1 or 2*x1 or 2x1. Group 1: "2" Group 2: "x1" */
    private static final Pattern TERMPAT = Pattern.compile("\\s*(-?\\s*\\d*)\\s*\\*?\\s*([\\w\\^]*)");
    
    private static final Pattern VARPAT = Pattern.compile("[A-Za-z]+[\\w\\^]*");

    /**
     * Construct a GradedModule by parsing a list of generators and relations. The generators and relations are gotten directly from
     * the user from the configuration JSON file for the run, and the relations are given as a list of strings which need a fair amount of parsing.
     * 
     * @param generators A list of generators formatted as a map varName ==> degree 
     * @param relations  A list of relations. Each relation is a string of the form "P^n(
     * @throws ParseException 
     */
    public JSONModule(JsonSpecification spec) throws ParseException{
        this.p = spec.p;        
        this.zero = new DModSet<>(p);
        Map<String, Integer> generators = spec.generators;
        List<String> relations = spec.relations;
        factory = AlgebraFactory.getInstance(p);
        // Default to the sphere.
        if(generators==null){
            generators = new TreeMap<String,Integer>();
            generators.put("x", 0);
        }
        
        // Default to no relations (a wedge of spheres)
        if(relations==null){
            relations = new ArrayList<>();
        }
        
        /* Variable name ==> internal generator object */
        Map<String,Dot<Sq>> variableMap = new TreeMap<>();

        // Get the variable list and their degrees
        for(Map.Entry<String, Integer> entry : generators.entrySet()){
            String varName = entry.getKey();
            if(!VARPAT.matcher(varName).matches()){
               throw new ParseException("Invalid variable name \"" + varName + "\"",0);
            }  
            int deg = entry.getValue();
            ArrayList<Dot<Sq>> gensEntry = getAndInitializeIfNeeded(gens,deg);
            Generator<Sq> g = new Generator<>(p,new int[] {-1,deg,0},gensEntry.size());
            Dot<Sq> d = new Dot<>(g, factory.UNIT);
            variableMap.put(varName,d);
            gensEntry.add(d);
            actions.put(d,new TreeMap<>());
        }
    
        for(final ListIterator<String> it = relations.listIterator(); it.hasNext();){
            String rel = it.next();
            if(rel == null){
                continue; // Handle trailing comma in relation list (GSON inserts a null into the list).
            }
            
            //  Set up some error messages
	    final String relationInfo = "relation number " + it.nextIndex() + ": \"" + rel + "\""; 
	    final String LHSErrorString = "Invalid left hand side in "  + relationInfo + "\n"
               + "Valid relations must have left hand side of the form \"b(<variable>)\", \"Sq<int>(<variable>)\", or \"P<int>(<variable>)\"\n";

	    rel=" " + rel + " "; // Pad with spaces on either end to ensure that trailing "=" causes error.
	    String[] eqSplit = rel.split("=");
            if(eqSplit.length < 2){
               throw new ParseException("No equals sign in "  + relationInfo,0);
            } else if(eqSplit.length > 2) {
               throw new ParseException("Multiple equals signs in "  + relationInfo,0);
            }
            String LHS = eqSplit[0].trim();
            String RHS = eqSplit[1].trim();
	    Matcher LHSmatcher = LHSPAT.matcher(LHS); 
	    if(!LHSmatcher.find()){
              throw new ParseException(LHSErrorString,0);
            }


            boolean shouldBeBeta = false;
            boolean isBeta = false;
            String operatorType = LHSmatcher.group(1);
            String operatorNumberString = LHSmatcher.group(2);
            String operatorName = LHSmatcher.group(1) + LHSmatcher.group(2);
	    int operatorDegree;
            try {
               operatorDegree = Integer.parseInt(operatorNumberString); 
            } catch (NumberFormatException e) {
               if(operatorNumberString.isEmpty()){ // Empty is fine as long as expression is "beta(x)"
                  operatorDegree = 0;
                  shouldBeBeta = true;
               } else {
                  throw new ParseException(LHSErrorString,0); // Invalid number (I'm not sure how to reach this).
               }
            }

            switch(operatorType){
	      case "Sq" : 
                 break;
              case "P":
                 operatorDegree *= spec.q;
                 break;
              case "b":
                 operatorDegree = 1;
                 isBeta = true;
                 break;
              default: // Isn't of the form "Sq", "P" or "b"
                 throw new ParseException(LHSErrorString,0);
             }
             if(isBeta && ! shouldBeBeta){ 
                 throw new ParseException(LHSErrorString,0); // "P" or "Sq" with missing number
             } else if(! isBeta && shouldBeBeta) {
                  throw new ParseException(LHSErrorString,0);// "b" with number                 
             }

             String inputVariableName = LHSmatcher.group(3);
             if(!variableMap.containsKey(inputVariableName))
                 throw new ParseException("Unknown variable \"" + inputVariableName + "\" in " + relationInfo,1);
             Dot<Sq> inputVariable = variableMap.get(LHSmatcher.group(3));
             int inputVariableDegree = inputVariable.deg[1];
             DModSet<Sq> outputSet = new DModSet<>(p);
             // Convert + into +- so that - is part of coefficient. Split on plus.
             for(String term : RHS.replace("-","+-").split("\\+")){
                 term = term.trim();
                 if(term.isEmpty()){ continue;} // This handles expressions that start with - and also ++.
                 Matcher termMatcher = TERMPAT.matcher(term);
                 if(!termMatcher.find()){
                    throw new ParseException("Invalid term " + term + " in " + relationInfo,0);
                 }
                 String ceofficientSring = termMatcher.group(1);
                 String termVariableName = termMatcher.group(2);
                 
                 int coeff;
                 try {
                    if(ceofficientSring.isEmpty()){
                        coeff = 1;
                    } else if("-".equals(ceofficientSring.trim())){
                        coeff = -1;
                    } else {
                        coeff = Integer.parseInt(ceofficientSring.replace(" ",""));  
                    }
                 } catch (NumberFormatException e) {
                    throw new ParseException("Invalid coefficient \"" + ceofficientSring + "\" in term \"" + term + "\" of " + relationInfo,0);
                 }
                 
                 if(!variableMap.containsKey(termVariableName ))
                     throw new ParseException("Unknown variable \"" + termVariableName + "\" in " + relationInfo,1);
                 Dot<Sq> termVariable = variableMap.get(termMatcher.group(2));
                 int termVariableDegree = termVariable.deg[1];
                 if(termVariableDegree != inputVariableDegree + operatorDegree){
                     throw new ParseException(String.format(
                        "Variable \"%s\" in " + relationInfo + " has the wrong degree. |%s| = %d, but |%s| + |%s| = %d",
                        termVariableName, termVariableName, termVariableDegree, operatorName, inputVariableName, operatorDegree + inputVariableDegree
                     ),1);
                 }
                 
                 // Maybe the user used the same variable more than once. If so, combine like terms.
                 if(outputSet.get(termVariable)!=null){
                    coeff += outputSet.get(termVariable);
                 }
                 outputSet.put(termVariable,coeff);
             }
	     actions.get(inputVariable).put(operatorDegree,outputSet);
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
