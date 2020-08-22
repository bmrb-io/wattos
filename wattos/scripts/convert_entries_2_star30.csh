#!/bin/csh

# This script will convert all mrfiles to star

# Sets classpath for java like always
# I do this so my software gets run every week and shows me in the cron
# output if errors occur; possibly before I commit them to production jobs
source ~/.cshrc



echo "PATH is              : " $PATH
echo "CLASSPATH for java is: " $CLASSPATH
echo "Cron script is       : " convert_entries_2_star30.csh

# List of arguments
# 1 Load(y), dump (n) or convert(c):
# 2 STAR version (2 for 3.1)
# 3 Directory name in
# 4 Directory name out
# 5 Convert one entry(y) or consecutive entries(n)? (y/n):
# 6 Which entry (. for first)
# 7 Do all now? (y/n): 
# 8 Start over? (y/n):

java -Xmx1g Wattos.Episode_II.MRInterloop << EOD
c
2
.
.
y
2jt2
n
n
EOD

echo "Done"
