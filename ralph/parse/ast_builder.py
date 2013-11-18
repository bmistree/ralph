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
    EndpointMiddle : VariableDeclaration EndpointMiddle
                   | FunctionDeclaration EndpointMiddle
                   | Empty
    '''

def p_VariableDeclaration(p):
    '''
    VariableDeclaration : VariableType Identifier
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

def p_Statement(p):
    '''
    Statement : Identifier
    '''
    
def p_Expression(p):
    '''
    Expression : RETURN_OPERATOR
    '''
    
def p_VariableType(p):
    '''
    VariableType : BOOL_TYPE
                 | NUMBER_TYPE
                 | STRING_TYPE
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
