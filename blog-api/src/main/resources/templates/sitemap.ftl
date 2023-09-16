<#-- @ftlvariable name="sitemap" type="cn.taketoday.blog.model.Sitemap" -->
<#-- @ftlvariable name="opt" type="java.util.Map" -->
<?xml version="1.0" encoding="utf-8"?>
<?xml-stylesheet type='text/xsl' href='sitemap.xsl'?>

<urlset xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.sitemaps.org/schemas/sitemap/0.9
         https://www.sitemaps.org/schemas/sitemap/0.9/sitemap.xsd"
        xmlns="https://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>${opt['site.host']}</loc>
    <changefreq>always</changefreq>
    <priority>1</priority>
  </url>
    <#list sitemap.urls as url>
      <url>
        <loc>${opt['site.host']}${contextPath}${url.loc}</loc>
<#--        <lastmod>${url.lastModify.format('yyyy-MM-dd\'T\'HH:mm:ss+hh:00')}</lastmod>-->
        <lastmod>${url.lastModify}</lastmod>
        <changefreq>${url.changeFreq}</changefreq>
        <priority>${url.priority}</priority>
      </url>
    </#list>
</urlset>
