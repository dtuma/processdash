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
// PSP Dashboard - Data Automation Tool for PSP-like processes
// Copyright (C) 2003 Software Process Dashboard Initiative
// 
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
// 
// The author(s) may be contacted at:
// OO-ALC/TISHD
// Attn: PSP Dashboard Group
// 6137 Wardleigh Road
// Hill AFB, UT 84056-5843
// 
// E-Mail POC:  processdash-devel@lists.sourceforge.net
<xsl:text disable-output-escaping="yes">--&gt;</xsl:text>

<HTML>
<HEAD>
<TITLE>Process Dashboard Users Manual</TITLE>
<link href="style.css" type="text/css" rel="stylesheet" />
<STYLE>
BODY { color: black; background-color: white }
.hideInBook { display: none }
.sepnotice { display: none }
</STYLE>
</HEAD>
<BODY>
<H1 NO_NUMBER="true">Process Dashboard Users Manual</H1>
<p align="center" class="unlesspsp">
<a href="Topics/Overview/Separation.html"><img src="Images/PSPDisclaimer.png" border="0" /></a>
</p>
<H2 NO_NUMBER="true">Table of Contents</H2>
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
    <HR />
</xsl:if>
<A demoteTo="{$depth}" HREF="{$targetHref}" CLASS="includeDoc">
<xsl:value-of select="@text"/>
</A><xsl:text>
</xsl:text>
<xsl:apply-templates select="tocitem" />
</xsl:template>



</xsl:stylesheet>
