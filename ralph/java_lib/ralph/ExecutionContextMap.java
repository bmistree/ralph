package ralph;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import RalphDurability.IDurabilityContext;
import ralph.ExecutionContext.ExecutionContext;
import ralph.ExecutionContext.LiveExecutionContext;
import ralph.BoostedManager.DeadlockAvoidanceAlgorithm;
import ralph.MessageSender.IMessageSender;

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

    public void create_and_push_root_atomic_evt(
        ExecutionContext exec_ctx, Endpoint root_endpoint,
        String event_entry_point_name)
    {
        AtomicActiveEvent root_event =
            boosted_manager.create_root_atomic_event(
                exec_ctx.current_active_event(), root_endpoint,
                event_entry_point_name, exec_ctx);
        exec_ctx.push_active_event(root_event);
    }

    /**
       Can only create supers for non atomic events.  Further, cannot
       change from being super to anything else.  Cannot change from
       anything else to becoming super.
     */
    public ExecutionContext create_super_root_non_atomic_exec_ctx(
        Endpoint root_endpoint, String event_entry_point_name,
        IDurabilityContext durability_ctx)
    {
        return create_root_non_atomic_exec_ctx(
            root_endpoint, event_entry_point_name,
            durability_ctx, true);
    }

    public ExecutionContext create_root_non_atomic_exec_ctx(
        Endpoint root_endpoint, String event_entry_point_name,
        IDurabilityContext durability_ctx)
    {
        return create_root_non_atomic_exec_ctx(
            root_endpoint, event_entry_point_name,
            durability_ctx, false);        
    }

    public ExecutionContext replay_create_root_non_atomic_exec_ctx(
        Endpoint root_endpoint, String event_entry_point_name, String evt_uuid,
        IMessageSender msg_sender)
    {
        ExecutionContext exec_ctx =
            new ExecutionContext(
                evt_uuid,
                msg_sender,
                // FIXME: should pass a uuid generator that
                // produces endpoint uuid, instead of using
                // RalphGlobals.
                ralph_globals,
                // do not log any additional to disk during replay
                null);

        Util.logger_warn(
            "Should pass in replay context's uuid generator.");
        
        NonAtomicActiveEvent root_event =
            boosted_manager.create_root_non_atomic_event(
                false,root_endpoint,event_entry_point_name,
                exec_ctx);
        
        exec_ctx.push_active_event(root_event);
        ralph_globals.all_ctx_map.put(exec_ctx.uuid,exec_ctx);
        return exec_ctx;
    }

    
    private ExecutionContext create_root_non_atomic_exec_ctx(
        Endpoint root_endpoint, String event_entry_point_name,
        IDurabilityContext durability_ctx, boolean is_super)
    {
        // FIXME: ensure that doc specifies will only create live
        // contexts.
        LiveExecutionContext exec_ctx =
            new LiveExecutionContext(ralph_globals,durability_ctx);
        
        NonAtomicActiveEvent root_event =
            boosted_manager.create_root_non_atomic_event(
                is_super,root_endpoint,event_entry_point_name,
                exec_ctx);
        
        exec_ctx.push_active_event(root_event);
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
            if (to_remove.current_active_event().event_parent.is_root)
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
     
     @returns {_ActiveEvent}
    */
    public ExecutionContext get_or_create_partner_exec_ctx(
        String uuid, String priority,boolean atomic,
        String event_entry_point_name, IDurabilityContext durability_context)
    {
        _lock();

        ExecutionContext to_return = get_exec_ctx(uuid);
        if (to_return == null)
        {
            // FIXME: double-check that should always produce
            // LiveExecutionContext.
            to_return = new LiveExecutionContext(
                ralph_globals,durability_context,uuid);

            
            PartnerEventParent pep =
                new PartnerEventParent(
                    local_endpoint, uuid, priority, ralph_globals,
                    event_entry_point_name);
            
            ActiveEvent new_event = null;
            if (atomic)
            {
                new_event = new AtomicActiveEvent(
                    pep,ralph_globals.thread_pool,this,null,ralph_globals,
                    to_return);
            }
            else
                new_event = new NonAtomicActiveEvent(pep,this,ralph_globals);
            
            to_return.push_active_event(new_event);
            ralph_globals.all_ctx_map.put(uuid,to_return);
        }
        _unlock();
        return to_return;        
    }
}
