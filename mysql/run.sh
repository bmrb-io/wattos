#!/bin/sh

docker run -d \
    --restart=always \
    -p 3306:3306 \
    --user 17473:10144 \
    -e MYSQL_ROOT_PASSWORD=5up3r53kr37 \
    -v /projects/BMRB/public/wattos/wattos/initdb:/docker-entrypoint-initdb.d/ \
    -v /projects/BMRB/public/wattos/dbfs:/wattos/dbfs \
    -v /projects/BMRB/public/wattos/html:/wattos/html \
    --name wattos-mysql \
    mysql:5 --secure-file-priv=/wattos/dbfs

#on DB update:
#
#sudo docker exec wattos-mysql sh -c 'mysql -uroot -p5up3r53kr37 wattos1 < /wattos_load.sql'
#
