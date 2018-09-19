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
