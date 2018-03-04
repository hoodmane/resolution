package res.backend;

import java.util.Collection;
import res.Callback;
import res.algebra.*;
import res.spectralsequencediagram.SseqClass;

public interface Backend<T extends MultigradedElement<T>, U extends MultigradedVectorSpace<T>> {
    void start();
    Backend<T,U> registerDoneCallback(Callback f);

    public int getTMax();
    public Collection<SseqClass> getClasses();
    public Collection<SseqClass> getClasses(int[] p);

    public int getState(int[] p);

    public int totalGens();

    public int num_gradings();

    public void addListener(PingListener l);
    public void removeListener(PingListener l);
}

