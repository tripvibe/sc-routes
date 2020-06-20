# sc-routes project

Routes near me. Using PTV Timetable API you need a developer id and apikey from [here.](http://timetableapi.ptv.vic.gov.au/swagger/ui/index)

Quarkus, mutiny reactive web, rest-easy, rest-client. Bootstrap, jquery UI.

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
