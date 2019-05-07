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
import de.gerdiproject.store.util.CacheGarbageCollectionTask;
import de.gerdiproject.store.util.ResearchDataInputStreamSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

import static spark.Spark.*;

/**
 * This class provides some functionality to implement a store service.
 * It also deals with some API issues, so an implementation only needs to implement the access to the storage.
 *
 * @author Nelson Tavares de Sousa
 *
 * @param <E> The type used to store the credentials. Must implement the ICredentials interface.
 */
public abstract class AbstractStoreService<E extends ICredentials> {

    /**
     * This class's logger instance
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractStoreService.class);

    /**
     * A Gson instance for serializing {@linkplain ResearchDataInputStream}.
     */
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(ResearchDataInputStream.class, new ResearchDataInputStreamSerializer()).create();
    /**
     * A map used as a cache
     */
    private final Map<String, CacheElement<E>> cacheMap = new ConcurrentHashMap<>(); // Possible to abstract here; allows for different implementations
    /**
     * Timer for garbage collection on the cache
     */
    private final Timer timer = new Timer(true);
    /**
     * Flag to check if this service was already executed
     */
    private boolean running = false;


    /**
     * This constructor initializes the store service
     */
    protected AbstractStoreService() {
        // Run a garbage collection task every 5 minutes
        timer.schedule(new CacheGarbageCollectionTask<E>(this.cacheMap), 300000, 300000);
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
    protected abstract boolean copyFile(E creds, String targetDir, ResearchDataInputStream taskElement);

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
     * @param creds The stored credentials, may be null if no credentials were stored
     */
    protected void preCopy(final E creds) { // NOPMD empty on purpose
    }

    /**
     * This method is executed if a new folder is requested.
     *
     * @param dir The directory where the new folder is requested to be created
     * @param dirName The name of the requested folder
     * @param creds The stored credentials, may be null if no credentials were stored
     * @return true on success, false otherwise
     */
    protected abstract boolean createDir(final String dir, final String dirName, final E creds);

    /**
     * Sets the folder in classpath serving static files. Observe: this method
     * must be called before all other methods.
     *
     * @param folder the folder in classpath.
     */
    protected void registerStaticFolder(final String folder) {
        if (running) {
            throw new IllegalStateException("This method must be called before executing the run method.");
        }
        staticFiles.location(folder);
    }

    /**
     * This method starts the webserver and initializes all predefined routes.
     * It must be executed after the inialization of this class.
     */
    protected void run() {
        if (running) {
            throw new IllegalStateException("The run method may only be executed once.");
        }
        this.running = true;
        port(5678);

        // Accepts new storing tasks and initializes them in the in-memory cache
        post("/", new PostRootRoute<E>(cacheMap));

        // Checker whether or not the user is logged in
        get("/loggedIn/:" + StoreConstants.SESSION_ID, this::getLoggedIn);

        // Return a list with the progress of each element
        get("/progress/:" + StoreConstants.SESSION_ID, this::getProgress);

        // Log in the user
        post("/login/:" + StoreConstants.SESSION_ID, this::postLogin);

        // Start the copy progress
        get("/copy/:" + StoreConstants.SESSION_ID, this::getCopy);

        // Returns a list of files for a given directory
        get("/files/:" + StoreConstants.SESSION_ID, this::getFiles);

        // Create new dir
        get("/createdir/:" + StoreConstants.SESSION_ID + "/:dirname", this::getCreatedir);
    }

    private Object getCreatedir(Request request, Response response) {
        final E creds = cacheMap.get(request.params(StoreConstants.SESSION_ID)).getCredentials();
        if (creds == null) {
            response.status(403);
            return "Not logged in";
        }
        final String dirName = request.params("dirname");
        final String dir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
        final boolean created = createDir(dir, dirName, creds);
        return String.format(StoreConstants.DIR_CREATED_RESPONSE, created);
    }

    private Object getFiles(Request request, Response response) {
        final E creds = cacheMap.get(request.params(StoreConstants.SESSION_ID)).getCredentials();
        if (creds == null) {
            response.status(403);
            return "Not logged in";
        }
        final String dir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
        final List<ListElement> ret = listFiles(dir, creds);
        return new Gson().toJson(ret);
    }

    private Object getCopy(Request request, Response response) {
        final String session = request.params(StoreConstants.SESSION_ID);
        final CacheElement<E> cacheElement = cacheMap.get(session);
        final StoreTask task = cacheElement.getTask();

        // Don't start the copy process twice
        if (task.isStarted()) {
            return "Process already started";
        } else {
            task.setStarted(true);
        }

        final E creds = cacheElement.getCredentials();
        final String targetDir = request.queryParamOrDefault(StoreConstants.DIR_QUERYPARAM, "/");
        this.preCopy(creds);
        boolean acknowledgedAll = true; // NOPMD May be used later
        for (final ResearchDataInputStream entry : cacheElement.getTask().getElements()) {
            if (!this.copyFile(creds, targetDir, entry)) {
                acknowledgedAll = false;
            }
        }
        return "";
    }

    private Object postLogin(Request request, Response response) {
        final CacheElement elem = cacheMap.get(request.params(StoreConstants.SESSION_ID));
        if (elem == null) {
            response.status(404);
            return "Session does not exist.";
        }
        final E credentials = this.login(request, response);
        if (credentials == null) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Login failed for Session " + request.queryParams(StoreConstants.SESSION_ID));
            }
            return "Login failed";
        } else {
            elem.setCredentials(credentials);
            return "Login Successful";
        }
    }

    private Object getProgress(Request request, Response response) {
        CacheElement<E> cacheElement = cacheMap.get(request.params(StoreConstants.SESSION_ID));
        if (cacheElement == null) {
            response.status(404);
            return "Session does not exist.";
        }
        final List<ResearchDataInputStream> elems = cacheElement.getTask().getElements();
        return GSON.toJson(elems);
    }

    private Object getLoggedIn(Request request, Response response)  {
        final CacheElement<E> element = cacheMap.get(request.params(StoreConstants.SESSION_ID));
        if (element == null) {
            response.status(404);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.warn("Attempt to access non-existent Session " + request.params(StoreConstants.SESSION_ID));
            }
            return "Session ID does not exist";
        }
        final boolean loggedIn = this.isLoggedIn(element.getCredentials());
        return String.format(StoreConstants.IS_LOGGED_IN_RESPONSE, loggedIn);
    }

}
