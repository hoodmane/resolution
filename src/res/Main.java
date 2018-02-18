package res;


import res.algebra.*;
import res.backend.*;
import res.frontend.*;
import res.transform.*;

import javax.swing.JOptionPane;

import java.text.ParseException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    
    private static String texOutputFilename;
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
	if(args.length>0) {
           JSONSpecification spec;
           GradedModule<Sq> sqmod;
           try {
               spec = JSONSpecification.loadFile(args[0]);
               Config.P = spec.prime;
               Config.Q = 2 * (Config.P - 1);
               Config.yscale = Config.Q;
	       sqmod = new JSONModule(spec.generators,spec.relations);
           } catch(ParseException | IOException e) {
              quit(e);
              return;
           }
           ResMath.calcInverses();
           texOutputFilename = spec.tex_output;
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
//                    System.out.println("n: " + n);
                    startBruner(new AnAlgebra(n),new AnModuleWrapper(sqmod));
                } catch (NumberFormatException e) {
                    if("".equals(match.group(1))){
                        startBruner(new SteenrodAlgebra(), sqmod);
                    } else {
                        quit(new ParseException("Algebra " + spec.algebra + " not recognized." ,0));
                    }
                }
           }


	} else {
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
                startCE();
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

    static <T extends GradedElement<T>> void startBruner(GradedAlgebra<T> alg, GradedModule<T> mod)
    {
        /* backend */
        BrunerBackend<T> back = new BrunerBackend<>(alg,mod);
        Decorated<Generator<T>, ? extends MultigradedVectorSpace<Generator<T>>> dec = back.getDecorated();
        if(texOutputFilename!=null){
            back.registerDoneCallback(() -> {new ExportToTex(dec).writeToFile("tex/"+texOutputFilename);});
        }
        
        ResDisplay.constructFrontend(dec).setScale(Config.xscale,Config.yscale).start();

        /* off we go */
        back.start();
    }

    static void startCE()
    {
        CotorLiftingBackend back = new CotorLiftingBackend();
        Decorated<Generator<Sq>, ? extends MultigradedVectorSpace<Generator<Sq>>> dec = back.getDecorated();

        /* frontend */
        String s = sd.front.getSelection().getActionCommand();
        if(s == SettingsDialog.FRONT3D)
            ResDisplay3D.constructFrontend(dec);
        else
            ResDisplay.constructFrontend(dec);

        /* off we go */
        back.start();
    }

}

