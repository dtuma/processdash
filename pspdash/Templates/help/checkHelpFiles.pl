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

print "Checking Map.xml contents...";
$dirty = 0;
foreach $line (@map) {
   if ($line =~ m/<mapID target="([^"]+)" *url="([^"]+)"/) {
      $ID = $1;
      $url = $2;

      # remove named tags
      ($file = $url) =~ s/html#.*/html/;
      ($tagname = $url) =~ s/^.*#//;

      # while here, check to see that the file exists
      if (! -f $file) {
         print "\n$file found in Map.xml, but the file does not exist";
         $dirty = 1;
      }

      # skip graphics files
      if ($file =~ m/\.gif$/) {
         next;
      }

      # see if the named tag exists
      $found = 0;
      if ($url ne $file) {
         if (-f $file) {
            open FH, "$file";
            @html = <FH>;
            close FH;

            $state = 0;
            foreach $l (@html) {
               # tokenize
               foreach $token (split /  */,$l) {
                  if ($state == 0) {
                     if ($token =~ m/<a/i) {
                        $state = 1;
                        next;
                     }
                  }

                  if ($state == 1) {
                     if ($token =~ m/name="([^"]+)"/i) {
                        if ($1 eq $tagname) {
                           $found = 1;
                           last;
                        }
                     }

                     if ($token =~ m/<\/a>/i) {
                        if ($token !~ m/<a/i) {
                           $state = 0;
                        }
                     }
                  }
               }

               if ($found) {
                  last;
               }
            }

            if (! $found) {
               print "\n$url found in Map.xml, but I could not find the name tag.";
               $dirty = 1;
            }
         }
      }

      if (! grep /^$file$/,@mapFiles) {
         push @mapFiles, $file;
      }
      push @mapIDs, $ID;
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";

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

# verify that every TOC file is found in Map
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

# verify that every Index file is found in Map
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

# verify that every mapped file is found in either TOC or Index
print "Checking Map.xml against TOC.xml and Index.xml...";
$dirty = 0;
foreach $item (@mapIDs) {
   if (! grep /^$item$/,@tocItems) {
      if (! grep /^$item$/,@indexItems) {
         print "\n$item found in Map.xml but not in TOC.xml nor Index.xml";
         $dirty = 1;
      }
   }
}
if ($dirty == 0) {
   print "No errors.";
}
print "\n\n";
