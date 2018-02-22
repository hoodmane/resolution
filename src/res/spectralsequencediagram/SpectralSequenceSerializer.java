/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.spectralsequencediagram;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import res.Config;
import static res.algebra.MultigradedVectorSpace.*;
import res.algebra.PingListener;
import res.spectralsequencediagram.SseqClassSerializer.DeserializedSseqClass;
import res.spectralsequencediagram.StructlineSerializer.DeserializedStructline;

/**
 *
 * @author Hood
 */
public class SpectralSequenceSerializer implements JsonSerializer<SpectralSequence> , JsonDeserializer<SpectralSequence>{
    
    static Gson gson = new Gson();
    @Override
    public JsonElement serialize(SpectralSequence t, Type type, JsonSerializationContext jsc) {
        SseqToSerialize ts = new SseqToSerialize();
        ts.num_gradings = t.num_gradings();
        ts.classes = t.getClasses();
        ts.structlines = t.getStructlines();
        ts.T_max = Config.T_CAP;
        return jsc.serialize(ts,SseqToSerialize.class);
    }

    @Override
    public SpectralSequence deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {
        DeserializedSseq sseq = gson.fromJson(je, DeserializedSseq.class);
        sseq.initialize();
        return sseq;
    }
    
    private class SseqToSerialize {
        String type = "display";
        int num_gradings;
        int T_max;
        Collection<SseqClass> classes;
        Collection<Structline> structlines;
        double xscale, yscale;
    }        
    
    class DeserializedSseq implements SpectralSequence {
        // Json fields:
        int num_gradings;
        int T_max;
        Collection<DeserializedSseqClass> classes;
        Collection<DeserializedStructline> structlines;
        double xscale, yscale;
        
        // Internal fields:
        int total_gens;
        Map<IntPair,List<DeserializedSseqClass>> classesByDegree;
        Map<String,DeserializedSseqClass> classesByName;
        
        private void initialize(){
            System.out.println(T_max);
            classesByDegree = classes.stream().collect(Collectors.groupingBy((c) -> new IntPair(c.getDegree())));
            classesByName = classes.stream().collect(Collectors.toMap((c) -> c.getName(), (c) -> c));
            classes.forEach(c -> c.structlines = new HashSet());
            total_gens = classes.size();
            structlines.stream().forEach(sl -> {
                sl.source = classesByName.get(sl.sourceName);
                sl.target = classesByName.get(sl.targetName);
                sl.source.structlines.add(sl);
            });
//            System.out.println(classesByDegree.get(new int[] {0,0}).iterator().next().getName());
        }
        

        @Override
        public int num_gradings() {
            return num_gradings;
        }

        @Override
        public int totalGens() {
            return total_gens;
        }

        @Override
        public Collection<SseqClass> getClasses() {
            return Collections.unmodifiableCollection(classes);
        }

        @Override
        public Collection<SseqClass> getClasses(int x, int y) {
            return getClasses(new int[] {x,y});
        }

        @Override
        public Collection<SseqClass> getClasses(int[] p) {
            List<DeserializedSseqClass> c = classesByDegree.get(new IntPair(p));
            if(c!=null){
                return Collections.unmodifiableCollection(c);
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public Collection<Structline> getStructlines() {
            return Collections.unmodifiableCollection(structlines);
        }

        @Override
        public int getState(int x, int y) {
            if(T_max == 0){
                return STATE_DONE;  
            } else if(y<=T_max){
                return STATE_DONE;
            } else {
                return STATE_NOT_COMPUTED;
            }
        }

        @Override
        public int getState(int[] p) {
            return getState(p[0],p[1]);
        }

        /**
         * We just ignore the listeners here, because our plan is to never update.
         * @param l 
         */
        @Override
        public void addListener(PingListener l) {
            
        }

        @Override
        public void removeListener(PingListener l) {
            
        }

    }
    
    private class IntPair {

        private final int x;
        private final int y;

        public IntPair(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public IntPair(int[] p) {
            this.x = p[0];
            this.y = p[1];
        }
        
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IntPair)) return false;
            IntPair key = (IntPair) o;
            return x == key.x && y == key.y;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            return result;
        }

}
    
}
