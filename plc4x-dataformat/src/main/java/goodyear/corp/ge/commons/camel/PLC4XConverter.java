package goodyear.corp.ge.commons.camel;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.camel.Converter;
import org.apache.camel.Exchange;
import org.apache.camel.spi.DataFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import org.apache.plc4x.camel.*;

/**
 * A <a href="http://camel.apache.org/data-format.html">data format</a>
 * ({@link DataFormat}) for plc4x ({@link TagData}).
 */
@Converter
public final class PLC4XConverter {

    static Logger logger = LoggerFactory.getLogger(PLC4XConverter.class);

    private static String ROOT_TAG = "transaction";

    @Converter
    public static Document toDocument(List<TagData> input, Exchange exchange) throws Exception {


        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        // Root element
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement(ROOT_TAG);
        doc.appendChild(rootElement);

        // XPATH to keep track of the visited path
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();

        for (TagData entry : input) {
            Element item = doc.createElement("tag");
            item.setAttribute("name", entry.getTagName());
            if (entry.getValue() != null) {
                item.setAttribute("type", entry.getQuery().split(":")[1]);
            }
            String nb = "1";
            if (entry.getValue() instanceof List) {
                nb = Integer.toString(((List) entry.getValue()).size());
            }
            item.setAttribute("length", nb);
            if (entry.getValue() != null) {
                if (nb.equals("1")) {
                    item.appendChild(doc.createTextNode(entry.getValue().toString()));
                } else {
                    int i = 1;
                    List<Object> list = (List<Object>) entry.getValue();
                    for (Object obj : list) {
                        Element val = doc.createElement("value");
                        val.setAttribute("idx", Integer.toString(i++));
                        val.appendChild(doc.createTextNode(obj.toString()));
                        item.appendChild(val);
                    }
                }
            }
            rootElement.appendChild(item);
        }
        return doc;
    }

    @Converter
    public static List<TagData> toTagData(Document doc, Exchange exchange) {
        doc.getDocumentElement().normalize();
        return documentToTagData(doc);

    }

    @Converter
    public static List<TagData> toTagData(String input, Exchange exchange) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory
                = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(input)));
        doc.getDocumentElement().normalize();

        return documentToTagData(doc);

    }

    private static List<TagData> documentToTagData(Document doc) {
        List<TagData> entries = new ArrayList<>();
        processNode(doc.getDocumentElement(), entries);
        return entries;

    }

    private static void processNode(Node el, List<TagData> entries) {
        if (el.getNodeName() == ROOT_TAG) {
            NodeList nodeList = el.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE:
                        NamedNodeMap attrList = node.getAttributes();
                        Node tagName = attrList.getNamedItem("name");
                        Node length = attrList.getNamedItem("length");
                        Node type = attrList.getNamedItem("type");
                        String value = null;
                        if (length.getNodeValue().equals("1")) {
                            value = node.getTextContent();
                        }
                        TagData tag = new TagData(tagName.getNodeValue(), "%" + tagName.getNodeValue());
                        if ((value != null && !value.isEmpty()) || Integer.parseInt(length.getNodeValue()) > 1) {
                            switch (type.getNodeValue()) {
                                case "INT":

                                    if (Integer.parseInt(length.getNodeValue()) > 1) {
                                        List<Integer> arrayInt = new ArrayList<>();
                                        NodeList listInt = node.getChildNodes();
                                        for (int j = 0; j < listInt.getLength(); j++) {
                                            arrayInt.add(Integer.parseInt(listInt.item(j).getTextContent()));
                                        }
                                        tag.setQuery(tag.getQuery() + ":INT:" + length.getNodeValue());
                                        tag.setValue(arrayInt);
                                    }
                                    else{
                                        int integer = Integer.parseInt(value);
                                        tag.setValue(integer);
                                        tag.setQuery(tag.getQuery() + ":INT");
                                    }
                                    break;
                                case "BOOL":
                                    if (Integer.parseInt(length.getNodeValue()) > 1) {
                                        List<Boolean> arrayBool = new ArrayList<>();
                                        NodeList listReal = node.getChildNodes();
                                        for (int j = 0; j < listReal.getLength(); j++) {
                                            arrayBool.add(Boolean.parseBoolean(listReal.item(j).getTextContent()));
                                        }
                                        tag.setQuery(tag.getQuery() + ":BOOL:" + length.getNodeValue());
                                        tag.setValue(arrayBool);
                                    }
                                    else{
                                        boolean bool = Boolean.parseBoolean(value);
                                        tag.setValue(bool);
                                        tag.setQuery(tag.getQuery() + ":BOOL");
                                    }
                                    break;
                                case "STRING":

                                    if (Integer.parseInt(length.getNodeValue()) > 1) {
                                        List<String> arrayString = new ArrayList<>();
                                        NodeList listString = node.getChildNodes();
                                        for (int j = 0; j < listString.getLength(); j++) {
                                            arrayString.add(listString.item(j).getTextContent());
                                        }
                                        tag.setQuery(tag.getQuery() + ":STRING:" + length.getNodeValue());
                                        tag.setValue(arrayString);
                                    }
                                    else{
                                        tag.setValue(value);
                                        tag.setQuery(tag.getQuery() + ":STRING");
                                    }
                                    break;
                                case "REAL":
                                    if (Integer.parseInt(length.getNodeValue()) > 1) {
                                        List<Double> arrayDouble = new ArrayList<>();
                                        NodeList listReal = node.getChildNodes();
                                        for (int j = 0; j < listReal.getLength(); j++) {
                                            arrayDouble.add(Double.parseDouble(listReal.item(j).getTextContent()));
                                        }
                                        tag.setQuery(tag.getQuery() + ":REAL:" + length.getNodeValue());
                                        tag.setValue(arrayDouble);
                                    }
                                    else{
                                        tag.setValue(Double.parseDouble(value));
                                        tag.setQuery(tag.getQuery() + ":REAL");
                                    }
                                    break;
                            }
                        }
                        entries.add(tag);
                        break;
                }
            }
        }
    }


    private static String appendToXpathExpressionTheIndex(String visitedPath, int elementIdx) {
        return visitedPath + "[@idx='" + elementIdx + "']";
    }

}
