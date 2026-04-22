Markdown
# Smart Campus Sensor & Room Management API

## 1. Overview of API Design
This project implements a robust, RESTful web service for managing a "Smart Campus" infrastructure. Built using Java and the JAX-RS (Jersey) framework, the API manages physical **Rooms**, IoT **Sensors** deployed within those rooms, and historical **Sensor Readings**. 

The architecture strictly adheres to REST principles, utilizing appropriate HTTP methods (GET, POST, DELETE), proper status codes (200, 201, 204, 400, 404, 409, 422, 500), and a nested resource hierarchy (`/sensors/{id}/readings`). Data is managed via a thread-safe, in-memory Singleton `DataStore` to persist state across the transient lifecycles of JAX-RS resource classes. Advanced error handling is implemented via custom `ExceptionMapper` classes to ensure the API remains resilient and "leak-proof".

---

## 2. Build and Launch Instructions
Follow these steps to compile and run the server locally.

**Prerequisites:**
* Java Development Kit (JDK) 11 or higher installed.
* Maven installed (or use the wrapper provided by your IDE).
* An IDE such as IntelliJ IDEA or Eclipse.

**Steps to Launch:**
1. **Clone the repository:** ```bash
   git clone <PASTE_YOUR_GITHUB_REPO_URL_HERE>
   cd smart-campus-api'''

2. Build the project: Let Maven download the necessary JAX-RS and Grizzly server dependencies.

'''Bash
mvn clean install'''

3.Start the Server: Locate the com.smartcampus.Main class in your src/main/java directory. Run the main() method from your IDE.

4. Verify: The console will output: Jersey app started with endpoints available at http://localhost:8080/api/v1/


##3. Sample API Interactions (cURL Commands)
Ensure the server is running. You can test the API by running these commands in your terminal. Note: For commands 4 and 5, you will need to replace <ROOM_ID> and <SENSOR_ID> with the actual IDs generated from the previous steps.

1. View API Discovery Metadata:

'''Bash
curl -X GET http://localhost:8080/api/v1/
'''
2. Create a New Room:

'''Bash
curl -X POST http://localhost:8080/api/v1/rooms \
-H "Content-Type: application/json" \
-d '{"name": "Main Library", "capacity": 150}'
'''
3. Retrieve All Rooms:

'''Bash
curl -X GET http://localhost:8080/api/v1/rooms
'''

4. Register a Sensor in a Room: (Replace <ROOM_ID>)

'''Bash
curl -X POST http://localhost:8080/api/v1/sensors \
-H "Content-Type: application/json" \
-d '{"type": "Temperature", "status": "ACTIVE", "roomId": "<ROOM_ID>"}'
'''

5. Record a New Sensor Reading: (Replace <SENSOR_ID>)

'''Bash
curl -X POST http://localhost:8080/api/v1/sensors/<SENSOR_ID>/readings \
-H "Content-Type: application/json" \
-d '{"value": 22.5}'
'''

##4. Conceptual Report

###Part 1: Service Architecture & Setup

####Question: Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.####

By default, JAX-RS utilizes a per-request lifecycle, meaning a brand new instance of the Resource class is instantiated for every single incoming HTTP request, and then destroyed immediately after the response is sent. If we used standard instance variables to hold our data, the data would be wiped out the moment the request finished. To prevent this data loss, our implementation utilizes the Singleton design pattern for a central DataStore class. By holding our Maps inside a Singleton, all transient JAX-RS resource instances access the exact same persistent object in memory.

####Question: Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?####
Hypermedia as the Engine of Application State (HATEOAS) makes an API discoverable and self-documenting. Instead of forcing client developers to hardcode specific URL paths based on static documentation, the API response itself provides the navigational links (URLs) required to interact with related resources. This decouples the client from the server's routing structure. If the backend architecture changes and URIs are updated, the client will not break because it dynamically follows the URLs provided in the server's JSON response.

###Part 2: Room Management
####Question: When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.####
Returning only IDs heavily conserves network bandwidth per request, resulting in a smaller JSON payload. However, it creates an "N+1 query problem" for the client, forcing them to make subsequent network calls for every single ID just to retrieve the room details, drastically increasing latency and client-side processing overhead. Returning the full room objects increases the payload size and bandwidth consumption of the initial request, but drastically reduces the total number of HTTP requests required, offering a smoother experience.

####Question: Is the DELETE operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same DELETE request for a room multiple times.####
Yes, the DELETE operation is idempotent. In REST, idempotency means that making multiple identical requests has the same effect on the server's state as making a single request. If a client sends a DELETE request for a room, the server removes it and returns a 204 No Content. If the client sends the exact same request again, the server simply returns a 404 Not Found because the room no longer exists. The state of the server (the room being absent) remains exactly the same after the second request as it did after the first.

###Part 3: Sensor Operations & Linking
####Question: We explicitly use the @Consumes(MediaType.APPLICATION_JSON) annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as text/plain or application/xml. How does JAX-RS handle this mismatch?####
The @Consumes annotation acts as a strict gateway filter. If a client sends a payload with a Content-Type header of text/plain or application/xml, the JAX-RS container intercepts the request before it even reaches our Java method logic. Because the container cannot find an exact match for the requested media type, it automatically aborts the routing and returns a standard HTTP 415 Unsupported Media Type error to the client.

####Question: You implemented this filtering using @QueryParam. Contrast this with an alternative design where the type is part of the URL path (e.g., /api/v1/sensors/type/CO2). Why is the query parameter approach generally considered superior for filtering and searching collections?####
Path parameters are semantically designed to uniquely identify a specific resource within a hierarchy. Query parameters are designed to provide optional parameters to an operation on a collection (e.g., filtering or sorting). If we used URL paths for filtering, the API becomes rigid and brittle; filtering by both type and status simultaneously would result in complex and deeply nested URLs. Query parameters (?type=CO2&status=ACTIVE) remain flexible, optional, and adhere to standard REST conventions.

###Part 4: Deep Nesting with Sub-Resources
####Question: Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path in one massive controller class?####
The Sub-Resource Locator pattern drastically improves maintainability and adheres to the Single Responsibility Principle. In a monolithic controller class, defining every nested path leads to "god classes" that are massive and difficult to maintain. By delegating the nested paths to a dedicated SensorReadingResource class, the code becomes highly modular. Each class manages only its specific domain entity, making the API easier to test, debug, and scale.

###Part 5: Advanced Error Handling, Exception Mapping & Logging
####Question: Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?####
A 404 Not Found strictly implies that the requested URI endpoint itself does not exist. However, when posting a sensor, the URI (/sensors) is correct, and the JSON syntax is perfectly valid. The failure occurs because a semantic business rule was broken (the roomId provided inside the payload does not reference an existing room). 422 Unprocessable Entity accurately communicates that the server understands the content type and syntax of the request entity, but was unable to process the contained instructions due to semantic logic errors.

####Question: From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?####
Exposing raw Java stack traces constitutes a severe "Information Disclosure" vulnerability. Stack traces leak explicit details about the application's internal architecture, including the specific frameworks being used, internal package structures, class names, and sometimes even file paths on the host operating system. Attackers utilize this reconnaissance data to search for known Common Vulnerabilities and Exposures (CVEs) specific to those exact framework versions.

####Question: Why is it advantageous to use JAX-RS filters for cross-cutting concerns like logging, rather than manually inserting Logger.info() statements inside every single resource method?####
Utilizing JAX-RS filters implements Aspect-Oriented Programming (AOP). Cross-cutting concerns like logging apply to the entire application. If developers manually inserted Logger.info() into every method, it would create massive code duplication and introduce human error. Filters centralize this logic; a single ContainerRequestFilter guarantees that every single incoming request is logged consistently, cleanly separating operational concerns from core business logic.
