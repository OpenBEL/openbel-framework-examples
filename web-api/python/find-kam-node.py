#!/usr/bin/env python2
# find-kam-node.py: python2 example of loading kam, resolving kam node, and
#                   printing out BEL terms 
#
# usage: find-kam-node.py <kam name> <source_bel_term>

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


if __name__ == '__main__':
    from sys import argv, exit, stderr
    if len(argv) != 3:
        msg = 'usage: find-kam-node.py <kam name> <source_bel_term>\n'
        stderr.write(msg)
        exit(1)
    # unpack command-line arguments; except the first script name argument
    (kam_name, source_term) = argv[1:]

    client = WS('http://localhost:8080/openbel-ws/belframework.wsdl')
    handle = load_kam(client, kam_name)
    print "loaded kam '%s', handle '%s'" % (kam_name, handle.handle)

    # create nodes using BEL term labels from command-line
    node = client.create("Node")
    node.label = source_term

    # resolve node
    result = client.service.ResolveNodes(handle, [node], None)
    if len(result) == 1 and result[0]:
        the_node = result[0]
        print "found node, id: %s" % (the_node.id)

        terms = client.service.GetSupportingTerms(the_node, None)
        for t in terms:
            print t
    else:
        print "edge not found"

    exit_success()
