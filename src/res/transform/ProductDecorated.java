package res.transform;

import res.algebra.*;
import java.util.*;

public class ProductDecorated<U extends GradedElement<U>, T extends MultigradedAlgebra<Generator<U>>> extends Decorated<Generator<U>,T>
{

    Collection<ProductRule> rules;

    public ProductDecorated(T und, Collection<ProductRule> rules)
    {
        super(und);
        this.rules = rules;
    }

    @Override public Collection<BasedLineDecoration<Generator<U>>> getStructlineDecorations(Generator<U> g)
    {
        ArrayList<BasedLineDecoration<Generator<U>>> ret = new ArrayList<>();

        for(ProductRule rule : rules) if(!rule.hide) {
            for(Dot<U> dot : g.img.keySet()) {
                if(dot.sq.equals(rule.trigger))
                    ret.add(new BasedLineDecoration<>(g, dot.base, rule.color));
            }
        }

        return ret;
    }
}

