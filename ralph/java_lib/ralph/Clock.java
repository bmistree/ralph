package ralph;

import java.util.concurrent.locks.ReentrantLock;


public class Clock {
    private float delta = 0;
    protected ReentrantLock _delta_mutex = new ReentrantLock();
    protected AllEndpoints all_endpoints = null;
	
    public Clock(AllEndpoints _all_endpoints)
    {		
        all_endpoints = _all_endpoints;
    }
    public String get_and_increment_timestamp()
    {
        Util.logger_assert("Error: use only for lamport.");
        return "";
    }
	
    public Clock(AllEndpoints _all_endpoints,float _initial_delta)
    {
        delta = _initial_delta;
        all_endpoints = _all_endpoints;
    }
    public long get_int_timestamp()
    {
        Util.logger_assert(
            "Error: clock has no get_int_timestamp.  That's only for lamport.");
        return 0;
    }
	
    public void check_update_timestamp(long comparison)
    {
        Util.logger_assert(
            "Error: clock has no get_int_timestamp.  That's only for lamport.");
    }
	
    protected void _delta_lock()
    {
    	_delta_mutex.lock();
    }
    protected void _delta_unlock()
    {
        _delta_mutex.unlock();
    }

    /**
     *  @returns {str} --- Fixed width string time since epoch as
     float in seconds.  First ten digits are seconds.  Then the six
     decimal digits represent microseconds.
    */
    public String get_timestamp()
    {
        // FIXME: system dependent time call.  On many systems, this
        // will only return seconds since epoch, not microseconds.  @see
        // http://docs.python.org/3/library/time.html#time.time... no microseconds
        _delta_lock();
        float date_time = System.currentTimeMillis();
        date_time /= 1000.;
        float timestamp = date_time + delta;
        _delta_unlock();  
        
        // using different float string
        return Float.toString(timestamp);        
    }
    

    /**
     *  @param {string} partner_clock_timestamp --- The string
     representation of a 16 digit float.

     If partner_clock_timestamp is from the future, we should
     forward our clock by updating delta with the amount of time
     the partner clock timestamp is from the future.

     Then, we notify all partners to check if they should update
     their clocks.
    */
    public void got_partner_timestamp(String partner_clock_timestamp)
    {
    	float partner_float = Float.parseFloat(partner_clock_timestamp);
    	boolean needs_update = false;
        
    	/*
          note: need to change delta atomically: therefore cannot call
          get_timestamp separately
        */
        _delta_lock();

        
        float date_time = System.currentTimeMillis();
        date_time /= 1000.;
        float timestamp = date_time + delta;
        
        if (partner_float > timestamp)
        {
            delta = delta + partner_float - timestamp;
            needs_update = true;
        }   
        _delta_unlock();

        if (needs_update)
            all_endpoints.send_clock_update();
    }
    
}
