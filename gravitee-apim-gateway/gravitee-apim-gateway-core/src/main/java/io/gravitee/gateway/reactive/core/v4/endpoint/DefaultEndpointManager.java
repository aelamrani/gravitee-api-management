         for (EndpointGroup endpointGroup : api.getEndpointGroups()) {
             final ManagedEndpointGroup managedEndpointGroup = createAndStartGroup(endpointGroup);
 
            if (defaultGroup == null && gatewayConfiguration.hasMatchingTags(endpointGroup.getTags())) {
                defaultGroup = managedEndpointGroup;
            }

             if (defaultGroup == null) {
                 defaultGroup = managedEndpointGroup;
             }
