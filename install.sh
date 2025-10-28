#!/bin/bash

# === CONFIGURAZIONE ===
USERNAME="admin"
PASSWORD="admin1234"
AUTH_HEADER="Authorization: Basic $(echo -n $USERNAME:$PASSWORD | base64)"

ORG="tesina"
BUCKET="bucket"
TOKEN="secret_token"

VOLUMI="$(pwd)/docker/volumi"

# === LOGGING ===
log() {
  local COLOR_RESET="\033[0m"
  local COLOR_INFO="\033[1;34m"
  local COLOR_SUCCESS="\033[1;32m"
  local COLOR_WARN="\033[1;33m"
  local COLOR_ERROR="\033[1;31m"
  local LEVEL=$1
  shift
  local MESSAGE="$@"
  local TIMESTAMP=$(date +"%Y-%m-%d %H:%M:%S")

  case "$LEVEL" in
    INFO)    echo -e "${COLOR_INFO}[$TIMESTAMP] [INFO]${COLOR_RESET}    $MESSAGE" ;;
    SUCCESS) echo -e "${COLOR_SUCCESS}[$TIMESTAMP] [SUCCESS]${COLOR_RESET} $MESSAGE" ;;
    WARN)    echo -e "${COLOR_WARN}[$TIMESTAMP] [WARNING]${COLOR_RESET} $MESSAGE" ;;
	ERROR)   echo -e "${COLOR_ERROR}[$TIMESTAMP] [ERROR]${COLOR_RESET}   $MESSAGE" ;;
    *)       echo -e "[$TIMESTAMP] [LOG]      $MESSAGE" ;;
  esac
}

# === FUNZIONI ===
clona_repository() {
  log INFO "Inizio clonazione della repository Leshan..."
  git clone https://github.com/eclipse-leshan/leshan.git

  log INFO "Inizio copia dei file da 'leshan-mod' a 'leshan'..."
  sudo cp -r leshan-mod/* leshan/ 2>/dev/null
  sudo cp -r leshan-mod/.* leshan/ 2>/dev/null
  log SUCCESS "File copiati correttamente."

  log INFO "Eliminazione della cartella 'leshan-mod'..."
  sudo rm -rf leshan-mod
  log SUCCESS "Cartella rimossa con successo."
}


pulizia_docker() {
  log INFO "Pulizia ambienti Docker..."
  sudo docker stop $(sudo docker ps -q) && log SUCCESS "Tutti i container fermati."
  sudo docker rm influxdb grafana &>/dev/null && log SUCCESS "Container InfluxDB e Grafana rimossi."
}

prepara_volumi() {
  log INFO "Preparazione volumi..."
  sudo rm -rf "$VOLUMI"
  sudo mkdir -p "$VOLUMI/grafana" "$VOLUMI/influxdb"
  sudo chown -R 472:472 "$VOLUMI"
  sudo chmod -R 777 "$VOLUMI"
  log SUCCESS "Volumi pronti."
}

avvia_influxdb() {
  log INFO "Avvio InfluxDB..."
  sudo docker run -d -p 8086:8086 --name="influxdb" --network="host" -v "$VOLUMI/influxdb:/var/lib/influxdb2" influxdb:2
  sleep 3
  log INFO "Setup InfluxDB..."
  sudo docker exec -it influxdb influx setup --username "$USERNAME" --password "$PASSWORD" --org "$ORG" --bucket "$BUCKET" --token "$TOKEN" --force
  log SUCCESS "InfluxDB configurato."
}

avvia_grafana() {
  log INFO "Avvio Grafana..."
  sudo docker run -d -p 3000:3000 --name="grafana" --network="host" \
    -e GF_SECURITY_ADMIN_USER="$USERNAME" \
    -e GF_SECURITY_ADMIN_PASSWORD="$PASSWORD" \
    -v "$VOLUMI/grafana:/var/lib/grafana" grafana/grafana
  sleep 10
  log SUCCESS "Grafana avviato."
}

crea_datasource() {
  log INFO "Creazione datasource InfluxDB in Grafana..."
  DATASOURCE_UID=$(sudo curl -s -X POST http://localhost:3000/api/datasources \
    -H "Content-Type: application/json" \
    -H "$AUTH_HEADER" \
    -d '{
      "name": "InfluxDB_Flux",
      "type": "influxdb",
      "access": "proxy",
      "url": "http://localhost:8086",
      "jsonData": {
        "version": "Flux",
        "organization": "'"$ORG"'",
        "defaultBucket": "'"$BUCKET"'"
      },
      "secureJsonData": {
        "token": "'"$TOKEN"'"
      },
      "isDefault": true
    }' | grep -o '"uid":"[^"]*"' | cut -d':' -f2 | tr -d '"')

  if [ -n "$DATASOURCE_UID" ]; then
    log SUCCESS "Datasource creato con UID: $DATASOURCE_UID"
  else
    log ERROR "Creazione datasource fallita."
    exit 1
  fi
}

importa_dashboard() {
  log INFO "Importazione dashboard..."
  DASHBOARD_JSON=$(sed "s/__DATASOURCE_UID__/$DATASOURCE_UID/g" docker/microservizio/dashboard.json)
  
  RESPONSE=$(curl -s -X POST http://localhost:3000/api/dashboards/db \
    -H "Content-Type: application/json" \
    -H "$AUTH_HEADER" \
    -d '{
  	  "dashboard": '"$DASHBOARD_JSON"',
  	  "overwrite": true
    }')
  
  if echo "$RESPONSE" | grep -q 'success'; then
    log SUCCESS "Dashboard disponibile al link http://localhost:3000/dashboards."
	log WARN "Login con username: $USERNAME e password: $PASSWORD"
  else
    log ERROR "Importazione fallita: $RESPONSE"
    exit 1
  fi
}

# === ESECUZIONE ===
clear
clona_repository
pulizia_docker
prepara_volumi
avvia_influxdb
avvia_grafana
crea_datasource
importa_dashboard
