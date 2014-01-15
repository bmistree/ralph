package ralph;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.UUID;


/**
 * 
    Priority structure:
      abc

      a --- Single character: 0 if super, 1 if boosted, 2 if standard event
      b --- 16 characters.  Lower numbers have precedence.
      c --- 8 characters.  Random data used to break ties between events
            that were begun at the exact same time.
 *
 */
public class EventPriority 
{
    private final static char SUPER_PREFIX = '0';
    private final static char BOOSTED_PREFIX = '1';
    private final static char STANDARD_PREFIX = '2';

    
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
        return BOOSTED_PREFIX + timestamp_last_boosted_completed + uuid.substring(0,8);
    }

    /**
       Returns a standard priority.
     */
    public static String generate_standard_priority(String current_timestamp)
    {
        return generate_priority_from_timestamp(
            current_timestamp,STANDARD_PREFIX);
    }

    /**
       Used for both super priorities and for standard.

       @param preceding_character --- {char} Should be either
       STANDARD_PREFIX or SUPER_PREFIX.
     */
    private static String generate_priority_from_timestamp(
        String current_timestamp, char preceding_character)
    {
        // FIXME: This is an ugly, inefficient way to generate random strings
        String uuid = UUID.randomUUID().toString();				
        return preceding_character + current_timestamp + uuid.substring(0,8);
    }
    
    public static String generate_super_priority(String current_timestamp)
    {
        return generate_priority_from_timestamp(
            current_timestamp,SUPER_PREFIX);
    }

    public static boolean is_super_priority(String priority)
    {
        return  (priority.charAt(0) == SUPER_PREFIX);
    }
    
}
