package RalphServiceActions;

import ralph.AtomicObject;

public class AtomicObjectTryNextAction extends ServiceAction
{
    private AtomicObject object_to_try_next = null;
	
    public AtomicObjectTryNextAction(AtomicObject object_to_try_next)
    {
        this.object_to_try_next = object_to_try_next;
    }
	
    @Override
    public void run() {
        object_to_try_next.try_next();
    }
}
