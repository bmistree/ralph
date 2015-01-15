package emit_test_harnesses;

import ralph_emitted.NullJava.NullService;
import ralph.RalphGlobals;

public class NullTest
{
    public static void main(String[] args)
    {
        if (NullTest.run_test())
            System.out.println("\nSUCCESS in NullTest\n");
        else
            System.out.println("\nFAILURE in NullTest\n");
    }

    public static boolean run_test()
    {
        try
        {
            NullService service =
                NullService.create_single_sided(new RalphGlobals());

            /** Test value types */
            if (service.get_number() != null)
                return false;

            if (service.get_text() != null)
                return false;

            if (service.get_tf() != null)
                return false;

            Double [] doubles_to_test = {3.3,45.,null,null,30.1,null};
            for (Double d : doubles_to_test)
            {
                if (! check_set_number(service,d))
                    return false;
            }

            String [] strings_to_test = {"hi","hi",null,"hie",null,"mm"};
            for (String s : strings_to_test)
            {
                if (! check_set_text(service,s))
                    return false;
            }

            Boolean [] bools_to_test = {true,false,null,null,false,false,true};
            for (Boolean b : bools_to_test)
            {
                if (! check_set_tf(service,b))
                    return false;
            }
            
            
            /** Test container types */
            if (service.get_map() != null)
                return false;
            service.reset_map();
            Double [] index_list = {3.3, 4.5,null,3.3,null,30.1, 3.3};
            Double [] value_list = {3.3,null,null,3.3,null,30.1,null};
            if (! check_set_map_indices(service,index_list,value_list))
                return false;

            if (! service.is_struct_null().booleanValue())
                return false;
            service.reset_struct();
            if (service.get_internal_val_struct() != null)
                return false;
                        
            
            
            /** Test null comparison */
            if (service.is_null(32.).booleanValue())
                return false;
            if (! service.is_null(null).booleanValue())
                return false;
            
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
       Returns true if test passed, false if it failed.
     */
    private static boolean check_set_number(
        NullService service,Double to_set_to) throws Exception
    {
        service.set_number(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_number();

        return service.get_number().equals(to_set_to);
    }

    private static boolean check_set_text(
        NullService service,String to_set_to) throws Exception
    {
        service.set_text(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_text();

        return service.get_text().equals(to_set_to);
    }
    
    private static boolean check_set_tf(
        NullService service,Boolean to_set_to) throws Exception
    {
        service.set_tf(to_set_to);

        if (to_set_to == null)
            return to_set_to == service.get_tf();

        return service.get_tf().equals(to_set_to);
    }

    /**
       Note: assumes that index_list and value list are same size.
     */
    private static boolean check_set_map_indices(
        NullService service, Double [] index_list, Double [] value_list)
        throws Exception
    {
        if (index_list.length != value_list.length)
        {
            System.err.println(
                "Tester err index and value list should be same size");
            return false;
        }

        int num_to_test = index_list.length;

        for (int i = 0; i < num_to_test; ++i)
        {
            Double index = index_list[i];
            Double value = value_list[i];
            service.set_element(index,value);

            Double actual_value = service.get_element(index);
            if (value == null)
                return value == actual_value;

            if (! value.equals(actual_value))
                return false;
        }
        return true;
    }
}
