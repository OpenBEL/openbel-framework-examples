#!perl -T

use Test::More tests => 1;

BEGIN {
	use_ok( 'BELFramework::WebAPI::Client' );
}

diag( "Testing BELFramework::WebAPI::Client $BELFramework::WebAPI::Client::VERSION, Perl $], $^X" );
