package analytics;

import models.Ticker;
import trading.Portfolio;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Hovedmodulen for teknisk analyse i Børskode.
 * Overvåker prisendringer og genererer signaler ved brudd på tekniske nivåer,
 * samt varsler om unormal volatilitet basert på statistiske avvik og 
 * momentum-analyser via RSI. Inkluderer nå Stop-Loss og Take-Profit logikk.
 */
public class MarketMonitor {
    private final Map<String, PriceHistoryBuffer> historyMap = new HashMap<>();
    
    // Holder styr på prisen vi kjøpte aksjen for i simulatoren
    private final Map<String, Double> entryPrices = new HashMap<>();
    
    // Definerer grenser for risikostyring (2% tap eller 5% gevinst)
    private static final double STOP_LOSS_LIMIT = -2.0;
    private static final double TAKE_PROFIT_LIMIT = 5.0;
    
    // Definerer vinduet for teknisk analyse (14 iterasjoner er standard for RSI)
    private static final int WINDOW_SIZE = 14;

    /**
     * Tar imot en ny ticker og oppdaterer tilhørende analysebuffer samt simulerer handel.
     * @param ticker Den oppdaterte aksjekursen fra børsen.
     * @param portfolio Porteføljen som brukes til paper trading.
     */
    public void processUpdate(Ticker ticker, Portfolio portfolio) {
        String symbol = ticker.getSymbol();
        
        // Initialiserer buffer for aksjen dersom den ikke eksisterer
        historyMap.putIfAbsent(symbol, new PriceHistoryBuffer(WINDOW_SIZE));
        
        PriceHistoryBuffer buffer = historyMap.get(symbol);
        buffer.addPrice(ticker.getLastPrice());

        // Oppdaterer alltid porteføljen med siste kjente markedspris for verdivurdering
        portfolio.updateLastPrice(symbol, ticker.getLastPrice());

        // Utfør analyse kun når vi har samlet nok data til å fylle vinduet
        if (buffer.size() >= WINDOW_SIZE) {
            double sma = buffer.calculateSMA();
            double volatility = buffer.calculateStandardDeviation();
            double rsi = buffer.calculateRSI();
            
            checkSignals(ticker, sma, volatility, rsi, portfolio);
        }
    }

    /**
     * Analyserer priskryssinger mot SMA, sjekker for statistisk signifikante avvik (volatilitet)
     * og vurderer momentum via RSI. Utfører kjøp/salg i porteføljen ved gitte kriterier.
     * Inkluderer nå sjekk for Stop-Loss og Take-Profit for å beskytte kapitalen.
     * @param ticker Den gjeldende aksjen.
     * @param sma Det beregnede enkle glidende gjennomsnittet.
     * @param vol Det nåværende standardavviket (volatilitet).
     * @param rsi Den nåværende Relative Strength Index (momentum).
     * @param portfolio Brukes for å simulere handler basert på signalene.
     */
    private void checkSignals(Ticker ticker, double sma, double vol, double rsi, Portfolio portfolio) {
        double currentPrice = ticker.getLastPrice();
        double diff = currentPrice - sma;
        double diffPercent = (diff / sma) * 100;
        String symbol = ticker.getSymbol();

        // --- RISIKOSTYRING (STOP-LOSS / TAKE-PROFIT) ---
        // Sjekk om vi eier aksjen for å vurdere exit-strategi
        if (portfolio.getHoldings().getOrDefault(symbol, 0) > 0 && entryPrices.containsKey(symbol)) {
            double purchasePrice = entryPrices.get(symbol);
            double currentReturn = ((currentPrice - purchasePrice) / purchasePrice) * 100;

            // Stop-Loss: Selg hvis tapet overstiger grensen
            if (currentReturn <= STOP_LOSS_LIMIT) {
                System.out.printf("[RISK] STOP-LOSS utløst for %s! Tap: %.2f%% (Kurs: %.2f)\n", 
                                  symbol, currentReturn, currentPrice);
                portfolio.sell(symbol, currentPrice);
                entryPrices.remove(symbol);
                return; // Avslutt sjekk for denne tickeren i denne omgangen
            }

            // Take-Profit: Selg hvis gevinsten når målet
            if (currentReturn >= TAKE_PROFIT_LIMIT) {
                System.out.printf("[RISK] TAKE-PROFIT nådd for %s! Gevinst: %.2f%% (Kurs: %.2f)\n", 
                                  symbol, currentReturn, currentPrice);
                portfolio.sell(symbol, currentPrice);
                entryPrices.remove(symbol);
                return;
            }
        }

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
        // Inkluderer simulerte handelsordre basert på momentum-signaler.
        if (rsi >= 70) {
            System.out.printf("[RSI] %s er OVERKJØPT (%.2f). Potensielt salgssignal.\n", 
                ticker.getSymbol(), rsi);
            // Strategi: Selg beholdning hvis aksjen er overkjøpt
            if (portfolio.getHoldings().getOrDefault(symbol, 0) > 0) {
                portfolio.sell(symbol, currentPrice);
                entryPrices.remove(symbol);
            }
        } else if (rsi <= 30) {
            System.out.printf("[RSI] %s er OVERSOLGT (%.2f). Potensielt kjøpssignal.\n", 
                ticker.getSymbol(), rsi);
            // Strategi: Kjøp hvis aksjen er oversolgt og vi ikke eier den fra før
            if (portfolio.getHoldings().getOrDefault(symbol, 0) == 0) {
                // Investerer ca. 20% av tilgjengelig kapital i aksjen
                int quantity = (int) ((portfolio.getCash() * 0.2) / currentPrice);
                if (quantity > 0) {
                    portfolio.buy(symbol, currentPrice, quantity);
                    // Lagrer kjøpspris for å kunne beregne stop-loss/take-profit senere
                    entryPrices.put(symbol, currentPrice);
                }
            }
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