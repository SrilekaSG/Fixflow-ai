ALTER TABLE knowledge_articles
    ADD COLUMN common_causes TEXT NOT NULL DEFAULT '',
    ADD COLUMN suggested_fix TEXT NOT NULL DEFAULT '';

INSERT INTO knowledge_articles
    (id, title, category, keywords, body, common_causes, diagnostic_steps, verification_steps, suggested_fix)
VALUES
('pg-auth-failed',
 'PostgreSQL password authentication failed',
 'DATABASE',
 'password authentication failed, FATAL, PSQLException, invalid password, role, login failed, postgres authentication, pg_hba',
 'PostgreSQL rejected the login because the username or password sent by the application does not match a database role. The server is reachable, so this is a credentials or role problem rather than a network problem.',
 'The username or password in the application configuration does not match the database role
The environment variable or config file that supplies the password is empty, outdated, or not loaded
The database role was created with a different password, or the Docker volume was initialised with older credentials
The application connects to a different PostgreSQL instance than expected, for example a local install on the same port',
 'Check the username and password configured for the datasource (application.yml, environment variables, or run configuration) | Confirms what the application is really sending | LOW
Try logging in to the same database with the same user using psql or pgAdmin | Separates an application configuration problem from a database role problem | LOW
Check which PostgreSQL instance is listening on the configured host and port | Another local PostgreSQL on the same port can reject your credentials | LOW
If you use Docker, check whether the data volume was created earlier with different credentials | Credentials are only applied when the volume is first initialised | LOW',
 'Connect with psql or pgAdmin using the exact username and password from the configuration
Restart the application and confirm the datasource connects without the authentication error
Confirm the health endpoint or the first database query succeeds',
 'Make the application username and password match an existing database role. If the role password is wrong, reset it on the database side (for example with ALTER ROLE ... PASSWORD) and update the application configuration, then restart. Review any change before applying it to a shared or production database.'),

('db-connection-refused',
 'Connection refused to database host or port',
 'DATABASE',
 'connection refused, connection to localhost refused, PSQLException, ConnectException, postmaster, TCP/IP, port 5432, port 5433, unable to obtain connection, communications link failure',
 'The application could not open a network connection to the database. Nothing accepted the connection on the configured host and port, so the database is usually not running, is listening on a different port, or is blocked.',
 'The database server or Docker container is not running
The host or port in the datasource URL is wrong, for example 5432 instead of a mapped port
A firewall, VPN, or Docker network setting blocks the connection
Inside Docker, the application uses localhost instead of the database service name',
 'Check that the database is running (for Docker: docker ps) | A stopped container is the most common reason | LOW
Compare the host and port in the datasource URL with the port the database is actually published on | A single wrong digit is enough to be refused | LOW
Test the connection with psql or a database client using the same host and port | Confirms the problem is outside the application code | LOW
If the app runs inside Docker, use the service name from docker-compose as the host instead of localhost | Localhost inside a container refers to the container itself | LOW',
 'docker ps shows the database container as running and healthy
A database client connects using the same host and port
The application starts and the database health check reports UP',
 'Start the database and correct the host and port in the datasource URL so they match where the database is really listening. In Docker Compose, point the backend at the database service name and its internal port.'),

('db-unknown-database',
 'Unknown database or schema',
 'DATABASE',
 'database does not exist, unknown database, schema does not exist, relation does not exist, invalid catalog name, 3D000',
 'The server accepted the connection, but the database or schema named in the URL or SQL does not exist on that server. This often happens when a database was never created or the name has a typo.',
 'The database name in the datasource URL is misspelled or was never created
The application is connected to a different server than the one where the database was created
The schema or table has not been created yet, for example a migration did not run
The Docker volume was initialised before the database name was configured',
 'List the databases on the server (for example with psql and its list-databases command) | Shows whether the database name exists exactly as written | LOW
Compare the database name in the URL with the name shown by the server, including case | Names must match exactly | LOW
Check that the connection goes to the expected server, host, and port | A different server may have different databases | LOW
If a table or schema is missing, check whether the migration tool (such as Flyway) ran successfully | Missing tables often mean a migration did not run | LOW',
 'The database name appears in the server database list
The application connects and the migration history table is present
The query that failed earlier now runs without a missing-database or missing-relation error',
 'Create the missing database (or correct the name in the URL), or run the missing migration so that the schema and tables exist. Create or change databases only on environments you control, and back up important data first.'),

('java-null-pointer',
 'Java NullPointerException',
 'NULL_POINTER',
 'NullPointerException, NPE, null, cannot invoke because is null, helpful NullPointerException',
 'A NullPointerException means the code used a reference that was null, for example by calling a method on it. The message and the first stack frame from your own code usually show which expression was null and where.',
 'A variable, field, or method result was never assigned or returned null
A dependency such as a Spring bean was not injected because the object was created with new instead of by the container
Missing data from a database, request, or configuration value that the code assumed would exist
A map lookup or optional value was used without checking for absence',
 'Read the exception message: recent Java versions name the exact expression that was null | The message often points directly at the null value | LOW
Open the first stack frame that belongs to your own code and look at that line | That line is where the null value was used | LOW
Trace where the null value should have been assigned and check that code path | Finds where the value was lost | LOW
Check whether the object was created with new instead of being injected by Spring | Manually created objects do not get injected fields | LOW',
 'Add a temporary check or log (without sensitive data) to confirm which value is null, then remove it
Re-run the same request or test and confirm the exception no longer occurs
Add a unit test that covers the case where the value is missing',
 'Make sure the value is assigned before it is used, or handle the missing case explicitly (for example with a null check, Optional, or a validated default). Fix the place where the value is lost rather than only adding a null check at the failing line.'),

('spring-datasource-config',
 'Spring Boot failed to configure a DataSource',
 'CONFIGURATION',
 'Failed to configure a DataSource, url attribute is not specified, no embedded database driver, spring.datasource.url, DataSourceAutoConfiguration, driver class',
 'Spring Boot found database libraries on the classpath but no usable connection settings, so it could not create a DataSource. Usually spring.datasource.url is missing or the configuration file is not being loaded.',
 'spring.datasource.url (or the username and password) is missing from the configuration
The configuration file is in the wrong folder, has the wrong name, or a different profile is active
The JDBC driver dependency for your database is missing from the build file
An environment variable that should provide the URL is not set for this run',
 'Check that application.yml or application.properties is in src/main/resources and named correctly | Spring only loads configuration from known locations | LOW
Confirm spring.datasource.url, username, and password are present and correctly indented | A missing or misplaced key leaves the DataSource unconfigured | LOW
Check the active Spring profile and any environment variables the configuration depends on | A different profile may not contain the datasource settings | LOW
Confirm the JDBC driver dependency (for example the PostgreSQL driver) is in pom.xml | Without a driver Spring cannot connect | LOW',
 'The application starts without the DataSource configuration error
The startup log shows the connection pool starting
The health endpoint reports the database as UP',
 'Add the missing spring.datasource settings in the file Spring actually loads (or set the matching environment variables), and make sure the JDBC driver dependency is present. Keep passwords in environment variables, not in committed files.'),

('spring-no-qualifying-bean',
 'No qualifying bean of type found',
 'DEPENDENCY',
 'NoSuchBeanDefinitionException, No qualifying bean, required a bean of type that could not be found, autowired, component scan, UnsatisfiedDependencyException',
 'Spring needed an object (bean) of a certain type but could not find one. The class is usually not registered as a bean, or it sits outside the packages Spring scans.',
 'The class is missing a stereotype annotation such as Component, Service, Repository, or RestController
The class is in a package that is not below the main application class, so component scanning skips it
A required configuration or bean method is missing or conditionally disabled
Several beans of the same type exist and none is marked as the one to use',
 'Find the type named in the error and check that the class has a Spring annotation | Beans only exist when Spring knows about the class | LOW
Check that the class package is the same as or below the package of the main application class | Component scanning starts at the main class package | LOW
Check for Profile or Conditional annotations that might disable the bean in this run | A disabled bean is reported as missing | LOW
If more than one bean matches, check for Primary or Qualifier usage | Ambiguity causes a related injection error | LOW',
 'The application context starts without the missing-bean error
The injected component works when the endpoint or test that uses it is run
A Spring Boot test that loads the context passes',
 'Register the missing class as a Spring bean (add the right annotation or a bean method) and make sure it is within the scanned packages. Do not work around it by creating the object manually unless that is intentional.'),

('spring-bean-creation-failure',
 'Bean creation or dependency injection failure',
 'DEPENDENCY',
 'BeanCreationException, Error creating bean, UnsatisfiedDependencyException, BeanInstantiationException, Injection of autowired dependencies failed, circular dependency, Caused by',
 'Spring failed while creating one of the beans. BeanCreationException is a wrapper: the real problem is almost always in the last Caused by section at the bottom of the chain.',
 'A dependency of the bean failed to start, for example a database connection or a missing configuration value
A constructor or initialisation method threw an exception
A circular dependency between beans
A required property or placeholder could not be resolved',
 'Scroll to the last Caused by line in the stack trace and read that exception first | The wrapper message hides the real cause | LOW
Identify which bean name is mentioned and what it depends on | Shows which dependency actually failed | LOW
Check that required configuration values and external services (such as the database) are available | Many bean failures come from unavailable resources | LOW
Look for circular dependencies between the beans named in the error | Cycles can prevent bean creation | LOW',
 'The application starts and the startup log shows no BeanCreationException
The root cause shown in the last Caused by section no longer appears
A Spring Boot test that loads the context passes',
 'Fix the root cause named in the last Caused by section (for example a connection, a missing property, or a missing bean); the wrapper itself does not need fixing. If a circular dependency is reported, restructure the beans so they no longer depend on each other.'),

('port-already-in-use',
 'Port already in use',
 'CONFIGURATION',
 'Port 8080 was already in use, Address already in use, BindException, web server failed to start, port in use, server.port',
 'The application tried to listen on a port that another process already uses. Often an earlier run of the same application is still open, or another program uses the same port.',
 'A previous instance of the application is still running
Another program on the computer uses the same port
Two services in Docker Compose or two run configurations publish the same port',
 'Find which process uses the port (on Windows: netstat -ano and look for the port number) | Identifies the process holding the port | LOW
If it is an old instance of your application, stop it from your IDE or terminal | Frees the port | LOW
If it is another program, change server.port in the configuration to a free port | Avoids conflicts without closing the other program | LOW
Update anything that points to the old port, such as a frontend proxy or a Docker port mapping | Keeps the setup consistent | LOW',
 'The application starts and the log shows it listening on the expected port
Opening the application URL in a browser or API client returns a response
netstat shows the application process on the port',
 'Stop the process that holds the port or change server.port to a free port, then update every place that references the old port. Be careful when stopping processes you do not recognise.'),

('http-404-not-found',
 'HTTP 404 endpoint not found',
 'HTTP',
 '404, not found, NoHandlerFoundException, NoResourceFoundException, endpoint not found, wrong URL, request mapping, context path',
 'A 404 means the server received the request but has no resource for that path and method. In Spring Boot this is usually a URL, mapping, or context path mismatch.',
 'The URL path or HTTP method does not match the controller mapping
A context path or API prefix (such as /api/v1) is missing from the request
The controller class is not scanned, so its mappings are not registered
A static resource or frontend route is requested from the wrong server or port',
 'Compare the requested URL and HTTP method with the controller mapping annotations | Path and method must both match | LOW
Check server.servlet.context-path and any common prefix on the controller | A prefix changes the final URL | LOW
Check that the controller class is in a scanned package and annotated as a controller | Unscanned controllers register no endpoints | LOW
Check the API documentation (such as Swagger UI) for the exact path | Shows the paths the application really exposes | LOW',
 'The corrected URL returns a non-404 response
The endpoint appears in Swagger UI or in the mapping log at startup
An automated request test for the endpoint passes',
 'Correct the URL, HTTP method, or mapping so that they match, and make sure the controller is scanned. If a frontend calls the API, check its proxy or base URL setting too.'),

('http-500-server-error',
 'HTTP 500 internal server error',
 'HTTP',
 '500, internal server error, HttpServerErrorException, unhandled exception, server error, status 500',
 'A 500 means the server hit an unexpected error while handling the request. The status code alone says little: the real cause is in the server-side exception and stack trace.',
 'An unhandled exception in the controller, service, or database layer
A database or external service the endpoint depends on is unavailable
Unexpected or missing request data that the code does not handle',
 'Find the server-side log entry for the request and read the first exception and the last Caused by | The server log holds the real cause | LOW
Reproduce the request with the same input in a test or API client | Confirms the failure is repeatable | LOW
Check the dependencies of that endpoint (database, other services, configuration) | Many 500 errors come from unavailable dependencies | LOW
Add or review exception handling so that expected errors return a clear 4xx response | Avoids a generic 500 for known problems | LOW',
 'The same request no longer returns 500
The server log shows no new exception for the request
A test covers the failing case and passes',
 'Fix the underlying exception shown in the server log (this guide only identifies the type of problem), and handle expected error cases so the API returns a meaningful status code. Avoid exposing stack traces in API responses.'),

('missing-env-or-placeholder',
 'Missing environment variable or unresolved placeholder',
 'CONFIGURATION',
 'Could not resolve placeholder, Cannot resolve placeholder, missing environment variable, IllegalArgumentException, property not found, value annotation, environment variable not set',
 'A configuration value that the application expects is not defined. Spring reports the property name it could not resolve, which points to a missing environment variable or a missing configuration entry.',
 'The environment variable was never set, or is set in a different terminal, IDE run configuration, or container
The property name in the code and in the configuration file differ by a typo
The configuration file for this profile does not contain the property',
 'Note the exact property name shown in the error message | That name is what Spring could not find | LOW
Search the configuration files and environment for that name and check spelling and case | Typos and case differences are common | LOW
Check where the application is run (IDE run configuration, terminal, Docker Compose) and whether the variable is set there | Variables set in one place are not visible in another | LOW
Provide a safe default in the configuration if the value is optional | Optional settings should not stop startup | LOW',
 'The application starts without the placeholder error
Printing the property in a safe way (without secrets) shows the expected value
Restarting from a fresh terminal or run configuration still works',
 'Define the missing property or environment variable where the application runs, or add a default value in the configuration if it is optional. Keep secrets in environment variables and out of committed files.'),

('maven-build-failure',
 'Maven dependency resolution or build failure',
 'BUILD',
 'BUILD FAILURE, Could not resolve dependencies, Failed to execute goal, Could not transfer artifact, maven, pom.xml, compilation failure, plugin',
 'Maven could not finish the build. The summary line is short, so the useful part is the first ERROR message that names the goal, artifact, or file.',
 'A dependency version is wrong, missing, or not compatible with the Spring Boot or Java version
No network access to the Maven repository, or a proxy blocks it
A compilation error in the source code or a Java version mismatch
A corrupted local Maven repository entry',
 'Read the first ERROR line and note the artifact, plugin, or file it mentions | The first error is usually the real one | LOW
Check the Java version used by Maven against the java.version property in pom.xml | A mismatch causes build failures | LOW
Check network access to the repository and any proxy settings | Download failures show as could not transfer artifact | LOW
Re-run the build with the -e flag for the full error, and in the IDE use Maven Update Project with force update | Gives detail and refreshes cached data | LOW',
 'mvn clean package (or the IDE build) completes with BUILD SUCCESS
The dependency appears in the resolved dependency list
Tests run and pass after the build',
 'Correct the dependency coordinates or version (prefer versions managed by the Spring Boot parent), fix compile errors, and make sure Maven uses the intended Java version. Delete a corrupted artifact folder from the local repository only if the error names it.'),

('class-not-found',
 'ClassNotFoundException or NoClassDefFoundError',
 'DEPENDENCY',
 'ClassNotFoundException, NoClassDefFoundError, class not found, missing dependency, classpath, ExceptionInInitializerError, jar',
 'The JVM could not find a class at runtime. Either code compiled against a library that is missing or has a different version at run time, or a static initialiser failed earlier.',
 'A dependency is missing from the runtime classpath or was excluded
Two versions of a library conflict, so the needed class is not present
The IDE or build output is out of date and needs a clean rebuild
The class failed to initialise earlier (look for ExceptionInInitializerError above)',
 'Note the full class name in the error and identify which library it belongs to | Shows which dependency is missing | LOW
Check that the dependency is declared in pom.xml with a compatible version and the correct scope | Scope runtime or test may hide it | LOW
Run mvn dependency:tree to look for conflicts or exclusions | Reveals version conflicts | LOW
Do a clean build and refresh the IDE project | Removes stale compiled classes | LOW',
 'The application starts without the class loading error
The dependency tree shows one compatible version of the library
The feature that used the class works',
 'Add or correct the dependency so that a compatible version is on the runtime classpath, and resolve version conflicts. If NoClassDefFoundError follows an ExceptionInInitializerError, fix that earlier initialisation error first.'),

('invalid-json-validation',
 'Invalid JSON or request validation failure',
 'HTTP',
 'HttpMessageNotReadableException, JSON parse error, MethodArgumentNotValidException, ConstraintViolationException, validation failed, 400 bad request, unrecognized token, Jackson',
 'The server could not read the request body as the expected JSON, or the data failed validation rules. Usually this is a 400 response, and the message names the field or the position of the problem.',
 'The JSON is malformed (missing quote, comma, or bracket) or has the wrong content type
A field has the wrong type, such as text where a number is expected
A required field is missing or breaks a validation rule such as not blank or maximum size
The request body is empty',
 'Check the JSON with a validator or format it in your API client | Finds syntax problems quickly | LOW
Check that the Content-Type header is application/json | Wrong content type causes parse errors | LOW
Compare the fields and types in the request with the API documentation or request class | Type mismatches fail parsing | LOW
Read the validation message for the field name and the rule that failed | It names exactly what to change | LOW',
 'The corrected request returns a success status
The API response no longer contains validation errors
A test with a valid and an invalid body gives the expected results',
 'Send a valid JSON body that matches the expected fields and rules. If the rules themselves are wrong for valid users, adjust the validation annotations in the request class.');