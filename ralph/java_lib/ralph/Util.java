package ralph;

public class Util
{
	
    public static final Endpoint PARTNER_ENDPOINT_SENTINEL = null;

    public static final int DEFAULT_TCP_PORT_NEW_CONNECTIONS = 55559;
    public static final String DEFAULT_IP_ADDRESS_NEW_CONNECTIONS =
        "0.0.0.0";

    
    /**
     * Queues must be declared with capacities.  All queues have this
     * default capacity
     */
    static public int QUEUE_CAPACITIES = 100;
	
    /**
     * For queues that are only really supposed to have one result
     * returned in them.
     */
    static public int SMALL_QUEUE_CAPACITIES = 3;

    /**
     * How many signal functions can get added to an event.
     */
    public static int MEDIUM_QUEUE_CAPACITIES = 10;

    public static int DEFAULT_NUM_THREADS = 70;
	
    /**
     * Takes in the name of the function that another endpoint has
     requested to be called.  Adds a prefix to distinguish the
     function as being called from an endpoint function call rather
     than from external code.
    */
    static public String endpoint_call_func_name(String func_name)
    {
        return "_endpoint_func_call_prefix__waldo__" + func_name;
    }
	
    static public String internal_call_func_name(String func_name)
    {
        return "_internal_func_call_prefix__waldo__" + func_name;
    }
	
	
    /**
     *
     @see endpoint_call_func_name, except as a result of partner
     sending a message in a message sequence.
     * @param func_name
     * @return
     */

    static public String internal_oncreate_func_call_name(String func_name)
    {
        return "_onCreate";
    }

	
    static public String generate_uuid()
    {
        return java.util.UUID.randomUUID().toString();
    }
	
    static public void logger_assert(String to_assert)
    {
        System.out.println("Compiler error: " + to_assert);
        assert false;
    }
	
    static public void logger_warn(String to_warn)
    {
        System.out.println("Compiler warn: " + to_warn);
    }
}
