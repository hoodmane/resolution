package res.algebra;

import java.util.Comparator;

public class Multidegrees
{
    public static final int WILDCARD = Integer.MAX_VALUE;

    public static final Comparator<int[]> multidegComparator = (int[] a, int[] b) -> {
        if(a.length != b.length) return a.length - b.length;
        for(int i = 0; i < a.length; i++)
            if(a[i] != b[i])
                return a[i] - b[i];
        return 0;
    };
}
