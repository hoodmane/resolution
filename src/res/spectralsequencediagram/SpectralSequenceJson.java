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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SpectralSequenceJson {
    private String json;
    
    static final Gson GSON = new GsonBuilder()
        .registerTypeHierarchyAdapter(SpectralSequence.class, new SpectralSequenceSerializer())
        .registerTypeHierarchyAdapter(SseqClass.class, new SseqClassSerializer())
        .registerTypeHierarchyAdapter(Structline.class, new StructlineSerializer())
        .create();
    
    
    /**
     * @param sseq The spectral sequence to serialize
     * @return SpectralSequenceJson object containing the Json representation of the input spectral sequence
     */
    public static SpectralSequenceJson ExportSseq(SpectralSequence sseq){
        SpectralSequenceJson s = new SpectralSequenceJson();
        s.json = GSON.toJson(sseq);
        return s;
    }
    
    /**
     * Import spectral sequence from a Json file.
     * @param filename The name of the file to import
     * @return A SpectralSequence object suitable for drawing.
     * @throws IOException 
     */
    public static SpectralSequence ImportSseq(String filename) throws IOException{
        try(FileReader fr = new FileReader(new File(filename))){
            return GSON.fromJson(fr, SpectralSequence.class);
        }
    }    
    
    public static SpectralSequence ImportSseq(JsonElement json){
        return GSON.fromJson(json, SpectralSequence.class);
    }        
    
    /**
     * @return The Json representation of the spectral sequence
     */
    @Override
    public String toString(){
        return json;
    }
    
    /**
     * Writes the Json representation of the spectral sequence to file.
     * @param filename The name of the output file.
     * @throws IOException 
     */
    public void writeToFile(String filename) throws IOException {
        try (FileWriter fileWriter = new FileWriter(new File(filename))) {
            System.out.println(filename);
            fileWriter.write(this.toString());
            fileWriter.flush();
            fileWriter.close();
        }
    }
}
