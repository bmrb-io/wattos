FROM alpine

COPY ./my.cnf /my.cnf
COPY ./startup.sh /startup.sh
COPY ./wattos_load.sql /wattos_load.sql

RUN addgroup -S mysql \
    && adduser -S mysql -G mysql \
    && apk update \
    && apk add --no-cache mysql mysql-client mc \
    && rm -f /var/cache/apk/* \
    && mv /my.cnf /etc/my.cnf \
    && mkdir -p /run/mysqld \
    && chown -R mysql:mysql /run/mysqld \
    && chmod -R 777 /run/mysqld

EXPOSE 3306

# startup.sh does mysql init, cleanup, and creates wattos db
#
ENTRYPOINT ["/startup.sh"]

#
#
CMD ["mysqld", "--user=mysql"]
#CMD ["sh"]
