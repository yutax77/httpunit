package com.meterware.servletunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2001, Russell Gold
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
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.httpunit.Base64;
import com.meterware.httpunit.HttpException;

import java.net.MalformedURLException;
import java.net.URL;
import java.io.IOException;
import java.util.Dictionary;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * This class represents the context in which a specific servlet request is being made.
 * It contains the objects needed to unit test the methods of a servlet.
 **/
class InvocationContextImpl implements InvocationContext {


    /**
     * Returns the request to be processed by the servlet.
     **/
    public HttpServletRequest getRequest() {
        return _request;
    }


    /**
     * Returns the response which the servlet should modify during its operation.
     **/
    public HttpServletResponse getResponse() {
        return _response;
    }


    /**
     * Returns the selected servlet, initialized to provide access to sessions
     * and servlet context information.
     **/
    public Servlet getServlet() throws ServletException {
        if (_servlet == null) {
            if (!_application.requiresAuthorization( _requestURL ) || userIsAuthorized() ) {
                _servlet = _application.getServlet( _requestURL );
            } else if (_request.getRemoteUser() != null) {
                throw new AccessDeniedException( _requestURL );
            } else if (_application.usesBasicAuthentication()) {
                throw new BasicAuthenticationRequiredException( _application.getAuthenticationRealm() );
            } else if (_application.usesFormAuthentication()) {
                _servlet = _application.getServlet( _application.getLoginURL() );
                ((ServletUnitHttpRequest) getRequest()).setOriginalURL( _requestURL );
            } else {
                throw new IllegalStateException( "Authorization required but no authentication method defined" );
            }
        }
        return _servlet;
    }


    /**
     * Returns the final response from the servlet. Note that this method should
     * only be invoked after all processing has been done to the servlet response.
     **/
    public WebResponse getServletResponse() {
        if (_webResponse == null) {
            HttpSession session = _request.getSession( /* create */ false );
            if (session != null && session.isNew()) {
                _response.addCookie( new Cookie( ServletUnitHttpSession.SESSION_COOKIE_NAME, session.getId() ) );
            }
            _webResponse = new ServletUnitWebResponse( _target, _requestURL, _response );
        }
        return _webResponse;
    }


    /**
     * Returns the target for the original request.
     */
    public String getTarget() {
        return _target;
    }


    private boolean userIsAuthorized() {
        final String[] roles = _request.getRoles();
        for (int i = 0; i < roles.length; i++) {
            if (_application.roleMayAccess( roles[i], _requestURL )) return true;
        }
        return false;
    }


    class AccessDeniedException extends HttpException {
        public AccessDeniedException( URL baseURL ) {
            super( 403, "Access Denied", baseURL );
        }
    }


//------------------------------ package methods ---------------------------------------


    /**
     * Constructs a servlet invocation context for a specified servlet container,
     * request, and cookie headers.
     **/
    InvocationContextImpl( ServletRunner runner, WebRequest request, Cookie[] cookies, Dictionary clientHeaders ) throws IOException, MalformedURLException {
        _application         = runner._application;
        _requestURL          = request.getURL();
        _target              = request.getTarget();

        _request = new ServletUnitHttpRequest( request, runner.getContext(), clientHeaders );
        for (int i = 0; i < cookies.length; i++) _request.addCookie( cookies[i] );

        if (_application.usesBasicAuthentication()) _request.readBasicAuthentication();
        else if (_application.usesFormAuthentication()) _request.readFormAuthentication();

        HttpSession session = _request.getSession( /* create */ false );
        if (session != null) ((ServletUnitHttpSession) session).access();
    }


//------------------------------ private members ---------------------------------------


    private WebApplication          _application;
    private ServletUnitHttpRequest  _request;
    private ServletUnitHttpResponse _response = new ServletUnitHttpResponse();
    private URL                     _requestURL;
    private String                  _target;

    private Servlet                 _servlet;
    private WebResponse             _webResponse;
}



