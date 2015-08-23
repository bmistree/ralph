package emit_test_harnesses;

import ralph_emitted.BackedSpeculationJava.BackedSpeculation;
import ralph_emitted.BackedSpeculationJava._InternalSwitch;
import ralph.RalphGlobals;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import ralph.ActiveEvent;

import RalphExtended.WrapApplyToHardware;
import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;
import static emit_test_harnesses.BackedSpeculationTestLib.create_switch;
import static emit_test_harnesses.BackedSpeculationTestLib.EventThread;
import java.util.concurrent.atomic.AtomicLong;

public class AlwaysWorksBackedSpeculationTest
{
    private final static int NUM_OPS_PER_THREAD = 5000;
    // gets incremented each time a switch applies to hardware and
    // decremented each time a switch undoes.  In aggregate then, this
    // should be the number of completed events that each switch
    // processes (or NUM_OPS_PER_THREAD*2... x2 because each event
    // hits 2 switches).
    public final static AtomicInteger num_ops_set_on_hardware =
        new AtomicInteger(0);

    private final static RalphGlobals ralph_globals = new RalphGlobals();

    public static void main(String[] args)
    {
        if (AlwaysWorksBackedSpeculationTest.run_test())
            System.out.println("\nSUCCESS in AlwaysWorksBackedSpeculationTest\n");
        else
            System.out.println("\nFAILURE in AlwaysWorksBackedSpeculationTest\n");
    }


    public static boolean run_test()
    {
        return run_test(
            new AlwaysSucceedsOnHardware(),new AlwaysSucceedsOnHardware(),
            new AlwaysZeroStateSupplier(), new AlwaysZeroStateSupplier());
    }

    public static boolean run_test(
        IHardwareChangeApplier<Double> hardware_change_applier_switch1,
        IHardwareChangeApplier<Double> hardware_change_applier_switch2,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch1,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch2)
    {
        return run_test(
            hardware_change_applier_switch1, hardware_change_applier_switch2,
            hardware_state_supplier_switch1, hardware_state_supplier_switch2,
            true,null,NUM_OPS_PER_THREAD);
    }

    /**
       @param time_to_run --- Returns the time it took to run both
       events.  Can be null.  If it's null, then do not report it.
     */
    public static boolean run_test(
        IHardwareChangeApplier<Double> hardware_change_applier_switch1,
        IHardwareChangeApplier<Double> hardware_change_applier_switch2,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch1,
        IHardwareStateSupplier<Double> hardware_state_supplier_switch2,
        boolean speculation_on,AtomicLong time_to_run, int num_ops_to_run)
    {
        try
        {
            BackedSpeculation endpt =
                BackedSpeculation.create_single_sided(ralph_globals);

            _InternalSwitch switch1 =
                create_switch(
                    ralph_globals,speculation_on,
                    hardware_change_applier_switch1,
                    hardware_state_supplier_switch1);

            _InternalSwitch switch2 =
                create_switch(
                    ralph_globals,speculation_on,
                    hardware_change_applier_switch2,
                    hardware_state_supplier_switch2);

            endpt.set_switches(switch1,switch2);

            AtomicBoolean had_exception = new AtomicBoolean(false);
            EventThread event_1 =
                new EventThread(endpt,false,num_ops_to_run,had_exception);
            EventThread event_2 =
                new EventThread(endpt,true,num_ops_to_run,had_exception);

            long start = System.nanoTime();

            event_1.start();
            event_2.start();
            event_1.join();
            event_2.join();

            long end = System.nanoTime();

            if (time_to_run != null)
                time_to_run.set(end - start);


            if (had_exception.get())
                return false;

            // see note on this condition above in documenation for
            // num_ops_set_on_hardware.
            if (num_ops_set_on_hardware.get() != (2*num_ops_to_run))
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    /**
       Just ensures that the change always gets applied to hardware.
     */
    public static class AlwaysSucceedsOnHardware
        implements IHardwareChangeApplier<Double>
    {
        @Override
        public boolean apply(Double to_apply)
        {
            num_ops_set_on_hardware.getAndIncrement();
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            // although will not get undo because hardware could not
            // comply, may get an undo message if preempted by another
            // event.
            num_ops_set_on_hardware.getAndDecrement();
            return true;
        }
        @Override
        public boolean partial_undo(Double to_undo) {
            return false;
        }
    }

    /**
       Always returns zero for state that want to apply.
     */
    public static class AlwaysZeroStateSupplier
        implements IHardwareStateSupplier<Double>
    {
        @Override
        public Double get_state_to_push(ActiveEvent active_event)
        {
            return 0.0;
        }
    }

}