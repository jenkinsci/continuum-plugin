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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Input builder for 'initiate_pipeline' command
 */
public class InitiatePipelineInputBuilder extends CommandInputBuilder {

    protected Set<String> getRequiredParamaterNames() {
        Set<String> result = new HashSet<String>();
        result.add(ContinuumConstants.COMMAND_PARAMETER__DEFINITION);
        result.add(ContinuumConstants.COMMAND_PARAMETER__GROUP);
        result.add(ContinuumConstants.COMMAND_PARAMETER__PROJECT);
        return Collections.unmodifiableSet(result);
    }
}
