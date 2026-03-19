package models;

import java.time.Instant;

/**
 * Representerer et enkelt verdipapir (aksje) med sanntidsdata.
 * Brukes som databærer mellom Ingestor og Analytics-modulene.
 */
public class Ticker {
    private final String symbol;
    private final double lastPrice;
    private final double changePercent;
    private final Instant timestamp;

    /**
     * Oppretter en ny ticker-instans med tidsstempel for mottak.
     * * @param symbol Aksjesymbol (f.eks. EQNR.OL)
     * @param lastPrice Siste omsatte pris
     * @param changePercent Endring i prosent siden forrige dags sluttkurs
     */
    public Ticker(String symbol, double lastPrice, double changePercent) {
        this.symbol = symbol;
        this.lastPrice = lastPrice;
        this.changePercent = changePercent;
        this.timestamp = Instant.now();
    }

    public String getSymbol() { return symbol; }
    public double getLastPrice() { return lastPrice; }
    public double getChangePercent() { return changePercent; }
    public Instant getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] Pris: %.2f NOK (%+.2f%%) - %s", 
            symbol, lastPrice, changePercent, timestamp);
    }
}