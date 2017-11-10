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

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLSocket;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

/**
 * Client to make Continuum REST API calls
 */
public final class ContinuumClient {

    /**
     * HTTP Connection and read timeout default.
     */
    private static final Integer DEFAULT_TIMEOUT = 10000;

    public static String post(String serverUrl, String apiToken, String command, String payload) throws Exception {
        StringBuffer apiUrl = new StringBuffer(serverUrl);
        if (!serverUrl.endsWith("/")) {
            apiUrl = apiUrl.append("/");
        }
        apiUrl = apiUrl
                .append(ContinuumConstants.PATH_API)
                .append(command)
                ;

        CloseableHttpClient httpClient = getHttpClient();
        CloseableHttpResponse httpResponse = null;

        try {
            HttpPost post = new HttpPost(apiUrl.toString());
            if (apiToken != null) {
                post.addHeader(HttpHeaders.AUTHORIZATION, "Token " + apiToken);
            }
            post.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            httpResponse = httpClient.execute(post);
            int responseCode = httpResponse.getStatusLine().getStatusCode();
            // Always read response to ensure the inputstream is closed
            String response = readResponse(httpResponse.getEntity());
            if ((responseCode / 100) != 2) {
                // TODO parse error
                throw new IOException(httpResponse.getStatusLine().getReasonPhrase());
            }
            return response;
        } finally {
            closeQuietly(httpResponse);
            closeQuietly(httpClient);
        }
    }

    protected static CloseableHttpClient getHttpClient() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                        .setConnectTimeout(DEFAULT_TIMEOUT).setSocketTimeout(DEFAULT_TIMEOUT).build())
                .setSSLSocketFactory(new TLSSocketFactory());

        /* TODO handle proxy
        if (Jenkins.getInstance() != null) {
            ProxyConfiguration proxy = Jenkins.getInstance().proxy;
            if (proxy != null) {
                httpClientBuilder.setRoutePlanner(new ProxyRoutePlanner(proxy));
                if (Util.fixEmpty(proxy.getUserName()) != null) {
                    BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(new AuthScope(proxy.name, proxy.port),
                            new UsernamePasswordCredentials(proxy.getUserName(), proxy.getPassword()));
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    httpClientBuilder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                }
            }
        }
        */

        return httpClientBuilder.build();
    }

    private static String readResponse(HttpEntity entity) throws IOException {
        return entity != null ? EntityUtils.toString(entity) : null;
    }

    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ioe) {
                //ignore quietly
            }
        }
    }

    private static class TLSSocketFactory extends SSLConnectionSocketFactory {

        public TLSSocketFactory() {
            super(SSLContexts.createDefault(), getDefaultHostnameVerifier());
        }

        @Override
        protected void prepareSocket(SSLSocket socket) throws IOException {
            String[] supportedProtocols = socket.getSupportedProtocols();
            List<String> protocols = new ArrayList<String>(5);
            for (String supportedProtocol : supportedProtocols) {
                if (!supportedProtocol.startsWith("SSL")) {
                    protocols.add(supportedProtocol);
                }
            }
            socket.setEnabledProtocols(protocols.toArray(new String[protocols.size()]));
        }
    }
}
