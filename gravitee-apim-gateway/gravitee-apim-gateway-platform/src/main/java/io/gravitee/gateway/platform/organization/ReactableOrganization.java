 import io.gravitee.definition.model.Organization;
 import io.gravitee.definition.model.Policy;
 import io.gravitee.definition.model.flow.Flow;
import io.gravitee.apim.core.tag.model.Tag;
 import io.gravitee.definition.model.flow.Step;
 import io.gravitee.gateway.reactor.Reactable;
 import java.io.Serializable;
         return isEnabled();
     }
 
    public boolean hasTag(Tag tag) {
        if (tag == null) {
            return false;
        }
        return tag.isApplicableToOrganization(definition);
    }

     @Override
     public <D> Set<D> dependencies(Class<D> type) {
         if (Policy.class.equals(type)) {
