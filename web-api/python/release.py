#!/usr/bin/env python2
# release.py: python2 example using ReleaseKam request
#
# usage: release.py <url> <kam name>
from sys import argv, exit, stderr
from ws import exit_success, WS

if __name__ == '__main__':
    if len(argv) != 3:
        msg = 'usage: release.py <url> <kam name>\n'
        stderr.write(msg)
        exit(1)

    # setup wsdl
    (url, kam_name) = argv[1:]
    client = WS(url)

    kam = client.create('Kam')
    kam.name = kam_name

    # load kam to obtain handle
    result = client.service.LoadKam(kam)

    # release the loaded kam using handle
    result = client.service.ReleaseKam(result.handle)

    print result
    exit_success()
