#!/usr/bin/perl


# make_reports.pl
#
# Generates very close approximations of the standard Heritrix
# crawl reports directly from the crawl log.
#
# Useful if Heritrix ends uncleanly and is unable to create them itself
#
# requires: 
#   Date::Calc module 
#   (or just comment out the two lines with "Delta_DHMS" if time calculation isn't needed in crawl-report.txt)
#
# usage: 
#   make_reports.pl <crawl.log>
#
# files created: 
#   ./crawl-report.txt.new
#   ./hosts-report.txt.new
#   ./mimetype-report.txt.new
#   ./responsecode-report.txt.new
#   ./seeds-report.txt.new
#   ./badloglines.log (where unparseable lines go)

use strict;
use Date::Calc qw( Delta_DHMS );

open (CRAWL, $ARGV[0]) or die $!;
my $starttime;
my $currenttime;
my $doccount = 0;
my $hosts;
my $mimes;
my $responses;
my $seedresponses;
my $totalsize = 0;

open (BADLINES, ">badloglines.log") or die $!;

while (my $line = <CRAWL>) {
  my ($time, $result, $size, $url, $hoppath, $refer, $mime, $thread, $dltime, $c1, $c2, $overflow) = split /\s+/, $line;

  # if there are more or less than 11 fields, one of the fields has a space in it, so skip parsing it
  if (($overflow ne "") || ($c2 eq "")) {
    print BADLINES $line;
    next;
  }

  # get first line's time
  if ($. == 1) {
    $starttime = $time;
  }
  # else get the time of the current line, in case this is the last line
  else {
    $currenttime = $time;
  }
  
  my ($host) = ($url =~ m|://(.+?)[:/]|);
  next unless ($host);
  $seedresponses->{$url} = $result if ($hoppath eq "-");
  # if not a fetching error
  if ($result > 0) {
    $hosts->{$host}{count}++;
    $hosts->{$host}{size} += $size;
    
    $mimes->{$mime}{count}++;
    $mimes->{$mime}{size} += $size;
    
    $responses->{$result}{count}++;
    
   
    
    $totalsize += $size;
  }
  if ($result >= 200 && $result <= 399) {
    $doccount++;
  }
}


#determine time taken for crawl
my ($syear, $smonth, $sday, $shour, $smin, $ssec, $sms) = ($starttime =~ /(....)(..)(..)(..)(..)(..)(...)/);
my ($eyear, $emonth, $eday, $ehour, $emin, $esec, $ems) = ($currenttime =~ /(....)(..)(..)(..)(..)(..)(...)/);
my ($days, $hours, $minutes, $seconds) = Delta_DHMS($syear, $smonth, $sday, $shour, $smin, $ssec, $eyear, $emonth, $eday, $ehour, $emin, $esec);

print_crawl_report();
print_hosts_report();
print_mimetype_report();
print_response_report();
print_seeds_report();


sub print_crawl_report {
  open (NEWCRAWL, "> crawl-report.txt.new") or die $!;
  print NEWCRAWL "Duration Time: ", ($days * 24) + $hours, "h${minutes}m${seconds}s\n";
  print NEWCRAWL "Total Hosts Crawled: ", scalar keys %$hosts, "\n";
  print NEWCRAWL "Total Documents Crawled: ", $doccount, "\n";
  print NEWCRAWL "Total Raw Data Size in Bytes: ", $totalsize, "\n";
  close NEWCRAWL; 
}

sub print_hosts_report {
  open (NEWHOSTS, "> hosts-report.txt.new") or die $!;
  print NEWHOSTS "[host]", " " x 49, "[#urls]      [#bytes]\n";
  # sort from most frequent to least frequent host
  my @sortedhosts = sort { $hosts->{$b}{count} <=> $hosts->{$a}{count} } keys %$hosts;
  foreach my $host (@sortedhosts) {
    printf NEWHOSTS ("%-50s%12d%14d\n", $host, $hosts->{$host}{count}, $hosts->{$host}{size});
  }
}

sub print_mimetype_report {
  open (NEWMIMES, "> mimetype-report.txt.new") or die $!;
  print NEWMIMES "[mime-types]", " " x 43, "[#urls]      [#bytes]\n";
  # sort from most frequent to least frequent mime-type
  my @sortedmimes = sort { $mimes->{$b}{count} <=> $mimes->{$a}{count} } keys %$mimes;
  foreach my $mime (@sortedmimes) {
    printf NEWMIMES ("%-50s%12d%14d\n", $mime, $mimes->{$mime}{count}, $mimes->{$mime}{size});
  }
}

sub print_response_report {
  open (NEWRESPONSES, "> responsecode-report.txt.new") or die $!;
  print NEWRESPONSES "[rescode]", " " x 7, "[#urls]\n";
  # sort from most frequent to least frequent response code
  my @sortedresps = sort { $responses->{$b}{count} <=> $responses->{$a}{count} } keys %$responses;
  foreach my $res (@sortedresps) {
    print NEWRESPONSES ("%-15s%8d\n", $res, $responses->{$res}{count});
  }
}

sub print_seeds_report {
  open (NEWSEEDS, "> seeds-report.txt.new") or die $!;
  # sort urls by response code, then by url
  my @sortedurls = sort { $seedresponses->{$a} cmp $seedresponses->{$b} || $a cmp $b } keys %$seedresponses;
  print NEWSEEDS "[seeds]", " " x 149, "[res-code] [status]\n";
  foreach my $url (@sortedurls) {
    print NEWSEEDS $url, " " x (166 - length($url) - length($seedresponses->{$url})), $seedresponses->{$url};
    # if anything but a heritrix error
    if ($seedresponses->{$url} > 0) {
      print NEWSEEDS " CRAWLED\n";
    }
    else {
      print NEWSEEDS " NOTCRAWLED\n";
    }
  }
}