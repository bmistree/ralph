package RalphDataWrappers;

import RalphExceptions.BackoutException;
import ralph.ActiveEvent;

/**
 * @param <T> --- The actual java type of the values holding (ie,
 * outside of locked object)
 * @param <D> --- What the java variables in the hashmap should
 * dewaldoify into (if they are locked objects)
 */
public interface ListTypeDataWrapperSupplier<T,D>
{
    public ListTypeDataWrapper<T,D> get_val_read(
        ActiveEvent active_event) throws BackoutException;
    public ListTypeDataWrapper<T,D> get_val_write(
        ActiveEvent active_event) throws BackoutException;
}
