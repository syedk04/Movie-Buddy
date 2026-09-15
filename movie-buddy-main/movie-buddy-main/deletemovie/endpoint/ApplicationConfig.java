package ryerson.ca.deletemovie.endpoint;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.Application;

@javax.ws.rs.ApplicationPath("webresources")  // Defines base path for REST API
public class ApplicationConfig extends Application {
    
    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> resources = new HashSet<>();
        addRestResourceClasses(resources);
        return resources;
    }

    private void addRestResourceClasses(Set<Class<?>> resources) {
        resources.add(ryerson.ca.deletemovie.endpoint.DeletemovieResource.class);
    }
}
