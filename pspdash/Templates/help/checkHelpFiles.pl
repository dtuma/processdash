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
print "Checking Map.xml against help.config...";
$dirty = 0;
foreach $item (@mapFiles) {
   if (! grep /^$item$/,@conf) {
      print "\n$item found in Map.xml but not in help.config";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

print "Checking help.config against Map.xml...";
$dirty = 0;
foreach $item (@conf) {
   if (! grep /^$item$/,@mapFiles) {
      print "\n$item found in help.config but not in Map.xml";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

# verify that every mapped file is found in TOC and vice versa
print "Checking Map.xml against TOC.xml...";
$dirty = 0;
foreach $item (@mapIDs) {
   if (! grep /^$item$/,@tocItems) {
      print "\n$item found in Map.xml but not in TOC.xml";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

print "Checking TOC.xml against Map.xml...";
$dirty = 0;
foreach $item (@tocItems) {
   if (! grep /^$item$/,@mapIDs) {
      print "\n$item found in TOC.xml but not in Map.xml";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

# verify that every mapped file is found in Index and vice versa
print "Checking Map.xml against Index.xml...";
$dirty = 0;
foreach $item (@mapIDs) {
   if (! grep /^$item$/,@indexItems) {
      print "\n$item found in Map.xml but not in Index.xml";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

print "Checking Index.xml against Map.xml...";
$dirty = 0;
foreach $item (@indexItems) {
   if (! grep /^$item$/,@mapIDs) {
      print "\n$item found in Index.xml but not in Map.xml";
      $dirty = 1;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";
