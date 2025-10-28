package org.eclipse.leshan.demo.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import org.json.JSONObject;

// Definisco questa classe per tutte le metriche
public abstract class MetricCollector {
    private final List<String> comando;
    private JSONObject ultimaMisura = new JSONObject();
    private Thread thread;
    private Process process;

    // Definizione del comando nel costruttore
    public MetricCollector(List<String> comando) {
        this.comando = comando;
    }

    // Funzione per inizializzazione del thread per calcolare le metriche
    public void start() {
        stop();

        thread = new Thread(() -> {
            try {
                process = new ProcessBuilder(comando).start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null && !Thread.currentThread().isInterrupted()) {
                    if (line.contains("- - - - -"))
                        break; // Interrompo il ciclo per evitare di calcolare il summary
                    try {
                        esecuzione(line.split("\\s+"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        thread.start();
    }

    // Stoppa il processo
    public void stop() {
        ultimaMisura = new JSONObject(); // Reset ultima misura

        if (thread != null && thread.isAlive()) {
            thread.interrupt();
            if (process != null) {
                process.destroy();
            }
        }
    }

    // Salvataggio metrica JSON (da effettuare nel metodo "esecuzione")
    public void save(String key, double value) {
        ultimaMisura.put(key, value);
        ultimaMisura.put("timestamp", System.currentTimeMillis());
    }

    // Restituisce l'ultimo elemento misurato
    public String last() {
        return ultimaMisura.toString();
    }

    // Metto questo metodo astratto così forzo l'override nelle singole classi per ogni metrica
    protected abstract void esecuzione(String[] split) throws Exception;
}
