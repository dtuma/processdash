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
// Copyright (C) 2003-2013 Tuma Solutions, LLC
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

<html>
<head>
<title>Process Dashboard Team Users Manual - Table of Contents</title>
<base target="contents"/>
<style>
BODY { color: black; background-color: white }
B  { font-family: sans-serif; font-size: x-small }
TD { white-space: nowrap; font-family: sans-serif; font-size: x-small }
A  { color: black; text-decoration: none }
</style>
</head>
<body>
<b>Team Use Help</b>

<table border="0" cellspacing="0">
<xsl:apply-templates select="toc/tocitem/tocitem" />
</table>

<script>
  var helpTopics = [
<xsl:apply-templates select="$targetMap/map/mapID" />
    [ "ignored", "ignored" ]
  ];
  var activateLink = location.search;
  var foundLink = false;
  if (activateLink != "") {
    activateLink = activateLink.substring(1);
    for (var i = helpTopics.length - 1;   i-- > 0; ) {
      if (activateLink == helpTopics[i][0]) {
        window.top.frames[1].location = helpTopics[i][1];
        foundLink = true;
        break;
      }
    }
  }
  if (foundLink == false) {
    window.top.frames[1].location = "Topics/Overview.html";
  }
</script>
</body>
</html>
</xsl:template>



<xsl:template match="tocitem">
<xsl:variable name="image"  select="@image"/>
<xsl:variable name="target" select="@target"/>
<xsl:variable name="indentWidth" select="20*(count(ancestor::*)-2)"/>
<xsl:variable name="imageSrc">
    <xsl:call-template name="mapLookup">
        <xsl:with-param name="target" select="@image"/>
    </xsl:call-template>
</xsl:variable>
<xsl:variable name="targetHref">
    <xsl:call-template name="mapLookup">
        <xsl:with-param name="target" select="@target"/>
    </xsl:call-template>
</xsl:variable>
<tr><td nowrap="true">
<xsl:if test="$indentWidth &gt; 0">
    <img align="absmiddle" src="Images/spacer.gif" height="1" 
         width="{$indentWidth}" />
</xsl:if>
<a href="frame.html?{$target}" target="_top">
<img align="absmiddle" src="{$imageSrc}">
    <xsl:choose>
        <xsl:when test="$image = 'BookIcon'">
            <xsl:attribute name="width">16</xsl:attribute>
            <xsl:attribute name="height">18</xsl:attribute>
        </xsl:when>
        <xsl:when test="$image = 'PageIcon'">
            <xsl:attribute name="width">17</xsl:attribute>
            <xsl:attribute name="height">20</xsl:attribute>
        </xsl:when>
    </xsl:choose>
</img></a>
<img align="absmiddle" src="Images/spacer.gif" width="4" height="1" />
<a href="{$targetHref}"><xsl:value-of select="@text"/></a>
</td></tr>
<xsl:apply-templates select="tocitem" />
</xsl:template>



<xsl:template match="mapID">
<xsl:if test="contains(@url,'.htm')">
    [ &quot;<xsl:value-of select="@target"/>&quot;, &quot;<xsl:value-of select="@url"/>&quot; ],</xsl:if>
</xsl:template>



</xsl:stylesheet>
