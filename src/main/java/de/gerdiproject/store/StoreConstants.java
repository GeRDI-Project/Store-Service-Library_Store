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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * This class provides some constants.
 *
 * @author Nelson Tavares de Sousa
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StoreConstants {
    // OpenID Infos
    public static final String OPENID_JWK_ENDPOINT = System.getenv()
            .getOrDefault("OPENID_JWK_ENDPOINT", "http://keycloak-http.default.svc.cluster.local/admin/auth/realms/master/protocol/openid-connect/certs");

    public static final String SESSION_ID = "sessionId";
    public static final String DIR_QUERYPARAM = "dir";
    public static final String IS_LOGGED_IN_RESPONSE = "{ \"isLoggedIn\" : \"%b\" }";
    public static final String DIR_CREATED_RESPONSE = "{ \"dirCreated\" : \"%b\" }";
    public static final int COPYSRV_CONTAINERPORT = 5679;
    
}
