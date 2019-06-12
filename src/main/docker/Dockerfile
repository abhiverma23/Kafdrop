FROM java:8-jdk-alpine
MAINTAINER nextsphere

RUN chmod +x /kafdrop.sh && apk update && apk add --no-cache libc6-compat

ADD kafdrop.sh /
ADD kafdrop*jar /kafdrop/

RUN chmod +x /kafdrop.sh

ENTRYPOINT ["/kafdrop.sh"]