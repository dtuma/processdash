: # -*- mode: perl; -*- for editor syntax highlighting 
eval '
   perl=`which perl`
   exec perl $0 ${1+"$@"}'
if 0;

#
#  To use, run in the Templates directory:
#
#     dataFileCheck.pl <datafile>
#
#  where <datafile> is the full pathname to the dataFile.txt for the process 
#  that you want to examine.
#
#  I wrote this on a Unix machine (SGI IRIX, actually), so I'm not sure what 
#  it will do on Windows machines.  Although I can't see any big problems 
#  there.
#


# argument check
if ($#ARGV < 0) {
   die "Need a dataFile.txt file to start with (full pathname from Templates\ndirectory: e.g. psp0/datafile.txt)\n";
}

opendir DIRH, "." or die "Cannot open directory for read:$!\n";
@files = readdir DIRH;
chomp(@files);
closedir DIRH;

# pull out the globalData files
@files = grep /globalData$/,@files;

@DF = ();
$maxLen = -1;
foreach $fname (@files) {

   open FH, "$fname" or die "Cannot open global datafile $fname:$!\n";
   @DFNEW = <FH>;
   close FH;
   chomp(@DFNEW);

   for ($i=0; $i < @DFNEW; $i++) {
      $DFNEW[$i] .= " | $fname";
   }

   if (length($fname) > $maxLen) {
      $maxLen = length($fname);
   }

   push @DF,@DFNEW;
}

# remove comments and blanks before error check
@DF = grep !/^[ 	]*\|.*$/,@DF;
@DF = grep !/^=/,@DF;

# at this point there should be no collisions (or error)
@sorted = sort @DF;

for ($i=0; $i < @sorted -1; $i++) {
   ($LHS1 = $sorted[$i]) =~ s/=.*$//;
   ($LHS2 = $sorted[$i+1]) =~ s/=.*$//;

   if ($LHS1 eq $LHS2) {
      print "*** ERROR ***\nFound element $LHS1 in more than one globalData file.\n";
      print "Line1:$sorted[$i]\nLine2:$sorted[$i+1]\n";
   }
}


# open first local datafile
$fname = $ARGV[0];
if (-f $fname) {
   open FH, "$fname" or die "Cannot open $fname:$!\n";
   @DFNEW = <FH>;
   close FH;
   chomp(@DFNEW);

   for ($i=0; $i < @DFNEW; $i++) {
      $DFNEW[$i] .= " | $fname";
   }

   if (length($fname) > $maxLen) {
      $maxLen = length($fname);
   }

   push @DF, @DFNEW;
}

# this probably depends on one or more others, so...
while (grep /#include/,@DF) {
   # find the line
   $found = 0;
   for ($i=0; $i < @DF; $i++) {
      if ($DF[$i] =~ m/#include *<(.*)>/) {
         # get filename
         $fname = $1;

         open FH, $fname or die "Cannot open file $fname:$!\n";
         @DFNEW = <FH>;
         close FH;
         chomp(@DFNEW);

         for ($j=0; $j < @DFNEW; $j++) {
            $DFNEW[$j] .= " | $fname";
         }

         if (length($fname) > $maxLen) {
            $maxLen = length($fname);
         }

         # put this filename's text where the #include line was
         splice(@DF,$i,1,@DFNEW);
         print "Including $fname...\n";

         $found = 1;
         last;
      }
   }

   if (! $found) {
      print "Strange... I found #include, but no #include <file>\nThis may cause an infinite loop!\n";
   }
}

# OK, now that the whole list is here, look for duplicates, taking the last 
# defined as the correct one.

# first, remove comments and blanks
@DF = grep !/^[ 	]*\|.*$/,@DF;
@DF = grep !/^=/,@DF;

# look for dups
for ($i=0; $i < @DF; $i++) {
   for ($j=0; $j < @DF; $j++) {
      if ($i == $j) {
         next;
      }

      ($LHS1 = $DF[$i]) =~ s/=.*$//;
      ($LHS2 = $DF[$j]) =~ s/=.*$//;

      if ($LHS1 eq $LHS2) {
         # duplicate!  choose later
         if ($i < $j) {
            $DF[$i] = "=";
         } else {
            $DF[$j] = "=";
         }
      }
   }
}

# removed marked entries
@DF = grep !/^=/,@DF;

# sort the output (just for nicer output)
@sorted = sort @DF;

# now dump the results, putting the filename origination stuff on the front
print "Formulas:\n";
foreach $line (@sorted) {
   @sides = split / \| /,$line;

   printf "%*s|%s\n", $maxLen, $sides[1], $sides[0];
}
