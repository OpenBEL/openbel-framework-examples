#!/usr/bin/env python2
# find-mechanism-edges.py: python2 example using FindKamEdges request
#
# usage: find-mechanism-edges.py <url> <kam name>

from suds import *
from ws import *
import csv
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
    from sys import argv, exit, stderr, stdout
    if len(argv) != 3:
        msg = 'usage: find-mechanism-edges.py <url> <kam name>\n'
        stderr.write(msg)
        exit(1)

    (url, kam_name) = argv[1:]

    client = WS(url)
    handle = load_kam(client, kam_name)

    rtypes = [
        'DECREASES',
        'DIRECTLY_DECREASES',
        'DIRECTLY_INCREASES',
        'INCREASES'
    ]
    csvw = csv.writer(stdout)
    csvw.writerow(['source', 'relationship', 'target'])
    mech_edge = lambda x: x.target.function == 'RNA_ABUNDANCE'
    for item in rtypes:
        flt = client.create('EdgeFilter')
        rcrit = client.create('RelationshipTypeFilterCriteria')
        rcrit.valueSet.append(item)
        flt.relationshipCriteria.append(rcrit)
        edges = client.service.FindKamEdges(handle, flt, None)
        mech_edges = filter(mech_edge, edges)
        for edge in mech_edges:
            src = edge.source.label
            tgt = edge.target.label
            csvw.writerow([src, edge.relationship, tgt])
    exit_success()
