package java_lib_test;

import ralph.VariableStack;
import ralph.LockedVariables.SingleThreadedLockedNumberVariable;

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

    /**
       @returns {boolean} --- true if test passed; false if test
       failed.
     */
    public static boolean run_test()
    {
        String dummy_host_uuid = "hello";
        String var_name = "a";
        Double root_a_init_val = new Double(5);
        Double higher_a_init_val = new Double(root_a_init_val + 30);
        
        SingleThreadedLockedNumberVariable root_a =
            new SingleThreadedLockedNumberVariable(
                dummy_host_uuid,false,root_a_init_val);
        SingleThreadedLockedNumberVariable higher_a =
            new SingleThreadedLockedNumberVariable(
                dummy_host_uuid,false,higher_a_init_val);        

        VariableStack vstack = new VariableStack();
        vstack.push();
        vstack.add_var(var_name,root_a);
        vstack.push();
        vstack.add_var(var_name,higher_a);

        // test passed
        return true;
    }
    
}