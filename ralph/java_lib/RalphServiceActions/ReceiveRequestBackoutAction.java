package RalphServiceActions;

import ralph.Util;
import ralph.Endpoint;
import ralph.ActiveEvent;
import ralph.RalphGlobals;
import ralph.ExecutionContext.ExecutionContext;

/**
 *  @param {_Endpoint object} local_endpoint --- The endpoint
    which was requested to backout.  (Ie, the endpoint on which
    this action will take place.)

    @param{UUID}  uuid --- @see _EndpointServiceThread.request_backout

    @param{either Endpoint object or
    util.PARTNER_ENDPOINT_SENTINEL} requesting_endpoint ---
    @see _EndpointServiceThread.request_backout
 *
 */

public class ReceiveRequestBackoutAction extends ServiceAction
{
    private final RalphGlobals ralph_globals;
    private final String evt_uuid;
    private final boolean skip_partner;

    /**
     *  We were requested to backout an event.  Check if we have the
     event, back it out if can, and forward the backout message to
     others.
     * @param uuid
     */
    public ReceiveRequestBackoutAction(
        RalphGlobals ralph_globals, String evt_uuid, boolean skip_partner)
    {
        this.ralph_globals = ralph_globals;
        this.evt_uuid = evt_uuid;
        this.skip_partner = skip_partner;
    }

    @Override
    public void run()
    {
        ExecutionContext exec_ctx = ralph_globals.all_ctx_map.get(evt_uuid);
        if (exec_ctx == null)
        {
            // could happen for instance if there are loops in
            // endpoint call graph.  In this case, might get more than
            // one request to backout an event.  However, the first
            // backout has already removed the the active event from
            // the active event map.
            return;
        }

        ActiveEvent evt = exec_ctx.curr_act_evt();
        evt.forward_backout_request_and_backout_self(skip_partner);
    }
}

