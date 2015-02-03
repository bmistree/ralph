package RalphDurability;

import ralph.EndpointConstructorObj;

public interface IDurabilitySaver
{
    /**
       Blocks until prepare and complete have been appended to log.
     */
    public void prepare_operation(IDurabilityContext dc);
    public void complete_operation(IDurabilityContext dc, boolean succeeded);

    /**
       If do not have a serialized version of this endpoint logged to
       durable storage, then force writing it to disk.
     */
    public void ensure_logged_endpt_constructor(
        EndpointConstructorObj endpt_constructor_obj);
}