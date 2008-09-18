<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema">
<xsl:output method="html" encoding="iso-8859-1"/>


<xsl:variable name="targetMap" select="document('Map.xml')"/>


<xsl:template name="mapLookup">
    <xsl:param name="target" />
    <xsl:for-each select="$targetMap/map/mapID[@target=$target]">
        <xsl:value-of select="@url"/>
    </xsl:for-each>
</xsl:template>



<xsl:template match="/">
<xsl:text disable-output-escaping="yes">&lt;!--</xsl:text>
// Process Dashboard - Data Automation Tool for high-maturity processes
// Copyright (C) 2003-2008 Tuma Solutions, LLC
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
// along with this program; if not, see http://www.gnu.org/licenses/
// 
// The author(s) may be contacted at:
//     processdash@tuma-solutions.com
//     processdash-devel@lists.sourceforge.net
<xsl:text disable-output-escaping="yes">--&gt;</xsl:text>

<HTML>
<HEAD>
<TITLE>Team Dashboard Users Manual</TITLE>
<link href="style.css" type="text/css" rel="stylesheet" />
<link href="Topics/Overview-style.css" type="text/css" rel="stylesheet" />
<STYLE>
BODY { color: black; background-color: white }
.hideInBook { display: none }
.sepnotice { display: none }
@media print {
  h1, h2, h3, h4, h5, h6 { page-break-after: avoid }
  h1, h2 { page-break-before: always }
  p { text-align: justify }
  .doNotPrint { display: none }
}
</STYLE>
</HEAD>
<BODY>
<H1 NO_NUMBER="true" style="page-break-before:avoid">Team Dashboard
     Users Manual</H1>
<H2 NO_NUMBER="true" style="page-break-before:avoid">Table of Contents</H2>
TABLE_OF_CONTENTS

<xsl:apply-templates select="toc/tocitem/tocitem" />

</BODY>
</HTML>
</xsl:template>



<xsl:template match="tocitem">
<xsl:variable name="depth" select="count(ancestor::*)-1"/>
<xsl:variable name="targetHref">
    <xsl:call-template name="mapLookup">
        <xsl:with-param name="target" select="@target"/>
    </xsl:call-template>
</xsl:variable>


<xsl:if test="$depth = 1">
    <hr class="doNotPrint" />
</xsl:if>

<xsl:choose>

<xsl:when test="string-length($targetHref)">
    <A demoteTo="{$depth}" HREF="{$targetHref}" CLASS="includeDoc">
    <xsl:value-of select="@text"/>
    </A>
</xsl:when>

<xsl:otherwise>
    <xsl:element name="{concat('h', $depth)}">
    <xsl:value-of select="@text"/>
    </xsl:element>
</xsl:otherwise>

</xsl:choose>

<xsl:text>
</xsl:text>

<xsl:apply-templates select="tocitem" />
</xsl:template>



</xsl:stylesheet>
