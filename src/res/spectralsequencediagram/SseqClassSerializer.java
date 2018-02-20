/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import com.google.gson.*;
import java.lang.reflect.Type;

/**
 *
 * @author Hood
 */
public class SseqClassSerializer implements JsonSerializer<SseqClass> {
    @Override
    public JsonElement serialize(SseqClass t, Type type, JsonSerializationContext jsc) {
        SseqClassToSerialize cts = new SseqClassToSerialize();
        cts.name = t.getName();
        int[] naiveDegree=t.getDegree();
        cts.degree = new int[] {naiveDegree[1]-naiveDegree[0],naiveDegree[0]};
        System.out.print(cts.degree[0] + " ");
        return jsc.serialize(cts,SseqClassToSerialize.class);
    }

    private class SseqClassToSerialize {
        String name;
        int[] degree;
    }    
}

