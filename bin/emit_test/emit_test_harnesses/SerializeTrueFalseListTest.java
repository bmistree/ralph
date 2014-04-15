package emit_test_harnesses;

import java.util.Random;

import ralph_emitted.SerializeTrueFalseListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeTrueFalseListTest
{
    private final static Random rand = new Random();
    private final static int TCP_CONNECTION_PORT_A = 20494;
    private final static int TCP_CONNECTION_PORT_B = 20495;
    
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeTrueFalseListTest\n");
        else
            System.out.println("\nFAILURE in SerializeTrueFalseListTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            ListSerializer side_a = new ListSerializer(
                new RalphGlobals(TCP_CONNECTION_PORT_A),conn_obj);
            ListSerializer side_b = new ListSerializer(
                new RalphGlobals(TCP_CONNECTION_PORT_B),conn_obj);

            // tests atomic text list serialization
            if (! tf_list_merge_test(true,side_a))
                return false;
            
            // tests non atomic text list serialization
            if (! tf_list_merge_test(false,side_a))
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
       If put in [true, true, true, true], get back:
       "1111"
       [false,true, true, false]:
       "0110"
       etc.
     */
    public static String expected_from_booleans(Boolean [] bool_list)
    {
        String to_return = "";
        for (Boolean b : bool_list)
        {
            if (b.booleanValue())
                to_return += "1";
            else
                to_return += "0";
        }
        return to_return;
    }

    public static boolean tf_list_merge_test(
        boolean atom,ListSerializer to_call_on)
        throws Exception
    {
        for (int i = 0; i < 20; ++i)
        {
            boolean a = rand.nextBoolean();
            boolean b = rand.nextBoolean();
            boolean c = rand.nextBoolean();
            boolean d = rand.nextBoolean();
            String expected = expected_from_booleans(
                new Boolean [] {a,b,c,d});
            String result = null;
            
            if (atom)
                result = to_call_on.atom_merge_true_falses(a,b,c,d);
            else
                result = to_call_on.merge_true_falses(a,b,c,d);
            
            if (! result.equals(expected))
                return false;
        }
        return true;
    }
}