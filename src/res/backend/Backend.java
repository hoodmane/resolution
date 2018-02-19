package res.backend;

import res.Callback;
import res.algebra.*;

public interface Backend<T extends MultigradedElement<T>, U extends MultigradedVectorSpace<T>> {
    void start();
    Backend<T,U> registerDoneCallback(Callback f);
}

