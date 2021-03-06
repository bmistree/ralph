package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;

import ralph.MessageSender.IMessageSender;

import ralph.ActiveEvent;
import ralph.NonAtomicActiveEvent;
import ralph.AtomicActiveEvent;
import ralph.Util;
import ralph.ExecutionContextMap;
import ralph.Endpoint;
import ralph.IUUIDGenerator;
import ralph.RalphGlobals;
import ralph.IEndpointMap;

public class NonAtomicExecutionContext extends ExecutionContext
{
    private final NonAtomicActiveEvent active_event;
    private final ExecutionContextMap exec_ctx_map;
    private final boolean is_replaying;


    public NonAtomicExecutionContext(
        IMessageSender message_sender,
        IUUIDGenerator uuid_gen, IDurabilityContext durability_context,
        NonAtomicActiveEvent active_event,ExecutionContextMap exec_ctx_map,
        RalphGlobals ralph_globals, IEndpointMap endpt_map, boolean is_replaying)
    {
        super(
            active_event.uuid,message_sender,uuid_gen,durability_context,
            ralph_globals,endpt_map);
        this.active_event = active_event;
        this.exec_ctx_map = exec_ctx_map;
        active_event.init_execution_context(this);
        this.is_replaying = is_replaying;
    }

    @Override
    public ActiveEvent curr_act_evt()
    {
        return active_event;
    }


    @Override
    public ExecutionContext clone_atomic_exec_ctx()
    {
        AtomicActiveEvent atom_evt =
            exec_ctx_map.create_root_atomic_evt(active_event);
        active_event.set_atomic_child(atom_evt);

        IDurabilityContext cloned_dur_ctx = null;
        if (durability_context != null)
            cloned_dur_ctx = durability_context.clone(atom_evt.uuid);

        AtomicExecutionContext to_return =
            new AtomicExecutionContext(
                message_sender,uuid_gen,cloned_dur_ctx,atom_evt,this,
                ralph_globals,endpt_map, is_replaying);

        return to_return;
    }

    @Override
    public ExecutionContext pop_exec_ctx()
    {
        Util.logger_assert("Should never be popping a non-atomic exec ctx.");
        return null;
    }

    @Override
    public boolean should_try_commit_act_evt()
    {
        // can always commit non-atomics.
        return true;
    }

    @Override
    public boolean is_replaying() {
        return is_replaying;
    }
}