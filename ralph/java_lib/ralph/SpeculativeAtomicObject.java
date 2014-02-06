package ralph;

import RalphExceptions.BackoutException;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;


public abstract class SpeculativeAtomicObject<T,D> extends AtomicObject<T,D>
{
    /**
       0th index is eldest element that we are speculating on.  Last
       index is most recent.
     */
    private List<SpeculationEntry> speculated_entries =
        new ArrayList<SpeculationEntry>();
    private SpeculationEntry speculating_on = null;

    public SpeculativeAtomicObject(RalphGlobals ralph_globals)
    {
        super(ralph_globals);
    }
    
    /**
       If speculating, re-map get_val to get from the object that
       we're speculating on instead.
     */
    @Override
    public T get_val(ActiveEvent active_event) throws BackoutException
    {
        T to_return = null;
        _lock();
        try
        {
            if (speculating_on != null)
            {
                to_return =
                    speculating_on.get_val(active_event);
            }
            else
                to_return = super.get_val(active_event);
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
        finally
        {
            _unlock();
        }
        return to_return;
    }

    @Override
    public void set_val(ActiveEvent active_event,T new_val)
        throws BackoutException
    {
        _lock();
        try
        {
            if (speculating_on != null)
                speculating_on.set_val(active_event,new_val);
            else
                super.set_val(active_event,new_val);
        }
        catch (BackoutException ex)
        {
            throw ex;
        }
        finally
        {
            _unlock();
        }
    }

    private class SpeculationEntry
    {
        /**
           When one AtomicActiveEvent gets into the first phase of its
           commit, it calls first_phase_commit on each
           SpeculativeAtomicObject it touched.  When the
           AtomicActiveEvent does this, SpeculativeAtomicObjects
           create a SpeculationEntry associated with that objects
           commit.  We allow other active events to subsequently
           operate on the dirty, uncommitted value of the
           SpeculativeAtomicObject.  These events are chained to the
           outstanding commit however: if the outstanding commit
           fails, then they are rolled back.  Importantly, these
           dependent AtomicActiveEvents also cannot commit their
           changes until the event speculated upon has completed.
         */
        private String waiting_on_uuid = null;
        private Set<ActiveEvent> dependent_events = new HashSet<ActiveEvent>();
        private AtomicObject<T,D> speculating_on = null;
        
        public SpeculationEntry(
            String waiting_on_uuid,
            AtomicObject<T,D> speculating_on)
        {
            this.waiting_on_uuid = waiting_on_uuid;
            this.speculating_on = speculating_on;
        }

        public T get_val(ActiveEvent active_event) throws BackoutException
        {
            return speculating_on.get_val(active_event);
        }
        public void set_val(ActiveEvent active_event, T new_val)
            throws BackoutException
        {
            speculating_on.set_val(active_event,new_val);
        }
        
        /**
           When we commit an entry that we have been speculating on,
           we must notify all those that were speculating on that
           entry to commit to master object instead.
         */
        public void commit_entry(AtomicObject<T,D> to_replace_with)
        {
            Util.logger_warn(
                "FIXME: must fill in commit_entry method " +
                "for SpeculationEntry");
        }
    }
    
}
    