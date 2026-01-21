  */
 package io.gravitee.apim.core.tag.model;
 
import io.gravitee.repository.management.model.Organization;
 import java.util.List;
 import lombok.Builder;
 import lombok.Data;
 public class Tag {
 
     private String id;
    private String name;
    private String description;
    private List<String> restrictedGroups;
    private String referenceId;
    private TagReferenceType referenceType;
 
     public enum TagReferenceType {
         ORGANIZATION,
     }
 
    public boolean isApplicableToOrganization(Organization organization) {
        return organization != null && (organization.getId().equals(Organization.DEFAULT.getId()) || referenceId.equals(organization.getId()));
    }

 }
