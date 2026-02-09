package com.homedashboard.mqtt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homedashboard.model.Room;
import com.homedashboard.model.Measurement;
import com.homedashboard.service.MeasurementService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

@Component
public class MqttSubscriber {

    private final MeasurementService measurementService;
    private final ObjectMapper objectMapper;

    @Value("${mqtt.brokerUrl}")
    private String brokerUrl;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.clientId}")
    private String clientId;

    private MqttClient client;

    public MqttSubscriber(MeasurementService measurementService, ObjectMapper objectMapper) {
        this.measurementService = measurementService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void start() throws MqttException {
        client = new MqttClient(brokerUrl, clientId, null);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                handleMessage(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        client.connect(options);
        client.subscribe(topic);
        System.out.println("MQTT connected: " + brokerUrl + " subscribed to " + topic);
    }

    private void handleMessage(String fullTopic, MqttMessage message) throws Exception {
        // topic pattern: zigbee2mqtt/<deviceName>
        String deviceName = extractDeviceName(fullTopic);
        if (deviceName == null || deviceName.isBlank()) {
            return;
        }

        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(payload);

        // Ignore “update/state available” etc. Only store when real values exist
        boolean hasTemperature = json.has("temperature");
        boolean hasHumidity = json.has("humidity");
        boolean hasPressure = json.has("pressure"); // some devices publish this
        boolean hasBattery = json.has("battery");

        if (!hasTemperature && !hasHumidity && !hasPressure && !hasBattery) {
            return;
        }

        Optional<Room> room = Room.from(deviceName);
        if (room.isEmpty()) {
            return;
        }

        Measurement measurement = new Measurement();
        measurement.setRoom(room.get());
        measurement.setTs(ZonedDateTime.now().toOffsetDateTime());

        if (hasTemperature) {
            measurement.setTemperature(json.get("temperature").asDouble());
        }
        if (hasHumidity) {
            measurement.setHumidity(json.get("humidity").asDouble());
        }
        if (hasBattery) {
            measurement.setBatteryPercentage(json.get("battery").asInt());
        }

        measurementService.create(measurement);
    }

    private String extractDeviceName(String fullTopic) {
        // e.g. zigbee2mqtt/livingroom
        int idx = fullTopic.indexOf('/');
        if (idx < 0) return null;
        return fullTopic.substring(idx + 1);
    }

    private Room mapDeviceToRoom(String deviceName) {
        return Room.valueOf(deviceName.trim().toUpperCase(Locale.ROOT));
    }

    @PreDestroy
    public void stop() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
        }
        if (client != null) {
            client.close();
        }
    }
}
