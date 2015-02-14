package ralph.ExecutionContext;

import java.util.ArrayDeque;
import java.util.Deque;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;

import RalphDurability.IDurabilityContext;

import ralph.MessageSender.IMessageSender;
import ralph.IUUIDGenerator;
import ralph.ActiveEvent;
import ralph.RalphGlobals;
import ralph.Endpoint;


public abstract class ExecutionContext implements IDurabilityContext
{
    protected final IMessageSender message_sender;
    protected final IUUIDGenerator uuid_gen;
    /**
       Could be null if logging is off.
     */
    protected final IDurabilityContext durability_context;
    public final String uuid;
    protected final RalphGlobals ralph_globals;
    
    public ExecutionContext(
        String uuid, IMessageSender message_sender,
        IUUIDGenerator uuid_gen, IDurabilityContext durability_context,
        RalphGlobals ralph_globals)
    {
        this.uuid = uuid;
        this.message_sender = message_sender;
        this.uuid_gen = uuid_gen;
        this.durability_context = durability_context;
        this.ralph_globals = ralph_globals;
        this.ralph_globals.all_ctx_map.put(uuid,this);
    }

    public Endpoint get_endpt_if_exists(String endpt_uuid)
    {
        return ralph_globals.all_endpoints.get_endpoint_if_exists(
            endpt_uuid);
    }
    

    @Override
    public IDurabilityContext clone(String new_event_uuid)
    {
        if (durability_context == null)
            return null;
        return durability_context.clone(new_event_uuid);
    }

    @Override
    public void add_endpt_created_info(
        String endpt_uuid,String endpt_constructor_name)
    {
        if (durability_context == null)
            return;
        durability_context.add_endpt_created_info(
            endpt_uuid, endpt_constructor_name);
    }

    @Override
    public void add_rpc_arg(
        PartnerRequestSequenceBlock arg, String endpoint_uuid)
    {
        if (durability_context == null)
            return;
        durability_context.add_rpc_arg(arg,endpoint_uuid);
    }

    @Override
    public Durability prepare_proto_buf()
    {
        if (durability_context == null)
            return null;
        return durability_context.prepare_proto_buf();
    }

    @Override
    public Durability complete_proto_buf(boolean succeeded)
    {
        if (durability_context == null)
            return null;
        return durability_context.complete_proto_buf(succeeded);
    }
    
    public IMessageSender message_sender()
    {
        return message_sender;
    }
    public IUUIDGenerator uuid_gen()
    {
        return uuid_gen;
    }
    
    public abstract ActiveEvent curr_act_evt();
    public abstract ExecutionContext clone_atomic_exec_ctx();
    public abstract ExecutionContext pop_exec_ctx();
    /**
       Returns true if should try to commit the active event
       associated with the executing context.  false, if shouldn't.

       Handles nested atomically blocks: only try to commit at base of
       atomically blocks.
     */
    public abstract boolean should_try_commit_act_evt();
}