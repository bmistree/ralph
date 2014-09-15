package RalphVersions;

import java.util.List;
import java.util.ArrayList;

public class EndpointInitializationHistory
{
    public final String endpoint_uuid;
    public final String endpoint_constructor_class_name;
    public final long local_lamport_time;
    // tuple is name of variable and uuid of object (not
    // endpoint).
    public final List<NameUUIDTuple> variable_list =
        new ArrayList<NameUUIDTuple>();

    public EndpointInitializationHistory(
        String endpoint_uuid, String endpoint_constructor_class_name,
        long local_lamport_time)
    {
        this.endpoint_uuid = endpoint_uuid;
        this.endpoint_constructor_class_name =
            endpoint_constructor_class_name;
        this.local_lamport_time = local_lamport_time;
    }

    public void add_variable(String name, String object_uuid)
    {
        variable_list.add(
            new NameUUIDTuple(name,object_uuid));
    }

    public class NameUUIDTuple
    {
        public final String name;
        public final String uuid;
        public NameUUIDTuple(String name, String uuid)
        {
            this.name = name;
            this.uuid = uuid;
        }
    }
}