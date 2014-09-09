package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 */
public interface ListTypeDataWrapperSupplier<T>
{
    public ListTypeDataWrapper<T> get_val_read(
        ActiveEvent active_event) throws BackoutException;
    public ListTypeDataWrapper<T> get_val_write(
        ActiveEvent active_event) throws BackoutException;
}
