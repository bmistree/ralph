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
    private IChangeProcessor cp = null;

    public BackedManager(IChangeProcessor _cp)
    {
        cp = _cp;
    }

    public synchronized void note_set(
        SpeculativeAtomicObject obj, ActiveEvent active_event)
    {
        if (! event_map.containsKey(active_event.uuid))
            event_map.put(active_event.uuid, new EventChangeSet(cp));

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
        private Set<SpeculativeAtomicObject> object_set =
            new HashSet<SpeculativeAtomicObject>();
        private int num_in_first_phase_commit = 0;
        private IChangeProcessor cp = null;
        
        public EventChangeSet(IChangeProcessor _cp)
        {
            cp = _cp;
        }

        /**
           @returns --- True if can now remove this event from map.
         */
        public boolean object_first_phase(SpeculativeAtomicObject obj)
        {
            if (object_set.contains(obj))
                ++ num_in_first_phase_commit;

            if (num_in_first_phase_commit == object_set.size())
            {
                // can begin pushing all changes to graph.
                cp.push_changes(object_set);
                return true;
            }
            return false;
        }
        
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


