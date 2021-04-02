FROM maven:3.6.3-jdk-11

RUN apt-get update && DEBIAN_FRONTEND=noninteractive apt-get -y install python3 \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/src
COPY . .

RUN mvn compile
EXPOSE 8000/tcp

ENTRYPOINT ["./run.sh"]