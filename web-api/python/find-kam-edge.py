#!/usr/bin/env python2
# find-kam-edge.py: python2 example of loading kam, resolving kam edge, and
#                   printing out BEL statement/citation/annotations
#
# usage: find-kam-edge.py <kam name> <source> <rel> <target>

from random import choice
from suds import *
from ws import *
import time


def load_kam(client, kam_name):
    '''
    Loads a KAM by name.  This function will sleep until the KAM's
    loadStatus is 'COMPLETE'.
    '''
    def call():
        '''
        Load the KAM and return result.  Exit with error if 'loadStatus'
        is FAILED.
        '''
        kam = client.create('Kam')
        kam.name = kam_name
        result = client.service.LoadKam(kam)
        status = result['loadStatus']
        if status == 'FAILED':
            print 'FAILED!'
            print sys.exc_info()[1]
            exit_failure()
        return result

    # load kam and wait for completion
    result = call()
    while result['loadStatus'] != 'COMPLETE':
        time.sleep(0.5)
        result = call()
    return result['handle']


def print_statement(s):
    # print subject rel object triple
    
    if hasattr(s, 'objectTerm'):
        sub = s.subjectTerm.label
        rel = s.relationship
        obj = s.objectTerm.label
        print "statement: %s %s %s" % (sub, rel, obj)
    # print nested statement
    elif hasattr(s, 'objectStatement'):
        sub = s.subjectTerm.label
        rel = s.relationship
        nsub = s.objectStatement.subjectTerm.label
        nrel = s.objectStatement.relationship.value
        nobj = s.objectStatement.objectTerm.label
        print "statement: %s %s (%s %s %s)" % (
            sub, rel, nsub, nrel, nobj)
    # print subject-only statement
    else:
        print "statement: %s" % (s.subjectTerm.label)


def print_citation(c):
    citation = s.citation
    if citation:
        ct = citation.citationType
        ci = citation.id
        cn = citation.name
        print "citation - type: %s, id: %s, name: %s" % (ct, ci, cn)


def print_annotation(a):
    atype = a.annotationType.name
    value = a.value
    print "annotation - name: %s, value: %s" % (atype, value)


if __name__ == '__main__':
    from sys import argv, exit, stderr
    if len(argv) != 5:
        msg = 'usage: find-kam-edge.py <kam name> <source> <rel> <target>\n'
        stderr.write(msg)
        exit(1)
    # unpack command-line arguments; except the first script name argument
    (kam_name, source, rel, target) = argv[1:]

    client = WS('http://localhost:8080/openbel-ws/belframework.wsdl')
    handle = load_kam(client, kam_name)
    print "loaded kam '%s', handle '%s'" % (kam_name, handle.handle)

    # create nodes using BEL term labels from command-line
    src = client.create("Node")
    src.label = source
    tgt = client.create("Node")
    tgt.label = target

    # create edge from both Nodes and BEL relationship from command-line
    edge = client.create("Edge")
    edge.source = src
    edge.target = tgt
    edge.relationship = rel

    # resolve edge
    result = client.service.ResolveEdges(handle, [edge], None)
    if len(result) == 1 and result[0]:
        the_edge = result[0]
        print "found edge, id: %s" % (the_edge.id)

        # retrieve statements
        statements = client.service.GetSupportingEvidence(the_edge, None)

        # print BEL, citation, and annotations for each statement
        for s in statements:
            print_statement(s)
            if hasattr(s, 'citation'):
                print_citation(s.citation)
            if hasattr(s, 'annotations'):
                annotations = s.annotations
                for a in annotations:
                    print_annotation(a)
    else:
        print "edge not found"

    exit_success()
