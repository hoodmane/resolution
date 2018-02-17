package res.algebra;

import java.util.*;

public abstract class GradedModule<T extends GradedElement<T>>
{
    abstract public Iterable<Dot<T>> basis(int deg);
    abstract public DModSet<T> act(Dot<T> o, T sq);

    public Iterable<DModSet<T>> basis_wrap(final int deg)
    {
        return () -> new Iterator<DModSet<T>>() {
            Iterator<Dot<T>> underlying = basis(deg).iterator();
            @Override public boolean hasNext() { return underlying.hasNext(); }
            @Override public DModSet<T> next() { return new DModSet<>(underlying.next()); }
            @Override public void remove() { underlying.remove(); }
        };
    }
}


