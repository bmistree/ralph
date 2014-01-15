package emit_test_harnesses;

import emit_test_package.HardwareFailure;
import emit_test_package.HardwareFailure.HardwareOwner;
import emit_test_package.HardwareFailure._InternalPieceOfHardware;
import RalphConnObj.SingleSideConnection;
import ralph.RalphGlobals;
import RalphDataWrappers.ListTypeDataWrapper;
import ralph.ExtendedVariables.ExtendedInternalAtomicList;
import ralph.Variables.AtomicListVariable;
import ralph.AtomicInternalList;
import ralph.ActiveEvent;
import RalphExceptions.BackoutException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;
import RalphAtomicWrappers.BaseAtomicWrappers;


public class HardwareFailureTest
{
    public static void main(String [] args)
    {
        if (HardwareFailureTest.run_test())
            System.out.println("\nSUCCESS in HardwareFailureTest\n");
        else
            System.out.println("\nFAILURE in HardwareFailureTest\n");
    }

    public static _InternalPieceOfHardware produce_two_op_fail_hardware()
    {
        _InternalPieceOfHardware to_return = new _InternalPieceOfHardware();
        ExtendedInternalHardwareList internal_hardware_list =
            new ExtendedInternalHardwareList();
        
        to_return.list =
            new AtomicListVariable<Double,Double>(
                "",false,internal_hardware_list,
                BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);

        return to_return;
    }
    
    public static boolean run_test()
    {
        try
        {
            String dummy_host_uuid = "dummy_host_uuid";
            HardwareOwner endpt = new HardwareOwner(
                new RalphGlobals(),
                dummy_host_uuid,
                new SingleSideConnection());

            _InternalPieceOfHardware hardware_to_add = produce_two_op_fail_hardware();
            double hardware_id = 1.0;
            endpt.add_piece_of_hardware(hardware_id,hardware_to_add);

            if (endpt.num_pieces_of_hardware().doubleValue() != 1.0)
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
    {
        public boolean undo_changes_called = false;
        private boolean next_time_fail_commit = false;
        // after hardware fails, cannot perform any more operations on
        // piece of hardware.
        private boolean hardware_failed = true;

        public ExtendedInternalHardwareList()
        {
            super(BaseAtomicWrappers.NON_ATOMIC_NUMBER_WRAPPER);
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
                (ListTypeDataWrapper<Double,Double>)super.acquire_read_lock(active_event);
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
                (ListTypeDataWrapper<Double,Double>)super.acquire_write_lock(active_event);
        }
        
        /**
           Can apply commit one time.  Second time try to commit, fails.
         */
        @Override
        protected boolean apply_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> dirty)
        {
            if (next_time_fail_commit)
            {
                hardware_failed = true;
                return false;
            }

            next_time_fail_commit = true;
            return true;
        }
        @Override
        protected void undo_dirty_changes_to_hardware(
            ListTypeDataWrapper<Double,Double> to_undo)
        {
            undo_changes_called = true;
        }
    }
}
