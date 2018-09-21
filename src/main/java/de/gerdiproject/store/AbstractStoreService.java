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
package de.gerdiproject.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.gerdiproject.store.datamodel.*;
import de.gerdiproject.store.handler.PostRootRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static spark.Spark.*;

/**
 * This class provides some functionality to implement a store service. It also deals with some API issues, so an implementation only needs to implement the access to the storage.
 *
 * @param <E> The type used to store the credentials. Must implement the ICredentials interface.
 */
public abstract class AbstractStoreService<E extends ICredentials> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractStoreService.class);
    private final Map<String, CacheElement<E>> CACHE_MAP = new HashMap<>(); // Possible to abstract here; allows for different implementations
    private final Properties props;
    private boolean running = false;


    public AbstractStoreService(Properties props) {
        this.props = props;
    }

    /**
     * This method checks whether or not the user is logged in.
     *
     * @param creds The stored credentials, may be null if no credentials were stored
     * @return true if logged in, false otherwise
     */
    protected abstract boolean isLoggedIn(E creds);

    /**
     * This method is called if a login request is performed.
     *
     * @param req A instance of SparkJava's Request class
     * @param res A instance of SparkJava's Response class
     * @return An instance of a newly created credential.
     */
    protected abstract E login(Request req, Response res);

    /**
     * This method is called for each research data element to be copied.
     *
     * @param creds       The stored credentials, may be null if no credentials were stored
     * @param targetDir   The directory where the data should be stored
     * @param taskElement A TaskElement containing the required information on where to retrieve the research data
     * @return true if this
     */
    protected abstract boolean copyFile(E creds, String targetDir, TaskElement taskElement);

    /**
     * Returns the list of files for a requested directory.
     *
     * @param directory The requested directory relative to the root directory.
     * @param creds     The stored credentials, may be null if no credentials were stored
     * @return A list of elements depicting the files/subdirectories
     */
    protected abstract List<ListElement> listFiles(String directory, E creds);

    /**
     * This method is executed once before any copy is triggered.
     * May be overwritten to perform tasks before starting to copy data sets.
     *
     * @param task
     */
    protected void preCopy(StoreTask task) {
    }

    /**
     * Sets the folder in classpath serving static files. Observe: this method
     * must be called before all other methods. {@see Spark}
     *
     * @param folder the folder in classpath.
     */
    protected void registerStaticFolder(String folder) {
        if (running) throw new IllegalStateException("This method must be called before executing the run method.");
        staticFiles.location(folder);
    }

    /**
     * This method starts the webserver and initializes all predefined routes.
     * It must be executed after the inialization of this class.
     */
    protected void run() {
        if (running) throw new IllegalStateException("The run method must be run only once.");
        this.running = true;
        port(5678);

        // Accepts new storing tasks and initializes them in the in-memory cache
        post("/", new PostRootRoute<E>(CACHE_MAP));

        get("/loggedIn/:sessionId", (request, response) -> {
            CacheElement<E> element = CACHE_MAP.get(request.params("sessionId"));
            if (element == null) {
                response.status(404);
                LOGGER.warn("Attempt to access non-existent Session " + request.params("sessionId"));
                return "Session ID does not exist";
            }
            boolean loggedIn = this.isLoggedIn(element.getCredentials());
            return "{ \"isLoggedIn\": \"" + loggedIn + "\" }";
        });

        get("/progress/:sessionId", (request, response) -> {
            Object[] elem = CACHE_MAP.get(request.params("sessionId")).getProgress().values().toArray();
            return new GsonBuilder().create().toJson(elem);
        });

        post("/login/:sessionId", (request, response) -> {
            CacheElement elem = CACHE_MAP.get(request.params("sessionId"));
            if (elem == null) {
                response.status(404);
                return "Session does not exist.";
            }
            E credentials = this.login(request, response);
            if (credentials != null) {
                elem.setCredentials(credentials);
                return "Login Successful";
            } else {
                LOGGER.warn("Login failed for Session " + request.queryParams("sessionId"));
                return "Login failed";
            }
        });

        get("/copy/:sessionId", (request, response) -> {
            final String targetDir = request.queryParamOrDefault("dir", "/");
            final String session = request.params("sessionId");
            final StoreTask task = CACHE_MAP.get(session).getTask();
            final E creds = CACHE_MAP.get(session).getCredentials();
            this.preCopy(task);
            for (TaskElement entry : CACHE_MAP.get(session).getProgress()) {
                this.copyFile(creds, targetDir, entry);
            }
            return "";
        });

        get("/files/:sessionId", (request, response) -> {
            final String dir = request.queryParamOrDefault("dir", "/");
            final E creds = CACHE_MAP.get(request.params("sessionId")).getCredentials();
            if (creds == null) {
                response.status(403);
                return "Not logged in";
            }
            List<ListElement> ret = listFiles(dir, creds);
            return new Gson().toJson(ret);
        });
    }
}
