package ralph.ExecutionContext;

import RalphDurability.IDurabilityContext;

import ralph.MessageSender.IMessageSender;
import ralph.ActiveEvent;
import ralph.AtomicActiveEvent;
import ralph.IUUIDGenerator;
import ralph.AtomicActiveEvent;
import ralph.RalphGlobals;
import ralph.IEndpointMap;


public class AtomicExecutionContext extends ExecutionContext
{
    private final AtomicActiveEvent active_event;
    private final NonAtomicExecutionContext parent_ctx;
    private int reference_counter = 0;
    private final boolean is_replaying;

    public AtomicExecutionContext(
        IMessageSender message_sender,
        IUUIDGenerator uuid_gen, IDurabilityContext durability_context,
        AtomicActiveEvent active_event, NonAtomicExecutionContext parent_ctx,
        RalphGlobals ralph_globals, IEndpointMap endpt_map,
        boolean is_replaying)
    {
        super(
            active_event.uuid,message_sender,uuid_gen,
            durability_context,ralph_globals,endpt_map);
        this.active_event = active_event;
        this.parent_ctx = parent_ctx;
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
        ++reference_counter;
        return this;
    }

    @Override
    public ExecutionContext pop_exec_ctx()
    {
        if (reference_counter == 0)
            return parent_ctx;
        --reference_counter;
        return this;
    }

    @Override
    public boolean should_try_commit_act_evt()
    {
        // handles nested blocks: only commit on base of atomically
        // blocks.
        if (reference_counter == 0)
            return true;
        return false;
    }

    @Override
    public boolean is_replaying() {
        return is_replaying;
    }
}