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

-- Indexes supporting the join/filter columns used by MRGridServlet's queries.
-- Without these, every page does a full scan of mrblock (~270K rows). With
-- them, the typical pdb-filtered page drops from ~700ms to ~10ms.
-- (MySQL 5.7 doesn't support CREATE INDEX IF NOT EXISTS, so this script
-- assumes a fresh DB. To add the indexes to an existing DB without wiping,
-- run them via `ALTER TABLE` once and ignore "duplicate key name" errors,
-- or guard each with information_schema lookups.)
create index entry_pdb_id_idx         on entry(pdb_id);
create index entry_bmrb_id_idx        on entry(bmrb_id);
create index mrfile_entry_id_idx      on mrfile(entry_id);
create index mrblock_mrfile_id_idx    on mrblock(mrfile_id);
create index mrblock_text_type_idx    on mrblock(text_type);
create index mrblock_type_subtype_idx on mrblock(type, subtype);
