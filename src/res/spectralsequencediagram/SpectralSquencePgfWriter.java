/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author Hood
 */
public class SpectralSquencePgfWriter {
    private final SpectralSequence sseq;
    private StringBuffer out;
    private Map<SseqClass,double[]> pos;
    private Map<String,double[]> offsets;
    
    private void initializePositions(int[] degree){
        Collection<SseqClass> classes = sseq.getClasses(degree);
        int s = classes.size();
        int i = 0;
        for(SseqClass c : classes){
            i++;
            int[] deg = c.getDegree();
            double[] offset = offsets.get(i + "/" + s);
            pos.put(c, new double[] {deg[1] - deg[0] + offset[0], deg[0] + offset[1]});
        }
    }
    
    public SpectralSquencePgfWriter(SpectralSequence sseq,DisplaySettings settings){
        this.sseq = sseq;
        offsets = new HashMap<>();
        offsets.put("1/1", new double[] {0,0});
    }
    
    public void writeToFile(String filename){
        pos = new HashMap();        
        out = new StringBuffer();
        out.append("\\documentclass{spectralsequence-example}");
        out.append("\\input{macrodefs}");
        out.append("\\begin{document}");        
        out.append("\\begin{pgfpicture}");
        out.append(sseq.getClasses().stream().map(c -> {
            StringBuffer s = new StringBuffer();
            double[] position = pos.get(c);
            if(position == null){
                initializePositions(c.getDegree());
                position = pos.get(c);
            }
            s.append(String.format("\\mynode{%f}{%f}{%s}",position[0],position[1],c.getName()));
            return s;
        }).collect(Collectors.joining("\n")));
        out.append(sseq.getStructlines().stream().map(sl->{
            StringBuffer s = new StringBuffer();
            s.append(String.format("\\linesourcetarget{%s}{%s}",sl.getSource().getName(),sl.getTarget().getName()));
            return s;
        }).collect(Collectors.joining("\n")));
        out.append("\\end{pgfpicture}");
        out.append("\\end{document}");        
        try (FileWriter fileWriter = new FileWriter(new File(filename))) {
            fileWriter.write(out.toString());
            fileWriter.flush();
            fileWriter.close();
            System.out.println(filename);
        } catch (IOException ex) {
            Logger.getLogger(SpectralSquencePgfWriter.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
}
