<#-- @ftlvariable name="sitemap" type="cn.taketoday.blog.model.Sitemap" -->
<#-- @ftlvariable name="opt" type="java.util.Map" -->
<?xml version="1.0" encoding="utf-8"?>
<?xml-stylesheet type='text/xsl' href='sitemap.xsl'?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">
  <url>
    <loc>${opt['site.host']}</loc>
    <changefreq>daily</changefreq>
    <priority>1</priority>
  </url>

<#list sitemap.urls as url>
  <url>
    <loc>${opt['site.host']}${url.loc}</loc>
    <lastmod>${url.lastModify}</lastmod>
    <changefreq>${url.changeFreq}</changefreq>
    <priority>${url.priority}</priority>

  <#if url.hasCover()>
    <image:image>
      <#if url.cover?starts_with("http")>
      <image:loc>${url.cover}</image:loc>
      <#else >
      <image:loc>${opt['site.cdn']}${url.cover}</image:loc>
      </#if>
    </image:image>
  </#if>
  </url>
</#list>
</urlset>
