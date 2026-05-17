import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestConnection {
    public static void main(String[] args) {
        System.out.println("Starting connection test...");
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/realms/eshop/protocol/openid-connect/certs"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            long start = System.currentTimeMillis();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long duration = System.currentTimeMillis() - start;
            
            System.out.println("Status: " + response.statusCode());
            System.out.println("Response time: " + duration + " ms");
            System.out.println("Content: " + response.body().substring(0, Math.min(100, response.body().length())) + "...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
