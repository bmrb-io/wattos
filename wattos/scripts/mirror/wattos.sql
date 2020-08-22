use wattos1;
create table if not exists entry (
    entry_id   integer primary key,
    bmrb_id    integer,
    pdb_id     char(4),
    in_recoord bit,
    in_dress   bit
);
truncate table entry;
load data infile '/website/admin/wattos/entry.csv'
    replace into table entry
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (entry_id,bmrb_id,pdb_id);

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
truncate table mrblock;
load data infile '/website/admin/wattos/mrblock.csv'
    replace into table mrblock
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (mrblock_id,mrfile_id,position,program,type,subtype,format,text_type,
     byte_count,item_count,date_modified,other_prop,dbfs_id,file_name,md5_sum);

create table if not exists mrfile (
    mrfile_id     integer primary key,
    entry_id      integer,
    detail        varchar(255),
    pdb_id        char(4),
    date_modified date
);
truncate table mrfile;
load data infile '/website/admin/wattos/mrfile.csv'
    replace into table mrfile
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (mrfile_id,entry_id,detail,pdb_id,date_modified);
