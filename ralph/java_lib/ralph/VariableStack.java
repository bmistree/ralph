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

    public void push()
    {
        scope_stack.add(new VariableStore());
    }

    public void pop()
    {
        scope_stack.remove(scope_stack.size() - 1);
    }

    /**
     *
     @param {String} unique_name --- 
     @returns {_WaldoVariable or None} --- None if variable does
     not exist, _WaldoVariable otherwise.
    */
    public LockedObject get_var_if_exists(String unique_name)
    {
        for (int index = scope_stack.size() - 1; index >= 0; --index)
        {
            VariableStore vstore = scope_stack.get(index);
            LockedObject var = vstore.get_var_if_exists(unique_name);
            if (var != null)
                return var;
        }
        return null;
    }


    /**
     @param {String} unique_name ---

     @param {_WaldoVariable} waldo_variable 
    */
    public void add_var(String unique_name,LockedObject ralph_variable)
    {
        //DEBUG
        if (scope_stack.size() == 0)
            Util.logger_assert("Error: trying to add to empty variable stack");
        //END DEBUG

        VariableStore vstore = scope_stack.get(scope_stack.size() -1 );

        vstore.add_var(unique_name,ralph_variable);
    }
    
}