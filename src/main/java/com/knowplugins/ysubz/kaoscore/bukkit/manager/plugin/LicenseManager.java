package com.knowplugins.ysubz.kaoscore.bukkit.manager.plugin;
import org.bukkit.Bukkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class LicenseManager {

    private final String licenseKey;
    private final String apiURL = "http://nl-01.freezehost.pro:10028/verificar?key=";

    public LicenseManager(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public boolean isValid() {
        try {
            URL url = new URL(apiURL + licenseKey);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

            int responseCode = con.getResponseCode();
            if (responseCode != 200) {
                return false;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine = in.readLine();
            in.close();

            if (inputLine != null) {
                inputLine = inputLine.toLowerCase();
                return inputLine.contains("true") && !inputLine.contains("false");
            }

            return false;

        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§c[KaosCore] Falha ao contatar servidor de licenças: " + e.getMessage());
            return false;
        }
    }
}