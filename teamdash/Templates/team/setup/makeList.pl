#!/cygdrive/c/Perl/bin/perl

opendir(DIR, ".");
@allfiles = readdir(DIR);
closedir(DIR);

@files = grep((/\.shtm$/ || /\.class$/) && !/sizeForm/, @allfiles);

open(OUT, ">filelist.txt");
print OUT "<!--#set var='Wizard_File_List' value='LIST=,",
    join(',', @files), ",' -->\n";
