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
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.xml.sax.SAXException;


/**
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
class FrameHolder {

    private Hashtable   _contents = new Hashtable();
    private Hashtable   _subFrames = new Hashtable();
    private String      _frameName;


    FrameHolder( WebClient client, String name ) {
        _frameName = name;
        DefaultWebResponse blankResponse = new DefaultWebResponse( client, null, WebResponse.BLANK_HTML );
        _contents.put( WebRequest.TOP_FRAME, blankResponse );
        HttpUnitOptions.getScriptingEngine().associate( blankResponse );
    }


    WebResponse get( String target ) {
        return (WebResponse) _contents.get( getFrameName( target ) );
    }


    List getActiveFrameNames() {
        List result = new ArrayList();
        for (Enumeration e = _contents.keys(); e.hasMoreElements();) {
            result.add( e.nextElement() );
        }

        return result;
    }


    String getFrameName( String target ) {
        if (WebRequest.TOP_FRAME.equalsIgnoreCase( target )) {
            return _frameName;
        } else if (WebRequest.NEW_WINDOW.equalsIgnoreCase( target )) {
            return _frameName;
        } else {
            return target;
        }
    }


    void updateFrames( WebResponse response, String target ) throws MalformedURLException, IOException, SAXException {
        removeSubFrames( target );
        _contents.put( target, response );

        if (response.isHTML()) {
            createSubFrames( target, response.getFrameNames() );
            WebRequest[] requests = response.getFrameRequests();
            for (int i = 0; i < requests.length; i++) response.getWindow().getResponse( requests[ i ] );
            HttpUnitOptions.getScriptingEngine().associate( response );
        }
    }


    private void removeSubFrames( String targetName ) {
        String[] names = (String[]) _subFrames.get( targetName );
        if (names == null) return;
        for (int i = 0; i < names.length; i++) {
            removeSubFrames( names[ i ] );
            _contents.remove( names[ i ] );
            _subFrames.remove( names[ i ] );
        }
    }


    private void createSubFrames( String targetName, String[] frameNames ) {
        _subFrames.put( targetName, frameNames );
        for (int i = 0; i < frameNames.length; i++) {
            _contents.put( frameNames[ i ], WebResponse.BLANK_RESPONSE );
        }
    }

}

