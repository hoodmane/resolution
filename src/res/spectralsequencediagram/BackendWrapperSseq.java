/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.awt.Color;
import java.util.Collection;
import java.util.stream.Collectors;
import res.algebra.PingListener;
import res.backend.Backend;

/**
 *
 * @author Hood
 */
public class BackendWrapperSseq implements SpectralSequence {

    final Color[] colors;
    private final Backend back;
    private final int p;
    
    private static Color[] intervalColors(float angleFrom, float angleTo, int n) {
        float angleRange = angleTo - angleFrom;
        float stepAngle = angleRange / n;
        Color[] colorList = new Color[n];
        colorList[0] = Color.BLACK;
        for (int i = 0; i < n-1; i++) {
            float angle = angleFrom + i*stepAngle;
            colorList[i+1] = Color.getHSBColor(angle, 1, (float) 0.7);        
        }
        return colorList;
    }
    
    
    public BackendWrapperSseq(Backend back, int p){
        this.back = back;
        this.p = p;
        this.colors = intervalColors(0,1,2*p-2);
    }
    
    private Collection<SseqClass>  setClassColors(Collection<SseqClass> classes){
        classes.forEach((c) -> {
            c.setColor(0, colors[(c.getDegree()[1] - c.getDegree()[0])  % (2*p-2)]);
        });        
        return classes;
    }
    
    @Override
    public Collection<SseqClass> getClasses() {
        return setClassColors(back.getClasses());
    }

    @Override
    public Collection<SseqClass> getClasses(int x, int y) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<SseqClass> getClasses(int[] degree) {
        return setClassColors(back.getClasses(degree));
    }

    /**
     *
     * @return
     */
    @Override
    public Collection<Structline> getStructlines() {
        Collection<SseqClass> classes = back.getClasses();
        return classes.stream().filter((g) -> g.getStructlines()!=null).flatMap((SseqClass g) -> g.getStructlines().stream()).collect(Collectors.toList());
    }


    @Override
    public int getState(int x, int y) {
        return back.getState(new int[] {x,y});
    }
    
    double xscale,yscale;

    @Override
    public int num_gradings() {
        return back.num_gradings();
    }

    @Override
    public int totalGens() {
        return back.totalGens();
    }

    @Override
    public int getTMax() {
        return back.getTMax();
    }

    @Override
    public int getState(int[] degree) {
        return back.getState(degree);
    }

    @Override
    public void addListener(PingListener l) {
        back.addListener(l);
    }

    @Override
    public void removeListener(PingListener l) {
       
    }

    
    
}
