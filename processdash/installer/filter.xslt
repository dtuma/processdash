<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema">
<xsl:output method="xml" encoding="iso-8859-1"/>

<xsl:param name="flags"/>


<!-- discard "includeIf" and "excludeIf" attributes -->
<xsl:template match="@includeIf"/>
<xsl:template match="@excludeIf"/>

<!-- copy all other attributes -->
<xsl:template match="@*">
  <xsl:copy/>
</xsl:template>


<!-- filter nodes with "includeIf" and "excludeIf" attributes -->
<xsl:template match="node()[@includeIf and not(contains($flags,@includeIf))]"/>
<xsl:template match="node()[@excludeIf and contains($flags,@excludeIf)]"/>

<!-- copy all other nodes -->
<xsl:template match="node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>


</xsl:stylesheet>
