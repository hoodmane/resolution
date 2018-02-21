package res;


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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import res.spectralsequencediagram.SpectralSequenceJson;

public class Main {
    
    private static String texOutputFilename;
    private static String jsonOutputFilename;
    private static String pdfOutputFilename;
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

    private static final Pattern A_N_ALGNAME_PAT = Pattern.compile("\\s*A\\(?\\s*(\\d*)\\s*\\)?\\s*$");
    static SettingsDialog sd;
    
    public static void main(String[] args)
    {
        JsonParser parser = new JsonParser();
        try {
            JsonObject json = parser.parse(new FileReader(new File("tex/X3-out-test.json"))).getAsJsonObject();
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
        SpectralSequenceDisplay.constructFrontend(SpectralSequenceJson.ImportSseq(json)).start();
    }
    
    static void resolveJsonModule(JsonElement json){
        JsonSpecification spec;
        GradedModule<Sq> sqmod;
        try {
            spec = JsonSpecification.loadJson(json);
            Config.P = spec.prime;
            System.out.println(Config.P);
            Config.Q = 2 * (Config.P - 1);
            Config.yscale = Config.Q;
            sqmod = new JSONModule(spec.generators,spec.relations);
        } catch(ParseException e) {
           quit(e);
           return;
        }
        ResMath.calcInverses();
        texOutputFilename = spec.tex_output;
        jsonOutputFilename = spec.json_output;
        pdfOutputFilename = spec.pdf_output;
        if(spec.xscale>0){
            Config.xscale = spec.xscale;
        }
        if(spec.yscale>0){
            Config.yscale = spec.yscale;
        }
        if(spec.scale>0){
            Config.xscale *= spec.scale;
            Config.yscale *= spec.scale;
        }


        if(spec.max_stem > 0){
            Config.T_CAP = spec.max_stem;
        }
        Matcher match;
        if(spec.algebra == null || "steenrod".equals(spec.algebra.toLowerCase())){
             startBruner(new SteenrodAlgebra(), sqmod);
        } else if("P".equals(spec.algebra)) {
             startBruner(new EvenSteenrodAlgebra(), sqmod);
        } else if((match = A_N_ALGNAME_PAT.matcher(spec.algebra)).find()) {
             int n;
             try {
                 n = Integer.parseInt(match.group(1));
                 startBruner(new AnAlgebra(n),new AnModuleWrapper(sqmod));
             } catch (NumberFormatException e) {
                 if("".equals(match.group(1))){
                     startBruner(new SteenrodAlgebra(), sqmod);
                 } else {
                     quit(new ParseException("Algebra " + spec.algebra + " not recognized." ,0));
                 }
             }
        }
    }

    static <T extends GradedElement<T>> void startBruner(GradedAlgebra<T> alg, GradedModule<T> mod)
    {
        /* backend */
        BrunerBackend<T> back = new BrunerBackend<>(alg,mod);
        SpectralSequenceDisplay display =  SpectralSequenceDisplay.constructFrontend(back).setScale(Config.xscale,Config.yscale).start();
        if(texOutputFilename!=null){
            back.registerDoneCallback(() -> {new ExportSpectralSequenceToTex(back).writeToFile("tex/"+texOutputFilename);});
        }
        
        if(jsonOutputFilename!=null){
            back.registerDoneCallback(() -> {
                try {
                    SpectralSequenceJson.ExportSseq(back).writeToFile("tex/"+jsonOutputFilename);
                } catch (IOException ex) {
                    System.out.println("Failed to write to tex/"+jsonOutputFilename);
//                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }
            });
        }
                
        if(pdfOutputFilename!=null){
            back.registerDoneCallback(() -> { 
                
                display.writeToFile("tex/"+pdfOutputFilename);
            });
        }        
        
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
            String s;
            sd = new SettingsDialog();
            sd.setVisible(true); /* blocks until dialog has completed */

            if(sd.cancelled)
                System.exit(0);

            /* prime */
            Config.P = (Integer) sd.prime.getSelectedItem();
            Config.Q = 2 * (Config.P - 1);
            Config.yscale = Config.Q;
            ResMath.calcInverses();
            Config.T_CAP = (Integer) sd.maxt.getValue();
            Config.THREADS = (Integer) sd.threads.getValue();

            /* intervene for the Cartan-Eilenberg option */
            if(sd.algcombo.getSelectedItem() == SettingsDialog.ALGCE) {
//                startCE();
                return;
            }

            /* module */
            GradedAlgebra<Sq> steen = null;
            GradedModule<Sq> sqmod;
            s = (String) sd.modcombo.getSelectedItem();
            if( null == s)
                sqmod = new Sphere<>(Sq.UNIT);
            else switch (s) {
                case SettingsDialog.MODBRUNER:
                    sqmod = new BrunerNotationModule();
                    break;
                case SettingsDialog.MODCOF2:
                    sqmod = new CofibHopf(0);
                    break;
                case SettingsDialog.MODCOFETA:
                    sqmod = new CofibHopf(1);
                    break;
                case SettingsDialog.MODCOFNU:
                    sqmod = new CofibHopf(2);
                    break;
                case SettingsDialog.MODCOFSIGMA:
                    sqmod = new CofibHopf(3);
                    break;
                case SettingsDialog.MODA1:
                    sqmod = new A1();
                    break;
                case SettingsDialog.MODEXCESS:
                    int exct = -1;
                    while(exct < 0) {
                        String excstr = JOptionPane.showInputDialog(null, "Excess less than or equal to what T?");
                        try {
                            exct = Integer.parseInt(excstr);
                        } catch(NumberFormatException e) {}
                    }       
                    steen = new SteenrodAlgebra();
                    sqmod = new ExcessModule(exct,steen);
                    break;
                default:
                    sqmod = new Sphere<>(Sq.UNIT);
                    break;
            }


            /* algebra */
            s = (String) sd.algcombo.getSelectedItem();
            Config.MICHAEL_MODE = (s == SettingsDialog.ALGODD);
            Config.MOTIVIC_GRADING = (s == SettingsDialog.ALGMOT);
            if(s == SettingsDialog.ALGSTEEN || s == SettingsDialog.ALGODD || s == SettingsDialog.ALGMOT) { // steenrod

                if(steen == null)
                    steen = new SteenrodAlgebra();

                startBruner(steen, sqmod);

            } else { // A(n)

                int N = -1;
                if(s == SettingsDialog.ALGA1) N = 1;
                else if(s == SettingsDialog.ALGA2) N = 2;
                else if(s == SettingsDialog.ALGAN) {
                    while(N < 0) {
                        String nstr = JOptionPane.showInputDialog(null, "Ext over A(n) for what n?");
                        try {
                            N = Integer.parseInt(nstr);
                        } catch(NumberFormatException e) {}
                        if(N > 20) N = -1; // huge inputs will break due to overflow
                    }
                }

                AnAlgebra analg = new AnAlgebra(N);
                GradedModule<AnElement> anmod = new AnModuleWrapper(sqmod);

                startBruner(analg, anmod);
            }
	}
    }
}

