package ryerson.ca.business;

import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XMLParser {

    private static final short TEXT = 3;

    public static String ConvertXmlToHtmlTable(String xml) {
        StringBuilder html = new StringBuilder("<table border=\"1\" cellpadding=\"5\" cellspacing=\"0\">\r\n");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();

            NodeList movies = root.getElementsByTagName("movie");

            for (int i = 0; i < movies.getLength(); i++) {
                Node movie = movies.item(i);
                if (movie.getNodeType() == Node.ELEMENT_NODE) {
                    html.append("<tr>");
                    html.append(getMovieDetails(movie));
                    html.append("</tr>");
                }
            }

            html.append("</table>");
        } catch (Exception e) {
            e.printStackTrace();
            return xml; // Return the original XML in case of error
        }
        return html.toString();
    }

    private static String getMovieDetails(Node movie) {
        StringBuilder html = new StringBuilder();
        NodeList childNodes = movie.getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = child.getNodeName();
                String textContent = child.getTextContent();
                html.append("<td>").append(tagName).append(": ").append(textContent).append("</td>");
            }
        }

        return html.toString();
    }
}