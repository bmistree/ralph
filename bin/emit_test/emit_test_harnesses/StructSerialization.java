package emit_test_harnesses;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import ralph_emitted.StructSerializationJava.StructSerializer;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;

public class StructSerialization
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in StructSerialization\n");
        else
            System.out.println("\nFAILURE in StructSerialization\n");
    }

    public static boolean run_test()
    {
        Random rand = new Random();
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


            // Instantiate StructSerializer and have it build a remote copy
            StructSerializer side_a = StructSerializer.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    StructSerializer.factory, globals_a);
            side_a.install_partner(service_receiver_factory_to_send);


            List<SerializerTest> tests_to_run =
                new ArrayList<SerializerTest>(
                    Arrays.asList(
                        new SerializerTest(3.0,1.5,side_a),
                        new SerializerTest(5.0,1.5,side_a),
                        new SerializerTest(-5.0,1.5,side_a),
                        new SerializerTest(-5.0,22.0,side_a)));
            for (int i = 0; i < 20; ++i)
            {
                tests_to_run.add(
                    new SerializerTest(
                        rand.nextDouble(),rand.nextDouble(),side_a));
            }


            for (SerializerTest st : tests_to_run)
            {
                if (! st.run_test())
                    return false;
            }

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    private static class SerializerTest
    {
        private final double num_a;
        private final double num_b;
        private final StructSerializer serializer;

        public SerializerTest(
            double _num_a, double _num_b, StructSerializer _serializer)
        {
            num_a = _num_a;
            num_b = _num_b;
            serializer = _serializer;
        }

        public boolean run_test() throws Exception
        {
            Double result =
                serializer.sum_struct_fields_on_other_side(num_a,num_b);
            if (! result.equals(num_a + num_b))
                return false;
            return true;
        }
    }
}