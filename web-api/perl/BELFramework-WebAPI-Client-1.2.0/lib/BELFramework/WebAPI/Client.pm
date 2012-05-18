package BELFramework::WebAPI::Client;

use warnings;
use strict;

our $VERSION = '2.0.0';

# import required modules 
use LWP::UserAgent;
use XML::Compile::WSDL11;
use XML::Compile::SOAP11;
use XML::Compile::Transport::SOAPHTTP;
use XML::Compile::Schema;
use Data::Dumper;

our $wsdl;
our %wsOperations = ();

sub loadWSDL {
    # read first argument which must be a WSDL URL.
    if (@_ != 1) {
        die "Must provide a WSDL URL to loadWSDL subroutine.\n"
    }
    my $wsdl_url = $_[0];

    # request WSDL URL 
    my $ua = LWP::UserAgent->new;
    my $response = $ua->get($wsdl_url);
    unless($response->is_success) {
        warn "Could not retrieve $wsdl_url: ", $response->status_line, "\n";
        exit 1;
    }

    print "== Loaded WSDL\n";
    $wsdl = XML::Compile::WSDL11->new($response->content);
    foreach my $wsop ($wsdl->operations) {
        $wsOperations{$wsop->name} = $wsdl->compileClient($wsop->name);
    }

    print "== Compiled Webservice Operations\n";
}

# compile WSDL client
sub callOperation {
    # read arguments WSDL operation and request hash.
    if (@_ != 2) {
        die "Must provide a WSDL Operation name and request hash.\n"
    }

    my $operation = $_[0];
    my $request = $_[1];

    my ($answer, $trace) = $wsOperations{$operation} -> ($request);

    # check for soap fault
    if(my $f = $answer->{Fault}) {
        my $errname = $f->{_NAME};
        my $error   = $answer->{$errname};
        print "$error->{code}\n";

        my $details = $error->{detail};
        if(not $details) {
            # system error, no $details
        }
        exit 1;
    }

    ($answer, $trace);
}


=head1 COPYRIGHT & LICENSE

Copyright 2012 Selventa, Inc., all rights reserved.

This program is free software; you can redistribute it and/or modify it
under the same terms as Perl itself.


=cut

1; # End of BELFramework::WebAPI::Client
