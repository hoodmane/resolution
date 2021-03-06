package res.algebra;

import java.util.*;

public abstract class MultigradedVectorSpace<T extends MultigradedElement<T>> implements PingListener
{
    public final static int STATE_NOT_COMPUTED = 0;
    public final static int STATE_STARTED = 1;
    public final static int STATE_VANISHES = 2;
    public final static int STATE_OK_TO_QUERY = 3;
    public final static int STATE_PARTIAL = 3;
    public final static int STATE_DONE = 4;

    private final Set<PingListener> listeners = new HashSet<>();

    public abstract int num_gradings();
    public abstract int getState(int[] i); /* what's the state of computation for the given multi-index */
    public abstract Collection<T> gens(int[] i);
    public void start() { }

    /* the ping mechanism is to receive updates when the state of a certain multi-index changes */
    public void addListener(PingListener l) {
        listeners.add(l);
    }
    public void removeListener(PingListener l) {
        listeners.remove(l);
    }
    @Override public void ping(int[] i) {
        listeners.forEach((l) -> {
            l.ping(i);
        });
    }
}

