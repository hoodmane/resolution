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
    public double xscale, yscale;
    public int xmin, xmax, ymin, ymax;
    public int xgridstep, ygridstep;
    public String gridStyle;
    
    public double getXScale(){
        if(xscale==0){
            xscale=1;
        }
        return xscale;
    }
    
    public double getYScale(){
        if(yscale==0){
            yscale=1;
        }
        return yscale;
    }
    
    public void initialize(){
        if(xscale == 0){
            xscale = 1;
        }
        if(xscale == 0){
            xscale = 1;
        }        
    }
}
