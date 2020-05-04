/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package goodyear.corp.ge.commons.camel;

import java.util.*;
import junit.framework.TestCase;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.plc4x.camel.TagData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author GE
 */
public class ConversionsTest extends TestCase {

    Logger logger = LoggerFactory.getLogger(ConversionsTest.class);
 
    public void testTagToXML() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(Document.class).log("${body}");
            }
        });
        context.start();

        ProducerTemplate template = context.createProducerTemplate();

        StringBuilder builder = new StringBuilder();
        builder.append("<transaction>");
        builder.append("<tag length=\"1\" name=\"tag1\"/>");
        builder.append("<tag length=\"1\" name=\"tag2\" type=\"INT\">123</tag>");
        builder.append("<tag length=\"1\" name=\"tag3\" type=\"BOOL\">true</tag>");
        builder.append("<tag length=\"1\" name=\"tag4\" type=\"STRING\">ge</tag>");
        builder.append("<tag length=\"5\" name=\"tag5\" type=\"INT\">" +
                "<value idx=\"1\">1</value>" +
                "<value idx=\"2\">2</value>" +
                "<value idx=\"3\">3</value>" +
                "<value idx=\"4\">4</value>" +
                "<value idx=\"5\">5</value></tag>");
        builder.append("</transaction>");

        List<TagData> input = new ArrayList<>();
        input.add(new TagData("tag1","%tag1"));
        input.add(new TagData("tag2","%tag2:INT",123));
        input.add(new TagData("tag3","%tag3:BOOL",true));
        input.add(new TagData("tag4","%tag4:STRING","ge"));
        input.add(new TagData("tag5","%tag5:INT:5",Arrays.asList(1,2,3,4,5)));

        String doc = template.requestBody("direct:start", input, String.class);

        assertEquals(builder.toString(), doc);

    }
    public void testXMLtoTag() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(List.class).log("${body}");
            }
        });
        context.start();
        ProducerTemplate template = context.createProducerTemplate();

        StringBuilder builder = new StringBuilder();
        builder.append("<transaction>");
        builder.append("<tag length=\"1\" name=\"tag1\"/>");
        builder.append("<tag length=\"1\" name=\"tag2\" type=\"INT\">123</tag>");
        builder.append("<tag length=\"1\" name=\"tag3\" type=\"BOOL\">true</tag>");
        builder.append("<tag length=\"1\" name=\"tag4\" type=\"STRING\">ge</tag>");
        builder.append("<tag length=\"5\" name=\"tag5\" type=\"INT\">" +
                "<value idx=\"1\">1</value>" +
                "<value idx=\"2\">2</value>" +
                "<value idx=\"3\">3</value>" +
                "<value idx=\"4\">4</value>" +
                "<value idx=\"5\">5</value></tag>");
        builder.append("</transaction>");

        List<TagData> input = new ArrayList<>();
        input.add(new TagData("tag1","%tag1"));
        input.add(new TagData("tag2","%tag2:INT",123));
        input.add(new TagData("tag3","%tag3:BOOL",true));
        input.add(new TagData("tag4","%tag4:STRING","ge"));
        input.add(new TagData("tag5","%tag5:INT:5",Arrays.asList(1,2,3,4,5)));

        List<TagData> got = template.requestBody("direct:start",builder.toString(), List.class);

        assertEquals(input, got);
    }

}
