#include <WiFiClientSecure.h>
#include <Adafruit_SSD1306.h>
#include <PubSubClient.h>
#include <Adafruit_GFX.h>
#include <ezButton.h>
#include <WiFi.h>
#include <Wire.h>

// the debounce time in milliseconds, increase this time if it still chatters
#define DEBOUNCE_TIME 100

#define RX 16
#define TX 17

#define BLACK_BUTTON 18
#define GREEN_BUTTON 15

#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64

#define DISPLAY_ADDR 0x3C

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
  DONE,
  PAUSED,
  FREEZE
};

// Global variables
String gcode;
int start_pos = 0;
State state = IDLE;

// Objects
WiFiClientSecure espClient;
PubSubClient client(espClient);
ezButton black_button(BLACK_BUTTON);
ezButton green_button(GREEN_BUTTON);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// Functions
void feed_hold();
void reconnect();
void soft_reset();
void setup_wifi();
void cycle_resume();
void setup_button();
void handle_states();
void setup_display();
void handle_buttons();
void setup_mqtt_client();
void get_next_gcode_chunk();
void display_print(String head, String msg);
void mqtt_callback(char* topic, byte* payload, unsigned int length);

// SETUP====================================
void setup() {
  Serial.begin(115200);   // Serial monitor->
  Serial1.begin(115200, SERIAL_8N1, RX, TX);    // GRBL->
  delay(300);

  setup_display();
  display_print("Loading...", "");

  setup_wifi();
  setup_mqtt_client();
  setup_button();
}

// LOOP=======================================
void loop() {
  handle_states();
  handle_buttons();

  if (!client.connected()) {
    reconnect();
  }
  client.loop();
}

// Functions==================================
void setup_wifi() {
  delay(10);

  display_print("Connecting wifi...", ssid);
  delay(1000);
  
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    if (WiFi.status() == WL_CONNECT_FAILED) {
      display_print("Connection failed", "");
    }
  }

  display_print("WiFi connected", WiFi.localIP().toString());
  delay(1500);
}

//==========================
void setup_mqtt_client() {
  espClient.setCACert(root_ca);
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(mqtt_callback);
}

//================
void reconnect() {
  while (!client.connected()) {
    if (client.connect("ESP32Client", mqtt_username, mqtt_password)) {
      client.subscribe(mqtt_topic);
      display_print("MQTT Connected", mqtt_topic);
      delay(1000);
    } else {
      display_print("MQTT failure", "rc="+client.state());
      delay(5000);
    }
  }
}

//===================================================================
void mqtt_callback(char* topic, byte* payload, unsigned int length) {
  if (length == 0) {
    state = DONE;
    return;
  }

  char char_array[length+1];
  memcpy(char_array, payload, length);
  char_array[length] = '\n';
  char_array[length+1] = 0x0;
  gcode = char_array;

  start_pos = 0;
  state = PRINTING;
  send_gcode();
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
  Serial1.println(line);

  start_pos = end_pos + 1;
  display_print("Printing...", line);
}

//===========================
void get_next_gcode_chunk() {
  gcode = "";
  client.publish(mqtt_next_chunk_topic, mqtt_next_chunk_message);
}

//=========================================
void display_print(String head, String msg) {
  display.clearDisplay();
  display.setCursor(0, 0);
  display.println(head);
  display.setCursor(0, 20);
  display.println(msg);
  display.display(); 
}

//====================
void setup_display() {
  if(!display.begin(SSD1306_SWITCHCAPVCC, DISPLAY_ADDR)) {
    Serial.println(F("SSD1306 allocation failed"));
    for(;;);
  }
  delay(2000);
  display.clearDisplay();
  display.setTextSize(1);
  display.setTextColor(WHITE);
}

//====================
void handle_states() {
  switch (state) {
    case IDLE: 
      display_print("LaserCraft", "Waiting something to print...");
    break;

    case WAITING_GCODE: 
      display_print("Waiting g-code", "..."); 
    break;

    case DONE: 
      display_print("Printing done!", "");
      delay(1000);
      state = IDLE;
    break;

    case PRINTING: 
      if(Serial1.available() > 0){
        String gcode_result = Serial1.readStringUntil('\n');

        if (gcode_result.indexOf("ok") != -1) {
          send_gcode();
        }
      }
    break;

    case PAUSED:
    break;

    case FREEZE:
      while(true){}
    break;

    default:
      display_print("ERROR!", "Invalid state!");
    break;
  }
}

//=====================
void handle_buttons() {
  black_button.loop();
  green_button.loop();

  if (black_button.isPressed()) {
    soft_reset();
  }
  if (green_button.isPressed()) {
    if(state == PAUSED){
      cycle_resume();
      return;
    }
    feed_hold();
  }
}

//===================
void setup_button() {
  black_button.setDebounceTime(DEBOUNCE_TIME);
  green_button.setDebounceTime(DEBOUNCE_TIME);
}

//===============
void feed_hold() {
  Serial1.println("!");
  state=PAUSED;
  display_print("System paused", "Press green button to resume!");
}

//===================
void cycle_resume() {
  display_print("Resuming...", "Here we go again!");
  delay(1500);
  Serial1.println("~");
  state=PRINTING;
  delay(100);
}

//=================
void soft_reset() {
  display_print("Soft reset...", "Restart and re-home the machine!");
  Serial1.write(0x18);
  Serial1.print("\n");
  state = FREEZE;
}
