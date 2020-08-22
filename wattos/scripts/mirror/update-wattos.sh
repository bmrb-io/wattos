#!/bin/sh
#
# Change mysql username, password, path to binary,
# and path to wattos.sql below
#
MAILTO=web@bmrb.wisc.edu
cd /website/ftp/pub/webdata
mysql -u root --password="TOPSECRET" -e "source /website/admin/wattos.sql"
if [ $? -ne 0 ]
then
    echo "mysql -u root --password=\"TOPSECRET\" -e \"source /website/admin/wattos.sql\" returned $?" | mailx -s "DB update error" $MAILTO
    exit 1
fi
