from influxdb_client import InfluxDBClient, Point, WritePrecision
from influxdb_client.client.write_api import SYNCHRONOUS

# Configurazione e funzioni influx
INFLUX_URL = "http://localhost:8086"
INFLUX_TOKEN = "secret_token"
INFLUX_ORG = "tesina"
INFLUX_BUCKET = "bucket"

influx_client = InfluxDBClient(url=INFLUX_URL, token=INFLUX_TOKEN, org=INFLUX_ORG, bucket=INFLUX_BUCKET)
write_api = influx_client.write_api(write_options=SYNCHRONOUS)

def salva_metrica(obj, metrica):
    valore = obj.get(metrica)
    timestamp = obj.get("timestamp")

    if valore is None or timestamp is None:
        return

    point = Point("serverstatus").field(metrica, float(valore)).time(timestamp, WritePrecision.MS)
    write_api.write(record=point, bucket=INFLUX_BUCKET, org=INFLUX_ORG)
