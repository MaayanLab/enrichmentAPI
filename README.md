### Gradle

##### Tasks

<table>
    <tr>
        <th>Task Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>bootRun</td>
        <td>Runs the Spring Boot application</td>
    </tr>
    <tr>
        <td>bootJar</td>
        <td>Creates an executable jar</td>
    </tr>
    <tr>
        <td>bootWar</td>
        <td>Creates an executable war</td>
    </tr>
    <tr>
        <td>test</td>
        <td>Run all tests of the application</td>
    </tr>
</table>

##### Run tasks

To run gradle task, enter
```text
./gradlew {task_name}
```

##### Documentation

- <a href="https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/pdf/spring-boot-gradle-plugin-reference.pdf">Spring Boot Gradle Plugin Reference Guide</a><br>
- <a href="https://docs.gradle.org/current/userguide/java_plugin.html">The Java Plugin documentation</a><br>
- <a href="https://docs.gradle.org/current/userguide/war_plugin.html">The War Plugin documentation</a><br>




### Configuration

Configuration stored in  `src/main/resources/application.yml`:

```
server:
  port:
    8080
ignite:
  addresses:
    127.0.0.1:8446, 127.0.0.2:8446
  clientPort:
    8443
  restPort:
    8444
  communicationSPIPort:
    8445
  discoverySPIPort:
    8446
  storage:
    /opt/data
  addToBaselineTopology:
    true

logging:
  file: ./logs.log
  level:
    enrichmentapi.calc: DEBUG
```


##### Available parameters 

| Parameter | Meaning | Default |
|:-----------|---------|---------|
| server.port                  | Enrichment API port         | 8080 |
| ignite.addresses             | list of Ignite nodes        |  |
| ignite.clientPort            | Client connector port (JDBC Thin port) | 10800 |
| ignite.communicationSPIPort  | Communication SPI port      | 47100 |
| ignite.discoverySPIPort      | Discovery SPI port          | 47500 |
| ignite.restPort              | REST access port            | 11211 |
| ignite.storage               | Location of Ignite files    |  |
| ignite.addToBaselineTopology | Add node to baseline topology | false |
| logging.file                 | Path to log file            |       |

##### Documentation
- <a href="https://apacheignite.readme.io/docs">Ignite documentation</a><br>

### Docker

To run Enrichment API in a Docker container host network has to be used, otherwise Ignite nodes won't be able
to create cluster. Also all described above ports have to be available between cluster instances.

Example:
```
docker run --rm -d \
           --network=host \            # using host network
           -v /opt/data:/opt/data \    # bind mount of Ignite data direcotry
           --name=enrichmentapi \
           enrichmentapi
```

##### Docker compose
To deploy application as part of Compose cluster could be used next configuration:
```
...

data-api:
  build: ./data-api
  network_mode: "host"        # host network
  volumes:
    - /opt/data:/opt/data     # Ignite data directory
...
```

Also could be useful to add alias to enrichmentAPI host:
```
extra_hosts:
  - "data-api:10.0.0.1"
```

### Activation of Ignite cluster

When the application is launched for the first time, the Ignite cluster must be activated. This must be done only once. 

To activate Ignite cluster, make a call to `GET /api/v1/activate`.


### Upload of data from SO files to Ignite

**POST** /api/v1/download-so

```text
{
	"datasetType": "geneset_library" or "rank_matrix",
	"fileName": "path/to/so/file",
	"name": "name of the dataset (e.g. creeds_uid)"
}
```

If you want to upload files into docker version of EnrichmentAPI ensure that `fileName` (path to file) is
available inside a container. For example, you can mount directory with SO files inside the container and specify  `fileName` 
accordingly.

##### Swagger

Description of API available on `/swagger-ui.html`