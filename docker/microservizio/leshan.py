import json
import requests
from sseclient import SSEClient

# Configurazione e funzioni Leshan
LESHAN_SERVER = "http://localhost:8080/api"
LESHAN_CLIENT = "saso"

def esegui_risorsa(risorsa):
    url = f"{LESHAN_SERVER}/clients/{LESHAN_CLIENT}/9000/0/{risorsa}"
    requests.post(url)
    
def osserva_risorsa(risorsa):
    url = f"{LESHAN_SERVER}/clients/{LESHAN_CLIENT}/9000/0/{risorsa}/observe"
    requests.post(url)

def leggi_risorsa(risorsa):
    url = f"{LESHAN_SERVER}/clients/{LESHAN_CLIENT}/9000/0/{risorsa}"
    return json.loads(requests.get(url).json()["content"]["value"])

def messaggi_observer():
    return SSEClient(f"{LESHAN_SERVER}/event?ep={LESHAN_CLIENT}")