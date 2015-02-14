package ralph;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import RalphDurability.IDurabilityContext;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import ralph.MessageSender.IMessageSender;
import ralph.ExecutionContext.LiveNonAtomicExecutionContext;
import ralph.ExecutionContext.LiveAtomicExecutionContext;
import ralph.ExecutionContext.ExecutionContext;
import ralph.ExecutionContext.ReplayNonAtomicExecutionContext;
import ralph.MessageSender.DurabilityReplayMessageSender;


public class ExecutionContextMap
{
    private final ReentrantLock mutex = new ReentrantLock();
    private final BoostedManager boosted_manager;
    private final RalphGlobals ralph_globals;
    private final Endpoint local_endpoint;
    
    public ExecutionContextMap(
        RalphGlobals ralph_globals, Endpoint local_endpoint)
    {
        this.ralph_globals = ralph_globals;
        this.local_endpoint = local_endpoint;
        boosted_manager = new BoostedManager(this,ralph_globals);
    }

    /**
       Using mutex so that can only create one event for a particular
       uuid.  As an example of what could go wrong if did not:

         1) Receive partner rpc request: start creating partner event
         
         2) While creating partner event, get a notification to
            backout partner.  (That event isn't yet in map, so do
            nothing.)
     */
    private void _lock()
    {    	
        mutex.lock();
    }
    private void _unlock()
    {
        mutex.unlock();
    }

    public AtomicActiveEvent create_root_atomic_evt(
        NonAtomicActiveEvent non_atom_evt)
    {
        AtomicActiveEvent root_event =
            boosted_manager.create_root_atomic_event(non_atom_evt);
        return root_event;
    }

    /**
       Can only create supers for non atomic events.  Further, cannot
       change from being super to anything else.  Cannot change from
       anything else to becoming super.
     */
    public NonAtomicActiveEvent create_super_root_non_atomic_evt(
        Endpoint root_endpoint, String event_entry_point_name)
    {
        return create_root_non_atomic_evt(
            root_endpoint, event_entry_point_name, true);
    }

    public NonAtomicActiveEvent create_root_non_atomic_evt(
        Endpoint root_endpoint, String event_entry_point_name)
    {
        return create_root_non_atomic_evt(
            root_endpoint, event_entry_point_name, false);        
    }

    private NonAtomicActiveEvent create_root_non_atomic_evt(
        Endpoint root_endpoint, String event_entry_point_name,
        boolean is_super)
    {
        NonAtomicActiveEvent root_event =
            boosted_manager.create_root_non_atomic_event(
                is_super,root_endpoint,event_entry_point_name);

        return root_event;
    }

    public ReplayNonAtomicExecutionContext replay_create_root_non_atomic_exec_ctx(
        Endpoint root_endpoint, String event_entry_point_name, 
        DurabilityReplayMessageSender msg_sender, IEndpointMap endpt_map)
    {
        NonAtomicActiveEvent root_event =
            boosted_manager.create_root_non_atomic_event(
                false,root_endpoint,event_entry_point_name);

        ReplayNonAtomicExecutionContext exec_ctx =
            new ReplayNonAtomicExecutionContext (
                ralph_globals, root_event, this, msg_sender,endpt_map);

        ralph_globals.all_ctx_map.put(exec_ctx.uuid,exec_ctx);
        return exec_ctx;
    }    

    
    /**
     *  @param event_uuid
     * 
     *  @returns --- @see remove_exec_ctx_if_exists' return statement
     */
    public ExecutionContext remove_exec_ctx(String event_uuid)
    {
        return remove_exec_ctx_if_exists(event_uuid);
    }

    /**
     * 
     * @param event_uuid 
     * @return ----
     a {Event or None} --- If an event existed in the map, then
     we return it.  Otherwise, return None.
    */
    public ExecutionContext remove_exec_ctx_if_exists(String event_uuid)
    {        
        ExecutionContext to_remove =
            ralph_globals.all_ctx_map.remove(event_uuid);

        if (to_remove != null)
        {
            if (to_remove.curr_act_evt().event_parent.is_root)
                boosted_manager.complete_root_event(event_uuid);
        }

        return to_remove;
    }
    
    
    /**
     * @returns {None,ExecutionContext} --- None if event with name
     uuid is not in map.  Otherwise, reutrns the ExecutionContext in
     the map.
    */
    public ExecutionContext get_exec_ctx(String uuid)
    {
        ExecutionContext to_return = ralph_globals.all_ctx_map.get(uuid);
        return to_return;
    }

    /**
     * Get or create an event because partner endpoint requested it.

     @param event_entry_point_name --- If have to generate the partner
     event, then use this to label the entry point for the entry point
     name.

     Only used to generate live execution contexts (ones that may be
     logging themselves).
     
     @returns {_ActiveEvent}
    */
    public ExecutionContext get_or_create_partner_live_exec_ctx(
        String uuid, String priority,boolean atomic,
        String event_entry_point_name)
    {
        _lock();

        ExecutionContext to_return = get_exec_ctx(uuid);
        if (to_return == null)
        {
            PartnerEventParent pep =
                new PartnerEventParent(
                    local_endpoint, uuid, priority, ralph_globals,
                    event_entry_point_name);
            
            ActiveEvent new_event = null;
            if (atomic)
            {
                new_event = new AtomicActiveEvent(
                    pep,this,null,ralph_globals);
                
                to_return =
                    LiveAtomicExecutionContext.exec_ctx_not_from_non_atomic(
                        ralph_globals, (AtomicActiveEvent)new_event);
            }
            else
            {
                new_event = new NonAtomicActiveEvent(pep,this,ralph_globals);
                to_return = 
                    new LiveNonAtomicExecutionContext(
                        ralph_globals, (NonAtomicActiveEvent) new_event, this);
            }
        }
        _unlock();
        return to_return;        
    }
}
