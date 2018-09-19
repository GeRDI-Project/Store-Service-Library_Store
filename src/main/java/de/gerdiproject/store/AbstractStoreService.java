package de.gerdiproject.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.gerdiproject.store.datamodel.*;
import de.gerdiproject.store.handler.PostRootRoute;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.util.*;

import static spark.Spark.*;

public abstract class AbstractStoreService<E extends Credentials> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractStoreService.class);
    private final Map<String, CacheElement<E>> CACHE_MAP = new HashMap<>(); // Possible to abstract here; allows for different implementations
    private final Properties props;

    public AbstractStoreService(Properties props) {
        this.props = props;
    }

    protected abstract boolean isLoggedIn(E creds);

    protected abstract E login(Request req, Response res);

    protected abstract boolean copyFile(E creds, String targetDir, TaskElement taskElement);

    protected abstract List<ListElement> listFiles(String directory, E creds);

    protected void preCopy(StoreTask task){};

    protected void registerStaticFolder(String folder){
        staticFiles.location(folder);
    }

    protected void run() {
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
            val elem = CACHE_MAP.get(request.params("sessionId")).getProgress().values().toArray();
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
            E creds = CACHE_MAP.get(request.params("sessionId")).getCredentials();
            if(creds == null) {
                response.status(403);
                return "Not logged in";
            }
            val ret = listFiles(dir, creds);
            return new Gson().toJson(ret);
        });
    }
}
