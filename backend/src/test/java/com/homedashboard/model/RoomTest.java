package com.homedashboard.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class RoomTest {

    @Test
    void parse_valid_rooms() {
        assertThat(Room.from("LIVINGROOM")).contains(Room.LIVINGROOM);
        assertThat(Room.from("BEDROOM")).contains(Room.BEDROOM);
        assertThat(Room.from("OFFICE")).contains(Room.OFFICE);
        assertThat(Room.from("KITCHEN")).contains(Room.KITCHEN);
        assertThat(Room.from("TOILET")).contains(Room.TOILET);
    }

    @Test
    void parsing_ignores_case() {
        assertThat(Room.from("livingroom")).contains(Room.LIVINGROOM);
        assertThat(Room.from("bedroom")).contains(Room.BEDROOM);
        assertThat(Room.from("office")).contains(Room.OFFICE);
        assertThat(Room.from("kitchen")).contains(Room.KITCHEN);
        assertThat(Room.from("toilet")).contains(Room.TOILET);
    }

    @Test
    void parsing_null_returns_optional_empty() {
        assertThat(Room.from(null)).isEmpty();
    }

    @Test
    void parsing_blank_returns_optional_empty() {
        assertThat(Room.from("")).isEmpty();
    }

    @Test
    void parsing_unknown_room_returns_optional_empty() {
        assertThat(Room.from("NOSUCHROOM")).isEmpty();
    }

    @Test
    void parsing_with_whitespaces_returns_room() {
        assertThat(Room.from(" KITCHEN ")).contains(Room.KITCHEN);
    }

    @Test
    void parsing_whitespaces_returns_optional_empty() {
        assertThat(Room.from(" ")).isEmpty();
    }
}
