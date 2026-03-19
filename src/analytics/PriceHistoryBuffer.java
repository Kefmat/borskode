package analytics;

import java.util.LinkedList;
import java.util.Queue;

/**
 * En trådsikker-lignende sirkulær buffer for lagring av historiske prispunkter.
 * Brukes for å beregne statistiske indikatorer over et glidende vindu.
 */
public class PriceHistoryBuffer {
    private final int maxSize;
    private final Queue<Double> history = new LinkedList<>();

    /**
     * @param maxSize Maksimalt antall datapunkter som skal beholdes i minnet.
     */
    public PriceHistoryBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Legger til en ny pris. Fjerner den eldste verdien hvis bufferen er full.
     */
    public void addPrice(double price) {
        if (history.size() >= maxSize) {
            history.poll();
        }
        history.add(price);
    }

    /**
     * Beregner Simple Moving Average (SMA) for de lagrede verdiene.
     * * @return Gjennomsnittspris over vinduet, eller 0.0 hvis tomt.
     */
    public double calculateSMA() {
        if (history.isEmpty()) return 0.0;
        double sum = 0;
        for (double price : history) {
            sum += price;
        }
        return sum / history.size();
    }

    public int size() {
        return history.size();
    }
}