package analytics;

import models.Ticker;
import java.util.HashMap;
import java.util.Map;

/**
 * Hovedmodulen for teknisk analyse i Børskode.
 * Overvåker prisendringer og genererer signaler ved brudd på tekniske nivåer.
 */
public class MarketMonitor {
    private final Map<String, PriceHistoryBuffer> historyMap = new HashMap<>();
    
    // Definerer vinduet for glidende gjennomsnitt (10 iterasjoner)
    private static final int SMA_WINDOW = 10;

    /**
     * Tar imot en ny ticker og oppdaterer tilhørende analysebuffer.
     * * @param ticker Den oppdaterte aksjekursen.
     */
    public void processUpdate(Ticker ticker) {
        String symbol = ticker.getSymbol();
        historyMap.putIfAbsent(symbol, new PriceHistoryBuffer(SMA_WINDOW));
        
        PriceHistoryBuffer buffer = historyMap.get(symbol);
        buffer.addPrice(ticker.getLastPrice());

        double sma = buffer.calculateSMA();
        
        // Utfør analyse kun når bufferen er tilstrekkelig fylt
        if (buffer.size() >= SMA_WINDOW) {
            checkSignals(ticker, sma);
        }
    }

    /**
     * Analyserer priskryssinger mot SMA for å generere Bullish/Bearish-signaler.
     */
    private void checkSignals(Ticker ticker, double sma) {
        double currentPrice = ticker.getLastPrice();
        double diffPercent = ((currentPrice - sma) / sma) * 100;

        // Margin på 0.1% brukes for å unngå falske signaler ved små fluktuasjoner
        if (currentPrice > sma && diffPercent > 0.1) {
            System.out.printf("[BULLISH] %s bryter over SMA: %.2f (SMA: %.2f)\n", 
                ticker.getSymbol(), currentPrice, sma);
        } else if (currentPrice < sma && Math.abs(diffPercent) > 0.1) {
            System.out.printf("[BEARISH] %s bryter under SMA: %.2f (SMA: %.2f)\n", 
                ticker.getSymbol(), currentPrice, sma);
        }
    }
}