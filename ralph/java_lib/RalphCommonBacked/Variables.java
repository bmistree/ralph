package RalphCommonBacked;

import RalphExceptions.BackoutException;
import ralph.Variables.AtomicNumberVariable;
import ralph.ActiveEvent;
import ralph.RalphGlobals;
import ralph.Util;

import java.util.concurrent.Future;

public class Variables
{
    public static class BackedNumberVariable extends AtomicNumberVariable
    {
        protected BackedManager backed_manager = null;
        
        public BackedNumberVariable(
            BackedManager _backed_manager,
            boolean log_changes,Object init_val,RalphGlobals ralph_globals)
        {
            super(log_changes,init_val,ralph_globals);
            backed_manager = _backed_manager;
        }

        @Override
        public void set_val(ActiveEvent active_event,Double new_val)
            throws BackoutException
        {
            backed_manager.note_set(this, active_event);
            super.set_val(active_event,new_val);
        }

        @Override
        public Future<Boolean> first_phase_commit (ActiveEvent active_event)
        {
            // FIXME: Still need to address what should happen on commit.
            Util.logger_assert("Still need to handle first_phase_commit.");
            return null;
        }

        @Override
        public void backout (ActiveEvent active_event)
        {
            backed_manager.note_backed_out(this,active_event);
            super.backout(active_event);
        }
    }
}