/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res.frontend;

/**
 *
 * @author Hood
 */

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class BevelArrows
{
    public static void main ( String...args )
    {
        SwingUtilities.invokeLater ( new Runnable () {
            BevelArrows arrows = new BevelArrows();

            @Override
            public void run () {
                JFrame frame = new JFrame ( "Bevel Arrows" );

                frame.add ( new JPanel() {
                    public void paintComponent ( Graphics g ) {
//                        arrows.draw ( ( Graphics2D ) g, getWidth(), getHeight() );
                    }
                }
                , BorderLayout.CENTER );

                frame.setSize ( 800, 400 );
                frame.setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );
                frame.setVisible ( true );
            }
        } );
    }

    interface Arrow {
        void draw ( Graphics2D g, double x1, double y1, double x2, double y2 );
    }

    public Arrow[] arrows = { new NoArrow(), new LineArrow(), new CurvedArrow() };

//    void draw ( Graphics2D g, int width, int height )
//    {
//        g.setRenderingHint ( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
//
//        g.setColor ( Color.WHITE );
//        g.fillRect ( 0, 0, width, height );
//
//        for ( Arrow arrow : arrows ) {
//            g.setColor ( Color.ORANGE );
//            g.fillRect ( 350, 20, 20, 280 );
//
//            g.setStroke ( new BasicStroke ( 20.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL ) );
//            g.translate ( 0, 60 );
//            arrow.draw ( g );
//
//            g.setStroke ( new BasicStroke ( 20.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER ) );
//            g.translate ( 0, 100 );
//            arrow.draw ( g );
//
//            g.setStroke ( new BasicStroke ( 20.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND ) );
//            g.translate ( 0, 100 );
//            arrow.draw ( g,  );
//
//            g.translate ( 400, -260 );
//        }
//    }

    static class LineArrow  implements Arrow
    {
        public void draw ( Graphics2D g, double x1, double y1, double x2, double y2 )
        {
            // where the control point for the intersection of the V needs calculating
            // by projecting where the ends meet

            double arrowRatio = 0.5f;
            double arrowLength = 80.0f;

            BasicStroke stroke = ( BasicStroke ) g.getStroke();

            double endX = 350.0f;

            double veeX;

            switch ( stroke.getLineJoin() ) {
                case BasicStroke.JOIN_BEVEL:
                    // IIRC, bevel varies system to system, this is approximate
                    veeX = endX - stroke.getLineWidth() * 0.25f;
                    break;
                default:
                case BasicStroke.JOIN_MITER:
                    veeX = endX - stroke.getLineWidth() * 0.5f / arrowRatio;
                    break;
                case BasicStroke.JOIN_ROUND:
                    veeX = endX - stroke.getLineWidth() * 0.5f;
                    break;
            }

            // vee
            Path2D.Float path = new Path2D.Float();

            path.moveTo ( veeX - arrowLength, -arrowRatio*arrowLength );
            path.lineTo ( veeX, 0.0f );
            path.lineTo ( veeX - arrowLength, arrowRatio*arrowLength );

            g.draw ( path );

            // stem for exposition only
            g.draw ( new Line2D.Double ( 50.0f, 0.0f, veeX, 0.0f ) );

            // in practice, move stem back a bit as rounding errors
            // can make it poke through the sides of the Vee
            g.draw ( new Line2D.Double ( 50.0f, 0.0f, veeX - stroke.getLineWidth() * 0.25, 0.0 ) );
        }
    }

    static class NoArrow  implements Arrow {

        @Override
        public void draw(Graphics2D g, double x1, double y1, double x2, double y2) {
            double endX = 350.0f;
            g.draw ( new Line2D.Double ( x1,y1,x2,y2 ) );
        }
        
    }
    
    static class CurvedArrow  implements Arrow
    {
        // to draw a nice curved arrow, fill a V shape rather than stroking it with lines
        public void draw ( Graphics2D g, double x1, double y1, double x2, double y2)
        {
            // as we're filling rather than stroking, control point is at the apex,

            double arrowRatio = 0.5f;
            double arrowLength = 80.0f;

            BasicStroke stroke = ( BasicStroke ) g.getStroke();

            double endX = 350.0f;

            double veeX = endX - stroke.getLineWidth() * 0.5f / arrowRatio;

            // vee
            Path2D.Float path = new Path2D.Float();

            double waisting = 0.5f;

            double waistX = endX - arrowLength * 0.5f;
            double waistY = arrowRatio * arrowLength * 0.5f * waisting;
            double arrowWidth = arrowRatio * arrowLength;

            path.moveTo ( veeX - arrowLength, -arrowWidth );
            path.quadTo ( waistX, -waistY, endX, 0.0f );
            path.quadTo ( waistX, waistY, veeX - arrowLength, arrowWidth );

            // end of arrow is pinched in
            path.lineTo ( veeX - arrowLength * 0.75f, 0.0f );
            path.lineTo ( veeX - arrowLength, -arrowWidth );

            g.fill ( path );

            // move stem back a bit
            g.draw ( new Line2D.Double ( 50.0f, 0.0f, veeX - arrowLength * 0.5f, 0.0f ) );
        }
    }
}
    
