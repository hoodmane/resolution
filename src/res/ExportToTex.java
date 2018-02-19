package res;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import res.spectralsequencediagram.*;


/**
 *
 * @author Hood
 */
public class ExportToTex {
    private final StringBuilder classes;
    private final StringBuilder structlines;
    private final StringBuilder output;
    
    private final String texHead = "\\documentclass{spectralsequence-example}\n\\begin{document}\n\\begin{sseqpage}\n";
    private final String texFoot = "\\end{sseqpage}\n\\end{document}\n";
    
    SpectralSequence sseq;
    
    public ExportToTex(SpectralSequence sseq){
        this.sseq = sseq;
        classes = new StringBuilder(20*sseq.totalGens());
        structlines = new StringBuilder(20*sseq.totalGens());
        output = new StringBuilder(40*sseq.totalGens());
        for(int x=0; x<Config.T_CAP;x++){
            for(int y=0;y+x<Config.T_CAP;y++){
                sseq.getClasses(multideg(x,y)).forEach((c)->{
                    addClass(c);
                    c.getStructlines().stream().forEach((sl) -> 
                        addStructline(sl)
                    );
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
    
    private void addClass(SseqClass g){
        int x = g.deg()[0];
        int y = g.deg()[1];
        classes.append(String.format("\\class[name=%s](%d,%d)\n", g.name(),y-x,x));
    }
    
    private void addStructline(Structline sl){
        structlines.append(String.format("\\structline(%s)(%s)\n", sl.getSource().name(),sl.getTarget().name()));
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
