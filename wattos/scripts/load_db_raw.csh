#!/bin/csh -f

# This script will load all mrfiles from the current working dir into 
# the db.

# Limit the cpu usage to 20 hours.
set max_cpu_time = 72000


## NO CHANGES BELOW THIS LINE

limit cputime $max_cpu_time

echo "Maximum cpu time is  : " $max_cpu_time
echo "PATH is              : " $PATH 
echo "CLASSPATH for java is: " $CLASSPATH
echo "Cron script is       : " load_db_raw.csh

# List of arguments
# 1 Load database (y)
# 2 Load one entry(y) or consecutive entries(n)? (y/n): n
# 3 Use all from above (y) or specify your own list(n) (y/n): y
# 4 to do first (or . for first): .
# 5 Do all now? (y/n):y 
#6 Start over? :n
java -Xmx$WATTOSMEM Wattos.Episode_II.MRInterloop << EOD
y
n
y
.
y
n
EOD

echo "Done"
