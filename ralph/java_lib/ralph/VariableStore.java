package ralph;

import java.util.HashMap;
import java.util.Map.Entry;

import RalphExceptions.BackoutException;
import ralph_protobuffs.VariablesProto.Variables;

public class VariableStore 
{
    private HashMap<String,LockedObject> name_to_var_map = 
        new HashMap<String, LockedObject>();

    public VariableStore()
    {}

    /**
     * @param {String} unique_name ---

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


