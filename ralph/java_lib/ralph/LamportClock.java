package ralph;

public class LamportClock
{
    protected AllEndpoints all_endpoints = null;
    long counter = 0;
	
    public LamportClock(AllEndpoints _all_endpoints)
    {
        all_endpoints = _all_endpoints;
    }

    /**
     @returns {str} --- Fixed width string time since epoch as
     float in seconds.  First ten digits are seconds.  Then the six
     decimal digits represent microseconds.       
     */
    synchronized public String get_timestamp()
    {
        return String.valueOf(counter);
    }
	
    synchronized public long get_int_timestamp()
    {
        return counter;
    }
	
    synchronized public String get_and_increment_timestamp()
    {		
        return String.valueOf(counter++);
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
