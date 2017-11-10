/*
 * Copyright 2017 CollabNet, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jenkins.plugins.continuum;

import java.util.Map;
import java.util.Set;

import hudson.model.Run;
import net.sf.json.JSONObject;

/**
 * Base class for continuum command input builders
 */
public abstract class CommandInputBuilder implements BuildToContinuumAPI {

    @Override
    public String toContinuumAPI(Run build, Map<String, Object> commandParameters) {
        verifyRequiredParameters(commandParameters);
        return toJsonString(commandParameters);
    }

    protected abstract Set<String> getRequiredParamaterNames();

    protected String toJsonString(Map<String, Object> commandParameters) {
        JSONObject response = JSONObject.fromObject(commandParameters);
        return response.toString();
    }

    protected void verifyRequiredParameters(Map<String, Object> commandParameters) {
        Set<String> requiredParamNames = getRequiredParamaterNames();
        if (requiredParamNames == null || requiredParamNames.isEmpty()) {
            return;
        }
        Set<String> keys = commandParameters.keySet();
        if (!keys.containsAll(requiredParamNames)) {
            // TODO throw exception
        }
    }

}
