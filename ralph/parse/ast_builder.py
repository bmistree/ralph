from ralph.lex.ralph_lex import tokens,construct_lexer
from ralph.lex.ralph_lex import STRUCT_TYPE_TOKEN,PRINT_TYPE_TOKEN
import deps.ply.ply.yacc as yacc
from ralph.parse.ast_node import *
from ralph.parse.parse_util import InternalParseException,ParseException

#note: global variable used by yacc.  Without this set, otherwise,
#starts at first rule.
start = 'RootStatement'
#need to have something named lexer for parser to chew into
lexer = None


def p_RootStatement(p):
    '''
    RootStatement : StructList EndpointList
    '''
    struct_list_node = p[1]
    endpoint_list_node = p[2]
    p[0] = RootStatementNode(struct_list_node,endpoint_list_node)
    

def p_StructList(p):
    '''
    StructList : StructList StructDefinition
               | Empty
    '''
    if len(p) == 2:
        struct_list_node = StructListNode()
    else:
        struct_list_node = p[1]
        struct_definition_node = p[2]
        struct_list_node.add_struct_definition_node(struct_definition_node)
    p[0] = struct_list_node

def p_StructDefinition(p):
    '''
    StructDefinition : STRUCT_TYPE Identifier CURLY_LEFT StructBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    struct_name_node = p[2]
    struct_body_node = p[4]
    p[0] = StructDefinitionNode(struct_name_node,struct_body_node,line_number)

    
def p_StructBody(p):
    '''
    StructBody : StructBody DeclarationStatement SEMI_COLON
               | Empty
    '''
    if len(p) == 2:
        struct_body_node = StructBodyNode()
    else:
        struct_body_node = p[1]
        declaration_statement_node = p[2]
        struct_body_node.add_struct_field(declaration_statement_node)
    p[0] = struct_body_node

    
    
def p_EndpointList(p):
    '''
    EndpointList : EndpointList EndpointDefinition 
                 | EndpointDefinition
    '''
    if len(p) == 2:
        endpoint_definition_node = p[1]
        endpoint_list_node = EndpointListNode(endpoint_definition_node)
    else:
        endpoint_list_node = p[1]
        endpoint_definition_node = p[2]
        endpoint_list_node.append_endpoint_definition(
            endpoint_definition_node)
        
    p[0] = endpoint_list_node

    
def p_EndpointDefinition(p):
    '''
    EndpointDefinition : ENDPOINT Identifier CURLY_LEFT EndpointBody CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    endpoint_name_identifier_node = p[2]
    endpoint_body_node = p[4]
    
    p[0] = EndpointDefinitionNode(
        endpoint_name_identifier_node,endpoint_body_node,line_number)


def p_EndpointBody(p):
    '''
    EndpointBody : DeclarationStatement SEMI_COLON EndpointBody
                 | MethodDeclaration EndpointBody
                 | Empty
    '''
    if len(p) == 4:
        # variable declaration statement
        declaration_statement_node = p[1]
        endpoint_body_node = p[3]
        endpoint_body_node.prepend_variable_declaration_node(
            declaration_statement_node)
    elif len(p) == 3:
        # method declaration statement
        method_declaration_node = p[1]
        endpoint_body_node = p[2]
        endpoint_body_node.prepend_method_declaration_node(
            method_declaration_node)
    else:
        # empty statement
        endpoint_body_node = EndpointBodyNode()

    p[0] = endpoint_body_node

def p_DeclarationStatement(p):
    '''
    DeclarationStatement : VariableType Identifier
                         | VariableType Identifier EQUALS Expression
    '''
    initializer_node = None
    
    if len(p) == 5:
        initializer_node = p[4]

    identifier_node = p[2]
    variable_type_node = p[1]
    p[0] = DeclarationStatementNode(
        identifier_node,variable_type_node,initializer_node)

    
def p_MethodDeclaration(p):
    '''
    MethodDeclaration : MethodSignature CURLY_LEFT ScopeBody CURLY_RIGHT
                      | MethodSignature CURLY_LEFT CURLY_RIGHT
    '''
    method_signature_node = p[1]
    if len(p) == 4:
        # construct empty method body node
        method_body_node = ScopeBodyNode(method_signature_node.line_number)
    if len(p) == 5:
        method_body_node = p[3]
    
    p[0] = MethodDeclarationNode(
        method_signature_node,method_body_node)

    
def p_ScopeBody(p):
    '''
    ScopeBody : ScopeBody Statement
              | Statement
    '''
    if len(p) == 2:
        statement_node = p[1]
        scope_body_node = ScopeBodyNode(statement_node.line_number)
    else:
        scope_body_node = p[1]
        statement_node = p[2]

    scope_body_node.append_statement_node(statement_node)
    p[0] = scope_body_node
    
    
def p_MethodSignature(p):
    '''
    MethodSignature : Identifier LEFT_PAREN MethodDeclarationArgs RIGHT_PAREN
                    | Identifier LEFT_PAREN MethodDeclarationArgs RIGHT_PAREN RETURNS VariableType
    '''
    method_name_identifier_node = p[1]
    method_declaration_args_node = p[3]
    returns_variable_type_node = None
    if len(p) == 7:
        returns_variable_type_node = p[6]

    p[0] = MethodSignatureNode(
        method_name_identifier_node,method_declaration_args_node,
        returns_variable_type_node)

    
def p_MethodDeclarationArgs(p):
    '''
    MethodDeclarationArgs : MethodDeclarationArg
                          | MethodDeclarationArgs COMMA MethodDeclarationArg
                          | Empty
    '''
    if len(p) == 2:
        method_declaration_args = MethodDeclarationArgsNode()
        second_arg = p[1]
        if second_arg is not None:
            method_declaration_args.append_method_declaration_arg(second_arg)
    else:
        method_declaration_args = p[1]
        method_declaration_arg = p[3]
        method_declaration_args.append_method_declaration_arg(
            method_declaration_arg)

    p[0] = method_declaration_args
        
    
def p_MethodDeclarationArg(p):
    '''
    MethodDeclarationArg : VariableType Identifier
    '''
    variable_type_node = p[1]
    identifier_node = p[2]
    p[0] = MethodDeclarationArgNode(variable_type_node,identifier_node)
    

def p_MethodCall(p):
    '''
    MethodCall : Variable LEFT_PAREN MethodCallArgs RIGHT_PAREN
               | PRINT LEFT_PAREN MethodCallArgs RIGHT_PAREN
    '''
    if p[1] == PRINT_TYPE_TOKEN:
        line_number = p.lineno(1)
        method_args_node = p[3]
        p[0] = PrintCallNode(method_args_node,line_number)
    else:
        method_name_node = p[1]
        method_args_node = p[3]
        p[0] = MethodCallNode(method_name_node,method_args_node)

def p_PartnerMethodCall(p):
    '''
    PartnerMethodCall : AT PARTNER DOT Identifier LEFT_PAREN MethodCallArgs RIGHT_PAREN
    '''
    line_number = p.lineno(1)
    method_name_node = p[4]
    method_call_args_node = p[6]
    p[0] = PartnerMethodCallNode(
        method_name_node,method_call_args_node,line_number)


    
def p_MethodCallArgs(p):
    '''
    MethodCallArgs : Expression
                   | MethodCallArgs COMMA Expression
                   | Empty
    '''
    if len(p) == 4:
        expression_node = p[3]
        method_call_args_node = p[1]
        method_call_args_node.append_arg(expression_node)
    else:
        if is_empty(p[1]):
            method_call_args_node = MethodCallArgsNode(0)
        else:
            expression_node = p[1]
            method_call_args_node = MethodCallArgsNode(
                expression_node.line_number)
            method_call_args_node.append_arg(expression_node)
        
    p[0] = method_call_args_node
        
def p_Statement(p):
    '''
    Statement : Expression SEMI_COLON
              | ReturnStatement SEMI_COLON
              | DeclarationStatement SEMI_COLON
              | AssignmentStatement SEMI_COLON
              | ForStatement
              | ConditionStatement
              | ParallelStatement
              | ScopeStatement
              | AtomicallyStatement
    '''
    p[0] = p[1]

    
def p_AtomicallyStatement(p):
    '''
    AtomicallyStatement : ATOMICALLY ScopeStatement
    '''
    scope_node = p[2]
    p[0] = AtomicallyNode(scope_node)
    
    
def p_ParallelStatement(p):
    '''
    ParallelStatement : PARALLEL LEFT_PAREN Expression COMMA Expression RIGHT_PAREN 
    '''
    line_number = p.lineno(1)
    to_iter_over_node = p[3]
    lambda_node = p[5]
    p[0] = ParallelNode(to_iter_over_node,lambda_node,line_number)


def p_LenExpression(p):
    '''
    LenExpression : LEN LEFT_PAREN Expression RIGHT_PAREN
    '''
    len_argument_node = p[3]
    p[0] = LenNode(len_argument_node)

    
def p_RangeExpression(p):
    '''
    RangeExpression : RANGE LEFT_PAREN Expression COMMA Expression COMMA Expression RIGHT_PAREN
                    | RANGE LEFT_PAREN Expression COMMA Expression RIGHT_PAREN
    '''
    start_expression_node = p[3]
    line_number = p.lineno(1)
    if len(p) == 9:
        increment_expression_node = p[5]
        end_expression_node = p[7]
    else:
        increment_expression_node = NumberLiteralNode(1,line_number)
        end_expression_node = p[5]

    range_expression_node = RangeExpressionNode(
        start_expression_node,increment_expression_node,end_expression_node,
        line_number)

    p[0] = range_expression_node
    
    
def p_ConditionStatement(p):
    '''
    ConditionStatement : IfStatement ElseIfStatements ElseStatement
    '''
    if_node = p[1]
    elifs_node = p[2]
    else_node = p[3]
    
    condition_statement = ConditionNode(if_node,elifs_node,else_node)
    p[0] = condition_statement
    
    
def p_IfStatement(p):
    '''
    IfStatement : IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''
    predicate_node = p[3]
    if_body_node = p[5]
    line_number = p.lineno(1)
    p[0] = IfNode(predicate_node,if_body_node,line_number)

def p_ElseIfStatements(p):
    '''
    ElseIfStatements : ElseIfStatements ElseIfStatement
                     | Empty
    '''
    if len(p) == 2:
        else_if_nodes = ElseIfNodes()
    else:
        else_if_nodes = p[1]
        else_if_statement = p[2]
        else_if_nodes.append_else_if(else_if_statement)
    p[0] = else_if_nodes

    
def p_ElseIfStatement(p):
    '''
    ElseIfStatement : ELSE_IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''
    line_number = p.lineno(1)
    # using if node for else if node
    predicate_node = p[3]
    body_node = p[5]
    else_if_node = ElifNode(predicate_node,body_node,line_number)
    p[0] = else_if_node

def p_ElseStatement(p):
    '''
    ElseStatement : ELSE Statement
                  | Empty
    '''
    if len(p) == 2:
        else_node = ElseNode(0)
    else:
        line_number = p.lineno(1)
        body_node = p[2]
        else_node = ElseNode(line_number)
        else_node.add_else_body_node(body_node)
    p[0] = else_node
    
    
def p_ForStatement(p):
    '''
    ForStatement : FOR LEFT_PAREN VariableType Identifier IN Expression RIGHT_PAREN Statement
    '''
    line_number = p.lineno(1)
    # declaring variable locally
    variable_type_node = p[3]
    variable_node = p[4]
    in_what_node = p[6]
    statement_node = p[8]
    p[0] = ForNode(
        variable_type_node,variable_node,in_what_node,statement_node,line_number)

def p_ScopeStatement(p):
    '''
    ScopeStatement : CURLY_LEFT ScopeBody CURLY_RIGHT
                   | CURLY_LEFT CURLY_RIGHT
    '''
    line_number = p.lineno(1)
    if len(p) == 3:
        scope_body_node = ScopeBodyNode(line_number)
    else:
        scope_body_node = p[2]
    p[0] = ScopeNode(scope_body_node)
        
    
def p_AssignmentStatement(p):
    '''
    AssignmentStatement : Variable EQUALS Expression
    '''
    lhs_node = p[1]
    rhs_node = p[3]
    p[0] = AssignmentNode(lhs_node,rhs_node)

    
def p_Variable(p):
    '''
    Variable : Identifier
             | Variable LEFT_BRACKET Expression RIGHT_BRACKET
             | Variable DOT Identifier
    '''

    if len(p) == 2:
        # Identifier
        identifier_node = p[1]
        p[0] = identifier_node
        
    elif len(p) == 5:
        # bracket expression
        
        # variable_node is either an identifier node, bracket node, or
        # dot node        
        variable_node = p[1]
        inner_bracket_node = p[3]
        bracket_node = BracketNode(variable_node,inner_bracket_node)
        p[0] = bracket_node
        
    elif len(p) == 4:
        # dot expression
        
        # variable_node is either an identifier node, bracket node, or
        # dot node
        variable_node = p[1]
        identifier_node = p[3]
        dot_node = DotNode(variable_node,identifier_node)
        p[0] = dot_node
    #### DEBUG
    else:
        raise InternalParseException(
            'Incorrect number of tokens in variable')
    #### END DEBUG


    
def p_ReturnStatement(p):
    '''
    ReturnStatement : RETURN_OPERATOR
                    | RETURN_OPERATOR Expression
    '''
    line_number = p.lineno(1)
    return_node = ReturnNode(line_number)
    if len(p) == 3:
        what_to_return_node = p[2]
        return_node.add_return_expression_node(what_to_return_node)
    p[0] = return_node
    
def p_Expression(p):
    '''
    Expression : OrExpression
    '''
    p[0] = p[1]
    
def p_OrExpression(p):
    '''
    OrExpression : OrExpression OR AndExpression
                 | AndExpression
    '''
    production_rule_for_binary_operator(p)

def p_AndExpression(p):
    '''
    AndExpression : AndExpression AND InNotInExpression
                  | InNotInExpression
    '''
    production_rule_for_binary_operator(p)

def p_InNotInExpression (p):
    '''
    InNotInExpression : InNotInExpression IN EqualsNotEqualsExpression
                      | InNotInExpression NOT IN EqualsNotEqualsExpression
                      | EqualsNotEqualsExpression
    '''
    if len(p) == 2:
        etc_expression = p[1]
        p[0] = etc_expression
    else:
        if len(p) == 4:
            lhs_expression_node = p[1]
            rhs_expression_node = p[3]
            operator = p[2]
        else:
            lhs_expression_node = p[1]
            rhs_expression_node = p[4]
            operator = 'not in'

        p[0] = create_binary_expression_node(
            operator,lhs_expression_node,rhs_expression_node)
        
    
def p_EqualsNotEqualsExpression(p):
    '''
    EqualsNotEqualsExpression : EqualsNotEqualsExpression BOOL_EQUALS GreaterThanLessThanExpression
                              | EqualsNotEqualsExpression BOOL_NOT_EQUALS GreaterThanLessThanExpression
                              | GreaterThanLessThanExpression
    '''
    production_rule_for_binary_operator(p)
    
def p_GreaterThanLessThanExpression(p):
    '''
    GreaterThanLessThanExpression : GreaterThanLessThanExpression GREATER_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression GREATER_THAN_EQ PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN_EQ PlusMinusExpression
                                  | PlusMinusExpression
    '''
    production_rule_for_binary_operator(p)
    
def p_PlusMinusExpression(p):
    '''
    PlusMinusExpression : PlusMinusExpression PLUS MultDivExpression
                        | PlusMinusExpression MINUS MultDivExpression
                        | MultDivExpression
    '''
    production_rule_for_binary_operator(p)

    
def p_MultDivExpression(p):
    '''
    MultDivExpression : MultDivExpression MULTIPLY NotExpression
                      | MultDivExpression DIVIDE NotExpression
                      | NotExpression
    '''
    production_rule_for_binary_operator(p)
    
def production_rule_for_binary_operator(p):
    '''
    Generally has the form
      rule : rule operator etc
           | etc
    '''
    if len(p) == 2:
        etc_expression = p[1]
        p[0] = etc_expression
    else:
        lhs_expression_node = p[1]
        rhs_expression_node = p[3]
        operator = p[2]

        p[0] = create_binary_expression_node(
            operator,lhs_expression_node,rhs_expression_node)

    
def p_NotExpression(p):
    '''
    NotExpression : NOT Term
                  | Term
    '''
    p[0] = p[1]
    if len(p) == 3:
        term_node = p[2]
        not_node = NotNode(term_node)
        p[0] = not_node
        
def p_Term(p):
    '''
    Term : Variable
         | MethodCall
         | PartnerMethodCall
         | Number
         | String
         | Boolean
         | LEFT_PAREN Expression RIGHT_PAREN
         | RangeExpression
         | LenExpression
    '''
    term_node = p[1]
    if len(p) == 4:
        term_node = p[2]

    p[0] = term_node

    
def p_Number(p):
    '''
    Number : NUMBER
           | MINUS NUMBER
    '''
    if len(p) == 2:
        number_literal = float(p[1])
    else:
        number_literal = -float(p[2])
    line_number = p.lineno(1)
    p[0] = NumberLiteralNode(number_literal,line_number)
    
def p_String(p):
    '''
    String : SINGLE_LINE_STRING
    '''
    text_literal = p[1]
    line_number = p.lineno(1)
    p[0] = TextLiteralNode(text_literal,line_number)
    
def p_Boolean(p):
    '''
    Boolean : TRUE
            | FALSE
    '''
    boolean_literal = p[1] == 'True'
    line_number = p.lineno(1)
    p[0] = TrueFalseLiteralNode(boolean_literal,line_number)

    
def p_VariableType(p):
    '''
    VariableType : BOOL_TYPE
                 | NUMBER_TYPE
                 | STRING_TYPE

                 | TVAR BOOL_TYPE
                 | TVAR NUMBER_TYPE
                 | TVAR STRING_TYPE

                 | MAP_TYPE LEFT_PAREN FROM COLON VariableType COMMA TO COLON VariableType RIGHT_PAREN
                 | TVAR MAP_TYPE LEFT_PAREN FROM COLON VariableType COMMA TO COLON VariableType RIGHT_PAREN

                 | LIST_TYPE LEFT_PAREN ELEMENT COLON VariableType RIGHT_PAREN
                 | TVAR LIST_TYPE LEFT_PAREN ELEMENT COLON VariableType RIGHT_PAREN
                 
                 | STRUCT_TYPE Identifier
                 | TVAR STRUCT_TYPE Identifier
    '''

    if len(p) >= 11:
        # It's a map
        if len(p) == 11:
            # non tvar map type
            is_tvar = False
            from_type_index = 5
            to_type_index = 9
        elif len(p) == 12:
            is_tvar = True
            from_type_index = 6
            to_type_index = 10
        else:
            raise InternalParseException(
                'Incorrect number of tokens fro map variable')

        from_type_node = p[from_type_index]
        to_type_node = p[to_type_index]
        line_number = p.lineno(0)
        p[0] = MapVariableTypeNode(
            from_type_node,to_type_node,is_tvar,line_number)
    elif len(p) in [7,8]:
        # it's a list type
        line_number = p.lineno(1)
        is_tvar = False
        list_element_type_index = 5
        if len(p) == 8:
            is_tvar = True
            list_element_type_index = 6
        list_element_type_node = p[list_element_type_index]
        p[0] = ListVariableTypeNode(
            list_element_type_node,is_tvar,line_number)

    elif (len(p) == 4) or (p[1] == STRUCT_TYPE_TOKEN):
        # emitting a struct type
        is_tvar = False
        struct_name_node_index = 2
        if len(p) == 4:
            # struct is a tvar
            is_tvar = True
            struct_name_node_index = 3
        line_number = p.lineno(1)
        struct_name_node = p[struct_name_node_index]
        p[0] = StructVariableTypeNode(
            struct_name_node,is_tvar,line_number)
    else:
        # basic type or tvar type
        basic_type_index = 1
        is_tvar = False
        if len(p) == 3:
            basic_type_index = 2
            is_tvar = True

        basic_type = p[basic_type_index]
        line_number = p.lineno(basic_type_index)
        p[0] = BasicTypeNode(basic_type,is_tvar,line_number)


def p_Identifier(p):
    '''
    Identifier : IDENTIFIER
    '''
    line_number = p.lineno(1)
    value = p[1]
    p[0] = IdentifierNode(value,line_number)
    

def p_Empty(p):
    '''
    Empty :
    '''
    p[0] = None

def is_empty(to_test):
    return to_test is None
    
def construct_parser(suppress_warnings):
    global lexer
    lexer = construct_lexer()
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
