package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public interface ListTypeDataWrapperSupplier<T,ValueDeltaType>
{
    public ListTypeDataWrapper<T,ValueDeltaType> get_val_read(
        ActiveEvent active_event) throws BackoutException;
    public ListTypeDataWrapper<T,ValueDeltaType> get_val_write(
        ActiveEvent active_event) throws BackoutException;

    /**
       Returns internal val directly.  Caller must ensure that there
       will be no conflicts when writing.  Used for deserialization,
       not real operations.
     */
    public ListTypeDataWrapper<T,ValueDeltaType> direct_get_val();
}
