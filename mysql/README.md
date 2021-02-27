# mysql container for wattos

Simple deployment: run tomcat container with `--link <name>`

## run

```
docker run -d \
    --restart=always \
    -p 3306:3306 \
    -e MYSQL_ROOT_PASSWORD=<supersekret> \
    -v <initdb>:/docker-entrypoint-initdb.d/ \
    -v <dbfs>:/wattos/dbfs \
    -v <html>:/wattos/html \
    --name <name> \
    mysql:5 --secure-file-priv=/wattos/dbfs

```

on DB update:

```
sudo docker exec <name> sh -c 'mysql -uroot -p<supersekret> wattos1 < /docker-entrypoint-initdb.d/wattos_load.sql'
```
  * pin to mysql v.5 becasue newer versions (and mariadb) require updating JDBC JAR in the wattos WAR file.
  * <initdb>: put `01_create_db.sql` and `wattos_load.sql` there, they'll be execuetd on startup.
  * <dbfs>: is where the CSV files are loaded from by `wattos_load.sql` and `--secure-file-priv=/wattos/dbfs` is needed to load them.
