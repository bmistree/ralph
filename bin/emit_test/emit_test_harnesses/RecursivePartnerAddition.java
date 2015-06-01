package emit_test_harnesses;

import ralph_emitted.RecursivePartnerAdditionJava.RecursiveAdder;
import ralph.RalphGlobals;
import ralph.Ralph;
import ralph.InternalServiceFactory;

public class RecursivePartnerAddition
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in RecursivePartnerAddition\n");
        else
            System.out.println("\nFAILURE in RecursivePartnerAddition\n");
    }

    public static boolean run_test()
    {
        RalphGlobals.Parameters params_a = new RalphGlobals.Parameters();
        params_a.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_A;

        RalphGlobals.Parameters params_b = new RalphGlobals.Parameters();
        params_b.tcp_port_to_listen_for_connections_on = TCP_CONNECTION_PORT_B;

        try
        {
            RalphGlobals globals_a = new RalphGlobals(params_a);
            RalphGlobals globals_b = new RalphGlobals(params_b);

            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_A,
                              globals_b);

            // wait for everything to settle down
            Thread.sleep(1000);

            RecursiveAdder side_a = RecursiveAdder.external_create(globals_a);
            InternalServiceFactory receiver_factory =
                new InternalServiceFactory(RecursiveAdder.factory, globals_a);
            side_a.install_partner(receiver_factory);

            Double[] list_first_nums = new Double [] {1.0, 2.0, 7.,5.5};
            Double[] list_second_nums = new Double [] {9.0, 2.2, 3.5,.5};
            Double[] num_times_to_run = new Double [] {10., 15., 3., 4., 5.};

            for (int i = 0; i < list_first_nums.length; ++i)
            {
                Double first_num = list_first_nums[i];
                for (int j = 0; j < list_second_nums.length; ++j)
                {
                    Double second_num = list_second_nums[i];
                    for (int k = 0; k < num_times_to_run.length; ++k)
                    {
                        Double num_to_run = num_times_to_run[k];

                        boolean test_passed =
                            run_single_test(side_a, first_num, second_num,
                                            num_to_run);

                        if (! test_passed)
                            return false;
                    }
                }
            }
            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    /**
       @return true if test passes, false if fails.
     */
    public static boolean run_single_test(
        RecursiveAdder side_a, Double internal_a, Double internal_b,
        Double num_iters) throws Exception
    {
        Double expected_result =
            internal_a*Math.ceil(num_iters/2.) +
            internal_b*Math.floor(num_iters/2.);

        side_a.set_internal_num(internal_a);
        side_a.set_remote_internal_num(internal_b);

        Double result = side_a.partner_recursive_add_to_number(num_iters);

        boolean to_return = double_check_equals(result,expected_result);
        if (! to_return)
        {
            System.out.println(
                "\nFailed: " + internal_a + " " + internal_b +
                " " + num_iters);
            System.out.println(
                "Expected " + expected_result + " actual: " +
                result + "\n");
        }
        return to_return;
    }

    public static boolean double_check_equals(Double a, Double b)
    {
        return Math.abs(a-b) < .00001;
    }
}