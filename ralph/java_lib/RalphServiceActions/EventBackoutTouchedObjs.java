package RalphServiceActions;

import ralph.ActiveEvent;

public class EventBackoutTouchedObjs extends ServiceAction {

    ActiveEvent event_to_backout = null;
	
    public EventBackoutTouchedObjs(ActiveEvent _to_backout)
    {
        event_to_backout = _to_backout;
    }
	
    @Override
    public void run() {
        event_to_backout._backout_touched_objs();
    }
}
