 import lombok.Setter;
 import lombok.ToString;
 
// Updated to handle tagging logic for default organization gateways.
 /**
  * Author: Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
  * Author: GraviteeSource Team
         this.deployedAt = new Date();
         this.enabled = true;
     }

    public boolean isDefaultOrganizationGateway() {
        return definition.isDefaultGateway();
    }

    // Additional methods to handle default organization gateway tagging logic can be added here.
 
     @Override
     public boolean enabled() {
