#!/usr/bin/env perl

use strict;
use warnings;

# import required modules 
use BELFramework::WebAPI::Client;
use Data::Dumper;

# load WSDL
BELFramework::WebAPI::Client::loadWSDL("http://localhost:8080/openbel-ws/belframework/belframework.wsdl");

# setup hash of LoadKam request payload
my $loadKamRequest = {
    LoadKamRequest => {
        kam => {
            id => "",
            name => "SMALL_CORPUS_XBEL",
            description => "",
            lastCompiled => "2011-08-18T00:00:00"
        },
        filter => {}
    }
}; 

# call the 'LoadKam' webservice operation, passing the LoadKamRequest hash, and
# retrieve response and trace.
print "Loading KAM.", "\n";
my ($loadResponse, $loadTrace) = BELFramework::WebAPI::Client::callOperation('LoadKam', $loadKamRequest);

# report results
#$loadTrace->printRequest;
#$loadTrace->printResponse;

my $handle = $loadResponse->{"LoadKamResponse"}->{"handle"};
print "KAM Loaded with handle: ", $handle->{"handle"}, "\n";

my $releaseKamRequest = {
    ReleaseKamRequest => {
        kam => $handle
    }
};

# call the 'ReleaseKam' webservice operation, passing the ReleaseKamRequest hash, and
# retrieve response and trace.
print "Releasing KAM.", "\n";
my ($releaseResponse, $releaseTrace) = BELFramework::WebAPI::Client::callOperation('ReleaseKam', $releaseKamRequest);
print "KAM Release.", "\n";

print "Done.", "\n";
