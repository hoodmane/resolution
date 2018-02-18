package res;

public class Config
{
    /* configurable from settings dialog; these set defaults */
    public static int P = 2;
    public static int THREADS = Runtime.getRuntime().availableProcessors() + 1;
    public static int T_CAP = 50;
    public static boolean MICHAEL_MODE = false;
    public static boolean MOTIVIC_GRADING = false;
    public static double xscale = 1;
    public static double yscale = 1;

    /* not configurable from settings dialog */
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_THREADS = false;
    public static final boolean STDOUT = false;
    public static final boolean TIMING = false;

    /* the following aren't actually config */
    public static int Q;
}
