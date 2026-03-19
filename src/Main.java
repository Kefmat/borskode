import ingestor.MarketDataIngestor;
import analytics.MarketMonitor;
import models.Ticker;
import java.util.List;

/**
 * Oppstartspunktet for Børskode Engine.
 * Koordinerer flyten mellom datainnhenting og analyse i en kontrollert loop.
 */
public class Main {
    public static void main(String[] args) {
        MarketDataIngestor ingestor = new MarketDataIngestor();
        MarketMonitor monitor = new MarketMonitor();

        System.out.println("Børskode Engine Online - Overvåker Oslo Børs...");

        try {
            // Hovedloop for sanntidsovervåking
            while (true) {
                List<Ticker> updates = ingestor.fetchLatestPrices();
                
                for (Ticker ticker : updates) {
                    System.out.println(ticker);
                    monitor.processUpdate(ticker);
                }

                // Poll-intervall satt til 15 sekunder for å balansere mellom 
                // sanntid og API-kvoter (Rate Limiting).
                Thread.sleep(15000);
            }
        } catch (InterruptedException e) {
            System.err.println("Systemet ble manuelt avbrutt.");
            Thread.currentThread().interrupt();
        }
    }
}