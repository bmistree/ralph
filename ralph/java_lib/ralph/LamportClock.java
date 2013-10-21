package ralph;

public class LamportClock extends Clock {

    long counter = 0;
	
    public LamportClock(AllEndpoints _all_endpoints) {
        super(_all_endpoints);
        // TODO Auto-generated constructor stub
    }

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
            counter = comparison;
    }
}
