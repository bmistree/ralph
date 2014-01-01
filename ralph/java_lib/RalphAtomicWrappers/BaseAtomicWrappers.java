package RalphAtomicWrappers;
import ralph.RalphObject;
import ralph.Variables;

public class BaseAtomicWrappers
{
    public final static SingleThreadedNumberWrapper SINGLE_THREADED_NUMBER_WRAPPER =
        new SingleThreadedNumberWrapper();
    public final static SingleThreadedTrueFalseWrapper SINGLE_THREADED_TRUE_FALSE_WRAPPER =
        new SingleThreadedTrueFalseWrapper();
    public final static SingleThreadedTextWrapper SINGLE_THREADED_TEXT_WRAPPER =
        new SingleThreadedTextWrapper();    

    public final static NumberWrapper NUMBER_WRAPPER = new NumberWrapper();
    public final static TrueFalseWrapper TRUE_FALSE_WRAPPER = new TrueFalseWrapper();
    public final static TextWrapper TEXT_WRAPPER = new TextWrapper();    
    
    
    /**** Single threaded wrappers for base variables */
    public static class SingleThreadedNumberWrapper
        implements EnsureAtomicWrapper<Double,Double>
    {
        public RalphObject<Double,Double> ensure_atomic_object(
            Double object_to_ensure)
        {
            return new Variables.SingleThreadedLockedNumberVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    public static class SingleThreadedTrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean,Boolean>
    {
        public RalphObject<Boolean,Boolean>ensure_atomic_object(
            Boolean object_to_ensure)
        {
            return new Variables.SingleThreadedLockedTrueFalseVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
    
    public static class SingleThreadedTextWrapper
        implements EnsureAtomicWrapper<String,String>
    {
        public RalphObject<String,String>ensure_atomic_object(
            String object_to_ensure)
        {
            return new Variables.SingleThreadedLockedTextVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    /**** Multithreaded wrappers for base variables */
    public static class NumberWrapper
        implements EnsureAtomicWrapper<Double,Double>
    {
        public RalphObject<Double,Double> ensure_atomic_object(
            Double object_to_ensure)
        {
            return new Variables.LockedNumberVariable(
                "host_uuid",false,object_to_ensure);
        }
    }

    public static class TrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean,Boolean>
    {
        public RalphObject<Boolean,Boolean>ensure_atomic_object(
            Boolean object_to_ensure)
        {
            return new Variables.LockedTrueFalseVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
    
    public static class TextWrapper
        implements EnsureAtomicWrapper<String,String>
    {
        public RalphObject<String,String>ensure_atomic_object(
            String object_to_ensure)
        {
            return new Variables.LockedTextVariable(
                "host_uuid",false,object_to_ensure);
        }
    }
}

