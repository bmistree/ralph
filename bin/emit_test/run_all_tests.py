#!/usr/bin/env python
import os
import sys
import subprocess

'''
Runs all generated test jars.
'''

JAR_DIRECTORY = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),'build','jar_dir')

def run_all():
    if not os.path.exists(JAR_DIRECTORY):
        print (
            '\nNo jar directory detected: go into build and run ' +
            'ant jarify_all.\n')
        return

    for test_to_run in os.listdir(JAR_DIRECTORY):
        run_single_test(
            os.path.join(JAR_DIRECTORY,test_to_run))

def run_single_test(fq_test_filename):
    cmd = ['java', '-ea', '-jar', fq_test_filename]
    subprocess.call(cmd)
    

if __name__ == '__main__':
    run_all()
