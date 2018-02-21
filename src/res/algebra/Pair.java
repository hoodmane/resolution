package res.algebra;

public class Pair<T,U>
{
    public T a;
    public U b;

    public Pair(T t, U u)
    {
        this.a = t;
        this.b = u;
    }
    


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair key = (Pair) o;
        return a == key.a && b == key.b;
    }

    @Override
    public int hashCode() {
        int result = a.hashCode();
        result = 31 * result + b.hashCode();
        return result;
    }
 
}
