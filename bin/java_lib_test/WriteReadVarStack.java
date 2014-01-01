package java_lib_test;

import ralph.VariableStack;
import ralph.Variables.SingleThreadedLockedNumberVariable;
import ralph.RalphObject;

public class WriteReadVarStack
{
    protected static String test_name = "WriteReadVarStack";
    
    /**
       Test pushes a stack frame to the stack.  Then, it adds a
       variable, named 'a'.  Following this, it adds a new stack frame
       and adds another variable, also named 'a'.  Modifies 'a' and
       checks that those modifications persist.  Then, pops stack
       frame and checks that original 'a' is not modified.
     */
    public static void main (String [] args)
    {
        if (WriteReadVarStack.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static Double get_number_from_stack (
        VariableStack vstack, String var_name)
    {
        RalphObject<Double,Double> lo =
            (RalphObject<Double,Double>)vstack.get_var_if_exists(var_name);

        Double val = null;
        try
        {
            val = lo.get_val(null);
            return val;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assert(false);
        }
        return val;
    }

    public static void set_number_on_stack(
        VariableStack vstack,String var_name,Double val_to_set_to)
    {
        RalphObject<Double,Double> lo =
            (RalphObject<Double,Double>)vstack.get_var_if_exists(var_name);
        try
        {
            lo.set_val(null,val_to_set_to);
            return;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            assert(false);
        }
    }
    
    /**
       @returns {boolean} --- true if test passed; false if test
       failed.
     */
    public static boolean run_test()
    {
        String dummy_host_uuid = "hello";
        String var_name = "a";
        String var_name_b = "b";
        Double root_a_init_val = new Double(5);
        Double root_b_init_val = new Double(38);
        Double higher_a_init_val = new Double(root_a_init_val + 30);

        /** Create new variables */
        SingleThreadedLockedNumberVariable root_a =
            new SingleThreadedLockedNumberVariable(
                dummy_host_uuid,false,root_a_init_val);
        SingleThreadedLockedNumberVariable higher_a =
            new SingleThreadedLockedNumberVariable(
                dummy_host_uuid,false,higher_a_init_val);        

        SingleThreadedLockedNumberVariable root_b =
            new SingleThreadedLockedNumberVariable(
                dummy_host_uuid,false,root_b_init_val);        

        
        /** Push new variables to stack */
        VariableStack vstack = new VariableStack();
        vstack.push(false);
        vstack.add_var(var_name,root_a);
        vstack.add_var(var_name_b,root_b);
        vstack.push(false);
        vstack.add_var(var_name,higher_a);

        /** Check can operate on variables on top of stack. */
        if (! get_number_from_stack(vstack,var_name).equals(
                higher_a_init_val))
            return false;
        

        Double new_higher_val = higher_a_init_val + 1;
        set_number_on_stack(vstack,var_name,new_higher_val);

        if (! get_number_from_stack(vstack,var_name).equals(
                new_higher_val))
            return false;

        /** Check can operate on non-shadowed variable in lower layer
         * of stack. */
        if (! get_number_from_stack(vstack,var_name_b).equals(
                root_b_init_val))
            return false;

        Double new_b_val = root_b_init_val + 1;
        set_number_on_stack(vstack,var_name_b,new_b_val);
        
        if (! get_number_from_stack(vstack,var_name_b).equals(
                new_b_val))
            return false;

        /** Pop stack and ensure that shadowed variable at root of
         * stack was not affected and that unshadowed variable was.*/
        vstack.pop();
        Double root_a_val = get_number_from_stack(vstack,var_name);
        if (! root_a_val.equals(root_a_init_val))
            return false;

        Double b_val  = get_number_from_stack(vstack, var_name_b);
        if (! b_val.equals(new_b_val))
            return false;

        // test passed
        return true;
    }
}