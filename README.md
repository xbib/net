# Java Net API for servers and clients

## A consolidated Uniform Resource Locator implementation for Java

A Uniform Resource Locator (URL) is a compact representation of the
location and access method for a resource available via the Internet.

Historically, there are many different forms of internet resource representations, for example,
the URL (RFC 1738 as of 1994), the URI (RFC 2396 as of 1998), and IRI (RFC 3987 as of 2005),
and most of them have updated specifications.

This Java implementation serves as a universal point of handling all
different forms. It follows the syntax of the Uniform Resource Identifier (RFC 3986)
in accordance with the https://url.spec.whatwg.org/[WHATWG URL standard].

This alternative implementation of Uniform Resource Locator combines the features of the vanilla URI/URL Java SDK implementations
but removes it peculiarities and deficiencies, such as `java.lang.IllegalArgumentException: Illegal character in path at ... at java.net.URI.create()`

Normalization, NIO charset encoding/decoding, IPv6, an extensive set of schemes, and path matching have been added.

Fast building and parsing URLs, improved percent decoding/encoding, and URI templating features are included, to make
this library also useful in URI and IRI contexts.

While parsing and building, you have better control about address resolving. Only explicit `resolveFromhost` methods
will execute host lookup queries against DNS resolvers, otherwise, no resolving will occur under the hood.

You can build URLs with a fluent API, for example

```
URL.http().host("foo.com").toUrlString()
```

And you can parse URLs with a fluent API, for exmaple

```
URL url = URL.parser().parse("file:///foo/bar?foo=bar#fragment");
```

There is no external dependency. The size of the jar library is ~118k. The only dependency on `java.net` are the classes

```
java.net.IDN
java.net.Inet4Address
java.net.Inet6Address
java.net.InetAddress
```

which might get re-implemented in another library at a later time, in a project like Netty DNS resolver.

## A simple HTTP server

## A netty-based HTTP server

# License

Copyright (C) 2018 Jörg Prante

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
