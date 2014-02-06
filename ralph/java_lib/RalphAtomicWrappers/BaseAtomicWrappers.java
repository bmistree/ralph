package RalphAtomicWrappers;
import ralph.RalphObject;
import ralph.Variables;
import ralph.RalphGlobals;

public class BaseAtomicWrappers
{
    public final static NonAtomicNumberWrapper NON_ATOMIC_NUMBER_WRAPPER =
        new NonAtomicNumberWrapper();
    public final static NonAtomicTrueFalseWrapper NON_ATOMIC_TRUE_FALSE_WRAPPER =
        new NonAtomicTrueFalseWrapper();
    public final static NonAtomicTextWrapper NON_ATOMIC_TEXT_WRAPPER =
        new NonAtomicTextWrapper();    

    public final static AtomicNumberWrapper ATOMIC_NUMBER_WRAPPER = new AtomicNumberWrapper();
    public final static AtomicTrueFalseWrapper ATOMIC_TRUE_FALSE_WRAPPER = new AtomicTrueFalseWrapper();
    public final static AtomicTextWrapper ATOMIC_TEXT_WRAPPER = new AtomicTextWrapper();    
    
    
    /**** Single threaded wrappers for base variables */
    private static class NonAtomicNumberWrapper
        implements EnsureAtomicWrapper<Double,Double>
    {
        public RalphObject<Double,Double> ensure_atomic_object(
            Double object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicNumberVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    private static class NonAtomicTrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean,Boolean>
    {
        public RalphObject<Boolean,Boolean>ensure_atomic_object(
            Boolean object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicTrueFalseVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
    
    private static class NonAtomicTextWrapper
        implements EnsureAtomicWrapper<String,String>
    {
        public RalphObject<String,String>ensure_atomic_object(
            String object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.NonAtomicTextVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    /**** Multithreaded wrappers for base variables */
    private static class AtomicNumberWrapper
        implements EnsureAtomicWrapper<Double,Double>
    {
        public RalphObject<Double,Double> ensure_atomic_object(
            Double object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicNumberVariable(
                false,object_to_ensure,ralph_globals);
        }
    }

    private static class AtomicTrueFalseWrapper
        implements EnsureAtomicWrapper<Boolean,Boolean>
    {
        public RalphObject<Boolean,Boolean>ensure_atomic_object(
            Boolean object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicTrueFalseVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
    
    private static class AtomicTextWrapper
        implements EnsureAtomicWrapper<String,String>
    {
        public RalphObject<String,String>ensure_atomic_object(
            String object_to_ensure,RalphGlobals ralph_globals)
        {
            return new Variables.AtomicTextVariable(
                false,object_to_ensure,ralph_globals);
        }
    }
}

