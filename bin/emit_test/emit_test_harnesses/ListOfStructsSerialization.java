package emit_test_harnesses;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;


import ralph_emitted.StructListSerializationJava.StructSerializer;
import RalphConnObj.SameHostConnection;
import ralph.RalphGlobals;

public class ListOfStructsSerialization
{
    public static void main(String[] args)
    {
        if (run_test())
            System.out.println("\nSUCCESS in ListOfStructsSerialization\n");
        else
            System.out.println("\nFAILURE in ListOfStructsSerialization\n");
    }

    public static boolean run_test()
    {
        Random rand = new Random();
        
        try
        {
            SameHostConnection conn_obj = new SameHostConnection();
            StructSerializer side_a = new StructSerializer(
                new RalphGlobals(),conn_obj);
            StructSerializer side_b = new StructSerializer(
                new RalphGlobals(),conn_obj);

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


            // run non-atomically
            for (SerializerTest st : tests_to_run)
            {
                if (! st.run_test(false))
                    return false;
            }
            // run atomically
            for (SerializerTest st : tests_to_run)
            {
                if (! st.run_test(true))
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

        public boolean run_test(boolean atomic) throws Exception
        {
            Double result = null;
            if (atomic)
            {
                result =
                    serializer.atom_sum_list_of_struct_fields_on_other_side(
                        num_a,num_b);
            }
            else
            {
                result =
                    serializer.sum_list_of_struct_fields_on_other_side(
                        num_a,num_b);
            }
            
            if (! result.equals(num_a + num_b))
                return false;
            return true;
        }
    }
}