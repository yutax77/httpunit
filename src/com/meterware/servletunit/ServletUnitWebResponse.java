package com.meterware.servletunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000, Russell Gold
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.meterware.httpunit.*;
                      

/**
 * A response from to a request from the simulated servlet environment.
 **/
class ServletUnitWebResponse extends WebResponse {


    /**
     * Constructs a response object from a servlet response.
     * @param url the url from which the response was received
     * @param inputStream the input stream from which the response can be read
     **/
    ServletUnitWebResponse( String target, URL url, ServletUnitHttpResponse response ) {
        super( target, url );
        _response = response;
    }


    /**
     * Returns the response code associated with this response.
     **/
    public int getResponseCode() {
        return _response.getStatus();
    }


    /**
     * Returns the value for the specified header field. If no such field is defined, will return null.
     **/
    public String getHeaderField( String fieldName ) {
        return _response.getHeaderField( fieldName );
    }
    
    
    /**
     * Returns the text of the response (excluding headers) as a string. Use this method in preference to 'toString'
     * which may be used to represent internal state of this object.
     **/
    public String getText() {
        return _response.getContents();
    }
    
    
//-------------------------------------------- private members ------------------------------------------------


    private ServletUnitHttpResponse _response;


}

