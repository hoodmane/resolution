package res;

public class Config
{
    public static int THREADS = Runtime.getRuntime().availableProcessors() + 1;

    /* not configurable from settings dialog */
    public static final boolean DEBUG = false;
    public static final boolean DEBUG_THREADS = false;
    public static final boolean STDOUT = false;
    public static final boolean TIMING = false;

}
