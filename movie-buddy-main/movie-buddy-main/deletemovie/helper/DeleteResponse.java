package ryerson.ca.deletemovie.helper;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DeleteResponse {
    private String type;
    private String message;
    
    public DeleteResponse() {
    }
    
    public DeleteResponse(String type, String message) {
        this.type = type;
        this.message = message;
    }
    
    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}