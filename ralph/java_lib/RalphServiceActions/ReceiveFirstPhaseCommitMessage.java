package RalphServiceActions;

import java.util.List;

import ralph.ActiveEvent;
import ralph.Endpoint;
import ralph.ExecutionContext.ExecutionContext;

/**
 * @see ralphEndpoint._EndpointServiceThread.receive_first_phase_commit_message
 *
 */
public class ReceiveFirstPhaseCommitMessage extends ServiceAction 
{
    private Endpoint local_endpoint = null;
    private String event_uuid;
    private String msg_originator_host_uuid;
    private boolean successful;
    private final List<String> children_event_host_uuids;
	
	
    public ReceiveFirstPhaseCommitMessage (
        Endpoint _local_endpoint,String _event_uuid,
        String _msg_originator_host_uuid,
        boolean _successful, List<String> _children_event_host_uuids)
    {
        local_endpoint = _local_endpoint;
        event_uuid = _event_uuid;
        msg_originator_host_uuid = _msg_originator_host_uuid;
        successful = _successful;
        children_event_host_uuids= _children_event_host_uuids;
    }
	
	
	
    @Override
    public void run() 
    {
        ExecutionContext exec_ctx =
            local_endpoint.exec_ctx_map.get_exec_ctx(
                event_uuid);
        
        if (exec_ctx != null)
        {
            ActiveEvent evt = exec_ctx.current_active_event();
            
            if (successful)
            {
                evt.receive_successful_first_phase_commit_msg(
                    event_uuid, msg_originator_host_uuid,
                    children_event_host_uuids);
            }
            else
            {
                evt.receive_unsuccessful_first_phase_commit_msg(
                    event_uuid,msg_originator_host_uuid);
            }
        }
    }
}
