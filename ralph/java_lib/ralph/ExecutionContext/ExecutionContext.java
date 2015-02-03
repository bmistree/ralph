package ralph.ExecutionContext;

import java.util.ArrayDeque;
import java.util.Deque;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;
import ralph_protobuffs.DurabilityProto.Durability;

import RalphDurability.IDurabilityContext;

import ralph.MessageSender.IMessageSender;
import ralph.IUUIDGenerator;
import ralph.ActiveEvent;


public class ExecutionContext implements IDurabilityContext
{
    private final IMessageSender message_sender;
    private final IUUIDGenerator uuid_gen;
    /**
       Could be null if logging is off.
     */
    private final IDurabilityContext durability_context;
    
    private final Deque<ActiveEvent> active_event_stack =
        new ArrayDeque<ActiveEvent>();

    public final String uuid;

    public ExecutionContext(
        String uuid, IMessageSender message_sender, IUUIDGenerator uuid_gen,
        IDurabilityContext durability_context)
    {
        this.uuid = uuid;
        this.message_sender = message_sender;
        this.uuid_gen = uuid_gen;
        this.durability_context = durability_context;
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

    public ActiveEvent base_active_event()
    {
        return active_event_stack.peekLast();
    }
    
    public ActiveEvent current_active_event()
    {
        return active_event_stack.peekFirst();
    }
    
    public void push_active_event(ActiveEvent evt)
    {
        active_event_stack.addFirst(evt);
    }
    public void pop_active_event()
    {
        active_event_stack.removeFirst();
    }
}