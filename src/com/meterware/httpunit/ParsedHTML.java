package com.meterware.httpunit;
/********************************************************************************************************************
* $Id$
*
* Copyright (c) 2000-2001, Russell Gold
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
import java.net.URL;

import java.util.Vector;

import org.w3c.dom.*;

/**
 * @author <a href="mailto:russgold@acm.org">Russell Gold</a>
 * @author <a href="mailto:bx@bigfoot.com">Benoit Xhenseval</a>
 **/
class ParsedHTML {


    ParsedHTML( URL baseURL, String baseTarget, Node rootNode, String characterSet ) {
        _baseURL      = baseURL;
        _baseTarget   = baseTarget;
        _rootNode     = rootNode;
        _characterSet = characterSet;
    }


    /**
     * Returns the forms found in the page in the order in which they appear.
     **/
    public WebForm[] getForms() {
        NodeList forms = NodeUtils.getElementsByTagName( _rootNode, "form" );
        WebForm[] result = new WebForm[ forms.getLength() ];
        for (int i = 0; i < result.length; i++) {
            result[i] = new WebForm( _baseURL, _baseTarget, forms.item( i ), _characterSet );
        }

        return result;
    }


    /**
     * Returns the form found in the page with the specified ID.
     * @exception SAXException thrown if there is an error parsing the response.
     **/
    public WebForm getFormWithID( String ID ) {
        WebForm[] forms = getForms();
        for (int i = 0; i < forms.length; i++) {
            if (forms[i].getID().equals( ID )) return forms[i];
            else if (HttpUnitOptions.getMatchesIgnoreCase() && forms[i].getID().equalsIgnoreCase( ID )) return forms[i];
        }
        return null;
    }


    /**
     * Returns the form found in the page with the specified name.
     * @exception SAXException thrown if there is an error parsing the response.
     **/
    public WebForm getFormWithName( String name ) {
        WebForm[] forms = getForms();
        for (int i = 0; i < forms.length; i++) {
            if (forms[i].getName().equals( name )) return forms[i];
            else if (HttpUnitOptions.getMatchesIgnoreCase() && forms[i].getName().equalsIgnoreCase( name )) return forms[i];
        }
        return null;
    }


    /**
     * Returns the links found in the page in the order in which they appear.
     **/
    public WebLink[] getLinks() {
        if (_links == null) {
            Vector list = new Vector();
            addLinkAnchors( list, NodeUtils.getElementsByTagName( _rootNode, "a" ) );
            addLinkAnchors( list, NodeUtils.getElementsByTagName( _rootNode, "area" ) );
            _links = new WebLink[ list.size() ];
            list.copyInto( _links );
        }

        return _links;
    }


    private void addLinkAnchors(Vector list, NodeList nl) {
        for (int i = 0; i < nl.getLength(); i++) {
            Node child = nl.item(i);
            if (isLinkAnchor( child )) {
                list.addElement( new WebLink( _baseURL, _baseTarget, child ) );
            }
        }
    }


    /**
     * Returns the first link which contains the specified text.
     **/
    public WebLink getLinkWith( String text ) {
        WebLink[] links = getLinks();
        for (int i = 0; i < links.length; i++) {
            if (contains( links[i].asText(), text )) return links[i];
        }
        return null;
    }


    /**
     * Returns the first link which contains an image with the specified text as its 'alt' attribute.
     **/
    public WebLink getLinkWithImageText( String text ) {
        WebLink[] links = getLinks();
        for (int i = 0; i < links.length; i++) {
            NodeList nl = ((Element) links[i].getDOMSubtree()).getElementsByTagName( "img" );
            for (int j = 0; j < nl.getLength(); j++) {
                NamedNodeMap nnm = nl.item(j).getAttributes();
                if (text.equals( getValue( nnm.getNamedItem( "alt" ) ) )) {
                    return links[i];
                } else if (HttpUnitOptions.getMatchesIgnoreCase() &&
                           text.equalsIgnoreCase( getValue( nnm.getNamedItem( "alt" ) ) )) {
                    return links[i];
                }
            }
        }
        return null;
    }


    /**
     * Returns the link found in the page with the specified ID.
     * @exception SAXException thrown if there is an error parsing the response.
     **/
    public WebLink getLinkWithID( String ID ) {
        WebLink[] links = getLinks();
        for (int i = 0; i < links.length; i++) {
            if (links[i].getID().equals( ID )) return links[i];
            else if (HttpUnitOptions.getMatchesIgnoreCase() && links[i].getID().equalsIgnoreCase( ID )) return links[i];
        }
        return null;
    }


    /**
     * Returns the link found in the page with the specified name.
     * @exception SAXException thrown if there is an error parsing the response.
     **/
    public WebLink getLinkWithName( String name ) {
        WebLink[] links = getLinks();
        for (int i = 0; i < links.length; i++) {
            if (links[i].getName().equals( name )) return links[i];
            else if (HttpUnitOptions.getMatchesIgnoreCase() && links[i].getName().equalsIgnoreCase( name )) return links[i];
        }
        return null;
    }


    /**
     * Returns the top-level tables found in this page in the order in which
     * they appear.
     **/
    public WebTable[] getTables() {
        return WebTable.getTables( getOriginalDOM(), _baseURL, _baseTarget, _characterSet );
    }


    /**
     * Returns the first table in the response which has the specified text as the full text of
     * its first non-blank row and non-blank column. Will recurse into any nested tables, as needed.
     * @return the selected table, or null if none is found
     **/
    public WebTable getTableStartingWith( final String text ) {
        return getTableSatisfyingPredicate( getTables(), new TablePredicate() {
            public boolean isTrue( WebTable table ) {
                table.purgeEmptyCells();
                return table.getRowCount() > 0 &&
                       matches( table.getCellAsText(0,0), text );
            }
        } );
    }


    /**
     * Returns the first table in the response which has the specified text as a prefix of the text
     * in its first non-blank row and non-blank column. Will recurse into any nested tables, as needed.
     * @return the selected table, or null if none is found
     **/
    public WebTable getTableStartingWithPrefix( final String text ) {
        return getTableSatisfyingPredicate( getTables(), new TablePredicate() {
            public boolean isTrue( WebTable table ) {
                table.purgeEmptyCells();
                return table.getRowCount() > 0 &&
                       hasPrefix( table.getCellAsText(0,0).toUpperCase(), text );
            }
        } );
    }


    /**
     * Returns the first table in the response which has the specified text as its summary attribute.
     * Will recurse into any nested tables, as needed.
     * @return the selected table, or null if none is found
     **/
    public WebTable getTableWithSummary( final String summary ) {
        return getTableSatisfyingPredicate( getTables(), new TablePredicate() {
            public boolean isTrue( WebTable table ) {
                return matches( table.getSummary(), summary );
            }
        } );
    }


    /**
     * Returns the first table in the response which has the specified text as its ID attribute.
     * Will recurse into any nested tables, as needed.
     * @return the selected table, or null if none is found
     **/
    public WebTable getTableWithID( final String ID ) {
        return getTableSatisfyingPredicate( getTables(), new TablePredicate() {
            public boolean isTrue( WebTable table ) {
                return matches( table.getID(), ID );
            }
        } );
    }


    /**
     * Returns a copy of the domain object model associated with this page.
     **/
    public Node getDOM() {
        return _rootNode.cloneNode( /* deep */ true );
    }

    /**
     * Returns the domain object model associated with this page, to be used internally.
     * @author <a href="mailto:bx@bigfoot.com">Benoit Xhenseval</a>
     **/
    Node getOriginalDOM() {
        return _rootNode;
    }


//---------------------------------- Object methods --------------------------------


    public String toString() {
        return _baseURL.toExternalForm() + System.getProperty( "line.separator" ) +
               _rootNode;
    }


//---------------------------------- protected members ------------------------------


    /**
     * Overrides the base URL for this HTML segment.
     **/
    protected void setBaseURL( URL baseURL ) {
        _baseURL = baseURL;
    }


    /**
     * Overrides the base target for this HTML segment.
     **/
    protected void setBaseTarget( String baseTarget ) {
        _baseTarget = baseTarget;
    }


//---------------------------------- package members --------------------------------


    /**
     * Returns the base URL for this HTML segment.
     **/
    URL getBaseURL() {
        return _baseURL;
    }


    interface TablePredicate {
        public boolean isTrue( WebTable table );
    }


//---------------------------------- private members --------------------------------

    private Node _rootNode;

    private WebLink[] _links;

    private URL _baseURL;

    private String _baseTarget;

    private String _characterSet;


    private boolean contains( String string, String substring ) {
        if (HttpUnitOptions.getMatchesIgnoreCase()) {
            return string.toUpperCase().indexOf( substring.toUpperCase() ) >= 0;
        } else {
            return string.indexOf( substring ) >= 0;
        }
    }


    private boolean hasPrefix( String string, String prefix ) {
        if (HttpUnitOptions.getMatchesIgnoreCase()) {
            return string.toUpperCase().startsWith( prefix.toUpperCase() );
        } else {
            return string.startsWith( prefix );
        }
    }


    private boolean matches( String string1, String string2 ) {
        if (HttpUnitOptions.getMatchesIgnoreCase()) {
            return string1.equalsIgnoreCase( string2 );
        } else {
            return string1.equals( string2 );
        }
    }


    private String getValue( Node node ) {
        return (node == null) ? "" : node.getNodeValue();
    }


    /**
     * Returns true if the node is a link anchor node.
     **/
    private boolean isLinkAnchor( Node node ) {
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            return false;
        } else if (!node.getNodeName().equals( "a" ) && !node.getNodeName().equals( "area" )) {
            return false;
        } else {
            return (node.getAttributes().getNamedItem( "href" ) != null);
        }
    }



    /**
     * Returns the table with the specified text in its summary attribute.
     **/
    private WebTable getTableSatisfyingPredicate( WebTable[] tables, TablePredicate predicate ) {
        for (int i = 0; i < tables.length; i++) {
            if (predicate.isTrue( tables[i] )) {
                return tables[i];
            } else {
                for (int j = 0; j < tables[i].getRowCount(); j++) {
                    for (int k = 0; k < tables[i].getColumnCount(); k++) {
                        TableCell cell = tables[i].getTableCell(j,k);
                        if (cell != null) {
                            WebTable[] innerTables = cell.getTables();
                            if (innerTables.length != 0) {
                                WebTable result = getTableSatisfyingPredicate( innerTables, predicate );
                                if (result != null) return result;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}