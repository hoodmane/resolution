/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.fileio;

/**
 *
 * @author Hood
 */
public class Relation { 
    String operatorName;
    int operatorDegree;
    String inputVariable;
    vector RHS;
    @Override
    public String toString(){
        return operatorName + "(" + inputVariable + ")"  + " = " + RHS.toString();
    }
}
