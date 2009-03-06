#!/bin/sh

outdir='target'
[ -e $outdir ] || mkdir -p $outdir

url='http://webteam.archive.org/confluence/display/Heritrix/HOWTO+Launch+Heritrix'
outfile='HOWTO-Launch-Heritrix.txt' 
out="$outdir/$outfile"

if [ -e $out ]
then
  echo "$0: $out exists, not downloading"
  exit
fi

echo "$0: attempting to download and convert with html2text: $url"
if html2text -style pretty $url > $out
then
  echo "$0: html2text succeeded!"
  exit
else
  echo "$0: html2text failed, perhaps you don't have it"
fi

echo "$0: attempting to download and convert with lynx -dump: $url"
if lynx -dump -nolist $url > $out
then 
  echo "$0: lynx -dump succeeded!"
  exit
else
  echo "$0: lynx -dump failed, perhaps you don't have lynx"
fi

echo "$0: ALERT: unable to convert $url to plain text, carrying on anyway without the HOWTO"
