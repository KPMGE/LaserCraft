#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>

#define RX 12
#define TX 13

// WiFi credentials
const char* ssid = "";
const char* password = "";

// MQTT Broker details
const char* mqtt_server = "";
const int mqtt_port = ;
const char* mqtt_username = "";
const char* mqtt_password = "";
const char *mqtt_topic = "laser_craft_gcode";

WiFiClientSecure espClient;
PubSubClient client(espClient);

void setup_wifi();
void setup_mqtt_client();
void list_available_ssids();
void reconnect();
void mqtt_callback(char* topic, byte* payload, unsigned int length);

void setup() {
  Serial.begin(115200);
  Serial1.begin(115200, SERIAL_8N1, RX, TX);
  setup_wifi();
  setup_mqtt_client();
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}

void setup_wifi() {
  delay(10);

  list_available_ssids();

  // Connect to WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");

    if (WiFi.status() == WL_CONNECT_FAILED) {
      Serial.println("Connection failed");
    }
  }

  Serial.println();
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void setup_mqtt_client() {
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqtt_callback);
}

void reconnect() {
  while (!client.connected()) {
    Serial.println("Attempting MQTT connection...");
    if (client.connect("ESP32Client", mqtt_username, mqtt_password)) {
      Serial.println("connected!");
      Serial.println("subscribing to topic: ");
      Serial.println(mqtt_topic);
      
      client.subscribe(mqtt_topic);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" Trying again in 5 seconds");
      delay(5000);
    }
  }
}

void mqtt_callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received [");
  Serial.print(topic);
  Serial.println("]: ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();
}

void list_available_ssids() {
  Serial.println("Scanning for networks...");

  int networks_amount = WiFi.scanNetworks();

  Serial.println("Scan done!");
  Serial.println("Available networks:");
  for (int i = 0; i < networks_amount; i++) {
    Serial.print(i + 1);
    Serial.print(": ");
    Serial.println(WiFi.SSID(i));
  }
}
