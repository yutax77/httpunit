package com.meterware.servletunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2001-2004, Russell Gold
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
* documentation files (the "Software"), to deal in the Software without restriction, including without limitation
* the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
* to permit persons to whom the Software is furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in all copies or substantial portions
* of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
* THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
* CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*******************************************************************************************************************/
import com.meterware.httpunit.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Properties;
import java.util.List;
import java.util.Arrays;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import junit.framework.TestSuite;
import junit.framework.Assert;


public class WebXMLTest extends EventAwareTestCase {

    public static void main(String args[]) {
        junit.textui.TestRunner.run( suite() );
    }


    public static TestSuite suite() {
        return new TestSuite( WebXMLTest.class );
    }


    public WebXMLTest( String name ) {
        super( name );
    }


    public void testBasicAccess() throws Exception {

        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );

        ServletRunner sr = new ServletRunner( new ByteArrayInputStream( wxs.asText().getBytes() ) );
        WebRequest request   = new GetMethodWebRequest( "http://localhost/SimpleServlet" );
        WebResponse response = sr.getResponse( request );
        assertNotNull( "No response received", response );
        assertEquals( "content type", "text/html", response.getContentType() );
        assertEquals( "requested resource", SimpleGetServlet.RESPONSE_TEXT, response.getText() );
    }


    public void testBasicAuthenticationConfig() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.requireBasicAuthentication( "SampleRealm" );

        WebApplication app = new WebApplication( newDocument( wxs.asText() ) );
        assertTrue( "Did not detect basic authentication", app.usesBasicAuthentication() );
        assertEquals( "Realm name", "SampleRealm", app.getAuthenticationRealm() );
    }


    public void testFormAuthenticationConfig() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.requireFormAuthentication( "SampleRealm", "/Login", "/Error" );

        WebApplication app = new WebApplication( newDocument( wxs.asText() ) );
        assertTrue( "Did not detect form-based authentication", app.usesFormAuthentication() );
        assertEquals( "Realm name", "SampleRealm", app.getAuthenticationRealm() );
        assertEquals( "Login path", "/Login", app.getLoginURL().getFile() );
        assertEquals( "Error path", "/Error", app.getErrorURL().getFile() );
    }


    public void testSecurityConstraint() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addSecureURL( "SecureArea1", "/SimpleServlet" );
        wxs.addAuthorizedRole( "SecureArea1", "supervisor" );

        WebApplication app = new WebApplication( newDocument( wxs.asText() ) );
        assertTrue( "Did not require authorization", app.requiresAuthorization( new URL( "http://localhost/SimpleServlet" ) ) );
        assertTrue( "Should not require authorization", !app.requiresAuthorization( new URL( "http://localhost/FreeServlet" ) ) );

        List roles = Arrays.asList( app.getPermittedRoles( new URL( "http://localhost/SimpleServlet" ) ) );
        assertTrue( "Should have access", roles.contains( "supervisor" ) );
        assertTrue( "Should not have access", !roles.contains( "peon" ) );
    }


    public void testServletParameters() throws Exception {
        WebXMLString wxs = new WebXMLString();
        Properties params = new Properties();
        params.setProperty( "color", "red" );
        params.setProperty( "age", "12" );
        wxs.addServlet( "simple", "/SimpleServlet", SimpleGetServlet.class, params );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient client = sr.newClient();
        InvocationContext ic = client.newInvocation( "http://localhost/SimpleServlet" );
        ServletConfig servletConfig = ic.getServlet().getServletConfig();
        assertNull( "init parameter 'gender' should be null", servletConfig.getInitParameter( "gender" ) );
        assertEquals( "init parameter via config", "red", ic.getServlet().getServletConfig().getInitParameter( "color" ) );
        assertEquals( "init parameter directly", "12", ((HttpServlet) ic.getServlet()).getInitParameter( "age" ) );
    }


    public void testContextParameters() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        wxs.addContextParam( "icecream", "vanilla" );
        wxs.addContextParam( "cone", "waffle" );
        wxs.addContextParam( "topping", "" );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient client = sr.newClient();
        assertEquals( "Context parameter 'icecream'", "vanilla", sr.getContextParameter( "icecream" ) );
        InvocationContext ic = client.newInvocation( "http://localhost/SimpleServlet" );

        javax.servlet.ServletContext sc = ((HttpServlet) ic.getServlet()).getServletContext();
        assertNotNull( "ServletContext should not be null", sc );
        assertEquals( "ServletContext.getInitParameter()", "vanilla", sc.getInitParameter( "icecream" ) );
        assertEquals( "init parameter: cone", "waffle", sc.getInitParameter( "cone" ) );
        assertEquals( "init parameter: topping", "", sc.getInitParameter( "topping" ) );
        assertNull( "ServletContext.getInitParameter() should be null", sc.getInitParameter( "shoesize" ) );

    }


    public void testContextListeners() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        EventVerifier verifyContext = new ServletContextEventVerifier();

        wxs.addContextListener( ListenerClass1.class );
        wxs.addContextListener( ListenerClass2.class );

        clearEvents();
        expectEvent( "startup", ListenerClass1.class, verifyContext );
        expectEvent( "startup", ListenerClass2.class, verifyContext );
        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        verifyEvents();

        clearEvents();
        expectEvent( "shutdown", ListenerClass2.class, verifyContext );
        expectEvent( "shutdown", ListenerClass1.class, verifyContext );
        sr.shutDown();
        verifyEvents();
    }


    static class ServletContextEventVerifier implements EventVerifier {

        public void verifyEvent( String eventLabel, Object eventObject ) {
            if (!(eventObject instanceof ServletContextEvent)) fail( "Event " + eventLabel + " did not include a servlet context event" );
        }
    }


    public void testSessionLifecycleListeners() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        EventVerifier verifyContext = new HttpSessionEventVerifier();

        wxs.addContextListener( ListenerClass3.class );
        wxs.addContextListener( ListenerClass4.class );

        clearEvents();
        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );

        ServletUnitClient client = sr.newClient();
        InvocationContext ic = client.newInvocation( "http://localhost/SimpleServlet" );
        verifyEvents();

        expectEvent( "created", ListenerClass3.class, verifyContext );
        expectEvent( "created", ListenerClass4.class );
        HttpSession session = ic.getRequest().getSession();
        verifyEvents();

        expectEvent( "destroyed", ListenerClass3.class, verifyContext );
        expectEvent( "destroyed", ListenerClass4.class );
        session.invalidate();
        verifyEvents();

        sr.shutDown();
    }


    static class HttpSessionEventVerifier implements EventVerifier {

        public void verifyEvent( String eventLabel, Object eventObject ) {
            if (!(eventObject instanceof HttpSessionEvent)) fail( "Event " + eventLabel + " did not include an http session event" );
        }
    }


    public void testSessionAttributeListeners() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        HttpSessionAttributeEventVerifier verifyAttribute = new HttpSessionAttributeEventVerifier();

        wxs.addContextListener( ListenerClass5.class );
        wxs.addContextListener( ListenerClass6.class );

        clearEvents();
        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );

        ServletUnitClient client = sr.newClient();
        InvocationContext ic = client.newInvocation( "http://localhost/SimpleServlet" );
        HttpSession session = ic.getRequest().getSession();
        verifyEvents();

        verifyAttribute.expect( "one", new Integer(1) );
        expectEvent( "added", ListenerClass5.class, verifyAttribute );
        expectEvent( "added", ListenerClass6.class, verifyAttribute );
        session.setAttribute( "one", new Integer(1) );
        verifyEvents();

        expectEvent( "replaced", ListenerClass5.class, verifyAttribute );
        expectEvent( "replaced", ListenerClass6.class, verifyAttribute );
        session.setAttribute( "one", "I" );
        verifyEvents();

        verifyAttribute.expect( "one", "I" );
        expectEvent( "removed", ListenerClass5.class, verifyAttribute );
        expectEvent( "removed", ListenerClass6.class );
        session.removeAttribute( "one" );
        verifyEvents();

        sr.shutDown();
    }


    static class HttpSessionAttributeEventVerifier implements EventVerifier {

        private String _name;
        private Object _value;


        public void verifyEvent( String eventLabel, Object eventObject ) {
            if (!(eventObject instanceof HttpSessionBindingEvent)) fail( "Event " + eventLabel + " did not include an http session binding event" );
            HttpSessionBindingEvent bindingChange = (HttpSessionBindingEvent) eventObject;
            Assert.assertEquals( "Changed attribute name", _name, bindingChange.getName() );
            Assert.assertEquals( "Changed attribute value", _value, bindingChange.getValue() );
        }


        public void expect( String name, Object value ) {
            _name  = name;
            _value = value;
        }
    }



    public void testContextAttributeListeners() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        ContextAttributeEventVerifier verifyAttribute = new ContextAttributeEventVerifier();

        wxs.addContextListener( ListenerClass7.class );
        wxs.addContextListener( ListenerClass8.class );

        clearEvents();
        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );

        ServletUnitClient client = sr.newClient();
        verifyAttribute.expect( "initialized", "SimpleGetServlet" );
        expectEvent( "added", ListenerClass7.class, verifyAttribute );
        expectEvent( "added", ListenerClass8.class, verifyAttribute );
        InvocationContext ic = client.newInvocation( "http://localhost/SimpleServlet" );
        ServletContext context = ic.getServlet().getServletConfig().getServletContext();
        verifyEvents();

        verifyAttribute.expect( "deux", new Integer(2) );
        expectEvent( "added", ListenerClass7.class, verifyAttribute );
        expectEvent( "added", ListenerClass8.class, verifyAttribute );
        context.setAttribute( "deux", new Integer(2) );
        verifyEvents();

        expectEvent( "replaced", ListenerClass7.class, verifyAttribute );
        expectEvent( "replaced", ListenerClass8.class, verifyAttribute );
        context.setAttribute( "deux", "II" );
        verifyEvents();

        verifyAttribute.expect( "deux", "II" );
        expectEvent( "removed", ListenerClass7.class, verifyAttribute );
        expectEvent( "removed", ListenerClass8.class );
        context.removeAttribute( "deux" );
        verifyEvents();

        sr.shutDown();
    }


    static class ContextAttributeEventVerifier implements EventVerifier {

        private String _name;
        private Object _value;


        public void verifyEvent( String eventLabel, Object eventObject ) {
            if (!(eventObject instanceof ServletContextAttributeEvent)) fail( "Event " + eventLabel + " did not include an http session binding event" );
            ServletContextAttributeEvent bindingChange = (ServletContextAttributeEvent) eventObject;
            Assert.assertEquals( "Changed attribute name", _name, bindingChange.getName() );
            Assert.assertEquals( "Changed attribute value", _value, bindingChange.getValue() );
        }


        public void expect( String name, Object value ) {
            _name  = name;
            _value = value;
        }
    }



    private Document newDocument( String contents ) throws UnsupportedEncodingException, SAXException, IOException, ParserConfigurationException  {
       DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
       return fac.newDocumentBuilder().parse( toInputStream( contents ) );    }


    private ByteArrayInputStream toInputStream( String contents ) throws UnsupportedEncodingException {
        return new ByteArrayInputStream( contents.getBytes( "UTF-8" ) );
    }


    public void testBasicAuthorization() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );
        wxs.requireBasicAuthentication( "Sample Realm" );
        wxs.addSecureURL( "SecureArea1", "/SimpleServlet" );
        wxs.addAuthorizedRole( "SecureArea1", "supervisor" );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient  wc = sr.newClient();
        try {
            wc.getResponse( "http://localhost/SimpleServlet" );
            fail( "Did not insist on validation for access to servlet" );
        } catch (AuthorizationRequiredException e) {
            assertEquals( "Realm", "Sample Realm", e.getAuthenticationParameter( "realm" ) );
            assertEquals( "Method", "Basic", e.getAuthenticationScheme() );
        }

        try {
            wc.setAuthorization( "You", "peon" );
            wc.getResponse( "http://localhost/SimpleServlet" );
            fail( "Permitted wrong user to access" );
        } catch (HttpException e) {
            assertEquals( "Response code", 403, e.getResponseCode() );
        }

        wc.setAuthorization( "Me", "supervisor,agent" );
        wc.getResponse( "http://localhost/SimpleServlet" );

        InvocationContext ic = wc.newInvocation( "http://localhost/SimpleServlet" );
        assertEquals( "Authenticated user", "Me", ic.getRequest().getRemoteUser() );
        assertTrue( "User assigned to 'bogus' role", !ic.getRequest().isUserInRole( "bogus" ) );
        assertTrue( "User not assigned to 'supervisor' role", ic.getRequest().isUserInRole( "supervisor" ) );
    }


    public void testFormAuthentication() throws Exception {
        HttpUnitOptions.setLoggingHttpHeaders( true );
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/Logon", SimpleLogonServlet.class );
        wxs.addServlet( "/Error", SimpleErrorServlet.class );
        wxs.addServlet( "/Example/SimpleServlet", SimpleGetServlet.class );
        wxs.requireFormAuthentication( "Sample Realm", "/Logon", "/Error" );
        wxs.addSecureURL( "SecureArea1", "/Example/SimpleServlet" );
        wxs.addAuthorizedRole( "SecureArea1", "supervisor" );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient wc = sr.newClient();
        WebResponse response = wc.getResponse( "http://localhost/Example/SimpleServlet" );
        WebForm form = response.getFormWithID( "login" );
        assertNotNull( "did not find login form", form );

        WebRequest request = form.getRequest();
        request.setParameter( "j_username", "Me" );
        request.setParameter( "j_password", "supervisor" );
        response = wc.getResponse( request );
        assertNotNull( "No response received after authentication", response );
        assertEquals( "content type", "text/html", response.getContentType() );
        assertEquals( "requested resource", SimpleGetServlet.RESPONSE_TEXT, response.getText() );

        InvocationContext ic = wc.newInvocation( "http://localhost/Example/SimpleServlet" );
        assertEquals( "Authenticated user", "Me", ic.getRequest().getRemoteUser() );
        assertTrue( "User assigned to 'bogus' role", !ic.getRequest().isUserInRole( "bogus" ) );
        assertTrue( "User not assigned to 'supervisor' role", ic.getRequest().isUserInRole( "supervisor" ) );
    }


    public void testGetContextPath() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ), "/mount" );
        ServletUnitClient wc = sr.newClient();
        InvocationContext ic = wc.newInvocation( "http://localhost/mount/SimpleServlet" );
        assertEquals("/mount", ic.getRequest().getContextPath());

        sr = new ServletRunner( toInputStream( wxs.asText() ) );
        wc = sr.newClient();
        ic = wc.newInvocation( "http://localhost/SimpleServlet" );
        assertEquals("", ic.getRequest().getContextPath());
    }


    public void testMountContextPath() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/SimpleServlet", SimpleGetServlet.class );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ), "/mount" );
        ServletUnitClient wc = sr.newClient();
        InvocationContext ic = wc.newInvocation( "http://localhost/mount/SimpleServlet" );
        assertTrue(ic.getServlet() instanceof SimpleGetServlet);
        assertEquals("/mount/SimpleServlet", ic.getRequest().getRequestURI());

        try {
            ic = wc.newInvocation( "http://localhost/SimpleServlet" );
            ic.getServlet();
            fail("Attempt to access url outside of the webapp context path should have thrown a 404");
        } catch (com.meterware.httpunit.HttpNotFoundException e) {}
    }


    public void testServletMapping() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "/foo/bar/*", Servlet1.class );
        wxs.addServlet( "/baz/*", Servlet2.class );
        wxs.addServlet( "/catalog", Servlet3.class );
        wxs.addServlet( "*.bop", Servlet4.class );
        wxs.addServlet( "/",     Servlet5.class );
        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient wc = sr.newClient();

        checkMapping( wc, "http://localhost/foo/bar/index.html",  Servlet1.class, "/foo/bar",             "/index.html" );
        checkMapping( wc, "http://localhost/foo/bar/index.bop",   Servlet1.class, "/foo/bar",             "/index.bop" );
        checkMapping( wc, "http://localhost/baz",                 Servlet2.class, "/baz",                 null );
        checkMapping( wc, "http://localhost/baz/index.html",      Servlet2.class, "/baz",                 "/index.html" );
        checkMapping( wc, "http://localhost/catalog",             Servlet3.class, "/catalog",             null );
        checkMapping( wc, "http://localhost/catalog/racecar.bop", Servlet4.class, "/catalog/racecar.bop", null );
        checkMapping( wc, "http://localhost/index.bop",           Servlet4.class, "/index.bop",           null );
        checkMapping( wc, "http://localhost/something/else",      Servlet5.class, "/something/else",      null );
    }


    private void checkMapping( ServletUnitClient wc, final String url, final Class servletClass, final String expectedPath, final String expectedInfo ) throws IOException, ServletException {
        InvocationContext ic = wc.newInvocation( url );
        assertTrue( "selected servlet is " + ic.getServlet() + " rather than " + servletClass, servletClass.isInstance( ic.getServlet() ) );
        assertEquals( "ServletPath for " + url, expectedPath, ic.getRequest().getServletPath() );
        assertEquals( "ServletInfo for " + url, expectedInfo, ic.getRequest().getPathInfo() );
    }


    /**
     * Verifies that only those servlets designated will pre-load when the application is initialized.
     * SimpleGetServlet and each of its subclasses adds its classname to the 'initialized' context attribute.
     */
    public void testLoadOnStartup() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "servlet1", "one", Servlet1.class );
        wxs.setLoadOnStartup( "servlet1" );
        wxs.addServlet( "servlet2", "two", Servlet2.class );
        wxs.addServlet( "servlet3", "three", Servlet3.class );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient wc = sr.newClient();
        InvocationContext ic = wc.newInvocation( "http://localhost/three" );
        assertEquals( "Initialized servlets", "Servlet1,Servlet3", ic.getServlet().getServletConfig().getServletContext().getAttribute( "initialized" ) );
    }


    /**
     * Verifies that servlets pre-load in the order specified.
     * SimpleGetServlet and each of its subclasses adds its classname to the 'initialized' context attribute.
     */
    public void testLoadOrder() throws Exception {
        WebXMLString wxs = new WebXMLString();
        wxs.addServlet( "servlet1", "one", Servlet1.class );
        wxs.setLoadOnStartup( "servlet1", 2 );
        wxs.addServlet( "servlet2", "two", Servlet2.class );
        wxs.setLoadOnStartup( "servlet2", 3 );
        wxs.addServlet( "servlet3", "three", Servlet3.class );
        wxs.setLoadOnStartup( "servlet3", 1 );

        ServletRunner sr = new ServletRunner( toInputStream( wxs.asText() ) );
        ServletUnitClient wc = sr.newClient();
        InvocationContext ic = wc.newInvocation( "http://localhost/two" );
        assertEquals( "Initialized servlets", "Servlet3,Servlet1,Servlet2", ic.getServlet().getServletConfig().getServletContext().getAttribute( "initialized" ) );
    }



//===============================================================================================================


//===============================================================================================================


    static class SimpleLogonServlet extends HttpServlet {
        static String RESPONSE_TEXT = "<html><body>\r\n" +
                                      "<form id='login' action='j_security_check' method='POST'>\r\n" +
                                      "  <input name='j_username' />\r\n" +
                                      "  <input type='password' name='j_password' />\r\n" +
                                      "</form></body></html>";

        protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            resp.setContentType( "text/html" );
            PrintWriter pw = resp.getWriter();
            pw.print( RESPONSE_TEXT );
            pw.close();
        }
    }

//===============================================================================================================


    static class SimpleErrorServlet extends HttpServlet {
        static String RESPONSE_TEXT = "<html><body>Sorry could not login</body></html>";

        protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            resp.setContentType( "text/html" );
            PrintWriter pw = resp.getWriter();
            pw.print( RESPONSE_TEXT );
            pw.close();
        }
    }

//===============================================================================================================


    static class SimpleGetServlet extends HttpServlet {
        static String RESPONSE_TEXT = "the desired content\r\n";

        public void init() throws ServletException {
            ServletConfig servletConfig = getServletConfig();
            String initialized = (String) servletConfig.getServletContext().getAttribute( "initialized" );
            if (initialized == null) initialized = getLocalName();
            else initialized = initialized + "," + getLocalName();
            servletConfig.getServletContext().setAttribute( "initialized", initialized );
        }

        private String getLocalName() {
            String className = getClass().getName();
            int dollarIndex = className.indexOf( '$' );
            if (dollarIndex < 0) return className;
            return className.substring( dollarIndex+1 );
        }

        protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException {
            resp.setContentType( "text/html" );
            PrintWriter pw = resp.getWriter();
            pw.print( RESPONSE_TEXT );
            pw.close();
        }
    }

    static class Servlet1 extends SimpleGetServlet {}
    static class Servlet2 extends SimpleGetServlet {}
    static class Servlet3 extends SimpleGetServlet {}
    static class Servlet4 extends SimpleGetServlet {}
    static class Servlet5 extends SimpleGetServlet {}


    static class EventDispatcher {

        public void contextInitialized( ServletContextEvent event ) { sendEvent( "startup", this, event ); }

        public void contextDestroyed( ServletContextEvent event ) { sendEvent( "shutdown", this, event ); }

        public void sessionCreated( HttpSessionEvent event ) { sendEvent( "created", this, event ); }

        public void sessionDestroyed( HttpSessionEvent event ) { sendEvent( "destroyed", this, event ); }

        public void attributeAdded( HttpSessionBindingEvent event ) { sendEvent( "added", this, event ); }

        public void attributeRemoved( HttpSessionBindingEvent event ) { sendEvent( "removed", this, event ); }

        public void attributeReplaced( HttpSessionBindingEvent event ) { sendEvent( "replaced", this, event ); }

        public void attributeAdded( ServletContextAttributeEvent event ) { sendEvent( "added", this, event ); }

        public void attributeRemoved( ServletContextAttributeEvent event ) { sendEvent( "removed", this, event ); }

        public void attributeReplaced( ServletContextAttributeEvent event ) { sendEvent( "replaced", this, event ); }
    }


    static class ListenerClass1 extends EventDispatcher implements ServletContextListener {}
    static class ListenerClass2 extends EventDispatcher implements ServletContextListener {}

    static class ListenerClass3 extends EventDispatcher implements HttpSessionListener {}
    static class ListenerClass4 extends EventDispatcher implements HttpSessionListener {}

    static class ListenerClass5 extends EventDispatcher implements HttpSessionAttributeListener {}
    static class ListenerClass6 extends EventDispatcher implements HttpSessionAttributeListener {}

    static class ListenerClass7 extends EventDispatcher implements ServletContextAttributeListener {}
    static class ListenerClass8 extends EventDispatcher implements ServletContextAttributeListener {}

}






