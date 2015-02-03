package ralph;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import RalphCallResults.RootCallResult;
import RalphCallResults.MessageCallResultObject;

public class RootEventParent extends EventParent
{    
    /**
     * 	# indices are event uuids.  Values are bools.  When all values
     # are true in this dict, then we can transition into second
     # phase commit.
    */
    private Map<String,Boolean> endpoints_waiting_on_commit =
        new HashMap<String,Boolean>();
    
    /**
       # when the root tries to commit the event, it blocks while
       # reading the event_complete_mvar
    */
    public MVar<RootCallResult.ResultType>event_complete_mvar = 
        new MVar<RootCallResult.ResultType>();

    /**
       # we can add and remove events to waiting on commit lock from
       # multiple threads.  it appears that this can desynchronize
       # operations on endpoints_waiting_on_commit.  For instance, if
       # one thread puts in that it's waiting on a particular
       # endpoint and another thread puts in that the other
       # endpoint's first phase transition has been received, we can
       # get a case where the first message overwrites the operations
       # of the second.
    */
    private ReentrantLock _endpoints_waiting_on_commit_lock =
        new ReentrantLock();
	
    /**
       @param{uuid} uuid --- If None, then generate a random uuid and
       use that.  Otherwise, use the uuid specified.  Note: not all
       root events use a random uuid because some must be boosted.
    */
    public RootEventParent(
        String _uuid, String _priority,
        RalphGlobals _ralph_globals, Endpoint _local_endpoint,
        String _event_entry_point_name)
    {
        super(
            _uuid,_priority,_ralph_globals,true,
            _local_endpoint, _event_entry_point_name);
    }

    private void  _lock_endpoints_waiting_on_commit()
    {
        _endpoints_waiting_on_commit_lock.lock();
    }
    
    private void _unlock_endpoints_waiting_on_commit()
    {
        _endpoints_waiting_on_commit_lock.unlock();
    }

    /**
       Non-atomic notified that we are done with event.
     */
    public void non_atomic_completed()
    {
    	event_complete_mvar.put(RootCallResult.ResultType.COMPLETE);
    }
    
    
    /**
     * @see second_phase_transition_success in EventParent
     */
    public void second_phase_transition_success(
        Set<Endpoint> local_endpoints_whose_partners_contacted)
    {
    	event_complete_mvar.put(RootCallResult.ResultType.COMPLETE);

    	super.second_phase_transition_success(
            local_endpoints_whose_partners_contacted);
    }

    /**
       Places the appropriate call result in the event complete mvar to 
       indicate to the endpoint that an error has occured and the event
       must be handled.        
    */
    @Override
    public void put_exception(
        Exception error, 
        Map<String,MVar<MessageCallResultObject>> message_listening_mvars_map)
    {
    	if (RalphExceptions.NetworkException.class.isInstance(error))
    	{
            // Send a NetworkFailureCallResult to each listening mvar
            for (String reply_with_uuid : message_listening_mvars_map.keySet())
            {
                MVar<MessageCallResultObject> message_listening_mvar = 
                    message_listening_mvars_map.get(reply_with_uuid);
                message_listening_mvar.put(
                    MessageCallResultObject.network_failure(error.toString()));
            }
    	}
    }
    
    /**
     * For arguments, @see EventParent.
     */
    @Override
    public void first_phase_transition_success(
        Set<Endpoint> local_endpoints_whose_partners_contacted,
        ActiveEvent _event, long root_commit_timestamp,
        String root_host_uuid,String application_uuid, String event_name)
    {
        // # note that we should not wait on ourselves to commit
    	_lock_endpoints_waiting_on_commit();

        for (Endpoint endpt : local_endpoints_whose_partners_contacted)
            endpoints_waiting_on_commit.put(endpt._partner_host_uuid, false);
    	
        //# not waiting on self.
        endpoints_waiting_on_commit.put(
            ralph_globals.host_uuid, true);

        _unlock_endpoints_waiting_on_commit();

        super.first_phase_transition_success(
            local_endpoints_whose_partners_contacted, _event,
            root_commit_timestamp,root_host_uuid, application_uuid,
            event_name);

        //# after first phase has completed, should check if can
        //# transition directly to second phase (ie, no other endpoints
        //# were involved in event.)
        check_transition();
    }

    /**
       If we are no longer waiting on any endpoint to acknowledge
       first phase commit, then transition into second phase commit.
    */
    public void check_transition()
    {
        _lock_endpoints_waiting_on_commit();
        for (Boolean endpoint_transitioned :
                 endpoints_waiting_on_commit.values())
        {
            if (! endpoint_transitioned.booleanValue())
            {
                _unlock_endpoints_waiting_on_commit();
                return;
            }
        }
            
        _unlock_endpoints_waiting_on_commit();
        event.second_phase_commit();
    }
	
    @Override
    public void rollback(
        String backout_requester_host_uuid, 
        Set<Endpoint> local_endpoints_whose_partners_contacted)
    {
        super.rollback(
            backout_requester_host_uuid,
            local_endpoints_whose_partners_contacted);

        event_complete_mvar.put(
            RootCallResult.ResultType.RESCHEDULE);
    }

    /**
       @see super class comments

       Update the list of endpoints that we are waiting on
       committing.  Check whether should transition into second phase
       of commit.
    */
    @Override
    public void receive_successful_first_phase_commit_msg(
        String event_uuid, String msg_originator_host_uuid,
        List<String> children_event_host_uuids)
    {
    	_lock_endpoints_waiting_on_commit();
    	endpoints_waiting_on_commit.put(msg_originator_host_uuid, true);
    	boolean may_transition = true;
    	for (String end_uuid : children_event_host_uuids)
    	{
            Boolean val = endpoints_waiting_on_commit.get(end_uuid);
            if (val == null)
            {
                endpoints_waiting_on_commit.put(end_uuid, false);
                may_transition = false;
            }
    	}
        _unlock_endpoints_waiting_on_commit();
    	if (may_transition)
            check_transition();
    }
}
