package com.smartcampus.api;

import com.smartcampus.data.DataStore;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore db = DataStore.getInstance();

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> allSensors = new ArrayList<>(db.getSensors().values());

        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = allSensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }
        return Response.ok(allSensors).build();
    }

    @POST
    public Response createSensor(Sensor newSensor) {
        if (newSensor.getRoomId() == null || !db.getRooms().containsKey(newSensor.getRoomId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Cannot create sensor: Room ID is invalid or missing\"}").build();
        }

        if (newSensor.getId() == null || newSensor.getId().isEmpty()) {
            newSensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        }
        db.getSensors().put(newSensor.getId(), newSensor);

        Room room = db.getRooms().get(newSensor.getRoomId());
        room.getSensorIds().add(newSensor.getId());

        return Response.status(Response.Status.CREATED).entity(newSensor).build();
    }

    // --- NEW PART 4 ENDPOINTS BELOW ---

    // 1. POST a new reading for a specific sensor
    @POST
    @Path("/{id}/readings")
    public Response recordReading(@PathParam("id") String sensorId, SensorReading reading) {
        // Verify sensor exists
        if (!db.getSensors().containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Sensor not found\"}").build();
        }

        // Auto-generate ID, timestamp, and set the sensor ID
        reading.setId("READ-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        reading.setSensorId(sensorId);
        reading.setTimestamp(LocalDateTime.now().toString());

        // Save the reading into the historical list for this sensor
        db.getReadings().computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);

        // Update the sensor's current live value
        db.getSensors().get(sensorId).setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    // 2. GET all historical readings for a specific sensor
    @GET
    @Path("/{id}/readings")
    public Response getSensorReadings(@PathParam("id") String sensorId) {
        if (!db.getSensors().containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Sensor not found\"}").build();
        }

        // Fetch the list of readings, or an empty list if there are none yet
        List<SensorReading> history = db.getReadings().getOrDefault(sensorId, new ArrayList<>());

        return Response.ok(history).build();
    }
}