/**
 * Copyright © 2018 Nelson Tavares de Sousa (tavaresdesousa@email.uni-kiel.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.gerdiproject.store.pac4j;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.gerdiproject.store.GerdiStoreException;
import de.gerdiproject.store.StoreConstants;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.http.client.direct.DirectBearerAuthClient;
import org.pac4j.jwt.config.signature.RSASignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.jwt.util.JWKHelper;
import org.pac4j.sparkjava.DefaultHttpActionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyPair;

/**
 * This class represents a configuration factory used to build configurations for pac4j used by this service.
 */
public class GerdiConfigFactory implements ConfigFactory {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GerdiConfigFactory.class);

    @Override
    public Config build(final Object... parameters) {
        final JwtAuthenticator jwtAuthenticator = new JwtAuthenticator();

        String jwks = "";
        try {
            jwks = readUrl(StoreConstants.OPENID_JWK_ENDPOINT);
        } catch (Exception e) {
            throw new GerdiStoreException("Could not retrieve JWKs. Initialization aborted.", e);
        }
        Gson gson = new Gson();
        final JsonElement data = new JsonParser().parse(jwks);
        final JsonArray keys = data.getAsJsonObject().getAsJsonArray("keys");

        for (JsonElement key : keys) {
            KeyPair pair = JWKHelper.buildRSAKeyPairFromJwk(key.getAsJsonObject().toString());
            jwtAuthenticator.addSignatureConfiguration(new RSASignatureConfiguration(pair));
        }
        final DirectBearerAuthClient bearerClient = new DirectBearerAuthClient(jwtAuthenticator);

        final Clients clients = new Clients(bearerClient);

        final Config config = new Config(clients);
        config.setHttpActionAdapter(new DefaultHttpActionAdapter());
        return config;
    }

    /**
     * Reads a string from a given URL
     *
     * @param urlString The url which will be connected to
     * @return The string read at the url
     * @throws Exception
     */
    private static String readUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        }
    }

}