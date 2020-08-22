#!/bin/csh -f
# Author: Jurgen F. Doreleijers @ Fri Jul 18 16:22:13 CDT 2003
#
# Use see usage.

if ( $#argv != 0 ) then
    goto usage
endif


# Get the entry codes from a csv listing of files (1st column):
set base_name = entries_with_mr_file
#set base_name = interesting_entries
# List of entries
set dir_list  = /share/jurgen/CloneWars/Lists/2003-10-13
#set dir_list  = /share/jurgen/CloneWars/Lists/General
# Dir with all mr files
set dir_mr    = /Users/jd/CloneWars/DOCR1000/oceans12/bmrb
#set dir_mr    = /share/wattos/mr_anno_backup
# Dir with all star output files
#set dir_str   = /usr/home/jurgen/notpub_html/collab/CloneWars/mrfiles_30/all_three_program
set dir_str   = /Users/jd/Sites/NRG3.1/star
# Star version desired (enumeration 0 is 2.1.1 and 1 is 3.0)
set star_id = 1


set file_list = $dir_list/$base_name.csv
set list = (`cat $file_list`)
#set list = ( 2k0e )
echo "Number of files in list: $#list"

set x = 1c5a
foreach x ( $list ) 
    echo "Doing $x"
    $WS/wattos/scripts/convert_mr_file.csh $dir_mr $dir_str $x $star_id
end


exit 0


usage:
    echo "convert_multiple_mr_file.csh"
    exit 1
    




