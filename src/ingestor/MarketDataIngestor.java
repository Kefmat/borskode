package ingestor;

import models.Ticker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Ansvarlig for å hente rådata fra eksterne API-er (Yahoo Finance).
 * Benytter et tråd-basseng (Thread Pool) og CompletableFuture for å hente 
 * data for flere aksjer i parallell, noe som reduserer ventetiden drastisk.
 */
public class MarketDataIngestor {
    private final HttpClient httpClient;
    private final ExecutorService threadPool;
    private final List<String> symbols;

    /**
     * Initialiserer HttpClient og et fast tråd-basseng for asynkrone oppslag.
     */
    public MarketDataIngestor() {
        this.symbols = new ArrayList<>();
        symbols.add("EQNR.OL");  // Equinor
        symbols.add("DNB.OL");   // DNB Bank
        symbols.add("AKRBP.OL"); // Aker BP
        symbols.add("TEL.OL");   // Telenor
        symbols.add("NHY.OL");   // Norsk Hydro
        symbols.add("ORK.OL");   // Orkla
        symbols.add("YAR.OL");   // Yara International

        // Bruker et fast tråd-basseng (Thread Pool) for å begrense ressursbruk
        this.threadPool = Executors.newFixedThreadPool(10);
        
        this.httpClient = HttpClient.newBuilder()
                .executor(threadPool)
                .build();
    }

    /**
     * Henter de siste prisene for alle definerte aksjer asynkront og i parallell.
     * @return En liste over Ticker-objekter med ferske kurser.
     */
    public List<Ticker> fetchLatestPrices() {
        // 1. Kartlegg hvert symbol til en asynkron oppgave (CompletableFuture)
        List<CompletableFuture<Ticker>> futures = symbols.stream()
                .map(this::fetchSingleTickerAsync)
                .collect(Collectors.toList());

        // 2. Vent til samtlige oppgaver er ferdigstilt (Barrier)
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        // 3. Samle opp resultatene fra alle trådene
        return allFutures.thenApply(v -> 
                futures.stream()
                        .map(CompletableFuture::join) // join() blokkerer ikke her siden allOf() er ferdig
                        .filter(ticker -> ticker.getLastPrice() > 0.0) // Filtrer ut feilede oppslag
                        .collect(Collectors.toList())
        ).join();
    }

    /**
     * Hjelpemetode som utfører et enkelt HTTP-kall asynkront.
     */
    private CompletableFuture<Ticker> fetchSingleTickerAsync(String symbol) {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0") // Omgår blokkering fra Yahoo
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return parseResponse(symbol, response.body());
                    } else {
                        System.err.println("Ugyldig statuskode for " + symbol + ": " + response.statusCode());
                        return new Ticker(symbol, 0.0, 0.0);
                    }
                })
                .exceptionally(e -> {
                    System.err.println("Nettverksfeil under asynkron henting av " + symbol + ": " + e.getMessage());
                    return new Ticker(symbol, 0.0, 0.0);
                });
    }

    /**
     * Parser JSON-streng manuelt for å hente ut pris og forrige sluttkurs.
     */
    private Ticker parseResponse(String symbol, String body) {
        try {
            int priceIndex = body.lastIndexOf("\"regularMarketPrice\":");
            int endIndex = body.indexOf(",", priceIndex);
            double price = Double.parseDouble(body.substring(priceIndex + 21, endIndex));
            
            int prevCloseIndex = body.lastIndexOf("\"chartPreviousClose\":");
            int prevEndIndex = body.indexOf(",", prevCloseIndex);
            double prevClose = Double.parseDouble(body.substring(prevCloseIndex + 21, prevEndIndex));
            
            double change = ((price - prevClose) / prevClose) * 100;
            return new Ticker(symbol, price, change);
        } catch (Exception e) {
            System.err.println("Parsing feilet for " + symbol);
            return new Ticker(symbol, 0.0, 0.0);
        }
    }

    /**
     * Ryddig avslutning av tråd-bassenget ved programslutt.
     */
    public void shutdown() {
        threadPool.shutdown();
    }
}