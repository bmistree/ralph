package RalphExtended;

import ralph.ActiveEvent;


/**
   Called by ExtendedHardwareOverrides to supply data to push into
   IHardwareChangeApplier.
 */
public interface IHardwareStateSupplier <T>
{
    public T get_state_to_push(ActiveEvent active_event);
}
