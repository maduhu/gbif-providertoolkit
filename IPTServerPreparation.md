# How To Prepare Your IPT Server #



## Introduction ##

Since the IPT ships as a .war file, installing the IPT consists of deploying the .war file in a servlet container such as Tomcat.

This page explains how to install different types of servlet containers on your server, and how to deploy the IPT in them.

If you use Tomcat and would like to front your instance with a proxy server, please read [this article](https://www.mulesoft.com/tcat/tomcat-proxy-configuration) that compares the proxy servers most commonly used with Tomcat.

In case you decide to use Apache HTTPD proxy server and do an Apache reverse proxy, sample configuration instructions for an Apache virtual host declaration for the IPT are provided below.

## Servlet Containers ##

The most common servlet containers used to deploy the IPT are Tomcat, Jetty, and Wildfly8 (JBoss). Information about how to deploy the IPT in these servlets follows.

### Tomcat ###

The IPT has been tested and works well with Tomcat 5.5, Tomcat 6.0 and Tomcat 7.0. The Apache Tomcat documentation can be found on http://tomcat.apache.org/. Also, minimal instructions for Tomcat installations on various operating systems can be found in associated Server Preparation pages:

  * [Tomcat on Mac OS X](TomcatInstallationMacOSX.md)
  * [Tomcat on Windows 7](TomcatInstallationWindows7.md)
  * [Tomcat on Cent OS 5](TomcatInstallationCentOS.md)

Download the ipt.war file of the latest release of the IPT from http://www.gbif.org/ipt or from http://code.google.com/p/gbif-providertoolkit/. Copy the ipt.war file to the Tomcat webapps folder and then start Tomcat, or restart Tomcat if it is already running. You can then invoke the IPT in a web browser running on the same server by using the following URL:

http://localhost:8080/ipt

If the installation doesn't start please check the catalina.out logfile, and refer to the [FAQ](FAQ.md) for help.

The following screencast also explains how to install the IPT using Tomcat, assuming Tomcat has already been installed.

<wiki:gadget url="https://gbif-providertoolkit.googlecode.com/svn/trunk/gbif-ipt-docs/ipt2/v22/vimeoSupport/gcVideo.xml" up\_video="https://vimeo.com/116142276" width="640" height="384"/>

### Jetty ###

The IPT comes equipped with a Jetty web server and servlet container. The IPT can be launched using Jetty if you have Java 6 (minimum), Maven, and Subversion installed (Mac OSX comes with all of these installed by default).

First, change the working directory to the location where you would like to put the IPT source code. Then issue the following commands (please check if 2.1.1 is still the latest stable version):

`$ svn checkout http://gbif-providertoolkit.googlecode.com/svn/tags/ipt-2.1.1 gbif-ipt`<br>
<code>$ cd gbif-ipt</code><br>
<code>$ mvn -Dmaven.test.skip=true</code><br>

The last "mvn" command should start the IPT with a local jetty instance on port 8080. Please note, this launches the IPT in testing mode, meaning that the checkbox "Production use" will be grayed out on Setup page II and registrations will be against the test registry "<a href='http://gbrdsdev.gbif.org'>http://gbrdsdev.gbif.org</a>" and not the live registry "<a href='http://gbrds.gbif.org'>http://gbrds.gbif.org</a>".<br>
<br>
To run in production mode, in place of the last "mvn" command use:<br>
<br>
<code>$ mvn -Dmaven.test.skip=true -P release</code><br>

This makes the checkbox "Production use" on Setup page II appear. It must be checked obviously to run in production. What this means is that registrations will be against the live registry "<a href='http://gbrds.gbif.org'>http://gbrds.gbif.org</a>" and not the test registry "<a href='http://gbrdsdev.gbif.org'>http://gbrdsdev.gbif.org</a>",<br>
<br>
Either way, you can then invoke the IPT in a web browser using the following URL :<br>
<br>
<a href='http://localhost:8080/ipt'>http://localhost:8080/ipt</a>

The first time the mvn command is run it will download many libraries and may take a long time, depending on the Internet data transfer rate. After this has been done once it will be it will be fairly fast thereafter, even if you update to the latest trunk again, which can be done by issuing the following commands from the root directory of the IPT source code:<br>
<br>
<code>$ cd gbif-ipt</code><br>
<code>$ svn up</code><br>

<h3>Wildfly8 (JBoss)</h3>

Instructions pending.<br>
<br>
<h2>Virtual Host Name</h2>

This section explains how to setup a virtual host name for your IPT (e.g. ipt.example.com) in Apache. A name-based virtual host is preferred over an IP-based virtual host because it is more aesthetically pleasing and easier to remember.<br>
<br>
<h3>Define Virtual Host Name</h3>

On RedHat and compatibles (e.g. Centos or Fedora) please make sure you have installed the "httpd" package (if not, "yum install httpd") then, in /etc/httpd/conf.d/ create a config file with a descriptive name (e.g. MyVhost.ipt.example.com.conf) in which you add<br>
<br>
<pre><code>&lt;VirtualHost *:80&gt;<br>
    DocumentRoot   /var/www/html/MyVhost   <br>
    ServerName  ipt.example.com<br>
    ServerAdmin webmaster@example.com<br>
    ErrorLog    logs/ipt.example.com-error_log<br>
    CustomLog   logs/ipt.example.com-access_log common<br>
&lt;/VirtualHost&gt;<br>
</code></pre>

You can combine the virtual host definition and the proxying feature presented below in one virtual host definition<br>
<br>
<pre><code>&lt;VirtualHost *:80&gt;<br>
    DocumentRoot   /var/www/html/MyVhost   <br>
    ServerName  ipt.example.com<br>
    ServerAdmin webmaster@example.com<br>
    ErrorLog    logs/ipt.example.com-error_log<br>
    CustomLog   logs/ipt.example.com-access_log common<br>
    ProxyPreserveHost       On<br>
    ProxyPass               /       http://apps.gbif-uat.org:8080/ipt/<br>
    ProxyPassReverse        /       http://apps.gbif-uat.org:8080/ipt/<br>
    ProxyPassReverseCookiePath  /ipt /<br>
&lt;/VirtualHost&gt;<br>
</code></pre>

After you have created the files, make sure to (re)start the Apache server.<br>
<br>
<h3>Configure Apache Reverse Proxy</h3>

If you wish to use Apache to proxy requests for example to Tomcat (because, for instance, your firewall blocks port 8080 and Apache is already running on port 80), you need to ensure that the cookies that Tomcat sets are handled correctly.<br>
<br>
Imagine the virtual host <a href='http://ipt.example.com'>http://ipt.example.com</a> has requests going through an Apache reverse proxy to an IPT deployed in Tomcat available at <a href='http://apps.gbif-uat.org:8080/ipt/'>http://apps.gbif-uat.org:8080/ipt/</a> (with no Tomcat Host defined). Here are the mod_proxy directives used:<br>
<br>
<pre><code>&lt;VirtualHost *:80&gt;<br>
    ServerName  ipt.example.com<br>
    ProxyPreserveHost       On<br>
    ProxyPass               /       http://apps.gbif-uat.org:8080/ipt/<br>
    ProxyPassReverse        /       http://apps.gbif-uat.org:8080/ipt/<br>
    ProxyPassReverseCookiePath  /ipt /<br>
&lt;/VirtualHost&gt;<br>
</code></pre>

Don't forget that the proxy and proxy_http modules have been loaded into Apache.<br>
