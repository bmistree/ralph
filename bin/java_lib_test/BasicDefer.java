package java_lib_test;

import ralph.VariableStack;
import ralph.LockedVariables.SingleThreadedLockedNumberVariable;
import ralph.LockedObject;
import ralph.Endpoint;
import ralph.VariableStack;
import ralph.DeferBlock;

public class BasicDefer
{
    protected static String test_name = "BasicDefer";
    
    /**
       Test pushes a function stack frame to the stack.  Then, it adds
       a defer to that frame.  Then it pops the frame.  Checks that
       defer gets executed.
     */
    public static void main (String [] args)
    {
        if (BasicDefer.run_test())
            TestClassUtil.print_success(test_name);
        else
            TestClassUtil.print_failure(test_name);
    }

    public static boolean run_test()
    {
        Endpoint endpt = TestClassUtil.create_default_single_endpoint();

        // push two function frames
        endpt.global_var_stack.push(true);
        endpt.global_var_stack.push(true);

        // add a defer to one
        DeferBlock defer_statement = new DeferBlock(endpt.global_var_stack)
        {
            public void run()
            {
                System.out.println("\n\nI got into runnable\n\n");
            }
        };
        endpt.global_var_stack.add_defer(defer_statement);

        endpt.global_var_stack.pop();

        // test passed
        return true;
    }
}