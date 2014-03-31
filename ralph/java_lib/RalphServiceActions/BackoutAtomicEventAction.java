package RalphServiceActions;

import ralph.AtomicActiveEvent;

public class BackoutAtomicEventAction extends ServiceAction {
    
    private AtomicActiveEvent event_to_backout = null;

    public BackoutAtomicEventAction(AtomicActiveEvent _event_to_backout)
    {
        event_to_backout = _event_to_backout;
    }
    
    @Override
    public void run() 
    {
        event_to_backout.blocking_backout(null,false);
    }
}
