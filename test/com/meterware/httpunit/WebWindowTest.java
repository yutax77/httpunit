package com.meterware.httpunit;
/********************************************************************************************************************
 * $Id$
 *
 * Copyright (c) 2002, Russell Gold
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
import junit.framework.TestSuite;
import junit.textui.TestRunner;

/**
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
public class WebWindowTest extends HttpUnitTest {

    public static void main( String args[] ) {
        TestRunner.run( suite() );
    }


    public static TestSuite suite() {
        return new TestSuite( WebWindowTest.class );
    }


    public WebWindowTest( String name ) {
        super( name );
    }


    public void testNewTarget() throws Exception {
        defineResource( "goHere", "You made it!" );
        defineWebPage( "start", "<a href='goHere' id='go' target='_blank'>here</a>" );

        WebClient wc = new WebConversation();
        assertEquals( "Number of initial windows", 1, wc.getOpenWindows().length );
        WebWindow main = wc.getMainWindow();
        WebResponse initialPage = main.getResponse( getHostPath() + "/start.html" );
        initialPage.getLinkWithID( "go" ).click();
        assertEquals( "Number of windows after following link", 2, wc.getOpenWindows().length );
        assertEquals( "Main page in original window", initialPage, main.getCurrentPage() );
        WebWindow other = wc.getOpenWindows()[1];
        assertEquals( "New window contents", "You made it!", other.getCurrentPage().getText() );
    }


    public void testWindowIndependence() throws Exception {
        defineResource( "next", "You made it!" );
        defineWebPage( "goHere", "<a href='next' id=proceed>more</a>" );
        defineWebPage( "start", "<a href='goHere.html' id='go' target='_blank'>here</a>" );

        WebClient wc = new WebConversation();
        WebWindow main = wc.getMainWindow();
        WebResponse initialPage = wc.getResponse( getHostPath() + "/start.html" );
        initialPage.getLinkWithID( "go" ).click();
        WebWindow other = wc.getOpenWindows()[1];
        WebResponse response = other.getResponse( other.getCurrentPage().getLinkWithID( "proceed" ).getRequest() );
        assertEquals( "Main page URL", getHostPath() + "/start.html", main.getCurrentPage().getURL().toExternalForm() );
        assertEquals( "New window contents", "You made it!", other.getCurrentPage().getText() );
    }


    public void testWindowContext() throws Exception {
        defineResource( "next", "You made it!" );
        defineWebPage( "goHere", "<a href='next' id=proceed>more</a>" );
        defineWebPage( "start", "<a href='goHere.html' id='go' target='_blank'>here</a>" );

        WebClient wc = new WebConversation();
        WebWindow main = wc.getMainWindow();
        WebResponse initialPage = wc.getResponse( getHostPath() + "/start.html" );
        initialPage.getLinkWithID( "go" ).click();
        WebWindow other = wc.getOpenWindows()[1];
        other.getCurrentPage().getLinkWithID( "proceed" ).click();
        assertEquals( "New window contents", "You made it!", other.getCurrentPage().getText() );
    }


}