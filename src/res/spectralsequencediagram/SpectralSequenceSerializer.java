/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 *
 * @author Hood
 */
public class SpectralSequenceSerializer implements JsonSerializer<SpectralSequence>{
    
    @Override
    public JsonElement serialize(SpectralSequence t, Type type, JsonSerializationContext jsc) {
        SseqToSerialize ts = new SseqToSerialize();
        ts.num_gradings = t.num_gradings();
        ts.classes = t.getClasses();
        ts.structlines = t.getStructlines();
        return jsc.serialize(ts,SseqToSerialize.class);
    }
    
    private class SseqToSerialize {
        int num_gradings;
        Collection<SseqClass> classes;
        Collection<Structline> structlines;
    }        
    
}
