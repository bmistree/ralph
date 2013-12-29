package ralph;

import java.util.ArrayList;

/**
 * value, returned from dewaldoify
 * @param <V> --- Java variables in the array list
 * @param <D> --- What the java variables in the arraylist should
 * dewaldoify into (if they are locked objects)
 */

public class ListTypeDataWrapperConstructor<V,D>
    extends DataWrapperConstructor<
    // The actual internal data that will be held by the data wrapper
    ArrayList<LockedObject<V,D>>, 
    // what you get when you call dewaldoify on the data wrapper
    ArrayList<D> >
{
    @Override
    public DataWrapper<ArrayList<LockedObject<V,D>>, ArrayList<D>>
        construct(
            ArrayList<LockedObject<V,D>> _val, boolean peered) 
    {
        return new ListTypeDataWrapper<V,D>(_val,peered);
    }
}

