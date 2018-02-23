package res;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import res.spectralsequencediagram.*;


/**
 *
 * @author Hood
 */
public class ExportSpectralSequenceToTex {
    private final StringBuilder classes;
    private final StringBuilder structlines;
    private final StringBuilder output;
    
    private final String texHead = "\\documentclass{spectralsequence-example}\n\\begin{document}\n\\begin{sseqpage}\n";
    private final String texFoot = "\\end{sseqpage}\n\\end{document}\n";
    
    SpectralSequence sseq;
    
    public ExportSpectralSequenceToTex(SpectralSequence sseq){
        this.sseq = sseq;
        classes = new StringBuilder(20*sseq.totalGens());
        structlines = new StringBuilder(20*sseq.totalGens());
        output = new StringBuilder(40*sseq.totalGens());
        for(int x=0; x < sseq.getTMax();x++){
            for( int y=0; y+x < sseq.getTMax(); y++){
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
        try (FileWriter fileWriter = new FileWriter(new File(filename))) {
            System.out.println(filename);
            fileWriter.write(this.toString());
            fileWriter.flush();
            fileWriter.close();
            System.out.println("closed file");
        } catch(IOException e) {
            System.out.println("error");
        }
    }
    
    private void addClass(SseqClass g){
        int x = g.getDegree()[0];
        int y = g.getDegree()[1];
        classes.append(String.format("\\class[name=%s](%d,%d)\n", g.getName(),y-x,x));
    }
    
    private void addStructline(Structline sl){
        structlines.append(String.format("\\structline(%s)(%s)\n", sl.getSource().getName(),sl.getTarget().getName()));
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
