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
        MarketDataIngestor ingestor = new MarketDataIngestor();
        MarketMonitor monitor = new MarketMonitor();
        
        // Oppretter en logger som skriver til en lokal CSV-fil
        DataLogger logger = new DataLogger("oslo_bors_data.csv");

        System.out.println("Børskode Engine Online - Overvåker Oslo Børs...");
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

                // Poll-intervall satt til 15 sekunder for å balansere mellom 
                // behovet for ferske data og API-begrensninger (Rate Limiting).
                Thread.sleep(15000);
            }
        } catch (InterruptedException e) {
            System.err.println("Systemet ble manuelt avbrutt.");
            // Gjenoppretter avbruddsstatus i tråden 
            Thread.currentThread().interrupt();
        }
    }
}