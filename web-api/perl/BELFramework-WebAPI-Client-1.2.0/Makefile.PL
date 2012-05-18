use strict;
use warnings;
use ExtUtils::MakeMaker;

WriteMakefile(
    NAME                => 'BELFramework::WebAPI::Client',
    AUTHOR              => 'Anthony Bargnesi <abargnesi@selventa.com>',
    VERSION_FROM        => 'lib/BELFramework/WebAPI/Client.pm',
    ABSTRACT_FROM       => 'lib/BELFramework/WebAPI/Client.pm',
    PL_FILES            => {},
    PREREQ_PM => {
        'Test::More' => 0,
    },
    dist                => { COMPRESS => 'gzip -9f', SUFFIX => 'gz', },
    clean               => { FILES => 'BELFramework-WebAPI-Client-*' },
);
