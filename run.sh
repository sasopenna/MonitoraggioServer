#!/bin/bash

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
chiudi_container() {
  log INFO "Chiusura container aperti..."
  sudo docker stop $(sudo docker ps -q)
  log SUCCESS "Tutti i container fermati."
}

esegui_maven() {
  log INFO "Esecuzione Maven..."
  cd leshan
  sudo mvn clean install -DskipTests -Dcheckstyle.skip
  log SUCCESS "Build Maven completato."
}

copia_artifacts() {
  log INFO "Copia dei JAR client/server nella cartella Docker..."
  cp leshan-demo-client/target/leshan-demo-client-2.0.0-SNAPSHOT-jar-with-dependencies.jar ../docker/client/
  cp leshan-demo-server/target/leshan-demo-server-2.0.0-SNAPSHOT-jar-with-dependencies.jar ../docker/server/
  log SUCCESS "JAR copiati correttamente."
}

build_immagini() {
  log INFO "Build delle immagini Docker..."
  cd ../docker
  sudo docker build --rm -t leshan-client ./client && log SUCCESS "Client buildato."
  sudo docker build --rm -t leshan-server ./server && log SUCCESS "Server buildato."
  sudo docker build --rm -t microservizio ./microservizio && log SUCCESS "Microservizio buildato."
}

prune_immagini() {
  log INFO "Pulizia immagini Docker non utilizzate..."
  sudo docker rmi -f $(sudo docker images --filter "dangling=true" -q)
  log SUCCESS "Immagini obsolete rimosse."
}

avvia_container() {
  local volumi="$(pwd)/volumi/"
  
  log INFO "Avvio InfluxDB..."
  sudo docker run -d -p 8086:8086 --network="host" -v "$volumi/influxdb:/var/lib/influxdb2" influxdb:2
  log SUCCESS "InfluxDB avviato."

  log INFO "Avvio Grafana..."
  sudo docker run -d -p 3000:3000 --network="host" -v "$volumi/grafana:/var/lib/grafana" grafana/grafana
  log SUCCESS "Grafana avviato."

  log INFO "Avvio Server Leshan..."
  sudo docker run -d --network="host" leshan-server
  log SUCCESS "Server Leshan avviato."

  log INFO "Avvio Client Leshan..."
  sudo docker run -d --network="host" leshan-client
  log SUCCESS "Client Leshan avviato."

  log INFO "Avvio microservizio..."
  sudo docker run --network="host" microservizio
}

# === ESECUZIONE ===
clear
chiudi_container
esegui_maven
copia_artifacts
build_immagini
prune_immagini
avvia_container
