#!/bin/csh -f

# This script will dump all contents of the mysql database.
# into a directory.
# Make sure that bin-log is set in the config.
#   http://free.netartmedia.net/Databases/Databases24.html
# TODO: Change the below to the right password. Don't want to include it with distribution
# but is required for using --master-data option
#set password = ''
set password = '\!Ecj%Y&R'
#set password = '4I4KMS'  ## this is in grunt's Globals.java
set user = root
#set user = docr
#set user = wattos1  ## this is in grunt's Globals.java

# Limit the cpu usage to 6 minutes.
set max_cpu_time = 360
#set DUMP_DIR = /Users/jd/Sites/viavia/mr_mysql_backup
# if( $host == "tang") then
# 	set DUMP_DIR = /share/wattos/mr_mysql_backup
# else if( $host == "grunt" ) then
	set DUMP_DIR = /raid/backup/mr_mysql_backup  
	# this might not be safe since it is on the same raid
#endif

## NO CHANGES BELOW THIS LINE 
##############################################################
echo "Doing backup_mysqldb.csh"

limit cputime $max_cpu_time

\rm -rf $DUMP_DIR >& /dev/null
mkdir -p $DUMP_DIR
chmod o+w $DUMP_DIR
#mysqldump --master-data --tab=$DUMP_DIR --opt -u $user wattos1
#mysqldump --master-data --tab=$DUMP_DIR --opt -u $user  --password=$password wattos1  # try this 01 oct 09
mysqldump --master-data --tab=$DUMP_DIR --opt --user=root --password='\!Ecj%Y&R' wattos1 # try this 01 oct 09
#mysqldump --master-data                       -u $user wattos1 > $DUMP_DIR/dump_file.sql
#mysqldump --master-data                       -u $user -p $password wattos1 > $DUMP_DIR/dump_file.sql
mysqldump --master-data                       --user=$user --password=$password wattos1 > $DUMP_DIR/dump_file.sql

chmod o-w $DUMP_DIR
echo "dump dir: $DUMP_DIR"
ls -al $DUMP_DIR
echo "Done"
