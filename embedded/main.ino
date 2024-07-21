#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ezButton.h>

#define DEBOUNCE_TIME 50 // the debounce time in millisecond, increase this time if it still chatters

#define RX 16
#define TX 17

#define BLACK_BUTTON 2
#define GREEN_BUTTON 15

// WiFi credentials
const char* ssid = "";
const char* password = "";

// MQTT Broker details
const char* mqtt_server = "";
const int mqtt_port = 0;
const char* mqtt_username = "";
const char* mqtt_password = "";
const char* mqtt_topic = "";
const char* mqtt_next_chunk_topic = "";
const char* mqtt_next_chunk_message = "";
static const char *root_ca PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
-----END CERTIFICATE-----
)EOF";

enum State {
  IDLE,
  WAITING_GCODE,
  PRINTING,
  DONE
};

// Global variables
String gcode;
int start_pos = 0;
State state = IDLE;

// Objects
WiFiClientSecure espClient;
PubSubClient client(espClient);
ezButton black_button(BLACK_BUTTON); // create ezButton object that attach to pin GPIO21
ezButton green_button(GREEN_BUTTON); // create ezButton object that attach to pin GPIO21

// Functions
void setup_wifi();
void setup_mqtt_client();
void list_available_ssids();
void reconnect();
void mqtt_callback(char* topic, byte* payload, unsigned int length);
void get_next_gcode_chunk();

// SETUP====================================
void setup() {
  Serial.begin(115200);
  Serial1.begin(115200, SERIAL_8N1, RX, TX);
  delay(300);
  black_button.setDebounceTime(DEBOUNCE_TIME); // set debounce time to 50 milliseconds
  green_button.setDebounceTime(DEBOUNCE_TIME); // set debounce time to 50 milliseconds
  setup_wifi();
  setup_mqtt_client();
}

// LOOP=====================================
void loop() {
  switch (state) {
    case IDLE: 
      Serial.println("Idle...");
    break;

    case WAITING_GCODE: 
      Serial.println("Waiting GCODE to print...");
    break;

    case DONE: 
      Serial.println("Printing done!");
    break;

    case PRINTING: 
      if(Serial1.available() > 0){
        String gcode_result = Serial1.readStringUntil('\n');
        Serial.print("GCODE RESULT: ");
        Serial.println(gcode_result);
        if (gcode_result.indexOf("ok") != -1) {
          send_gcode();
        }
      }
    break;
  }

  black_button.loop(); // MUST call the loop() function first
  green_button.loop(); // MUST call the loop() function first

  if (black_button.isPressed()) {
    Serial.println("The black_button is pressed");
  }
  if (black_button.isReleased()) {
    Serial.println("The black_button is released");
  }
  if (green_button.isPressed()) {
    Serial.println("The green_button is pressed");
  }
  if (green_button.isReleased()) {
    Serial.println("The green_button is released");
  }
  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}

//==================
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

//=========================
void setup_mqtt_client() {
  espClient.setCACert(root_ca);
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqtt_callback);
}

//================
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

//===================================================================
void mqtt_callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message received [");
  Serial.print(topic);
  Serial.println("]: ");

  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();

  char char_array[length+1];
  memcpy(char_array, payload, length);
  char_array[length+1] = 0x0;
  gcode = char_array;

  Serial.println("GCODE: ");
  Serial.println(gcode);

  start_pos = 0;
  state = PRINTING;
}

//===========================
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

//=================
void send_gcode() {
  int end_pos = gcode.indexOf('\n', start_pos);
  if (end_pos == -1) {
    get_next_gcode_chunk();
    state = WAITING_GCODE;
    return;
  }

  String line = gcode.substring(start_pos, end_pos);
  Serial.println("Sending GCODE line: ");
  Serial.println(line);
  Serial1.println(line);

  start_pos = end_pos + 1;
}

//===========================
void get_next_gcode_chunk() {
  gcode = "";
  client.publish(mqtt_next_chunk_topic, mqtt_next_chunk_message);
}
