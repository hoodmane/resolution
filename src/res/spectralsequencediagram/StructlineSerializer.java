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
import res.spectralsequencediagram.SseqClassSerializer.DeserializedSseqClass;

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
        ts.page = t.getPage();
//        ts.shape = t.getShape();
//        ts.color = t.getColor();
        return jsc.serialize(ts,StructlineToSerialize.class);
    }

    private class StructlineToSerialize {
        String sourceName;
        String targetName;
        Shape shape;
        Color color;
        int page;
    }            
    
    class DeserializedStructline implements Structline {
        // populated by GSON
        String sourceName;
        String targetName;
        Shape shape;
        Color color;
        int page;
        
        // internal
        DeserializedSseqClass source, target;
        
        @Override
        public SseqClass getSource() {
            return source;
        }

        @Override
        public SseqClass getTarget() {
            return target;
        }

        @Override
        public Shape getShape(int page) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Color getColor(int page) {
            return color;
        }

        @Override
        public int getPage() {
            return this.page;
        }

        @Override
        public Structline setPage(int page) {
            this.page = page;
            return this;
        }

        @Override
        public boolean drawOnPageQ(int page) {
            return true;
        }

        @Override
        public int getPageMin() {
            return 0;
        }
        
    }
}
