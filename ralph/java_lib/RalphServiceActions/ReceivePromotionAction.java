package RalphServiceActions;

import ralph.LockedActiveEvent;


/**
   When an action gets promoted, partner endpoint and other endpoints
   that called methods on this endpoint forward the promotion message
   to us.  We handle the promotion message by finding the event (if
   it exists), and requesting it to promote its uuid.
*/
public class ReceivePromotionAction extends ServiceAction {

    private ralph.Endpoint local_endpoint = null;
    private String event_uuid;
    private String new_priority;
	
    public ReceivePromotionAction(
        ralph.Endpoint _local_endpoint, String _event_uuid, String _new_priority)
    {
        local_endpoint = _local_endpoint;
        event_uuid = _event_uuid;
        new_priority = _new_priority;
    }
	
	
    @Override
    public void run() 
    {
        LockedActiveEvent evt =
            local_endpoint._act_event_map.get_event(event_uuid);
        if (evt != null)
            evt.promote_boosted(new_priority);
				

    }
}
