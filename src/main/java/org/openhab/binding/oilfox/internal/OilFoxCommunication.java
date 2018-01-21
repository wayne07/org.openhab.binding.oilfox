package org.openhab.binding.oilfox.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;

import org.openhab.binding.oilfox.handler.OilFoxHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OilFoxCommunication {
    private final Logger logger = LoggerFactory.getLogger(OilFoxHandler.class);

    // TBD: store token
    private String token = null;

    public void Login(String address, String email, String password) throws MalformedURLException, IOException {
        URL url = new URL("https://" + address + "/v1/user/login");
        logger.info("Login({})", url.toString());
        HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
        request.setReadTimeout(10000);
        request.setConnectTimeout(15000);
        request.setRequestMethod("POST");
        request.setRequestProperty("Content-Type", "application/json");
        request.setDoInput(true);
        request.setDoOutput(true);

        JsonObject requestObject = new JsonObject();
        requestObject.addProperty("email", email);
        requestObject.addProperty("password", password);

        OutputStream os = request.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(requestObject.toString());
        writer.flush();
        writer.close();
        os.close();

        request.connect();

        switch (request.getResponseCode()) {
            case 401:
                throw new IOException("Unauthorized");
            case 200:
                // authorized
            default:
                Reader reader = new InputStreamReader(request.getInputStream(), "UTF-8");
                JsonParser parser = new JsonParser();
                JsonElement element = parser.parse(reader);
                if (element.isJsonObject()) {
                    JsonObject responseObject = element.getAsJsonObject();
                    token = responseObject.get("token").getAsString();
                    logger.info("Token " + token);
                }
                reader.close();
                break;
        }
    }

    public Properties Summary(String address) throws MalformedURLException, IOException {
        if (token == null) {
            return null;
        }

        URL url = new URL("https://" + address + "/v1/user/summary");
        logger.info("Summary({})", url.toString());
        HttpsURLConnection request = (HttpsURLConnection) url.openConnection();
        request.setReadTimeout(10000);
        request.setConnectTimeout(15000);
        request.setRequestProperty("Content-Type", "application/json");
        request.setRequestProperty("X-Auth-Token", token);
        request.setDoInput(true);
        request.connect();

        switch (request.getResponseCode()) {
            case 401:
                throw new IOException("Unauthorized");
            case 200:
                // authorized
            default:
                Properties prop = new Properties();

                BufferedReader in = new BufferedReader(new InputStreamReader(request.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                logger.info(response.toString());

                /*
                 * Reader reader = new InputStreamReader(request.getInputStream(), "UTF-8");
                 * JsonParser parser = new JsonParser();
                 * JsonElement element = parser.parse(reader);
                 * if (element.isJsonObject()) {
                 * JsonObject object = element.getAsJsonObject();
                 * String metering = object.get("metering").getAsString();
                 * prop.setProperty("metering", metering);
                 * logger.info("Metering " + metering);
                 * }
                 *
                 * reader.close();
                 */

                return prop;
        }
    }
}
