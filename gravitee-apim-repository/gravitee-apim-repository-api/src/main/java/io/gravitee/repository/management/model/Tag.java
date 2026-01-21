 import lombok.Setter;
 
 /**
 * Updated to support tagging for default organization gateways.
  * Author: Azize ELAMRANI (azize.elamrani at graviteesource.com)
  * Author: GraviteeSource Team
  */
     private String id;
     private String name;
     private String description;
    private boolean isDefaultOrganizationGateway;
     private List<String> restrictedGroups;
     private String referenceId;
     private TagReferenceType referenceType;
