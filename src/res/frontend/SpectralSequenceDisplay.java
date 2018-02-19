package res.frontend;

import res.algebra.*;
import res.transform.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import res.spectralsequencediagram.*;

public class SpectralSequenceDisplay<U extends MultigradedElement<U>> extends JPanel 
        implements PingListener, MouseMotionListener, MouseListener, MouseWheelListener
{
    final static int DEFAULT_MINFILT = 0;
    final static int DEFAULT_MAXFILT = 100;
    
    final static int ZOOMINTERVAL = 50;
    final static double ZOOM_BASE = 1.1;
    final static int DEFAULT_WINDOW_WIDTH = 1600;
    final static int DEFAULT_WINDOW_HEIGHT = 800;
    final static int MARGIN_WIDTH = 30;

    final static Color transparent = new Color(0,0,0,0);
    
    private final static Map<Integer,Color> STATE_COLORS = new HashMap();
    static {
        STATE_COLORS.put(MultigradedVectorSpace.STATE_NOT_COMPUTED,Color.black);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_STARTED,Color.darkGray);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_PARTIAL,Color.yellow);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_VANISHES,Color.lightGray);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_DONE,transparent);
    }
    
    Decorated<U, ? extends MultigradedVectorSpace<U>> dec;
    MultigradedVectorSpace<U> under;
    SpectralSequence sseq;

    int[] minfilt;
    int[] maxfilt;
    
    private double yscale = 1; 
    private final double yscale_log = Math.log(yscale)/Math.log(ZOOM_BASE);
    double viewx = 0;
    double viewy = 0;
    double zoom = 0;
    // Derived in updateOffsets():
    double scale;   
    double xoffset;  
    double yoffset;
    double block_width;
    double block_height;
    
    int selx = -1;
    int sely = -1;
    int mousex = -1;
    int mousey = -1;    
    
    final JFrame frame;
    JTextArea textarea = null;

    /*
     * Setup methods: each of these get called in order to initialize the display:
     *    constructFrontEnd()
     *       ResDisplay()
     *       setBackened()
     *    setScale() (optional)
     *    start()
     *     
     */
    
    public static SpectralSequenceDisplay constructFrontend(SpectralSequence sseq) 
    {
        SpectralSequenceDisplay d = new SpectralSequenceDisplay<>();
        d.sseq = sseq;
        sseq.addListener(d);
        d.setupGradings();
        JFrame fr = d.frame;
        fr.getContentPane().add(d, BorderLayout.CENTER);
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        d.addMouseListener(d);
        d.addMouseMotionListener(d);
        d.addMouseWheelListener(d);
        return d;
    }
    
    private SpectralSequenceDisplay(){
        frame = new JFrame("Resolution");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(DEFAULT_WINDOW_WIDTH,DEFAULT_WINDOW_HEIGHT);        
    }
    
    private void setupGradings()
    {
        int i;
        if(minfilt == null) {
            i = 0;
            minfilt = new int[sseq.num_gradings()];
            maxfilt = new int[sseq.num_gradings()];
        } else if(minfilt.length != sseq.num_gradings()) {
            i = minfilt.length;
            minfilt = Arrays.copyOf(minfilt, sseq.num_gradings());
            maxfilt = Arrays.copyOf(maxfilt, sseq.num_gradings());
        } else return;

        for(; i < minfilt.length; i++) {
            minfilt[i] = DEFAULT_MINFILT;
            maxfilt[i] = DEFAULT_MAXFILT;
        } 
    }
    
    public SpectralSequenceDisplay<U> setScale(double xscale,double yscale){
        this.yscale = yscale/xscale;
        this.zoom = Math.log(xscale)/Math.log(ZOOM_BASE);
        return this;
    }

    public SpectralSequenceDisplay<U> start(){
        initializePosition();
        this.frame.setVisible(true);
        return this;
    }
    
    private void initializePosition(){
        updateOffsets(); // We have to call this to initialize block_width and block_height.
        viewx = MARGIN_WIDTH + block_width/2; // Which are needed to initialize viewx and viewy
        viewy = - MARGIN_WIDTH + 0.5*block_height;
        updateOffsets();
    }
    /**
     * getScreenX converts the x chart coordinate into an x screen coordinate. 
     * Make sure this is the inverse of getChartX(x) if you make any changes! 
     * @param x chart coordinate
     * @return screen coordinate
     */
    private double getScreenX(double sx) {
        return (block_width * (sx-0.5) +  xoffset);
    }
    private double getScreenY(double sy) {
        return getHeight() - (block_height * (sy+0.5) - yoffset);
    }
    
    private double[] getScreenP(double[] sp){
        return new double[] {getScreenX(sp[0]),getScreenY(sp[1])};
    }
    
    /**
     * getChartX converts the x screen coordinate into an x chart coordinate.
     * Make sure this is the inverse of getSX(cx) if you make any changes!
     * @param cx screen coordinate
     * @return chart coordinate
     */
    private double getChartX(double cx) {
        double x = cx - xoffset;
        x /= block_width;
        x += 0.5;
        return x;
    }
    private double getChartY(double cy) {
        double y = cy - getHeight() - yoffset;
        y = -y;
        y /= block_height;
        y -= 0.5;
        return y;
    }
    
    private double[] getChartP(double[] cp){
        return new double[] {getChartX(cp[0]),getChartY(cp[1])};
    }

    private boolean isVisible(SseqClass d)
    {
        int[] deg = d.getDegree();
        for(int i = 2; i < minfilt.length; i++)
            if(deg[i] < minfilt[i] || deg[i] > maxfilt[i])
                return false;
        return true; //dec.isVisible(d);
    }

    private int[] multideg(int x, int y)
    {
        return new int[] {y,x+y};
    }

    private void drawLine(Graphics2D g,double x1,double y1,double x2,double y2){
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
     
    private void drawStructline(Graphics2D g, Structline sl){
        g.setColor(sl.getColor());
        SseqClass source = sl.getSource();
        SseqClass target = sl.getTarget();
        if(pos.get(source)!=null && pos.get(target)!=null){//if(isVisible(u) && isVisible(d.dest)){
            drawStructlineHelper(g,pos.get(source),pos.get(target));
        }
                g.setColor(sl.getColor());
//        AffineTransform saveTransform = g.getTransform();        
//        double[] source = pos.get(sl.getSource());
//        double[] target = pos.get(sl.getTarget());
//        double x0 = target[0];
//        double y0 = target[1];
//        double dx = target[0]-source[0];
//        double dy = target[1]-source[1];
//        double veclen = Math.sqrt(Math.pow(dx,2)+Math.pow(dy,2));
//        
//        AffineTransform affineTransform = new AffineTransform();
//        affineTransform.scale(veclen,veclen);
//        affineTransform.rotate(Math.atan2(dy, dx));
//        affineTransform.translate(x0, y0);
//        
//        g.draw(sl.getShape());
    }
    
    private void drawUnbasedStructline(Graphics2D g, U u,UnbasedLineDecoration<U> d){
        g.setColor(d.color);
        drawStructlineHelper(g,pos.get(u),getScreenP(d.dest));
    }    
    
    private void drawStructlineHelper(Graphics2D g, double[] p1,double[] p2){
        g.draw(new Line2D.Double(p1[0] + block_width/2, p1[1] - block_height/2, p2[0] + block_width/2, p2[1] - block_height/2));
    }

    private void shadeChartCell(Graphics2D g,Color color,double x,double y){
        g.setColor(color);
        g.fill(new  Rectangle2D.Double(getScreenX(x), getScreenY(y) - block_height, block_width, block_height));
    }
    
    private TreeMap<SseqClass,double[]> pos;
    
    private void drawClass(Graphics2D g, SseqClass c){
        g.setColor(Color.black);
        AffineTransform saveTransform = g.getTransform();
        double p[] = pos.get(c);
        AffineTransform affineTransform = new AffineTransform();
        affineTransform.setToTranslation(p[0] - 3 + block_width/2,p[1] - 3 - block_height/2);
        g.setTransform(affineTransform);
        g.fill(c.getShape(0));
        g.setTransform(saveTransform);
    }
    
    @Override public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        
        /* calculate visible region */
        int min_x_visible = (int) getChartX(-3*block_width);
        int min_y_visible = (int) getChartY((getHeight() + 3*block_height));
        if(min_x_visible < 0) min_x_visible = 0;
        if(min_y_visible < 0) min_y_visible = 0;
        int max_x_visible = (int) getChartX(getWidth() + 3*block_width);
        int max_y_visible = (int) getChartY(-3*block_height);
        int max_visible = (max_x_visible < max_y_visible) ? max_y_visible : max_x_visible;
      
       
        /* draw grid  -- this needs to go first so that the uncomputed region is correctly blacked out 
         * (it looks ugly to have a light gray grid on a black background).
         */
        int xtickstep = 5;
        int ytickstep = 5;
        for(double i = -zoom; i > 0; i -= ZOOMINTERVAL) xtickstep *= 2;
        for(double i = (-zoom - yscale_log); i > 0; i -= ZOOMINTERVAL*yscale) ytickstep *= 2;
        for(int x = 0; x <= max_visible; x++) {
            g.setColor(Color.lightGray);
            drawLine(g,getScreenX(x), getScreenY(0), getScreenX(x), 0);
            drawLine(g,getScreenX(0), getScreenY(x), getWidth(), getScreenY(x));

            g.setColor(Color.black);
            // Draw axes ticks. Not such a fan of these magic numbers...
            if(x % xtickstep == 0) {
                g.drawString(String.valueOf(x), (float)(getScreenX(x)-8), (float)(getScreenY(0)+(18.*block_height/30.))+17);
            }
            if(x % ytickstep == 0){
                g.drawString(String.valueOf(x), (float)(getScreenX(0)-(19.*block_width/30.)-19), (float)(getScreenY(x)+5));
            }
        }                
        
        /* assign dots a location; at this point we definitively decide what's visible */
        Set<SseqClass> frameVisibles = new TreeSet<>();
        pos = new TreeMap<>();
        for(int x = min_x_visible; x <= max_x_visible; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cellState = sseq.getState(multideg(x,y));
                Color cellColor;
                if(x == selx && y == sely) {
                    cellColor = Color.orange;
                } else {
                    cellColor = STATE_COLORS.get(cellState);
                }
                
                shadeChartCell(g,cellColor,x,y);                

                if(!(cellState == MultigradedVectorSpace.STATE_PARTIAL || cellState == MultigradedVectorSpace.STATE_DONE)){
                    continue;
                }
                
                Collection<SseqClass> classes = sseq.getClasses(multideg(x,y));

                int visible;
                synchronized(classes) {
                    visible = (int) classes.stream().filter((d) -> (isVisible(d))).map((d) -> {
                        frameVisibles.add(d);
                        return d;
                    }).count();
                    int offset = -5 * (visible-1) / 2;
                    for(SseqClass d : classes) { 
                        if(frameVisibles.contains(d)) {
                            double[] newpos = new double[] { getScreenX(x) + offset, getScreenY(y) - offset/2 };
                            pos.put(d, newpos);
                            offset += 5;
                        }
                    }
                }
            }
        }
 

        /* draw decorations */
        frameVisibles.forEach((SseqClass u) -> {
            /* based */
            u.getStructlines().forEach((s) ->
                drawStructline(g,s)
            );
            
//            /* unbased */
//            dec.getUnbasedStructlineDecorations(u).forEach((d) -> 
//                drawUnbasedStructline(g,u,d)
//            );
        });

        /* draw dots */
        g.setColor(Color.black);
        frameVisibles.forEach((c) -> 
            drawClass(g,c)
        );

        /* draw axes */
        int bmy = getHeight() - MARGIN_WIDTH;
        g.setColor(getBackground());
        g.fillRect(0, 0, MARGIN_WIDTH, getHeight());
        g.fillRect(0, bmy, getWidth(), MARGIN_WIDTH);
        g.setColor(Color.gray);
        g.drawLine(MARGIN_WIDTH, 0, MARGIN_WIDTH, bmy);
        g.drawLine(MARGIN_WIDTH, bmy, getWidth(), bmy);
        g.setColor(Color.black);
        for(int x = 0; x <= max_visible; x += xtickstep) {
            g.drawString(String.valueOf(x),(float) getScreenX(x)-8, getHeight()-10);
        }
        for(int y = 0; y <= max_visible; y += ytickstep) {
            g.drawString(String.valueOf(y), 10, (float)(getScreenY(y)+5));
        }

    }
    
    void updateOffsets(){
        block_width = 1 + (29.0 * Math.pow(ZOOM_BASE,zoom));
        double oldscale = scale;
        scale = block_width/30.0;
        // Update (viewx,viewy) so that zoom centers on mouse.
        viewx += mousex*(1/scale - 1/oldscale);
        viewy -= (getHeight()-mousey)*(1/scale - 1/oldscale);
        xoffset = scale * viewx;
        yoffset = scale * viewy;
        block_height = (block_width*yscale);
    }

    @SuppressWarnings("fallthrough")
    void setSelected(int x, int y)
    {
        selx = x;
        sely = y;
        repaint();

        if(textarea == null) return;

        String ret = "";
        switch(sseq.getState(multideg(x,y))) {
            case MultigradedVectorSpace.STATE_VANISHES:
                ret = "This degree vanishes formally.";
                break;
            case MultigradedVectorSpace.STATE_NOT_COMPUTED:
                ret = "This degree has not yet been computed.";
                break;
            case MultigradedVectorSpace.STATE_STARTED:
                ret = "This degree is still being computed.";
                break;
            case MultigradedVectorSpace.STATE_PARTIAL:
                ret = "This degree might not yet be complete.\n";
                // case falls through
            case MultigradedVectorSpace.STATE_DONE:
            default:
                ret += "Bidegree ("+x+","+y+")\n";
                Collection<SseqClass> classes = sseq.getClasses(multideg(x,y));
                for(SseqClass c : classes) if(isVisible(c)) {
                    ret += "\n" + c.toString();
                    ret += "\n" + c.extraInfo();
                    ret += "\n";
                }
        }

        textarea.setText(ret);
    }

    @Override public void mouseClicked(MouseEvent evt)
    {
        int x = (int) getChartX(evt.getX());
        int y = (int) getChartY(evt.getY());
        if(x >= 0 && y >= 0) {
            setSelected(x,y);
        } else {
            setSelected(-1,-1);
        }
    }
    @Override public void mousePressed(MouseEvent evt) { }
    @Override public void mouseReleased(MouseEvent evt) { }
    @Override public void mouseEntered(MouseEvent evt) { }
    @Override public void mouseExited(MouseEvent evt) { }

    @Override public void mouseMoved(MouseEvent evt)
    {
        mousex = evt.getX();
        mousey = evt.getY();
    }

    @Override public void mouseDragged(MouseEvent evt)
    {
        double dx = evt.getX() - mousex;
        double dy = evt.getY() - mousey;

        mousex = evt.getX();
        mousey = evt.getY();

        viewx += dx/scale;
        viewy += dy/scale;
        updateOffsets();
        
        repaint();
    }

    @Override public void mouseWheelMoved(MouseWheelEvent evt)
    {
        zoom -= evt.getWheelRotation();
        updateOffsets();
        repaint();
    }

    @Override public void ping(int[] deg)
    {
        repaint();
        int[] s = multideg(selx, sely);
        if(deg[0] == s[0] && deg[1] == s[1])
            setSelected(selx,sely); // updates text
    }
}

class ControlPanel2D extends Box {

    ControlPanel2D(final SpectralSequenceDisplay<?> parent)
    {
        super(BoxLayout.Y_AXIS);

        setup_gui(parent);
    }

    private void setup_gui(final SpectralSequenceDisplay<?> parent)
    {

        /* filtration sliders */
        for(int i = 2; i < parent.minfilt.length; i++) {
            final int icopy = i;

            final JSpinner s1 = new JSpinner(new SpinnerNumberModel(parent.minfilt[i],0,1000,1));
            final JSpinner s2 = new JSpinner(new SpinnerNumberModel(parent.maxfilt[i],0,1000,1));

            s1.addChangeListener((ChangeEvent e) -> {
                parent.minfilt[icopy] = (Integer) s1.getValue();
                parent.setSelected(parent.selx, parent.sely);
                parent.repaint();
            });
            
            s2.addChangeListener((ChangeEvent e) -> {
                parent.maxfilt[icopy] = (Integer) s2.getValue();
                parent.setSelected(parent.selx, parent.sely);
                parent.repaint();
            });

            if(i == 2) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager()
                    .addKeyEventDispatcher((KeyEvent e) -> {
                        if(e.getID() != KeyEvent.KEY_PRESSED)
                            return false;
                        switch(e.getKeyCode()) {
                            case KeyEvent.VK_PAGE_UP:
                                s1.setValue( ((Integer) s1.getValue()) + 1);
                                s2.setValue( ((Integer) s2.getValue()) + 1);
                                return true;
                            case KeyEvent.VK_PAGE_DOWN:
                                s1.setValue( ((Integer) s1.getValue()) - 1);
                                s2.setValue( ((Integer) s2.getValue()) - 1);
                                return true;
                            default:
                                return false;
                        }
                });
            }

            Dimension smin = new Dimension(0,30);
            s1.setMinimumSize(smin);
            s2.setMinimumSize(smin);
            s1.setPreferredSize(smin);
            s2.setPreferredSize(smin);

            add(new JLabel("Filtration "+(i-1)+":"));
            add(new JLabel("min:"));
            add(s1);
            add(new JLabel("max:"));
            add(s2);
            add(Box.createVerticalStrut(20));
        }

        /*
        final JCheckBox diff = new JCheckBox("Alg Novikov differentials");
        diff.setSelected(false);
        diff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.diff = diff.isSelected();
                parent.repaint();
            }
        });
        add(diff);
        
        final JCheckBox cartdiff = new JCheckBox("Cartan differentials");
        cartdiff.setSelected(true);
        cartdiff.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                parent.cartdiff = cartdiff.isSelected();
                parent.repaint();
            }
        });
        add(cartdiff);
        add(Box.createVerticalStrut(20));

        for(int i = 0; i <= 2; i++) {
            final int j = i;

            Box h = Box.createHorizontalBox();
            h.add(new JLabel("h"+i+":"));
            final JCheckBox hlines = new JCheckBox("lines", parent.hlines[i]);
            hlines.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.hlines[j] = hlines.isSelected();
                    parent.repaint();
                }
            });
            final JCheckBox hhide = new JCheckBox("hide", parent.hhide[i]);
            hhide.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.hhide[j] = hhide.isSelected();
                    parent.repaint();
                }
            });
            final JCheckBox htowers = new JCheckBox("towers", parent.htowers[i]);
            htowers.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent evt) {
                    parent.htowers[j] = htowers.isSelected();
                    parent.repaint();
                }
            });
            h.add(hlines);
            h.add(hhide);
            h.add(htowers);
            
            h.setAlignmentX(-1.0f);
            add(h);
        }
        add(Box.createVerticalStrut(20));
        */

        parent.textarea = new JTextArea();
        parent.textarea.setMaximumSize(new Dimension(250,3000));
        parent.textarea.setPreferredSize(new Dimension(250,3000));
        parent.textarea.setEditable(false);
        parent.textarea.setLineWrap(true);
        JScrollPane textsp = new JScrollPane(parent.textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textsp.setMaximumSize(new Dimension(250,3000));
        textsp.setPreferredSize(new Dimension(250,3000));
        textsp.setAlignmentX(-1.0f);
        add(textsp);
    }

}

