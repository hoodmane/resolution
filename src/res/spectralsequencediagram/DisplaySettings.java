/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

/**
 *
 * @author Hood
 */
public class DisplaySettings {
    public int prime;
    public double xscale, yscale;
    public int xmin, xmax, ymin, ymax;
    public int xgridstep, ygridstep;
    public String gridStyle;
    public int T_max;
    
    public double getXScale(){
        if(xscale==0){
            xscale=1;
        }
        return xscale;
    }
    
    public double getYScale(){
        if(yscale == 0){
            yscale = (prime==0 ? 1 : 2 * prime - 2);
        }        
        return yscale;
    }
}
