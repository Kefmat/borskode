import ingestor.MarketDataIngestor;
import analytics.MarketMonitor;
import analytics.DataLogger;
import models.Ticker;
import java.util.List;

/**
 * Oppstartspunktet for Børskode Engine.
 * Koordinerer flyten mellom datainnhenting, teknisk analyse og 
 * persistent lagring i en kontrollert loop.
 */
public class Main {
    public static void main(String[] args) {
        // Initialisering av kjernekomponenter
        // MarketDataIngestor bruker nå internt et trådbasseng for parallell henting
        MarketDataIngestor ingestor = new MarketDataIngestor();
        MarketMonitor monitor = new MarketMonitor();
        
        // Oppretter en logger som skriver til en lokal CSV-fil
        DataLogger logger = new DataLogger("oslo_bors_data.csv");

        System.out.println("Børskode Engine Online - Overvåker Oslo Børs i parallell...");
        System.out.println("Logger data til oslo_bors_data.csv");

        try {
            // Hovedloop for sanntidsovervåking
            while (true) {
                List<Ticker> updates = ingestor.fetchLatestPrices();
                
                for (Ticker ticker : updates) {
                    System.out.println(ticker);
                    
                    monitor.processUpdate(ticker);
                    
                    logger.logTicker(ticker);
                }

                // Poll-intervall på 15 sekunder for å balansere mellom 
                // sanntid og respekt for API-begrensninger (Rate Limiting).
                Thread.sleep(15000);
            }
        } catch (InterruptedException e) {
            System.err.println("Systemet ble manuelt avbrutt.");
            Thread.currentThread().interrupt();
        } finally {
            // Sørger for at trådbassenget i ingestoren stenges ned korrekt
            // slik at JVM-en kan avslutte prosessen helt.
            System.out.println("Frigjør ressurser og avslutter...");
            ingestor.shutdown();
        }
    }
}