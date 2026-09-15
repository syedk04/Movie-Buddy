package ryerson.ca.addservice.endpoint;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationPath("webresources")
public class ApplicationConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new java.util.HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(ryerson.ca.addservice.endpoint.AddResource.class);
    }
}
