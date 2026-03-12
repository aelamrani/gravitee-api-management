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
package io.gravitee.definition.model;

import io.gravitee.definition.model.flow.Flow;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Organization model representing an organization entity.
 * 
 * <p>Security Note: When serving content from the Gravitee Console UI, 
 * a strict Content-Security-Policy (CSP) should be applied to mitigate 
 * XSS and other browser-based attacks. The recommended CSP directives are:</p>
 * <ul>
 *   <li>default-src 'none' - Blocks all content by default</li>
 *   <li>script-src 'self' - Only allows scripts from the same origin</li>
 *   <li>style-src 'self' 'unsafe-inline' - Allows styles from same origin and inline styles</li>
 *   <li>img-src 'self' data: - Allows images from same origin and data URIs</li>
 *   <li>font-src 'self' - Allows fonts from same origin</li>
 *   <li>connect-src 'self' - Allows connections to same origin</li>
 *   <li>frame-ancestors 'none' - Prevents embedding in frames</li>
 *   <li>base-uri 'self' - Restricts base URI to same origin</li>
 *   <li>form-action 'self' - Restricts form submissions to same origin</li>
 *   <li>object-src 'none' - Blocks plugins like Flash</li>
 * </ul>
 * 
 * <p>Example CSP header value:</p>
 * <pre>
 * Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; 
 * img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; 
 * base-uri 'self'; form-action 'self'; object-src 'none'
 * </pre>
 */
public class Organization implements Serializable {

    /**
     * Default Content-Security-Policy header value for the Gravitee Console UI.
     * This strict CSP helps mitigate XSS and other browser-based attacks.
     */
    public static final String DEFAULT_CONSOLE_CSP = "default-src 'none'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'; object-src 'none'";

    private String id;
    private List<String> hrids;
    private String name;
    private String description;
    private List<String> domainRestrictions;
    private FlowMode flowMode;
    private List<Flow> flows = new ArrayList<>();
    private Date updatedAt;
    private String consoleContentSecurityPolicy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getHrids() {
        return hrids;
    }

    public void setHrids(List<String> hrids) {
        this.hrids = hrids;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getDomainRestrictions() {
        return domainRestrictions;
    }

    public void setDomainRestrictions(List<String> domainRestrictions) {
        this.domainRestrictions = domainRestrictions;
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    public List<Flow> getFlows() {
        return flows;
    }

    public void setFlows(List<Flow> flows) {
        this.flows = flows;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets the Content-Security-Policy for the Gravitee Console UI.
     * If not explicitly set, returns the default strict CSP.
     * 
     * @return the CSP header value to use for the Console UI
     */
    public String getConsoleContentSecurityPolicy() {
        return consoleContentSecurityPolicy != null ? consoleContentSecurityPolicy : DEFAULT_CONSOLE_CSP;
    }

    /**
     * Sets a custom Content-Security-Policy for the Gravitee Console UI.
     * 
     * @param consoleContentSecurityPolicy the CSP header value, or null to use the default
     */
    public void setConsoleContentSecurityPolicy(String consoleContentSecurityPolicy) {
        this.consoleContentSecurityPolicy = consoleContentSecurityPolicy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Organization organization = (Organization) o;
        return Objects.equals(id, organization.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toString() {
        return "Organization{" + "id='" + id + '\'' + ", name='" + name + '\'' + '}';
    }
}
