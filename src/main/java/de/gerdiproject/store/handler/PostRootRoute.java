package de.gerdiproject.store.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.gerdiproject.store.datamodel.CacheElement;
import de.gerdiproject.store.datamodel.Credentials;
import de.gerdiproject.store.datamodel.StoreTask;
import de.gerdiproject.store.util.RandomString;
import spark.Request;
import spark.Response;
import spark.Route;

import java.util.Map;

public class PostRootRoute<E extends Credentials> implements Route {

    private static final RandomString RDM_GENERATOR = new RandomString();
    private final Map<String, CacheElement<E>> cacheMap;
    private final Gson gson = new GsonBuilder().create();

    public PostRootRoute( Map<String, CacheElement<E>> cacheMap){
        this.cacheMap = cacheMap;
    }

    @Override
    public Object handle(Request request, Response response) throws Exception {
        StoreTask input = gson.fromJson(request.body(), StoreTask.class);
        if (input.getDocs().isEmpty() || input.getUserId() == null) {
            response.status(400);
            return null;
        }
        String id = RDM_GENERATOR.nextString();
        cacheMap.put(id, new CacheElement(input));
        response.status(201);
        return "{ \"sessionId\": \"" + id + "\" }";
    }
}
