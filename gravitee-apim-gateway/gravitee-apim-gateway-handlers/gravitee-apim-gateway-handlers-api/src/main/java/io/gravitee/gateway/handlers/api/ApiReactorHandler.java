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
import io.gravitee.gateway.core.logging.LoggingContext;
import io.gravitee.gateway.core.logging.utils.LoggingUtils;
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
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.reporter.api.v4.metric.Metrics;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiReactorHandler extends AbstractApiReactorHandler {

    private final Logger logger = LoggerFactory.getLogger(ApiReactorHandler.class);

    private RequestProcessorChainFactory requestProcessorChainFactory;
    private ResponseProcessorChainFactory responseProcessorChainFactory;
    private OnErrorProcessorChainFactory errorProcessorChainFactory;
    private Invoker invoker;
    private PolicyManager policyManager;
    private GroupLifecycleManager groupLifecycleManager;
    private ResourceLifecycleManager resourceLifecycleManager;
    private V3ExecutionContextFactory executionContextFactory;
    private Node node;

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
    protected void doHandle(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Set the logging context if needed
        LoggingContext loggingContext = LoggingUtils.getLoggingContext(reactableApi.getDefinition());
        if (loggingContext != null) {
            context.request().customFrameHandler(new LogRequestDebugHandler(logger, context));
            context.setAttribute(LoggingContext.LOGGING_ATTRIBUTE, loggingContext);
        }

        // Apply request processors
        requestProcessorChainFactory
            .create()
            .handler(__ -> handleProxyInvocation(context, endHandler))
            .errorHandler(failure -> handleProcessorFailure(failure, context, endHandler))
            .exitHandler(__ -> handleProcessorExit(context, endHandler))
            .handle(context);
    }

    private void handleProxyInvocation(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Call the invoker
        invoker.invoke(
            context,
            context.request().createReadStream(),
            connectionHandler -> {
                if (connectionHandler instanceof ProxyConnection) {
                    handleProxyConnection(context, (ProxyConnection) connectionHandler, endHandler);
                } else {
                    handleProxyResponse(context, (ProxyResponse) connectionHandler, endHandler);
                }
            }
        );
    }

    private void handleProxyConnection(
        MutableExecutionContext context,
        ProxyConnection proxyConnection,
        Handler<ExecutionContext> endHandler
    ) {
        proxyConnection.responseHandler(proxyResponse -> handleProxyResponse(context, proxyResponse, endHandler));

        proxyConnection.exceptionHandler(failure -> {
            context.request().metrics().setMessage(failure.getMessage());
            handleProxyError(context, endHandler);
        });
    }

    private void handleProxyResponse(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        Handler<ExecutionContext> endHandler
    ) {
        if (proxyResponse == null) {
            handleProxyError(context, endHandler);
        } else {
            handleProxyResponseSuccess(context, proxyResponse, endHandler);
        }
    }

    private void handleProxyResponseSuccess(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        Handler<ExecutionContext> endHandler
    ) {
        // Set the proxy response on the context
        context.setAttribute(ExecutionContext.ATTR_INVOKER_RESPONSE, proxyResponse);

        // Apply response processors
        StreamableProcessor<ExecutionContext, Buffer> responseProcessor = responseProcessorChainFactory.create();
        responseProcessor
            .handler(__ -> handleProcessorSuccess(context, proxyResponse, endHandler))
            .errorHandler(failure -> handleProcessorFailure(failure, context, endHandler))
            .exitHandler(__ -> handleProcessorExit(context, endHandler))
            .streamErrorHandler(failure -> handleStreamError(failure, context, proxyResponse, endHandler));

        // Plug the response processor to the proxy response
        proxyResponse.bodyHandler(responseProcessor::write).endHandler(result -> responseProcessor.end());

        responseProcessor.handle(context);
    }

    private void handleProcessorSuccess(
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        Handler<ExecutionContext> endHandler
    ) {
        // Write response headers
        context.response().headers().forEach((name, values) -> values.forEach(value -> context.response().writeHeader(name, value)));

        // End the response
        context.response().end();

        // Clean up
        proxyResponse.cancel();

        endHandler.handle(context);
    }

    private void handleProcessorFailure(
        ProcessorFailure failure,
        MutableExecutionContext context,
        Handler<ExecutionContext> endHandler
    ) {
        context.request().metrics().setErrorKey(failure.key());
        context.request().metrics().setMessage(failure.message());

        // Apply error processors
        errorProcessorChainFactory
            .create()
            .handler(__ -> handleErrorResponse(context, failure, endHandler))
            .errorHandler(__ -> handleErrorResponse(context, failure, endHandler))
            .exitHandler(__ -> handleProcessorExit(context, endHandler))
            .handle(context);
    }

    private void handleErrorResponse(
        MutableExecutionContext context,
        ProcessorFailure failure,
        Handler<ExecutionContext> endHandler
    ) {
        context.response().status(failure.statusCode());
        context.response().reason(failure.message());

        // Write response headers
        context.response().headers().forEach((name, values) -> values.forEach(value -> context.response().writeHeader(name, value)));

        // Write error content if any
        if (failure.contentType() != null) {
            context.response().headers().set(io.gravitee.common.http.HttpHeaders.CONTENT_TYPE, failure.contentType());
        }

        if (failure.message() != null) {
            context.response().write(Buffer.buffer(failure.message()));
        }

        context.response().end();

        endHandler.handle(context);
    }

    private void handleProcessorExit(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        // Write response headers
        context.response().headers().forEach((name, values) -> values.forEach(value -> context.response().writeHeader(name, value)));

        context.response().end();

        endHandler.handle(context);
    }

    private void handleProxyError(MutableExecutionContext context, Handler<ExecutionContext> endHandler) {
        ProcessorFailure failure = new ProcessorFailure() {
            @Override
            public int statusCode() {
                return HttpStatusCode.BAD_GATEWAY_502;
            }

            @Override
            public String message() {
                return "Bad Gateway";
            }

            @Override
            public String key() {
                return "PROXY_ERROR";
            }

            @Override
            public String contentType() {
                return null;
            }

            @Override
            public Object parameters() {
                return null;
            }
        };

        handleProcessorFailure(failure, context, endHandler);
    }

    private void handleStreamError(
        ProcessorFailure failure,
        MutableExecutionContext context,
        ProxyResponse proxyResponse,
        Handler<ExecutionContext> endHandler
    ) {
        proxyResponse.cancel();
        handleProcessorFailure(failure, context, endHandler);
    }

    @Override
    protected V3ExecutionContextFactory executionContextFactory() {
        return executionContextFactory;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();

        // Start policy manager
        if (policyManager != null) {
            policyManager.start();
        }

        // Start group lifecycle manager
        if (groupLifecycleManager != null) {
            groupLifecycleManager.start();
        }

        // Start resource lifecycle manager
        if (resourceLifecycleManager != null) {
            resourceLifecycleManager.start();
        }

        // Configure invoker
        if (invoker instanceof EndpointInvoker) {
            ((EndpointInvoker) invoker).setGroupManager(groupLifecycleManager);
        }

        dumpAcceptors();
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        // Stop resource lifecycle manager
        if (resourceLifecycleManager != null) {
            resourceLifecycleManager.stop();
        }

        // Stop group lifecycle manager
        if (groupLifecycleManager != null) {
            groupLifecycleManager.stop();
        }

        // Stop policy manager
        if (policyManager != null) {
            policyManager.stop();
        }
    }

    @Override
    public String toString() {
        return "ApiReactorHandler{" + "api=" + reactableApi.getId() + ", contextPath=" + reactableApi.getDefinition().getProxy().getVirtualHosts() + '}';
    }

    public void setRequestProcessorChain(RequestProcessorChainFactory requestProcessorChainFactory) {
        this.requestProcessorChainFactory = requestProcessorChainFactory;
    }

    public void setResponseProcessorChain(ResponseProcessorChainFactory responseProcessorChainFactory) {
        this.responseProcessorChainFactory = responseProcessorChainFactory;
    }

    public void setErrorProcessorChain(OnErrorProcessorChainFactory errorProcessorChainFactory) {
        this.errorProcessorChainFactory = errorProcessorChainFactory;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    public void setGroupLifecycleManager(GroupLifecycleManager groupLifecycleManager) {
        this.groupLifecycleManager = groupLifecycleManager;
    }

    public void setResourceLifecycleManager(ResourceLifecycleManager resourceLifecycleManager) {
        this.resourceLifecycleManager = resourceLifecycleManager;
    }

    public void setExecutionContextFactory(V3ExecutionContextFactory executionContextFactory) {
        this.executionContextFactory = executionContextFactory;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    private static class LogRequestDebugHandler implements Handler<String> {

        private final Logger logger;
        private final ExecutionContext context;

        LogRequestDebugHandler(Logger logger, ExecutionContext context) {
            this.logger = logger;
            this.context = context;
        }

        @Override
        public void handle(String frame) {
            logger.debug("[{}] Received frame: {}", context.request().id(), frame);
        }
    }
}
