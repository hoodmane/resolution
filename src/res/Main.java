package res;


import res.fileio.ExportSpectralSequenceToTex;
import res.fileio.JSONModule;
import res.fileio.JsonSpecification;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import res.algebra.*;
import res.backend.*;
import res.frontend.*;

import javax.swing.JOptionPane;

import java.text.ParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import res.spectralsequencediagram.BackendWrapperSseq;
import res.spectralsequencediagram.DisplaySettings;
import res.spectralsequencediagram.EmptySpectralSequence;
import res.spectralsequencediagram.SpectralSequence;
import res.spectralsequencediagram.SseqJson;

public class Main {
    private static final int T_MAX_DEFAULT = 50;
    
    public static void die_if(boolean test, String fail)
    {
        if(test) {
            System.err.println(fail);
            Thread.dumpStack();
            System.err.println("Failing.");
            JOptionPane.showMessageDialog(null, fail);
            System.exit(1);
        }
    }
 
    private static void quit(Exception e){
        System.out.println("" + e);
        System.out.println("Quitting.");
	System.exit(1);
    }


    static SettingsDialog sd;
    
    public static void main(String[] args){
        
//        DisplaySettings settings = new DisplaySettings();
//        settings.T_max = 5000;
//        settings.x_full_range = true;
//        SpectralSequence empty_sseq = new EmptySpectralSequence();
//        SpectralSequenceDisplay.constructFrontend(empty_sseq,settings).start();
//        boolean end_now = true;
//        if(end_now){
//            return;
//        }
        if(args.length == 0){
            System.out.println("You must run resolution with an argument which should be a JSON file with fields detailed in \"README.md\".\n"
                    + "For example, to resolve the sphere, make a file \"S2.json\" containing \"{prime=2}\" and call \"./resolution S2.json\".");
            return;
        }
        JsonParser parser = new JsonParser();
        try {
            JsonObject json = parser.parse(new FileReader(new File(args[0]))).getAsJsonObject();
            if(json.get("type") != null){
                switch(json.get("type").getAsString()){
                    case "display":
                        displaySpectralSequence(json);
                        break;
                    default:
                        break;
                }
            } else {
                resolveJsonModule(json);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    static void displaySpectralSequence(JsonElement json){
        DisplaySettings fromJson = SseqJson.GSON.fromJson(json, DisplaySettings.class);
        ((JsonObject) json).entrySet().forEach(e -> System.out.println(e.getKey() +" : " + e.getValue()));
        SpectralSequenceDisplay.constructFrontend(
            SseqJson.GSON.fromJson(json,SpectralSequence.class),
            SseqJson.GSON.fromJson(json, DisplaySettings.class)
        ).start();
    }
    
    private static final Pattern A_N_ALGNAME_PAT = Pattern.compile("\\s*A\\(?\\s*(\\d*)\\s*\\)?\\s*$");
    static void resolveJsonModule(JsonElement json){
        JsonSpecification spec = JsonSpecification.loadJson(json);
        spec.json = json;
        int p = spec.prime;
        spec.p = p;
        spec.q = 2*p-2;
        GradedModule<Sq> sqmod;
        try {
            sqmod = new JSONModule(spec);      
        } catch(ParseException e) {
           quit(e);
           return;
        }
        
        if( spec.xscale == 0 ){
            spec.xscale = 1;
        }
        if( spec.yscale == 0 ){
            spec.yscale = 2*p-2;
        }        

        if(spec.scale>0){
            spec.xscale *= spec.scale;
            spec.yscale *= spec.scale;
        }
        
        if(spec.T_max == 0){
            spec.T_max = T_MAX_DEFAULT;
        }
        
        System.out.println(spec.T_max);
        Matcher match;
        if(spec.algebra == null || "steenrod".equals(spec.algebra.toLowerCase())){
             startBruner(new SteenrodAlgebra(spec.p), sqmod,spec);
        } else if("P".equals(spec.algebra)) {
             startBruner(new EvenSteenrodAlgebra(spec.p), sqmod,spec);
        } else if((match = A_N_ALGNAME_PAT.matcher(spec.algebra)).find()) {
             int n;
             try {
                 n = Integer.parseInt(match.group(1));
                 startBruner(new AnAlgebra(spec.p,n),new AnModuleWrapper(sqmod),spec);
             } catch (NumberFormatException e) {
                 if("".equals(match.group(1))){
                     startBruner(new SteenrodAlgebra(spec.p), sqmod, spec);
                 } else {
                     quit(new ParseException("Algebra " + spec.algebra + " not recognized." ,0));
                 }
             }
        }
    }

    static <T extends GradedElement<T>> void startBruner(GradedAlgebra<T> alg, GradedModule<T> mod,JsonSpecification spec)
    {
        JsonElement json = spec.json;
        /* backend */
        BrunerBackend<T> back = new BrunerBackend<>(alg,mod,spec);
        SpectralSequence sseq = new BackendWrapperSseq(back,spec.p);
        DisplaySettings settings = SseqJson.GSON.fromJson(json,DisplaySettings.class);        
        if(spec.windowed){
            SpectralSequenceDisplay display = 
                SpectralSequenceDisplay.constructFrontend(
                    sseq, settings
                ).start();
        }
        
       if(spec.tex_output != null || spec.json_output != null){
           Path outputDirectory = Paths.get("resolution-out");
            try {           
                if(!Files.isDirectory(outputDirectory)){
                    Files.createDirectories(outputDirectory);
                }
                
                if(spec.tex_output!=null){
                    back.registerDoneCallback(() -> {
                            new ExportSpectralSequenceToTex(sseq,spec.p).writeToFile("resolution-out/"+spec.tex_output);});
                }

                if(spec.json_output!=null){
                    back.registerDoneCallback(() -> {
                        try {
                            SseqJson.ExportSseq(sseq,settings).writeToFile("resolution-out/"+spec.json_output);
                        } catch (IOException ex) {
                            System.out.println("Failed to write to out/"+spec.json_output);
        //                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    });
                }                
            } catch (IOException ex) {
                System.out.println("Output directory resolution-out does not exist and I failed to create it, won't produce output files.");
            }           
       }
        
                
//        if(spec.pdf_output!=null){
//            back.registerDoneCallback(() -> { 
//                new SpectralSquencePgfWriter(back,settings).writeToFile("out/testa.tex");
//            });
//        }        
        
        /* off we go */
        back.start();
    }

//    static void startCE()
//    {
//        CotorLiftingBackend back = new CotorLiftingBackend();
//        Decorated<Generator<Sq>, ? extends MultigradedVectorSpace<Generator<Sq>>> dec = back.getDecorated();
//
//        /* frontend */
//        String s = sd.front.getSelection().getActionCommand();
//        if(s == SettingsDialog.FRONT3D)
//            ResDisplay3D.constructFrontend(dec);
//        else
//            ResDisplay.constructFrontend(dec);
//
//        /* off we go */
//        back.start();
//    }

    
    static void dialogInput(){
        {
//            String s;
//            sd = new SettingsDialog();
//            sd.setVisible(true); /* blocks until dialog has completed */
//
//            if(sd.cancelled)
//                System.exit(0);
////
////            /* prime */
////            Config.P = (Integer) sd.prime.getSelectedItem();
////            Config.Q = 2 * (Config.P - 1);
////            Config.yscale = Config.Q;
////            Config.T_CAP = (Integer) sd.maxt.getValue();
////            Config.THREADS = (Integer) sd.threads.getValue();
////
////            /* intervene for the Cartan-Eilenberg option */
////            if(sd.algcombo.getSelectedItem() == SettingsDialog.ALGCE) {
//////                startCE();
////                return;
////            }

            /* module */
//            GradedAlgebra<Sq> steen = null;
//            GradedModule<Sq> sqmod;
//            s = (String) sd.modcombo.getSelectedItem();
////            if( null == s)
////                sqmod = new Sphere<>(Sq.UNIT);
////            else switch (s) {
////                case SettingsDialog.MODBRUNER:
////                    sqmod = new BrunerNotationModule();
////                    break;
////                case SettingsDialog.MODCOF2:
////                    sqmod = new CofibHopf(0);
////                    break;
////                case SettingsDialog.MODCOFETA:
////                    sqmod = new CofibHopf(1);
////                    break;
////                case SettingsDialog.MODCOFNU:
////                    sqmod = new CofibHopf(2);
////                    break;
////                case SettingsDialog.MODCOFSIGMA:
////                    sqmod = new CofibHopf(3);
////                    break;
////                case SettingsDialog.MODA1:
////                    sqmod = new A1();
////                    break;
////                case SettingsDialog.MODEXCESS:
////                    int exct = -1;
////                    while(exct < 0) {
////                        String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
////                        try {
////                            exct = Integer.parseInt(excstr);
////                        } catch(NumberFormatException e) {}
////                    }       
////                    steen = new SteenrodAlgebra();
////                    sqmod = new ExcessModule(exct,steen);
////                    break;
////                default:
////                    sqmod = new Sphere<>(Sq.UNIT);
////                    break;
//            }
//
//
//            /* algebra */
//            s = (String) sd.algcombo.getSelectedItem();
//            Config.MICHAEL_MODE = (s == SettingsDialog.ALGODD);
//            Config.MOTIVIC_GRADING = (s == SettingsDialog.ALGMOT);
//            if(s == SettingsDialog.ALGSTEEN || s == SettingsDialog.ALGODD || s == SettingsDialog.ALGMOT) { // steenrod
//
//                if(steen == null)
//                    steen = new SteenrodAlgebra(Config.P);
//
////                startBruner(steen, sqmod);
//
//            } else { // A(n)
//
//                int N = -1;
//                if(s == SettingsDialog.ALGA1) N = 1;
//                else if(s == SettingsDialog.ALGA2) N = 2;
//                else if(s == SettingsDialog.ALGAN) {
//                    while(N < 0) {
//                        String nstr = JOptionPane.showInputDialog(null, "Ext over A(n) for what n?");
//                        try {
//                            N = Integer.parseInt(nstr);
//                        } catch(NumberFormatException e) {}
//                        if(N > 20) N = -1; // huge inputs will break due to overflow
//                    }
//                }
//
//                AnAlgebra analg = new AnAlgebra(N,Config.P);
//                GradedModule<AnElement> anmod = new AnModuleWrapper(sqmod);
//
////                startBruner(analg, anmod);
//            }
	}
    }
}

