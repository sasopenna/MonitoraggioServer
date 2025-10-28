import time
import leshan
import influxdb

# Campagna di misurazioni
PERIODO = 1
DURATA = 600
NUM_CICLI = DURATA // PERIODO 

print(f"[----]\tAvvio {NUM_CICLI} misurazioni con periodicità {PERIODO}s ...")
time.sleep(10)

for risorsa in [0, 1, 2]: # invoco le risorse per iniziare le misurazioni
    leshan.esegui_risorsa(risorsa)

for i in range(NUM_CICLI): # print dell'ultima misurazione
    rtt = leshan.leggi_risorsa(3)
    thr = leshan.leggi_risorsa(4)
    loss = leshan.leggi_risorsa(5)

    print(f"[{i+1}°]\tRTT: {rtt.get('rtt', 'N/A')}\tThr: {thr.get('throughput', 'N/A')}\tLoss: {loss.get('loss', 'N/A')}")

    influxdb.salva_metrica(rtt, "rtt")
    influxdb.salva_metrica(thr, "throughput")
    influxdb.salva_metrica(loss, "loss")

    time.sleep(PERIODO)

print("[----]\tArresto misurazioni...")
for risorsa in [6, 7, 8]: # invoco le risorse per stoppare le misurazioni
    leshan.esegui_risorsa(risorsa)