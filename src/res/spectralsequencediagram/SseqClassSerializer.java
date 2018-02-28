/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import com.google.gson.*;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author Hood
 */
public class SseqClassSerializer implements JsonSerializer<SseqClass>{
    @Override
    public JsonElement serialize(SseqClass t, Type type, JsonSerializationContext jsc) {
        SseqClassToSerialize cts = new SseqClassToSerialize();
        cts.name = t.getName();
        cts.degree = t.getDegree();
        cts.extraInfo = t.extraInfo();
//        System.out.print(cts.degree[0] + " ");
        return jsc.serialize(cts,SseqClassToSerialize.class);
    }


    private class SseqClassToSerialize {
        String name;
        int[] degree;
        String extraInfo;
    }    
    
    class DeserializedSseqClass implements SseqClass {
        // Populated by GSON
        int[] degree;
        String name;
        String extraInfo;
        
        // internal fields
        Collection<Structline> structlines = new ArrayList<>();
        Shape shape =  new Ellipse2D.Double( 0, 0, 6, 6);
        
        @Override
        public int[] getDegree() {
            return degree;
        }

        @Override
        public Collection<Structline> getStructlines() {
            return Collections.unmodifiableCollection(structlines);
        }

        @Override
        public Collection<Structline> getDifferentials() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public int getDeathPage() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Shape getShape(int page) {
            return new Ellipse2D.Double( 0, 0, 6, 6);
        }

        @Override
        public String extraInfo() {
            return extraInfo;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Color getColor(int page) {
            return Color.BLACK;
        }

        @Override
        public void setColor(int page, Color color) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
        
    }
}

