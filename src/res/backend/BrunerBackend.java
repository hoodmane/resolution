package res.backend;

import res.*;
import res.algebra.*;
import res.transform.*;
import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import res.fileio.JsonSpecification;
import res.spectralsequencediagram.*;


/* Computes Ext_A^{s,t} (M, Z/2) through a minimal resolution of M, following a paper of Bruner. */

public class BrunerBackend<T extends GradedElement<T>>
    extends MultigradedAlgebra<Generator<T>>
    implements Backend<Generator<T>, MultigradedAlgebra<Generator<T>>>
{

    private final int p;
    private int T_max;
    private final boolean windowed;    
    private final ResMath resmath;
    private final AlgebraFactory factory;
    private final GradedAlgebra<T> alg;
    private final GradedModule<T> module;

    private Set<SseqClass> gens = new HashSet<>();
    private Map<int[],Set<Generator<T>>> gensByMultidegree = new TreeMap<>(Multidegrees.multidegComparator);
    private Map<int[],BrunerCellData<T>> output = new TreeMap<>(Multidegrees.multidegComparator);
    
    private int computedStems = 0;
    private int totalGens = 0;
    
    private CompoundDecorated<Generator<T>, MultigradedAlgebra<Generator<T>>> dec;
    
    public boolean isDone(){
         return computedStems - 1 == T_max;
    }
    
    private final Set<Callback> doneCallbacks = new HashSet<>();
    
    @Override
    public BrunerBackend<T> registerDoneCallback(Callback f){
        doneCallbacks.add(f);
        return this;
    }
    
    @Override
    public int totalGens(){
        return totalGens;
    }

    public BrunerBackend(GradedAlgebra<T> alg,GradedModule<T> m,JsonSpecification spec) {
        this.alg = alg;
        this.module = m;
        this.T_max = spec.T_max;
        this.windowed = spec.windowed;
        dec = new CompoundDecorated<>(this);
        this.p = spec.p;
        resmath = ResMath.getInstance(p);
        factory = AlgebraFactory.getInstance(p);

//        Collection<DifferentialRule> diffrules = new ArrayList<DifferentialRule>();
//        diffrules.add(new DifferentialRule(new int[] {2,1,1}, new int[] {1,1,0}, Color.green));
//        diffrules.add(new DifferentialRule(new int[] {1,0,2}, new int[] {0,0,1}, Color.red));
//        dec.add(new DifferentialDecorated<Generator<T>,MultigradedAlgebra<Generator<T>>>(this, diffrules));

        /* // RGB
        Color[] colors = new Color[] {
            new Color(128,0,0),
            new Color(0,128,0),
            new Color(0,0,128)
        }; */
        /* // fading
        Color[] colors = new Color[] {
            new Color(0,0,0),
            new Color(0,0,0),
            new Color(96,96,96),
            new Color(192,192,192)
        };*/
        // black
        Color[] colors = new Color[] {
            new Color(0,0,0),
            new Color(0,0,0),
            new Color(0,0,0),
        };
        List<T> distinguished = alg.distinguished();
        Collection<ProductRule> prodrules = new ArrayList<>();
        for(int i = 0; i < colors.length && i < distinguished.size(); i++)
            prodrules.add(new ProductRule("h_"+i, distinguished.get(i), true, false, false, colors[i]));
        dec.add(new ProductDecorated<>(this, prodrules));        
    }
    


    /* methods implementing MultigradedAlgebra */

    @Override public int num_gradings() {
        return 2 + alg.extraDegrees();
    }

    @Override public Collection<Generator<T>> gens(int[] i)
    {
        if(i.length >= 2 && (i[0] < 0 || i[1] < 0))
            return Collections.emptyList();

        if(i.length == 2) {
            BrunerCellData<T> dat = dat(i[0],i[1]);
            if(dat != null && dat.gens != null)
                return dat.gens;
            return Collections.EMPTY_LIST;
        } else if(i.length == num_gradings()) {
            Collection<Generator<T>> gens = gensByMultidegree.get(i);
            if(gens == null) return Collections.emptyList();
            return gens;
        } else return null;
    }

    @Override public int getState(int[] i)
    {
        if(i.length < 2) return STATE_VANISHES;
        for(int j = 0; j < i.length; j++)
            if(i[j] < 0) return STATE_VANISHES;
        if(i[1] < i[0]) return STATE_VANISHES;
        
        /* XXX this is just for the Steenrod. query the underlying GradedAlgebra? */
        if(i.length >= 3 && i[2] > i[0]) return STATE_VANISHES; 

        BrunerCellData<T> dat = dat(i[0], i[1]);
        if(dat != null && dat.gens != null) return STATE_DONE;
        if(claims != null && claims.contains(new int[] {i[0],i[1]})) return STATE_STARTED;
        return STATE_NOT_COMPUTED;
    }


    private boolean isKerComputed(int[] i)
    {
        if(i.length < 2) return false;
        if(i[0] < 0 || i[1] < 0) return true;
        BrunerCellData<T> dat = dat(i[0], i[1]);
        return (dat != null);
    }

    @Override public ModSet<Generator<T>> times(Generator<T> a, Generator<T> b)
    {
        /* XXX TODO full product structure */

        /* so far we can only do products by very simple s=1 generators. in fact this might only work for the Steenrod algebra */
        if(a.deg[0] != 1)
            return null;
        if(a.img.size() != 1)
            return null;
        Dot<T> op = a.img.keySet().iterator().next();
        int coeff = a.img.get(op);

        Collection<Generator<T>> dgens = gens(new int[] {b.deg[0] + 1, b.deg[1] + a.deg[1]});
        if(dgens == null)
            return null;

        ModSet<Generator<T>> ret = factory.ModSet();
        dgens.forEach((g) -> {
            Integer i = g.img.get(op);
            if (!(i == null)) {
                ret.add(g, i * coeff);
            }
        });
        
        return ret;
    }


    /* internal lookup / put */

    private BrunerCellData<T> dat(int s, int t)
    {
        synchronized(output) {
            return output.get(new int[] {s,t});
        }
    }
    private Collection<Generator<T>> gens(int s, int t)
    {
        BrunerCellData<T> dat = dat(s,t);
        if(dat == null) return null;
        return dat.gens;
    }
    private boolean isComputed(int s, int t)
    {
        return getState(new int[] {s,t}) == STATE_DONE;
    }
    private boolean isKerComputed(int s, int t)
    {
        return isKerComputed(new int[] {s,t});
    }
    private void putOutput(int s, int t, BrunerCellData<T> dat)
    {
        synchronized(output) {
            output.put(new int[] {s,t}, dat);
        }
    }
    private void putGenerator(int[] deg, Generator<T> g)
    {
        synchronized(gensByMultidegree) {
            Set<Generator<T>> s = gensByMultidegree.get(deg);
            if(s != null)
                s.add(g);
            else {
                s = new TreeSet<>();
                s.add(g);
                gensByMultidegree.put(deg,s);
            }
            gens.add(g);
            totalGens++;
        }
    }



    /* task management */
    long start;
    BlockingQueue<BrunerResTask> tasks;

    @Override public void start()
    {
        if(Config.TIMING) start = System.currentTimeMillis();

        tasks = new PriorityBlockingQueue<>();
        claims = new TreeSet<>(Multidegrees.multidegComparator);
        putTask(new BrunerResTask(BrunerResTask.COMPUTE, 0, 0));

        for(int i = 0; i < Config.THREADS; i++)
            new BrunerResTaskThread(this).start();
    }

    TreeSet<int[]> claims;
    private boolean atomic_claim_grid(int s, int t)
    {
        int[] key = new int[] {s,t};
        synchronized(claims) {
            if(claims.contains(key)) {
                //System.out.println("failed to claim "+s+","+t);
                return false;
            }
            claims.add(key);
        }
        //System.out.println("Claimed "+s+","+t);
        return true;
    }
    
    private void putTask(BrunerResTask t)
    {
        while(true) {
            try {
                tasks.put(t);
                return;
            } catch(InterruptedException e) {
                continue;
            }
        }
    }

    
    /* math */

    void compute(int s, int t)
    {
        if(Config.DEBUG) System.out.printf("(%d,%d)\n", s,t);
        /* get the old kernel basis */
        BrunerCellData<T> olddat = dat(s-1, t);
        Iterable<DModSet<T>> okbasis;

        if(s == 0)
            okbasis = module.basis_wrap(t);
        else
            okbasis = olddat.kbasis;

        Map<Dot<T>,DModSet<T>> list_x = new TreeMap<>();
        Map<Dot<T>,DModSet<T>> list_dx = new TreeMap<>();
        ArrayList<DModSet<T>> ker = new ArrayList<>();
        /* loop over existing dots in this bidegree */
        for(int gt = s; gt < t; gt++) {
            if(Config.DEBUG && gens(s,gt) == null)
                System.out.printf("null gens at (%d,%d)\n", s, gt);
            if(Config.DEBUG) System.out.printf("%d gens at (%d,%d)\n", gens(s,gt).size(), s, gt);

            for(Generator<T> g : gens(s,gt)) {
                for(T q : alg.basis(t-gt)) {
                    DModSet<T> x = factory.DModSet(new Dot<>(g,q));
                    /* compute the image */
                    DModSet<T> dx;
                    if(s > 0) dx = g.img.times(q, alg);
                    else dx = g.img.times(q, module);
                    if(Config.DEBUG) System.out.printf("1: %s --> %s", x, dx);

                    /* reduce against the existing image */
                    while(! dx.isEmpty()) {
                        Map.Entry<Dot<T>,Integer> high = dx.lastEntry();
                        Dot<T> d = high.getKey();
                        Integer coeff = high.getValue();

                        DModSet<T> modx = list_x.get(d);
                        if(modx == null)
                            break;

                        x.add(modx, -coeff);
                        dx.add(list_dx.get(d), -coeff);
                        if(Config.DEBUG) System.out.printf(" reduces to %s --> %s", x, dx);
                    }
                    if(Config.DEBUG) System.out.println();

                    if(dx.isEmpty()) { /* dx = 0, add to kernel */
                        if(Config.DEBUG) System.out.printf("Adding %s to kernel\n", x);
                        ker.add(x);
                    } else { /* register this as the entry with highest dot <highest> */
                        Map.Entry<Dot<T>,Integer> high = dx.lastEntry();
                        Dot<T> d = high.getKey();
                        Integer coeff = high.getValue();
                        if(Config.DEBUG) System.out.println("highest term "+d);
                        if(Config.DEBUG) Main.die_if(list_x.containsKey(d), "key clash on "+d);
                        list_x.put(d, x.dscaled(resmath.inverse(coeff)));
                        list_dx.put(d, dx.dscaled(resmath.inverse(coeff)));
                    }
                }
            }
        }

        if(Config.DEBUG) {
            System.out.println("Dump of image:");
            for(Dot<T> d : list_x.keySet())
                System.out.printf("%s : %s --> %s\n", d, list_x.get(d), list_dx.get(d));
        }
        
        /* free some memory */
        list_x = null;

        /* save the kernel data */
        BrunerCellData<T> dat = new BrunerCellData<>(null, ker);
        putOutput(s, t, dat);
        
        /* kick off the first child task -- only depends ker, not gens */
        if(s < t && (t == s+1 || isComputed(s+1, t-1))) 
            if(atomic_claim_grid(s+1,t))
                putTask(new BrunerResTask(BrunerResTask.COMPUTE, s+1, t)); /* move up-left */
        

        /* now see how we're doing with respect to the old kernel. modifies okbasis elements */
        ArrayList<Generator<T>> gens = new ArrayList<>();
        for(DModSet<T> k : okbasis) {
            if(Config.DEBUG) System.out.printf("kernel element %s ", k);
            /* reduce against the image */
            while(! k.isEmpty()) {
                Map.Entry<Dot<T>,Integer> ent = k.lastEntry();
                Dot<T> d = ent.getKey();
                Integer coeff = ent.getValue();

                DModSet<T> moddx = list_dx.get(d);
                if(moddx == null)
                    break;
                int ocoeff = moddx.get(d);
                k.add(moddx, -coeff * resmath.inverse(ocoeff));
            }
            if(Config.DEBUG) System.out.printf("reduces to %s\n", k);

            if(k.isEmpty()) { /* successfully killed this kernel class */
                if(Config.DEBUG) System.out.println("has been already killed");
                continue;
            }

            if(Config.DEBUG) System.out.printf("adding a generator to kill %s\n", k);
            
            /* haven't yet killed this kernel class -- add a generator */
            
            /* compute the novikov filtration */
            int[] deg = new int[2 + alg.extraDegrees()];
            deg[0] = s;
            deg[1] = t;
            for(int g = 2; g < deg.length; g++) {
                int nov = -1;
                for(Dot<T> o : k.keySet()) 
                    if(nov == -1 || o.deg[g] < nov)
                        nov = o.deg[g];
                deg[g] = nov;
            }

            Generator<T> gen = new Generator<>(p,deg, totalGens);
            gen.setStructlineGetter(() -> ConvertCollection(dec.getStructlineDecorations(gen)));
            gen.img = k;

            /* add this into the existing image */
            list_dx.put(k.lastKey(), k);

            gens.add(gen);
            putGenerator(deg, gen);
        }

        /* okbasis is done (and modified), free it */
        if(olddat != null)
            olddat.kbasis = null;

        /* save the result -- at this point the computation is considered finished */
        dat.gens = gens;
        ping(algToTopGrading(s,t));

        if(Config.STDOUT) System.out.printf("(%2d,%2d): %2d gen, %2d ker\n\n", s, t, dat.gens.size(), dat.kbasis.size());
        
        if(s == t){
            computedStems ++;
            if(isDone()){
                doneCallbacks.forEach((f) -> f.call());
            }
        }
    
        if(Config.TIMING && s == t) {
            long elapsed = System.currentTimeMillis() - start;
            double log = Math.log(elapsed);
            double score = log / t; 
            Runtime run = Runtime.getRuntime();
            System.out.printf("t=%d elapsed=%dms log/t=%f mem=%dM/%dM\n",
                t, elapsed, score, (run.maxMemory() - run.freeMemory())>>20, run.maxMemory()>>20);
        }

        /* kick off the second task */
        if(isKerComputed(s-1, t+1))
            if(atomic_claim_grid(s,t+1))
                putTask(new BrunerResTask(BrunerResTask.COMPUTE, s, t+1)); /* move right */
    }
    
    int[] algToTopGrading(int x, int y){
        return new int[] {y, x + y};
    }
    
    int[] topToAlgGrading(int x, int y){
        return new int[] {y - x, x};
    }    
    
    /**
     * Java needs some coercion to downcast Collection<T> to Collection<S> where S is a superclass of T. This is because
     * neither is a subtype of the other.
     * @param <T> Some class
     * @param <S> a super class of T
     * @param c a collection of T 
     * @return c downcast from a collection of T to a collection of S
     */
    static <S,T extends S> Collection<S> ConvertCollection(Collection<T> c){
        return c.stream().map((T t) -> (S) t).collect(Collectors.toList());
    }
    /**
     * A variant of ConvertCollection that takes a stream.
     * @param <T> Some class
     * @param <S> a super class of T
     * @param c a collection of T 
     * @return c downcast from a steam of T to a collection of S
     */
    static <S,T extends S> Collection<S> ConvertCollection(Stream<T> c){
        return c.map((T t) -> (S) t).collect(Collectors.toList());
    }    
    
    /* admin */


    @Override
    public Collection<SseqClass> getClasses() {
        return gens;
    }


    @Override
    public Collection<SseqClass> getClasses(int[] p) {
        // Just have to convince Java to regart our Collection of Generator<T> as a collection of SseqClass.
        return ConvertCollection(gens(p));
    }

    /**
     *
     * @return
     */


    
    double xscale,yscale;

    public int getTMax() {
        return T_max;
    }


}

class BrunerResTaskThread extends Thread
{
    BrunerBackend<?> back;
    int id;
    volatile static int ids;
    BrunerResTaskThread(BrunerBackend<?> back) {
        setPriority(Thread.MIN_PRIORITY);
        this.back = back;
        id = ids++;
    }

    @Override public void run()
    {
        while(true) {
            if(Config.DEBUG_THREADS) System.out.println(id + ": Waiting for task...");
            BrunerResTask t;
            try {
                t = back.tasks.take();
            } catch(InterruptedException e) {
                continue;
            }
            if(Config.DEBUG_THREADS) System.out.println(id + ": got task ...");

            if(t.t > back.getTMax())
                continue;

            switch(t.type) {
                case BrunerResTask.COMPUTE:
                    back.compute(t.s,t.t);
                    break;
                default:
                    Main.die_if(true, "Bad task type.");
            }
        }
    }
}

class BrunerResTask implements Comparable<BrunerResTask>
{
    /* task types */
    final static int COMPUTE = 0;

    int type;
    int s;
    int t;

    BrunerResTask(int type, int s, int t)
    {
        this.type = type;
        this.s = s;
        this.t = t;
    }

    @Override public int compareTo(BrunerResTask o)
    {
        return t - o.t;
    }
}

class BrunerCellData<T extends GradedElement<T>>
{
    Collection<Generator<T>> gens;
    Collection<DModSet<T>> kbasis; /* kernel basis dot-sums in bidegree s,t */

    BrunerCellData() { }
    BrunerCellData(Collection<Generator<T>> g, Collection<DModSet<T>> k) {
        gens = g;
        kbasis = k;
    }
}
