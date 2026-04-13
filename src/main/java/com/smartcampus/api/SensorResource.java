package com.smartcampus.api;

import com.smartcampus.data.DataStore;
import com.smartcampus.models.Room;
import com.smartcampus.models.Sensor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    private DataStore db = DataStore.getInstance();

    // 1. GET all sensors (with optional query parameter for filtering by type)
    @GET
    public Response getSensors(@QueryParam("type") String type) {
        List<Sensor> allSensors = new ArrayList<>(db.getSensors().values());

        // If the user typed ?type=Temperature in the URL, filter the list
        if (type != null && !type.isEmpty()) {
            List<Sensor> filtered = allSensors.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
            return Response.ok(filtered).build();
        }

        return Response.ok(allSensors).build();
    }

    // 2. POST to create a new sensor and link it to a room
    @POST
    public Response createSensor(Sensor newSensor) {
        // Step A: Verify the room actually exists before creating the sensor
        if (newSensor.getRoomId() == null || !db.getRooms().containsKey(newSensor.getRoomId())) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Cannot create sensor: Room ID is invalid or missing\"}")
                    .build();
        }

        // Step B: Generate an ID and save the sensor
        if (newSensor.getId() == null || newSensor.getId().isEmpty()) {
            newSensor.setId("SENS-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        }
        db.getSensors().put(newSensor.getId(), newSensor);

        // Step C: Add this sensor's new ID to the Room's internal list of sensors
        Room room = db.getRooms().get(newSensor.getRoomId());
        room.getSensorIds().add(newSensor.getId());

        return Response.status(Response.Status.CREATED).entity(newSensor).build();
    }
}
