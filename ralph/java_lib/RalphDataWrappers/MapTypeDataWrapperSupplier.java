package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public interface MapTypeDataWrapperSupplier<K,V,ValueDeltaType>
{
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException;
    public MapTypeDataWrapper<K,V,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException;
    /**
       Returns internal val directly.  Caller must ensure that there
       will be no conflicts when writing.  Used for deserialization,
       not real operations.
     */
    public MapTypeDataWrapper<K,V,ValueDeltaType> direct_get_val();
}
