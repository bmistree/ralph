package RalphServiceActions;

import java.util.ArrayList;


/**
 * @see ralphEndpoint._EndpointServiceThread.receive_first_phase_commit_message
 *
 */
public class ReceiveFirstPhaseCommitMessage extends ServiceAction 
{
    private ralph.Endpoint local_endpoint = null;
    private String event_uuid;
    private String msg_originator_endpoint_uuid;
    private boolean successful;
    private ArrayList<String> children_event_endpoint_uuids = null;
	
	
    public ReceiveFirstPhaseCommitMessage (
        ralph.Endpoint _local_endpoint,String _event_uuid,
        String _msg_originator_endpoint_uuid,
        boolean _successful, ArrayList<String> _children_event_endpoint_uuids)
    {
        local_endpoint = _local_endpoint;
        event_uuid = _event_uuid;
        msg_originator_endpoint_uuid = _msg_originator_endpoint_uuid;
        successful = _successful;
        children_event_endpoint_uuids= _children_event_endpoint_uuids;
    }
	
	
	
    @Override
    public void run() 
    {
        ralph.LockedActiveEvent act_event =
            local_endpoint._act_event_map.get_event(event_uuid);

        if (act_event != null)
        {
            if (successful)
            {
                act_event.receive_successful_first_phase_commit_msg(
                    event_uuid, msg_originator_endpoint_uuid,
                    children_event_endpoint_uuids);
            }
            else
            {
                act_event.receive_unsuccessful_first_phase_commit_msg(
                    event_uuid,msg_originator_endpoint_uuid);
            }
        }
    }
}
