package ralph;

public class ActiveEventTwoTuple
{
    /**
     * {2-tuple} --- (a,b)     
    
         a {Event or None} --- If an event existed in the map, then
         we return it.  Otherwise, return None.

         b {Event or None} --- If we requested retry-ing, then
         return a new root event with
         successor uuid to event_uuid.
         Otherwise, return None.
    */	

    ActiveEvent a = null;
    ActiveEvent b = null;
    public ActiveEventTwoTuple(ActiveEvent _a, ActiveEvent _b)
    {
        a = _a;
        b = _b;		
    }
}
