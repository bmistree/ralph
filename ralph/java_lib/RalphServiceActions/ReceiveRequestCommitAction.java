package RalphServiceActions;



/**
 * The local endpoint's partner has requested the local endpoint to
 begin the first phase of committing changes (note: not complete
 committing the changes, just begin the first phase) for an event.
 *
 */
public class ReceiveRequestCommitAction extends ServiceAction 
{
    final ralph.Endpoint local_endpoint;
    final String event_uuid;
    final Long root_timestamp;
    final String root_host_uuid;

    public ReceiveRequestCommitAction(
        ralph.Endpoint _local_endpoint, String _event_uuid,
        Long _root_timestamp,String _root_host_uuid)
    {
        local_endpoint = _local_endpoint;
        event_uuid = _event_uuid;
        root_timestamp = _root_timestamp;
        root_host_uuid = _root_host_uuid;
    }
	
    @Override
    public void run() 
    {
        ralph.ActiveEvent evt =
            local_endpoint._act_event_map.get_event(event_uuid);

        if (evt == null)
        {
            //# can happen if commit is requested and then
            //#  a ---> b ---> c
            //# 
            //#     a asks for commit.  b backs out and forwards commit
            //#     request on to c.  c waits on active event map lock
            //#     before receiving request for commit.  a tells b to back
            //#     out and forwards the request to b to backout, which
            //#     forwards the request on to c.  Then, if c reads the
            //#     backout before the request to commit, we may get to this
            //#     point.  Just ignore the request.
        }
        else
        {
            evt.non_local_root_begin_first_phase_commit(
                root_timestamp, root_host_uuid);
        }
    }
}

