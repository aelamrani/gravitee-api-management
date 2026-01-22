     }
 
     public boolean hasMatchingTags(Set<String> tags) {
        return shardingTags().map(tags::containsAll).orElse(true);
     }
 
     public boolean allowOverlappingApiContexts() {
