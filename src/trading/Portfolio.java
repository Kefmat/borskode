package trading;

import java.util.HashMap;
import java.util.Map;

/**
 * En simulator for aksjehandel (Paper Trading).
 * Holder styr på kontanter, aksjebeholdning og beregner totalformue
 * basert på sist kjente markedskurser.
 */
public class Portfolio {
    private double cash;
    private final Map<String, Integer> holdings = new HashMap<>();
    private final Map<String, Double> lastPrices = new HashMap<>();

    /**
     * Initialiserer porteføljen med en startkapital.
     * @param startCash Beløpet du starter med (f.eks. 100000.0).
     */
    public Portfolio(double startCash) {
        this.cash = startCash;
    }

    /**
     * Gjennomfører et fiktivt kjøp av aksjer.
     */
    public void buy(String symbol, double price, int quantity) {
        double cost = price * quantity;
        if (cost <= cash) {
            cash -= cost;
            holdings.put(symbol, holdings.getOrDefault(symbol, 0) + quantity);
            lastPrices.put(symbol, price);
            System.out.printf("[TRADE] KJØPT %d stk %s til pris %.2f\n", quantity, symbol, price);
        }
    }

    /**
     * Gjennomfører et fiktivt salg av aksjer.
     */
    public void sell(String symbol, double price) {
        int quantity = holdings.getOrDefault(symbol, 0);
        if (quantity > 0) {
            double revenue = price * quantity;
            cash += revenue;
            holdings.put(symbol, 0);
            lastPrices.put(symbol, price);
            System.out.printf("[TRADE] SOLGT alle %s til pris %.2f. Gevinst/tap realisert.\n", symbol, price);
        }
    }

    public void updateLastPrice(String symbol, double price) {
        lastPrices.put(symbol, price);
    }

    /**
     * Beregner totalverdien av porteføljen (Cash + Markedsverdi av aksjer).
     */
    public double getTotalValue() {
        double holdingsValue = 0;
        for (Map.Entry<String, Integer> entry : holdings.entrySet()) {
            holdingsValue += entry.getValue() * lastPrices.getOrDefault(entry.getKey(), 0.0);
        }
        return cash + holdingsValue;
    }

    public double getCash() { return cash; }
    
    public Map<String, Integer> getHoldings() { return holdings; }
}