package res.transform;

import res.algebra.*;
import java.util.*;

public abstract class Decorated<U extends MultigradedElement<U>, T extends MultigradedVectorSpace<U>>
{
    private final T under;

    protected Decorated(T under) {
        this.under = under;
    }

    public final T underlying() {
        return under;
    }

    /* good to override: */
    public boolean isVisible(U u) { return true; }
    public Collection<BasedLineDecoration<U>> getStructlineDecorations(U u) {
        return Collections.emptyList();
    }
    public Collection<UnbasedLineDecoration<U>> getUnbasedStructlineDecorations(U u) {
        return Collections.emptyList();
    }
}

