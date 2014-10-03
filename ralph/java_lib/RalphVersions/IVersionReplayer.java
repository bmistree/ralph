package RalphVersions;

import ralph.EndpointConstructorObj;
import ralph.EnumConstructorObj;

public interface IVersionReplayer
{
    /**
       @returns null if does not exist.
     */
    public EndpointInitializationHistory
        get_endpoint_initialization_history(String endpoint_uuid);

    /**
       @returns null if does not exist.
     */
    public ObjectHistory get_full_object_history(String obj_uuid);

    /**
       @returns null if does not exist.
     */
    public EndpointConstructorObj get_endpoint_constructor_obj(
        String endpoint_constructor_obj_classname);

    /**
       @returns null if does not exist.
     */
    public EnumConstructorObj get_enum_constructor_obj(
        String enum_constructor_obj_classname);
    
    
    /**
       @param lower_range --- null if should query from earliest
       record.

       @param upper_range --- null if should query to latest record.
       
       @returns null if does not exist.  Returns objecthistory object
       with no records if no records exist within range.
     */
    public ObjectHistory get_ranged_object_history(
        String obj_uuid,Long lower_range, Long upper_range);
}