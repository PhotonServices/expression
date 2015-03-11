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

MIT
