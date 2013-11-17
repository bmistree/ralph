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
    RootExpression : RETURN_OPERATOR
    '''

def construct_parser(suppress_warnings):
    global lexer
    lexer = construct_lexer()
    
    if suppress_warnings:
        returner = yacc.yacc(errorlog=yacc.NullLogger())
    else:
        returner = yacc.yacc()

    return returner;
