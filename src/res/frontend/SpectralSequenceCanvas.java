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
import res.transform.UnbasedLineDecoration;

/**
 *
 * @author Hood
 */
public class SpectralSequenceCanvas<U extends MultigradedElement<U>> 
        extends JPanel
        implements PingListener {
    SpectralSequenceDisplay display;
    
    int page;
    int page_index;
    List<Integer> page_list;
        
    final static int MARGIN_WIDTH = 30;
    
    final static double ZOOM_BASE = 1.1;    
    final static int TICK_STEP_LOG_BASE = 10;
    final static double TICK_GAP = 20;
    final static double BLOCK_FACTOR = 30;

    final static Color transparent = new Color(0, 0, 0, 0);
    final static BevelArrows.CurvedArrow curvedArrow = new BevelArrows.CurvedArrow();
    final static AffineTransform ID_TRANSFORM = AffineTransform.getRotateInstance(0);
    AffineTransform transform;
    AffineTransform inverseTransform = new AffineTransform();
    
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
    
    Decorated<U, ? extends MultigradedVectorSpace<U>> dec;
    MultigradedVectorSpace<U> under;
    SpectralSequence sseq;

    int T_max;
    boolean x_full_range; // Allow negative x?
    boolean y_full_range; // Allow negative y?
    
    final double scale_aspect_ratio; 
    final double scale_aspect_ratio_log;
    double zoom = 0;
    // Derived in updateOffsets():
    double scale, xscale, yscale;  
    private int xtickstep;
    private int ytickstep;
    private int xgridstep;
    private int ygridstep;
    
    private Stroke gridStroke = new BasicStroke();
    private Stroke lineStroke = new BasicStroke();    
    
    SpectralSequenceCanvas(SpectralSequenceDisplay d, SpectralSequence sseq, DisplaySettings settings){
        sseq.addListener(this);
        this.display = d;
        this.transform = new AffineTransform();
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
        Point2D.Double p = new Point2D.Double(sx, 0);
        return transform.transform(p, p).getX();
    }
    double getScreenY(double sy) {
        Point2D.Double p = new Point2D.Double(0, sy);
        return transform.transform(p, p).getY();
    }
    
    
    /**
     * getChartX converts the x screen coordinate into an x chart coordinate.
     * Make sure this is the inverse of getSX(cx) if you make any changes!
     * @param cx screen coordinate
     * @return chart coordinate
     */
    double getChartX(double cx) {
        Point2D.Double p = new Point2D.Double(cx, 0);
        return inverseTransform.transform(p, p).getX();
    }
    
    double getChartY(double cy) {
        Point2D.Double p = new Point2D.Double(0, cy);
        try {
            return transform.inverseTransform(p, p).getY();
        } catch (NoninvertibleTransformException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
            return 0;
        }
    }
    
    int getMinX(){
        int min_x_visible = (int) getChartX(-3*transform.getScaleX());
        if(!x_full_range && min_x_visible < 0){
            min_x_visible = 0;
        }
        return min_x_visible;
    }
    
    int getMinY(){
        int min_y_visible = (int) getChartY((getHeight() - 3*transform.getScaleY()));
        if(!y_full_range && min_y_visible < 0){
            min_y_visible = 0;
        }        
        return  min_y_visible;
    }
    
    int getMaxX(){
        return (int) getChartX(getWidth() + 3*transform.getScaleX()) + 10;
    }
    
    int getMaxY(){
        return (int) getChartY(3*transform.getScaleY()) + 20;
    }

    void setGridStroke(Stroke s){
        this.gridStroke = s;
    }

    void setLineStroke(Stroke s){
        this.lineStroke = s;
    }
    
    
    public void translateCanvas(double dx, double dy){
        transform.translate(dx/transform.getScaleX(), -dy/transform.getScaleY());
    }
    
    public void zoomCanvasAround(double dZ, double px, double py){
        double scale_factor = Math.pow(ZOOM_BASE,-dZ);
        transform.translate((1 - scale_factor) * px, (1 - scale_factor) * py);        
        transform.scale(scale_factor, scale_factor);
        zoom -= dZ;
        updateTransform();
    }
    
    public boolean isVisible(SseqClass d){
        int x = d.getDegree()[0];
        int y = d.getDegree()[1];   
        return getMinX() <= x && x <= getMaxX() && getMinY() <= y && y <= getMaxY();
    }

    int[] algToTopGrading(int x, int y){
        return new int[] {y, x + y};
    }
    
    int[] topToAlgGrading(int x, int y){
        return new int[] {y - x, x};
    }    
    

    
    /**
     * The key callback to paint the JFrame.
     * @param graphics A canvas provided by JFrame.
     */
    @Override public void paintComponent(Graphics graphics)
    {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        drawGrid(g);        
        paintComponentHelper(g, transform);

        
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
            g.setColor(Color.lightGray);
            /* Offset x component by xgridstep/2 in order to keep dots centered in grid. */
            g.draw(transform.createTransformedShape(new Line2D.Double(x - xgridstep/2, min_y_visible, x - xgridstep/2, max_y_visible)));
            g.setColor(Color.black);
            if(x % xtickstep == 0) {
                Rectangle2D r = metrics.getStringBounds(String.valueOf(x), g);                                      
                tempTransform.setToTranslation(-r.getWidth()/2, TICK_GAP);
                tempTransform.concatenate(transform);
                tempPoint.setLocation(x + 0.5, 0);
                tempTransform.transform(tempPoint, tempPoint);
//                g.drawString(String.valueOf(x), (float) tempPoint.getX(), (float) tempPoint.getY());           
            }
        }
        for(int y = min_y_visible; y <= max_y_visible; y += ygridstep) {        
            g.setColor(Color.lightGray);
            /* Offset x start point by xgridstep/2 in order to match up with vertical grid lines. */
            g.draw(transform.createTransformedShape(new Line2D.Double(min_x_visible - xgridstep/2, y, max_x_visible, y)));
            g.setColor(Color.black);  
            if(y % ytickstep == 0){
                Rectangle2D r = metrics.getStringBounds(String.valueOf(y), g);                  
                tempTransform.setToTranslation(-TICK_GAP, r.getHeight()/2);
                tempTransform.concatenate(transform);
                tempPoint.setLocation(0, y + 0.5);
                tempTransform.transform(tempPoint, tempPoint);
//                g.drawString(String.valueOf(y), (float) tempPoint.getX(), (float) tempPoint.getY());                 
            }
        }
    }
    
    public void paintComponentHelper(Graphics2D g2d, AffineTransform transform){
        Graphics2DWithArrow g = new Graphics2DWithArrow(g2d);
        class DrawingCommands {            
            private void drawLine(double x1, double y1, double x2, double y2){
                g.draw(new Line2D.Double(x1, y1, x2, y2));
            }

            private void drawEdge(SseqEdge edge){
                if(!edge.drawOnPageQ(page)){
                    return;
                }
                    
                g.setColor(edge.getColor(page));
                g.setArrow(curvedArrow);
                SseqClass source = edge.getSource();
                SseqClass target = edge.getTarget();
                Node source_node = source.getNode(page);
                Node target_node = target.getNode(page);
                if( isVisible(source) && source.drawOnPageQ(page) && isVisible(target) && target.drawOnPageQ(page) ){
                    Node.drawLine(g, source_node, target_node);
                }
            }

            private void drawUnbasedStructline(U u, UnbasedLineDecoration<U> d){
                g.setColor(d.color);
            }    

            private void drawStructlineHelper(Point2D p1, Point2D p2){
                g.draw(AffineTransform.getTranslateInstance(transform.getScaleX()/2,  transform.getScaleY()/2).createTransformedShape(new Line2D.Double(p1, p2)));
//                curvedArrow.draw(g, AffineTransform.getTranslateInstance(transform.getScaleX()/2,  transform.getScaleY()/2).createTransformedShape(new Line2D.Double(p1,p2)));                
            }

            private void shadeChartCell(Color color, double x, double y){
                g.setColor(color);
                g.fill(transform.createTransformedShape(new Rectangle2D.Double(x, y, 1, 1)));
            }            
            
            final Color[] colors = new Color[] {Color.BLACK, Color.BLUE, Color.GREEN, Color.ORANGE};
            
            private void drawClass(SseqClass c){
                Node n = c.getNode(page);
//              TODO: Figure out how to scale these dots so that they get bigger as we zoom in
//              raising the scale factor to a power of 1.5
                n.draw(g);
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
        g.draw(transform.createTransformedShape(new Line2D.Double(min_x_visible, 0, max_x_visible, 0)));
        g.draw(transform.createTransformedShape(new Line2D.Double(0, min_y_visible, 0, max_y_visible)));        


        /* assign classes a location; at this point we definitively decide what's visible */
        Set<SseqClass> frameVisibles = new HashSet<>();
        for(int x = min_x_visible; x <= max_x_visible && x <= T_max + 3; x++) {
            for(int y = min_y_visible; y <= max_y_visible; y++) {
                int cellState = sseq.getState(algToTopGrading(x, y));
                Color cellColor;
                cellColor = STATE_COLORS.get(cellState);
                
                if(cellColor!=transparent){
                    drawHelpers.shadeChartCell(cellColor, x, y);                
                }

                if(!(cellState == MultigradedVectorSpace.STATE_PARTIAL || cellState == MultigradedVectorSpace.STATE_DONE)){
                    continue;
                }
                
                Collection<SseqClass> classes = sseq.getClasses(algToTopGrading(x, y), page);
//                System.out.println("classes: " + sseq.getClasses(multideg(15,1)).size());
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
                    double offset = -10 * (visible-1) / 2 * scale;
                    for(SseqClass d : classes) { 
                        if(frameVisibles.contains(d)) {
                            d.getNode(page).setPosition( getScreenX(x) + offset + transform.getScaleX()/2,
                                                         getScreenY(y) - offset/2 + transform.getScaleY()/2);
                            offset += 10 * scale;
                        }
                    }
                }
            }
        }
        g.fill(transform.createTransformedShape(new Rectangle.Double(T_max + 1, min_y_visible, max_x_visible, max_y_visible)));
        drawHelpers.shadeChartCell(Color.orange, selx, sely);

        /* draw decorations */
        for(SseqClass u : frameVisibles) {
            /* based */
            
            g.setNoArrow();
            for(Structline s : u.getStructlines()){
                drawHelpers.drawEdge(s);
            }
            
            g.setArrow(curvedArrow);
            for(Differential s : u.getOutgoingDifferentials()){
                drawHelpers.drawEdge(s);
            }
//            /* unbased */
//            dec.getUnbasedStructlineDecorations(u).forEach((d) -> 
//                drawUnbasedStructline(g,u,d)
//            );
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
        switch(sseq.getState(algToTopGrading(x, y))) {
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
                Collection<SseqClass> classes = sseq.getClasses(algToTopGrading(x, y), page);
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
        int[] topDeg = algToTopGrading(deg[0], deg[1]);
        if(topDeg[0] < getMaxX() && !display.mouseDown){
            repaint();
        }
        int[] s = algToTopGrading(selx, sely);
        if(deg[0] == s[0] && deg[1] == s[1]){
            setSelected(selx,sely); // updates text
        }
    }
    
}
