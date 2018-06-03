/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import res.algebra.PingListener;

/**
 *
 * @author Hood
 */
public class EmptySpectralSequence implements SpectralSequence{

    @Override
    public int num_gradings() {
        return 2;
    }

    @Override
    public int totalGens() {
        return 0;
    }

    @Override
    public Collection<SseqClass> getClasses() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<SseqClass> getClasses(int x, int y) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<SseqClass> getClasses(int[] p, int page) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Structline> getStructlines(int page) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<Differential> getDifferentials(int page) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Integer> getPageList() {
        ArrayList a = new ArrayList(2);
        a.add(0);
        a.add(1000);
        return a;
    }

    @Override
    public int getTMax() {
        return 5000;
    }

    @Override
    public int getState(int x, int y) {
        return 4;
    }

    @Override
    public int getState(int[] p) {
        return 4;
    }

    @Override
    public void addListener(PingListener l) {
        return;
    }

    @Override
    public void removeListener(PingListener l) {
        return;
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
