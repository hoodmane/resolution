package res.transform;

import res.algebra.*;
import java.util.*;

public class CompoundDecorated<U extends MultigradedElement<U>, T extends MultigradedVectorSpace<U>> extends Decorated<U,T>
{
    private final Collection<Decorated<U,T>> sub;

    public CompoundDecorated(T u) {
        super(u);
        sub = new LinkedList<>();
    }

    public boolean add(Decorated<U,T> d)
    {
        if(d.underlying() != underlying()) {
            System.err.println("Error adding decorated to CompoundDecorated: different underyling algebra");
            return false;
        }
        sub.add(d);
        return true;
    }

    @Override public boolean isVisible(U u)
    {
        return sub.stream().noneMatch((d) -> (! d.isVisible(u)));
    }

    @Override public Collection<BasedLineDecoration<U>> getStructlineDecorations(U u)
    {
        Collection<BasedLineDecoration<U>> ret = new ArrayList<>();
        sub.forEach((d) -> {
            ret.addAll(d.getStructlineDecorations(u));
        });
        return ret;
    }

    @Override public Collection<UnbasedLineDecoration<U>> getUnbasedStructlineDecorations(U u)
    {
        Collection<UnbasedLineDecoration<U>> ret = new ArrayList<>();
        sub.forEach((d) -> {
            ret.addAll(d.getUnbasedStructlineDecorations(u));
        });
        return ret;
    }
 }
