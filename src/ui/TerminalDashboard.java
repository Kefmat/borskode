package ui;

import models.Ticker;
import analytics.PriceHistoryBuffer;
import trading.Portfolio;
import java.util.List;
import java.util.Map;

/**
 * Ansvarlig for å tegne et sanntids-dashboard i terminalen med fargekoding.
 * Bruker ANSI-koder for å visualisere trender, momentum og porteføljestatus.
 */
public class TerminalDashboard {

    // ANSI Fargekoder for terminal-formatering
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";

    /**
     * Renser terminalskjermen og flytter markøren til øverste venstre hjørne.
     */
    public void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Tegner en formatert tabell med fargekodede markedsdata og porteføljestatus.
     */
    public void render(List<Ticker> tickers, Map<String, PriceHistoryBuffer> historyMap, Portfolio portfolio) {
        clearScreen();
        System.out.println(CYAN + "================================================================================" + RESET);
        System.out.println(BOLD + "                BØRSKODE ENGINE - PROFESSIONAL TRADING TERMINAL                " + RESET);
        System.out.println(CYAN + "================================================================================" + RESET);
        System.out.printf(BOLD + "%-10s | %-10s | %-10s | %-10s | %-10s | %-10s\n" + RESET, 
                          "SYMBOL", "PRIS", "ENDRING %", "SMA (14)", "RSI", "STATUS");
        System.out.println("--------------------------------------------------------------------------------");

        for (Ticker t : tickers) {
            String symbol = t.getSymbol();
            PriceHistoryBuffer buffer = historyMap.get(symbol);
            
            double sma = (buffer != null) ? buffer.calculateSMA() : 0.0;
            double rsi = (buffer != null) ? buffer.calculateRSI() : 50.0;
            
            String changeColor = (t.getChangePercent() >= 0) ? GREEN : RED;
            String rsiColor = (rsi >= 70) ? RED : (rsi <= 30 ? GREEN : RESET);
            String status = determineStatus(t.getLastPrice(), sma, rsi);
            String statusColor = getStatusColor(status);

            System.out.printf("%-10s | %-10.2f | " + changeColor + "%-10.2f" + RESET + " | %-10.2f | " + rsiColor + "%-10.2f" + RESET + " | " + statusColor + "%-10s" + RESET + "\n",
                              symbol, t.getLastPrice(), t.getChangePercent(), sma, rsi, status);
        }

        // --- PORTEFØLJEOVERSIKT ---
        double totalValue = portfolio.getTotalValue();
        double profit = totalValue - 100000.0;
        String profitColor = (profit >= 0) ? GREEN : RED;

        System.out.println(CYAN + "================================================================================" + RESET);
        System.out.println(BOLD + "                DIN SIMULERTE PORTEFØLJE (PAPER TRADING)                " + RESET);
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Kontanter:    %-15.2f NOK | Aksjeverdi:   %.2f NOK\n", 
                          portfolio.getCash(), (totalValue - portfolio.getCash()));
        System.out.printf("Totalverdi:   " + BOLD + "%-15.2f" + RESET + " NOK | Avkastning:   " + profitColor + "%.2f NOK" + RESET + "\n", 
                          totalValue, profit);
        System.out.println("--------------------------------------------------------------------------------");
        
        System.out.print(BOLD + "Beholdning: " + RESET);
        portfolio.getHoldings().forEach((sym, qty) -> {
            if (qty > 0) System.out.print(BLUE + sym + RESET + ": " + qty + " stk | ");
        });
        
        System.out.println("\n" + CYAN + "================================================================================" + RESET);
        System.out.println("Siste oppdatering: " + YELLOW + java.time.LocalTime.now().withNano(0) + RESET + " | Trykk Ctrl+C for stopp");
    }

    private String determineStatus(double price, double sma, double rsi) {
        if (rsi >= 70) return "OVERBOUGHT";
        if (rsi <= 30) return "OVERSOLD";
        if (price > sma) return "BULLISH";
        if (price < sma) return "BEARISH";
        return "NEUTRAL";
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "OVERBOUGHT": return RED;
            case "OVERSOLD": return GREEN;
            case "BULLISH": return GREEN;
            case "BEARISH": return RED;
            default: return RESET;
        }
    }
}