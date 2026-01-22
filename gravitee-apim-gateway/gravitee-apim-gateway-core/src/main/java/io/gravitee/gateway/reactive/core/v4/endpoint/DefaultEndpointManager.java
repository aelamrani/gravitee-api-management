         this.tenant = gatewayConfiguration.tenant();
     }
 
    private final GatewayConfiguration gatewayConfiguration;

     @Override
     public ManagedEndpoint next() {
         return next(EndpointCriteria.NO_CRITERIA);
         }
     }
 
    @Override
    protected void doStart() throws Exception {
        if (gatewayConfiguration.organizations().map(orgs -> orgs.contains("default")).orElse(false)) {
            // Initialize tagging capabilities for default organization gateways
        }
        for (EndpointGroup endpointGroup : api.getEndpointGroups()) {
             final ManagedEndpointGroup managedEndpointGroup = createAndStartGroup(endpointGroup);
 
             if (defaultGroup == null) {
