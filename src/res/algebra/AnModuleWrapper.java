package res.algebra;

import java.util.*;

public class AnModuleWrapper extends GradedModule<AnElement>
{
    private final int p;
    private final AlgebraFactory factory;
    GradedModule<Sq> base;
    
    Map<Dot<Sq>,Dot<AnElement>> dmap = new TreeMap<>();
    Map<Dot<AnElement>,Dot<Sq>> rmap = new TreeMap<>();

    public AnModuleWrapper(GradedModule<Sq> base)
    {
        this.base = base;
        this.p = base.getP();
        this.factory = AlgebraFactory.getInstance(p);
    }

    @Override public Iterable<Dot<AnElement>> basis(int deg)
    {
        List<Dot<AnElement>> ret = new ArrayList<>();
        for(Dot<Sq> old : base.basis(deg))
            ret.add(wrap(old));
        return ret;
    }

    @Override public DModSet<AnElement> act(Dot<AnElement> o, AnElement elt)
    {
        Dot<Sq> under = rmap.get(o);
        DModSet<AnElement> ret = factory.DModSet();

        elt.modset.entrySet().forEach((sqe) -> {
            DModSet<Sq> prod = base.act(under, sqe.getKey());
            prod.entrySet().forEach((de) -> {
                Dot<AnElement> w = wrap(de.getKey());
                ret.add(w, sqe.getValue() * de.getValue());
            });
        });
        
        return ret;
    }

    static int gencount = 0;
    private Dot<AnElement> wrap(Dot<Sq> in) {
        Dot<AnElement> ret = dmap.get(in);
        if(ret != null) return ret;

        Generator<AnElement> gen = new Generator<>(p,in.deg, gencount++);
        ret = new Dot<>(gen, factory.AnUNIT);
        dmap.put(in,ret);
        rmap.put(ret,in);
        return ret;
    }

    @Override
    public int getP() {
        return p;
    }
}

