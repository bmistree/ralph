package emit_test_harnesses;

import ralph_emitted.SerializeTextListJava.ListSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class SerializeTextListTest
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in SerializeTextListTest\n");
        else
            System.out.println("\nFAILURE in SerializeTextListTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            ListSerializer side_a = new ListSerializer(
                new RalphGlobals(),conn_obj);
            ListSerializer side_b = new ListSerializer(
                new RalphGlobals(),conn_obj);

            // FIXME: still must test atomic text lists.
            // // tests atomic text list serialization
            // if (! text_list_concat_test(true,side_a))
            //     return false;
            
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