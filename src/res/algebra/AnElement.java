package res.algebra;

import java.util.HashMap;
import java.util.Map;

public class AnElement implements GradedElement<AnElement>
{
    static final int[] EGR = {};
    ModSet<Sq> modset;
    int deg;
    int idx;
    static int idxcounter = 0;
    private final int p;

    @Override
    public int getP(){
        return p;
    }
    

    AnElement(int p, ModSet<Sq> ms, int d) {
        this.p = p;
        modset = ms;
        deg = d;
        idx = idxcounter++;
    }

    @Override public int deg() {
        return deg;
    }
    @Override public int[] extraGrading() {
        return EGR;
    }
    @Override public int compareTo(AnElement o) {
        return idx - o.idx;
    }
    @Override public boolean equals(Object o) {
        return ((AnElement)o).idx == idx;
    }

    @Override public String toString() {
        return "(" + modset.toString() + ")";
    }

}

