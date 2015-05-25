package emit_test_harnesses;

import ralph_emitted.SerializeTextListJava.ListSerializer;
import ralph.RalphGlobals;
import ralph.InternalServiceFactory;
import ralph.Ralph;



public class SerializeTextListTest
{
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;

    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeTextListTest\n");
        else
            System.out.println("\nFAILURE in SerializeTextListTest\n");
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


            // Instantiate ListSerializer and have it build a remote copy
            ListSerializer side_a = ListSerializer.external_create(globals_a);
            InternalServiceFactory service_receiver_factory_to_send =
                new InternalServiceFactory(
                    ListSerializer.factory, globals_a);
            side_a.install_partner(service_receiver_factory_to_send);


            // tests atomic text list serialization
            if (! text_list_concat_test(true,side_a))
                return false;

            // tests non atomic text list serialization
            if (! text_list_concat_test(false,side_a))
                return false;

            return true;
        }
        catch(Exception _ex)
        {
            _ex.printStackTrace();
            return false;
        }
    }

    public static boolean text_list_concat_test(
        boolean atom,ListSerializer to_call_on)
        throws Exception
    {
        String[][] test_cases =
            new String [][] {
            {"hello","is","it"},
            {"me","you're","looking"},
            {"for?","I","can"},
            {"see","it","in"},
            {"your","eyes","."}};

        for (String[] single_test : test_cases)
        {
            String a = single_test[0];
            String b = single_test[1];
            String c = single_test[2];

            String expected = a+b+c;

            String result = null;
            if (atom)
                result = to_call_on.atom_concatenate_strings(a,b,c);
            else
                result = to_call_on.concatenate_strings(a,b,c);

            if (! result.equals(expected))
                return false;
        }
        return true;
    }
}