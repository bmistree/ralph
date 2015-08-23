package emit_test_harnesses;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ralph.ActiveEvent;
import RalphExtended.IHardwareChangeApplier;
import RalphExtended.IHardwareStateSupplier;

public class RandomDelaysAlwaysWorksBackedSpeculationTest
{
    private static Random rand = new Random();

    public static void main(String[] args)
    {
        if (run_test())
        {
            System.out.println(
                "\nSUCCESS in RandomDelaysAlwaysWorksBackedSpeculationTest\n");
        }
        else
        {
            System.out.println(
                "\nFAILURE in RandomDelaysAlwaysWorksBackedSpeculationTest\n");
        }
    }

    public static boolean run_test()
    {
        return AlwaysWorksBackedSpeculationTest.run_test(
            new RandomDelaysAlwaysSucceedsOnHardware(),
            new RandomDelaysAlwaysSucceedsOnHardware(),
            new RandomDelaysAlwaysZeroStateSupplier(),
            new RandomDelaysAlwaysZeroStateSupplier());
    }

    /**
       Just ensures that the change always gets applied to hardware.
     */
    public static class RandomDelaysAlwaysSucceedsOnHardware
        implements IHardwareChangeApplier<Double>
    {
        // copied in from AlwaysWorksBackedSpeculationTest to keep
        // track of number of commits to hardwaer.
        private final AtomicInteger num_ops_set_on_hardware =
            AlwaysWorksBackedSpeculationTest.num_ops_set_on_hardware;

        @Override
        public boolean apply(Double to_apply)
        {
            num_ops_set_on_hardware.getAndIncrement();
            random_sleep();
            return true;
        }
        @Override
        public boolean undo(Double to_undo)
        {
            // although will not get undo because hardware could not
            // comply, may get an undo message if preempted by another
            // event.
            num_ops_set_on_hardware.getAndDecrement();
            random_sleep();
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
    public static class RandomDelaysAlwaysZeroStateSupplier
        implements IHardwareStateSupplier<Double>
    {
        @Override
        public Double get_state_to_push(ActiveEvent active_event)
        {
            random_sleep();
            return 0.0;
        }
    }

    /**
       Used to insert random delays into system.
     */
    public static void random_sleep()
    {
        try
        {
            Thread.sleep(rand.nextInt(2),rand.nextInt(100000));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            assert(false);
        }
    }
}