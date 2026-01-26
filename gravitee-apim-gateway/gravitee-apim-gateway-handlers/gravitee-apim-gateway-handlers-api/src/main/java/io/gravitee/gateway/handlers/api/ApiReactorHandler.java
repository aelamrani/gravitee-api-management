/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
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
package io.gravitee.gateway.handlers.api;

import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.context.MutableExecutionContext;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.processor.ProcessorFailure;
import io.gravitee.gateway.api.proxy.ProxyConnection;
import io.gravitee.gateway.api.proxy.ProxyResponse;
import io.gravitee.gateway.core.endpoint.lifecycle.GroupLifecycleManager;
import io.gravitee.gateway.core.invoker.EndpointInvoker;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.handlers.api.definition.Api;
import io.gravitee.gateway.handlers.api.processor.OnErrorProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.RequestProcessorChainFactory;
import io.gravitee.gateway.handlers.api.processor.ResponseProcessorChainFactory;
import io.gravitee.gateway.policy.PolicyManager;
import io.gravitee.gateway.reactor.handler.HttpAcceptorFactory;
import io.gravitee.gateway.reactor.handler.context.V3ExecutionContextFactory;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.node.api.configuration.Configuration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiReactorHandler extends AbstractApiReactorHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiReactorHandler.class);
    private static final String FILE_UPLOAD_AUDIT_EVENT = "FILE_UPLOAD";

    private Invoker invoker;

    private RequestProcessorChainFactory requestProcessorChain;
    private ResponseProcessorChainFactory responseProcessorChain;
    private OnErrorProcessorChainFactory errorProcessorChain;

    private GroupLifecycleManager groupLifecycleManager;
    private PolicyManager policyManager;
    private ResourceLifecycleManager resourceLifecycleManager;

    private V3ExecutionContextFactory executionContextFactory;

    public ApiReactorHandler(
        Configuration configuration,
        Api api,
        AccessPointManager accessPointManager,
        EventManager eventManager,
        HttpAcceptorFactory httpAcceptorFactory
    ) {
        super(configuration, api, accessPointManager, eventManager, httpAcceptorFactory);
    }

    @Override
    protected void handleRequest(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Set the invoker (theைone responsible for calling the backend)
        context.setAttribute(ExecutionContext.ATTR_INVOKER, invoker);

        // Log file upload activities for audit purposes
        logFileUploadActivity(context);

        // Apply request policies
        requestProcessorChain
            .create()
            .handler(__ -> handleProxyRequest(context, endHandler))
            .errorHandler(failure -> handleError(context, failure, endHandler))
            .exitHandler(__ -> handleClientExit(context, endHandler))
            .handle(context);
    }

    /**
     * Logs file upload activities for audit and security monitoring purposes.
     *
     * @param context the execution context
     */
    private void logFileUploadActivity(MutableExecutionContext context) {
        String contentType = context.request().headers().getFirst("Content-Type");
        if (contentType != null && isFileUploadContentType(contentType)) {
            String apiId = (String) context.getAttribute(ExecutionContext.ATTR_API);
            String clientId = (String) context.getAttribute(ExecutionContext.ATTR_CLIENT_IDENTIFIER);
            String remoteAddress = context.request().remoteAddress();
            String path = context.request().path();
            long contentLength = context.request().headers().contentLength();

            LOGGER.info(
                "[AUDIT] {} - API: {}, Client: {}, Remote Address: {}, Path: {}, Content-Type: {}, Content-Length: {}",
                FILE_UPLOAD_AUDIT_EVENT,
                apiId != null ? apiId : "unknown",
                clientId != null ? clientId : "unknown",
                remoteAddress != null ? remoteAddress : "unknown",
                path,
                contentType,
                contentLength
            );
        }
    }

    /**
     * Checks if the content type indicates a file upload.
     *
     * @param contentType the content type header value
     * @return true if this is a file upload content type
     */
    private boolean isFileUploadContentType(String contentType) {
        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.startsWith("multipart/form-data") ||
               lowerContentType.startsWith("application/octet-stream") ||
               lowerContentType.startsWith("application/pdf") ||
               lowerContentType.startsWith("image/");
    }

    private void handleProxyRequest(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Call the invoker to get a proxy connection (connection to the backend)
        Invoker upstreamInvoker = (Invoker) context.getAttribute(ExecutionContext.ATTR_INVOKER);

        final long serviceInvocationStart = System.currentTimeMillis();

        ProxyConnection proxyConnection = upstreamInvoker.invoke(context);

        context.request().bodyHandler(buffer -> proxyConnection.write(buffer));

        context.request().endHandler(result -> proxyConnection.end());

        proxyConnection.exceptionHandler(throwable -> handleException(context, throwable, endHandler));

        proxyConnection.responseHandler(proxyResponse -> handleProxyResponse(context, proxyResponse, serviceInvocationStart, endHandler));
    }

    private void handleProxyResponse(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        long serviceInvocationStart,
        Handler<ExecutionContext> endHandler
    ) {
        if (proxyResponse == null) {
            handleError(context, null, endHandler);
            return;
        }

        handleProxyResponseHeaders(context, proxyResponse, serviceInvocationStart);

        responseProcessorChain
            .create()
            .handler(handleProxyResponseBody(context, proxyResponse, endHandler))
            .errorHandler(failure -> handleError(context, failure, endHandler))
            .exitHandler(__ -> handleClientExit(context, endHandler))
            .handle(context);
    }

    private void handleProxyResponseHeaders(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        long serviceInvocationStart
    ) {
        context.response().status(proxyResponse.status());
        proxyResponse.headers().forEach((name, values) -> context.response().headers().set(name, values));

        long serviceInvocationEnd = System.currentTimeMillis();
        context.setAttribute(ExecutionContext.ATTR_INVOKER_RESPONSE_TIME, serviceInvocationEnd - serviceInvocationStart);
    }

    private Handler<ExecutionContext> handleProxyResponseBody(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        Handler<ExecutionContext> endHandler
    ) {
        return __ -> {
            proxyResponse.bodyHandler(buffer -> context.response().write(buffer));

            proxyResponse.endHandler(result -> {
                context.response().end();
                endHandler.handle(context);
            });
        };
    }

    private void handleError(
        MutableExecutionContext context,
        ProcessorFailure failure,
        Handler<ExecutionContext> endHandler
    ) {
        context.setAttribute(ExecutionContext.ATTR_FAILURE, failure);

        errorProcessorChain
            .create()
            .handler(__ -> {
                if (failure != null) {
                    context.response().status(failure.statusCode());
                    if (failure.message() != null) {
                        context.response().write(Buffer.buffer(failure.message()));
                    }
                } else {
                    context.response().status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                }
                context.response().end();
                endHandler.handle(context);
            })
            .errorHandler(__ -> {
                context.response().status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                context.response().end();
                endHandler.handle(context);
            })
            .handle(context);
    }

    private void handleException(
        MutableExecutionContext context,
        Throwable throwable,
        Handler<ExecutionContext> endHandler
    ) {
        LOGGER.error("An error occurred while invoking the backend", throwable);
        handleError(context, null, endHandler);
    }

    private void handleClientExit(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        endHandler.handle(context);
    }

    @Override
    protected V3ExecutionContextFactory executionContextFactory() {
        return executionContextFactory;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        resourceLifecycleManager.start();
        policyManager.start();
        groupLifecycleManager.start();

        if (invoker instanceof EndpointInvoker) {
            ((EndpointInvoker) invoker).setGroupManager(groupLifecycleManager);
        }

        dumpAcceptors();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        groupLifecycleManager.stop();
        policyManager.stop();
        resourceLifecycleManager.stop();
    }

    @Override
    public String toString() {
        return "ApiReactorHandler{" + "api=" + reactableApi.getId() + ", contextPath=" + reactableApi.getDefinition().getProxy().getVirtualHosts() + "}";
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public void setRequestProcessorChain(RequestProcessorChainFactory requestProcessorChain) {
        this.requestProcessorChain = requestProcessorChain;
    }

    public void setResponseProcessorChain(ResponseProcessorChainFactory responseProcessorChain) {
        this.responseProcessorChain = responseProcessorChain;
    }

    public void setErrorProcessorChain(OnErrorProcessorChainFactory errorProcessorChain) {
        this.errorProcessorChain = errorProcessorChain;
    }

    public void setGroupLifecycleManager(GroupLifecycleManager groupLifecycleManager) {
        this.groupLifecycleManager = groupLifecycleManager;
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    public void setResourceLifecycleManager(ResourceLifecycleManager resourceLifecycleManager) {
        this.resourceLifecycleManager = resourceLifecycleManager;
    }

    public void setExecutionContextFactory(V3ExecutionContextFactory executionContextFactory) {
        this.executionContextFactory = executionContextFactory;
    }
}
