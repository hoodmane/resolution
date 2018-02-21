package res;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Map;
import java.util.List;

public class JsonSpecification {
    public int prime;
    public String algebra;
    public Map<String,Integer> generators;
    public List<String> relations;
    public int max_stem;
    public String tex_output;
    public String json_output;
    public String pdf_output;    
    public double xscale;
    public double yscale;
    public double scale;
    
    private static final Gson gson = new GsonBuilder().create();
    
    /**
     *
     * @param filename
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public static JsonSpecification loadJson(String filename) throws IOException, ParseException {
        try(Reader reader = new FileReader(filename)){
            return gson.fromJson(reader, JsonSpecification.class);
        }
    }

    /**
     *
     * @param json
     * @return
     */
    public static JsonSpecification loadJson(JsonElement json) {
        return gson.fromJson(json, JsonSpecification.class);
    }
    
    @Override
    public String toString() {
 	String ret;
	ret = "prime: " + Integer.toString(prime) + "\n";
	ret += "generators: \n";	
        ret = generators.entrySet().stream().map((entry) -> entry.getKey() + " : " + Integer.toString(entry.getValue()) + "\n").reduce(ret, String::concat);
	ret += "relations: \n";
        ret = relations.stream().map((r) -> r + "\n").reduce(ret, String::concat);
        return ret;
    }
}
