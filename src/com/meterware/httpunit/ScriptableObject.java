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

/**
 * An interface for objects which will be accessible via scripting.
 *
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 **/
abstract public class ScriptableObject {

    private ScriptEngine _scriptEngine;


    /**
     * Executes the specified scripted event.
     **/
    public boolean doEvent( String eventScript ) {
        if (_scriptEngine == null) throw new IllegalStateException( "Script engine must be defined before running an event" );
        if (eventScript.length() == 0) return true;
        return _scriptEngine.performEvent( eventScript );
    }


    /**
     * Executes the specified script.
     **/
    public void runScript( String script ) {
        if (_scriptEngine == null) throw new IllegalStateException( "Script engine must be defined before running an event" );
        _scriptEngine.executeScript( script );
    }


    /**
     * Returns the value of the named property. Will return null if the property does not exist.
     **/
    public Object get( String propertyName ) {
        return null;
    }


    /**
     * Returns the value of the index property. Will return null if the property does not exist.
     **/
    public Object get( int index ) {
        return null;
    }


    /**
     * Sets the value of the named property. Will throw a runtime exception if the property does not exist or
     * cannot accept the specified value.
     **/
    public void set( String propertyName, Object value ) {
        throw new RuntimeException( "No such property: " + propertyName );
    }


    /**
     * Specifies the scripting engine to be used.
     */
    public void setScriptEngine( ScriptEngine scriptEngine ) {
        _scriptEngine = scriptEngine;
    }

}
