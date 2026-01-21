import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import io.gravitee.repository.management.model.Organization;

public class OrganizationTest {

    @Test
    public void testDefaultOrganizationCanBeTagged() {
        Organization defaultOrg = Organization.DEFAULT;
        assertNotNull(defaultOrg);
        assertEquals("DEFAULT", defaultOrg.getId());
    }
}
