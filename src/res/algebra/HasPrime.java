/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.algebra;

import res.ResMath;

/**
 *
 * @author Hood
 */
public interface HasPrime {
    int getP();
    default int getQ(){
        return 2*getP() - 2;
    }
    
    default ResMath getResMath(){
        return ResMath.get(getP());
    }
}
