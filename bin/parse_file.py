#!/usr/bin/env python
import sys
import os

base_path = os.path.join(
    os.path.realpath(os.path.dirname(__file__)),'..')
sys.path.append(base_path)
from ralph.parse.ast_builder import construct_parser


def parse(filename):
    file_fd = open(filename,'r')
    prog_text = file_fd.read()
    file_fd.close()

    parser = construct_parser(False)
    return parser.parse(prog_text)

if __name__ == '__main__':
    filename = sys.argv[1]
    parse(filename)
