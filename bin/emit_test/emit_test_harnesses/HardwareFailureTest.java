package emit_test_harnesses;

import ralph_emitted.HardwareFailureJava;
import ralph_emitted.HardwareFailureJava.HardwareOwner;
import ralph_emitted.HardwareFailureJava._InternalPieceOfHardware;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import RalphDataWrappers.ListTypeDataWrapper;
import ralph.ExtendedVariables.ExtendedInternalAtomicList;
import ralph.Variables.AtomicListVariable;
import ralph.AtomicInternalList;
import ralph.ActiveEvent;
import RalphExceptions.BackoutException;
import ralph.EventPriority.IsSuperFlag;
import ralph.ICancellableFuture;
import RalphAtomicWrappers.BaseAtomicWrappers;
import static ralph.FutureAlwaysValue.ALWAYS_TRUE_FUTURE;
import static ralph.FutureAlwaysValue.ALWAYS_FALSE_FUTURE;


import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;


public class HardwareFailureTest
{
    final static AtomicBoolean problem = new AtomicBoolean(false);
    final static AtomicBoolean undo_changes_called = new AtomicBoolean(false);
    
    public static void main(String [] args)
    {
        if (HardwareFailureTest.run_test())
            System.out.println("\nSUCCESS in HardwareFailureTest\n");
        else
            System.out.println("\nFAILURE in HardwareFailureTest\n");
    }

    public static _InternalPieceOfHardware produce_two_op_fail_hardware(
        double hardware_id, HardwareOwner endpt)
    {
        _InternalPieceOfHardware to_return =
            new _InternalPieceOfHardware(endpt.ralph_globals);
        ExtendedInternalHardwareList internal_hardware_list =
            new ExtendedInternalHardwareList(hardware_id,endpt);
        
        to_return.list =
            new AtomicListVariable<Double,Double>(
                false,internal_hardware_list,
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
                endpt.ralph_globals);

        return to_return;
    }
    
    public static boolean run_test()
    {
        try
        {
            HardwareOwner endpt = new HardwareOwner(
                new RalphGlobals(), new SingleSideConnection());

            double hardware_id = 1.0;
            _InternalPieceOfHardware hardware_to_add =
                produce_two_op_fail_hardware(hardware_id,endpt);
            endpt.add_piece_of_hardware(hardware_id,hardware_to_add);

            if (endpt.num_pieces_of_hardware().doubleValue() != 1.0)
                return false;

            // should be able to add a number one time to internal list
            if (!endpt.append_num_to_hardware(hardware_id,1.0).booleanValue())
                return false;

            // second time append number should fail
            if (endpt.append_num_to_hardware(hardware_id,1.0).booleanValue())
                return false;

            if (problem.get())
                return false;

            Thread.sleep(50);
            if (!undo_changes_called.get())
                return false;
            
        }
        catch (Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
        
        return true;
    }



    /**
       Allows committing to once.  Next time commit, it will fail and
       lock object so that cannot continue operations on it.
     */
    private static class ExtendedInternalHardwareList
        extends ExtendedInternalAtomicList<Double,Double>
        implements Runnable
    {
        private boolean next_time_fail_commit = false;
        // after hardware fails, cannot perform any more operations on
        // piece of hardware.
        private boolean hardware_failed = false;
        private double hardware_id;
        private HardwareOwner endpt;

        public ExtendedInternalHardwareList(
            double _hardware_id, HardwareOwner _endpt)
        {
            super(
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER,
                _endpt.ralph_globals);
            endpt = _endpt;
            hardware_id = _hardware_id;
        }

        /**
           @see discussion above hardware_failed private variable.
         */
        @Override
        protected ListTypeDataWrapper<Double,Double>  acquire_read_lock(
            ActiveEvent active_event) throws BackoutException
        {
            // if hardware has failed, cannot operate on data anymore:
            // will backout event.  relies on another event that doesn't
            // actually operate on routing table list to remove all
            // references to it.  See discussion above in hardware_failed.
            if (hardware_failed)
                throw new BackoutException();

            return
                (ListTypeDataWrapper<Double,Double>)
                super.acquire_read_lock(active_event);
        }

        /**
           @see discussion above hardware_failed private variable.
         */
        @Override
        protected ListTypeDataWrapper<Double,Double> acquire_write_lock(
            ActiveEvent active_event) throws BackoutException
        {
            if (hardware_failed)
                throw new BackoutException();

            return
                (ListTypeDataWrapper<Double,Double>)super.acquire_write_lock(
                    active_event);
        }
        
        /**
           Can apply commit one time.  Second time try to commit, fails.
         */
        @Override
        protected ICancellableFuture apply_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> dirty)
        {
            if (next_time_fail_commit)
            {
                hardware_failed = true;
                Thread t = new Thread(this);
                t.start();
                return ALWAYS_FALSE_FUTURE;
            }

            next_time_fail_commit = true;
            return ALWAYS_TRUE_FUTURE;
        }
        @Override
        protected void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> to_undo)
        {
            undo_changes_called.set(true);
        }

        @Override
        public void run()
        {
            // cleans up the crashed piece of hardware
            try {
                endpt.remove_piece_of_hardware(hardware_id,IsSuperFlag.SUPER);
            } catch (Exception _ex) {
                _ex.printStackTrace();
                problem.set(true);
            }
        }
    }
}
