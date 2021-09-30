FROM csf-docker-delivered.repo.lab.pl.alcatel-lucent.com/httpd:1.2.4-26

WORKDIR /var/www/obr
COPY repository .
COPY conf.d /etc/httpd/ext_conf.d

RUN chmod -R 755 .