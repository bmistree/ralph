from ralph.parse.parse_util import InternalParseException,ParseException
from ralph.parse.parse_util import TypeCheckException
import ralph.parse.ast_labels as ast_labels
from ralph.parse.type import BasicType, MethodType, MapType,StructType
from ralph.parse.type import ListType, Type, EndpointType, WildcardType
from ralph.parse.type import ServiceFactoryType,NullType
from ralph.parse.type_check_context import TypeCheckContext,StructTypesContext
from ralph.parse.type_check_context import AliasContext
from ralph.parse.type_check_context import FixupableObject
# Type check is broken into two passes:
#
#   Pass one:
#     To handle user-defined struct types, first run through entire
#     ast with a dictionary mapping struct names to their field types.
#     For each struct variable type, find its associated field types
#     from the dict and label the struct variable type with them.
#     After first pass, should be guaranteed that all ast type nodes
#     (ie, BasicTypeNode, MapTypeNode, StructTypeNode) have their
#     .type fields populated correctly.
#
#   Pass two:
#     Propagate type information from ast type nodes (ie,
#     BasicTypeNode, MapTypeNode, StructTypeNode) to all ast nodes.

class _AstNode(object):

    def __init__(self,filename,label,line_number,node_type=None):
        '''
        @param {ast_labels} label --- Eg., whether this node
        corresponds to a for statement, a condition statement, a
        number, etc.

        @param {types or None} node_type --- Only expressions have
        types.  And they only get types after type checking has
        completed.
        '''
        self.filename = filename
        self.label = label
        self.line_number = line_number
        self.type = node_type
        self.children = []

    def _append_child(self,child_node):
        '''
        Should be protected method, but do not have decorator for it
        '''
        self.children.append(child_node)
    def _prepend_child(self,child_node):
        self.children.insert(0,child_node)

    def type_check_pass_one(self,struct_types_ctx):
        """Type check this statement.
        """
        raise InternalParseException(
            self.filename,self.line_number,
            'Pure virtual type check pass one in AstNode.')
        
    def type_check_pass_two(self,type_check_ctx):
        """Type check this statement.
        """
        raise InternalParseException(
            self.filename,self.line_number,
            'Pure virtual type check pass two in AstNode.')

    
class RootStatementNode(_AstNode):
    def __init__(self,filename,alias_list_node,struct_node_list,endpoint_node_list):
        super(RootStatementNode,self).__init__(
            filename,ast_labels.ROOT_STATEMENT,0,None)
        
        #### DEBUG
        if endpoint_node_list.label != ast_labels.ENDPOINT_LIST_STATEMENT:
            raise InternalParseException(
                self.filename,self.line_number,
                'RootStatementNode requires endpoint list statement node')
        #### END DEBUG

        self.alias_node_list = alias_list_node.to_list()
        self.interface_node_list = list(
            endpoint_node_list.endpoint_interfaces_node_list())
        self.endpoint_node_list = list(
            endpoint_node_list.endpoint_definitions_node_list())
        
        self.struct_node_list = struct_node_list.to_list()

        
    def type_check(self,struct_types_ctx,filename):
        '''
        Args:
            struct_types_ctx: {StructTypesContext or None} --- If not
            None, contains user-defined structs from imported
            libraries.
        
            filename: {String} The name of the file that we are type
            checking over
        
        Returns:
            StructTypesContext object that maps from struct names to
            field values.
        '''
        alias_ctx = AliasContext()
        for alias_node in self.alias_node_list:
            alias_node.type_check_alias_pass(alias_ctx,struct_types_ctx)

        # notate all user-defined struct types.
        if struct_types_ctx is None:
            struct_types_ctx = StructTypesContext(filename,alias_ctx)
        else:
            struct_types_ctx.new_type_check(filename,alias_ctx)
            
        for struct_node in self.struct_node_list:
            to_fixup = struct_node.add_struct_type(struct_types_ctx)

        for struct_node in self.struct_node_list:
            struct_node.register_struct_endpoint_fields(struct_types_ctx)
            
        struct_types_ctx.perform_fixups()

            
        # in case any of the structs have maps/lists/structs in them,
        # will need to resolve those.
        for struct_node in self.struct_node_list:
            type_check_ctx = TypeCheckContext(
                struct_node.struct_name,struct_types_ctx)
            type_check_ctx.push_scope()
            struct_node.type_check_pass_one(struct_types_ctx)
            struct_node.type_check_pass_two(type_check_ctx)
            
        for interface_node in self.interface_node_list:
            interface_node.type_check_pass_one(struct_types_ctx)
            type_check_ctx = TypeCheckContext(interface_node.name,struct_types_ctx)
            type_check_ctx.push_scope()
            interface_node.type_check_pass_two(type_check_ctx)
            
        for endpt_node in self.endpoint_node_list:
            endpt_node.type_check_pass_one(struct_types_ctx)
            type_check_ctx = TypeCheckContext(endpt_node.name,struct_types_ctx)
            type_check_ctx.push_scope()
            endpt_node.type_check_pass_two(type_check_ctx)

            
        return struct_types_ctx

    
class AliasStatementNode(_AstNode):
    def __init__(self,filename,for_struct,identifier_node,
                 to_alias_to_string_node,line_number):
        super(AliasStatementNode,self).__init__(
            filename,ast_labels.ALIAS,line_number)
        self.for_struct = for_struct
        self.to_alias_string = identifier_node.value
        self.to_alias_to_string = to_alias_to_string_node.value

    def type_check_alias_pass(self,alias_ctx,struct_types_ctx):
        '''Register all aliased structs and endpoints.

        Args:

            alias_ctx: {AliasContext} context to register aliased
            struct/endpoint.

            struct_types_ctx: {StructTypesContext or None} If not
            None, then check if struct type exists in context.  If it
            does, then update the type object's alias name.
            struct_types_ctx may be None if not including any
            dependent files and will not be None if we have.
        '''
        if self.for_struct:
            alias_ctx.add_struct_alias(
                self.to_alias_string,self.to_alias_to_string)

            if struct_types_ctx is not None:
                type_obj = struct_types_ctx.get_type_obj_from_struct_name(
                    self.to_alias_string)
                if type_obj is not None:
                    type_obj.set_alias_name(self.to_alias_to_string)
            
        else:
            alias_ctx.add_endpoint_alias(
                self.to_alias_string,self.to_alias_to_string)
    
class StructDefinitionNode(_AstNode):
    def __init__(self,filename,struct_name_identifier_node,struct_body_node,
                 line_number):
        
        super(StructDefinitionNode,self).__init__(
            filename,ast_labels.STRUCT_DEFINITION,line_number)

        self.struct_name = struct_name_identifier_node.value
        self.struct_body_node = struct_body_node
        # name_to_types_dict: {dict} Indices are strings; each is the
        # name of a field in the struct.  Values are type objects (not
        # type astnodes) associated with each field.
        name_to_types_dict,self.to_fixup = struct_body_node.get_field_dict()
        initializer_dict = struct_body_node.get_initializer_dict()
        
        self.type = StructType(
            self.struct_name,name_to_types_dict,False,None,initializer_dict)

        
    def register_struct_endpoint_fields(self,struct_types_ctx):
        '''
        Run through all fields of struct and ask any endpoints in
        those fields to perform their alias operations.
        '''
        self.struct_body_node.register_struct_endpoint_fields(struct_types_ctx)

        
    def add_struct_type(self,struct_types_ctx):
        alias_name = (
            struct_types_ctx.alias_ctx.get_struct_alias(self.struct_name))
        self.type.set_alias_name(alias_name)
        
        struct_types_ctx.add_struct_type_obj_for_name(
            self.struct_name,self.type,self.line_number)

        # for structs that have fields that point to other structs.
        for field_name_to_fixup in self.to_fixup:
            to_fixup_with = self.to_fixup[field_name_to_fixup]
            struct_types_ctx.to_fixup(
                self.struct_name,field_name_to_fixup,to_fixup_with)


    def type_check_pass_one(self,struct_types_ctx):
        self.struct_body_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.struct_body_node.type_check_pass_two(type_check_ctx)

class InterfaceDefinitionNode(_AstNode):
    def __init__(self,filename,name_identifier_node,interface_body_node,
                 line_number):
        super(InterfaceDefinitionNode,self).__init__(
            filename,ast_labels.INTERFACE_DEFINITION_STATEMENT,line_number)
        
        self.name = name_identifier_node.get_value()
        self.body_node = interface_body_node
        
    def type_check_pass_one(self,struct_types_ctx):
        self.body_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.body_node.type_check_pass_two(type_check_ctx)


class InterfaceBodyNode(_AstNode):
    def __init__(self,filename):
        super(InterfaceBodyNode,self).__init__(
            filename,ast_labels.INTERFACE_BODY,0)

        # list of interface method declaration nodes
        self.method_declaration_nodes = []
        
    def append_interface_method_declaration_node(self,method_declaration_node):
        self.method_declaration_nodes.insert(0,method_declaration_node)

    def type_check_pass_one(self,struct_types_ctx):
        for method_declaration_node in self.method_declaration_nodes:
            method_declaration_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        # Populate every method signature in ctx
        for method_declaration_node in self.method_declaration_nodes:
            method_name = method_declaration_node.method_name
            method_type = method_declaration_node.method_signature_node.type
            type_check_ctx.add_var_name(method_name,method_declaration_node)
            

class InterfaceMethodDeclarationNode(_AstNode):
    def __init__(self,filename,method_signature_node):
        super(InterfaceMethodDeclarationNode,self).__init__(
            filename,ast_labels.INTERFACE_METHOD_DECLARATION,
            method_signature_node.line_number)

        self.method_name = method_signature_node.get_method_name()
        self.method_signature_node = method_signature_node

    def returns_value(self):
        '''
        Returns:
           {boolean} True if method actually returns value; false otherwise.
        '''
        return self.method_signature_node.type.returns_type is not None

    def type_check_pass_one(self,struct_types_ctx):
        self.method_signature_node.type_check_pass_one(struct_types_ctx)
        self.type = self.method_signature_node.type

    def type_check_pass_two(self,type_check_ctx):
        """
        Note: does not insert type name and signature into
        type_check_ctx.  Should have already been inserted in
        EndpointBodyNode.
        """        
        # each method declaration node has separate var scope
        type_check_ctx.push_scope()
        # pushes method arguments into scope
        self.method_signature_node.type_check_pass_two(type_check_ctx)
        type_check_ctx.pop_scope()

        
class EndpointDefinitionNode(_AstNode):
    def __init__(self,filename,name_identifier_node,optional_implements_list_node,
                 endpoint_body_node,line_number):
        super(EndpointDefinitionNode,self).__init__(
            filename,ast_labels.ENDPOINT_DEFINITION_STATEMENT,line_number)
        
        self.name = name_identifier_node.get_value()
        self.body_node = endpoint_body_node
        self.implements_variable_type_node_list = (
            optional_implements_list_node.to_list())
        
    def type_check_pass_one(self,struct_types_ctx):
        self.body_node.type_check_pass_one(struct_types_ctx)
        for variable_type_node in self.implements_variable_type_node_list:
            variable_type_node.type_check_pass_one(struct_types_ctx)
                
    def type_check_pass_two(self,type_check_ctx):
        self.body_node.type_check_pass_two(type_check_ctx)
        for variable_type_node in self.implements_variable_type_node_list:
            variable_type_node.type_check_pass_two(type_check_ctx)

            
class EndpointBodyNode(_AstNode):
    def __init__(self,filename):
        super(EndpointBodyNode,self).__init__(
            filename,ast_labels.ENDPOINT_BODY,0)
        self.variable_declaration_nodes = []
        self.method_declaration_nodes = []
        
    def prepend_variable_declaration_node(
        self,variable_declaration_node):
        self.variable_declaration_nodes.insert(0,variable_declaration_node)
        
    def prepend_method_declaration_node(
        self,method_declaration_node):
        self.method_declaration_nodes.insert(0,method_declaration_node)

    def type_check_pass_one(self,struct_types_ctx):
        for variable_declaration_node in self.variable_declaration_nodes:
            variable_declaration_node.type_check_pass_one(struct_types_ctx)

        for method_declaration_node in self.method_declaration_nodes:
            method_declaration_node.type_check_pass_one(struct_types_ctx)

        
    def type_check_pass_two(self,type_check_ctx):
        # First populate global scope with all endpoint nodes.
        for variable_declaration_node in self.variable_declaration_nodes:
            variable_declaration_node.type_check_pass_two(type_check_ctx)
            
        # Populate every method signature in ctx
        for method_declaration_node in self.method_declaration_nodes:
            method_name = method_declaration_node.method_name
            method_type = method_declaration_node.method_signature_node.type
            type_check_ctx.add_var_name(method_name,method_declaration_node)
            
        # Type check the body of each method
        for method_declaration_node in self.method_declaration_nodes:
            method_declaration_node.type_check_pass_two(type_check_ctx)

class PartnerMethodCallNode(_AstNode):
    def __init__(self,filename,method_name_node,method_call_args_node,
                 line_number):
        """
        Args:
            method_name_node: {IdentifierNode}
        """
        super(PartnerMethodCallNode,self).__init__(
            filename,ast_labels.PARTNER_METHOD_CALL,line_number)

        self.partner_method_name = method_name_node.value
        self.args_list = method_call_args_node.get_args_list()
        
    def type_check_pass_one(self,struct_types_ctx):
        for arg_node in self.args_list:
            arg_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        for arg_node in self.args_list:
            arg_node.type_check_pass_two(type_check_ctx)
            # FIXME: for now, only permitting putting in identifiers
            # as arguments to partner method calls.  This is because
            # later, when emitting, must generate RPCArgObjects in
            # Java, which require wrapping RalphObjects, not Booleans,
            # Doubles, Strings, etc.
            if arg_node.label != ast_labels.IDENTIFIER_EXPRESSION:
                raise TypeCheckException(
                    self.filename,self.line_number,
                    'arg to partner method call must be an identifier ' +
                    'for now.  For now, only permitting putting in ' +
                    'identifiers as arguments to partner method calls.  ' +
                    'This is because later, when emitting, must ' +
                    'generate RPCArgObjects in Java, which require ' +
                    'wrapping RalphObjects, not Booleans, Doubles, ' +
                    'Strings, etc.')

            
class IdentifierNode(_AstNode):
    def __init__(self,filename,value,line_number):
        super(IdentifierNode,self).__init__(
            filename,ast_labels.IDENTIFIER_EXPRESSION,line_number)

        self.value = value

    def type_check_pass_one(self,struct_types_ctx):
        pass

    def type_check_pass_two(self,type_check_ctx):
        decl_ast_node = type_check_ctx.lookup_internal_ast_node(self.value)
        if decl_ast_node is None:
            raise TypeCheckException(
                self.filename,self.line_number,
                ' %s is not declared.' % self.value )
        self.type = decl_ast_node.type

    def get_value(self):
        return self.value

class DeclarationStatementNode(_AstNode):
    def __init__(self,filename,var_name_identifier_node,type_node,
                 initializer_node = None):
        
        super(DeclarationStatementNode,self).__init__(
            filename,ast_labels.DECLARATION_STATEMENT,
            type_node.line_number)

        self.type_node = type_node
        self.var_name = var_name_identifier_node.get_value()
        self.initializer_node = initializer_node

    def type_check_pass_one(self,struct_types_ctx):
        self.type_node.type_check_pass_one(struct_types_ctx)
        if self.initializer_node is not None:
            self.initializer_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        self.type_node.type_check_pass_two(type_check_ctx)
        self.type = self.type_node.type

        # when we declare a new variable, add it to scope.
        type_check_ctx.add_var_name(self.var_name,self)

        if self.initializer_node is not None:
            self.initializer_node.type_check_pass_two(type_check_ctx)

class PrintCallNode(_AstNode):
    def __init__(self,filename,method_args_node,line_number):
        super(PrintCallNode,self).__init__(
            filename,ast_labels.PRINT_CALL,line_number)

        print_call_args_list = method_args_node.get_args_list()
        if len(print_call_args_list) != 1:
            raise TypeCheckException(
                self.filename,self.line_number,
                ('Print requires 1 argument, not %s.' %
                 len(print_call_args_list)))
        
        self.print_arg_node = print_call_args_list[0]
        
        
    def type_check_pass_one(self,struct_types_ctx):
        self.print_arg_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        self.print_arg_node.type_check_pass_two(type_check_ctx)


class SelfNode(_AstNode):
    def __init__(self,filename,line_number):
        super(SelfNode,self).__init__(
            filename,ast_labels.SELF,line_number,WildcardType())
        
    def type_check_pass_one(self,struct_types_ctx):
        pass

    def type_check_pass_two(self,type_check_ctx):
        pass

    
class SpeculateCallNode(_AstNode):
    def __init__(self,filename,method_args_node,line_number):
        super(SpeculateCallNode,self).__init__(
            filename,ast_labels.SPECULATE_CALL,line_number)

        self.speculate_call_args_list = []
        if method_args_node is not None:
            self.speculate_call_args_list = method_args_node.get_args_list()
            if len(self.speculate_call_args_list) == 0:
                raise TypeCheckException(
                    self.filename,self.line_number,
                    'Speculate requires at least one argument.')
        
    def type_check_pass_one(self,struct_types_ctx):
        for arg in self.speculate_call_args_list:
            arg.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        for arg in self.speculate_call_args_list:
            arg.type_check_pass_two(type_check_ctx)

       
class SpeculateAllCallNode(_AstNode):
    def __init__(self,filename,line_number):
        super(SpeculateAllCallNode,self).__init__(
            filename,ast_labels.SPECULATE_ALL_CALL,line_number)

    def type_check_pass_one(self,struct_types_ctx):
        pass

    def type_check_pass_two(self,type_check_ctx):
        pass
        
        
class VerbatimCallNode(_AstNode):
    def __init__(self,filename,method_args_node,line_number):
        super(VerbatimCallNode,self).__init__(
            filename,ast_labels.VERBATIM_CALL,line_number)

        verbatim_call_args_list = method_args_node.get_args_list()
        if len(verbatim_call_args_list) != 1:
            raise TypeCheckException(
                self.filename,self.line_number,
                ('Verbatim requires 1 argument, not %s.' %
                 len(verbatim_call_args_list)))
        
        self.verbatim_arg_node = verbatim_call_args_list[0]
        
        
    def type_check_pass_one(self,struct_types_ctx):
        self.verbatim_arg_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        self.verbatim_arg_node.type_check_pass_two(type_check_ctx)
        

class MethodDeclarationNode(_AstNode):
    def __init__(self,filename,method_signature_node,scope_body_node):

        super(MethodDeclarationNode,self).__init__(
            filename,ast_labels.METHOD_DECLARATION,
            method_signature_node.line_number)

        self.method_name = method_signature_node.get_method_name()
        self.method_signature_node = method_signature_node
        self.method_body_statement_list = scope_body_node.get_statement_list()

    def returns_value(self):
        '''
        Returns:
           {boolean} True if method actually returns value; false otherwise.
        '''
        return self.method_signature_node.type.returns_type is not None

    def type_check_pass_one(self,struct_types_ctx):
        self.method_signature_node.type_check_pass_one(struct_types_ctx)
        self.type = self.method_signature_node.type
        for node in self.method_body_statement_list:
            node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        """
        Note: does not insert type name and signature into
        type_check_ctx.  Should have already been inserted in
        EndpointBodyNode.
        """
        # each method declaration node has separate var scope
        type_check_ctx.push_scope()
        # pushes method arguments into scope
        self.method_signature_node.type_check_pass_two(type_check_ctx)
        for node in self.method_body_statement_list:
            node.type_check_pass_two(type_check_ctx)
        type_check_ctx.pop_scope()

        
class MethodSignatureNode(_AstNode):
    def __init__(self,filename,method_name_identifier_node,
                 method_declaration_args_node, returns_type_node):
        '''
        @params{}
        @params{}
        
        @params{VariableTypeNode or None} --- None if doesn't return
        anything
        '''
        super(MethodSignatureNode,self).__init__(
            filename,ast_labels.METHOD_SIGNATURE,
            method_name_identifier_node.line_number)

        self.method_name = method_name_identifier_node.get_value()
        self.method_declaration_args = method_declaration_args_node.to_list()

        self.returns_type_node = returns_type_node

    def type_check_pass_one(self,struct_types_ctx):
        if self.returns_type_node is not None:
            self.returns_type_node.type_check_pass_one(struct_types_ctx)
        for arg_node in self.method_declaration_args:
            arg_node.type_check_pass_one(struct_types_ctx)
            
        # construct type from return type and args type
        return_type = None
        if self.returns_type_node is not None:
            return_type = self.returns_type_node.type
        arg_type_list = map(
            lambda arg_node: arg_node.type,
            self.method_declaration_args)
        
        self.type = MethodType(return_type,arg_type_list)
        
    def get_method_name(self):
        return self.method_name

    def type_check_pass_two(self,type_check_ctx):
        # push all arguments into type check context
        for method_arg_node in self.method_declaration_args:
            method_arg_node.type_check_pass_two(type_check_ctx)
            type_check_ctx.add_var_name(
                method_arg_node.arg_name,method_arg_node)

class ForNode(_AstNode):
    def __init__(self,filename,variable_type_node,variable_node,in_what_node,
                 statement_node,line_number):

        # for ( variable_type_node variable_node in in_what_node)
        #    statement_node
        # note: may be None if the variable already existed
        super(ForNode,self).__init__(filename,ast_labels.FOR,line_number)

        self.variable_type_node = variable_type_node
        self.variable_node = variable_node

        # for time-being, only allowing single identifiers to be
        if self.variable_node.label != ast_labels.IDENTIFIER_EXPRESSION:
            raise TypeCheckException(
                self.filename,self.line_number,
                'For loop requires a single identifier in predicate')
        
        self.in_what_node = in_what_node
        self.statement_node = statement_node
        
    def type_check_pass_one(self,struct_types_ctx):
        self.variable_type_node.type_check_pass_one(struct_types_ctx)
        self.variable_node.type_check_pass_one(struct_types_ctx)
        self.in_what_node.type_check_pass_one(struct_types_ctx)
        self.statement_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        type_check_ctx.push_scope()
        type_check_ctx.add_var_name(
            #variable node is an identifier
            self.variable_node.value,
            self.variable_type_node)
        self.variable_type_node.type_check_pass_two(type_check_ctx)
        
        self.variable_node.type_check_pass_two(type_check_ctx)
        self.in_what_node.type_check_pass_two(type_check_ctx)
        self.statement_node.type_check_pass_two(type_check_ctx)

        type_check_ctx.pop_scope()
        
            
class MethodDeclarationArgNode(_AstNode):
    def __init__(self,filename, variable_type_node, name_identifier_node):
        super(MethodDeclarationArgNode,self).__init__(
            filename,ast_labels.METHOD_DECLARATION_ARG,
            variable_type_node.line_number)

        self.variable_type_node = variable_type_node
        self.arg_name = name_identifier_node.get_value()


    def type_check_pass_one(self,struct_types_ctx):
        self.variable_type_node.type_check_pass_one(struct_types_ctx)
        self.type = self.variable_type_node.type
        
    def type_check_pass_two(self,type_check_ctx):
        self.variable_type_node.type_check_pass_two(type_check_ctx)
        

class AtomicallyNode(_AstNode):
    def __init__(self,filename,scope_node):

        super(AtomicallyNode,self).__init__(
            filename,ast_labels.ATOMICALLY,scope_node.line_number)

        self.statement_list = scope_node.get_statement_list()

    def type_check_pass_one(self,struct_types_ctx):
        for node in self.statement_list:
            node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        # push new scope for any variables declared in statement list.
        type_check_ctx.push_scope()
        for node in self.statement_list:
            node.type_check_pass_two(type_check_ctx)
        type_check_ctx.pop_scope()
        
class ScopeNode (_AstNode):
    def __init__(self,filename,scope_body_node):
        super(ScopeNode,self).__init__(
            filename,ast_labels.SCOPE,scope_body_node.line_number)
        self.statement_list = scope_body_node.get_statement_list()

    def get_statement_list(self):
        return list(self.statement_list)

    def type_check_pass_one(self,struct_types_ctx):
        for node in self.statement_list:
            node.type_check_pass_one(struct_types_ctx)
    
    def type_check_pass_two(self,type_check_ctx):
        # push new scope for any variables declared in statement list.
        type_check_ctx.push_scope()
        for node in self.statement_list:
            node.type_check_pass_two(type_check_ctx)
        type_check_ctx.pop_scope()

        
class ParallelNode(_AstNode):
    def __init__(self,filename,to_iter_over_expression_node,
                 lambda_expression_node,line_number):
        super(ParallelNode,self).__init__(filename,ast_labels.PARALLEL,line_number)
        self.to_iter_over_expression_node = to_iter_over_expression_node
        self.lambda_expression_node = lambda_expression_node

    def type_check_pass_one(self,struct_types_ctx):
        self.to_iter_over_expression_node.type_check_pass_one(struct_types_ctx)
        self.lambda_expression_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.to_iter_over_expression_node.type_check_pass_two(type_check_ctx)
        self.lambda_expression_node.type_check_pass_two(type_check_ctx)
        
class AssignmentNode(_AstNode):
    def __init__(self,filename,lhs_node,rhs_node):
        super(AssignmentNode,self).__init__(
            filename,ast_labels.ASSIGNMENT,lhs_node.line_number)
        self.lhs_node = lhs_node
        self.rhs_node = rhs_node

    def type_check_pass_one(self,struct_types_ctx):
        self.lhs_node.type_check_pass_one(struct_types_ctx)
        self.rhs_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.lhs_node.type_check_pass_two(type_check_ctx)
        self.rhs_node.type_check_pass_two(type_check_ctx)
        
        if self.lhs_node.type != self.rhs_node.type:
            # endpoint method calls return wildcard types, which match
            # any left-hand type.
            if not isinstance(self.rhs_node.type,WildcardType):
                return
            
            # check return type of method node
            if (isinstance(self.rhs_node.type,MethodType) and
                (self.lhs_node.type != self.rhs_node.type.returns_type)):
                    raise TypeCheckException(
                        self.filename,self.line_number,
                        'lhs type of %s does not agree with rhs type of %s' %
                        (str(self.lhs_node.type),str(self.rhs_node.type)))
            
        
class NotNode(_AstNode):
    def __init__(self,filename,to_not_node):
        super(NotNode,self).__init__(
            filename,ast_labels.NOT,to_not_node.line_number)
        
        self.to_not_node = to_not_node
        self.type = BasicType(ast_labels.BOOL_TYPE,False)
        
    def type_check_pass_one(self,struct_types_ctx):
        pass
        
    def type_check_pass_two(self,type_check_ctx):
        pass

        
class LenNode(_AstNode):
    def __init__(self,filename,len_of_node,line_number):
        super(LenNode,self).__init__(
            filename,ast_labels.LEN,line_number)
        
        self.len_of_node = len_of_node
        self.type = BasicType(ast_labels.NUMBER_TYPE,False)
        
    def type_check_pass_one(self,struct_types_ctx):
        pass

    def type_check_pass_two(self,type_check_ctx):
        pass
    
        
class ReturnNode(_AstNode):
    def __init__(self,filename,line_number):
        super(ReturnNode,self).__init__(filename,ast_labels.RETURN,line_number)
        self.what_to_return_node = None
    def add_return_expression_node(self,what_to_return_node):
        self.what_to_return_node = what_to_return_node

    def type_check_pass_one(self,struct_types_ctx):
        if self.what_to_return_node is not None:
            self.what_to_return_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        if self.what_to_return_node is None:
            self.type = None
        else:
            self.what_to_return_node.type_check_pass_two(type_check_ctx)
            self.type = self.what_to_return_node.type

class ConditionNode(_AstNode):
    def __init__(self,filename,if_node,elifs_node,else_node):
        '''
        @param {ElseNode} else_node --- Can have a body of None.
        '''
        super(ConditionNode,self).__init__(
            filename,ast_labels.CONDITION,if_node.line_number)

        self.if_node = if_node
        self.elifs_list = elifs_node.get_else_if_node_list()
        # else_none_body may be None
        self.else_node_body = else_node.body_node

    def type_check_pass_one(self,struct_types_ctx):
        self.type = None
        self.if_node.type_check_pass_one(struct_types_ctx)
        for elif_node in self.elifs_list:
            elif_node.type_check_pass_one(struct_types_ctx)
        if self.else_node_body is not None:
            self.else_node_body.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.type = None
        self.if_node.type_check_pass_two(type_check_ctx)
        for elif_node in self.elifs_list:
            elif_node.type_check_pass_two(type_check_ctx)
        if self.else_node_body is not None:
            self.else_node_body.type_check_pass_two(type_check_ctx)

            
class IfNode(_AstNode):
    def __init__(self,filename,predicate_node,if_body_node,line_number):
        super(IfNode,self).__init__(filename,ast_labels.IF,line_number)

        self.predicate_node = predicate_node
        self.body_node = if_body_node
        self.type = None
        
    def type_check_pass_one(self,struct_types_ctx):
        self.predicate_node.type_check_pass_one(struct_types_ctx)
        self.body_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.predicate_node.type_check_pass_two(type_check_ctx)
        self.body_node.type_check_pass_two(type_check_ctx)

        
class ElifNode(_AstNode):
    def __init__(self,filename,predicate_node,elif_body_node,line_number):
        super(ElifNode,self).__init__(filename,ast_labels.ELIF,line_number)

        self.predicate_node = predicate_node
        self.body_node = elif_body_node
        self.type = None

    def type_check_pass_one(self,struct_types_ctx):
        self.predicate_node.type_check_pass_one(struct_types_ctx)
        self.body_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.predicate_node.type_check_pass_two(type_check_ctx)
        self.body_node.type_check_pass_two(type_check_ctx)


class BracketNode(_AstNode):
    def __init__(self,filename,outside_bracket_node,inside_bracket_node):
        '''
        @param {_AstNode} outside_bracket_node --- variable_node is
        either an identifier node, bracket node, or dot node
        '''
        super(BracketNode,self).__init__(
            filename,ast_labels.BRACKET,outside_bracket_node.line_number)
        
        self.outside_bracket_node = outside_bracket_node
        self.inside_bracket_node = inside_bracket_node
        
    def type_check_pass_one(self,struct_types_ctx):
        self.outside_bracket_node.type_check_pass_one(struct_types_ctx)
        self.inside_bracket_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.outside_bracket_node.type_check_pass_two(type_check_ctx)
        self.inside_bracket_node.type_check_pass_two(type_check_ctx)
        self.type = self.outside_bracket_node.type.value_type
        
class DotNode(_AstNode):
    def __init__(self,filename,left_of_dot_node, right_of_dot_node):
        super(DotNode,self).__init__(
            filename,ast_labels.DOT,left_of_dot_node.line_number)
        
        self.left_of_dot_node = left_of_dot_node
        self.right_of_dot_node = right_of_dot_node

    def type_check_pass_one(self,struct_types_ctx):
        self.left_of_dot_node.type_check_pass_one(struct_types_ctx)
        self.right_of_dot_node.type_check_pass_one(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        self.left_of_dot_node.type_check_pass_two(type_check_ctx)
        # for dots, need to ensure that right hand of dot exists/is
        # available.
        if self.right_of_dot_node.label == ast_labels.METHOD_CALL:
            # check in left hand side whether method is a valid field
            # of left hand side.
            method_name = self.right_of_dot_node.method_node.value
            dict_dot_fields = self.left_of_dot_node.type.dict_dot_fields()

            if method_name not in dict_dot_fields:
                raise TypeCheckException(
                    self.filename,self.line_number,
                    'Unknown field %s on lhs of type %s' %
                    (method_name,str(self.left_of_dot_node.type)))

            self.right_of_dot_node.type = dict_dot_fields[method_name]
            
        elif self.right_of_dot_node.label == ast_labels.DOT:
            # FIXME: type check to ensure that identifier exists
            self.right_of_dot_node.type_check_pass_two(type_check_ctx)
        elif self.right_of_dot_node.label == ast_labels.IDENTIFIER_EXPRESSION:
            identifier_name = self.right_of_dot_node.value

            if not isinstance(self.left_of_dot_node.type,EndpointType):
                dict_dot_fields = self.left_of_dot_node.type.dict_dot_fields()

                if identifier_name not in dict_dot_fields:
                    raise TypeCheckException(
                        self.filename,self.line_number,
                        'Unknown field %s on lhs of type %s' %
                        (identifier_name,str(self.left_of_dot_node.type)))
                self.right_of_dot_node.type = dict_dot_fields[identifier_name]
            else:
                self.right_of_dot_node.type = WildcardType()
        else:
            raise TypeCheckException(
                self.filename,self.line_number,
                'Type check error for dot expression.  Right hand side of ' + 
                'dot must be an identifier.')
            
        self.type = self.right_of_dot_node.type

        
class MethodCallNode(_AstNode):
    def __init__(self,filename,variable_node,method_call_args_node):
        super(MethodCallNode,self).__init__(
            filename,ast_labels.METHOD_CALL,variable_node.line_number)
        
        self.method_node = variable_node
        self.args_list = method_call_args_node.get_args_list()
        
    def type_check_pass_one(self,struct_types_ctx):
        self.method_node.type_check_pass_one(struct_types_ctx)
        for arg_node in self.args_list:
            arg_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        self.method_node.type_check_pass_two(type_check_ctx)
        self.type = self.method_node.type

        if not isinstance(self.type, WildcardType):
            # wildcards are returned from endpoint calls, do not have
            # program src to build correct number of arguments:
            # ignoring checkin that num arguments match.
            if self.type.num_arguments() != len(self.args_list):
                method_name = self.method_node.value
                raise TypeCheckException(
                    self.filename,self.line_number,
                    'Type check error on method call: incorrect number ' +
                    'of arguments passed in to method %s.' % method_name)
        
        # type check each argument passed in
        for arg_node in self.args_list:
            arg_node.type_check_pass_two(type_check_ctx)

class RangeExpressionNode(_AstNode):
    def __init__(self,filename,start_expression_node,increment_expression_node,
                 end_expression_node,line_number):
        
        super(RangeExpressionNode,self).__init__(
            filename,ast_labels.RANGE,line_number)

        # where range statement starts from
        self.start_expression_node = start_expression_node
        # how much to increment for each value
        self.increment_expression_node = increment_expression_node
        # where to end range expression
        self.end_expression_node = end_expression_node

    def type_check_pass_one(self,struct_types_ctx):
        raise InternalParseException(
            self.filename,self.line_number,
            'FIXME: must add type check for range expression')
    def type_check_pass_two(self,type_check_ctx):
        raise InternalParseException(
            self.filename,self.line_number,
            'FIXME: must add type check for range expression')        
        
class _LiteralNode(_AstNode):
    '''
    Parent class of NumberLiteralNode, TextLiteralNode,
    TrueFalseLiteralNode
    '''
    def __init__(self,filename,label,value,line_number,basic_type):
        super(_LiteralNode,self).__init__(filename,label,line_number)
        self.line_number = line_number
        self.value = value
        self.basic_type = basic_type
        self.type = BasicType(self.basic_type,False)

    def type_check_pass_one(self,struct_types_ctx):
        pass        
    def type_check_pass_two(self,type_check_ctx):
        pass

        
class NumberLiteralNode(_LiteralNode):
    def __init__(self,filename,number,line_number):
        super(NumberLiteralNode,self).__init__(
            filename,ast_labels.NUMBER_LITERAL,number,line_number,
            ast_labels.NUMBER_TYPE)

class TextLiteralNode(_LiteralNode):
    def __init__(self,filename,text,line_number):
        super(TextLiteralNode,self).__init__(
            filename,ast_labels.TEXT_LITERAL,text,line_number,
            ast_labels.STRING_TYPE)
        
class TrueFalseLiteralNode(_LiteralNode):
    def __init__(self,filename,true_false,line_number):
        super(TrueFalseLiteralNode,self).__init__(
            filename,ast_labels.TRUE_FALSE_LITERAL,true_false,line_number,
            ast_labels.BOOL_TYPE)

class NullLiteralNode(_AstNode):
    def __init__(self,filename,line_number):
        super(NullLiteralNode,self).__init__(
            filename,ast_labels.NULL_TYPE,line_number)
        self.type = NullType()

    def type_check_pass_one(self,struct_types_ctx):
        pass        
    def type_check_pass_two(self,type_check_ctx):
        pass
        
class VariableTypeNode(_AstNode):
    pass
        
class BasicTypeNode(VariableTypeNode):
    def __init__(self,filename,basic_type,is_tvar,line_number):
        super(VariableTypeNode,self).__init__(
            filename,ast_labels.VARIABLE_TYPE,line_number)

        self.type = self._build_type(basic_type,is_tvar)
        
    def _build_type(self,basic_type,is_tvar):
        return BasicType(basic_type,is_tvar)

    def type_check_pass_one(self,struct_types_ctx):
        pass    
    def type_check_pass_two(self,type_check_ctx):
        pass

class ListVariableTypeNode(VariableTypeNode):
    def __init__(self,filename,element_type_node,is_tvar,line_number):
        super(ListVariableTypeNode,self).__init__(
            filename,ast_labels.LIST_VARIABLE_TYPE,line_number)
        self.element_type_node = element_type_node
        self.is_tvar = is_tvar
        self.type = ListType()

    def type_check_pass_one(self,struct_types_ctx):
        self.element_type_node.type_check_pass_one(struct_types_ctx)
        self.type.update_element_type_tvar(
            self.element_type_node,self.is_tvar)

    def type_check_pass_two(self,type_check_ctx):
        pass
    
class MapVariableTypeNode(VariableTypeNode):
    def __init__(self,filename,from_type_node,to_type_node,is_tvar,
                 line_number):
        super(MapVariableTypeNode,self).__init__(
            filename,ast_labels.MAP_VARIABLE_TYPE,line_number)
        self.from_type_node = from_type_node
        self.to_type_node = to_type_node
        self.is_tvar = is_tvar
        self.type = MapType()

    def type_check_pass_one(self,struct_types_ctx):
        self.from_type_node.type_check_pass_one(struct_types_ctx)
        self.to_type_node.type_check_pass_one(struct_types_ctx)
        self.type.update_from_to_tvar(
            self.from_type_node,self.to_type_node,self.is_tvar)

    def type_check_pass_two(self,type_check_ctx):
        pass

class StructVariableTypeNode(VariableTypeNode):
    def __init__(self,filename,struct_name_identifier_node,is_tvar,
                 line_number):
        super(StructVariableTypeNode,self).__init__(
            filename,ast_labels.STRUCT_VARIABLE_TYPE,line_number)
        self.struct_name = struct_name_identifier_node.value
        self.is_tvar = is_tvar

        self.fixupable_object = FixupableObject(
            FixupableObject.FIXUPABLE_TYPE_STRUCT,
            self.struct_name,self.is_tvar)

    def get_fixupable_object(self):
        return self.fixupable_object

        
    def type_check_pass_one(self,struct_types_ctx):
        struct_type_obj = (
            struct_types_ctx.get_type_obj_from_struct_name(
                self.struct_name))
        if struct_type_obj is None:
            raise TypeCheckException(
                self.filename,self.line_number,
                'Unknown struct named %s.' % self.struct_name)
        # struct_type_obj should be a StructType object
        self.type = struct_type_obj.clone(self.is_tvar)
        
    def type_check_pass_two(self,type_check_ctx):
        pass

class EndpointVariableTypeNode(VariableTypeNode):
    def __init__(self,filename,endpoint_name_identifier_node,is_tvar,
                 line_number):
        super(EndpointVariableTypeNode,self).__init__(
            filename,ast_labels.ENDPOINT_VARIABLE_TYPE,line_number)
        self.endpoint_name = endpoint_name_identifier_node.value
        self.is_tvar = is_tvar

        self.alias_name = None
        self.fixupable_object = FixupableObject(
            FixupableObject.FIXUPABLE_TYPE_ENDPOINT,
            self.endpoint_name,self.is_tvar)

    def get_fixupable_object(self):
        return self.fixupable_object
        
    def setup_type_from_alias(self,struct_types_ctx):
        self.alias_name = struct_types_ctx.alias_ctx.get_endpoint_alias(
            self.endpoint_name)
        
        if self.alias_name is None:
            raise TypeCheckException(
                self.filename,self.line_number,
                'Require alias definition for endpoint %s.' % self.endpoint_name)

        self.type = EndpointType(
            self.endpoint_name,self.is_tvar,self.alias_name)

        struct_types_ctx.add_endpoint_type_obj_for_name(
            self.endpoint_name,self.type,self.line_number)
        
        
    def type_check_pass_one(self,struct_types_ctx):
        if self.alias_name is None:
            self.setup_type_from_alias(struct_types_ctx)
        
    def type_check_pass_two(self,type_check_ctx):
        pass

class ServiceFactoryVariableTypeNode(VariableTypeNode):
    def __init__(self,filename,is_tvar,line_number):
        super(ServiceFactoryVariableTypeNode,self).__init__(
            filename,ast_labels.SERVICE_FACTORY_TYPE,line_number)

        self.is_tvar = is_tvar
        self.type = ServiceFactoryType(self.is_tvar)

    def type_check_pass_one(self,struct_types_ctx):
        pass
    
    def type_check_pass_two(self,type_check_ctx):
        pass
        
class _BinaryExpressionNode(_AstNode):
    def __init__(self,filename,label,lhs_expression_node,rhs_expression_node):
        super (_BinaryExpressionNode,self).__init__(
            filename,label,lhs_expression_node.line_number)
        self.lhs_expression_node = lhs_expression_node
        self.rhs_expression_node = rhs_expression_node

class _ArithmeticExpressionNode(_BinaryExpressionNode):

    def type_check_pass_one(self,struct_types_ctx):
        self.lhs_expression_node.type_check_pass_one(struct_types_ctx)
        self.rhs_expression_node.type_check_pass_one(struct_types_ctx)
    
    def type_check_pass_two(self,type_check_ctx):
        self.lhs_expression_node.type_check_pass_two(type_check_ctx)
        self.rhs_expression_node.type_check_pass_two(type_check_ctx)
        self.type = BasicType(ast_labels.NUMBER_TYPE,False)

class _LogicalExpressionNode(_BinaryExpressionNode):
    def type_check_pass_one(self,struct_types_ctx):
        self.lhs_expression_node.type_check_pass_one(struct_types_ctx)
        self.rhs_expression_node.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        self.lhs_expression_node.type_check_pass_two(type_check_ctx)
        self.rhs_expression_node.type_check_pass_two(type_check_ctx)
        self.type = BasicType(ast_labels.BOOL_TYPE,False)
        
        
class MultiplyExpressionNode(_ArithmeticExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(MultiplyExpressionNode,self).__init__(
            filename,ast_labels.MULTIPLY,lhs_expression_node,
            rhs_expression_node)
        
class DivideExpressionNode(_ArithmeticExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(DivideExpressionNode,self).__init__(
            filename,ast_labels.DIVIDE,lhs_expression_node,rhs_expression_node)
        
class AddExpressionNode(_ArithmeticExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(AddExpressionNode,self).__init__(
            filename,ast_labels.ADD,lhs_expression_node,rhs_expression_node)
        
class SubtractExpressionNode(_ArithmeticExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(SubtractExpressionNode,self).__init__(
            filename,ast_labels.SUBTRACT,lhs_expression_node,
            rhs_expression_node)
        
class GreaterThanExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(GreaterThanExpressionNode,self).__init__(
            filename,ast_labels.GREATER_THAN,lhs_expression_node,
            rhs_expression_node)
        
class GreaterThanEqualsExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(GreaterThanEqualsExpressionNode,self).__init__(
            filename,ast_labels.GREATER_THAN_EQUALS,lhs_expression_node,
            rhs_expression_node)
        
class LessThanExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(LessThanExpressionNode,self).__init__(
            filename,ast_labels.LESS_THAN,lhs_expression_node,
            rhs_expression_node)
        
class LessThanEqualsExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(LessThanEqualsExpressionNode,self).__init__(
            filename,ast_labels.LESS_THAN_EQUALS,lhs_expression_node,
            rhs_expression_node)
        
class EqualsExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(EqualsExpressionNode,self).__init__(
            filename,ast_labels.EQUALS,lhs_expression_node,rhs_expression_node)
        
class NotEqualsExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(NotEqualsExpressionNode,self).__init__(
            filename,ast_labels.NOT_EQUALS,lhs_expression_node,
            rhs_expression_node)
        
class AndExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(AndExpressionNode,self).__init__(
            filename,ast_labels.AND,lhs_expression_node,rhs_expression_node)
        
class OrExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(OrExpressionNode,self).__init__(
            filename,ast_labels.OR,lhs_expression_node,rhs_expression_node)
        
class InExpressionNode(_BinaryExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(InExpressionNode,self).__init__(
            filename,ast_labels.IN,lhs_expression_node,rhs_expression_node)
    def type_check(self,type_check_ctx):
        self.type = BasicType(ast_labels.BOOL_TYPE,False)
        
class NotInExpressionNode(_LogicalExpressionNode):
    def __init__(self,filename,lhs_expression_node,rhs_expression_node):
        super(NotInExpressionNode,self).__init__(
            filename,ast_labels.NOT_IN,lhs_expression_node,rhs_expression_node)
        
        
def create_binary_expression_node(
    operator,lhs_expression_node,rhs_expression_node):

    if operator == '*':
        return MultiplyExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '/':
        return DivideExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '+':
        return AddExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '-':
        return SubtractExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '>':
        return GreaterThanExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '>=':
        return GreaterThanEqualsExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == '<':
        return LessThanExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)    
    elif operator == '<=':
        return LessThanEqualsExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)    
    elif operator == '==':
        return EqualsExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)    
    elif operator == '!=':
        return NotEqualsExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == 'and':
        return AndExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == 'or':
        return OrExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == 'in':
        return InExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    elif operator == 'not in':
        return NotInExpressionNode(
            lhs_expression_node.filename,lhs_expression_node,
            rhs_expression_node)
    
    raise InternalParseException(
        lhs_expression_node.filename,lhs_expression_node.line_number,
        'Unknown binary operator when creating binary expression')

    
    
#### Intermediate nodes that get removed from actual AST ####
    
class EndpointListNode(_AstNode):
    def __init__(self,filename):
        super(EndpointListNode,self).__init__(
            filename,ast_labels.ENDPOINT_LIST_STATEMENT,0)
        self.endpoint_definition_node_list = []
        self.interface_definition_node_list = []
        
    def append_endpoint_definition(self,endpoint_definition_node):
        self.endpoint_definition_node_list.append(endpoint_definition_node)

    def append_interface_definition(self,interface_definition_node):
        self.interface_definition_node_list.append(interface_definition_node)

    def endpoint_definitions_node_list(self):        
        return self.endpoint_definition_node_list
    
    def endpoint_interfaces_node_list(self):
        return self.interface_definition_node_list

        
class EmptyNode(_AstNode):
    def __init__(self,filename):
        super(EmptyNode,self).__init__(filename,ast_labels.EMPTY_STATEMENT,0)

class MethodDeclarationArgsNode(_AstNode):
    def __init__(self,filename,):
        super(MethodDeclarationArgsNode,self).__init__(
            filename,ast_labels.METHOD_DECLARATION_ARGS,0)
    def append_method_declaration_arg(self,method_declaration_arg):
        self._append_child(method_declaration_arg)

    def to_list(self):
        return list(self.children)
        
class ScopeBodyNode(_AstNode):
    def __init__(self,filename,line_number):
        super(ScopeBodyNode,self).__init__(
            filename,ast_labels.SCOPE_BODY,line_number)
        
    def append_statement_node(self,statement_node):
        self._append_child(statement_node)

    def get_statement_list(self):
        '''
        @returns{list} --- Each element is an ast node ordered by the
        order that it appears in the method's body.
        '''
        return list(self.children)

class MethodCallArgsNode(_AstNode):
    def __init__(self,filename,line_number):
        super(MethodCallArgsNode,self).__init__(
            filename,ast_labels.METHOD_CALL_ARGS,line_number)

    def append_arg(self,expression_node):
        self._append_child(expression_node)

    def get_args_list(self):
        return list(self.children)
    
class ElseIfNodes(_AstNode):
    def __init__(self,filename):
        super(ElseIfNodes,self).__init__(filename,ast_labels.ELSE_IFS,0)
        # each element of children is an if ndoe

    def append_else_if(self,else_if_node):
        '''
        @param {IfNode} else_if_node --- Because of similar structure,
        each else_if_node is an IfNode.
        '''
        self._append_child(else_if_node)
        
    def get_else_if_node_list(self):
        return list(self.children)

class ElseNode (_AstNode):
    def __init__(self,filename,line_number):
        super(ElseNode,self).__init__(filename,ast_labels.ELSE,line_number)
        self.body_node = None
    def add_else_body_node (self,body_node):
        self.body_node = body_node
        
    
class StructListNode(_AstNode):
    def __init__(self,filename):
        super(StructListNode,self).__init__(
            filename,ast_labels.STRUCT_LIST_NODE,0)
        
    def add_struct_definition_node(self,struct_definition_node):
        self._append_child(struct_definition_node)
        
    def to_list(self):
        return list(self.children)
    

class AliasListNode(_AstNode):
    def __init__(self,filename):
        super(AliasListNode,self).__init__(
            filename,ast_labels.ALIAS_LIST_NODE,0)
        
    def add_alias_node(self,alias_node):
        self._append_child(alias_node)
        
    def to_list(self):
        return list(self.children)

    
class StructBodyNode(_AstNode):
    def __init__(self,filename):
        super(StructBodyNode,self).__init__(
            filename,ast_labels.STRUCT_BODY,0)
    
    def add_struct_field(self,declaration_statement_node):
        self._append_child(declaration_statement_node)

    def register_struct_endpoint_fields(self,struct_types_ctx):
        for declaration_statement_node in self.children:
            if isinstance(declaration_statement_node.type_node,EndpointVariableTypeNode):
                # tell the endpoint variable what its alias is if it
                # needs one
                declaration_statement_node.type_node.setup_type_from_alias(
                    struct_types_ctx)

    def get_initializer_dict(self):
        '''
        Returns:
            {dict} indices are strings; each is the name of a field in
            the struct.  Values are initializer nodes or None if no
            initializer is defined.
        '''
        initializer_dict = {}
        for declaration_statement_node in self.children:
            field_name = declaration_statement_node.var_name
            initializer_dict[field_name] = (
                declaration_statement_node.initializer_node)

        return initializer_dict

    def get_field_dict(self):
        '''
        Returns:
            {dict} Indices are strings; each is the name of a field in
            the struct.  Values are type objects (not type astnodes)
            associated with each field.

            {dict} To fixup
        '''
        field_dict = {}
        to_fixup = {}
        for declaration_statement_node in self.children:
            field_name = declaration_statement_node.var_name
            field_dict[field_name] = declaration_statement_node.type_node.type
            
            if field_dict[field_name] is None:
                # means that it's a pointer to a struct or endpoint
                # that we may have to fixup later
                to_fixup[field_name] = (
                    declaration_statement_node.type_node.get_fixupable_object())

                
        return field_dict, to_fixup

    def type_check_pass_one(self,struct_types_ctx):
        for child in self.children:
            child.type_check_pass_one(struct_types_ctx)

    def type_check_pass_two(self,type_check_ctx):
        for child in self.children:
            child.type_check_pass_two(type_check_ctx)

class ImplementsListNode(_AstNode):
    def __init__(self,filename,line_number):
        super(ImplementsListNode,self).__init__(
            filename,ast_labels.IMPLEMENTS_LIST_NODE,line_number)

    def add_variable_type_node(self,variable_type_node):
        self._append_child(variable_type_node)
        
    def to_list(self):
        return list(self.children)
