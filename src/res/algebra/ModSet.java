package res.algebra;

import res.*;
import java.util.*;
import java.util.stream.Collectors;

/* A formal F_p-linear combination of things of type T. */
public class ModSet<T> extends TreeMap<T,Integer>
{
    final ResMath resmath; 
    final int p;

    public ModSet(int p) {
        this.p = p;
        resmath = ResMath.getInstance(p);
    }
    public ModSet(int p, T t) {
        this.p = p;
        resmath = ResMath.getInstance(p);
        add(t,1);
    }


    public void add(T d, int mult)
    {
        Integer got = get(d);
        int c = (got == null) ? 0 : got;

        c = resmath.positive_modp(c + mult);

        if(c == 0) 
            remove(d);
        else
            put(d, c);
    } 

    public void add(ModSet<T> d, int mult)
    {
        if(resmath.positive_modp(mult) == 0) return;
        d.entrySet().forEach((e) -> {
            add(e.getKey(), e.getValue() * mult);
        });
    }

    public ModSet<T> scaled(int scale)
    {
        ModSet<T> ret = new ModSet<>(p);
        entrySet().forEach((e) -> {
            ret.add(e.getKey(), e.getValue() * scale);
        });
        return ret;
    }

    public int getsafe(T d)
    {
        Integer i = get(d);
        if(i == null)
            return 0;
        return i;
    }

    public boolean contains(T d)
    {
        return (getsafe(d) % p != 0);
    }

    public void union(ModSet<T> s)
    {
        s.keySet().stream().filter((d) -> (!containsKey(d))).forEachOrdered((d) -> {
            put(d,1);
        });
    }

    @Override public String toString()
    {
        return toStringDelim(" + ");
    }

    public String toStringDelim(String delim)
    {
        if(isEmpty())
            return "0";
        String s = "";
        for(Map.Entry<T,Integer> e : entrySet()) {
            if(s.length() != 0)
                s += delim;
            if(e.getValue() != 1)
                s += e.getValue();
            s += e.getKey().toString();
        }
        return s;
    }
    
    public String toString(Stringifier<T> strf) {
        if(isEmpty())
            return "0";
        return entrySet().stream().map(e -> 
            (e.getValue()!=1 ? e.getValue() : "") + strf.toString(e.getKey())
        ).collect(Collectors.joining(" + "));
    }
}

