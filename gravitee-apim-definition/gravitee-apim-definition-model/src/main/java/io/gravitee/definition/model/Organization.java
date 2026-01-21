 import java.util.Date;
 import java.util.List;
 import java.util.Objects;

 public class Organization implements Serializable {
 
     private String id;
     private String name;
     private String description;
     private List<String> domainRestrictions;
    private boolean isDefaultGateway;
     private FlowMode flowMode;
     private List<Flow> flows = new ArrayList<>();
     private Date updatedAt;
         this.hrids = hrids;
     }
 
    public boolean isDefaultGateway() {
        return isDefaultGateway;
    }

    public void setDefaultGateway(boolean defaultGateway) {
        isDefaultGateway = defaultGateway;
    }

     public String getName() {
         return name;
     }
