package RalphServiceActions;

import ralph.Endpoint;
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

    private final Endpoint local_endpoint;
    private final String event_uuid;
    private final boolean request_from_partner;
	
    public ReceiveRequestCompleteCommitAction(
        Endpoint _local_endpoint, String _event_uuid,
        boolean _request_from_partner)
    {
        local_endpoint = _local_endpoint;
        event_uuid = _event_uuid;
        request_from_partner = _request_from_partner;
    }

    /**
     *
     1.  Grab the endpoint from active event map (if it exists)
     2.  Call its complete_commit_and_forward_complete_msg method.
    */
    @Override
    public void run() 
    {
        ExecutionContext exec_ctx =
            local_endpoint.exec_ctx_map.get_exec_ctx(event_uuid);
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
        ActiveEvent evt = exec_ctx.current_active_event();
        evt.complete_commit_and_forward_complete_msg(request_from_partner);
    }
}