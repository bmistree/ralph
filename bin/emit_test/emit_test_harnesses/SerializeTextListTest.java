package emit_test_harnesses;

import ralph_emitted.SerializeTextListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

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
            SameHostConnection conn_obj = new SameHostConnection();
            ListSerializer side_a = ListSerializer.external_create(
                new RalphGlobals(params_a),conn_obj);
            ListSerializer side_b = ListSerializer.external_create(
                new RalphGlobals(params_b),conn_obj);

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