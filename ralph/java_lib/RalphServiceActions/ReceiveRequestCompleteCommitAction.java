package RalphServiceActions;

import ralph.RalphGlobals;
import ralph.ActiveEvent;
import ralph.ExecutionContext.ExecutionContext;

/**
 *  Another endpoint (either on the same host as I am or my
 partner) asked me to complete the second phase of the commit
 for an event with uuid event_uuid.

 @see ralphEndpoint._EndpointServiceThread.request_complete_commit
 *
 */
public class ReceiveRequestCompleteCommitAction extends ServiceAction {

    private final RalphGlobals ralph_globals;
    private final String event_uuid;
    private final boolean request_from_partner;

    public ReceiveRequestCompleteCommitAction(
        RalphGlobals ralph_globals, String event_uuid,
        boolean request_from_partner)
    {
        this.ralph_globals = ralph_globals;
        this.event_uuid = event_uuid;
        this.request_from_partner = request_from_partner;
    }

    /**
     *
     1.  Grab the endpoint from active event map (if it exists)
     2.  Call its complete_commit_and_forward_complete_msg method.
    */
    @Override
    public void run()
    {
        ExecutionContext exec_ctx = ralph_globals.all_ctx_map.get(event_uuid);
        if (exec_ctx == null)
        {
            // event may not exist, for instance if got multiple
            // complete commit messages because of loops in endpoint
            // call graph.
            return;
        }

        // if the request to complete the commit was from our partner,
        // then we can skip sending a request to our partner to
        // complete the commit.
        ActiveEvent evt = exec_ctx.curr_act_evt();
        evt.complete_commit_and_forward_complete_msg(request_from_partner);
    }
}