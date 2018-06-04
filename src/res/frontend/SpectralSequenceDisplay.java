package res.frontend;

import res.algebra.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;

import javax.swing.*;
import javax.swing.UIManager;
import res.spectralsequencediagram.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.DefaultCaret;

public class SpectralSequenceDisplay<U extends MultigradedElement<U>> extends JPanel 
        implements MouseMotionListener, MouseListener, MouseWheelListener, WindowListener, ActionListener
{
    final static int DEFAULT_WINDOW_WIDTH = 1600;
    final static int DEFAULT_WINDOW_HEIGHT = 800;
    final static int MARGIN_WIDTH = 30;
    // selected x, selected y. Initialize to a large negative value representing "no selection".
    int selx = -10000;
    int sely = selx;
    int mousex = -1000;
    int mousey = -1000;
//  These record the mouse position when the user clicked so that if they only 
//  "drag" the mouse a tiny distance, we can handle it as if they clicked.
    int mouseDownX;
    int mouseDownY;
    boolean mouseDown;
   
    
    JFrame frame;
    JTextArea textarea;
    JPanel consolePanel;    
    JScrollPane consoleOutputPane;
    public  JTextArea consoleOutputText;
    JTextField consoleInput;
    boolean consoleOpen;
    int consoleWidth = 3000;
    int consoleHeight = 250;
    Dimension openConsoleDimension = new Dimension(2000,295);   
    Dimension closedConsoleDimension = new Dimension(2000,35);
    
    SpectralSequenceCanvas canvas;
    SpectralSequence sseq;
   
    private SpectralSequenceDisplay(){}
    
    public static SpectralSequenceDisplay constructFrontend(SpectralSequence sseq,DisplaySettings settings) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        } catch (UnsupportedLookAndFeelException ex) {
            Logger.getLogger(SpectralSequenceDisplay.class.getName()).log(Level.SEVERE, null, ex);
        }
        SpectralSequenceDisplay d = new SpectralSequenceDisplay();
        d.sseq = sseq;
        d.canvas = new SpectralSequenceCanvas(d,sseq,settings);        
        d.setPreferredSize(new Dimension(1000,1000));
        d.frame = new JFrame(settings.getWindowName());
        d.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        d.frame.setSize(DEFAULT_WINDOW_WIDTH,DEFAULT_WINDOW_HEIGHT);        
        JFrame fr = d.frame;
       
        Font font = d.getFont().deriveFont((float)16);        
        d.consoleOutputText = new JTextArea(20,20);
//        d.consoleOutputText.setText("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n");
        d.consoleOutputText.setMaximumSize(new Dimension(d.consoleHeight,d.consoleWidth));
        d.consoleOutputText.setPreferredSize(new Dimension(d.consoleHeight,d.consoleWidth));
//        consoleTextArea.setEditable(false);
        d.consoleOutputText.setLineWrap(true);
        d.consoleOutputText.setMargin( new Insets(10,10,10,10) ); 
        d.consoleOutputText.setFont(font);      
        
        d.consoleOutputPane = new JScrollPane(d.consoleOutputText, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        d.consoleOutputPane.setPreferredSize(new Dimension(10,250));
        d.consoleOutputPane.setAlignmentX(-1.0f);
//      Scroll to bottom of console output pane
        d.consoleOutputPane.getVerticalScrollBar().setValue(d.consoleOutputPane.getVerticalScrollBar().getMaximum());
        

        DefaultCaret caret = (DefaultCaret)d.consoleOutputText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        d.consoleInput = new JTextField(0);
        d.consoleInput.setFont(font);
        d.consoleInput.setMargin( new Insets(0,10,0,10) ); 

        d.consoleInput.addActionListener(d);          
        
        fr.getContentPane().add(d.canvas, BorderLayout.CENTER);
//        fr.getContentPane().add(d.consoleArea,BorderLayout.SOUTH);  
        d.consolePanel = new JPanel();
        d.consolePanel.setLayout(new GridBagLayout());
        d.consolePanel.setPreferredSize(d.closedConsoleDimension);        
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.SOUTH;
        c.weightx = 1;
        c.weighty = 1.0; 
        c.gridx = 0;
        c.gridy = 0;
//        c.anchor = GridBagConstraints.PAGE_END;
        d.consolePanel.add(d.consoleOutputPane,c);
        c.gridx = 0;
        c.gridy = 1;
        d.consolePanel.add(d.consoleInput,c);
        d.consolePanel.revalidate();
//        fr.getContentPane().add(textField,BorderLayout.SOUTH);   
        fr.getContentPane().add(new ControlPanel2D(d), BorderLayout.EAST);
        fr.getContentPane().add(d.consolePanel,BorderLayout.SOUTH);
        d.consoleOpen = false;
        d.consoleOutputPane.setVisible(false);
        
        
        
        d.canvas.addMouseListener(d);
        d.canvas.addMouseMotionListener(d);
        d.canvas.addMouseWheelListener(d);
        d.frame.addWindowListener(d);
        return d;
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        this.sseq.executeJython(consoleInput.getText(), (String str) -> {
                consoleOutputText.append(str);
                canvas.repaint();                
            }
        );
    }    
    
            
    
    public SpectralSequenceDisplay<U> start(){
        this.frame.setVisible(true);     
        return this;
    }
    
    @Override public void mouseClicked(MouseEvent evt) {
        this.canvas.repaint();
        this.canvas.requestFocusInWindow();
        int x = (int) Math.floor(canvas.getChartX(evt.getX()));
        int y = (int) Math.floor(canvas.getChartY(evt.getY()));
        if((x >= 0 || canvas.x_full_range) && ( y >= 0 || canvas.y_full_range ) ) {
            canvas.setSelected(x,y);
        } else {
//            setSelected(-1000,-1000);
        }
    }
    @Override public void mousePressed(MouseEvent evt) { 
        mouseDown = true; 
        mouseDownX = evt.getX();
        mouseDownY = evt.getY();
    }
    @Override public void mouseReleased(MouseEvent evt) { 
        mouseDown = false; 
//      If the user dragged a tiny amount, treat as a click.
        double dx = evt.getX() - mouseDownX;
        double dy = evt.getY() - mouseDownY;
        if(dx*dx + dy*dy < 10){
            mouseClicked(evt);
        }
    }
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

        canvas.updateTransform();
        canvas.translateCanvas(dx, -dy);      
        canvas.repaint();
    }    
    
    @Override public void mouseWheelMoved(MouseWheelEvent evt){
        double dZ = evt.getWheelRotation();
        Point2D pt = new Point2D.Double(mousex, mousey);
        canvas.zoomCanvasAround(dZ, canvas.getChartX(mousex), canvas.getChartY(mousey));       
        canvas.updateTransform();
        this.canvas.repaint();
    }
    
//  Initialize the scale here!
    @Override
    public void windowOpened(WindowEvent e) {
        canvas.initializeTransform();
        this.canvas.repaint();  
    }

    @Override
    public void windowClosing(WindowEvent e) {}

    @Override
    public void windowClosed(WindowEvent e) {}

    @Override
    public void windowIconified(WindowEvent e) {}
    
    @Override
    public void windowDeiconified(WindowEvent e) {
        canvas.updateTransform();
        this.canvas.repaint();
        repaint();
    }

    @Override
    public void windowActivated(WindowEvent e) {
        canvas.updateTransform();
        this.canvas.repaint();
        repaint();
    }

    @Override
    public void windowDeactivated(WindowEvent e) {}
    
    public void toggleConsole(){
        this.consoleOpen = ! this.consoleOpen;
        consoleOutputPane.setVisible(this.consoleOpen);        
        canvas.translateCanvas(0, ((this.consoleOpen) ? 1 : -1) * (openConsoleDimension.height - closedConsoleDimension.height)); 
//        consolePanel.setPreferredSize(openConsoleDimension);
        if(this.consoleOpen){
            consolePanel.setPreferredSize(this.openConsoleDimension);
        } else {
            consolePanel.setPreferredSize(this.closedConsoleDimension);
        }
        frame.revalidate();     
        this.consolePanel.revalidate();
        repaint();
    }        

}



class ControlPanel2D extends Box {

    ControlPanel2D(final SpectralSequenceDisplay<?> parent){
        super(BoxLayout.Y_AXIS);

        setup_gui(parent);
        
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
            .addKeyEventDispatcher((KeyEvent e) -> {
                if(e.getID() != KeyEvent.KEY_PRESSED)
                    return false;
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        if(parent.consoleInput.isFocusOwner()){
                            return false;
                        }
                        parent.canvas.incrementPage();
                        return true;
                    case KeyEvent.VK_RIGHT:
                        if(parent.consoleInput.isFocusOwner()){
                            return false;
                        }                        
                        parent.canvas.decrementPage();
                        return true;
                    case KeyEvent.VK_G:  
                        handleGo();
                        return true;  
                    case KeyEvent.VK_C:
                         if (e.isControlDown()){
                            parent.toggleConsole();
                            return true;
                         }
                         return false;
                    default:
                        return false;
                }
            });

    }
    
    public void handleGo(){
        
    }

    
    public JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu, submenu;
        JMenuItem menuItem;
        JRadioButtonMenuItem rbMenuItem;
        JCheckBoxMenuItem cbMenuItem;

        //Create the menu bar.
        menuBar = new JMenuBar();

        //Build the first menu.
        menu = new JMenu("A Menu");
        menu.setMnemonic(KeyEvent.VK_A);
        menu.getAccessibleContext().setAccessibleDescription(
                "The only menu in this program that has menu items");
        menuBar.add(menu);

        //a group of JMenuItems
        menuItem = new JMenuItem("A text-only menu item",
                                 KeyEvent.VK_T);
        //menuItem.setMnemonic(KeyEvent.VK_T); //used constructor instead
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_1, ActionEvent.ALT_MASK));
        menuItem.getAccessibleContext().setAccessibleDescription(
                "This doesn't really do anything");
        menu.add(menuItem);

//        ImageIcon icon = createImageIcon("images/middle.gif");
//        menuItem = new JMenuItem("Both text and icon", icon);
//        menuItem.setMnemonic(KeyEvent.VK_B);
//        menu.add(menuItem);

//        menuItem = new JMenuItem(icon);
//        menuItem.setMnemonic(KeyEvent.VK_D);
//        menu.add(menuItem);

        //a group of radio button menu items
        menu.addSeparator();
        ButtonGroup group = new ButtonGroup();

        rbMenuItem = new JRadioButtonMenuItem("A radio button menu item");
        rbMenuItem.setSelected(true);
        rbMenuItem.setMnemonic(KeyEvent.VK_R);
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("Another one");
        rbMenuItem.setMnemonic(KeyEvent.VK_O);
        group.add(rbMenuItem);
        menu.add(rbMenuItem);

        //a group of check box menu items
        menu.addSeparator();
        cbMenuItem = new JCheckBoxMenuItem("A check box menu item");
        cbMenuItem.setMnemonic(KeyEvent.VK_C);
        menu.add(cbMenuItem);

        cbMenuItem = new JCheckBoxMenuItem("Another one");
        cbMenuItem.setMnemonic(KeyEvent.VK_H);
        menu.add(cbMenuItem);

        //a submenu
        menu.addSeparator();
        submenu = new JMenu("A submenu");
        submenu.setMnemonic(KeyEvent.VK_S);

        menuItem = new JMenuItem("An item in the submenu");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_2, ActionEvent.ALT_MASK));
        submenu.add(menuItem);

        menuItem = new JMenuItem("Another item");
        submenu.add(menuItem);
        menu.add(submenu);

        //Build second menu in the menu bar.
        menu = new JMenu("Another Menu");
        menu.setMnemonic(KeyEvent.VK_N);
        menu.getAccessibleContext().setAccessibleDescription(
                "This menu does nothing");
        menuBar.add(menu);

        return menuBar;
    }
    
    
    private ControlPanel2D setup_gui(final SpectralSequenceDisplay<?> parent){

//        parent.frame.setJMenuBar(createMenuBar());
        parent.textarea = new JTextArea();
        parent.textarea.setMaximumSize(new Dimension(250,500));
        parent.textarea.setPreferredSize(new Dimension(250,500));
        parent.textarea.setEditable(false);
        parent.textarea.setLineWrap(true);
        parent.textarea.setMargin( new Insets(10,10,10,10) );
        JScrollPane textsp = new JScrollPane(parent.textarea, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        textsp.setMaximumSize(new Dimension(250,500));
        textsp.setPreferredSize(new Dimension(250,500));
        textsp.setAlignmentX(-1.0f);
        add(textsp);
        return this;
    }

}

