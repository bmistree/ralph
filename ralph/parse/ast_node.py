from ralph.parse.parse_util import InternalParseException,ParseException
import ralph.parse.ast_labels as ast_labels

class _AstNode(object):

    def __init__(self,label,line_number,node_type=None):
        '''
        @param {ast_labels} label --- Eg., whether this node
        corresponds to a for statement, a condition statement, a
        number, etc.

        @param {types or None} node_type --- Only expressions have
        types.  And they only get types after type checking has
        completed.
        '''
        self.label = label
        self.line_number = line_number
        self.type = node_type
        self.children = []

        
    def _add_child(self,child_node):
        '''
        Should be protected method, but do not have decorator for it
        '''
        self.children.append(child_node)
    def _prepend_child(self,child_node):
        self.children.insert(0,child_node)
        
        
class RootStatementNode(_AstNode):
    def __init__(self,endpoint_node_list):
        super(RootStatementNode,self).__init__(ast_labels.ROOT_STATEMENT,0,None)
        
        #### DEBUG
        if endpoint_node_list.label != ast_labels.ENDPOINT_LIST_STATEMENT:
            raise InternalParseException(
                'RootStatementNode requires endpoint list statement node')
        #### END DEBUG
        
        for endpoint_node in endpoint_node_list:
            self._add_child(endpoint_node)

class EndpointDefinitionNode(_AstNode):
    def __init__(self,line_number):
        super(EndpointDefinitionNode,self).__init__(
            ast_labels.ENDPOINT_DEFINITION,line_number)
        
            
#### Intermediate nodes that get removed from actual AST ####
class EndpointListNode(_AstNode):
    def __init__(self,endpoint_definition_node):
        super(EndpointListNode,self).__init__(
            ast_labels.ENDPOINT_LIST_STATEMENT,0)

        self._add_child(endpoint_definition_node)

    def prepend_endpoint_definition(self,endpoint_definition_node):
        self._prepend_child(endpoint_definition_node)
        
    def __iter__(self):
        return iter(self.children)
