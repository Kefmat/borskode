package ingestor;

import models.Ticker;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Ansvarlig for å hente rådata fra eksterne kilder (Yahoo Finance).
 * Inneholder logikk for nettverkstilkobling og enkel parsing av JSON-responser.
 */
public class MarketDataIngestor {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final List<String> symbols;

    /**
     * Initialiserer ingestoren med en liste over relevante tickers for Oslo Børs.
     */
    public MarketDataIngestor() {
        this.symbols = new ArrayList<>();
        symbols.add("EQNR.OL");  // Equinor
        symbols.add("DNB.OL");   // DNB Bank
        symbols.add("AKRBP.OL"); // Aker BP
        symbols.add("TEL.OL");   // Telenor
    }

    /**
     * Utfører HTTP-forespørsler for alle definerte symboler.
     * * @return En liste over Ticker-objekter med ferske kurser.
     */
    public List<Ticker> fetchLatestPrices() {
        List<Ticker> updates = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "Mozilla/5.0") // Påkrevd av Yahoo Finance API
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    updates.add(parseResponse(symbol, response.body()));
                }
            } catch (Exception e) {
                // Logger feil per symbol for å unngå at hele loopen stopper
                System.err.println("Nettverksfeil for " + symbol + ": " + e.getMessage());
            }
        }
        return updates;
    }

    /**
     * Parser JSON-streng manuelt for å hente ut pris og forrige sluttkurs.
     * MERK: Ved utvidelse bør et JSON-bibliotek som Jackson eller Gson benyttes.
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
            return new Ticker(symbol, 0.0, 0.0);
        }
    }
}