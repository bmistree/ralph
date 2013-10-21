package RalphServiceActions;

import ralph.LockedActiveEvent;

public class EventBackoutTouchedObjs extends ServiceAction {

    LockedActiveEvent event_to_backout = null;
	
    public EventBackoutTouchedObjs(LockedActiveEvent _to_backout)
    {
        event_to_backout = _to_backout;
    }
	
    @Override
    public void run() {
        event_to_backout._backout_touched_objs();
    }
}
