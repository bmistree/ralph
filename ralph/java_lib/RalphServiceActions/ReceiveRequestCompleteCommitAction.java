package RalphServiceActions;


/**
 *  Another endpoint (either on the same host as I am or my
 partner) asked me to complete the second phase of the commit
 for an event with uuid event_uuid.

 @see ralphEndpoint._EndpointServiceThread.request_complete_commit
 *
 */
public class ReceiveRequestCompleteCommitAction extends ServiceAction {

    private ralph.Endpoint local_endpoint = null;
    private String event_uuid;
    private boolean request_from_partner;
	
    public ReceiveRequestCompleteCommitAction(
        ralph.Endpoint _local_endpoint, String _event_uuid,
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
        ralph.ActiveEvent evt =
            local_endpoint._act_event_map.get_event(event_uuid);
		
        if (evt == null)
        {
            //# event may not exist, for instance if got multiple
            //# complete commit messages because of loops in endpoint
            //# call graph.
            return;
        }
		

        //# if the request to complete the commit was from our partner,
        //# then we can skip sending a request to our partner to
        //# complete the commit.
        evt.complete_commit_and_forward_complete_msg(request_from_partner);
    }
}