--
-- Loads CSVs from /wattos/dbfs into the wattos1 schema. This file is mounted
-- into /docker-entrypoint-initdb.d/ and runs automatically the first time the
-- mysql container initialises its data directory; no manual step is needed.
--
-- To re-run after the data dir already exists (e.g. CSVs changed):
--   docker exec -i -e MYSQL_PWD=PASSWORD wattos-mysql \
--       mysql -u root -D wattos1 < initdb/wattos_load.sql
-- Or wipe and re-init: docker compose down -v && docker compose up -d
--
use wattos1;
truncate table entry;
load data infile '/wattos/dbfs/entry.csv'
    replace into table entry
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (entry_id,bmrb_id,pdb_id,in_recoord,in_dress);
optimize table entry;
truncate table mrblock;
load data infile '/wattos/dbfs/mrblock.csv'
    replace into table mrblock
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (mrblock_id,mrfile_id,position,program,type,subtype,format,text_type,
     byte_count,item_count,date_modified,other_prop,dbfs_id,file_name,md5_sum);
optimize table mrblock;
truncate table mrfile;
load data infile '/wattos/dbfs/mrfile.csv'
    replace into table mrfile
    fields terminated by ','
    optionally enclosed by '"'
    lines terminated by '\n'
    (mrfile_id,entry_id,detail,pdb_id,date_modified);
optimize table mrfile;
