class Scope(object):
    def __init__(self):
        # from ralph variable name to ast_nodes 
        self.var_dict = {}
        
    def add_var(self,ralph_var_name,ast_node):
        self.var_dict[ralph_var_name] = ast_node

    def get_internal_name(self,ralph_var_name):
        '''
        @returns {AstNode} --- None if variable isn't in scope.
        '''
        return self.var_dict.get(ralph_var_name,None)


class TypeCheckContext(object):
    """Tracks information necessary for type checking as type
    checking.  (Eg., variables previously declared in this lexical
    scope, etc.

    """

    def __init__(self,endpoint_name):
        # end of list is more recent scope
        self.scope_stack = []
        self.endpoint_name = endpoint_name

    def push_scope(self):
        self.scope_stack.append(Scope())

    def pop_scope(self):
        self.scope_stack.pop()

    def add_var_name(self,ralph_var_name,ast_node):
        last_valid_index = len(self.scope_stack) - 1
        top_scope = self.scope_stack[last_valid_index]
        top_scope.add_var(ralph_var_name,ast_node)
        
    def lookup_internal_ast_node(self,ralph_var_name):
        '''
        @returns {AstNode or None} --- None if variable is endpoint
        global
        '''
        # search through in reverse order to get most recent variable
        # in case of shadowing.
        for scope in reversed(self.scope_stack):
            ast_node = scope.get_internal_name(ralph_var_name)
            if ast_node is not None:
                return ast_node

        return None
