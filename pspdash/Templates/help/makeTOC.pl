#!/usr/bin/perl

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


# generate TOC.html
print "Generating TOC.html...";


open(OUT, ">TOC.html");
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
<TITLE>Process Dashboard Users' Manual - Table of Contents</TITLE>
<base target="contents">
<STYLE>
BODY { color: black; background-color: white }
B  { font-family: sans-serif; font-size: x-small }
TD { white-space: nowrap; font-family: sans-serif; font-size: x-small }
A  { color: black; text-decoration: none }
</STYLE>
</HEAD>
<BODY>
<B>Process Dashboard Help</B>
<TABLE BORDER=0 CELLSPACING=0>
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

       print OUT '<TR><TD NOWRAP>';
       if ($depth > 0) {
	  $spacedepth = 20 * $depth;
          print OUT "<IMG ALIGN=ABSMIDDLE SRC=\"Images/spacer.gif\" width=$spacedepth height=1>";
       }
       print OUT "<IMG ALIGN=ABSMIDDLE src=\"$img_src\"$img_extra>",
             '<IMG ALIGN=ABSMIDDLE SRC="Images/spacer.gif" width="4" height="1">',
             "<A HREF=\"$href?$target\">$text</A>",
             "</TD></TR>\n";

       $depth++ unless ($line =~ m|/>|);
   }
}

print OUT <<'FOOTER';

</TABLE>

<SCRIPT>
  var activateLink = location.search;
  var foundLink = false;
  if (activateLink != "") {
    for (var i = document.links.length;   i-- > 0; ) {
          // it is annoying that we have to use the search string of each 
          // hyperlink above to store the Help topic ID. Something like the
          // NAME attribute would be much more appropriate.  Alas, Netscape
          // does not expose the NAME attribute via JavaScript if the HREF
          // attribute is present. The search string was just about the only
          // viable option. :-(
      if (document.links[i].search == activateLink) {
        window.top.frames[1].location = document.links[i].href;
        foundLink = true;
        break;
      }
    }
  }
  if (foundLink == false) {
    window.top.frames[1].location = "Topics/Overview/QuickOverview.html";
  }
</SCRIPT>
</BODY>
</HTML>
FOOTER

close(OUT);
print "Done.\n\n";
