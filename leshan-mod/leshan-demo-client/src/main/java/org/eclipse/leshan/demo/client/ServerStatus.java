package org.eclipse.leshan.demo.client;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerStatus {
    private static final Logger LOG = LoggerFactory.getLogger(ServerStatus.class);

    private static final String HOST = "iperf3.moji.fr"; // Preso qui https://iperf.fr/iperf-servers.php
    private static final String PORT = "5210";

    // Setup delle variabili
    private static final int PERIODO = 1; // 1 secondo
    private static final int DURATA = 600; // 10 minuti
    private static final int TENTATIVI = (int) (DURATA / PERIODO);

    // Setup dei calcolatori delle metriche
    public static final RTTCollector RTT = new RTTCollector();
    public static final ThroughputCollector Throughput = new ThroughputCollector();
    public static final LossCollector Loss = new LossCollector();

    // Calcolatore di RTT
    static class RTTCollector extends MetricCollector {
        public RTTCollector() {
            // Utilizzo il comando ping per ottenere l'RTT
            // Esempio comando: ping -i 1 -c 600 iperf3.moji.fr
            super(Arrays.asList("ping", "-i", String.valueOf(PERIODO), "-c", String.valueOf(TENTATIVI), HOST));
        }

        @Override
        protected void esecuzione(String[] split) {
            // Esempio riga: 64 bytes from 45.147.210.189: seq=3 ttl=50 time=75.827 ms

            String time = split[6]; // es: time=75.827
            if (time.startsWith("time")) {
                double rtt = Double.parseDouble(time.split("=")[1]); // es: 75.827
                save("rtt", rtt);
            }
        }
    }

    // Calcolatore di Throughput
    static class ThroughputCollector extends MetricCollector {
        public ThroughputCollector() {
            // Utilizzo iperf3 per misurare il throughput verso l'host in modalità TCP
            // Esempio comando: iperf3 -c iperf3.moji.fr -R -p 5210 -i 1 -t 600
            // con -c definisco l'host
            // con -R faccio il test verso il receiver
            // con -p setto la porta definita dal server
            // con -i imposto il periodo tra un pacchetto e un altro
            // con -t definisco la durata della cattura
            // con --forceflush forzo il flush di ogni linea dell'output iperf
            super(Arrays.asList("iperf3", "-c", HOST, "-R", "-p", PORT, "-i", String.valueOf(PERIODO), "-t",
                    String.valueOf(DURATA), "--forceflush"));
        }

        @Override
        protected void esecuzione(String[] split) {
            // [ ID] Interval Transfer Bitrate
            // Esempio riga: [ 5] 0.00-1.00 sec 5.38 MBytes 45.0 Mbits/sec

            if (split[3].equals("sec")) { // Faccio il check se contiene la parola sec nella linea
                double mbps = Double.parseDouble(split[6]); // es: 45.0
                save("throughput", mbps);
            }
        }
    }

    // Calcolatore di Loss
    static class LossCollector extends MetricCollector {
        public LossCollector() {
            // Utilizzo iperf3 per misurare la loss verso l'host in modalità UDP
            // Esempio comando: iperf3 -c iperf3.moji.fr -R -u -p 5210 -i 1 -t 600
            // con -c definisco l'host
            // con -R faccio il test verso il receiver
            // con -u faccio modalità UDP
            // con -p setto la porta definita dal server
            // con -i imposto il periodo tra un pacchetto e un altro
            // con -t definisco la durata della cattura
            // con --forceflush forzo il flush di ogni linea dell'output iperf
            super(Arrays.asList("iperf3", "-c", HOST, "-R", "-u", "-p", "5211", "-i", String.valueOf(PERIODO), "-t",
                    String.valueOf(DURATA), "--forceflush"));
        }

        @Override
        protected void esecuzione(String[] split) {
            // [ ID] Interval Transfer Bitrate Jitter Lost/Total Datagrams
            // Esempio riga: [ 5] 0.00-1.01 sec 125 KBytes 1.02 Mbits/sec 2.487 ms 0/94 (0%)

            if (split[3].equals("sec")) { // Faccio il check se contiene la parola sec nella linea
                String[] loss = split[10].split("/"); // es ["0", "94"]

                // Anche se la percentuale dei pacchetti persi è presente nella riga la ricalcolo a mano
                int persi = Integer.parseInt(loss[0]);
                int inviati = Integer.parseInt(loss[1]);
                double percentuale = 0.0;
                if (inviati != 0) { // per evitare in NaN
                    percentuale = (persi * 100.0) / inviati;
                }

                save("loss", percentuale);
            }
        }
    }
}
