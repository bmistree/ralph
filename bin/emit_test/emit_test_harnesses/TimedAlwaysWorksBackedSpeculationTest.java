package emit_test_harnesses;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import RalphExtended.IHardwareChangeApplier;
import static emit_test_harnesses.AlwaysWorksBackedSpeculationTest.AlwaysZeroStateSupplier;
import emit_test_harnesses.AlwaysWorksBackedSpeculationTest;

public class TimedAlwaysWorksBackedSpeculationTest
{
    private final static int NUM_OPS_TO_RUN_PER_THREAD = 10;
    private final static int SLEEP_TIME_MS = 500;
    public static void main(String[] args)
    {
        if (run_test())
        {
            System.out.println(
                "\nSUCCESS in TimedAlwaysWorksBackedSpeculationTest\n");
        }
        else
        {
            System.out.println(
                "\nFAILURE in TimedAlwaysWorksBackedSpeculationTest\n");
        }
    }

    public static boolean run_test()
    {
        boolean worked = true;
        AtomicLong speculation_time = new AtomicLong(0);
        AtomicLong no_speculation_time = new AtomicLong(0);
        AtomicInteger num_ops_set_on_hardware =
            AlwaysWorksBackedSpeculationTest.num_ops_set_on_hardware;
        
        // run under speculation
        num_ops_set_on_hardware.set(0);
        worked = AlwaysWorksBackedSpeculationTest.run_test(
            new DelayedAlwaysSucceedsOnHardware(num_ops_set_on_hardware),
            new DelayedAlwaysSucceedsOnHardware(num_ops_set_on_hardware),
            new AlwaysZeroStateSupplier(), new AlwaysZeroStateSupplier(),
            true,speculation_time,NUM_OPS_TO_RUN_PER_THREAD);

        if (! worked)
            return false;
        
        // run under no speculation
        num_ops_set_on_hardware.set(0);
        worked = AlwaysWorksBackedSpeculationTest.run_test(
            new DelayedAlwaysSucceedsOnHardware(num_ops_set_on_hardware),
            new DelayedAlwaysSucceedsOnHardware(num_ops_set_on_hardware),
            new AlwaysZeroStateSupplier(), new AlwaysZeroStateSupplier(),
            false,no_speculation_time,NUM_OPS_TO_RUN_PER_THREAD);

        if (! worked)
            return false;
        
        // speculative version should take no more than 70% of time of
        // non-speculative version.  (In fact, should take only ~50% of time,
        // but adding the additional fudge factor to acommodate start/stop,
        // re-orderings, etc.)
        if ((.7*no_speculation_time.get()) < speculation_time.get())
            return false;

        return true;
    }

    /**
       Just ensures that the change always gets applied to hardware.
     */
    public static class DelayedAlwaysSucceedsOnHardware
        implements IHardwareChangeApplier<Double>
    {
        private final AtomicInteger num_ops_set_on_hardware;

        public DelayedAlwaysSucceedsOnHardware(
            AtomicInteger _num_ops_set_on_hardware)
        {
            num_ops_set_on_hardware = _num_ops_set_on_hardware;
        }

        
        @Override
        public boolean apply(Double to_apply)
        {
            try
            {
                Thread.sleep(SLEEP_TIME_MS);
            }
            catch(InterruptedException ex)
            {
                ex.printStackTrace();
                assert(false);
            }
            
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
    }
}