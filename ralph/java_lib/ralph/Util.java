package ralph;

import java.util.concurrent.TimeUnit;

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


    static public void logger_assert(String to_assert)
    {
        System.out.println("Compiler error: " + to_assert);
        assert false;
        System.exit(-1);
    }
	
    static public void logger_warn(String to_warn)
    {
        System.out.println("Compiler warn: " + to_warn);
    }
}
