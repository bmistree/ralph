package emit_test_harnesses;

import java.lang.Math;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import ralph_emitted.SerializeMapOfStructsJava.MapSerializer;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;

public class SerializeMapOfStructs
{
    // Using inexact check for floating point equality between doubles
    private final static double THRESHOLD_DELTA_BETWEEN_DOUBLES = .000001;
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;


    private final static Random rand = new Random();

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeMapOfStructs\n");
        else
            System.out.println("\nFAILURE in SerializeMapOfStructs\n");
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

            // connect hosts a and b, via a tcp connection
            Thread.sleep(500);
            Ralph.tcp_connect("127.0.0.1", TCP_CONNECTION_PORT_B, globals_a);
            Thread.sleep(500);


            // Instantiate MapSerializer and have it build a remote copy
            MapSerializer side_a = MapSerializer.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    MapSerializer.factory, globals_a);
            side_a.install_partner(service_receiver_factory_to_send);
            
            // tests atomic number map serialization
            if (! map_num_sum_test(true,side_a))
                return false;
            // tests non atomic number map serialization
            if (! map_num_sum_test(false,side_a))
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }


    public static boolean map_num_sum_test(
        boolean atom,MapSerializer to_call_on)
        throws Exception
    {
        List<Double[]> num_vals_to_sum = new ArrayList<Double[]>(
            Arrays.asList(
                new Double [] {1.0, 2.0, 18.0, 20.0},
                new Double [] {-1.0, 2.0, 8.0, 20.0},
                new Double [] {1.0, 30.0, 8.0, 20.0},
                new Double [] {1.0, 2.0, 8.0, 2.0},
                new Double [] {1.0, 2.0, -8.0, 2.0},
                new Double [] {1.0, 2.3, 8.5, 20.0},
                new Double [] {1.0, .4, 8.0, 20.0}));

        for (int i = 0; i < 20; ++i)
        {
            num_vals_to_sum.add(
                new Double [] {
                    rand.nextDouble(),
                    rand.nextDouble(),
                    rand.nextDouble(),
                    rand.nextDouble()});
        }


        for (int i = 0; i < num_vals_to_sum.size(); ++i)
        {
            Double [] four_tuple = num_vals_to_sum.get(i);
            Double a = four_tuple[0];
            Double b = four_tuple[1];
            Double c = four_tuple[2];
            Double d = four_tuple[3];
            Double expected_value = a+b+c+d;

            Double result = null;
            if (atom)
                result = to_call_on.atom_sum_numbers(a,b,c,d);
            else
                result = to_call_on.sum_numbers(a,b,c,d);

            if (! threshold_equal(result,expected_value))
                return false;
        }
        return true;
    }

    public static boolean threshold_equal(double a, double b)
    {
        return Math.abs(a-b) < THRESHOLD_DELTA_BETWEEN_DOUBLES;
    }

}