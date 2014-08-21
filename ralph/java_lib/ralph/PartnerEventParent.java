package ralph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Set;
import RalphCallResults.MessageCallResultObject;

public class PartnerEventParent extends EventParent
{
    public PartnerEventParent(
        String _host_uuid, Endpoint local_endpoint, String _uuid,
        String _priority, RalphGlobals _ralph_globals,
        String _event_entry_point_name)
    {
        super(
            _host_uuid,_uuid,_priority,_ralph_globals,
            false,local_endpoint,_event_entry_point_name);
    }
    
    @Override
    public void first_phase_transition_success(
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        ActiveEvent _event,long root_timestamp,
        String root_host_uuid,String application_uuid,String event_name)
    {
        //# forwards the message to others
        super.first_phase_transition_success(
            // FIXME: Previous logic stated:
            //# using false for partner contacted, because we know that
            //# we do not have to forward the commit request back to
            //# partner: our partner must have sent it to us.	
            local_endpoints_whose_partners_contacted,_event,
            root_timestamp,root_host_uuid, application_uuid,event_name);

        //# tell parent endpoint that first phase has gone well and that
        //# it should wait on receiving responses from all the following
        //# endpoint uuids before entering second phase
        ArrayList<String> children_hosts = new ArrayList<String>();
        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
            children_hosts.add(endpt._partner_host_uuid);

        local_endpoint._forward_first_phase_commit_successful(
            uuid,local_endpoint._host_uuid,children_hosts);
    }
	
    @Override
    public void rollback(
        String backout_requester_host_uuid,
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        boolean stop_request)
	        
    {
        // add local endpoint to endpoints to roll back.
        local_endpoints_whose_partners_contacted.add(local_endpoint);
        super.rollback(
            backout_requester_host_uuid,
            local_endpoints_whose_partners_contacted,stop_request);
    }
        
	
    @Override
    public void second_phase_transition_success(
        Set<Endpoint> local_endpoints_whose_partners_contacted)
    {
        super.second_phase_transition_success(
            // FIXME: Previous logic stated:
            //# using false for partner contacted, because we know that
            //# we do not have to forward the commit request back to
            //# partner: our partner must have sent it to us.
            local_endpoints_whose_partners_contacted);
    }
	
	
    /**
     * Informs the partner that an exception has occured at runtime
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
        String event_uuid, String msg_originator_host_uuid,
        ArrayList<String> children_event_host_uuids) 
    {
        local_endpoint._forward_first_phase_commit_successful(
            uuid, msg_originator_host_uuid, children_event_host_uuids);
    }
}

        
