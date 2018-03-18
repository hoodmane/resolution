

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Hood
 */
package res.fileio;

import com.hoodiv.javaluator.*;
import java.util.Iterator;
import java.util.Deque;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.stream.Collectors;


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
        private static final String WRONG_DEGREE_ERROR_TEMPLATE = "Variable \"%s\" in relation %shas the wrong degree. |%s| = %d, but |%s| + |%s| = %d";
        private static final String RELATION_INFO_PLACEHOLDER = "$relationInfo";
        private static final int RELATION_WRAP_LENGTH = 0;
        private int degree;
        int p, q;
        private String relationString;
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
        
        public VectorEvaluationContext setRelationInfo(String relnInfo){
    //      If the relation is long, put it on it's own line
            if(relnInfo.length()>RELATION_WRAP_LENGTH){
                relnInfo = "\n    " + relnInfo.toString() + "\n";
            } else {
                relnInfo += " ";
                relnInfo = relnInfo.toString();
            }
            this.relationString = relnInfo;
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
        public IllegalArgumentException getDegreeErrorMessage(String var, int varDegree){
            return new IllegalArgumentException(String.format(WRONG_DEGREE_ERROR_TEMPLATE, var,relationString, var, varDegree, LHSOperator, LHSGenerator, degree));
        }
        
//      If the string has "$relationInfo" in it, replace that with relationString, otherwise append it at end.
        public IllegalArgumentException getErrorMessage(String msg){
            if(msg.contains(RELATION_INFO_PLACEHOLDER)){
                return new IllegalArgumentException(msg.replaceAll(RELATION_INFO_PLACEHOLDER + "\\s*", relationString));
            } else {
                return new IllegalArgumentException((msg + " in relation " + relationString).trim());
            }
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
        myTokenizer = new Tokenizer(
                tokenDelimitersBuilder,
                functions,
                operators,
                constants, 
                functionArgumentSeparator, 
                functionBrackets, 
                expressionBrackets                
        );
    }

    @Override
    protected Collection<Token> tokenize(String expression) {
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
    protected vector toValue(Token literalTok, Object ec) {
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext) ec;
        String literal = literalTok.getLiteral();
//      Split into integer coefficient and remaining actual "literal"
        Matcher matcher = TERMPAT.matcher(literal);
        if(!matcher.find()){
            assert(false); // A pattern of the form ([]*)(.*) matches everything.
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
                throw evaluationContext.getErrorMessage(
                    String.format("Coefficient %s for literal %s in relation $relationInfo is invalid.", coefficientString, literal));
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
                throw evaluationContext.getDegreeErrorMessage(subVarName,variableSet.get(subVarName));
            }
        } else if(evaluationContext.hasIteratorVariableValue(literal)){
//          Case 3:  it's an iterator
            return vector.getScalar(evaluationContext.getIteratorVariableValue(literal) * coefficient,evaluationContext.p);
        } else {
            throw evaluationContext.getErrorMessage("Invalid literal " + literal);
        }
    }

    private static final Pattern OPERATOR_PAT = Pattern.compile("(Sq|bP|P|b)\\^?(.*)");
    
    public Collection<Relation> evaluateRelation(Iterator<Token> toks,Object ec){
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext)ec;        
        PeekingIterator<Token> tokens = PeekingIterator.getInstance(toks);
        if(!tokens.hasNext()){
            return Collections.emptySet();
        }
        Token token = tokens.next();
        String strToken = token.getString();
        if("table".equals(strToken)){
            Collection<Collection<Relation>> results = handleIterator(tokens, 
                    (expression,evalContext) -> evaluateRelation(expression,evalContext),
                    evaluationContext);
            Collection<Relation> flattenedResults = results.stream().flatMap(Collection::stream).collect(Collectors.toList());
            return flattenedResults;
        } 
//      Here we expect an operator symbol
        Matcher matcher = OPERATOR_PAT.matcher(strToken);
        if(!matcher.find()){
            throw evaluationContext.getErrorMessage("Expected an operator (either \"Sq\", \"b\", \"P\", or \"bP\") instead of " + strToken);   
        }

        boolean shouldBeBeta = false;
        boolean isBeta = false;
        String operatorType = matcher.group(1);
        String operatorNumberString = matcher.group(2);
        int operatorDegree = 0;
        if(operatorNumberString.isEmpty()){ // Empty is fine as long as expression is "beta(x)"
            operatorDegree = 0;
            shouldBeBeta = true;
        }  else {      
            try {
               operatorDegree = evaluate(operatorNumberString,evaluationContext).getInt(); 
            } catch (NumberFormatException e) {
              // The string came from matching a regex that says it's a sequence of digits. A nonempty sequence of digits should be valid input to parseInt.
              // I guess you can trigger this error by passing a number larger than Integer.MAX_VALUE, but that sounds unlikely.
              assert(false);
           }        
        }
        String operatorName = matcher.group(1) + operatorDegree;
        switch(operatorType){
            case "Sq" : 
                break;
            case "P": 
                operatorDegree *= evaluationContext.q;
                break;
//              The order of these last two matters for some reason.
            case "bP": 
                operatorDegree *= evaluationContext.q;
                operatorDegree++;
                break;
            case "b":
                operatorDegree = 1;
                isBeta = true;
                break;
            default: // Isn't of the form "Sq", "P" or "b"
                assert(false); // It matched the regex (Sq|P|b|bP)
        }        

        if(isBeta && ! shouldBeBeta){ 
            throw evaluationContext.getErrorMessage(""); // "P" or "Sq" with missing number
        } else if(! isBeta && shouldBeBeta) {
            throw evaluationContext.getErrorMessage("");// "b" with number                 
        }        
        
//      Now we should see a basis vector in parenthesis.
        expectedToken(tokens,"(",evaluationContext);
        tokens.next();
//      Here's the basis vector
        strToken = tokens.next().getString(); 
        String inputVariableName = grabSubscripts(strToken,tokens,evaluationContext).getLiteral();
        expectedToken(tokens,")",evaluationContext);
        tokens.next();  
        
        String inputVariable = substituteVariableName(inputVariableName,evaluationContext);
        if(!variableSet.containsKey(inputVariable)){
            throw evaluationContext.getErrorMessage("Unknown variable " + inputVariableName);
        }
        int inputDegree = variableSet.get(inputVariable);
//      Now the RHS
        expectedToken(tokens,"=",evaluationContext);
        tokens.next();
//      The rest is a vector expression.
        evaluationContext.setDegree(operatorDegree + inputDegree);
        evaluationContext.setLHSOperator(operatorName);
	evaluationContext.setLHSGenerator(inputVariable);
        vector RHS = evaluate(tokens,evaluationContext);
        Relation rel = new Relation();
        rel.inputVariable = inputVariable;
        rel.operatorName = operatorName;
        rel.operatorDegree = operatorDegree;
        rel.RHS = RHS;
        List<Relation> ret = new ArrayList();
        ret.add(rel);
        return ret;
    }
    
    public Collection<Relation> evaluateRelation(String expression, VectorEvaluationContext evaluationContext) {      
        final Collection<Token> tokens = tokenize(expression);
        Collection<Relation> result = evaluateRelation(tokens.iterator(),evaluationContext);
        result.forEach(System.out::println);
        return result;
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
        final Collection<Token> tokens = tokenize(expression);
        return evaluate(tokens.iterator(),evaluationContext);
    }
    
    
    public vector evaluate(Iterator<Token> tokens, Object ec){
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext)ec;
        final Deque<vector> values = new ArrayDeque<>(); // values stack
        final Deque<Token> stack = new ArrayDeque<>(); // operator stack
        final Deque<Integer> previousValuesSize = functions.isEmpty()?null:new ArrayDeque<>();
        Token previous = null;
        while(tokens.hasNext()) {
            // read one token from the input stream
            Token token = tokens.next();
            final String strToken = token.getString();
            if (token.isOpenBracket()) {
                // If the token is a left parenthesis, then push it onto the stack.
                stack.push(token);
                if (previous!=null && previous.isFunction()) {
                    if (!functionBrackets.containsKey(token.getBrackets().getOpen())) {
                        throw token.getError("Invalid bracket after function: "+strToken);
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
                            throw evaluationContext.getErrorMessage("Invalid parenthesis match "+sc.getBrackets().getOpen()+brackets.getClose());
                        }
                    } else {
                        output(values, sc, evaluationContext);
                    }
                }
                if (!openBracketFound) {
                    // If the stack runs out without finding a left parenthesis, then
                    // there are mismatched parentheses.
                    throw evaluationContext.getErrorMessage("Parentheses mismatched");
                }
                if (!stack.isEmpty() && stack.peek().isFunction()) {
                    // If the token at the top of the stack is a function token, pop it
                    // onto the output queue.
                    int argCount = values.size()-previousValuesSize.pop();
                    doFunction(values, stack.pop(), argCount, evaluationContext);
                }
            } else if (token.isFunctionArgumentSeparator()) {
                if (previous==null) {
                    throw evaluationContext.getErrorMessage("expression can't start with a function argument separator");
                }
                // Verify that there was an argument before this separator
                if (previous.isOpenBracket() || previous.isFunctionArgumentSeparator()) {
                    // The cases were operator miss an operand are detected elsewhere.
                    throw evaluationContext.getErrorMessage("argument is missing");
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
                    throw evaluationContext.getErrorMessage("Separator or parentheses mismatched");
                } else {
                    // Verify we are in function scope
                    Token openBracket = stack.pop();
                    Token scopeToken = stack.peek();
                    stack.push(openBracket);
                    if (!scopeToken.isFunction()) {
                        throw evaluationContext.getErrorMessage("Argument separator used outside of function scope");
                    }
                }
            } else if (token.isFunction()) {
                // If the token is a function token, then push it onto the stack.
                insertImplicitMultiplicationIfNeeded(previous,values,stack,evaluationContext);
                if(token.getFunction() == SUM){
                    Collection<vector> results = 
                        handleIterator(tokens, 
                            (expression, context) -> evaluate(expression,context)
                            ,evaluationContext);
                    try {
                        values.push(results.stream().reduce(vector::add).get());
                    } catch(NoSuchElementException e) {
                        throw evaluationContext.getErrorMessage("Empty sum range");
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
                throw evaluationContext.getErrorMessage("Parentheses mismatched");
            }
            output(values, sc, evaluationContext);
        }
        if (values.size()!=1) {
            throw evaluationContext.getErrorMessage("");
        }
        vector pop = values.pop();
        return pop;
    }        
    
    private Token grabSubscripts(String strToken, Iterator<Token> toks, Object ec) {
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext)ec;
        PeekingIterator<Token> tokens  = PeekingIterator.getInstance(toks);
        String t;
        while(strToken.endsWith("_")){
            final StringBuilder str = new StringBuilder(strToken);
            if(!tokens.hasNext()){
                throw evaluationContext.getErrorMessage("");
            }
            t = tokens.next().getString();            
            boolean success = false;
            if(!"{".equals(t)){
                str.append(t);
            } else {
//                str.append(t);
                while(tokens.hasNext()){
                    t = tokens.next().getString();
                    if("}".equals(t)){
                        success = true;
                        break;
                    }
                    str.append(t);                    
                }
            }
            if(!success){
                throw evaluationContext.getErrorMessage("");
            }
            if(tokens.peek().getString().startsWith("_")){
                str.append(tokens.next());
            }
            strToken = str.toString();
        }        
        return tokenizer.toToken(null,strToken);
    }    

    private void expectedToken(PeekingIterator<Token> tokens,String expectedToken,VectorEvaluationContext evaluationContext){
        if(!tokens.hasNext()){
            throw evaluationContext.getErrorMessage("Unexpected end of expression");
        }
        if(!expectedToken.equals(tokens.peek().getString())){
            throw tokens.peek().getError("Expected " + expectedToken + " but instead " + tokens.peek().getString());
        }
    }

    private void expectedToken(PeekingIterator<Token> tokens,Predicate<Token> pred,VectorEvaluationContext evaluationContext){
        if(!tokens.hasNext()){
            throw evaluationContext.getErrorMessage("Unexpected end of expression");
        }
        if(!pred.test(tokens.peek())){
            throw tokens.peek().getError("Unexpected token " + tokens.peek().getString());
        }
    }    
    
    
//  This is just for handleIterator(): table(reln,iterator) and sum(table,iterator) have the same logic, but the function to run after handling is different.
//      
    interface IteratorCallback<T> {
        T doBody(Iterator<Token> expression, Object evaluationContext);
    }
    
    /**
     * After finding a "table" or a "sum", call this to find the variable ranges and iterate over the body of the loop
     * for each value of the variable.
     * @param toks The token stream to find the iterator expression in
     * @param callback Calls this on (expressionBody, updatedEvaluationContext) for each step of the loop
     * @param evaluationContext 
     */
    private <T> Collection<T> handleIterator(Iterator<Token> toks, IteratorCallback<T> callback, Object ec) {
        PeekingIterator<Token> tokens = PeekingIterator.getInstance(toks);
        VectorEvaluationContext evaluationContext = (VectorEvaluationContext)ec;
        int parens = 0;
        ArrayList<Token> expression = new ArrayList<>();
        expectedToken(tokens,"(",evaluationContext);
        tokens.next();
        Token previous = null;
        boolean success = false;
        while (tokens.hasNext()) {
            final Token token = tokens.next();
            final String strToken = token.getString();
            if (token.isOpenBracket()) {
                expression.add(token);
                parens ++;
            } else if (token.isCloseBracket()) {
                expression.add(token);
                parens -- ;
                if(parens<0){
                    throw evaluationContext.getErrorMessage("");
                }
            } else if(token.isFunctionArgumentSeparator()) {
                if(parens > 0){
                    expression.add(token);
                } else {
                    success = true;
                    break;
                }
            } else if(token.isFunction()) {
                expression.add(token);
            } else if(token.isOperator()){
                expression.add(token);
            } else if(token.isLiteral()){
                expression.add(token);
            } else {
                assert(false);
            }
            previous = token;
        }
        if(!success){
            throw evaluationContext.getErrorMessage("");
        }
        
        expectedToken(tokens,"{",evaluationContext);
        tokens.next();
        expectedToken(tokens,Token::isLiteral,evaluationContext);
        String itvar = tokens.next().getLiteral();
        expectedToken(tokens,Token::isFunctionArgumentSeparator,evaluationContext);
        tokens.next();
        ArrayList<Token> argTokens = new ArrayList<>();
        ArrayList<Integer> args = new ArrayList<>(3);
        success = false;
        while (tokens.hasNext()) {
            final Token token = tokens.next();
            final String strToken = token.getString();
            if("}".equals(strToken)){
                args.add(evaluate(argTokens.iterator(),evaluationContext).getInt());
                success = true;
                break;
            } else if(token.isFunctionArgumentSeparator()){
                args.add(evaluate(argTokens.iterator(),evaluationContext).getInt());
                argTokens = new ArrayList<>();
            } else {
                argTokens.add(token);
            }
        }
        if(!success){
            throw evaluationContext.getErrorMessage("");
        }
        expectedToken(tokens,")",evaluationContext);
        tokens.next();
        int min = 1;
        int max;
        int step = 1;        
        switch(args.size()){
            case 0:
                throw evaluationContext.getErrorMessage("");
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
                throw evaluationContext.getErrorMessage("");
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
        doIt(evaluator, "2 x", context);
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
