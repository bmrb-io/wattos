set @@SESSION.SQL_LOG_BIN=0;
drop database if exists test;
delete from mysql.user WHERE User='';
create user if not exists 'wattos1'@'%' identified by '4I4KMS';
grant all privileges on *.* to 'wattos1'@'%';
create user if not exists 'wattos1'@'localhost' identified by '4I4KMS';
grant all privileges on *.* to 'wattos1'@'localhost';
create user if not exists 'wattos1'@'127.0.0.1' identified by '4I4KMS';
grant all privileges on *.* to 'wattos1'@'127.0.0.1';
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

