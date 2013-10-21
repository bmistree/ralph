package RalphServiceActions;

import ralph.LockedActiveEvent;

public class PromoteBoostedAction extends ServiceAction {

    private LockedActiveEvent evt_to_promote;
    private String new_priority;
	
    public PromoteBoostedAction(LockedActiveEvent _evt_to_promote,String _new_priority)
    {
        evt_to_promote = _evt_to_promote;
        new_priority = _new_priority;
    }
	
    @Override
    public void run() 
    {
        evt_to_promote.promote_boosted(new_priority);
    }
}
