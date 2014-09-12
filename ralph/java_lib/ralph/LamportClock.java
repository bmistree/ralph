package ralph;

public class LamportClock
{
    protected AllEndpoints all_endpoints = null;
    private long counter = 0;

    private final static String CLOCK_FORMAT_STRING = "%08d";
	
    public LamportClock(AllEndpoints _all_endpoints)
    {
        all_endpoints = _all_endpoints;
    }

    public LamportClock()
    {}

    /**
     @returns {str} --- Fixed width string time since epoch as
     float in seconds.  First ten digits are seconds.  Then the six
     decimal digits represent microseconds.       
     */
    synchronized public String get_timestamp()
    {
        // FIXME: decide on how high the clock can get.
        return String.format(CLOCK_FORMAT_STRING,counter);
    }
	
    synchronized public long get_int_timestamp()
    {
        return counter;
    }

    synchronized public long get_and_increment_int_timestamp()
    {
        return ++counter;
    }
    
    synchronized public String get_and_increment_timestamp()
    {
        // FIXME: decide on how high the clock can get.
        return String.format(CLOCK_FORMAT_STRING,++counter);
    }
    synchronized public void check_update_timestamp(long comparison)
    {
        if (comparison > counter)
        {
            counter = comparison;
            // Util.logger_warn(
            //     "Warning: may want to push Lamport clock timestamp update.");
        }
    }
}
