/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.ballerinalang.net.websocket.serviceendpoint;

import io.ballerina.runtime.api.Environment;
import io.ballerina.runtime.api.Runtime;
import io.ballerina.runtime.api.types.MethodType;
import io.ballerina.runtime.api.types.Type;
import io.ballerina.runtime.api.values.BArray;
import io.ballerina.runtime.api.values.BError;
import io.ballerina.runtime.api.values.BObject;
import io.ballerina.runtime.api.values.BString;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpUtil;
import org.ballerinalang.net.websocket.WebSocketConstants;
import org.ballerinalang.net.websocket.WebSocketUtil;
import org.ballerinalang.net.websocket.server.WebSocketServerService;
import org.ballerinalang.net.websocket.server.WebSocketServicesRegistry;

import static org.ballerinalang.net.websocket.WebSocketConstants.FULL_WEBSOCKET_CLIENT_NAME;
import static org.ballerinalang.net.websocket.WebSocketConstants.WEBSOCKET_CALLER_NAME;

/**
 * Register a service to the listener.
 *
 */
public class Register extends AbstractWebsocketNativeFunction {
    public static Object register(Environment env, BObject serviceEndpoint, BObject service,
            Object serviceName) {

        WebSocketServicesRegistry webSocketServicesRegistry = getWebSocketServicesRegistry(serviceEndpoint);
        Runtime runtime = env.getRuntime();
        String basePath = getBasePath(serviceName);

        Type param;
        MethodType[] resourceList = service.getType().getMethods();
        try {
            if (resourceList.length > 0 && (param = resourceList[0].getParameterTypes()[0]) != null) {
                String callerType = param.getQualifiedName();
                if (WEBSOCKET_CALLER_NAME.equals(callerType) || HttpConstants.HTTP_CALLER_NAME.equals(callerType)) {
                    webSocketServicesRegistry
                            .registerService(new WebSocketServerService(service, runtime, basePath));
                } else if (FULL_WEBSOCKET_CLIENT_NAME.equals(callerType)) {
                    return WebSocketUtil.getWebSocketError(
                            "Client service cannot be attached to the Listener", null,
                            WebSocketConstants.ErrorCode.WsGenericError.errorCode(), null);
                } else {
                    return HttpUtil.createHttpError("Invalid websocket Service");
                }
            }
        } catch (BError ex) {
            return ex;
        }
        return null;
    }

    private static String getBasePath(Object serviceName) {
        if (serviceName instanceof BArray) {
            String basePath = String.join("/", ((BArray) serviceName).getStringArray());
            return HttpUtil.sanitizeBasePath(basePath);
        } else if (serviceName instanceof BString) {
            String basePath = ((BString) serviceName).getValue();
            return HttpUtil.sanitizeBasePath(basePath);
        } else {
            return HttpConstants.DEFAULT_BASE_PATH;
        }
    }
}
