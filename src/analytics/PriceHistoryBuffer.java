package analytics;

import java.util.LinkedList;
import java.util.Queue;
import java.util.List;
import java.util.ArrayList;

/**
 * En sirkulær buffer for lagring av historiske prispunkter.
 * Brukes for å beregne statistiske indikatorer som Simple Moving Average (SMA),
 * volatilitet (standardavvik) og Relative Strength Index (RSI) over et glidende vindu.
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
     * Legger til en ny pris. Fjerner den eldste verdien hvis bufferen er full
     * for å opprettholde det glidende vinduet.
     * @param price Den ferske prisen som skal legges til.
     */
    public void addPrice(double price) {
        if (history.size() >= maxSize) {
            history.poll();
        }
        history.add(price);
    }

    /**
     * Beregner Simple Moving Average (SMA) for de lagrede verdiene.
     * @return Gjennomsnittspris over vinduet, eller 0.0 hvis tomt.
     */
    public double calculateSMA() {
        if (history.isEmpty()) return 0.0;
        double sum = 0;
        for (double price : history) {
            sum += price;
        }
        return sum / history.size();
    }

    /**
     * Beregner standardavviket for prisene i vinduet. 
     * Dette brukes som et direkte mål på markedets volatilitet.
     * * Formel: sqrt( sum( (pris - gjennomsnitt)^2 ) / N )
     * * @return Standardavviket (volatilitet) som en double.
     */
    public double calculateStandardDeviation() {
        if (history.size() < 2) return 0.0;
        
        double sma = calculateSMA();
        double sumSquaredDiffs = 0;
        
        for (double price : history) {
            sumSquaredDiffs += Math.pow(price - sma, 2);
        }
        
        return Math.sqrt(sumSquaredDiffs / history.size());
    }

    /**
     * Beregner Relative Strength Index (RSI) for prisutviklingen i vinduet.
     * Brukes for å identifisere om en aksje er overkjøpt eller oversolgt.
     * * Formel: 100 - (100 / (1 + (Gjennomsnittlig gevinst / Gjennomsnittlig tap)))
     * @return RSI-verdi mellom 0 og 100.
     */
    public double calculateRSI() {
        if (history.size() < 2) return 50.0;

        List<Double> list = new ArrayList<>(history);
        double totalGain = 0;
        double totalLoss = 0;

        for (int i = 1; i < list.size(); i++) {
            double difference = list.get(i) - list.get(i - 1);
            if (difference > 0) {
                totalGain += difference;
            } else {
                totalLoss += Math.abs(difference);
            }
        }

        if (totalLoss == 0) return 100.0;
        
        double avgGain = totalGain / (list.size() - 1);
        double avgLoss = totalLoss / (list.size() - 1);
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    /**
     * @return Antall prispunkter som for øyeblikket er lagret i bufferen.
     */
    public int size() {
        return history.size();
    }
}