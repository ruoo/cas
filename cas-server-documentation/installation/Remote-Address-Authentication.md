---
layout: default
title: CAS - Remote Address Authentication
---

# Remote Address Authentication
This handler uses the request's remote address to transparently authenticate a user, having verified
the address against a range of configured IP addresses. The mechanics of this approach are very similar
to X.509 certificate authentication, but trust is instead placed on the client internal network address.

The benefit of this approach is that transparent authentication is achieved within a large corporate
network without the need to manage certificates.

<div class="alert alert-danger"><strong>Be Careful</strong><p>Keep in mind that this authentication
mechanism should only be enabled for internal network clients with relatively static IP addresses.</p></div>


## Caveats

This method of authentication assumes internal clients will be hitting the CAS server directly
and not coming via a web proxy. In the event of the client using the web proxy the likelihood
of the remote address lookup succeeding is reduced because to CAS the client address is that
of the proxy server and not the client. Given that this form of CAS authentication would typically
be deployed within an internal network this is generally not a problem.


## Authentication Components
Support is enabled by including the following dependency in the Maven WAR overlay:

{% highlight xml %}
<dependency>
  <groupId>org.jasig.cas</groupId>
  <artifactId>cas-server-support-generic</artifactId>
  <version>${cas.version}</version>
</dependency>
{% endhighlight %}

### Configuring Authentication

{% highlight xml %}
<alias name="remoteAddressAuthenticationHandler" alias="primaryAuthenticationHandler" />
{% endhighlight %}

The following settings are applicable:

{% highlight properties %}
# ip.address.range=192.168.1.0/255.255.255.0
{% endhighlight %}


### Configuring Webflow

Add the following action to the webflow configuration file:

{% highlight xml %}
<action-state id="startAuthenticate">
   <action bean="remoteAddressCheck" />
   <transition on="success" to="sendTicketGrantingTicket" />
   <transition on="error" to="viewLoginForm" />
</action-state>
{% endhighlight %}

You should appropriately evaluate your configuration to route the flow to `startAuthenticate` where needed.