/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import com.google.gson.*;
import java.awt.Color;
import java.awt.Shape;
import java.lang.reflect.Type;

/**
 *
 * @author Hood
 */
public class StructlineSerializer implements JsonSerializer<Structline> {
    
    @Override
    public JsonElement serialize(Structline t, Type type, JsonSerializationContext jsc) {
        StructlineToSerialize ts = new StructlineToSerialize();
        ts.sourceName = t.getSource().getName();
        ts.targetName = t.getTarget().getName();
//        ts.shape = t.getShape();
//        ts.color = t.getColor();
        return jsc.serialize(ts,StructlineToSerialize.class);
    }
    
    private class StructlineToSerialize {
        String sourceName;
        String targetName;
        Shape shape;
        Color color;
    }            
    
}
