/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.frontend;

import res.spectralsequencediagram.nodes.Node;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import res.algebra.MultigradedElement;
import res.algebra.MultigradedVectorSpace;
import res.algebra.PingListener;
import res.spectralsequencediagram.*;
import res.transform.Decorated;

/**
 *
 * @author Hood
 * @param <U>
 */
public class SpectralSequenceCanvas<U extends MultigradedElement<U>> 
        extends JPanel
        implements PingListener {
    SpectralSequenceDisplay display;
    SpectralSequence sseq;    
    
    int page;
    int page_index;
    List<Integer> page_list;

    final static Color transparent = new Color(0, 0, 0, 0);
    final static BevelArrows.CurvedArrow curvedArrow = new BevelArrows.CurvedArrow();
    final static AffineTransform ID_TRANSFORM = AffineTransform.getRotateInstance(0);

    
    int T_max;
    boolean x_full_range; // Allow negative x?
    boolean y_full_range; // Allow negative y?    
    
//  Coordinate transformations:
//      The method updateTransform() is responsible for ensuring the following AffineTransforms have the 
//      relationships described below. The method initializeTransform() sets up the initial values.
//      The window handler windowOpened() in SpectralSequenceDisplay calls intializeTransform().
//      because we don't know the height of the window until it is open, and we need that info
//      in order to set up the origin.
    
    final static double INITIAL_SCALE = 30;
//  initialTransform and inverseInitialTransform dosn't change after the window opens. It just includes INITIAL_SCALE, 
//  inverts the y axis, and translates to put (0,0) in the right place.
    AffineTransform initialTransform, inverseInitialTransform;
//  transform and inverseTransform contain the transformation from whatever scaling and panning the user does.
    AffineTransform transform, inverseTransform;
//  fullTransform is the composition transform o initialTransform
    AffineTransform fullTransform, inverseFullTransform;
//  This is the transformation to apply to nodes. Only the scale matters.
    AffineTransform nodeTransform;
    
//  
    final static double ZOOM_BASE = 1.1;        
    double zoom = 0;


//  Derived from updateTransform()
    double scale, xscale, yscale;  
    private int xtickstep;
    private int ytickstep;
    private int xgridstep;
    private int ygridstep;
    

    final static int MARGIN_WIDTH = 30;    
    final static int TICK_STEP_LOG_BASE = 10;
    final static double TICK_GAP = 20;    
    
//  These 
    final double scale_aspect_ratio; 
    final double scale_aspect_ratio_log;
    
    int selx = -10000;
    int sely = selx;    
    
    final static Map<Integer,Color> STATE_COLORS = new HashMap();
    static {
        STATE_COLORS.put(MultigradedVectorSpace.STATE_NOT_COMPUTED, Color.black);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_STARTED,      Color.darkGray);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_PARTIAL,      Color.yellow);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_VANISHES,     Color.lightGray);
        STATE_COLORS.put(MultigradedVectorSpace.STATE_DONE,         transparent);
    }


    
    private Stroke gridStroke = new BasicStroke();
    private Color gridColor = Color.lightGray;
    private Stroke lineStroke = new BasicStroke();    
    
    SpectralSequenceCanvas(SpectralSequenceDisplay d, SpectralSequence sseq, DisplaySettings settings){
        this.initialTransform = new AffineTransform();
        this.inverseInitialTransform = new AffineTransform();
        this.transform = new AffineTransform();
        this.inverseTransform = new AffineTransform();
        this.fullTransform = new AffineTransform();
        this.inverseFullTransform = new AffineTransform();
        this.nodeTransform = new AffineTransform();
        
        sseq.addListener(this);
        this.display = d;
        xscale = settings.getXScale();
        yscale = settings.getYScale();
        scale_aspect_ratio = yscale/xscale;
        scale_aspect_ratio_log = Math.log(scale_aspect_ratio)/Math.log(ZOOM_BASE);
        this.T_max = settings.T_max;        
        this.x_full_range = settings.x_full_range;
        this.y_full_range = settings.y_full_range;
        this.page_list = sseq.getPageList();
        this.zoom = Math.log(xscale)/Math.log(ZOOM_BASE);        
        this.sseq = sseq;
        updateTransform();
    }    


    public SpectralSequenceCanvas setPage(int page){
        this.page = page;
        return this;
    }
    
    public int getPage(){
        return this.page;
    }
        
    
    void initializeTransform(){
        updateTransform();
        initialTransform.translate(0, getHeight());        
        initialTransform.scale(xscale * INITIAL_SCALE, - yscale * INITIAL_SCALE); 
        initialTransform.translate(1, 1/scale_aspect_ratio);
        try {
            inverseInitialTransform = initialTransform.createInverse();
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }
        updateTransform();      
    }    
    
   /** 
    * updateTransform makes sure that all the transformation matrices have the intended relationship.
    * The variables zoom and transform are the inputs.
    * Sets the variables scale, xscale & yscale, xtickstep & ytickstep, xgridstep & ygridstep
    * inverseTransform, fullTransform, inverseTransform, and nodeTransform
    * If you want to change any of these things, either change zoom, change transform, or change this function.
    */
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

        fullTransform.setTransform(initialTransform);
        fullTransform.concatenate(transform);
        inverseFullTransform.setTransform(inverseInitialTransform);
        inverseFullTransform.preConcatenate(inverseTransform); 
        
        if(transform.getScaleX() >= 1){
            nodeTransform.setTransform(transform);
        } else {
            nodeTransform.setToIdentity();
        }
    }
    
    
    
    /**
     * getScreenX converts the x chart coordinate into an x screen coordinate. 
     * Make sure this is the inverse of getChartX(x) if you make any changes! 
     * @param x chart coordinate
     * @return screen coordinate
     */
    double getScreenX(double sx) {
        Point2D.Double p = new Point2D.Double(sx, 0);
        fullTransform.transform(p,p);
        return p.getX();
    }
    
    double getScreenY(double sy) {
        Point2D.Double p = new Point2D.Double(0, sy);
        fullTransform.transform(p,p);
        return p.getY();
    }
    
    
    /**
     * getChartX converts the x screen coordinate into an x chart coordinate.
     * Make sure this is the inverse of getSX(cx) if you make any changes!
     * @param cx screen coordinate
     * @return chart coordinate
     */
    double getChartX(double cx) {
        Point2D.Double p = new Point2D.Double(cx, 0);
        inverseFullTransform.transform(p, p);
        return p.getX();
    }
    
    double getChartY(double cy) {
        Point2D.Double p = new Point2D.Double(0, cy);
        inverseFullTransform.transform(p, p);
        return p.getY();        
    }
    
    int getMinX(){
        int min_x_visible = (int) getChartX( -3 * fullTransform.getScaleX());
        if(!x_full_range && min_x_visible < 0){
            min_x_visible = 0;
        }
        return min_x_visible;
    }
    
    int getMinY(){
        int min_y_visible = (int) getChartY((getHeight() - 3*fullTransform.getScaleY()));
        if(!y_full_range && min_y_visible < 0){
            min_y_visible = 0;
        }        
        return  min_y_visible;
    }
    
    int getMaxX(){
        return (int) getChartX(getWidth() + 3*fullTransform.getScaleX()) + 10;
    }
    
    int getMaxY(){
        return (int) getChartY(3*fullTransform.getScaleY()) + 10;
    }

    void setGridStroke(Stroke s){
        this.gridStroke = s;
    }

    void setLineStroke(Stroke s){
        this.lineStroke = s;
    }
    
    
    public void translateCanvas(double dx, double dy){
        transform.translate(dx/fullTransform.getScaleX(), -dy/fullTransform.getScaleY());
        updateTransform();
    }
    
    public void zoomCanvasAround(double dZ, double px, double py){
        double scale_factor = Math.pow(ZOOM_BASE, -dZ);
        transform.translate((1 - scale_factor) * (px), (1 - scale_factor) * (py));        
        transform.scale(scale_factor, scale_factor);
        zoom -= dZ;
        updateTransform(); 
    }
    
    public boolean isVisible(SseqClass d){
        int x = d.getDegree()[0];
        int y = d.getDegree()[1];   
//        int[] p = topToAlgGrading(x,y);
//        x = p[0];
//        y = p[1];
        return getMinX() <= x && x <= getMaxX() && getMinY() <= y && y <= getMaxY();
    }

//    int[] algToTopGrading(int x, int y){
//        return new int[] {y, x + y};
//    }
//    
//    int[] topToAlgGrading(int x, int y){
//        return new int[] {y - x, x};
//    }    
    
    /**
     * The key callback to paint the JFrame.
     * @param graphics A canvas provided by JFrame.
     */
    @Override public void paintComponent(Graphics graphics)
    {
        updateTransform();
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        
        drawGrid(g);        
        paintComponentHelper(g);
        
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        g.setRenderingHint( RenderingHints.KEY_STROKE_CONTROL,
                            RenderingHints.VALUE_STROKE_PURE);
        
        /* draw side fill and axes */
        int bmy = getHeight() - MARGIN_WIDTH;
        g.setColor(getBackground());
        g.fillRect(0, 0, MARGIN_WIDTH, getHeight());
        g.fillRect(0, bmy, getWidth(), MARGIN_WIDTH);
        g.setColor(Color.gray);
        g.drawLine(MARGIN_WIDTH, 0, MARGIN_WIDTH, bmy);
        g.drawLine(MARGIN_WIDTH, bmy, getWidth(), bmy);
        
        g.setColor(Color.black);
        FontMetrics metrics = g.getFontMetrics(g.getFont());  
        for(int x = (getMinX()/xtickstep)*xtickstep; x <= getMaxX(); x += xtickstep) {
            Rectangle2D r = metrics.getStringBounds(String.valueOf(x), g);  
            g.drawString(String.valueOf(x),(float) (getScreenX(x+ 0.5) - r.getWidth()/2), (float) (getHeight() - MARGIN_WIDTH/2 + r.getHeight()/2));
        }
        for(int y = (getMinY()/ytickstep)*ytickstep; y <= getMaxY(); y += ytickstep) {
            Rectangle2D r = metrics.getStringBounds(String.valueOf(y), g);  
            g.drawString(String.valueOf(y), (float) (MARGIN_WIDTH/2 - r.getWidth()/2), (float)(getScreenY(y + 0.5) + r.getHeight()/2));
        }
        
    }
    

    private void drawGrid(Graphics2D g){
        int min_x_visible = getMinX();
        int min_y_visible = getMinY();
        int max_x_visible = getMaxX();
        int max_y_visible = getMaxY();   
        g.setStroke(gridStroke);
        FontMetrics metrics = g.getFontMetrics(g.getFont());  
        AffineTransform tempTransform = new AffineTransform();
        Point2D tempPoint = new Point2D.Double();
        for(int x = min_x_visible; x <= max_x_visible; x += xgridstep) {
            g.setColor(gridColor);
            /* Offset x component by xgridstep/2 in order to keep dots centered in grid. */
            g.draw(fullTransform.createTransformedShape(new Line2D.Double(x - xgridstep/2, min_y_visible, x - xgridstep/2, max_y_visible)));
            g.setColor(Color.black);
            if(x % xtickstep == 0) {
                Rectangle2D r = metrics.getStringBounds(String.valueOf(x), g);                                      
                tempTransform.setToTranslation(-r.getWidth()/2, TICK_GAP);
                tempTransform.concatenate(fullTransform);
                tempPoint.setLocation(x + 0.5, 0);
                tempTransform.transform(tempPoint, tempPoint);
                g.drawString(String.valueOf(x), (float) tempPoint.getX(), (float) tempPoint.getY());           
            }
        }
        for(int y = min_y_visible; y <= max_y_visible; y += ygridstep) {        
            g.setColor(gridColor);
            /* Offset x start point by xgridstep/2 in order to match up with vertical grid lines. */
            g.draw(fullTransform.createTransformedShape(new Line2D.Double(min_x_visible - xgridstep/2, y, max_x_visible, y)));
            g.setColor(Color.black);  
            if(y % ytickstep == 0){
                Rectangle2D r = metrics.getStringBounds(String.valueOf(y), g);                  
                tempTransform.setToTranslation(-TICK_GAP, r.getHeight()/2);
                tempTransform.concatenate(fullTransform);
                tempPoint.setLocation(0, y + 0.5);
                tempTransform.transform(tempPoint, tempPoint);
                g.drawString(String.valueOf(y), (float) tempPoint.getX(), (float) tempPoint.getY());                 
            }
        }
    }
    
    public void paintComponentHelper(Graphics2D g){
        class DrawingCommands {

            private void drawEdge(SseqEdge edge){
                if(!edge.drawOnPageQ(page)){
                    return;
                }
                    
                g.setColor(edge.getColor(page));
                SseqClass source = edge.getSource();
                SseqClass target = edge.getTarget();
                Node source_node = source.getNode(page);
                Node target_node = target.getNode(page);
                if( isVisible(source) && source.drawOnPageQ(page) && isVisible(target) && target.drawOnPageQ(page) ){
                    Node.drawLine(g, nodeTransform, source_node, target_node);
                }
            }

            private void shadeChartCell(Color color, double x, double y){
                g.setColor(color);
                g.fill(fullTransform.createTransformedShape(new Rectangle2D.Double(x, y, 1, 1)));
            }            
            
            final Color[] colors = new Color[] {Color.BLACK, Color.BLUE, Color.GREEN, Color.ORANGE};
            
            private void drawClass(SseqClass c){
                c.getNode(page).draw(g, nodeTransform);
            }                
        }
        
        DrawingCommands drawHelpers = new DrawingCommands();

        /* calculate visible region */
        int min_x_visible = getMinX();
        int min_y_visible = getMinY();
        int max_x_visible = getMaxX();
        int max_y_visible = getMaxY();
        
        g.setStroke(lineStroke);
        g.setColor(Color.black);

        /* draw axes */
        g.draw(fullTransform.createTransformedShape(new Line2D.Double(min_x_visible, 0, max_x_visible, 0)));
        g.draw(fullTransform.createTransformedShape(new Line2D.Double(0, min_y_visible, 0, max_y_visible)));        

        /* assign classes a location; at this point we definitively decide what's visible */
        Set<SseqClass> frameVisibles = new HashSet<>();
        for(int x = min_x_visible; x <= max_x_visible && x <= T_max + 3; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cellState = sseq.getState(x, y);
                Color cellColor;
                cellColor = STATE_COLORS.get(cellState);
                
                if(cellColor!=transparent){
                    drawHelpers.shadeChartCell(cellColor, x, y);                
                }

                if(!(cellState == MultigradedVectorSpace.STATE_PARTIAL || cellState == MultigradedVectorSpace.STATE_DONE)){
                    continue;
                }
                
                Collection<SseqClass> classes = sseq.getClasses(x, y, page);
                int visible = 0;
                synchronized(classes) {
                    for(SseqClass d : classes){
                        if(isVisible(d) && d.drawOnPageQ(page) ){
                            frameVisibles.add(d);
                            d.getNode(page).visibleQ = true;                            
                            visible++;
                        } else {
                            d.getNode(page).visibleQ = false;   
                        }
                    }
                    double offset = -10 * (visible - 1) / 2 * scale;
                    for(SseqClass d : classes) { 
                        if(frameVisibles.contains(d)) {
                            d.getNode(page).setPosition(
                                    getScreenX(x) + offset + fullTransform.getScaleX()/2,
                                    getScreenY(y) - offset/2 + fullTransform.getScaleY()/2);
                            offset += 10 * scale;
                        }
                    }
                }
            }
        }
        g.fill(fullTransform.createTransformedShape(new Rectangle.Double(T_max + 1, min_y_visible, max_x_visible, max_y_visible)));
        drawHelpers.shadeChartCell(Color.orange, selx, sely);

        /* draw edges */
        for(SseqClass u : frameVisibles) {
            for(Structline s : u.getStructlines()){
                drawHelpers.drawEdge(s);
            }

            for(Differential s : u.getOutgoingDifferentials()){
                drawHelpers.drawEdge(s);
            }
        }

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

        if(display.textarea == null) return;

        String ret = "";
        switch(sseq.getState(x,y)) {
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
                ret += "Bidegree (" + x + "," + y + ")\n";
                Collection<SseqClass> classes = sseq.getClasses(x, y, page);
                for(SseqClass c : classes) if(isVisible(c) && c.getPage() >= page ) {
                    ret += "\n" + c.toString();
                    ret += "\n" + c.extraInfo();
                    ret += "\n";
                }
        }

        display.textarea.setText(ret);
    }
   
    public void incrementPage(){
        if(page_index > 0){
            page_index --;
        }
        page = page_list.get(page_index);
        repaint();
    }
    
    public void decrementPage(){
        if(page_index < page_list.size() - 1){
            page_index ++;
        }
        page = page_list.get(page_index);
        repaint();
    }    

    @Override
    public void ping(int[] deg) {
        if(deg[0] < getMaxX() && !display.mouseDown){
            repaint();
        }
        if(deg[0] == selx && deg[1] == sely){
            setSelected(selx,sely); // updates text
        }
    }
    
}
