package RalphDataWrappers;

import java.util.ArrayList;
import ralph.RalphObject;
/**
 * value, returned from dewaldoify
 * @param <V> --- Java variables in the array list
 * @param <D> --- What the java variables in the arraylist should
 * dewaldoify into (if they are locked objects)
 */

public class ListTypeDataWrapperFactory<V,D>
    extends DataWrapperFactory<
    // The actual internal data that will be held by the data wrapper
    ArrayList<RalphObject<V,D>>, 
    // what you get when you call dewaldoify on the data wrapper
    ArrayList<D> >
{
    @Override
    public DataWrapper<ArrayList<RalphObject<V,D>>, ArrayList<D>>
        construct(
            ArrayList<RalphObject<V,D>> _val, boolean log_changes) 
    {
        return new ListTypeDataWrapper<V,D>(_val,log_changes);
    }
}

