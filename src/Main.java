import ingestor.MarketDataIngestor;
import analytics.MarketMonitor;
import analytics.DataLogger;
import ui.TerminalDashboard;
import trading.Portfolio;
import models.Ticker;
import java.util.List;

/**
 * Oppstartspunktet for Børskode Engine.
 * Koordinerer flyten mellom datainnhenting, teknisk analyse, 
 * persistent lagring, simulerte handelsordre og sanntidsvisning.
 */
public class Main {
    public static void main(String[] args) {
        // Initialisering av kjernekomponenter
        MarketDataIngestor ingestor = new MarketDataIngestor();
        MarketMonitor monitor = new MarketMonitor();
        
        // Initialiserer porteføljen for Paper Trading med 100 000 NOK
        Portfolio myPortfolio = new Portfolio(100000.0);
        
        // Oppretter logger for historisk arkivering
        DataLogger logger = new DataLogger("oslo_bors_data.csv");
        
        // Initialiserer grensesnittet
        TerminalDashboard dashboard = new TerminalDashboard();

        try {
            // Hovedloop for sanntidsovervåking
            while (true) {
                // Henter ferske kurser asynkront og i parallell
                List<Ticker> updates = ingestor.fetchLatestPrices();
                
                for (Ticker ticker : updates) {
                    // Oppdater interne statistikk-buffere (SMA, Volatilitet, RSI) 
                    // og utfør simulerte handler basert på signaler.
                    monitor.processUpdate(ticker, myPortfolio);
                    
                    // Arkiver dataen i CSV-filen
                    logger.logTicker(ticker);
                }

                // Oppdater det visuelle dashbordet i terminalen
                // Dette gir en ryddig oversikt over alle aksjer på ett sted.
                // Vi sender nå med myPortfolio for å vise beholdning og avkastning.
                dashboard.render(updates, monitor.getHistoryMap(), myPortfolio);

                // Poll-intervall på 15 sekunder for å balansere mellom 
                // sanntid og respekt for API-begrensninger (Rate Limiting).
                Thread.sleep(15000);
            }
        } catch (InterruptedException e) {
            System.err.println("\nSystemet ble manuelt avbrutt.");
            Thread.currentThread().interrupt();
        } finally {
            // Sørger for at trådbassenget i ingestoren stenges ned korrekt
            // slik at JVM-en kan avslutte prosessen helt.
            System.out.println("Frigjør ressurser og avslutter...");
            ingestor.shutdown();
        }
    }
}