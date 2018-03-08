package res.frontend;

import res.algebra.*;
import res.transform.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.*;
import res.spectralsequencediagram.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SpectralSequenceDisplay<U extends MultigradedElement<U>> extends JPanel 
        implements PingListener, MouseMotionListener, MouseListener, MouseWheelListener, WindowListener 
{
    final static int DEFAULT_MINFILT = 0;
    final static int DEFAULT_MAXFILT = 100;
    
    final static int TICK_STEP_LOG_BASE = 10;
    final static double ZOOM_BASE = 1.1;
    final static int DEFAULT_WINDOW_WIDTH = 1600;
    final static int DEFAULT_WINDOW_HEIGHT = 800;
    final static int MARGIN_WIDTH = 30;
    final static double TICK_GAP = 20;
    final static double BLOCK_FACTOR = 30;
    
    // These are for output to pdf.
    final static int PAGE_HEIGHT = 300;
    final static int PAGE_WIDTH  = PAGE_HEIGHT * 16/9; // Screens have a 16 x 9 aspect ratio.

    final static Color transparent = new Color(0,0,0,0);
    final static AffineTransform ID_TRANSFORM = AffineTransform.getRotateInstance(0);
    AffineTransform transform;
    AffineTransform inverseTransform = new AffineTransform();
    
    
    final static Map<Integer,Color> STATE_COLORS = new HashMap();
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
    int T_max;
    
    final double scale_aspect_ratio; 
    final double scale_aspect_ratio_log;
    double zoom = 0;
    // Derived in updateOffsets():
    double scale, xscale, yscale;  
    private int xtickstep;
    private int ytickstep;
    private int xgridstep;
    private int ygridstep;
    
    // selected x, selected y. Initialize to a large negative value representing "no selection".
    int selx = -10000;
    int sely = selx;
    int mousex = -1;
    int mousey = -1;    
    
    private Stroke gridStroke = new BasicStroke();
    private Stroke lineStroke = new BasicStroke();
    
    private boolean mouseDown;
    JFrame frame;
    JTextArea textarea;

    private final Timer screenPaintTimer;
    
    
    public static SpectralSequenceDisplay constructFrontend(SpectralSequence sseq,DisplaySettings settings) {
        SpectralSequenceDisplay d = new SpectralSequenceDisplay(sseq,settings);
        d.frame = new JFrame("Resolution");
        d.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        d.frame.setSize(DEFAULT_WINDOW_WIDTH,DEFAULT_WINDOW_HEIGHT);        
        JFrame fr = d.frame;
        fr.getContentPane().add(d, BorderLayout.CENTER);
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        d.addMouseListener(d);
        d.addMouseMotionListener(d);
        d.addMouseWheelListener(d);
        d.frame.addWindowListener(d);
        sseq.addListener(d);
        return d;
    }
    
    
    SpectralSequenceDisplay(SpectralSequence sseq,DisplaySettings settings){
        this.screenPaintTimer = new Timer(40,new updateScreenIfNeeded());
        this.transform = new AffineTransform();
        xscale = settings.getXScale();
        yscale = settings.getYScale();
        scale_aspect_ratio = yscale/xscale;
        scale_aspect_ratio_log = Math.log(scale_aspect_ratio)/Math.log(ZOOM_BASE);
        this.T_max = settings.T_max;        
        this.zoom = Math.log(xscale)/Math.log(ZOOM_BASE);        
        this.sseq = sseq;
        this.setupGradings();
        updateTransform();
        screenPaintTimer.start();
    }
    
    private boolean needsRepainting;
    
            
    class updateScreenIfNeeded implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if(needsRepainting){
                repaint();
                needsRepainting = false;
            }
        }
    }


    void requestRepaint(){
        needsRepainting = true;
    }
    
    @Override public void ping(int[] deg) {
        int[] topDeg = algToTopGrading(deg[0],deg[1]);
        if(topDeg[0] < getMaxX() && !mouseDown){
            repaint();
        }
        int[] s = algToTopGrading(selx, sely);
        if(deg[0] == s[0] && deg[1] == s[1]){
            setSelected(selx,sely); // updates text
        }
    }    
    
    
    private void setupGradings() {
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


    public SpectralSequenceDisplay<U> start(){
        this.frame.setVisible(true);
        return this;
    }
    
    void updateTransform(){
        scale = Math.pow(ZOOM_BASE,zoom);
        xscale = scale;
        yscale = scale * scale_aspect_ratio;
        xtickstep = 5;
        ytickstep = 5;
        
        for(double i = -zoom; i > 0; i -= TICK_STEP_LOG_BASE) xtickstep *= 2;
        for(double i = (-zoom - scale_aspect_ratio_log); i > 0; i -= TICK_STEP_LOG_BASE ) ytickstep *= 2;
        
        xgridstep = (xtickstep / 10 == 0) ? 1 : xtickstep / 10 ;
        ygridstep = (ytickstep / 10 == 0) ? 1 : ytickstep / 10 ;        
        try {
            inverseTransform = transform.createInverse();
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * getScreenX converts the x chart coordinate into an x screen coordinate. 
     * Make sure this is the inverse of getChartX(x) if you make any changes! 
     * @param x chart coordinate
     * @return screen coordinate
     */
    double getScreenX(double sx) {
        Point2D.Double p = new Point2D.Double(sx,0);
        return transform.transform(p,p).getX();
    }
    double getScreenY(double sy) {
        Point2D.Double p = new Point2D.Double(0,sy);
        return transform.transform(p,p).getY();
    }
    
    
    /**
     * getChartX converts the x screen coordinate into an x chart coordinate.
     * Make sure this is the inverse of getSX(cx) if you make any changes!
     * @param cx screen coordinate
     * @return chart coordinate
     */
    double getChartX(double cx) {
        Point2D.Double p = new Point2D.Double(cx,0);
        return inverseTransform.transform(p,p).getX();
    }
    
    double getChartY(double cy) {
        Point2D.Double p = new Point2D.Double(0,cy);
        try {
            return transform.inverseTransform(p,p).getY();
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    int getMinX(){
        int min_x_visible = (int) getChartX(-3*transform.getScaleX());
        return ( min_x_visible < 0 ) ? 0 : min_x_visible;
    }
    
    int getMinY(){
        int min_y_visible = (int) getChartY((getHeight() - 3*transform.getScaleY()));
        return ( min_y_visible < 0 ) ? 0 : min_y_visible;
    }
    
    int getMaxX(){
        return (int) getChartX(getWidth() + 3*transform.getScaleX()) + 10;
    }
    
    int getMaxY(){
        return (int) getChartY(3*transform.getScaleY()) + 10;
    }

    void setGridStroke(Stroke s){
        this.gridStroke = s;
    }

    void setLineStroke(Stroke s){
        this.lineStroke = s;
    }
    
    @Override public void mouseClicked(MouseEvent evt) {
        repaint();
        int x = (int) getChartX(evt.getX());
        int y = (int) getChartY(evt.getY());
        if(x >= 0 && y >= 0) {
            setSelected(x,y);
        } else {
            setSelected(-1,-1);
        }
    }
    @Override public void mousePressed(MouseEvent evt) { mouseDown = true; }
    @Override public void mouseReleased(MouseEvent evt) { mouseDown = false; }
    @Override public void mouseEntered(MouseEvent evt) { }
    @Override public void mouseExited(MouseEvent evt) { }

    @Override public void mouseMoved(MouseEvent evt){
        mousex = evt.getX();
        mousey = evt.getY();
    }

    @Override public void mouseDragged(MouseEvent evt){
        double dx = evt.getX() - mousex;
        double dy = evt.getY() - mousey;
        mousex = evt.getX();
        mousey = evt.getY();

        updateTransform();
        transform.translate(dx/transform.getScaleX(), dy/transform.getScaleY());
        
        repaint();
    }

    @Override public void mouseWheelMoved(MouseWheelEvent evt){
        double dZ = evt.getWheelRotation();
        double scale_factor = Math.pow(ZOOM_BASE,-dZ);
        transform.translate((1-scale_factor) * getChartX(mousex), (1-scale_factor) * getChartY(mousey));        
        transform.scale(scale_factor,scale_factor);
        zoom -= dZ;
        updateTransform();
        repaint();
    }
    
    
    boolean isVisible(SseqClass d){
        int[] deg = d.getDegree();
        for(int i = 2; i < minfilt.length; i++)
            if(deg[i] < minfilt[i] || deg[i] > maxfilt[i])
                return false;
        return true; //dec.isVisible(d);
    }

    int[] algToTopGrading(int x, int y){
        return new int[] {y,x+y};
    }
    
    int[] topToAlgGrading(int x, int y){
        return new int[] {y - x,x};
    }    
    
    
    private Map<SseqClass,Point2D> pos;
    

    
    /**
     * The key callback to paint the JFrame.
     * @param graphics A canvas provided by JFrame.
     */
    @Override public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        paintComponentHelper(graphics,transform);
        Graphics2D g = (Graphics2D) graphics;
        /* draw axes */
        int bmy = getHeight() - MARGIN_WIDTH;
        g.setColor(getBackground());
        g.fillRect(0, 0, MARGIN_WIDTH, getHeight());
        g.fillRect(0, bmy, getWidth(), MARGIN_WIDTH);
        g.setColor(Color.gray);
        g.drawLine(MARGIN_WIDTH, 0, MARGIN_WIDTH, bmy);
        g.drawLine(MARGIN_WIDTH, bmy, getWidth(), bmy);
        
        g.setColor(Color.black);
        FontMetrics metrics = g.getFontMetrics(g.getFont());  
        int max_x_visible = getMaxX();
        int max_y_visible = getMaxY();
        for(int x = 0; x <= max_x_visible; x += xtickstep) {
            Rectangle2D r = metrics.getStringBounds(String.valueOf(x), g);  
            g.drawString(String.valueOf(x),(float) (getScreenX(x+ 0.5)-r.getWidth()/2), (float) (getHeight() - MARGIN_WIDTH/2  + r.getHeight()/2));
        }
        for(int y = 0; y <= max_y_visible; y += ytickstep) {
            Rectangle2D r = metrics.getStringBounds(String.valueOf(y), g);  
            g.drawString(String.valueOf(y), (float) (MARGIN_WIDTH/2-r.getWidth()/2), (float)(getScreenY(y + 0.5) + r.getHeight()/2));
        }        
    }
    
    /**
     * We want to also produce pdf output. There are two issues with just using the paintComponent() method
     * @param graphics 
     */
    public void paintPDF(Graphics graphics){
            paintComponentHelper(graphics,transform);
    }    

    public void paintComponentHelper(Graphics graphics, AffineTransform transform){
        Graphics2D g = (Graphics2D) graphics;
        class DrawingCommands {            
            private void drawLine(double x1,double y1,double x2,double y2){
                g.draw(new Line2D.Double(x1, y1, x2, y2));
            }

            private void drawStructline(Structline sl){
                g.setColor(Color.BLACK);
                SseqClass source = sl.getSource();
                SseqClass target = sl.getTarget();
                if(pos.get(source)!=null && pos.get(target)!=null){//if(isVisible(u) && isVisible(d.dest)){
                    drawStructlineHelper(pos.get(source),pos.get(target));
                }
        //          g.setColor(sl.getColor());
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

            private void drawUnbasedStructline(U u,UnbasedLineDecoration<U> d){
                g.setColor(d.color);
        //        drawStructlineHelper(g,pos.get(u),getScreenP(d.dest));
            }    

            private void drawStructlineHelper(Point2D p1, Point2D p2){
                g.draw(AffineTransform.getTranslateInstance(transform.getScaleX()/2,  transform.getScaleY()/2).createTransformedShape(new Line2D.Double(p1,p2)));
            }

            private void shadeChartCell(Color color,double x,double y){
                g.setColor(color);
                g.fill(transform.createTransformedShape(new Rectangle2D.Double(x, y, 1, 1)));
            }            
            
            final Color[] colors = new Color[] {Color.BLACK, Color.BLUE,Color.GREEN,Color.ORANGE};
            
            private void drawClass(SseqClass c){
                g.setColor(c.getColor(0));
                AffineTransform saveTransform = g.getTransform();
                Point2D p = pos.get(c);
                if(p==null){
                    return;
                }
                g.translate(p.getX() - 3 + transform.getScaleX()/2,p.getY() - 3 + transform.getScaleY()/2);
                g.fill(c.getShape(0));
                g.setTransform(saveTransform);
            }                
        }
        
        DrawingCommands drawHelpers = new DrawingCommands();

        /* calculate visible region */
        int min_x_visible = getMinX();
        int min_y_visible = getMinY();
        int max_x_visible = getMaxX();
        int max_y_visible = getMaxY();
        
       
        /* draw grid  -- this needs to go first so that the uncomputed region is correctly blacked out 
         * (it looks ugly to have a light gray grid on a black background).
         */
        g.setStroke(gridStroke);
        FontMetrics metrics = g.getFontMetrics(g.getFont());  
        AffineTransform tempTransform = new AffineTransform();
        Point2D tempPoint = new Point2D.Double();
        for(int x = 0; x <= max_x_visible; x += xgridstep) {
            g.setColor(Color.lightGray);
            /* Offset x component by xgridstep/2 in order to keep dots centered in grid. */
            g.draw(transform.createTransformedShape(new Line2D.Double(x - xgridstep/2, min_y_visible , x - xgridstep/2,max_y_visible)));
            g.setColor(Color.black);
            if(x % xtickstep == 0) {
                Rectangle2D r = metrics.getStringBounds(String.valueOf(x), g);                                      
                tempTransform.setToTranslation(-r.getWidth()/2,TICK_GAP);
                tempTransform.concatenate(transform);
                tempPoint.setLocation(x+0.5, 0);
                tempTransform.transform(tempPoint, tempPoint);
                g.drawString(String.valueOf(x), (float) tempPoint.getX(), (float) tempPoint.getY());           
            }
        }
        for(int y = 0; y <= max_y_visible; y += ygridstep) {        
            g.setColor(Color.lightGray);
            /* Offset x start point by xgridstep/2 in order to match up with vertical grid lines. */
            g.draw(transform.createTransformedShape(new Line2D.Double(min_x_visible - xgridstep/2, y  , max_x_visible, y)));
            g.setColor(Color.black);  
            if(y % ytickstep == 0){
                Rectangle2D r = metrics.getStringBounds(String.valueOf(y), g);                  
                tempTransform.setToTranslation(-TICK_GAP,r.getHeight()/2);
                tempTransform.concatenate(transform);
                tempPoint.setLocation(0, y+0.5);
                tempTransform.transform(tempPoint, tempPoint);
                g.drawString(String.valueOf(y), (float) tempPoint.getX(), (float) tempPoint.getY());                 
            }
        }    

        g.setStroke(lineStroke);
        /* assign classes a location; at this point we definitively decide what's visible */
        Set<SseqClass> frameVisibles = new HashSet<>();
        pos = new HashMap<>();
        for(int x = min_x_visible; x <= max_x_visible && x <= T_max + 3; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cellState = sseq.getState(algToTopGrading(x,y));
                Color cellColor;
                cellColor = STATE_COLORS.get(cellState);
                
                if(cellColor!=transparent){
                    drawHelpers.shadeChartCell(cellColor,x,y);                
                }

                if(!(cellState == MultigradedVectorSpace.STATE_PARTIAL || cellState == MultigradedVectorSpace.STATE_DONE)){
                    continue;
                }
                
                Collection<SseqClass> classes = sseq.getClasses(algToTopGrading(x,y));
//                System.out.println("classes: " + sseq.getClasses(multideg(15,1)).size());
                int visible;
                synchronized(classes) {
                    visible = (int) classes.stream().filter((d) -> (isVisible(d))).map((d) -> {
                        frameVisibles.add(d);
                        return d;
                    }).count();
                    double offset = -10 * (visible-1) / 2 * scale;
                    for(SseqClass d : classes) { 
                        if(frameVisibles.contains(d)) {
                            Point2D newpos = new Point2D.Double( getScreenX(x) + offset, getScreenY(y) - offset/2 );
                            pos.put(d, newpos);
                            offset += 10 * scale;
                        }
                    }
                }
            }
        }
        g.fill(transform.createTransformedShape(new Rectangle.Double(T_max + 1,min_y_visible,max_x_visible,max_y_visible)));
        drawHelpers.shadeChartCell(Color.orange,selx,sely);

        /* draw decorations */
        frameVisibles.forEach((SseqClass u) -> {
            /* based */
            u.getStructlines().forEach((s) ->
                drawHelpers.drawStructline(s)
            );
            
//            /* unbased */
//            dec.getUnbasedStructlineDecorations(u).forEach((d) -> 
//                drawUnbasedStructline(g,u,d)
//            );
        });

        /* draw dots */
        g.setColor(Color.black);
        frameVisibles.forEach((c) -> 
            drawHelpers.drawClass(c)
        );

    }

    @SuppressWarnings("fallthrough")
    void setSelected(int x, int y)
    {
        selx = x;
        sely = y;
        repaint();

        if(textarea == null) return;

        String ret = "";
        switch(sseq.getState(algToTopGrading(x,y))) {
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
                Collection<SseqClass> classes = sseq.getClasses(algToTopGrading(x,y));
                for(SseqClass c : classes) if(isVisible(c)) {
                    ret += "\n" + c.toString();
                    ret += "\n" + c.extraInfo();
                    ret += "\n";
                }
        }

        textarea.setText(ret);
    }
   


    @Override
    public void windowOpened(WindowEvent e) {
        updateTransform();
        transform.translate(0, getHeight());        
        transform.scale(xscale * BLOCK_FACTOR, - yscale * BLOCK_FACTOR); 
        transform.translate(1, 1/scale_aspect_ratio);          
        updateTransform();
        repaint();
    }

    @Override
    public void windowClosing(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}
    
    @Override
    public void windowDeiconified(WindowEvent e) {
        updateTransform();
        repaint();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        updateTransform();
        repaint();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}

}

class ControlPanel2D extends Box {

    ControlPanel2D(final SpectralSequenceDisplay<?> parent){
        super(BoxLayout.Y_AXIS);

        setup_gui(parent);
    }

    private void setup_gui(final SpectralSequenceDisplay<?> parent){

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

