#!/usr/bin/python

from ralph.lex.ralph_lex import tokens,construct_lexer
import deps.ply.ply.yacc as yacc


#note: global variable used by yacc.  Without this set, otherwise,
#starts at first rule.
start = 'RootExpression';
#need to have something named lexer for parser to chew into
lexer = None;


def p_RootExpression(p):
    '''
    RootExpression : EndpointList
    '''
def p_EndpointList(p):
    '''
    EndpointList : EndpointList EndpointDefinition 
                 | EndpointDefinition
    '''

def p_EndpointDefinition(p):
    '''
    EndpointDefinition : ENDPOINT Identifier CURLY_LEFT EndpointMiddle CURLY_RIGHT
    '''

def p_EndpointMiddle(p):
    '''
    EndpointMiddle : DeclarationStatement SEMI_COLON EndpointMiddle
                   | FunctionDeclaration EndpointMiddle
                   | Empty
    '''

def p_DeclarationStatement(p):
    '''
    DeclarationStatement : VariableType Identifier
                         | VariableType Identifier EQUALS Expression
    '''

def p_FunctionDeclaration(p):
    '''
    FunctionDeclaration : FunctionSignature CURLY_LEFT FunctionBody CURLY_RIGHT
                        | FunctionSignature CURLY_LEFT CURLY_RIGHT
    '''

def p_FunctionBody(p):
    '''
    FunctionBody : FunctionBody Statement
                 | Statement
    '''
    
def p_FunctionSignature(p):
    '''
    FunctionSignature : Identifier LEFT_PAREN FunctionDeclarationArgs RIGHT_PAREN
                      | Identifier LEFT_PAREN FunctionDeclarationArgs RIGHT_PAREN RETURNS VariableType
    '''
    
def p_FunctionDeclarationArgs(p):
    '''
    FunctionDeclarationArgs : FunctionDeclarationArg
                            | FunctionDeclarationArgs COMMA FunctionDeclarationArg
                            | Empty
    '''

def p_FunctionDeclarationArg(p):
    '''
    FunctionDeclarationArg : VariableType Identifier
    '''

def p_FunctionCall(p):
    '''
    FunctionCall : Variable LEFT_PAREN FunctionCallArgs RIGHT_PAREN
    '''
    
def p_FunctionCallArgs(p):
    '''
    FunctionCallArgs : Expression
                     | FunctionCallArgs COMMA Expression
                     | Empty
    '''
    
def p_Statement(p):
    '''
    Statement : Expression SEMI_COLON
              | ReturnStatement SEMI_COLON
              | DeclarationStatement SEMI_COLON
              | AssignmentStatement SEMI_COLON
              | ScopeStatement
              | ForStatement
              | ConditionStatement                  
    '''

def p_ConditionStatement(p):
    '''
    ConditionStatement : IfStatement ElseIfStatements ElseStatement
    '''

def p_IfStatement(p):
    '''
    IfStatement : IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''

def p_ElseIfStatements(p):
    '''
    ElseIfStatements : ElseIfStatements ElseIfStatement
                     | Empty
    '''
def p_ElseIfStatement(p):
    '''
    ElseIfStatement : ELSE_IF LEFT_PAREN Expression RIGHT_PAREN Statement
    '''

def p_ElseStatement(p):
    '''
    ElseStatement : ELSE Statement
                  | Empty
    '''
    
    
    
def p_ForStatement(p):
    '''
    ForStatement : FOR LEFT_PAREN VariableType Identifier IN Expression RIGHT_PAREN Statement
                 | FOR LEFT_PAREN Variable IN Expression RIGHT_PAREN Statement
    '''

def p_ScopeStatement(p):
    '''
    ScopeStatement : CURLY_LEFT Statement CURLY_RIGHT
                   | CURLY_LEFT CURLY_RIGHT
    '''
    
def p_AssignmentStatement(p):
    '''
    AssignmentStatement : Variable EQUALS Expression
    '''

def p_Variable(p):
    '''
    Variable : Identifier
             | Variable LEFT_BRACKET Expression RIGHT_BRACKET
             | Variable DOT Identifier
    '''
    
def p_ReturnStatement(p):
    '''
    ReturnStatement : RETURN_OPERATOR
                    | RETURN_OPERATOR Expression
    '''

def p_Expression(p):
    '''
    Expression : OrExpression
    '''
    
def p_OrExpression(p):
    '''
    OrExpression : OrExpression OR AndExpression
                 | AndExpression
    '''
    
def p_AndExpression(p):
    '''
    AndExpression : AndExpression AND InNotInExpression
                  | InNotInExpression
    '''
def p_InNotInExpression (p):
    '''
    InNotInExpression : InNotInExpression IN EqualsNotEqualsExpression
                      | InNotInExpression NOT IN EqualsNotEqualsExpression
                      | EqualsNotEqualsExpression
    '''
    
def p_EqualsNotEqualsExpression(p):
    '''
    EqualsNotEqualsExpression : EqualsNotEqualsExpression BOOL_EQUALS GreaterThanLessThanExpression
                              | EqualsNotEqualsExpression BOOL_NOT_EQUALS GreaterThanLessThanExpression
                              | GreaterThanLessThanExpression
    '''

def p_GreaterThanLessThanExpression(p):
    '''
    GreaterThanLessThanExpression : GreaterThanLessThanExpression GREATER_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression GREATER_THAN_EQ PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN PlusMinusExpression
                                  | GreaterThanLessThanExpression LESS_THAN_EQ PlusMinusExpression
                                  | PlusMinusExpression
    '''

    
def p_PlusMinusExpression(p):
    '''
    PlusMinusExpression : PlusMinusExpression PLUS MultDivExpression
                        | PlusMinusExpression MINUS MultDivExpression
                        | MultDivExpression
    '''
def p_MultDivExpression(p):
    '''
    MultDivExpression : MultDivExpression MULTIPLY NotExpression
                      | MultDivExpression DIVIDE NotExpression
                      | NotExpression
    '''    
def p_NotExpression(p):
    '''
    NotExpression : NOT Term
                  | Term
    '''
    
def p_Term(p):
    '''
    Term : Variable
         | FunctionCall
         | Number
         | String
         | Boolean
         | LEFT_PAREN Expression RIGHT_PAREN
    '''
def p_Number(p):
    '''
    Number : NUMBER
    '''
def p_String(p):
    '''
    String : SINGLE_LINE_STRING
    '''
def p_Boolean(p):
    '''
    Boolean : TRUE
            | FALSE
    '''
    
def p_VariableType(p):
    '''
    VariableType : BOOL_TYPE
                 | NUMBER_TYPE
                 | STRING_TYPE

                 | TVAR BOOL_TYPE
                 | TVAR NUMBER_TYPE
                 | TVAR STRING_TYPE
    '''

def p_Identifier(p):
    '''
    Identifier : IDENTIFIER
    '''

def p_Empty(p):
    '''
    Empty :
    '''
    
    
def construct_parser(suppress_warnings):
    global lexer
    lexer = construct_lexer()
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
