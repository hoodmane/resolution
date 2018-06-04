package res.algebra;
import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import res.spectralsequencediagram.nodes.*;
import res.spectralsequencediagram.*;


public class Generator<T extends GradedElement<T>> implements MultigradedElement<Generator<T>>
{
    public DModSet<T> img;

    public int[] deg;
    public int idx;
    public String extraInfo = "";
    private Collection<Structline> structlines;
    private StructlineProducer structlineGetter;
    private Node node = new RectangleNode(8,8,RectangleNode.NO_FILL);

    int[] algToTopGrading(int x, int y){
        return new int[] {y, x + y};
    }
    
    int[] topToAlgGrading(int x, int y){
        return new int[] {y - x, x};
    }    
    
    @Override
    public Color getColor(int page) {
//        System.out.println("getting color");
        return node.getColor();
    }
    
    @Override
    public void setColor(int page,Color color) {
//        System.out.println("setting color");
        node.setColor(color);
    }    

    @Override
    public int getPage() {
        return 0;
    }

    @Override
    public Collection<Differential> getOutgoingDifferentials() {
        return Collections.EMPTY_SET;
    }

    @Override
    public SseqClass setNode(Node s) {
        this.node = s;
        return this;
    }

    
    public static interface StructlineProducer {
        Collection<Structline> get();
    }
    
    /**
     * This deals with the annoying fact that the decorator is needed to figure out what the structlines sourced at a 
     * particular class are, but the Generator doesn't have the decorator.
     * @param structlineGetter A method that returns the structlines of the class.
     * @return this -- chainable
     */
    public Generator<T> setStructlineGetter(StructlineProducer structlineGetter){
        this.structlineGetter = structlineGetter;
        return this;
    }
    
    public Generator(int p,int[] deg, int idx)
    {
        this.deg = deg;
        this.idx = idx;
        img = new DModSet<>(p);
    }
            
    @Override public int compareTo(Generator<T> b)
    {
        if(deg.length != b.deg.length)
            return deg.length - b.deg.length;
        for(int i = 0; i < deg.length; i++)
            if(deg[i] != b.deg[i])
                return deg[i] - b.deg[i];
        return idx - b.idx;
    }

    @Override public int[] getDegree()
    {
        return topToAlgGrading(deg[0],deg[1]);
    }

    @Override public boolean equals(Object o)
    {
        Generator<?> g = (Generator<?>) o;
        for(int i = 0; i < idx; i++)
            if(deg[i] != g.deg[i])
                return false;
        return (idx == g.idx);
    }

    @Override public String extraInfo()
    {
        String ret = extraInfo;
        ret += "Image: "+img;
        return ret;
    }

    public String getName() {
        return String.format("%d-%d-%d",deg[0],deg[1],idx);
    }
    
    public Generator<T> setStructlines(Collection<Structline> sls){
        this.structlines = sls;
        return this;
    }


    @Override
    public Node getNode(int page) {
        return node;
    }

    @Override
    public Collection<Structline> getStructlines() {
        return structlineGetter.get();
    }
    
    @Override
    public boolean drawOnPageQ(int page) {
        return true;
    }    

}
