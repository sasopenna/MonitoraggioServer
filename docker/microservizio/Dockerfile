FROM python:3.13.4

ADD run.py .
ADD leshan.py .
ADD influxdb.py .

RUN pip install requests
RUN pip install sseclient
RUN pip install influxdb-client

CMD ["python", "-u", "run.py"]