#!/usr/bin/perl

# To make the new book file, 
#  - run this to generate the skeleton from TOC.xml
#  - In this directory, type:
#      java -classpath \tuma\va\docSpider\DocSpider.jar DocSpider Skeleton.html book.html none -nostatus


%EXTRA_IMAGE_HTML = ( 'BookIcon' => ' width="16" height="18"',
		      'PageIcon' => ' width="17" height="20"' );
$SPACER_HTML = '<IMG ALIGN=ABSMIDDLE SRC="Images/spacer.gif" width="20" height="18">';

# read in the map file
open FH, "Map.xml" or die "No Map.xml file.\n";
@map = <FH>;
chomp(@map);
close FH;

# read in the TOC file
open FH, "TOC.xml" or die "No TOC.xml file.\n";
@toc = <FH>;
chomp(@toc);
close FH;

# read all the mappings
foreach $line (@map) {
    if ($line =~ m/<mapID target="([^"]+)" *url="([^"]+)"/) {
	# store the mapping for later use
	$map{$1} = $2
    }
}


# see how deep TOC.xml is nested.
$depth = 0;
$MAX_DEPTH=0;
foreach $line (@toc) {
   if ($line =~ m/DashIcon/) {
      next;	# don't generate HTML for the top-level TOC item
   } elsif ($line =~ m|</|) {
       $depth--;	# keep track of nesting depth
   } elsif ($line =~ m/<tocitem/) {
       $depth++ unless ($line =~ m|/>|);
   }
   if ($depth > $MAX_DEPTH) { $MAX_DEPTH = $depth; }
}
$MAX_DEPTH++;


# generate Skeleton.html
print "Generating Skeleton.html...";


open(OUT, ">Skeleton.html");
print OUT <<'HEADER';
<!--
// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 1999  United States Air Force
// 
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// 
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
// 
// E-Mail POC:  ken.raisor@hill.af.mil
-->

<HTML>
<HEAD>
<TITLE>Process Dashboard Users Manual</TITLE>
<link rel=stylesheet type="text/css" href="style.css">
<STYLE>
BODY { color: black; background-color: white }
.hideInBook { display: none }
.sepnotice { display: none }
</STYLE>
</HEAD>
<BODY>
<H1 NO_NUMBER>Process Dashboard Users Manual</H1>
<p class="unlesspsp" align="center"><a href="Topics/Overview/Separation.html"><img border="0" src="Images/PSPDisclaimer.png"></a></p>
<H2 NO_NUMBER>Table of Contents</h2>
TABLE_OF_CONTENTS

HEADER

$depth = 0;

LINE:
foreach $line (@toc) {
   if ($line =~ m/DashIcon/) {
      next LINE;	# don't generate HTML for the top-level TOC item
   } elsif ($line =~ m|</|) {
       $depth--;	# keep track of nesting depth
   } elsif ($line =~ m/<tocitem image="([^"]+)" target="([^"]+)" text="([^"]+)"/) { # "{
       $image  = $1; $img_src = $map{$image};
       $img_extra = $EXTRA_IMAGE_HTML{$image};

       $target = $2; $href = $map{$target};

       $text   = $3;

       if ($depth == 0) { print OUT "<HR>\n"; }
       $demote = $depth+1;
       print OUT "<A CLASS='includeDoc' HREF='$href' demoteTo='$demote'>$text</A>\n";

       $depth++ unless ($line =~ m|/>|);
   }
}

print OUT <<'FOOTER';
</BODY>
</HTML>
FOOTER

close(OUT);
print "Done.\n\n";
