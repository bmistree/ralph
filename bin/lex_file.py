#!/usr/bin/env python
import sys
import os

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),
    '..')
sys.path.append(base_path)
from ralph.lex.ralph_lex import construct_lexer


def lex_file(filename):
    prog_file = open(filename,'r')
    prog_text = prog_file.read()
    prog_file.close()
    lexer = construct_lexer(filename)
    lexer.input(prog_text)

    while True:
        tok = lexer.token()
        if tok:
            print tok
        else:
            break

if __name__ == '__main__':
    lex_file(sys.argv[1])
