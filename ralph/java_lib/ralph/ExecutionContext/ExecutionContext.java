package ralph.ExecutionContext;

import java.util.Stack;

import ralph_protobuffs.PartnerRequestSequenceBlockProto.PartnerRequestSequenceBlock;

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
    
    private final Stack<ActiveEvent> active_event_stack =
        new Stack<ActiveEvent>();

    public ExecutionContext(
        IMessageSender message_sender, IUUIDGenerator uuid_gen,
        IDurabilityContext durability_context)
    {
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
    
    public IMessageSender message_sender()
    {
        return message_sender;
    }
    public IUUIDGenerator uuid_gen()
    {
        return uuid_gen;
    }
    
    public ActiveEvent current_active_event()
    {
        return active_event_stack.peek();
    }
    
    public void push_active_event(ActiveEvent evt)
    {
        active_event_stack.push(evt);
    }
    public void pop_active_event()
    {
        active_event_stack.pop();
    }
}