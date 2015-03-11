Expression Middleware v0.3.0
============================

Extracts the sentiment from a spanish text using a machine learning model, uses Play Framework + Akka + Actor Model to optimize and distribute the computation, also is able to serve clients through web sockets.

Docs
----

Documentation for the web socket API and an image of the architecture are in the `docs` directory.

Setup
-----

After cloning or forking the repository just:

First setup your MongoDB instance. Then use the file on `conf/application.conf` to change settings, for example the Mongo database settings are in:

```
# MongoDB
mongo.host="localhost"
mongo.port=27017
mongo.db="expression"
```

Finally start the service.

```bash
cd /path/to/expression
./activator start
```

This will start the server on port 9000 and web sockets will be listening on path `http://localhost:9000/ws`

Check the Play Framework documentation for more configuration.

###Run Tests

```bash
./activator test
```

Licence
-------

The MIT License (MIT)

Copyright (c) 2015 Francisco Miguel Aramburo Torres

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
