package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Set;
import RalphCallResults.MessageCallResultObject;

public class PartnerEventParent extends EventParent {

    public PartnerEventParent(
        Endpoint _local_endpoint, String _uuid , String _priority)
    {
        super(_local_endpoint,_uuid,_priority);
		
    }

    @Override
    public void first_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        ActiveEvent _event)
    {
        //# forwards the message to others
        super.first_phase_transition_success(
            same_host_endpoints_contacted_dict,
            // FIXME: Previous logic stated:
            //# using false for partner contacted, because we know that
            //# we do not have to forward the commit request back to
            //# partner: our partner must have sent it to us.	
            local_endpoints_whose_partners_contacted,_event);


        //# tell parent endpoint that first phase has gone well and that
        //# it should wait on receiving responses from all the following
        //# endpoint uuids before entering second phase
        ArrayList<String> children_endpoints =
            new ArrayList<String>(same_host_endpoints_contacted_dict.keySet());

        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
            children_endpoints.add(endpt._partner_uuid);

        local_endpoint._forward_first_phase_commit_successful(
            uuid,local_endpoint._uuid,children_endpoints);
    }
	
    @Override
    public void rollback(
        String backout_requester_endpoint_uuid,
        HashMap<String,EventSubscribedTo> same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        boolean stop_request)
	        
    {		
        super.rollback(
            backout_requester_endpoint_uuid,same_host_endpoints_contacted_dict,
            local_endpoints_whose_partners_contacted,stop_request);
    }
        
	
    @Override
    public void second_phase_transition_success(
        HashMap<String,EventSubscribedTo>same_host_endpoints_contacted_dict,
        Set<Endpoint> local_endpoints_whose_partners_contacted)
    {
        super.second_phase_transition_success(
            same_host_endpoints_contacted_dict,

            // FIXME: Previous logic stated:
            //# using false for partner contacted, because we know that
            //# we do not have to forward the commit request back to
            //# partner: our partner must have sent it to us.
            local_endpoints_whose_partners_contacted);
    }
	
	
    /**
     *  Informs the partner that an exception has occured at runtime
     (thus the event should be backed out).
     * @param error
     * @param message_listening_queues_map
     */
    @Override
    public void put_exception(
        Exception error,
        HashMap<String, ArrayBlockingQueue<MessageCallResultObject>> message_listening_queues_map)
    {
        local_endpoint._propagate_back_exception(
            uuid,get_priority(),error);		
    }

    /**
     * @see super class' comments
     * 
     * Forward message on to parent
     */
    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_endpoint_uuid,
        ArrayList<String> children_event_endpoint_uuids) 
    {
        local_endpoint._forward_first_phase_commit_successful(
            uuid, msg_originator_endpoint_uuid, children_event_endpoint_uuids);
    }

}

        
