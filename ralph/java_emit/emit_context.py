
class EmitContext(object):
    '''
    Each emit context manages variables that are in and out of scope
    at a particular time, so can track whether need to read variable
    from global stack frame or can just use local value.
    '''
    # FIXME: Unimplemented
