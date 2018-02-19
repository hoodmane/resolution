package res.backend;

import res.algebra.*;

public interface Backend<T extends MultigradedElement<T>, U extends MultigradedVectorSpace<T>> {
    void start();
}

