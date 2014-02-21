package RalphCommonBacked;

import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

import ralph.SpeculativeAtomicObject;
import ralph.ActiveEvent;

public class BackedManager
{
    // Key is active event uuid.  Value is a set of local objects that
    // that active event has touched.
    private HashMap<String,EventChangeSet>event_map =
        new HashMap<String,EventChangeSet>();

    public BackedManager()
    {}

    public synchronized void note_set(
        SpeculativeAtomicObject obj, ActiveEvent active_event)
    {
        if (! event_map.containsKey(active_event.uuid))
            event_map.put(active_event.uuid, new EventChangeSet());

        EventChangeSet change_set = event_map.get(active_event.uuid);
        change_set.add_object(obj);
    }
    
    public synchronized void note_backed_out(
        SpeculativeAtomicObject obj, ActiveEvent active_event)
    {
        if (! event_map.containsKey(active_event.uuid))
            return;

        event_map.remove(active_event.uuid);
        
        EventChangeSet change_set = event_map.get(active_event.uuid);
        if (change_set.remove_object(obj))
            event_map.remove(active_event.uuid);
    }

    protected static class EventChangeSet
    {
        Set<SpeculativeAtomicObject> object_set =
            new HashSet<SpeculativeAtomicObject>();
        
        public void add_object(SpeculativeAtomicObject obj)
        {
            object_set.add(obj);
        }

        /**
           @returns --- True if event change set is empty and
           therefore can be reclaimed.
         */
        public boolean remove_object(SpeculativeAtomicObject obj)
        {
            object_set.remove(obj);
            return object_set.isEmpty();
        }
    }
}


