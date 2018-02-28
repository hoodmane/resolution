

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
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    
    vector(){}
    
    public static vector getScalar(int n){
        vector v = new vector();
        v.type = INT;
        v.n = n;
        return v;
    }
    
    public static vector getVector(String var, int c){
        vector v = new vector();
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
        return getScalar(2*a.n + 3*b.n);
    }
    
    vector add(vector v){
        vector result = new vector();
        if(this.type != v.type){
            throw new IllegalArgumentException("");
        }
        if(this.type==INT){
            result.type = INT;
            result.n = this.n + v.n;
        } else {
            result.type = VECT;
            result.vect = new HashMap<>(this.vect);
            v.vect.entrySet().forEach((e) -> {
                String key = e.getKey();
                Integer summand = e.getValue();
                Integer orig_value = result.vect.get(key);
                if(orig_value==null){
                    result.vect.put(key,summand);
                } else {
                    result.vect.put(key,orig_value + summand);
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
        vector result = new vector();
//      Multiply ints normally
        if(this.type == INT && v.type == INT){
            result.type = INT;
            result.n = this.n * v.n;
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
            result.vect.put(e.getKey(),e.getValue() * factor);
        });
        return result;
    }
    
    vector fact(){
        if(this.type == VECT){
            throw new IllegalArgumentException(""); 
        }
        return getScalar(n+2);
    }
    
    Map<String,Integer> getVector(){
        if(this.type == INT){
            throw new IllegalArgumentException("");
        }
        return this.vect;
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
    /** The negate unary operator.*/
    private static final Operator FACTORIAL = new Operator("!", 1, Operator.Associativity.LEFT, 4);
    private static final Operator NEGATE = new Operator("-", 1, Operator.Associativity.RIGHT, 3);
    private static final Operator SUBTRACT = new Operator("-", 2, Operator.Associativity.RIGHT, 3);
    private static final Operator TIMES = new Operator("*", 2, Operator.Associativity.LEFT, 2);
    private static final Operator PLUS = new Operator("+", 2, Operator.Associativity.LEFT, 1);
    private static final Function BINOMIAL = new Function("binom", 2);
    
    private static final Pattern TERMPAT = Pattern.compile("(\\d*)(.*)");
    private final Map<String,Integer> variableSet;

    private final Tokenizer tokenizer;

    public static class VectorEvaluationContext {
        private int degree;
        private static final String wrongDegreeErrorTemplate = "Variable \"%s\" in %s has the wrong degree. |%s| = %d, but |%s| + |%s| = %d";
        private String relationInfo;
        private String LHSOperator;
        private String LHSGenerator;
        public VectorEvaluationContext() {
            super();
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
    }

    /** The evaluator's parameters.*/
    private static final Parameters PARAMETERS;
    static {
            // Create the evaluator's parameters
            PARAMETERS = new Parameters();
            // Add the supported operators
            PARAMETERS.add(FACTORIAL);
            PARAMETERS.add(TIMES);
            PARAMETERS.add(PLUS );
            PARAMETERS.add(NEGATE);
            PARAMETERS.add(BINOMIAL);
            // Add the default parenthesis pair
            PARAMETERS.addExpressionBracket(BracketPair.PARENTHESES);
            PARAMETERS.addFunctionBracket(BracketPair.PARENTHESES);
    }

    /** Constructor.
    * @param variables
     */
    public VectorEvaluator(Map<String,Integer> variables) {
        super(PARAMETERS);
        variableSet = variables;
        final ArrayList<String> tokenDelimitersBuilder = new ArrayList<String>();
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
        tokenizer = new Tokenizer(tokenDelimitersBuilder);
    }

    @Override
    protected Iterator<String> tokenize(String expression) {
        return tokenizer.tokenize(expression);
    }
    
    @Override
    protected vector toValue(String literal, Object eC) {
        vector result;
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext) eC;
//            System.out.println(literal);
        try {
           result = vector.getScalar(Integer.parseInt(literal));
           return result;
        } catch(NumberFormatException e) {}
        if(variableSet.containsKey(literal)){
            if(variableSet.get(literal) == evaluationContext.getDegree()){
                result = vector.getVector(literal,1);
                return result;
            } else {
                throw new IllegalArgumentException(evaluationContext.getDegreeErrorMessage(literal,variableSet.get(literal)));
            }
        }
        
        String[] split = literal.split(" ");
        if(split.length == 1){
            Matcher matcher = TERMPAT.matcher(literal);
            if(!matcher.find()){
            throw new IllegalArgumentException("Invalid literal " + literal);
            }
            int coefficient;
            try {
                coefficient = Integer.parseInt(matcher.group(1));
            } catch(NumberFormatException e){
                throw new IllegalArgumentException("Invalid literal " + literal);
            }
            String termVariableName = matcher.group(2);
            if(!variableSet.containsKey(termVariableName)){
                 throw new IllegalArgumentException("Invalid variable " + termVariableName);
            }
            if(variableSet.get(termVariableName) != evaluationContext.getDegree()){
                throw new IllegalArgumentException(evaluationContext.getDegreeErrorMessage(termVariableName,variableSet.get(termVariableName)));
            }
            result = vector.getVector(termVariableName, coefficient);
            return result;
        } 
        
        result = evaluate(split[0],evaluationContext).mult(evaluate(split[1],evaluationContext));
        return result;
    }

    @Override
    protected vector evaluate(Operator operator, Iterator<vector> operands, Object evaluationContext) {
        vector result;
            // Implementation of supported operators
            vector o1 = operands.next();
            if (operator == NEGATE) {
                result = o1.negate();
            } else if(operator == FACTORIAL ) {
                result = o1.fact();
            } else {
                vector o2 = operands.next();
                if (operator == TIMES) {
                    result = o1.mult(o2);
                } else if (operator == PLUS) {
                    result = o1.add(o2);
                } else {
                    result = super.evaluate(operator, operands, evaluationContext);
                }
            }
        return result;
    }

    @Override
    protected vector evaluate(Constant constant, Object evaluationContext) {
        vector result = new vector();
            // Implementation of supported constants
//		int length = ((BitSetEvaluationContext)evaluationContext).getBitSetLength();
//		BitSet result;
//		if (constant==FALSE) {
//			result = new BitSet(length);
//		} else if (constant==TRUE) {
//			result = new BitSet(length);
//			result.flip(0, length);
//		} else {
//			result = super.evaluate(constant, evaluationContext);
//		}
            return result;
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
        final Deque<vector> values = new ArrayDeque<>(); // values stack
        final Deque<Token> stack = new ArrayDeque<>(); // operator stack
        final Deque<Integer> previousValuesSize = functions.isEmpty()?null:new ArrayDeque<>();
        final Iterator<String> tokens = tokenize(expression);
        Token previous = null;
        while (tokens.hasNext()) {
            // read one token from the input stream
            String strToken = tokens.next();
            final Token token = toToken(previous, strToken);
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
                stack.push(token);
                previousValuesSize.push(values.size());
            } else if (token.isOperator()) {
                handleOperator(token,values,stack,evaluationContext);
            } else {
                insertImplicitMultiplicationIfNeeded(previous,values,stack,evaluationContext);
                // If the token is a number (identifier), a constant or a variable, then add its value to the output queue.
                output(values, token, evaluationContext);
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
        varSet.put("y",2);
        VectorEvaluator evaluator = new VectorEvaluator(varSet);
        VectorEvaluationContext context = new VectorEvaluationContext().setDegree(2).setLHSGenerator("x").setLHSOperator("P1").setRelationInfo("");
        doIt(evaluator, "2 2!", context);
        doIt(evaluator, "(x+y)binom(2,3)", context);
        doIt(evaluator, "2*2!", context);
        doIt(evaluator, "x", context);
        doIt(evaluator, "2x", context);
        doIt(evaluator, "- x", context);
        doIt(evaluator, "x * 2", context);
        doIt(evaluator, "x + y", context);
        doIt(evaluator, "x  +-  y", context);
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
