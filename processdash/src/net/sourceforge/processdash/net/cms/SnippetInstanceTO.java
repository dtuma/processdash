// Copyright (C) 2006-2008 Tuma Solutions, LLC
// Process Dashboard - Data Automation Tool for high-maturity processes
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 3
// of the License, or (at your option) any later version.
//
// Additional permissions also apply; see the README-license.txt
// file in the project root directory for more information.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, see <http://www.gnu.org/licenses/>.
//
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net

package net.sourceforge.processdash.net.cms;

import net.sourceforge.processdash.ui.snippet.SnippetDefinition;

/** Holds data about a single snippet instance that appears on a CMS page.
 */
public class SnippetInstanceTO {

    private String snippetID;

    private String snippetVersion;

    private String instanceID;

    private String persisterID;

    private String persistedText;

    private SnippetDefinition definition;

    private String namespace;

    private int status = SnippetInvoker.STATUS_NOT_RUN;

    private Exception invocationException;

    private String uri;

    private String generatedContent;

    private String alternateName;

    private int pageRegion = PageContentTO.REGION_CONTENT;

    /** Returns the ID of the snippet which created this instance */
    public String getSnippetID() {
        return snippetID;
    }

    /** Sets the ID of the snippet which created this instance */
    public void setSnippetID(String snippetID) {
        this.snippetID = snippetID;
    }

    /** Returns the version of the snippet which created this instance */
    public String getSnippetVersion() {
        return snippetVersion;
    }

    /** Sets the version of the snippet which created this instance */
    public void setSnippetVersion(String snippetVersion) {
        this.snippetVersion = snippetVersion;
    }

    /** Gets the unique ID of this instance of this snippet */
    public String getInstanceID() {
        return instanceID;
    }

    /** Sets the unique ID of this instance of this snippet */
    public void setInstanceID(String instanceID) {
        this.instanceID = instanceID;
    }

    /** Gets a string identifying the component which generated the persistent
     * text.  If the snippet persisted its own text, returns null. */
    public String getPersisterID() {
        return persisterID;
    }

    /** Sets a string identifying the component which generated the persistent
     * text. */
    public void setPersisterID(String persister) {
        if ("".equals(persister))
            this.persisterID = null;
        else
            this.persisterID = persister;
    }

    /** Gets the text that was persisted by the snippet which created this
     * instance. */
    public String getPersistedText() {
        return persistedText;
    }

    /** Sets the text that was persisted by the snippet which created this
     * instance. */
    public void setPersistedText(String persistedText) {
        this.persistedText = persistedText;
    }

    /** Gets the snippet definition for this instance */
    public SnippetDefinition getDefinition() {
        return definition;
    }

    /** Sets the snippet definition for this instance */
    public void setDefinition(SnippetDefinition definition) {
        this.definition = definition;
    }

    /** Gets the namespace assigned to this snippet by the page assembler */
    public String getNamespace() {
        return namespace;
    }

    /** Sets the namespace assigned to this snippet by the page assembler */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /** Gets the status returned by the snippet invoker when running this
     * instance */
    public int getStatus() {
        return status;
    }

    /** Sets the status returned by the snippet invoker when running this
     * instance */
    public void setStatus(int status) {
        this.status = status;
    }

    /** Gets the error that was encountered by the snippet invoker when running
     *  this instance, if applicable */
    public Exception getInvocationException() {
        return invocationException;
    }

    /** Sets the error that was encountered by the snippet invoker when running
     *  this instance */
    public void setInvocationException(Exception invocationException) {
        this.invocationException = invocationException;
    }

    /** Get the URI that was used in the invocation of this snippet */
    public String getUri() {
        return uri;
    }

    /** Set the URI that was used to invoke of this snippet */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /** Gets the content generated by this snippet instance */
    public String getGeneratedContent() {
        return generatedContent;
    }

    /** Sets the content generated by this snippet instance */
    public void setGeneratedContent(String generatedContent) {
        this.generatedContent = generatedContent;
    }

    /** Gets an alternate name to display for this snippet */
    public String getAlternateName() {
        return alternateName;
    }

    /** Sets an alternate name to display for this snippet */
    public void setAlternateName(String alternateName) {
        this.alternateName = alternateName;
    }

    /** Get the region of the page to which this snippet belongs. */
    public int getPageRegion() {
        return pageRegion;
    }

    /** Set the region of the page to which this snippet belongs. */
    public void setPageRegion(int pageRegion) {
        this.pageRegion = pageRegion;
    }

}
