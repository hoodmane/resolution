package res.algebra;

import res.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class BrunerNotationModule extends GradedModule<Sq>
{
   /* We make a Generator<Sq> for each generator and then embed each in a Dot<Sq>.
      A Dot<Sq> is a pair, an element of the steenrod algebra (a Sq) and a Generator<Sq> (which has an integer id and a degree).
      dotsidx: each Generator<Sq> paired with the identity element of the Steenrod algebra, embedded in a Dot.
      dots:    a map from degree to generators in that degree.
      actions: a map from generators to a map from integers (presumably representing some P^n) to a DModSet which represents an Fp linear combination of 
               elements of the module.
      TODO: refactor these to "dotsidx" ==> "gens" and  "dots" ==> "gensByDegree" or something.
   */
    ArrayList<Dot<Sq>> dotsidx = new ArrayList<>();
    Map<Integer,ArrayList<Dot<Sq>>> dots = new TreeMap<>();

    Map<Dot<Sq>,Map<Integer,DModSet<Sq>>> actions = new TreeMap<>();

    static <T> Collection<T> getRO(Map<Integer,ArrayList<T>> map, int i) {
        ArrayList<T> alist = map.get(i);
        if(alist == null) return Collections.emptySet();
        else return alist;
    }
    static <T> ArrayList<T> getRW(Map<Integer,ArrayList<T>> map, int i) {
        ArrayList<T> alist = map.get(i);
        if(alist == null) {
            ArrayList<T> ret = new ArrayList<>(1);
            map.put(i,ret);
            return ret;
        }
        else return alist;
    }

    public BrunerNotationModule(){
        this("");
    }

    public BrunerNotationModule(String filepath)
    {
	File file;
        /* XXX issues with extra gradings -- follow alg? */
        if(filepath==null || filepath.isEmpty()){
           JFileChooser jfc = new JFileChooser();
           jfc.setDialogTitle("Load module...");
           int ret = jfc.showOpenDialog(null);
           if(ret != JFileChooser.APPROVE_OPTION) {
               System.err.println("User aborted module loading.");
               System.exit(1);
           }
           file = jfc.getSelectedFile();
        } else {
           file = new File(filepath);
        }

        try {
            load(file);
        } catch(IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load module.");
            System.exit(1);
        }
    }

  /*  private void load(ModuleSpecification s){
	for(Map.Entry<String, Integer> entry : generators.entrySet()){
            ret+= entry.getKey() + " : " + Integer.toString(entry.getValue()) + "\n"; 
	}
    }*/


    private void load(File f) throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(f));

        /* line 1: total dimension */
        int n = Integer.parseInt(reader.readLine());

        /* line 2: degrees of generators */
        String[] toks = reader.readLine().split(" ");
        if(toks.length != n) {
            System.err.println("Error in bruner-notation module: number of degrees of generators on line 2 is not the given number of generators on line 1");
            System.exit(1);
        }
        for(int i = 0; i < n; i++) {
            int deg = Integer.parseInt(toks[i]);
            ArrayList<Dot<Sq>> adots = getRW(dots,deg);
            Generator<Sq> g = new Generator<>(new int[] {-1,deg,0},adots.size());
            Dot<Sq> d = new Dot<>(g, Sq.UNIT);
            dotsidx.add(d);
            adots.add(d);
            actions.put(d,new TreeMap<>());
        }


        /* remaining lines: actions */
        for(String str = reader.readLine(); str != null && ! str.trim().equals(""); str = reader.readLine()) {
            if(str.trim().equals("")) continue;

            toks = str.split(" ");
            int g = Integer.parseInt(toks[0]);
            int r = Integer.parseInt(toks[1]);
            int k = Integer.parseInt(toks[2]);

            DModSet<Sq> set = new DModSet<>();

            if(Config.P == 2) {
                if(toks.length != 3 + k) {
                    System.err.printf("Bruner notation: invalid action specification for Sq^%d g_%d\n", r, g);
                    System.exit(1);
                }

                for(int i = 3; i < 3+k; i++)
                    set.add(dotsidx.get(Integer.parseInt(toks[i])),1);

            } else {
                if(toks.length != 3 + 2*k) {
                    System.err.printf("Bruner notation: invalid action specification for Sq^%d g_%d\n", r, g);
                    System.exit(1);
                }
                for(int i = 0; i < k; i++)
                    set.add(dotsidx.get(Integer.parseInt(toks[3+2*i])), Integer.parseInt(toks[4+2*i]));
            }

            actions.get(dotsidx.get(g)).put(r,set);
        }


        reader.close();
    }

    @Override public Iterable<Dot<Sq>> basis(int deg)
    {
        return getRO(dots,deg);
    }

    DModSet<Sq> zero = new DModSet<>();
    @Override public DModSet<Sq> act(Dot<Sq> o, Sq sq)
    {
        if(sq.q.length == 0)
            return new DModSet<>(o);
        else if(sq.q.length == 1) {

            Map<Integer,DModSet<Sq>> map = actions.get(o);
            if(map == null) {
                System.err.println("Foreign dot detected in BrunerNotationModule");
                System.exit(1);
            }
            DModSet<Sq> ret = map.get(sq.q[0]);
            if(ret == null) return zero; // no defined action indicates zero
            else return ret;

        } else {
            int[] sqcopy = new int[sq.q.length-1];
            for(int i = 0; i < sq.q.length-1; i++) sqcopy[i] = sq.q[i];
            Sq next = new Sq(sqcopy);
            Sq curr = new Sq(sq.q[sq.q.length-1]);
            return act(o, curr).times(next,this);
        }
    }

}

