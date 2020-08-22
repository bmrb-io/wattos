mysql container for wattos

Need fixed ip for tomcat container to talk to us.

run:

sudo docker run -d \
    --restart=always \
    -p 3306:3306 \
    -v /websites/wattos/dbfs:/wattos/dbfs \
    -v /websites/wattos/html:/wattos/html \
    --name <name> \
    --cap-add=NET_ADMIN \
    <image>

On centos 6 (docker 1.7) "--ip 172.18.0.2" doesn't work
so it is set inside the entrypoint script instead. That
requires NET_ADMIN capability.

On newer docker you could just run with " --network <net> --ip <addr>" 
but then you'll need to also edit startup.sh and remove "ip addr add".

on DB update:

sudo docker exec <name> sh -c 'mysql -uroot -p5up3r53kr37 wattos1 < /wattos_load.sql'
