package res.frontend;

import res.algebra.*;
import res.transform.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import res.Config;

public class ResDisplay<U extends MultigradedElement<U>> extends JPanel 
        implements PingListener, MouseMotionListener, MouseListener, MouseWheelListener, ComponentListener
{
    final static int DEFAULT_MINFILT = 0;
    final static int DEFAULT_MAXFILT = 100;
    
    final static int ZOOMINTERVAL = 50;
    final static double ZOOM_BASE = 1.1;
    final static int DEFAULT_WINDOW_WIDTH = 1600;
    final static int DEFAULT_WINDOW_HEIGHT = 800;
    final static int MARGIN_WIDTH = 30;
    

    
    

    

    Decorated<U, ? extends MultigradedVectorSpace<U>> dec;
    MultigradedVectorSpace<U> under;

    int[] minfilt;
    int[] maxfilt;
    
    private double yscale = 1; 
    private final double yscale_log = Math.log(yscale)/Math.log(ZOOM_BASE);
    double viewx = MARGIN_WIDTH;
    double viewy = - MARGIN_WIDTH;
    double zoom = 0;
    double block_width = 30;
    // Derived in updateOffsets():
    double scale;
    double xoffset;
    double yoffset;
    double block_height;
    int block_width_int;
    int half_block_width_int;
    int block_height_int;
    int half_block_height_int;    
    
    int selx = -1;
    int sely = -1;
    int mousex = -1;
    int mousey = -1;
    int width = 0;
    int height = 0;
    
    public int getOldWidth(){
        return width;
    }    
    
    public int getOldHeight(){
        return height;
    }

    
    JTextArea textarea = null;

    private ResDisplay(Decorated<U, ? extends MultigradedVectorSpace<U>> dec)
    {
        setBackend(dec);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addComponentListener(this);
        updateOffsets();
    }

    private void setBackend(Decorated<U, ? extends MultigradedVectorSpace<U>> dec)
    {
        if(under != null)
            under.removeListener(this);
        this.dec = dec;
        under = dec.underlying();
        under.addListener(this);

        int i;
        if(minfilt == null) {
            i = 0;
            minfilt = new int[under.num_gradings()];
            maxfilt = new int[under.num_gradings()];
        } else if(minfilt.length != under.num_gradings()) {
            i = minfilt.length;
            minfilt = Arrays.copyOf(minfilt, under.num_gradings());
            maxfilt = Arrays.copyOf(maxfilt, under.num_gradings());
        } else return;

        for(; i < minfilt.length; i++) {
            minfilt[i] = DEFAULT_MINFILT;
            maxfilt[i] = DEFAULT_MAXFILT;
        } 
    }

    /**
     * getcx converts the x chart coordinate into a screen coordinate.
     * @param x chart coordinate
     * @return screen coordinate
     */
    private int getcx(int x) {
        return (int) (block_width * (x-0.5) +  xoffset);
    }
    private int getcy(int y) {
        return getHeight() - (int) (block_height * (y-0.5) - yoffset);
    }
    
    /**
     * getx converts the screen coordinate into a chart coordinate.
     * Make sure this is the inverse of getcx!!
     * @param cx screen coordinate
     * @return chart coordinate
     */
    private int getx(int cx) {
        double x = cx - xoffset;
        x /= block_width;
        x += 0.5;
        return (int) x;
    }
    private int gety(int cy) {
        double y = cy - getHeight() - yoffset;
        y = -y;
        y /= block_height;
        y += 0.5;
        return (int) y;
    }

    private boolean isVisible(U d)
    {
        int[] deg = d.deg();
        for(int i = 2; i < minfilt.length; i++)
            if(deg[i] < minfilt[i] || deg[i] > maxfilt[i])
                return false;
        return dec.isVisible(d);
    }

    private int[] multideg(int x, int y)
    {
        return new int[] {y,x+y};
    }

    private void drawLine(Graphics g,int x,int y,int z,int w){
        g.drawLine(x, y, z, w);
    }

    private void fillRect(Graphics g,int x,int y){
        g.fillRect(getcx(x), getcy(y), block_width_int, block_height_int);
    }

    private void drawString(Graphics g,String s,int x,int y){
        g.drawString(s, x - half_block_width_int, y + half_block_height_int);
    }
    
    private TreeMap<U,int[]> pos;
    
    private void drawDot(Graphics g, U u){
        g.setColor(Color.black);
        int p[] = pos.get(u);
        g.fillOval(p[0]-3+ half_block_width_int, p[1]-3-half_block_height_int, 6, 6);
    }
    
    @Override public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        
        int min_x_visible = getx(-3*block_width_int);
        int min_y_visible = gety((getHeight() + 3*block_height_int));
        if(min_x_visible < 0) min_x_visible = 0;
        if(min_y_visible < 0) min_y_visible = 0;
        int max_x_visible = getx(getWidth() + 3*block_width_int);
        int max_y_visible = gety(-3*block_height_int);
        int max_visible = (max_x_visible < max_y_visible) ? max_y_visible : max_x_visible;
        

        /* draw grid */
        int xtickstep = 5;
        int ytickstep = 5;
        for(double i = -zoom; i > 0; i -= ZOOMINTERVAL) xtickstep *= 2;
        for(double i = (-zoom - yscale_log); i > 0; i -= ZOOMINTERVAL*yscale) ytickstep *= 2;
        for(int x = 0; x <= max_visible; x++) {
            g.setColor(Color.lightGray);
            drawLine(g,getcx(x), getcy(0), getcx(x), 0);
            drawLine(g,getcx(0), getcy(x), getWidth(), getcy(x));

            g.setColor(Color.black);
            // Draw axes ticks. Not such a fan of these magic numbers...
            if(x % xtickstep == 0) {
                g.drawString(String.valueOf(x), getcx(x)-8, (int)(getcy(0)+(18.*block_height/30.))+17);
            }
            if(x % ytickstep == 0){
                g.drawString(String.valueOf(x), getcx(0)-(int)(19.*block_width/30.)-19, (getcy(x)+5));
            }
        }
        
        /* assign dots a location; at this point we definitively decide what's visible */
        Set<U> frameVisibles = new TreeSet<>();
        pos = new TreeMap<>();
        for(int x = min_x_visible; x <= max_x_visible; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cx = getcx(x);
                int cy = getcy(y);
                switch(under.getState(multideg(x,y))) {
                    case MultigradedVectorSpace.STATE_NOT_COMPUTED:
                        g.setColor(Color.black);
                        fillRect(g,x,y);
                        continue;
                    case MultigradedVectorSpace.STATE_STARTED:
                        g.setColor(Color.darkGray);
                        fillRect(g,x,y);
                        continue;
                    case MultigradedVectorSpace.STATE_PARTIAL:
                        g.setColor(Color.yellow);
                        fillRect(g,x,y);
                        break;
                    case MultigradedVectorSpace.STATE_VANISHES:
                        g.setColor(Color.lightGray);
                        fillRect(g,x,y);
                        continue;
                    default:
                        break;
                }
                if(x == selx && y == sely) {
                    g.setColor(Color.orange);
                    fillRect(g,x,y);
                }
        
                Collection<U> gens = under.gens(multideg(x,y));

                int visible = 0;
                synchronized(gens) {
                    visible = gens.stream().filter((d) -> (isVisible(d))).map((d) -> {
                        frameVisibles.add(d);
                        return d;
                    }).map((_item) -> 1).reduce(visible, Integer::sum);
                    int offset = -5 * (visible-1) / 2;
                    for(U d : gens) { 
                        if(frameVisibles.contains(d)) {
                            int[] newpos = new int[] { cx + offset, cy - offset/2 };
                            pos.put(d, newpos);
                            offset += 5;
                        }
                    }
                }
            }
        }

        /* draw decorations */
        frameVisibles.forEach((u) -> {
            int[] p1 = pos.get(u);

            /* based */
            dec.getBasedLineDecorations(u).stream().filter((d) -> !(! frameVisibles.contains(d.dest))).map((d) -> {
                g.setColor(d.color);
                return pos.get(d.dest);
            }).forEachOrdered((p2) -> {
                drawLine(g,p1[0] + half_block_width_int, p1[1] - half_block_height_int, p2[0] + half_block_width_int, p2[1] - half_block_height_int);
            });
            
            /* unbased */
            dec.getUnbasedLineDecorations(u).stream().map((d) -> {
                g.setColor(d.color);
                return d;
            }).forEachOrdered((d) -> {
                int destx = getcx(d.dest[0]);
                int desty = getcy(d.dest[1]);
                drawLine(g,p1[0] + half_block_width_int, p1[1] - half_block_height_int, destx, desty);
            });
        });

        /* draw dots */
        g.setColor(Color.black);
        for(U u : frameVisibles){
            drawDot(g,u);
        }

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
            g.drawString(String.valueOf(x), getcx(x)-8, getHeight()-10);
        }
        for(int y = 0; y <= max_visible; y += ytickstep) {
            g.drawString(String.valueOf(y), 10, (getcy(y)+5));
        }

    }
    
    void updateOffsets(){
        block_width = 1 + (29.0 * Math.pow(ZOOM_BASE,zoom));
        scale = block_width/30.0;
        xoffset = (scale * (viewx - MARGIN_WIDTH) ) + MARGIN_WIDTH;
        yoffset = (scale * (viewy + MARGIN_WIDTH) ) - MARGIN_WIDTH;
        block_width_int = (int)block_width;
        half_block_width_int = (int)(block_width/2);
        block_height = (block_width*yscale);
        block_height_int = (int)(block_height);
        half_block_height_int = (int)(block_height/2);
    }

    @SuppressWarnings("fallthrough")
    void setSelected(int x, int y)
    {
        selx = x;
        sely = y;
        repaint();

        if(textarea == null) return;

        String ret = "";
        switch(under.getState(multideg(x,y))) {
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
                Collection<U> gens = under.gens(multideg(x,y));
                for(U d : gens) if(isVisible(d)) {
                    ret += "\n" + d.toString();
                    ret += "\n" + d.extraInfo();
                    ret += "\n";
                }
        }

        textarea.setText(ret);
    }

    
    @Override public void componentResized(ComponentEvent e){
    }
     @Override public void componentMoved(ComponentEvent e) {}
     @Override public void componentShown(ComponentEvent e) {}
     @Override public void componentHidden(ComponentEvent e) {}

    @Override public void mouseClicked(MouseEvent evt)
    {
        int x = getx(evt.getX());
        int y = gety(evt.getY());
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
        int dx = evt.getX() - mousex;
        int dy = evt.getY() - mousey;

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

    public static <U extends MultigradedElement<U>> ResDisplay<U> constructFrontend(Decorated<U, ? extends MultigradedVectorSpace<U>> back) 
    {
        JFrame fr = new JFrame("Resolution");
        fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        fr.setSize(DEFAULT_WINDOW_WIDTH,DEFAULT_WINDOW_HEIGHT);
        ResDisplay<U> d = new ResDisplay<>(back);
        fr.getContentPane().add(d, BorderLayout.CENTER);
        
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        fr.setVisible(true);
        return d;
    }
    
    public void setScale(double xscale,double yscale){
        this.yscale = yscale/xscale;
        this.zoom = Math.log(xscale)/Math.log(ZOOM_BASE);
        updateOffsets();
    }

}

class ControlPanel2D extends Box {

    ControlPanel2D(final ResDisplay<?> parent)
    {
        super(BoxLayout.Y_AXIS);

        setup_gui(parent);
    }

    void setup_gui(final ResDisplay<?> parent)
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

