package ralph;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.ArrayList;
import RalphExceptions.BackoutException;

public class VariableStore 
{
    private HashMap<String,LockedObject> name_to_var_map = 
        new HashMap<String, LockedObject>();

    /**
       defer statements have different semantics for when a function's
       frame goes out of scope compared to a block.  When a function
       frame goes out of scope, execute all pending defer blocks.
     */
    private boolean function_scope = true;
    // highest indices get run first; lowest indices get run last.
    private ArrayList<DeferBlock> defer_stack = null;
    
    public VariableStore(boolean _function_scope)
    {
        function_scope = _function_scope;
    }

    public boolean is_func_scope()
    {
        return function_scope;
    }

    /**
       defer statements get executed when the function in which they
       are defined completes.
     */
    public void add_defer (DeferBlock defer_statement)
    {
        // DEBUG
        if (! function_scope)
        {
            Util.logger_assert(
                "Should only add a defer statement to a function stack.");
        }
        // END DEBUG

        if (defer_stack == null)
            defer_stack = new ArrayList<DeferBlock>();

        defer_stack.add(defer_statement);
    }

    /**
       When this store goes out of frame, check if it has any defer
       statements it needs to execute for cleanup.
     */
    public void out_of_scope()
    {
        if (defer_stack == null)
            return;

        for (int i = defer_stack.size() - 1; i >= 0; --i)
        {
            DeferBlock defer_to_run = defer_stack.get(i);
            defer_to_run.run();
        }
    }
    
    
    /**
     @param {String} unique_name ---
     @param {LockedObject} ralph_variable 
    */
    public void add_var(String unique_name,LockedObject ralph_variable)
    {
        // DEBUG
        if (get_var_if_exists(unique_name) != null)
        {
            Util.logger_assert(
                "Already had an entry for variable trying to " +
                "insert into store.");
        }
        //#### END DEBUG
          
        name_to_var_map.put(unique_name, ralph_variable); 
    }

    /**
     *
     @param {String} unique_name --- 
     @returns {LockedObject or null} --- None if variable does
     not exist, LockedObject otherwise.
    */
    public LockedObject get_var_if_exists(String unique_name) 
    {
        return name_to_var_map.get(unique_name);
    }

}


