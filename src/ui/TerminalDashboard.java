package ui;

import models.Ticker;
import analytics.PriceHistoryBuffer;
import trading.Portfolio;
import java.util.List;
import java.util.Map;

/**
 * Ansvarlig for å tegne et sanntids-dashboard i terminalen.
 * Bruker ANSI-koder for å oppdatere skjermen uten å rulle teksten.
 * Viser nå også en oversikt over den simulerte porteføljen (Paper Trading).
 */
public class TerminalDashboard {

    /**
     * Renser terminalskjermen og flytter markøren til øverste venstre hjørne.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Tegner en formatert tabell med markedsdata, tekniske indikatorer og porteføljestatus.
     * @param tickers Listen over aktive aksjer.
     * @param historyMap Map som inneholder historikkbuffere for beregning av RSI/SMA.
     * @param portfolio Den simulerte porteføljen som viser nåværende beholdning og saldo.
     */
    public void render(List<Ticker> tickers, Map<String, PriceHistoryBuffer> historyMap, Portfolio portfolio) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("                BØRSKODE ENGINE - LIVE TRADING DASHBOARD                       ");
        System.out.println("================================================================================");
        System.out.printf("%-10s | %-10s | %-10s | %-10s | %-10s | %-10s\n", 
                          "SYMBOL", "PRIS", "ENDRING %", "SMA (14)", "RSI", "STATUS");
        System.out.println("--------------------------------------------------------------------------------");

        for (Ticker t : tickers) {
            String symbol = t.getSymbol();
            PriceHistoryBuffer buffer = historyMap.get(symbol);
            
            double sma = (buffer != null) ? buffer.calculateSMA() : 0.0;
            double rsi = (buffer != null) ? buffer.calculateRSI() : 50.0;
            String status = determineStatus(t.getLastPrice(), sma, rsi);

            System.out.printf("%-10s | %-10.2f | %-10.2f | %-10.2f | %-10.2f | %-10s\n",
                              symbol, t.getLastPrice(), t.getChangePercent(), sma, rsi, status);
        }

        // --- PORTEFØLJEOVERSIKT (NY SEKSJON) ---
        double totalValue = portfolio.getTotalValue();
        double cash = portfolio.getCash();
        double holdingsValue = totalValue - cash;
        double profit = totalValue - 100000.0; // Profit basert på initial 100k

        System.out.println("================================================================================");
        System.out.println("                DIN SIMULERTE PORTEFØLJE (PAPER TRADING)                ");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Kontanter:    %-15.2f NOK | Aksjeverdi:   %.2f NOK\n", cash, holdingsValue);
        System.out.printf("Totalverdi:   %-15.2f NOK | Avkastning:   %.2f NOK\n", totalValue, profit);
        System.out.println("--------------------------------------------------------------------------------");
        
        System.out.print("Beholdning: ");
        Map<String, Integer> holdings = portfolio.getHoldings();
        boolean hasHoldings = false;
        
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.print(entry.getKey() + ": " + entry.getValue() + " stk | ");
                hasHoldings = true;
            }
        }
        
        if (!hasHoldings) {
            System.out.print("Ingen aktive posisjoner.");
        }

        System.out.println("\n================================================================================");
        System.out.println("Siste oppdatering: " + java.time.LocalTime.now().withNano(0));
        System.out.println("Trykk Ctrl+C for å avslutte.");
    }

    private String determineStatus(double price, double sma, double rsi) {
        if (rsi >= 70) return "OVERBOUGHT";
        if (rsi <= 30) return "OVERSOLD";
        if (price > sma) return "BULLISH";
        if (price < sma) return "BEARISH";
        return "NEUTRAL";
    }
}