FROM openjdk:17-alpine

RUN apk add iperf3

# RUN apk add python3 py3-pip
# ADD misure.py /

ADD leshan-demo-client-2.0.0-SNAPSHOT-jar-with-dependencies.jar /

EXPOSE 3000 5000 5683 5684 8080 8086

CMD java -jar leshan-demo-client-2.0.0-SNAPSHOT-jar-with-dependencies.jar