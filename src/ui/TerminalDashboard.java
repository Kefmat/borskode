package ui;

import models.Ticker;
import analytics.PriceHistoryBuffer;
import java.util.List;
import java.util.Map;

/**
 * Ansvarlig for å tegne et sanntids-dashboard i terminalen.
 * Bruker ANSI-koder for å oppdatere skjermen uten å rulle teksten.
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
     * Tegner en formatert tabell med markedsdata og tekniske indikatorer.
     * @param tickers Listen over aktive aksjer.
     * @param historyMap Map som inneholder historikkbuffere for beregning av RSI/SMA.
     */
    public void render(List<Ticker> tickers, Map<String, PriceHistoryBuffer> historyMap) {
        clearScreen();
        System.out.println("================================================================================");
        System.out.println("                BØRSKODE ENGINE - SANNTIDSDASHBOARD (OSLO BØRS)                ");
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
        System.out.println("================================================================================");
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