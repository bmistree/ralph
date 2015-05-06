package RalphServiceActions;

import ralph.ActiveEvent;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RalphGlobals;
import ralph.ExecutionContext.ExecutionContext;

/**
   When an action gets promoted, partner endpoint and other endpoints
   that called methods on this endpoint forward the promotion message
   to us.  We handle the promotion message by finding the event (if
   it exists), and requesting it to promote its uuid.
*/
public class ReceivePromotionAction extends ServiceAction
{
    private final RalphGlobals ralph_globals;
    private final String event_uuid;
    private final String new_priority;

    public ReceivePromotionAction(
        RalphGlobals ralph_globals, String event_uuid, String new_priority)
    {
        this.ralph_globals = ralph_globals;
        this.event_uuid = event_uuid;
        this.new_priority = new_priority;
    }

    @Override
    public void run()
    {
        ExecutionContext exec_ctx = ralph_globals.all_ctx_map.get(event_uuid);

        if (exec_ctx != null)
        {
            ActiveEvent evt = exec_ctx.curr_act_evt();
            evt.promote_boosted(new_priority);
        }
    }
}
