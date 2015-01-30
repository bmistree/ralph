package ralph.ExecutionContext;

import java.util.Stack;

import ralph.MessageSender.IMessageSender;
import ralph.IUUIDGenerator;
import ralph.ActiveEvent;


public class ExecutionContext
{
    private final IMessageSender message_sender;
    private final IUUIDGenerator uuid_gen;

    private final Stack<ActiveEvent> active_event_stack =
        new Stack<ActiveEvent>();

    public ExecutionContext(
        IMessageSender message_sender, IUUIDGenerator uuid_gen)
    {
        this.message_sender = message_sender;
        this.uuid_gen = uuid_gen;
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