     }
 
     public boolean hasMatchingTags(Set<String> tags) {
        return EnvironmentUtils.hasMatchingTags(shardingTags(), tags) || organizations().map(orgs -> orgs.contains("default")).orElse(false);
     }
 
     public boolean allowOverlappingApiContexts() {
