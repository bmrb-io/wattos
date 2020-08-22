#!/bin/tcsh -f 
# Author: Jurgen F. Doreleijers @ Fri Jul 18 16:22:13 CDT 2003
#
# Use see usage.

if ( $#argv != 4 ) then
    goto usage
endif


# Dir with all mr files
set dir_mr      = $1
# Dir with all star output files
set dir_str     = $2
# Entry
set x           = $3
# Star version
set star_id     = $4

echo $CLASSPATH
java -Xmx256m Wattos.Episode_II.MRInterloop << EOD
c
$star_id
$dir_mr
$dir_str
y
$x
n
EOD


exit 0


usage:
    echo "convert_mr_file.csh <dir_mr> <dir_str> <pdb_entry_code> <star_id>"
    exit 1
    





