#!/bin/csh -f

# This script will load all mrfiles from the dir specified as mr_anno_dir in the Globals class into 
# the db.

# Limit the cpu usage to 20 hours.
set max_cpu_time = 72000

set docr_fred_dir = /share/jurgen/CloneWars/DB

## NO CHANGES BELOW THIS LINE
setenv W /share/wattos/Wattos
setenv WATTOSROOT $W
source $W/wsetup

limit cputime $max_cpu_time

echo "Maximum cpu time is  : " $max_cpu_time
echo "PATH is              : " $PATH
echo "CLASSPATH for java is: " $CLASSPATH
echo "Cron script is       : " load_db_raw.csh

# List of arguments
# 1 load DOCR/FRED db files (l)  (y/n/c/l)
# 2 Load one entry(y) or consecutive entries(n)? (y/n): n
# 3 Use all from above (y) or specify your own list(n) (y/n): y
# 4 to do first (or . for first): .
# 5 Do all now? (y/n):y

java -Xmx$WATTOSMEM Wattos.Episode_II.MRInterloop << EOD
l
$docr_fred_dir
n
.
y
n
EOD

if ( $status ) then
    echo "ERROR: MRInterloop exited with error: $status"
    exit 1
endif

echo "Done"
