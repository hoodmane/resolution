/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    private List<Integer> page_list;
    
    int[] algToTopGrading(int x, int y){
        return new int[] {y, x + y};
    }
    
    int[] topToAlgGrading(int x, int y){
        return new int[] {y - x, x};
    }    
    
    
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
        this.page_list = new ArrayList<>(1);
        this.page_list.add(0);
    }
    
    private Collection<SseqClass>  setClassColors(Collection<SseqClass> classes){
        classes.forEach((c) -> {
            c.setColor(0, colors[(c.getDegree()[0])  % (2*p-2)]);
        });        
        return classes;
    }
    
    @Override
    public Collection<SseqClass> getClasses(int page) {
        return setClassColors(back.getClasses());
    }

    @Override
    public Collection<SseqClass> getClasses(int x, int y, int page) {
        return setClassColors(back.getClasses(algToTopGrading(x,y)));
    }

    @Override
    public Collection<SseqClass> getClasses(int[] degree,int page) {
        return setClassColors(back.getClasses(algToTopGrading(degree[0],degree[1])));
    }

    /**
     *
     * @return
     */
    @Override
    public Collection<Structline> getStructlines(int page) {
        Collection<SseqClass> classes = back.getClasses();
        return classes.stream().filter((g) -> g.getStructlines()!=null).flatMap((SseqClass g) -> g.getStructlines().stream()).collect(Collectors.toList());
    }


    @Override
    public int getState(int x, int y) {
        return back.getState(algToTopGrading(x,y));
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
       back.removeListener(l);
    }

    @Override
    public Collection<Differential> getDifferentials(int page) {
        return Collections.EMPTY_SET;
    }

    @Override
    public List<Integer> getPageList() {
        return page_list;
    }

    @Override
    public void executeJython(String acommand, ConsoleOutputCallback callback) {
        return;
    }

    @Override
    public String getQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
    
}
