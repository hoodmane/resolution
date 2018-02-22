package res.algebra;

public interface GradedElement<T> extends Comparable<T>
{
    int getP();
    int deg();
    int[] extraGrading();
}
