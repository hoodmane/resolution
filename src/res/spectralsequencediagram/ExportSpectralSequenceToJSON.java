/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

/**
 *
 * @author Hood
 */

import com.google.gson.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ExportSpectralSequenceToJSON {
    private final SpectralSequence sseq;
    private final String json;
    static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(SpectralSequence.class, new SpectralSequenceSerializer())
        .registerTypeHierarchyAdapter(SseqClass.class, new SseqClassSerializer())
        .registerTypeHierarchyAdapter(Structline.class, new StructlineSerializer())
        .create();
    
    public ExportSpectralSequenceToJSON(SpectralSequence sseq){
        this.sseq = sseq;
        json = GSON.toJson(sseq);
    }
    
    @Override
    public String toString(){
        return json;
    }
    
    public void writeToFile(String filename) {
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
}
