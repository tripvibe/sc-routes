# sc-routes project

Routes near me. Using PTV Timetable API you need a developer id and apikey from [here.](http://timetableapi.ptv.vic.gov.au/swagger/ui/index)

Quarkus, mutiny reactive web, rest-easy, rest-client. Bootstrap, jquery UI.

Start infinispan locally
```bash
make podman-run 
```

Running locally
```bash
mvn quarkus:dev -DDEVID=<dev id> -DAPIKEY=<api key>
```

User Interface
```bash
http://localhost:8080/
```

Client testing server sent events
```bash
http localhost:8080/api/routes/-37.8974484,145.088703 --stream
```

Swagger exposes API
```
http://localhost:8080/swagger-ui/
```

#### Notes on Infinispan authentication

Locally
```
-- exec container
podman exec -it ispn1 /bin/bash
podman exec -it ispn2 /bin/bash

-- do this on BOTH nodes
/opt/infinispan/bin/user-tool.sh
Specify a username: admin
Set a password for the user: 
Confirm the password for the user: 
bash-4.4$ exit
Test CLI

podman exec -it ispn1 /opt/infinispan/bin/cli.sh
[disconnected]> connect http://127.0.0.1:11222
Username: admin
Password: *****

create cache --template=org.infinispan.DIST_SYNC distcache
cache distcache
put k1 v1
put k2 v2
ls
get k1
```

