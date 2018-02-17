package res;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.Map;
import java.util.List;

public class JSONSpecification {
    public int prime;
    public String algebra;
    public Map<String,Integer> generators;
    public List<String> relations;
    public int max_stem;
    public String tex_output;
    
    public static JSONSpecification loadFile(String filename) throws IOException, ParseException {
        try(Reader reader = new FileReader(filename)){
            Gson gson = new GsonBuilder().create();
            return gson.fromJson(reader, JSONSpecification.class);
        }
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
