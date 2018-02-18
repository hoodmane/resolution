package res;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import res.algebra.MultigradedElement;
import res.algebra.MultigradedVectorSpace;
import res.transform.Decorated;


/**
 *
 * @author Hood
 * @param <U>
 */
public class ExportToTex <U extends MultigradedElement<U>> {
    private final StringBuilder classes;
    private final StringBuilder structlines;
    private final StringBuilder output;
    
    private final String texHead = "\\documentclass{spectralsequence-example}\n\\begin{document}\n\\begin{sseqpage}\n";
    private final String texFoot = "\\end{sseqpage}\n\\end{document}\n";
    
    Decorated<U, ? extends MultigradedVectorSpace<U>> dec;
    MultigradedVectorSpace<U> comp;
    
    public ExportToTex(Decorated<U, ? extends MultigradedVectorSpace<U>> dec){
        this.dec = dec;
        comp = dec.underlying();
        classes = new StringBuilder(20*comp.totalGens());
        structlines = new StringBuilder(20*comp.totalGens());
        output = new StringBuilder(40*comp.totalGens());
        for(int x=0; x<Config.T_CAP;x++){
            for(int y=0;y+x<Config.T_CAP;y++){
                comp.gens(multideg(x,y)).forEach((g)->{
                    addClass(g);
//                    dec.getBasedLineDecorations(u).stream().filter((d) -> !(! frameVisibles.contains(d.dest))).map((d) -> {
//                        g.setColor(d.color);
//                        return pos.get(d.dest);
//                    }).forEachOrdered((p2) -> {
//                        g.drawLine(p1[0], p1[1], p2[0], p2[1]);
//                    });                    
                    dec.getStructlineDecorations(g).stream().map((d) -> {
                        return d.dest;
                    }).forEachOrdered((dest) -> {
                        addStructline(g,dest);
                    });                    
                });
            }
        }
        output.append(texHead);
        output.append(this.classes);
        output.append(this.structlines);
        output.append(texFoot);
    }
    
    public void writeToFile(String filename){
        try {
            System.out.println(filename);
            File file = new File(filename);
            FileWriter fileWriter = new FileWriter(file);
            fileWriter.write(this.toString());
            fileWriter.flush();
            fileWriter.close();
            System.out.println("closed file");
        } catch(IOException e) {
            System.out.println("error");
        }
    }
    
    private void addClass(U g){
        int x = g.deg()[0];
        int y = g.deg()[1];
        classes.append(String.format("\\class[name=%s](%d,%d)\n", g.name(),y-x,x));
    }
    
    private void addStructline(U source,U dest){
        structlines.append(String.format("\\structline(%s)(%s)\n", source.name(),dest.name()));
    }
    
    @Override
    public String toString(){
        return output.toString();
    }
    
    private static int[] multideg(int x, int y)
    {
        return new int[] {y,x+y};
    }

}
