package res.fileio;

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
    private final int p,q;
    
    private final String texHead = "\\documentclass{spectralsequence-example}\n\\makeatletter\n\\begin{document}\n";
    private final String beginSseqpage = "\\begin{sseqpage}[scale to fit width =\\textwidth-10pt,scale to fit height=\\textheight-10pt]\n";
    private final String texFoot = "\\end{sseqpage}\n\\end{document}\n";
    static String[] colorStrings = new String[] 
        {"cblack", "cred", "cgreen", "cblue", "corange","cpurple","ccyan","cmagenta","clime","cpink","cteal","clavender","cbrown","cbeige","cmaroon" };
    SpectralSequence sseq;
    
    public ExportSpectralSequenceToTex(SpectralSequence sseq,int p){
        this.sseq = sseq;
	this.p = p;
	this.q = 2*p-2;
        classes = new StringBuilder(20*sseq.totalGens());
        structlines = new StringBuilder(20*sseq.totalGens());
        output = new StringBuilder(40*sseq.totalGens());
        for(int x=0; x < sseq.getTMax();x++){
            for( int y=0; y+x < sseq.getTMax(); y++){
                sseq.getClasses(multideg(x,y),0).forEach((c)->{
                    addClass(c);
                    c.getStructlines().stream().forEach((sl) -> 
                        addStructline(sl)
                    );
                });
            }
        }
        output.append(texHead);
        output.append(beginSseqpage);
        output.append(this.classes);
        output.append(this.structlines);
        output.append(texFoot);
    }
    
    public void writeToFile(String filename){
        try (FileWriter fileWriter = new FileWriter(new File(filename))) {
            fileWriter.write(this.toString());
            fileWriter.flush();
            fileWriter.close();
            System.out.println(filename);
        } catch(IOException e) {
            System.out.println("Failed to write tex to file " + filename);
        }
    }
    
    private void addClass(SseqClass g){
        int x = g.getDegree()[0];
        int y = g.getDegree()[1];
        classes.append(String.format("\\sseq@qclassnamed[\\sseq@qcolor{%s}](%d,%d){%s}\n",colorStrings[(y-x)%q], y-x,x,g.getName()));
    }
    
    private void addStructline(Structline sl){
        structlines.append(String.format("\\sseq@qstructlinenamed(%s)(%s)\n", sl.getSource().getName(),sl.getTarget().getName()));
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
