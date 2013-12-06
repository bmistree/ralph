package RalphServiceActions;

import ralph.Util;

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
    private ralph.Endpoint local_endpoint = null;
    private String uuid = null;
    private ralph.Endpoint requesting_endpoint = null;
	
    /**
     *  We were requested to backout an event.  Check if we have the
     event, back it out if can, and forward the backout message to
     others.
     * @param endpoint
     * @param uuid
     * @param requesting_endpoint
     */
    public ReceiveRequestBackoutAction(
        ralph.Endpoint _endpoint, String _uuid,
        ralph.Endpoint _requesting_endpoint) 
    {
        local_endpoint = _endpoint;
        uuid = _uuid;
        requesting_endpoint = _requesting_endpoint;
    }

    public void run()
    {
        ralph.ActiveEvent event =
            local_endpoint._act_event_map.get_event(uuid);
        if (event == null)
        {
            //# could happen for instance if there are loops in endpoint
            //# call graph.  In this case, might get more than one
            //# request to backout an event.  However, the first backout
            //# has already removed the the active event from the active
            //# event map.
            return;
        }
        boolean skip_partner = false;
        if (requesting_endpoint == Util.PARTNER_ENDPOINT_SENTINEL)
            skip_partner = true;

        event.forward_backout_request_and_backout_self(skip_partner);
    }
}

