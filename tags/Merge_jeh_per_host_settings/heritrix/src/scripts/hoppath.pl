#!/usr/local/bin/perl -w

# usage: heri_hoppath crawl.log urlprefix
# urlprefix has to start with 'http(s)://' or 'dns:'
# Keep in mind that this is very raw perl code that works in the IA environment only.
# To run it outside the IA environment replace sort ($sort) and bin_search ($bin_search) commands.
# Please notify igor at archive dot org of all problems.

use strict;

my $crawllog = shift;
my $urlprefix = shift;
my $flatcrawllog = "flat.crawl.log";
my $sort = "~brad/bin/av_sort_good_buffer_size -k5,5 -S 70%";
my $bin_search = "/alexa/bin/bin_search";
my @stack;
my $cmd;

# make flat and sorted crawl.log if needed.
# todo: add some smarts to it.
if ( ! -f $flatcrawllog ) {
    open (FH, "< $crawllog")
	or die "Coudn't open file $crawllog: ($!)\n";
    open (FLATLOG, "| $sort > $flatcrawllog")
	or die "Couldn't open filehandle to $flatcrawllog: ($!)\n";
    print STDOUT "Sorting crawl log file... please wait. This may take a several minutes and it is done only once!\n"; 
    my $line = "";
    while (<FH>) {
	chomp;
	if (/^200[34]/) {
	    if ($line) {
		$line =~ tr/ //s;
		print FLATLOG "$line\n";
	    }
	    $line = "";
	    $line = $_;
	} else {
	    if (/^  /) {
		$line .= $_;
	    } else {
		$line .= "\n";
	    }
	}
    }
    close(FH);
    close(FLATLOG);

}

# search for requested url prefix
$cmd = "$bin_search -f 5 -d ' ' '$urlprefix' $flatcrawllog 2>/dev/null";
my $res = `$cmd`;
if (! $res) {
    print "Url prefix not found: $urlprefix\n";
    exit;
}
chomp($res);
my @parts = split(/ /, $res);

# Adjust for crawl.log that has extra field due (multi part mime type)
if (scalar @parts == 10) {
    splice(@parts, 7, 1);
}

# Check if seed.
if (scalar @parts == 7) {
    print "Found seed for requested prefix!\nSeed: $parts[4]\n";
    exit;
}

# Get last path character
my @chars = split(//, $parts[7]); 
my $pathchar = $chars[-1];

# push first url in path to stack
my $line = "$pathchar $parts[4]\n";
push @stack, $line;

# search for this url next
my $urlinpath = $parts[8];

# follow the path
my $pathlength = length($parts[7]);
my $i = 1;
while ( $i < $pathlength ){
    # search for the url in path
    $cmd = "$bin_search -f 5 -d ' ' '$urlinpath' $flatcrawllog";
    $res = `$cmd 2>/dev/null`;
    chomp($res);
    @parts = split(/ /, $res);

    # Adjust for crawl.log that has extra field due (multi part mime type)
    if (scalar @parts == 10) {
	splice(@parts, 7, 1);
    }

    # get next path character
    @chars = split(//, $parts[7]);
    $pathchar = $chars[-1];
    
    # push url in path to stack
    $line = "$pathchar: $urlinpath\n";
    push @stack, $line;
    
    # next url in path
    $urlinpath = $parts[8];
    $i++;
}

$line = "$urlinpath\n";
push @stack, $line;
my $spaces = "";
while (@stack){
    print "$spaces", pop(@stack);
    $spaces .= " ";
}
