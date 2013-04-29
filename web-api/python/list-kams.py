#!/usr/bin/env python
from ws import exit_failure, exit_success, start
import sys
import time

def get_catalog(ws):
    '''
    Retrieve the catalog of all KAMs.
    '''
    return ws.service.GetCatalog()

def load_kam(client, kam_name):
    '''
    Loads a KAM by name.  This function will sleep until the KAM's
    loadStatus is 'COMPLETE'.
    '''
    def call():
        kam = client.create('Kam')
        kam.name = kam_name
        result = client.service.LoadKam(kam)
        print result
        status = result['loadStatus']
        if status == 'FAILED':
            print 'FAILED!'
            print sys.exc_info()[1]
            exit_failure()
        return result
    # attempt kam load
    result = call()
    while result['loadStatus'] != 'COMPLETE':
        time.sleep(0.5)
        result = call()
    return result['handle']

if __name__ == '__main__':
    print
    print 'OpenBEL Framework V2.0.0: Web API Client, List KAMs'
    print 'Copyright (c) 2011-2012, Selventa. All Rights Reserved'
    print
    # setup wsdl
    client = start()

    # fetch catalog
    try:
        catalog = get_catalog(client)
        print '   === LISTING KAMs ===   '
        for entry in catalog:
            print '   ' + entry.name
        print
    except WebFault:
        print 'FAILED!'
        print sys.exc_info()[1]
        exit_failure()

    # retrieve bel documents for each KAM
    for entry in catalog:
        name = entry.name
        print '   === LOADING KAM ===   '
        print '   KAM: "' + name + '"'
        handle = load_kam(client, name)
        print '   KAM loaded'
        print
        documents = client.service.GetBelDocuments(handle)
        print '   === LISTING DOCUMENTS ===   '
        for document in documents:
            id = document.id
            name = document.name
            desc = document.description
            print '   Name:', name
            print '   Description:', desc
            print '   ID:', id
            print
        print
    print 'Success!'
    exit_success()
