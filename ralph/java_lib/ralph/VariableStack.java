package ralph;

import java.util.ArrayList;

public class VariableStack
{
    // indices at end are at top of stack.
    private ArrayList<VariableStore> scope_stack =
        new ArrayList<VariableStore>();
    
    public VariableStack()
    {}

    /**
    only used when forking, do not use otherwise
    */
    public VariableStack(ArrayList<VariableStore> new_stack)
    {
        scope_stack = new_stack;
    }
    

    public VariableStack fork_stack()
    {
        return new VariableStack(
            (ArrayList<VariableStore>)scope_stack.clone());
    }

    /**
       @param {boolean} is_function_scope --- defer statements have
       different semantics for when a function's frame goes out of
       scope compared to a block.  When a function frame goes out of
       scope, execute all pending defer blocks.
    */
    public void push(boolean is_function_scope)
    {
        push(new VariableStore(is_function_scope));
    }

    public void push(VariableStore vstore)
    {
        scope_stack.add(vstore);
    }
    
    public void pop()
    {
        VariableStore vstore = scope_stack.remove(scope_stack.size() - 1);
        vstore.out_of_scope();
    }

    
    /**
       @param {DeferBlock} defer_block --- Runs after the outside
       function frame goes out of scope.
    */
    public void add_defer(DeferBlock defer_block)
    {
        // find the first function frame and add the defer statement
        // to it.
        for (int i = scope_stack.size() - 1; i >= 0; --i)
        {
            VariableStore vstore = scope_stack.get(i);
            if (vstore.is_func_scope())
            {
                vstore.add_defer(defer_block);
                break;
            }
        }
    }
    
    /**
     *
     @param {String} unique_name --- 
     @returns {_WaldoVariable or None} --- None if variable does
     not exist, _WaldoVariable otherwise.
    */
    public RalphObject get_var_if_exists(String unique_name)
    {
        for (int index = scope_stack.size() - 1; index >= 0; --index)
        {
            VariableStore vstore = scope_stack.get(index);
            RalphObject var = vstore.get_var_if_exists(unique_name);
            if (var != null)
                return var;
        }
        return null;
    }


    /**
     @param {String} unique_name ---

     @param {_WaldoVariable} waldo_variable 
    */
    public void add_var(String unique_name,RalphObject ralph_variable)
    {
        //DEBUG
        if (scope_stack.size() == 0)
            Util.logger_assert("Error: trying to add to empty variable stack");
        //END DEBUG

        VariableStore vstore = scope_stack.get(scope_stack.size() -1 );

        vstore.add_var(unique_name,ralph_variable);
    }
    
}