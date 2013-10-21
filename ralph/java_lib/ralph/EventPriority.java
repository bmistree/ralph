package ralph;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;


/**
 * '''
Priority structure:
  abcd

  a --- Single character: 0 if boosted.  1 if standard event
  b --- 16 characters.  Lower numbers have precedence.
  c --- 8 characters.  Random data used to break ties between events
        that were begun at the exact same time.
 *
 */
public class EventPriority 
{

    /**
       Returns true if prioritya is greater than or equal to priorityb.  That is,
       returns True if prioritya should be able to preempt priorityb.
       * @return
       */
    public static boolean gte_priority(String prioritya, String priorityb)
    {
        //returns < 0 then the String calling the method is
        //lexicographically first (comes first in a dictionary)
        
        //returns == 0 then the two strings are lexicographically
        //equivalent
        
        //returns > 0 then the parameter passed to the compareTo
        //method is lexicographically first.
        int comparison = prioritya.compareTo(priorityb);
        return comparison <= 0;
    }
	
    public static String generate_boosted_priority(String timestamp_last_boosted_completed)
    {
        // FIXME: This is an ugly, inefficient way to generate random strings
        String uuid = UUID.randomUUID().toString();		
        return '0' + timestamp_last_boosted_completed + uuid.substring(0,8);
    }
	
    public static String generate_timed_priority(String current_timestamp)
    {
        // FIXME: This is an ugly, inefficient way to generate random strings
        String uuid = UUID.randomUUID().toString();				
        return '1' + current_timestamp + uuid.substring(0,8);
    }

    public static boolean is_boosted_priority(String priority)
    {
        return (priority.charAt(0) == '0');
    }
}
