//Rebecka Skareng
package paradis.assignment4;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class CopyOnWriteArrayList<E> {

    private AtomicReference<ImmutableArray> atomicArray;

    public CopyOnWriteArrayList() {
        E[] arr = (E[]) new Object[0];
        atomicArray = new AtomicReference<>(new ImmutableArray(arr));
    }


    public Stream<E> stream(){
        return atomicArray.get().stream();
    }

    //försöker uppdatera atomicArray med en ny array som har param värdet tillagt.
    public boolean add(E element) {

        for (int i = 0; i < 100; i++) {
            ImmutableArray old = atomicArray.get();
            ImmutableArray newArray = old.copyToAdd();
            newArray.array[newArray.array.length - 1] = element;
            if (atomicArray.compareAndSet(old, newArray)) {
                return true;
            }
        }
        return false;
    }

    //försöker uppdatera atomicArray med en ny array som tagit bort det medskickade objectet
    public boolean remove(Object obj) {
        for (int i = 0; i < 100; i++) {
            ImmutableArray old = atomicArray.get();
            int newLength = old.array.length - 1;
            ImmutableArray newArray = new ImmutableArray((E[]) new Object[newLength]);
            for (int z = 0; z < newLength; z++) {
                if(!obj.equals(old.array[z])){
                    newArray.array[z] = old.array[z];
                }
            }
            if(atomicArray.compareAndSet(old, newArray)){
                return true;
            }
        }

        return false;
    }

    
    public void forEach(Consumer<E> action) {
        ImmutableArray imArray = atomicArray.get();
        for(int i=0; i<imArray.array.length; i++){
            action.accept(imArray.array[i]);
        }
    }

    //håller referesen till den underliggande arrayen
    class ImmutableArray {

        private final E[] array;

        private ImmutableArray(E[] arr) {
            array = Arrays.copyOf(arr, arr.length);
        }

        private final ImmutableArray copyToAdd() {
            return new ImmutableArray(Arrays.copyOf(array, array.length + 1));
        }

        private final Stream<E> stream(){
            return Arrays.stream(array);

        }

    }

}