

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Hood
 */
package res;

import com.fathzer.soft.javaluator.*;
import java.util.Iterator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

class relation { 
    String operatorName;
    int operatorDegree;
    String inputVariable;
    vector RHS;
    @Override
    public String toString(){
        return operatorName + "(" + inputVariable + ")"  + " = " + RHS.toString();
    }
}

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

/** An example of how to implement an evaluator from scratch, working on something more complex 
 * than doubles.
 * <br>This evaluator computes expressions that use boolean sets.
 * <br>A boolean set is a vector of booleans. A true is represented by a one, a false, by a zero.
 * For instance "01" means {false, true}
 * <br>The evaluator uses the BitSet java class to represent these vectors.
 * <br>It supports the logical OR (+), AND (*) and NEGATE (-) operators.
 */
public class VectorEvaluator extends AbstractEvaluator<vector> {
    private static final int INFINITY = 10000; // Big enough.
    /** The negate unary operator.*/
    private static final Operator FACTORIAL = new Operator("!", 1, Operator.Associativity.LEFT, 4);
    private static final Operator NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 3);
    private static final Operator SUBTRACT = new Operator("-", 2, Operator.Associativity.LEFT, 3);
    private static final Operator TIMES = new Operator("*", 2, Operator.Associativity.LEFT, 2);
    private static final Operator PLUS = new Operator("+", 2, Operator.Associativity.LEFT, 1);
    private static final Function BINOMIAL = new Function("binom", 2);
    private static final Function SUM = new Function("sum", 2);

    /** The evaluator's parameters.*/
    private static final Parameters PARAMETERS;
    static {
            // Create the evaluator's parameters
            PARAMETERS = new Parameters();
            // Add the supported operators
            PARAMETERS.add(PLUS);
            PARAMETERS.add(NEGATE);
            PARAMETERS.add(SUBTRACT);
            PARAMETERS.add(TIMES);            
            PARAMETERS.add(FACTORIAL);
            PARAMETERS.add(BINOMIAL);            
            PARAMETERS.add(SUM);
            // Add the default parenthesis pair
            PARAMETERS.addExpressionBracket(BracketPair.PARENTHESES);
            PARAMETERS.addFunctionBracket(BracketPair.PARENTHESES);
    }
    
    private final Map<String,Integer> variableSet;

    private final Tokenizer myTokenizer;

    public static class VectorEvaluationContext implements AbstractVariableSet<vector> {
        private int degree;
        int p, q;
        private static final String wrongDegreeErrorTemplate = "Variable \"%s\" in %s has the wrong degree. |%s| = %d, but |%s| + |%s| = %d";
        private String relationInfo;
        private String LHSOperator;
        private String LHSGenerator;
        private final Map<String,Integer> iteratorVariables;
        public VectorEvaluationContext(int p) {
            super();
            iteratorVariables = new HashMap<>();
            this.p = p;
            q = 2*p-2;
            iteratorVariables.put("p",p);
            iteratorVariables.put("q",q);
        }
        
        
        public VectorEvaluationContext setDegree(int degree){
            this.degree = degree;
            return this;
        }
        
        public VectorEvaluationContext setRelationInfo(String relationInfo){
            this.relationInfo = relationInfo;
            return this;
        }
        
        public VectorEvaluationContext setLHSOperator(String LHSOperator){
            this.LHSOperator = LHSOperator;
            return this;
        }
               
        public VectorEvaluationContext setLHSGenerator(String LHSGenerator){
            this.LHSGenerator = LHSGenerator;
            return this;
        }
        
        public int getDegree() {
            return degree;
        }
        public String getDegreeErrorMessage(String var, int varDegree){
            return String.format(wrongDegreeErrorTemplate, var,relationInfo, var, varDegree, LHSOperator, LHSGenerator, degree);
        }
        
        String substituteVariables(String s){
            for(Map.Entry<String, Integer> e : iteratorVariables.entrySet()){
                s = s.replace(e.getKey(),String.valueOf(e.getValue()));
            }
            return s;
        }
        
        
        void setIteratorVariable(String var, int val){
            iteratorVariables.put(var, val);
        }
        
        @Override
        public vector get(String var){
            if(iteratorVariables.containsKey(var)){
                return vector.getScalar(iteratorVariables.get(var),p);
            } else {
                return null;
            }
        }
        
        int getIteratorVariableValue(String var){
            return iteratorVariables.get(var);
        }

        boolean hasIteratorVariableValue(String var){
            return iteratorVariables.containsKey(var);
        }        

        void removeIteratorVariable(String var){
            iteratorVariables.remove(var);
        }        
    }

    /** Constructor. Takes a map variable => degree
    * @param variables
     */
    public VectorEvaluator(Map<String,Integer> variables) {
        super(PARAMETERS);
        variableSet = variables;
        final ArrayList<String> tokenDelimitersBuilder = new ArrayList<>();
        for(final BracketPair pair : PARAMETERS.getFunctionBrackets()) {
            tokenDelimitersBuilder.add(pair.getOpen());
            tokenDelimitersBuilder.add(pair.getClose());
        }
        for(final BracketPair pair : PARAMETERS.getExpressionBrackets()) {
            tokenDelimitersBuilder.add(pair.getOpen());
            tokenDelimitersBuilder.add(pair.getClose());
        }
        for(Operator ope : PARAMETERS.getOperators()) {
            tokenDelimitersBuilder.add(ope.getSymbol());
        }
        tokenDelimitersBuilder.add(PARAMETERS.getFunctionArgumentSeparator());
        tokenDelimitersBuilder.add(" ");
        tokenDelimitersBuilder.add("=");
        tokenDelimitersBuilder.add(BracketPair.BRACES.getOpen());
        tokenDelimitersBuilder.add(BracketPair.BRACES.getClose());
        myTokenizer = new Tokenizer(tokenDelimitersBuilder);
    }

    @Override
    protected Iterator<String> tokenize(String expression) {
        return myTokenizer.tokenize(expression);
    }
    
    private static final Pattern TERMPAT = Pattern.compile("(\\d*)(.*)");
    
    String substituteVariableName(String varName, Object ec){
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext) ec;
        String[] split = varName.split("_");
        int p = evaluationContext.p;
        evaluationContext.p = INFINITY;
        String ret = split[0] + Arrays.stream(split).skip(1)
                .map(s -> String.valueOf(evaluate(s,evaluationContext).getInt()))
                .collect(Collectors.joining());
        evaluationContext.p = p;
        return ret;
    }
    
    /**
     * There are three cases here:
     *   1. If the literal is of the form <integer> e.g. "123" then we just make that into an "INT" vector
     *   2. If the literal is of the form <variable_name> e.g. "x" then we check if the variable has the right degree and if so make a vector with coefficient one.
     *   3. If the literal is of the form <iterator> e.g. "i" then we make an "INT" vector with the associated value
     * 
     *   In case 2 or 3, we can also handle a literal with a constant in front e.g., 2x or 2i.
     *   In case 2, the variable can have subscripted expressions: x_i_{n-i} becomes x13
     * @param literal A literal
     * @param ec The evaluation context
     * @return the literal converted to a vector
     */
    @Override
    protected vector toValue(String literal, Object ec) {
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext) ec;
//      Split into integer coefficient and remaining actual "literal"
        Matcher matcher = TERMPAT.matcher(literal);
        if(!matcher.find()){
            throw new IllegalArgumentException("Invalid literal " + literal);
        }        
        String coefficientString = matcher.group(1);
        literal = matcher.group(2);
//      Find the coefficient.
        int coefficient;
        if("".equals(coefficientString)){
            coefficient = 1;
        } else {
            try {
                coefficient = Integer.parseInt(coefficientString);
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException("");
            }
        }
        
        if("".equals(literal)){
//         Case 1: it's an integer
           return vector.getScalar(coefficient,evaluationContext.p);
        }
        
//      Calculate actual variable name: all subscripts are to be evaluated into integers and appended.
        String subVarName = substituteVariableName(literal,evaluationContext);
        if(variableSet.containsKey(subVarName)){
//          Case 2: it is a variable "x" or "x_i"            
//          Make sure degree is right
            if(variableSet.get(subVarName) == evaluationContext.getDegree()){
                return vector.getVector(subVarName,coefficient,evaluationContext.p);
            } else {
                throw new IllegalArgumentException(evaluationContext.getDegreeErrorMessage(subVarName,variableSet.get(subVarName)));
            }
        } else if(evaluationContext.hasIteratorVariableValue(literal)){
//          Case 3:  it's an iterator
            return vector.getScalar(evaluationContext.getIteratorVariableValue(literal) * coefficient,evaluationContext.p);
        } else {
            throw new IllegalArgumentException(literal);
        }
    }

    @Override
    protected vector evaluate(Operator operator, Iterator<vector> operands, Object evaluationContext) {
        // Implementation of supported operators
        vector o1 = operands.next();
        if (operator == NEGATE) {
            return o1.negate();
        } else if(operator == FACTORIAL ) {
            return o1.fact();
        } else {
            vector o2 = operands.next();
            if (operator == TIMES) {
                return o1.mult(o2);
            } else if (operator == PLUS) {
                return o1.add(o2);
            } else if(operator == SUBTRACT){ 
                return o1.add(o2.negate());
            } else {
                return super.evaluate(operator, operands, evaluationContext);
            }
        }
    }
    
    @Override
    protected vector evaluate(Function function, Iterator<vector> arguments, Object evaluationContext) {
      if (function == BINOMIAL) {
        // Implements the new function
        return vector.binom(arguments.next(),arguments.next());
      } else {
        // If it's another function, pass it to DoubleEvaluator
        return super.evaluate(function, arguments, evaluationContext);
      }
    }    

    @Override
    public vector evaluate(String expression, Object evaluationContext) {      
        final Iterator<String> tokens = tokenize(expression);
        return evaluate(tokens,evaluationContext);
    }
    
    public Collection<relation> evaluateRelation(String expression, VectorEvaluationContext evaluationContext) {      
        final Iterator<String> tokens = tokenize(expression);
        Collection<relation> result = evaluateRelation(tokens,evaluationContext);
        result.forEach(System.out::println);
        return result;
    }
    
    private static final Pattern OPERATOR_PAT = Pattern.compile("(Sq|bP|P|b)\\^?(.*)");
    public Collection<relation> evaluateRelation(Iterator<String> toks,Object ec){
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext)ec;
        PeekingIterator<String> tokens = PeekingIterator.getInstance(toks);
        if(!tokens.hasNext()){
            throw new IllegalArgumentException("");
        }
        String strToken = tokens.next();
        if("table".equals(strToken)){
            Collection<Collection<relation>> results = handleIterator(tokens, 
                    (expression,evalContext) -> evaluateRelation(expression,evalContext),
                    evaluationContext);
            Collection<relation> flattenedResults = results.stream().flatMap(Collection::stream).collect(Collectors.toList());
            return flattenedResults;
        } 
//      Here we expect an operator symbol
        Matcher matcher = OPERATOR_PAT.matcher(strToken);
        if(!matcher.find()){
            throw new IllegalArgumentException("");   
        }

        boolean shouldBeBeta = false;
        boolean isBeta = false;
        String operatorType = matcher.group(1);
        String operatorNumberString = matcher.group(2);
        int operatorDegree;
        if(operatorNumberString.isEmpty()){ // Empty is fine as long as expression is "beta(x)"
            operatorDegree = 0;
            shouldBeBeta = true;
        }  else {      
            try {
               operatorDegree = evaluate(operatorNumberString,evaluationContext).getInt(); 
            } catch (NumberFormatException e) {
              throw new IllegalArgumentException(""); // Invalid number (I'm not sure how to reach this).
           }        
        }
        String operatorName = matcher.group(1) + operatorDegree;
        switch(operatorType){
            case "Sq" : 
                break;
            case "P": 
                operatorDegree *= evaluationContext.q;
                break;
            case "bP": 
                operatorDegree *= evaluationContext.q;
                operatorDegree++;
                break;
            case "b":
                operatorDegree = 1;
                isBeta = true;
                break;
            default: // Isn't of the form "Sq", "P" or "b"
                throw new IllegalArgumentException("");
        }        

        if(isBeta && ! shouldBeBeta){ 
            throw new IllegalArgumentException(""); // "P" or "Sq" with missing number
        } else if(! isBeta && shouldBeBeta) {
            throw new IllegalArgumentException("");// "b" with number                 
        }        
        
//      Now we should see a basis vector in parenthesis.
        expectedToken(tokens,"(");
        tokens.next();
//      Here's the basis vector
        strToken = tokens.next(); 
        String inputVariableName = grabSubscripts(strToken,tokens,evaluationContext).getLiteral();
        expectedToken(tokens,")");
        tokens.next();  
        
        String inputVariable = substituteVariableName(inputVariableName,evaluationContext);
        if(!variableSet.containsKey(inputVariable)){
            throw new IllegalArgumentException("Unknown variable " + inputVariableName);
        }
        int inputDegree = variableSet.get(inputVariable);
//      Now the RHS
        expectedToken(tokens,"=");
        tokens.next();
//      The rest is a vector expression.
        evaluationContext.setDegree(operatorDegree + inputDegree);
        evaluationContext.setLHSOperator(operatorName);
	evaluationContext.setLHSGenerator(inputVariable);
        vector RHS = evaluate(tokens,evaluationContext);
        relation rel = new relation();
        rel.inputVariable = inputVariable;
        rel.operatorName = operatorName;
        rel.operatorDegree = operatorDegree;
        rel.RHS = RHS;
        List<relation> ret = new ArrayList();
        ret.add(rel);
        return ret;
    }
    
    private Collection<relation> handleTable(Iterator<String> tokens,VectorEvaluationContext evaluationContext){
        return null;
    }
    
    public vector evaluate(Iterator<String> tokens, Object evaluationContext){
        final Deque<vector> values = new ArrayDeque<>(); // values stack
        final Deque<Token> stack = new ArrayDeque<>(); // operator stack
        final Deque<Integer> previousValuesSize = functions.isEmpty()?null:new ArrayDeque<>();
        Token previous = null;
        while (tokens.hasNext()) {
            // read one token from the input stream
            final String strToken = tokens.next();
            Token token = toToken(previous, strToken);
            if (token.isOpenBracket()) {
                // If the token is a left parenthesis, then push it onto the stack.
                stack.push(token);
                if (previous!=null && previous.isFunction()) {
                    if (!functionBrackets.containsKey(token.getBrackets().getOpen())) {
                        throw new IllegalArgumentException("Invalid bracket after function: "+strToken);
                    }
                } else {
                    if (!expressionBrackets.containsKey(token.getBrackets().getOpen())) {
                        throw new IllegalArgumentException("Invalid bracket in expression: "+strToken);
                    }
                }
                //// If the open bracket follows a a literal or a close parenthesis, there is an implicit multiplication.
                insertImplicitMultiplicationIfNeeded(previous,values,stack,evaluationContext);
            } else if (token.isCloseBracket()) {
                if (previous==null) {
                    throw new IllegalArgumentException("expression can't start with a close bracket");
                }
                if (previous.isFunctionArgumentSeparator()) {
                    throw new IllegalArgumentException("argument is missing");
                }
                BracketPair brackets = token.getBrackets();
                // If the token is a right parenthesis:
                boolean openBracketFound = false;
                // Until the token at the top of the stack is a left parenthesis,
                // pop operators off the stack onto the output queue
                while (!stack.isEmpty()) {
                    Token sc = stack.pop();
                    if (sc.isOpenBracket()) {
                        if (sc.getBrackets().equals(brackets)) {
                            openBracketFound = true;
                            break;
                        } else {
                            throw new IllegalArgumentException("Invalid parenthesis match "+sc.getBrackets().getOpen()+brackets.getClose());
                        }
                    } else {
                        output(values, sc, evaluationContext);
                    }
                }
                if (!openBracketFound) {
                    // If the stack runs out without finding a left parenthesis, then
                    // there are mismatched parentheses.
                    throw new IllegalArgumentException("Parentheses mismatched");
                }
                if (!stack.isEmpty() && stack.peek().isFunction()) {
                    // If the token at the top of the stack is a function token, pop it
                    // onto the output queue.
                    int argCount = values.size()-previousValuesSize.pop();
                    doFunction(values, (Function)stack.pop().getFunction(), argCount, evaluationContext);
                }
            } else if (token.isFunctionArgumentSeparator()) {
                if (previous==null) {
                    throw new IllegalArgumentException("expression can't start with a function argument separator");
                }
                // Verify that there was an argument before this separator
                if (previous.isOpenBracket() || previous.isFunctionArgumentSeparator()) {
                    // The cases were operator miss an operand are detected elsewhere.
                    throw new IllegalArgumentException("argument is missing");
                }
                // If the token is a function argument separator
                boolean pe = false;
                while (!stack.isEmpty()) {
                    if (stack.peek().isOpenBracket()) {
                        pe = true;
                        break;
                    } else {
                        // Until the token at the top of the stack is a left parenthesis,
                        // pop operators off the stack onto the output queue.
                        output(values, stack.pop(), evaluationContext);
                    }
                }
                if (!pe) {
                    // If no left parentheses are encountered, either the separator was misplaced
                    // or parentheses were mismatched.
                    throw new IllegalArgumentException("Separator or parentheses mismatched");
                } else {
                    // Verify we are in function scope
                    Token openBracket = stack.pop();
                    Token scopeToken = stack.peek();
                    stack.push(openBracket);
                    if (!scopeToken.isFunction()) {
                        throw new IllegalArgumentException("Argument separator used outside of function scope");
                    }
                }
            } else if (token.isFunction()) {
                // If the token is a function token, then push it onto the stack.
                insertImplicitMultiplicationIfNeeded(previous,values,stack,evaluationContext);
                if(token.getFunction() == SUM){
                    Collection<vector> results = 
                        handleIterator(tokens, 
                            (expression, ec) -> evaluate(expression,ec)
                            ,evaluationContext);
                    try {
                        values.push(results.stream().reduce(vector::add).get());
                    } catch(NoSuchElementException e) {
                        throw new IllegalArgumentException("Empty sum range");
                    }                    
                } else {
                    stack.push(token);
                    previousValuesSize.push(values.size());
                }
            } else if (token.isOperator()) {
                handleOperator(token,values,stack,evaluationContext);
            } else if(token.isLiteral()) {
                if(strToken.endsWith("_")){
                    token = grabSubscripts(strToken,tokens,evaluationContext);
                }
                insertImplicitMultiplicationIfNeeded(previous,values,stack,evaluationContext);
                // If the token is a number (identifier), a constant or a variable, then add its value to the output queue.
                output(values, token, evaluationContext);
            } else {
                assert(false);
            }
            previous = token;
        }
        // When there are no more tokens to read:
        // While there are still operator tokens in the stack:
        while (!stack.isEmpty()) {
            Token sc = stack.pop();
            if (sc.isOpenBracket() || sc.isCloseBracket()) {
                throw new IllegalArgumentException("Parentheses mismatched");
            }
            output(values, sc, evaluationContext);
        }
        if (values.size()!=1) {
            throw new IllegalArgumentException();
        }
        vector pop = values.pop();
        return pop;
    }        
    
    private Token grabSubscripts(String strToken, Iterator<String> toks, Object evaluationContext) {
        PeekingIterator<String> tokens  = PeekingIterator.getInstance(toks);
        String t;
        while(strToken.endsWith("_")){
            final StringBuilder str = new StringBuilder(strToken);
            if(!tokens.hasNext()){
                throw new IllegalArgumentException("");
            }
            t = tokens.next();            
            boolean success = false;
            if(!"{".equals(t)){
                str.append(t);
            } else {
//                str.append(t);
                while(tokens.hasNext()){
                    t = tokens.next();
                    if("}".equals(t)){
                        success = true;
                        break;
                    }
                    str.append(t);                    
                }
            }
            if(!success){
                throw new IllegalArgumentException("");
            }
            if(tokens.peek().startsWith("_")){
                str.append(tokens.next());
            }
            strToken = str.toString();
        }        
        return toToken(null,strToken);
    }    

    private void expectedToken(PeekingIterator<String> tokens,String expectedToken){
        if(!tokens.hasNext()){
            throw new IllegalArgumentException("Unexpected end of expression");
        }
        if(!expectedToken.equals(tokens.peek())){
            throw new IllegalArgumentException("Expected " + expectedToken + " but instead " + tokens.peek());
        }
    }

    private void expectedToken(PeekingIterator<String> tokens,Predicate<Token> pred){
        if(!tokens.hasNext()){
            throw new IllegalArgumentException("Unexpected end of expression");
        }
        if(!pred.test(toToken(null, tokens.peek()))){
            throw new IllegalArgumentException("Unexpected token " + tokens.peek());
        }
    }    
    
    
    interface IteratorCallback<T> {
        T doBody(Iterator<String> expression, Object evaluationContext);
    }
    
    /**
     * After finding a "table" or a "sum", call this to find the variable ranges and iterate over the body of the loop
     * for each value of the variable.
     * @param toks The token stream to find the iterator expression in
     * @param callback Calls this on (expressionBody, updatedEvaluationContext) for each step of the loop
     * @param evaluationContext 
     */
    private <T> Collection<T> handleIterator(Iterator<String> toks, IteratorCallback<T> callback, Object evaluationContext) {
        PeekingIterator<String> tokens = PeekingIterator.getInstance(toks);
        int parens = 0;
        ArrayList<String> expression = new ArrayList<>();
        expectedToken(tokens,"(");
        tokens.next();
        Token previous = null;
        boolean success = false;
        while (tokens.hasNext()) {
            final String strToken = tokens.next();
            final Token token = toToken(previous, strToken);
            if (token.isOpenBracket()) {
                expression.add(strToken);
                parens ++;
            } else if (token.isCloseBracket()) {
                expression.add(strToken);
                parens -- ;
                if(parens<0){
                    throw new IllegalArgumentException("");
                }
            } else if(token.isFunctionArgumentSeparator()) {
                if(parens > 0){
                    expression.add(strToken);
                } else {
                    success = true;
                    break;
                }
            } else if(token.isFunction()) {
                expression.add(strToken);
            } else if(token.isOperator()){
                expression.add(strToken);
            } else if(token.isLiteral()){
                expression.add(strToken);
            } else {
                assert(false);
            }
            previous = token;
        }
        if(!success){
            throw new IllegalArgumentException("");
        }
        
        expectedToken(tokens,"{");
        tokens.next();
        expectedToken(tokens,Token::isLiteral);
        String itvar = tokens.next();
        expectedToken(tokens,Token::isFunctionArgumentSeparator);
        tokens.next();
        ArrayList<String> argTokens = new ArrayList<>();
        ArrayList<Integer> args = new ArrayList<>(3);
        success = false;
        while (tokens.hasNext()) {
            final String strToken = tokens.next();
            final Token token = toToken(null, strToken);
            if("}".equals(strToken)){
                args.add(evaluate(argTokens.iterator(),evaluationContext).getInt());
                success = true;
                break;
            } else if(token.isFunctionArgumentSeparator()){
                args.add(evaluate(argTokens.iterator(),evaluationContext).getInt());
                argTokens = new ArrayList<>();
            } else {
                argTokens.add(strToken);
            }
        }
        if(!success){
            throw new IllegalArgumentException("");
        }
        expectedToken(tokens,")");
        tokens.next();
        int min = 1;
        int max;
        int step = 1;        
        switch(args.size()){
            case 0:
                throw new IllegalArgumentException("");
            case 1:
                max = args.get(0);
                break;
            case 2:
                min = args.get(0);
                max = args.get(1);
                break;
            case 3:
                min = args.get(0);
                max = args.get(1);
                step = args.get(2);
                break;    
            default:
                throw new IllegalArgumentException("");
        }
        
        final VectorEvaluationContext vectEvaluationContext = (VectorEvaluationContext) evaluationContext;
        ArrayList<T> results = new ArrayList((max-min+1)/step);
        for(int i = min; i<=max; i+=step){
            vectEvaluationContext.setIteratorVariable(itvar, i);
            results.add(callback.doBody(expression.iterator(), evaluationContext));
        }
        vectEvaluationContext.removeIteratorVariable(itvar);
        return results;
    }        
    
    

    
    
    //// If there are two literals in a row, or a close parenthesis followed by a literal, there is an implicit multiplication.
    private void insertImplicitMultiplicationIfNeeded(Token previous, Deque<vector> values, Deque<Token> stack, Object evaluationContext) {
        if(previous!=null && (previous.isLiteral() || previous.isCloseBracket() || (previous.isOperator() && previous.getOperator() == FACTORIAL))){
            handleOperator(Token.buildOperator(TIMES),values,stack,evaluationContext);
        }
    }    

    private void handleOperator(Token token,Deque<vector> values,Deque<Token> stack,Object evaluationContext){
        // If the token is an operator, op1, then:
        while (!stack.isEmpty()) {
            Token sc = stack.peek();
            // While there is an operator token, o2, at the top of the stack
            // op1 is left-associative and its precedence is less than or equal
            // to that of op2,
            // or op1 has precedence less than that of op2,
            // Let + and ^ be right associative.
            // Correct transformation from 1^2+3 is 12^3+
            // The differing operator priority decides pop / push
            // If 2 operators have equal priority then associativity decides.
            if (sc.isOperator()
                            && ((token.getAssociativity().equals(Operator.Associativity.LEFT) && (token.getPrecedence() <= sc.getPrecedence())) ||
                                            (token.getPrecedence() < sc.getPrecedence()))) {
                    // Pop o2 off the stack, onto the output queue;
                    output(values, stack.pop(), evaluationContext);
            } else {
                    break;
            }
        }
        // push op1 onto the stack.
        stack.push(token);
    }

    
    
    

    /** A simple program using this evaluator.
      * @param args */
    public static void main(String[] args) {
        Map<String,Integer> varSet = new HashMap<>();
        varSet.put("x",2);
        varSet.put("x1",2);
        varSet.put("x2",2);
        varSet.put("x3",2);
        varSet.put("x4",2);
        varSet.put("x11",2);
        varSet.put("x12",2);
        varSet.put("x21",2);
        varSet.put("x13",2);
        varSet.put("x31",2);
        varSet.put("x22",2);
        varSet.put("y",2);        
        VectorEvaluator evaluator = new VectorEvaluator(varSet);
        VectorEvaluationContext context = new VectorEvaluationContext(7).setDegree(2).setLHSGenerator("x").setLHSOperator("P1").setRelationInfo("");
        doIt(evaluator, "binom(2,0) x", context);
        doIt(evaluator, "5-1-1", context);
        doIt(evaluator, "sum(i x_i_{4-i},{i,3})", context);
        doIt(evaluator, "sum(sum(2i x_i_j,{i,3-j}),{j,2})", context);
        doIt(evaluator, "sum(x_i,{i,1,4,2})", context);
        doIt(evaluator, "binom(5,3) x", context);
        doIt(evaluator, "2 2!", context);
        doIt(evaluator, "(x+y)binom(2,3)", context);
        doIt(evaluator, "2*3!", context);
        doIt(evaluator, "x", context);
        doIt(evaluator, "2x", context);
        doIt(evaluator, "- x", context);
        doIt(evaluator, "x * 2", context);
        doIt(evaluator, "x + y", context);
        doIt(evaluator, "x  -  y", context);
        doIt(evaluator, "(2+1)x", context);
        doIt(evaluator, "2 2!", context);
        doIt(evaluator, "(x+y)2!", context);
        doIt(evaluator, "2(x+y)", context);
        doIt(evaluator, "2x", context);
    }
    
    

    private static void doIt(VectorEvaluator evaluator, String expression, VectorEvaluationContext context) {
            // Evaluate the expression
        vector result  = evaluator.evaluate(expression, context);
//		 Display the result
        System.out.println (expression + " = " + result);
    }
}
