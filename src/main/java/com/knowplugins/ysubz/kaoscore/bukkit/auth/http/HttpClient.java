package com.knowplugins.ysubz.kaoscore.bukkit.auth.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class HttpClient {

    public static final HttpClient DEFAULT = new HttpClient(
            "KaosCore Auth (+https://knowplugins.com)",
            5000,
            5000
    );

    private final String userAgent;
    private final int connectTimeout;
    private final int readTimeout;

    private HttpClient(String userAgent, int connectTimeout, int readTimeout) {
        this.userAgent = userAgent;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    private void buildHttp(HttpURLConnection http, String requestMethod) throws IllegalArgumentException {
        http.setInstanceFollowRedirects(false);
        if (requestMethod != null && !requestMethod.isEmpty()) {
            try {
                http.setRequestMethod(requestMethod);
            } catch (ProtocolException e) {
                throw new IllegalArgumentException("Method '" + requestMethod + "' does not exist!", e);
            }
        }
        http.setRequestProperty("User-Agent", userAgent);
        http.setConnectTimeout(connectTimeout);
        http.setReadTimeout(readTimeout);
    }

    public String get(String url) throws IOException {
        try {
            HttpURLConnection http = (HttpURLConnection) new URL(url).openConnection();
            buildHttp(http, "GET");

            boolean redirect;
            int loopCount = 0;
            do {
                int status = http.getResponseCode();
                redirect = status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER;

                if (redirect) {
                    loopCount++;
                    if (loopCount >= 20) {
                        throw new IOException("Too many redirects!");
                    }
                    String newUrl = http.getHeaderField("Location");
                    http = (HttpURLConnection) new URL(newUrl).openConnection();
                    buildHttp(http, "GET");
                }
            } while (redirect);

            InputStream inputStream = http.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            int read;
            while ((read = bufferedReader.read()) != -1) {
                response.append((char) read);
            }

            bufferedReader.close();
            inputStream.close();

            return response.toString();
        } catch (SocketTimeoutException e) {
            throw new SocketTimeoutException("[GET] The connection took too long to be answered.");
        }
    }
}