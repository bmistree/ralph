
class Scope(object):
    def __init__(self):
        # from ralph variable name to internal name
        self.var_dict = {}
        
    def add_var(self,ralph_var_name,internal_name):
        self.var_dict[ralph_var_name] = internal_name

    def get_internal_name(self,ralph_var_name):
        '''
        @returns {String or None} --- None if variable isn't in scope.
        '''
        return self.var_dict.get(ralph_var_name,None)
        
class EmitContext(object):
    '''
    Each emit context manages variables that are in and out of scope
    at a particular time, so can track whether need to read variable
    from global stack frame or can just use local value.
    '''
    # FIXME: Unimplemented
    
    STATIC_MONOTONIC_ID = 0
    INTERNAL_VARIABLE_PREFIX = '__internal__'
    
    def __init__(self,struct_types_ctx):
        '''
        @param {parse.type_check_context.StructTypesContext} struct_types_ctx
        '''
        self.struct_types_ctx = struct_types_ctx
        
        # end of list is more recent scope
        self.scope_stack = []

        # True if emitting expression for left hand side of assignment
        # statement.  False if emitting a statement that is not an
        # assign or if emitting the rhs of an assignment statement.
        self.lhs_of_assign = False

        self.method_set = set()
        self.in_endpoint_global_vars = False

    def add_method_name_to_method_set(self,method_name):
        self.method_set.add(method_name)

    def method_name_in_method_set(self,method_name):
        return method_name in self.method_set

    def get_in_endpoint_global_vars(self):
        return self.in_endpoint_global_vars
    def set_in_endpoint_global_vars(self,inside):
        """
        Args:
            inside: {bool} True if now declaring endpoint global
            variables.  False otherwise.
        """
        self.in_endpoint_global_vars = inside
    
    def set_lhs_of_assign(self,lhs_of_assign):
        '''
        @param {boolean} lhs_of_assign --- True if emitting expression for left
        hand side of assignment statement.  False if emitting a
        statement that is not an assign or if emitting the rhs of an
        assignment statement.
        '''
        self.lhs_of_assign = lhs_of_assign

    def get_lhs_of_assign(self):
        return self.lhs_of_assign
        
    def push_scope(self):
        self.scope_stack.append(Scope())
    def pop_scope(self):
        self.scope_stack.pop()

    def add_var_name(self,ralph_var_name):
        last_valid_index = len(self.scope_stack) - 1
        new_var_id = EmitContext.STATIC_MONOTONIC_ID
        EmitContext.STATIC_MONOTONIC_ID += 1
        internal_var_name = (
            EmitContext.INTERNAL_VARIABLE_PREFIX + str(new_var_id) +
            ralph_var_name)

        top_scope = self.scope_stack[last_valid_index]
        top_scope.add_var(ralph_var_name,internal_var_name)

    def set_var_name(self,ralph_var_name):
        """Pushes the name ralph_var_name as an internal variable name
        in stack.
        """
        last_valid_index = len(self.scope_stack) - 1
        top_scope = self.scope_stack[last_valid_index]
        top_scope.add_var(ralph_var_name,ralph_var_name)

        
    def lookup_internal_var_name(self,ralph_var_name):
        '''
        @returns {String or None} --- None if variable is endpoint
        global
        '''
        # search through in reverse order to get most recent variable
        # in case of shadowing.
        for scope in reversed(self.scope_stack):
            internal_name = scope.get_internal_name(ralph_var_name)
            if internal_name is not None:
                return internal_name
        # could not find variable name, must be an endpoint global
        # variable (or undeclared)
        return None


