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

public final class ContinuumConstants {
    public static final String PATH_API = "api/";
    public static final String COMMAND_PARAMETER__DEFINITION = "definition"; 
    public static final String COMMAND_PARAMETER__DETAILS = "details"; 
    public static final String COMMAND_PARAMETER__GROUP = "group"; 
    public static final String COMMAND_PARAMETER__KEY = "key"; 
    public static final String COMMAND_PARAMETER__INSTANCE_NAME = "instance_name"; 
    public static final String COMMAND_PARAMETER__PI = "pi"; 
    public static final String COMMAND_PARAMETER__PROJECT = "project"; 
    public static final String COMMAND_PARAMETER__VALUE = "value";
    
    public static final String COMMAND_NAME__INITIATE_PIPELINE = "initiate_pipeline";
    public static final String COMMAND_NAME__POST_PI_DATA = "post_pi_data";
    public static final String COMMAND_NAME__SET_PI_DATA = "set_pi_data";

    public static final String ENV_VARIABLE__SERVER_URL = "CTM_SERVER_URL";
    public static final String ENV_VARIABLE__CREDENTIAL_ID = "CTM_CRED_ID";
}
