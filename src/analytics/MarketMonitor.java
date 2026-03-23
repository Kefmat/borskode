package analytics;

import models.Ticker;
import java.util.HashMap;
import java.util.Map;

/**
 * Hovedmodulen for teknisk analyse i Børskode.
 * Overvåker prisendringer og genererer signaler ved brudd på tekniske nivåer,
 * samt varsler om unormal volatilitet basert på statistiske avvik.
 */
public class MarketMonitor {
    private final Map<String, PriceHistoryBuffer> historyMap = new HashMap<>();
    
    // Definerer vinduet for glidende gjennomsnitt (10 iterasjoner)
    private static final int SMA_WINDOW = 10;

    /**
     * Tar imot en ny ticker og oppdaterer tilhørende analysebuffer.
     * @param ticker Den oppdaterte aksjekursen fra børsen.
     */
    public void processUpdate(Ticker ticker) {
        String symbol = ticker.getSymbol();
        
        // Initialiserer buffer for aksjen dersom den ikke eksisterer
        historyMap.putIfAbsent(symbol, new PriceHistoryBuffer(SMA_WINDOW));
        
        PriceHistoryBuffer buffer = historyMap.get(symbol);
        buffer.addPrice(ticker.getLastPrice());

        // Utfør analyse kun når vi har samlet nok data til å fylle vinduet
        if (buffer.size() >= SMA_WINDOW) {
            double sma = buffer.calculateSMA();
            double volatility = buffer.calculateStandardDeviation();
            
            checkSignals(ticker, sma, volatility);
        }
    }

    /**
     * Analyserer priskryssinger mot SMA og sjekker for statistisk signifikante avvik (volatilitet).
     * @param ticker Den gjeldende aksjen.
     * @param sma Det beregnede enkle glidende gjennomsnittet.
     * @param vol Det nåværende standardavviket (volatilitet).
     */
    private void checkSignals(Ticker ticker, double sma, double vol) {
        double currentPrice = ticker.getLastPrice();
        double diff = currentPrice - sma;
        double diffPercent = (diff / sma) * 100;

        // Trend-analyse (SMA Crossover)
        // Margin på 0.1% brukes for å filtrere ut ubetydelig støy.
        if (currentPrice > sma && diffPercent > 0.1) {
            System.out.printf("[BULLISH] %s over SMA: %.2f (SMA: %.2f)\n", 
                ticker.getSymbol(), currentPrice, sma);
        } else if (currentPrice < sma && Math.abs(diffPercent) > 0.1) {
            System.out.printf("[BEARISH] %s under SMA: %.2f (SMA: %.2f)\n", 
                ticker.getSymbol(), currentPrice, sma);
        }

        // Volatilitets-analyse (2-Sigma deteksjon)
        // Hvis avviket fra snittet er mer enn 2 standardavvik, regnes det som en uvanlig hendelse.
        // Legger inn en minimumsgrense for volatilitet (0.05) for å unngå varsler på flate kurslinjer.
        if (vol > 0.05 && Math.abs(diff) > (2 * vol)) {
            String type = (diff > 0) ? "SPIKE" : "DROP";
            System.out.printf("[VARSEL] %s: %s detektert! Avvik: %.2f NOK (Sigma: %.2f)\n", 
                ticker.getSymbol(), type, diff, vol);
        }
    }
}