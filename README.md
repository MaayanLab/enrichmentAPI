# API documentation

The API contains two main endpoints. One is for generation of new data called ```origin/api/v1/*``` and another endpoint is the primary access point to data queries ```api/v1/*```.

# Adding data to Signature Commons

Signature Commons can load libraries from a cloud repository on startup if the environmental variable ``` S3_AUTOLOAD ``` is set to ``` 'true' ```. 

#### Parameters
<table>
    <tr>
        <td><strong>Name</strong></td>
        <td><strong>Type</strong></td>
        <td><strong>Description</strong></td>
    </tr>
    <tr>
        <td><code>api_key</code></td>
        <td>string</td>
        <td>Your own API public key</td>
    </tr>
    <tr>
        <td><code>timestamp</code></td>
        <td>integer</td>
        <td>Current unix timestamp (GMT+0) in <a href="http://www.epochconverter.com/">seconds</a></td>
    </tr>
    <tr>
        <td><code>dev_hash</code></td>
        <td>string</td>
        <td>
            Calculate with <code>timestamp</code> and <code>api_secret</code>
            <br>
            Formula: <code>md5(concatenate(&lt;timestamp&gt;, &lt;api_secret&gt;))</code>
        </td>
    </tr>
</table>

# Deploying to rancher or marathon

When launching SigCom it requires several environmental variabls.

<table>
    <tr>
        <th>Environmental Variable</th>
        <th>Description</th>
    </tr>
    <tr><td>```TOKEN```</td><td>'pass code needed for several API endpoints'</td></tr>
    <tr><td>```PREFIX```</td><td>/sigcom/data-api</td></tr>
    <tr><td>```JAVA_OPTS```</td><td>'-Xmx30G -XX:PermSize=13G -XX:MaxPermSize=13G -XX:+UseCompressedOops'</td></tr>
    <tr><td>```deployment```</td><td>'marathon_deployed'</td></tr>
    <tr><td>```AWS_ACCESS_KEY_ID```</td><td>'access key id'</td></tr>
    <tr><td>```AWS_SECRET_ACCESS_KEY```</td><td>'aws key with S3 credentials to upload new data to AWS bucket'</td></tr>
    <tr><td>```AWS_ENDPOINT_URL```</td><td>'http://s3.amazonaws.com'</td></tr>
    <tr><td>```AWS_BUCKET_PREFIX```</td><td>'sigcom/'</td></tr>
    <tr><td>```AWS_BUCKET```</td><td>'name of bucket'</td></tr>
    <tr><td>```S3_AUTOLOAD```</td><td>'true/false'</td></tr>
TOKEN	'pass code needed for several API endpoints'
PREFIX	/sigcom/data-api
JAVA_OPTS	'-Xmx30G -XX:PermSize=13G -XX:MaxPermSize=13G -XX:+UseCompressedOops'
deployment	'marathon_deployed'
AWS_ACCESS_KEY_ID	'access key id'
AWS_SECRET_ACCESS_KEY	'aws key with S3 credentials to upload new data to AWS bucket'
AWS_ENDPOINT_URL	http://s3.amazonaws.com
AWS_BUCKET_PREFIX	sigcom/
AWS_BUCKET	'name of bucket'
S3_AUTOLOAD	'true/false'


# Installation and docker packaging

## Build with Gradle
### Important tasks

<table>
    <tr>
        <th>Task Name</th>
        <th>Description</th>
    </tr>
    <tr>
        <td>war</td>
        <td>Assembles the application WAR file and saves it in the <i>/docker</i> directory</td>
    </tr>
    <tr>
        <td>tomcatRunWar</td>
        <td>Starts a Tomcat instance and deploys the WAR to it</td>
    </tr>
    <tr>
        <td>clean</td>
        <td>Deletes the project build directory and files generated by tasks</td>
    </tr>
    <tr>
        <td>test</td>
        <td>Runs the unit tests using JUnit or TestNG</td>
    </tr>
    <tr>
        <td>check</td>
        <td>Aggregate task that performs verification tasks, such as running the tests</td>
    </tr>
    <tr>
        <td>compileJava</td>
        <td>Compiles production Java source files using the JDK compiler</td>
    </tr>
</table>

### Running tasks

To run gradle task, enter
```text
./gradlew {task_name}
```

### Documentation

<a href="https://docs.gradle.org/current/userguide/java_plugin.html">The Java Plugin documentation</a><br>
<a href="https://docs.gradle.org/current/userguide/war_plugin.html">The War Plugin documentation</a><br>
<a href="https://github.com/bmuschko/gradle-tomcat-plugin">The Tomcat Plugin documentation</a>

### Testing locally
This will launch the enrichment API on the local machine and should be accessible through the localhost.
```text
./gradlew tomcatRunWar
```

### Stoping gradle process
Processes need to be stopped manually if timcatRunWar has previously been executed.
```text
./gradlew stop
```

### Docker-compose
Getting started with docker-compose

```bash
gradle war
docker-compose build
docker-compose up
```
