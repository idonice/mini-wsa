import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates realistic security events and sends them to the ingest API (or writes them to a file).
 * Background traffic + attack waves (bursts from one IP on one sensitive path).
 *
 * Run (no build needed, JDK 21+):
 *   java generator/EventGenerator.java --count 10000
 *
 * Options:
 *   --count N        total events (default 10000)
 *   --url URL        ingest endpoint (default http://localhost:8080/v1/events/ingest)
 *   --batch-size N   events per request, max 1000 (default 1000)
 *   --minutes N      spread events over the last N minutes (default 60)
 *   --seed N         same seed = same events (default: random)
 *   --out FILE       write a JSON array to FILE instead of sending
 */
public class EventGenerator {

    private static final double WAVE_SHARE = 0.3;     
    private static final int MIN_WAVE_SIZE = 10;
    private static final int MAX_WAVE_SIZE = 60;

    private static final long[] CONFIG_IDS = {14227, 14228, 30001};
    private static final Map<Long, String> HOSTNAMES = Map.of(
            14227L, "www.example.com", 14228L, "shop.example.com", 30001L, "api.example.com");

    private static final String[] NORMAL_PATHS = {
            "/", "/index.html", "/search", "/products", "/api/v1/products", "/api/v1/cart",
            "/api/v1/orders", "/static/app.js", "/api/v1/export", "/contact"};
    private static final String[] SENSITIVE_PATHS = {
            "/login", "/admin", "/api/v1/login", "/admin/users", "/wp-admin/login.php"};

    private static final String[] CATEGORIES = {
            "INJECTION", "XSS", "PROTOCOL_VIOLATION", "DATA_LEAKAGE", "BOT", "DOS", "RATE_LIMIT"};
    private static final int[] CATEGORY_WEIGHTS = {25, 15, 10, 5, 25, 10, 10};
    // Waves are the noisy, automated kinds of attack
    private static final String[] WAVE_CATEGORIES = {"INJECTION", "BOT", "DOS", "RATE_LIMIT"};

    private static final String[] SEVERITIES = {"CRITICAL", "HIGH", "MEDIUM", "LOW"};
    private static final int[] SEVERITY_WEIGHTS = {15, 30, 35, 20};
    private static final String[] ACTIONS = {"DENY", "ALERT", "MONITOR"};
    private static final int[] ACTION_WEIGHTS = {50, 30, 20};

    private static final Map<String, String[]> RULES = Map.of(
            "INJECTION", new String[]{"950001", "SQL_INJECTION", "SQL Injection Attack Detected"},
            "XSS", new String[]{"941100", "XSS_ATTACK", "Cross-Site Scripting Attack Detected"},
            "PROTOCOL_VIOLATION", new String[]{"920100", "MALFORMED_HTTP", "Invalid HTTP Request Line"},
            "DATA_LEAKAGE", new String[]{"950100", "DATA_LEAK", "Sensitive Data in Response"},
            "BOT", new String[]{"990001", "BAD_BOT", "Known Malicious Bot"},
            "DOS", new String[]{"912100", "DOS_PATTERN", "Denial of Service Pattern"},
            "RATE_LIMIT", new String[]{"300001", "RATE_LIMIT", "Rate Limit Exceeded"});

    private static final String[][] GEO = {
            {"CN", "Beijing"}, {"RU", "Moscow"}, {"US", "Dallas"}, {"BR", "Sao Paulo"},
            {"DE", "Frankfurt"}, {"IN", "Mumbai"}, {"NL", "Amsterdam"}, {"IL", "Tel Aviv"}};
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64)", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0)",
            "sqlmap/1.7", "python-requests/2.31", "curl/8.4.0", "Go-http-client/1.1"};

    private final Random random;
    private final long seed;
    private final Instant end = Instant.now().truncatedTo(ChronoUnit.SECONDS);
    private final Instant start;
    private final List<String> backgroundIps = new ArrayList<>();
    private final List<String> attackerIps = new ArrayList<>();
    private int sequence = 0;

    EventGenerator(long seed, int minutes) {
        this.seed = seed;
        this.random = new Random(seed);
        this.start = end.minus(minutes, ChronoUnit.MINUTES);
        for (int i = 0; i < 500; i++) backgroundIps.add(randomIp());
        for (int i = 0; i < 20; i++) attackerIps.add(randomIp());
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> options = parseArgs(args);
        int count = Integer.parseInt(options.getOrDefault("count", "10000"));
        String url = options.getOrDefault("url", "http://localhost:8080/v1/events/ingest");
        int batchSize = Math.min(1000, Integer.parseInt(options.getOrDefault("batch-size", "1000")));
        int minutes = Integer.parseInt(options.getOrDefault("minutes", "60"));
        long seed = options.containsKey("seed") ? Long.parseLong(options.get("seed")) : new Random().nextInt(1_000_000);

        EventGenerator generator = new EventGenerator(seed, minutes);
        List<Event> events = generator.generate(count);
        System.out.printf("Generated %d events (seed %d, last %d minutes)%n", events.size(), seed, minutes);

        if (options.containsKey("out")) {
            writeFile(events, Path.of(options.get("out")));
        } else {
            send(events, url, batchSize);
        }
    }

    // ---------- generation ----------

    List<Event> generate(int count) {
        List<Event> events = new ArrayList<>(count);
        int waveTarget = (int) (count * WAVE_SHARE);

        while (events.size() < waveTarget) {
            int waveSize = Math.min(between(MIN_WAVE_SIZE, MAX_WAVE_SIZE), waveTarget - events.size());
            events.addAll(attackWave(waveSize));
        }
        while (events.size() < count) {
            events.add(backgroundEvent());
        }

        // Send in time order, so the repeat-offender check sees earlier events first
        events.sort(Comparator.comparing(Event::timestamp));
        return events;
    }

    // One IP hammering one sensitive path, a few seconds apart
    private List<Event> attackWave(int size) {
        String ip = pick(attackerIps);
        String path = pick(SENSITIVE_PATHS);
        String category = pick(WAVE_CATEGORIES);
        long config = CONFIG_IDS[random.nextInt(CONFIG_IDS.length)];
        String[] geo = pick(GEO);
        Instant time = randomTime();

        List<Event> wave = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String severity = random.nextInt(100) < 70 ? "CRITICAL" : "HIGH";
            String action = random.nextInt(100) < 85 ? "DENY" : "ALERT";
            wave.add(event(time, config, ip, path, category, severity, action, geo));
            time = time.plusSeconds(between(1, 10));
        }
        return wave;
    }

    private Event backgroundEvent() {
        String path = random.nextInt(100) < 15 ? pick(SENSITIVE_PATHS) : pick(NORMAL_PATHS);
        return event(randomTime(), CONFIG_IDS[random.nextInt(CONFIG_IDS.length)], pick(backgroundIps), path,
                weighted(CATEGORIES, CATEGORY_WEIGHTS), weighted(SEVERITIES, SEVERITY_WEIGHTS),
                weighted(ACTIONS, ACTION_WEIGHTS), pick(GEO));
    }

    private Event event(Instant time, long configId, String ip, String path,
                        String category, String severity, String action, String[] geo) {
        // Waves can run past "now": clamp so no event is in the future
        Instant timestamp = time.isAfter(end) ? end : time;
        String method = path.contains("login") || path.contains("cart") ? "POST" : "GET";
        int status = action.equals("DENY") ? 403 : 200;
        String id = "gen-" + seed + "-" + (++sequence);
        return new Event(id, timestamp, configId, "pol_" + configId, ip, HOSTNAMES.get(configId), path,
                method, status, pick(USER_AGENTS), RULES.get(category), severity, category, action,
                geo, between(200, 4000), between(100, 20000));
    }

    // ---------- output ----------

    private static void writeFile(List<Event> events, Path file) throws IOException {
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < events.size(); i++) {
            json.append("  ").append(events.get(i).toJson()).append(i < events.size() - 1 ? ",\n" : "\n");
        }
        json.append("]\n");
        Files.writeString(file, json);
        System.out.printf("Wrote %s%n", file.toAbsolutePath());
    }

    private static void send(List<Event> events, String url, int batchSize) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        int batches = (events.size() + batchSize - 1) / batchSize;
        int ingested = 0, duplicates = 0, rejected = 0;
        long startMillis = System.currentTimeMillis();

        for (int b = 0; b < batches; b++) {
            List<Event> batch = events.subList(b * batchSize, Math.min(events.size(), (b + 1) * batchSize));
            StringBuilder body = new StringBuilder("[");
            for (int i = 0; i < batch.size(); i++) {
                body.append(i > 0 ? "," : "").append(batch.get(i).toJson());
            }
            body.append("]");

            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            long batchStart = System.currentTimeMillis();
            HttpResponse<String> response;
            try {
                response = client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (ConnectException e) {
                System.err.printf("Cannot reach %s - is the app running?%n", url);
                System.exit(1);
                return;
            }

            int batchIngested = number(response.body(), "ingested");
            int batchDuplicates = number(response.body(), "duplicates");
            int batchRejected = countOccurrences(response.body(), "\"index\"");
            ingested += batchIngested;
            duplicates += batchDuplicates;
            rejected += batchRejected;
            System.out.printf("batch %d/%d  %4d events -> HTTP %d  ingested=%d duplicates=%d rejected=%d  (%d ms)%n",
                    b + 1, batches, batch.size(), response.statusCode(),
                    batchIngested, batchDuplicates, batchRejected, System.currentTimeMillis() - batchStart);
        }

        double seconds = (System.currentTimeMillis() - startMillis) / 1000.0;
        System.out.printf("Done: %d sent, %d ingested, %d duplicates, %d rejected/failed in %.1f s (%.0f events/s)%n",
                events.size(), ingested, duplicates, rejected, seconds, events.size() / seconds);
    }

    // ---------- helpers ----------

    private String randomIp() {
        return between(11, 223) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + between(1, 254);
    }

    private Instant randomTime() {
        long span = end.getEpochSecond() - start.getEpochSecond();
        return start.plusSeconds((long) (random.nextDouble() * span));
    }

    private int between(int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private <T> T pick(T[] values) {
        return values[random.nextInt(values.length)];
    }

    private <T> T pick(List<T> values) {
        return values.get(random.nextInt(values.size()));
    }

    private String weighted(String[] values, int[] weights) {
        int total = 0;
        for (int w : weights) total += w;
        int roll = random.nextInt(total);
        for (int i = 0; i < values.length; i++) {
            roll -= weights[i];
            if (roll < 0) return values[i];
        }
        return values[values.length - 1];
    }

    private static int number(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\":(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + 1)) count++;
        return count;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            options.put(args[i].replaceFirst("^--", ""), args[i + 1]);
        }
        return options;
    }

    // ---------- the event ----------

    record Event(String eventId, Instant timestamp, long configId, String policyId, String clientIp,
                 String hostname, String path, String method, int statusCode, String userAgent,
                 String[] rule, String severity, String category, String action, String[] geo,
                 int requestSize, int responseSize) {

        String toJson() {
            return "{" +
                    field("eventId", eventId) + "," +
                    field("timestamp", timestamp.toString()) + "," +
                    "\"configId\":" + configId + "," +
                    field("policyId", policyId) + "," +
                    field("clientIp", clientIp) + "," +
                    field("hostname", hostname) + "," +
                    field("path", path) + "," +
                    field("method", method) + "," +
                    "\"statusCode\":" + statusCode + "," +
                    field("userAgent", userAgent) + "," +
                    "\"rule\":{" + field("id", rule[0]) + "," + field("name", rule[1]) + "," +
                    field("message", rule[2]) + "," + field("severity", severity) + "," +
                    field("category", category) + "}," +
                    field("action", action) + "," +
                    "\"geoLocation\":{" + field("country", geo[0]) + "," + field("city", geo[1]) + "}," +
                    "\"requestSize\":" + requestSize + "," +
                    "\"responseSize\":" + responseSize +
                    "}";
        }

        private static String field(String key, String value) {
            return "\"" + key + "\":\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }
}
