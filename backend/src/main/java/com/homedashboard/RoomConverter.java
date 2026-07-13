package com.homedashboard;

import com.homedashboard.model.Room;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RoomConverter implements AttributeConverter<Room, String> {

    @Override
    public String convertToDatabaseColumn(Room room) {
        return room == null ? null : room.name().toLowerCase();
    }

    @Override
    public Room convertToEntityAttribute(String room) {
        return room == null ? null : Room.valueOf(room);
    }
}
