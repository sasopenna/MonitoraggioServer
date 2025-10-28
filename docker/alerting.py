# Non implementato al 100%, utilizzato solo per testing

import subprocess
import json

# Iscrizione al topic mosquitto per monitoraggio
cmd = ["mosquitto_sub", "-h", "test.mosquitto.org", "-t", "tesina-penna", "-q", "2"]

# Avvia il processo
with subprocess.Popen(cmd, stdout=subprocess.PIPE, text=True) as proc:
    print(f"Iscritto al topic con successo. In ascolto dei messaggi...")

    try:
        for line in proc.stdout:
            line = line.strip()
            try:
                msg = json.loads(line)
                alert = msg.get("commonLabels").get("alertname")
                status = msg.get("status")

                if status == "firing":
                    print("L'RTT ha superato il valore soglia")
                elif status == "resolved":
                    print("L'RTT è tornato al valore sotto soglia")
            except Exception as e:
                print(e)
    except KeyboardInterrupt:
        proc.terminate()