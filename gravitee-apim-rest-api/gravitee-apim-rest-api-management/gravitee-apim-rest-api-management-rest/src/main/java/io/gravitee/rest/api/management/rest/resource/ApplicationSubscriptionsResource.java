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
package io.gravitee.rest.api.management.rest.resource;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.rest.model.Subscription;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Tag(name = "Application Subscriptions")
public class ApplicationSubscriptionsResource extends AbstractResource {

    @Inject
    private SubscriptionService subscriptionService;

    @Inject
    private UserService userService;

    @Inject
    private ParameterService parameterService;

    @Context
    private ResourceContext resourceContext;

    @SuppressWarnings("UnresolvedRestParam")
    @PathParam("application")
    @Parameter(name = "application", hidden = true)
    private String application;

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Subscribe to a plan", description = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service")
    @ApiResponse(
        responseCode = "201",
        description = "Subscription successfully created",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.CREATE) })
    public Response createSubscriptionWithApplication(
        @Parameter(name = "plan", required = true) @NotNull @QueryParam("plan") String plan,
        @Parameter(name = "customApiKey") @QueryParam("customApiKey") String customApiKey,
        @Valid NewSubscriptionConfigurationEntity newSubscriptionConfigurationEntity
    ) {
        NewSubscriptionEntity newSubscriptionEntity = new NewSubscriptionEntity();
        newSubscriptionEntity.setApplication(application);
        newSubscriptionEntity.setPlan(plan);
        newSubscriptionEntity.setConfiguration(newSubscriptionConfigurationEntity);

        SubscriptionEntity subscription = subscriptionService.create(
            GraviteeContext.getExecutionContext(),
            newSubscriptionEntity,
            customApiKey
        );

        if (subscription.getStatus() == SubscriptionStatus.PENDING) {
            return Response.status(Response.Status.OK).entity(convert(subscription)).build();
        }

        return Response
            .created(URI.create("/applications/" + application + "/subscriptions/" + subscription.getId()))
            .entity(convert(subscription))
            .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "List subscriptions for the application",
        description = "User must have the READ_SUBSCRIPTION permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Paged result of application's subscriptions",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = PageSubscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public PageSubscription getSubscriptionsForApplicationSubscription(
        @QueryParam("page") @DefaultValue("1") int page,
        @QueryParam("size") @DefaultValue("10") int size,
        @Parameter(
            name = "status",
            explode = Explode.FALSE,
            schema = @Schema(type = "array", implementation = SubscriptionStatus.class)
        ) @QueryParam("status") SubscriptionListItem.SubscriptionStatusListParam statuses,
        @QueryParam("api_key") String apiKey
    ) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplication(application);

        if (statuses != null && !statuses.getStatus().isEmpty()) {
            subscriptionQuery.setStatuses(statuses.getStatus());
        }

        if (apiKey != null) {
            subscriptionQuery.setApiKey(apiKey);
        }

        Page<SubscriptionEntity> subscriptions = subscriptionService.search(
            GraviteeContext.getExecutionContext(),
            subscriptionQuery,
            new PageableImpl(page, size)
        );

        PageSubscription pageSubscription = new PageSubscription();
        pageSubscription.setData(
            subscriptions.getContent().stream().map(this::convert).collect(Collectors.toList())
        );
        pageSubscription.setMetadata(subscriptions.getMetadata());
        pageSubscription.setPage(
            new PageSubscription.PageItem(subscriptions.getPageNumber(), (int) subscriptions.getPageElements(), subscriptions.getTotalElements())
        );

        return pageSubscription;
    }

    @GET
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Get subscription information",
        description = "User must have the READ permission to use this service"
    )
    @ApiResponse(
        responseCode = "200",
        description = "Subscription information",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class))
    )
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.READ) })
    public Subscription getSubscriptionForApplicationSubscription(
        @PathParam("subscription") String subscriptionId
    ) {
        return convert(subscriptionService.findById(subscriptionId));
    }

    @DELETE
    @Path("{subscription}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Close the subscription",
        description = "User must have the MANAGE_SUBSCRIPTIONS permission to use this service"
    )
    @ApiResponse(responseCode = "200", description = "Subscription successfully closed",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subscription.class)))
    @ApiResponse(responseCode = "500", description = "Internal server error")
    @Permissions({ @Permission(value = RolePermission.APPLICATION_SUBSCRIPTION, acls = RolePermissionAction.DELETE) })
    public Response closeSubscriptionForApplicationSubscription(
        @PathParam("subscription") String subscriptionId
    ) {
        SubscriptionEntity subscription = subscriptionService.close(GraviteeContext.getExecutionContext(), subscriptionId);
        return Response.ok(convert(subscription)).build();
    }

    @Path("{subscription}/apikeys")
    public ApplicationSubscriptionApiKeysResource getApplicationSubscriptionApiKeysResource() {
        return resourceContext.getResource(ApplicationSubscriptionApiKeysResource.class);
    }

    private Subscription convert(SubscriptionEntity subscriptionEntity) {
        Subscription subscription = new Subscription();

        subscription.setId(subscriptionEntity.getId());
        subscription.setApi(subscriptionEntity.getApi());
        subscription.setPlan(subscriptionEntity.getPlan());
        subscription.setProcessedAt(subscriptionEntity.getProcessedAt());
        subscription.setStatus(subscriptionEntity.getStatus());
        subscription.setProcessedBy(subscriptionEntity.getProcessedBy());
        subscription.setRequest(subscriptionEntity.getRequest());
        subscription.setReason(subscriptionEntity.getReason());
        subscription.setApplication(
            new Subscription.Application(
                subscriptionEntity.getApplication(),
                "",
                "",
                "",
                null,
                null
            )
        );
        subscription.setClosedAt(subscriptionEntity.getClosedAt());
        subscription.setCreatedAt(subscriptionEntity.getCreatedAt());
        subscription.setEndingAt(subscriptionEntity.getEndingAt());
        subscription.setPausedAt(subscriptionEntity.getPausedAt());
        subscription.setStartingAt(subscriptionEntity.getStartingAt());
        subscription.setSubscribedBy(
            new Subscription.User(
                subscriptionEntity.getSubscribedBy(),
                userService.findById(GraviteeContext.getExecutionContext(), subscriptionEntity.getSubscribedBy()).getDisplayName()
            )
        );

        return subscription;
    }

    private static class PageSubscription {
        private List<Subscription> data;
        private java.util.Map<String, java.util.Map<String, Object>> metadata;
        private PageItem page;

        public List<Subscription> getData() {
            return data;
        }

        public void setData(List<Subscription> data) {
            this.data = data;
        }

        public java.util.Map<String, java.util.Map<String, Object>> getMetadata() {
            return metadata;
        }

        public void setMetadata(java.util.Map<String, java.util.Map<String, Object>> metadata) {
            this.metadata = metadata;
        }

        public PageItem getPage() {
            return page;
        }

        public void setPage(PageItem page) {
            this.page = page;
        }

        static class PageItem {
            private int current;
            private int perPage;
            private long totalElements;

            public PageItem(int current, int perPage, long totalElements) {
                this.current = current;
                this.perPage = perPage;
                this.totalElements = totalElements;
            }

            public int getCurrent() {
                return current;
            }

            public void setCurrent(int current) {
                this.current = current;
            }

            public int getPerPage() {
                return perPage;
            }

            public void setPerPage(int perPage) {
                this.perPage = perPage;
            }

            public long getTotalElements() {
                return totalElements;
            }

            public void setTotalElements(long totalElements) {
                this.totalElements = totalElements;
            }
        }
    }
}
