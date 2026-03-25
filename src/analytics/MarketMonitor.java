package analytics;

import models.Ticker;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Hovedmodulen for teknisk analyse i Børskode.
 * Overvåker prisendringer og genererer signaler ved brudd på tekniske nivåer,
 * samt varsler om unormal volatilitet basert på statistiske avvik og 
 * momentum-analyser via RSI.
 */
public class MarketMonitor {
    private final Map<String, PriceHistoryBuffer> historyMap = new HashMap<>();
    
    // Definerer vinduet for teknisk analyse (14 iterasjoner er standard for RSI)
    private static final int WINDOW_SIZE = 14;

    /**
     * Tar imot en ny ticker og oppdaterer tilhørende analysebuffer.
     * @param ticker Den oppdaterte aksjekursen fra børsen.
     */
    public void processUpdate(Ticker ticker) {
        String symbol = ticker.getSymbol();
        
        // Initialiserer buffer for aksjen dersom den ikke eksisterer
        historyMap.putIfAbsent(symbol, new PriceHistoryBuffer(WINDOW_SIZE));
        
        PriceHistoryBuffer buffer = historyMap.get(symbol);
        buffer.addPrice(ticker.getLastPrice());

        // Utfør analyse kun når vi har samlet nok data til å fylle vinduet
        if (buffer.size() >= WINDOW_SIZE) {
            double sma = buffer.calculateSMA();
            double volatility = buffer.calculateStandardDeviation();
            double rsi = buffer.calculateRSI();
            
            checkSignals(ticker, sma, volatility, rsi);
        }
    }

    /**
     * Analyserer priskryssinger mot SMA, sjekker for statistisk signifikante avvik (volatilitet)
     * og vurderer momentum via RSI.
     * @param ticker Den gjeldende aksjen.
     * @param sma Det beregnede enkle glidende gjennomsnittet.
     * @param vol Det nåværende standardavviket (volatilitet).
     * @param rsi Den nåværende Relative Strength Index (momentum).
     */
    private void checkSignals(Ticker ticker, double sma, double vol, double rsi) {
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

        // Momentum-analyse (RSI)
        // Definerer overkjøpte (>70) og oversolgte (<30) nivåer.
        if (rsi >= 70) {
            System.out.printf("[RSI] %s er OVERKJØPT (%.2f). Potensielt salgssignal.\n", 
                ticker.getSymbol(), rsi);
        } else if (rsi <= 30) {
            System.out.printf("[RSI] %s er OVERSOLGT (%.2f). Potensielt kjøpssignal.\n", 
                ticker.getSymbol(), rsi);
        }
    }

    /**
     * Eksponerer historikk-kartet for eksterne komponenter (f.eks. UI/Dashboard).
     * Returnerer et skrivebeskyttet kart for å bevare dataintegritet.
     * @return En uforanderlig Map med symboler og tilhørende prishistorikk.
     */
    public Map<String, PriceHistoryBuffer> getHistoryMap() {
        return Collections.unmodifiableMap(historyMap);
    }
}