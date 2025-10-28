import time
import json
import leshan
import influxdb

# Avvio misurazioni tramite observe
print(f"[----] Avvio OBSERVE delle misurazioni ...")
time.sleep(10)

for risorsa in [0, 1, 2]: # invoco le risorse per iniziare le misurazioni
    leshan.esegui_risorsa(risorsa)

for risorsa in [3, 4, 5]: # osservo le risorse
    leshan.osserva_risorsa(risorsa)

# observe delle risorse attraverso event-stream
messaggi = leshan.messaggi_observer()
try:
    for messaggio in messaggi:
        if messaggio.event != "NOTIFICATION":
            continue
            
        val = json.loads(messaggio.data).get("val")
        id_risorsa = val.get("id")
        payload = json.loads(val.get("value"))
        
        metrica = None
        if id_risorsa == 3: metrica = "rtt"
        elif id_risorsa == 4: metrica = "throughput"
        elif id_risorsa == 5: metrica = "loss"
            
        if metrica is not None:
            influxdb.salva_metrica(payload, metrica)
            print(f"[ID {id_risorsa}] Metrica: {metrica} - Payload: {payload}")
except KeyboardInterrupt:
    print(f"[----] Arresto dell'OBSERVE delle misurazioni ...")

for risorsa in [6, 7, 8]: # arresto le misurazioni
    leshan.esegui_risorsa(risorsa)