#!/usr/bin/perl

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
print "Generating book.html...";


open(OUT, ">book.html");
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
<TITLE>Process Dashboard Users' Manual</TITLE>
<STYLE>
BODY { color: black; background-color: white }
</STYLE>
</HEAD>
<BODY>
<H1>Process Dashboard User's Manual</H1>
HEADER

$depth = 0;
undef $/;

LINE:
foreach $line (@toc) {
   if ($line =~ m/DashIcon/) {
      next LINE;	# don't generate HTML for the top-level TOC item
   } elsif ($line =~ m|</|) {
       $depth--;	# keep track of nesting depth
   } elsif ($line =~ m/<tocitem image="([^"]+)" target="([^"]+)" text="([^"]+)"/) { # "{

       $target = $2; $href = $map{$target};

       if (! $has_been_printed{$target}) {
	   print_file($href);
	   $has_been_printed{$target} = 1;
       }

       $depth++ unless ($line =~ m|/>|);
   }
}

print OUT <<'FOOTER';

</SCRIPT>
</BODY>
</HTML>
FOOTER

close(OUT);
print "Done.\n\n";

sub print_file($) {
    my $href = $_[0];

    open(FILE, "<$href");
    $contents = <FILE>;
    close(FILE);

    # remove all the hyperlinks from the file.
    $contents =~ s/<a/<IgnoredA/ig;
    $contents =~ s|</a|</IgnoredA|ig;

    # fix the SRC attributes on the IMG tags
    $contents =~ s|[^"']+/Images|Images|ig; #"]

    # remove the HTML headers and footers
    $contents =~ m/<body/i;
    $contents = "<IgnoredBody" . $';
    $contents =~ m|</body|i;
    $contents = $`;

    print OUT $contents;
}

