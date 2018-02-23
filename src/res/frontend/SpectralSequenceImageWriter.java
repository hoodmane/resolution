/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.frontend;

import de.erichseifert.vectorgraphics2d.Document;
import de.erichseifert.vectorgraphics2d.VectorGraphics2D;
import de.erichseifert.vectorgraphics2d.intermediate.CommandSequence;
import de.erichseifert.vectorgraphics2d.pdf.PDFProcessor;
import de.erichseifert.vectorgraphics2d.util.PageSize;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.FileOutputStream;
import java.io.IOException;
import static res.frontend.SpectralSequenceDisplay.PAGE_WIDTH;
import res.spectralsequencediagram.DisplaySettings;
import res.spectralsequencediagram.SpectralSequence;

/**
 *
 * @author Hood
 */
public class SpectralSequenceImageWriter extends SpectralSequenceDisplay{
        /**
     * 
     * @param sseq The spectral sequence to draw
     * @param settings
     * @return A SpectralSequenceDisplay object. Use sseqDisplay.paint(vectorGraphics2D) to make 
     */

    public SpectralSequenceImageWriter constructSpectralSequenceImageWriter(SpectralSequence sseq, DisplaySettings settings){
        return (SpectralSequenceImageWriter) SpectralSequenceDisplay.constructFrontend(sseq, settings);
    }
    
    public SpectralSequenceImageWriter(SpectralSequence sseq, DisplaySettings settings) {
        super(sseq, settings);
        this.windowOpened(null);
    }
    
    public void writeToFile(String filename){
        Graphics2D vg2d = new VectorGraphics2D();
//        transform.scale(10, 10);
        transform.setToIdentity();
        transform.translate(10, 200);              
        transform.scale(xscale * 10, - yscale * 5);     
        updateTransform();
        this.paintComponent(vg2d);
        vg2d.drawLine(0,0,100,100);
        vg2d.drawLine(30,330,100,100);
        vg2d.setColor(Color.BLUE);
        Shape s = transform.createTransformedShape(new Line2D.Double(0,0,10,10));
        System.out.println("x range: " + s.getBounds2D().getMinX() + " -- " + s.getBounds2D().getMaxX());
        System.out.println("y range: " + s.getBounds2D().getMinY() + " -- " + s.getBounds2D().getMaxY());
        vg2d.draw(transform.createTransformedShape(new Line2D.Double(0,0,10,10)));
        CommandSequence commands = ((VectorGraphics2D) vg2d).getCommands();
        PDFProcessor pdfProcessor = new PDFProcessor(true);
        Document doc = pdfProcessor.getDocument(commands, new PageSize(PAGE_WIDTH,PAGE_HEIGHT )); // Screens have a 16 : 9 aspect ratio
        try{
            doc.writeTo(new FileOutputStream(filename));
            System.out.println(filename);
        }catch(IOException e) {
            
        }
    }
}
