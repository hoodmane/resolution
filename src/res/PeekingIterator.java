/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package res;

/**
 *
 * @author Hood
 */
import java.util.Iterator;
import java.util.NoSuchElementException;
public class PeekingIterator<T> implements Iterator<T> {
    T next;
    Iterator<T> iter;
    boolean noSuchElement;

    public static <T> PeekingIterator<T> getPeekingIterator(Iterator<T> iter){
        if(iter instanceof PeekingIterator){
            return (PeekingIterator)iter;
        } else {
            return new PeekingIterator(iter);
        }
    }
    
    private PeekingIterator(Iterator<T> iterator) {
	// initialize any member here.
	iter = iterator;
        advanceIter();
    }

    // Returns the next element in the iteration without advancing the iterator.
    public T peek() {
        // you should confirm with interviewer what to return/throw
        // if there are no more values
        return next;
    }

    // hasNext() and next() should behave the same as in the Iterator interface.
    // Override them if needed.
    @Override
    public T next() {
        if (noSuchElement)
            throw new NoSuchElementException();
        T res = next;
        advanceIter();
        return res;
    }

    @Override 
    public boolean hasNext() {
        return !noSuchElement;
    }
    
    private void advanceIter() {
        if (iter.hasNext()) {
            next = iter.next();
        } else {
            noSuchElement = true;
        }
    }
}
