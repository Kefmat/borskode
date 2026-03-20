package analytics;

import models.Ticker;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;

/**
 * DataLogger har ansvaret for persistent lagring av markedsdata.
 * Skriver alle mottatte tickere til en CSV-fil for senere analyse.
 */
public class DataLogger {
    private final String fileName;

    /**
     * @param fileName Navnet på CSV-filen som skal opprettes (f.eks. "oslo_bors_data.csv").
     */
    public DataLogger(String fileName) {
        this.fileName = fileName;
        initializeFile();
    }

    /**
     * Oppretter filen og skriver header-linjen dersom filen ikke eksisterer fra før.
     */
    private void initializeFile() {
        File file = new File(fileName);
        if (!file.exists()) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.println("Timestamp,Symbol,Price,ChangePercent");
            } catch (IOException e) {
                System.err.println("Kunne ikke initialisere loggfil: " + e.getMessage());
            }
        }
    }

    /**
     * Logger en enkelt ticker-oppdatering til CSV-filen.
     * @param ticker Objektet som inneholder dataene som skal lagres.
     */
    public void logTicker(Ticker ticker) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            writer.printf("%s,%s,%.2f,%.2f\n",
                ticker.getTimestamp(),
                ticker.getSymbol(),
                ticker.getLastPrice(),
                ticker.getChangePercent());
        } catch (IOException e) {
            System.err.println("Feil ved skriving til loggfil: " + e.getMessage());
        }
    }
}