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

    private Map<Integer,ArrayList<Dot<Sq>>> gens = new TreeMap<>();
    private Map<Dot<Sq>,Map<Integer,DModSet<Sq>>> actions = new TreeMap<>();
 
    /* Get the i-index of a map from Integers to Lists, but if the i-index does not exist, make an empty list, set the ith index equal to it, and return it */
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
    private static final Pattern TERMPAT = Pattern.compile("(\\d*)\\s*\\*?\\s*([\\w\\^]*)");
    
    private static final Pattern VARPAT = Pattern.compile("[A-Za-z]+[\\w\\^]*");

    public JSONModule(Map<String,Integer> generators, List<String> relations) throws ParseException{
        /* Variable name ==> internal generator object */
        Map<String,Dot<Sq>> variableMap = new TreeMap<>();

        for(Map.Entry<String, Integer> entry : generators.entrySet()){
            String varName = entry.getKey();
            if(!VARPAT.matcher(varName).matches()){
               throw new ParseException("Invalid variable name \"" + varName + "\"",0);
            }  
            int deg = entry.getValue();
            ArrayList<Dot<Sq>> gensEntry = getAndInitializeIfNeeded(gens,deg);
            Generator<Sq> g = new Generator<>(new int[] {-1,deg,0},gensEntry.size());
            Dot<Sq> d = new Dot<>(g, Sq.UNIT);
            variableMap.put(varName,d);
            gensEntry.add(d);
            actions.put(d,new TreeMap<>());
        }
    
        for(final ListIterator<String> it = relations.listIterator(); it.hasNext();){
            String rel = it.next();

	    final String relationInfo = "relation number " + it.nextIndex() + ": \"" + rel + "\""; // For errors
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
	    int n;
            try {
               n = Integer.parseInt(LHSmatcher.group(2)); 
            } catch (NumberFormatException e) {
               if(LHSmatcher.group(2).isEmpty()){ // Empty is fine as long as expression is "beta(x)"
                  n=0;
                  shouldBeBeta = true;
               } else {
                  throw new ParseException(LHSErrorString,0); // Invalid number (I'm not sure how to reach this).
               }
            }
            switch(LHSmatcher.group(1)){
	      case "Sq" : 
                 break;
              case "P":
                 n*=Config.Q;
                 break;
              case "b":
                 n=1;
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

             if(!variableMap.containsKey(LHSmatcher.group(3)))
                 throw new ParseException("Unknown variable \"" + LHSmatcher.group(3) + "\" in " + relationInfo,1);
             Dot<Sq> inputVariable = variableMap.get(LHSmatcher.group(3));
             DModSet<Sq> outputSet = new DModSet<>();
             for(String term : RHS.split("\\+")){
                 term = term.trim();
                 Matcher termMatcher = TERMPAT.matcher(term);
                 if(!termMatcher.find()){
                    throw new ParseException("Invalid term " + term + " in " + relationInfo,0);
                 }
                 int coeff;
                 try {
                    coeff = (termMatcher.group(1).isEmpty()) ? 1 : Integer.parseInt(termMatcher.group(1)); // Error if this fails
                 } catch (NumberFormatException e) {
                    throw new ParseException("Invalid coefficient " + termMatcher.group(1) + " in term " + term + " of " + relationInfo,0);
                 }
                 if(!variableMap.containsKey(termMatcher.group(2)))
                     throw new ParseException("Unknown variable \"" + termMatcher.group(2) + "\" in " + relationInfo,1);
                 Dot<Sq> variable = variableMap.get(termMatcher.group(2)); // Error if this fails
                 outputSet.add(variable,coeff);
             }
	     actions.get(inputVariable).put(n,outputSet);
        }
    }
 
    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        ArrayList<Dot<Sq>> alist = gens.get(deg);
        if(alist == null) return Collections.emptySet();
        else return alist;
    }

    DModSet<Sq> zero = new DModSet<>();
    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        if(sq.q.length == 0)
            return new DModSet<>(o);
        else if(sq.q.length == 1) {

            Map<Integer,DModSet<Sq>> map = actions.get(o);
            if(map == null) {
                System.err.println("Foreign dot detected in BrunerNotationModule");
                System.exit(1);
            }
            DModSet<Sq> ret = map.get(sq.q[0]);
            if(ret == null) return zero; // no defined action indicates zero
            else return ret;

        } else {
            int[] sqcopy = new int[sq.q.length-1];
            System.arraycopy(sq.q, 0, sqcopy, 0, sq.q.length-1);
            Sq next = new Sq(sqcopy);
            Sq curr = new Sq(sq.q[sq.q.length-1]);
            return act(o, curr).times(next,this);
        }
    }
}
