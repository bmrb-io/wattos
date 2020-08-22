#!/bin/sh
#
# because nothing persists to container's run
#
if [ ! -d /var/lib/mysql/mysql ] ; then
    echo "init db"
    mysql_install_db --user=mysql --rpm # > /dev/null

    mysqld --skip-networking &
    mysql_pid="$!"

    i=0
#    for i in 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1 0 
    while [ 30 -gt "$((i+=1))" ]
    do
        mysql --protocol=socket -uroot -e 'SELECT 1' > /dev/null 2>&1
        if [ $? -eq 0 ]
        then
            break
        fi
        echo ".. wait"
        sleep 1
    done
    if [ $i -eq 0 ] ; then
        echo >&2 "mysql startup failed"
        exit 1
    fi

# mysql bs
#
    mysql --protocol=socket -uroot > /dev/null 2>&1 << EOSQL 
        set @@SESSION.SQL_LOG_BIN=0;
        drop database if exists test;
        delete from mysql.user WHERE User='';
        create user if not exists 'root'@'%';
        update mysql.user set password = PASSWORD( '5up3r53kr37' ) where user like 'root%';
        grant all privileges on *.* to 'root'@'localhost';
        grant all privileges on *.* to 'root'@'127.0.0.1';
        grant all privileges on *.* to 'root'@'::1';
        grant all privileges on *.* to 'root'@'%';
        create user if not exists 'wattos1'@'localhost' identified by '4I4KMS';
        grant all privileges on *.* to 'wattos1'@'localhost';
        create user if not exists 'wattos1'@'127.0.0.1' identified by '4I4KMS';
        grant all privileges on *.* to 'wattos1'@'127.0.0.1';
        create user if not exists 'wattos1'@'%' identified by '4I4KMS';
        grant all privileges on *.* to 'wattos1'@'%';
        flush privileges;
        create database if not exists wattos1;
        use wattos1;
        create table if not exists entry (
            entry_id   integer primary key,
            bmrb_id    integer,
            pdb_id     char(4),
            in_recoord tinyint(1),
            in_dress   tinyint(1)
        );
        create table if not exists mrblock (
            mrblock_id    integer primary key,
            mrfile_id     integer,
            position      integer,
            program       varchar(255),
            type          varchar(255),
            subtype       varchar(255),
            format        varchar(255),
            text_type     varchar(255),
            byte_count    integer,
            item_count    integer,
            date_modified date,
            other_prop    varchar(255),
            dbfs_id       integer,
            file_name     varchar(255),
            md5_sum       varchar(32)
        );
        create table if not exists mrfile (
            mrfile_id     integer primary key,
            entry_id      integer,
            detail        varchar(255),
            pdb_id        char(4),
            date_modified date
        );
EOSQL

# now load the data
#
    if [ ! -f /wattos/dbfs/entry.csv ]
    then
        echo >&2 'dbfs not found (volume not mounted?)'
        exit 1
    fi
    mysql --protocol=socket -uwattos1 -p4I4KMS > /dev/null 2>&1 < /wattos_load.sql 

    kill -s TERM "$mysql_pid"
    if [ $? -ne 0 ]
    then
        echo >&2 'db init failed'
        exit 1
    fi
    sleep 5
    echo
    echo 'all done, ready to run'
    echo

fi

# add fixed ip address: works on 1.7
#  container must run with --cap-add=NET_ADMIN
#
ip addr add 172.18.0.2 dev eth0

# "$@" is docker CMD, should be 'mysqld'
# then docker will keep the container running while mysqld is up
#
exec "$@"
