#!/usr/bin/perl

# read in help.config entries
open FH, "help.config" or die "No help.config file.\n";
@conf = <FH>;
chomp(@conf);
close FH;

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

# read in the index file
open FH, "Index.xml" or die "No Index.xml file.\n";
@index = <FH>;
chomp(@index);
close FH;

# preprocess files so they match easily
for ($i=0; $i < @conf; $i++) {
   $conf[$i] =~ s/File //;
}

foreach $line (@map) {
   if ($line =~ m/<mapID target="([^"]+)" *url="([^"]+)"/) {
      $ID = $1;
      $file = $2;

      # while here, check to see that the file exists
      if (! -f $file) {
         print "$file found in Map.xml, but the file does not exist\n";
      }

      # skip graphics files
      if ($file =~ m/\.gif$/) {
         next;
      }

      push @mapFiles, $file;
      push @mapIDs, $ID;
   }
}

foreach $line (@toc) {
   if ($line =~ m/<tocitem .*target="([^"]+)"/) {
      push @tocItems, $1;
   }
}

foreach $line (@index) {
   if ($line =~ m/<indexitem .*target="([^"]+)"/) {
      push @indexItems, $1;
   }
}

# verify that all files that are mapped are in the config file, and vice versa
foreach $item (@mapFiles) {
   if (! grep /^$item$/,@conf) {
      print "$item found in Map.xml but not in help.config\n";
   }
}
foreach $item (@conf) {
   if (! grep /^$item$/,@mapFiles) {
      print "$item found in help.config but not in Map.xml\n";
   }
}

# verify that every mapped file is found in TOC and vice versa
foreach $item (@mapIDs) {
   if (! grep /^$item$/,@tocItems) {
      print "$item found in Map.xml but not in TOC.xml\n";
   }
}

foreach $item (@tocItems) {
   if (! grep /^$item$/,@mapIDs) {
      print "$item found in TOC.xml but not in Map.xml\n";
   }
}

# verify that every mapped file is found in Index and vice versa
#foreach $item (@mapIDs) {
#   if (! grep /^$item$/,@indexItems) {
#      print "$item foundin Map.xml but not in Index.xml\n";
#   }
#}

#foreach $item (@indexItems) {
#   if (! grep /^$item$/,@mapIDs) {
#      print "$item foundin Index.xml but not in Map.xml\n";
#   }
#}
