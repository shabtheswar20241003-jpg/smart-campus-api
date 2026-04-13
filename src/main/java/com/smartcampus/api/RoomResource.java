package com.smartcampus.api;

import com.smartcampus.data.DataStore;
import com.smartcampus.models.Room;
import com.smartcampus.exceptions.RoomNotEmptyException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private DataStore db = DataStore.getInstance();

    // 1. GET all rooms
    @GET
    public Response getAllRooms() {
        List<Room> roomList = new ArrayList<>(db.getRooms().values());
        return Response.ok(roomList).build();
    }

    // 2. GET a specific room by ID
    @GET
    @Path("/{id}")
    public Response getRoomById(@PathParam("id") String id) {
        Room room = db.getRooms().get(id);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found\"}").build();
        }
        return Response.ok(room).build();
    }

    // 3. POST to create a new room
    @POST
    public Response createRoom(Room newRoom) {
        // Generate a random ID if the user didn't provide one
        if (newRoom.getId() == null || newRoom.getId().isEmpty()) {
            newRoom.setId("ROOM-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase());
        }

        db.getRooms().put(newRoom.getId(), newRoom);
        return Response.status(Response.Status.CREATED).entity(newRoom).build();
    }

    // 4. DELETE a room (with the custom exception check!)
    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") String id) {
        Room room = db.getRooms().get(id);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Room not found\"}").build();
        }

        // Check if room has sensors (triggers our custom exception)
        if (room.getSensorIds() != null && !room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException("Cannot delete room " + id + " because it contains active sensors.");
        }

        db.getRooms().remove(id);
        return Response.noContent().build(); // Returns HTTP 204 (Success, no content)
    }
}