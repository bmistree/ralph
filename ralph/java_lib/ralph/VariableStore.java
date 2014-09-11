package ralph;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.List;
import RalphExceptions.BackoutException;

public class VariableStore 
{
    private final Map<String,RalphObject> name_to_var_map = 
        new HashMap<String, RalphObject>();

    /**
       defer statements have different semantics for when a function's
       frame goes out of scope compared to a block.  When a function
       frame goes out of scope, execute all pending defer blocks.
     */
    private final boolean function_scope;
    // highest indices get run first; lowest indices get run last.
    private List<DeferBlock> defer_stack = null;
    
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
     @param {RalphObject} ralph_variable 
    */
    public void add_var(String unique_name,RalphObject ralph_variable)
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
       Should only be called on global variable store used by endpoint
       (likely in endpoint constructor).  We want to keep track of
       mapping between endpoint-visible variable names and their
       uuids.  This way, can reconstruct an endpoint, rather than just
       reconstruct a bunch of individual objects.
     */
    public void save_root_reachable_data(
        RalphGlobals ralph_globals,String endpoint_uuid)
    {
        // logging is off: no need to stamp data as being reachable.
        if (ralph_globals.local_version_manager == null)
            return;

        long local_lamport_time =
            ralph_globals.clock.get_and_increment_int_timestamp();
        
        for (Entry<String,RalphObject> entry : name_to_var_map.entrySet())
        {
            String var_name = entry.getKey();
            RalphObject ralph_object = entry.getValue();
            ralph_globals.local_version_manager.save_endpoint_global_mapping(
                var_name, ralph_object.uuid(),endpoint_uuid,
                local_lamport_time);
        }
    }
    
    /**
     *
     @param {String} unique_name --- 
     @returns {RalphObject or null} --- None if variable does
     not exist, RalphObject otherwise.
    */
    public RalphObject get_var_if_exists(String unique_name) 
    {
        return name_to_var_map.get(unique_name);
    }
}


