package RalphCommonBacked;

import ralph.SpeculativeAtomicObject;
import java.util.Set;

public interface IChangeProcessor
{
    public void push_changes(
        Set<SpeculativeAtomicObject> objects_pushing_changes);
}