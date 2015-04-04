package ralph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import RalphCallResults.MessageCallResultObject;

public class PartnerEventParent extends EventParent
{
    public PartnerEventParent(
        Endpoint local_endpoint, String _uuid,
        String _priority, RalphGlobals _ralph_globals,
        String _event_entry_point_name, String spanning_tree_parent_uuid)
    {
        super(
            _uuid,_priority,_ralph_globals,false,local_endpoint,
            _event_entry_point_name, spanning_tree_parent_uuid);
    }
    
    @Override
    public void first_phase_transition_success(
        Set<String> remote_hosts_contacted_uuid, ActiveEvent _event,
        long root_timestamp, String root_host_uuid, String application_uuid,
        String event_name)
    {}
    
    @Override
    public void second_phase_transition_success()
    {}

    /**
     * Informs the partner that an exception has occured at runtime
     (thus the event should be backed out).
     * @param error
     * @param message_listening_mvars_map
     */
    @Override
    public void put_exception(
        Exception error,
        Map<String, MVar<MessageCallResultObject>> message_listening_mvars_map)
    {}

    
    /**
     */
    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_host_uuid,
        Set<String> children_event_host_uuids) 
    {}
}