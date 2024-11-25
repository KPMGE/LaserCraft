#include <WiFiClientSecure.h>
#include <Adafruit_SSD1306.h>
#include <PubSubClient.h>
#include <Adafruit_GFX.h>
#include <ezButton.h>
#include <WiFi.h>
#include <Wire.h>

#define RX 16
#define TX 17
#define BLACK_BUTTON 18
#define GREEN_BUTTON 15
#define BAUD_RATE 115200
#define SCREEN_WIDTH 128
#define SCREEN_HEIGHT 64
#define DEBOUNCE_TIME 50
#define DISPLAY_ADDR 0x3C

// WiFi credentials
const char* ssid = "";
const char* password = "";

// MQTT Broker details
const int mqtt_port = 0;
const char* mqtt_topic = "";
const char* mqtt_server = "";
const char* mqtt_password = "";
const char* mqtt_username = "";
const char* mqtt_next_chunk_topic = "";
const char* mqtt_next_chunk_message = "";
static const char *root_ca PROGMEM = R"EOF(
-----BEGIN CERTIFICATE-----
MIIFazCCA1OgAwIBAgIRAIIQz7DSQONZRGPgu2OCiwAwDQYJKoZIhvcNAQELBQAw
TzELMAkGA1UEBhMCVVMxKTAnBgNVBAoTIEludGVybmV0IFNlY3VyaXR5IFJlc2Vh
cmNoIEdyb3VwMRUwEwYDVQQDEwxJU1JHIFJvb3QgWDEwHhcNMTUwNjA0MTEwNDM4
WhcNMzUwNjA0MTEwNDM4WjBPMQswCQYDVQQGEwJVUzEpMCcGA1UEChMgSW50ZXJu
ZXQgU2VjdXJpdHkgUmVzZWFyY2ggR3JvdXAxFTATBgNVBAMTDElTUkcgUm9vdCBY
MTCCAiIwDQYJKoZIhvcNAQEBBQADggIPADCCAgoCggIBAK3oJHP0FDfzm54rVygc
h77ct984kIxuPOZXoHj3dcKi/vVqbvYATyjb3miGbESTtrFj/RQSa78f0uoxmyF+
0TM8ukj13Xnfs7j/EvEhmkvBioZxaUpmZmyPfjxwv60pIgbz5MDmgK7iS4+3mX6U
A5/TR5d8mUgjU+g4rk8Kb4Mu0UlXjIB0ttov0DiNewNwIRt18jA8+o+u3dpjq+sW
T8KOEUt+zwvo/7V3LvSye0rgTBIlDHCNAymg4VMk7BPZ7hm/ELNKjD+Jo2FR3qyH
B5T0Y3HsLuJvW5iB4YlcNHlsdu87kGJ55tukmi8mxdAQ4Q7e2RCOFvu396j3x+UC
B5iPNgiV5+I3lg02dZ77DnKxHZu8A/lJBdiB3QW0KtZB6awBdpUKD9jf1b0SHzUv
KBds0pjBqAlkd25HN7rOrFleaJ1/ctaJxQZBKT5ZPt0m9STJEadao0xAH0ahmbWn
OlFuhjuefXKnEgV4We0+UXgVCwOPjdAvBbI+e0ocS3MFEvzG6uBQE3xDk3SzynTn
jh8BCNAw1FtxNrQHusEwMFxIt4I7mKZ9YIqioymCzLq9gwQbooMDQaHWBfEbwrbw
qHyGO0aoSCqI3Haadr8faqU9GY/rOPNk3sgrDQoo//fb4hVC1CLQJ13hef4Y53CI
rU7m2Ys6xt0nUW7/vGT1M0NPAgMBAAGjQjBAMA4GA1UdDwEB/wQEAwIBBjAPBgNV
HRMBAf8EBTADAQH/MB0GA1UdDgQWBBR5tFnme7bl5AFzgAiIyBpY9umbbjANBgkq
hkiG9w0BAQsFAAOCAgEAVR9YqbyyqFDQDLHYGmkgJykIrGF1XIpu+ILlaS/V9lZL
ubhzEFnTIZd+50xx+7LSYK05qAvqFyFWhfFQDlnrzuBZ6brJFe+GnY+EgPbk6ZGQ
3BebYhtF8GaV0nxvwuo77x/Py9auJ/GpsMiu/X1+mvoiBOv/2X/qkSsisRcOj/KK
NFtY2PwByVS5uCbMiogziUwthDyC3+6WVwW6LLv3xLfHTjuCvjHIInNzktHCgKQ5
ORAzI4JMPJ+GslWYHb4phowim57iaztXOoJwTdwJx4nLCgdNbOhdjsnvzqvHu7Ur
TkXWStAmzOVyyghqpZXjFaH3pO3JLF+l+/+sKAIuvtd7u+Nxe5AW0wdeRlN8NwdC
jNPElpzVmbUq4JUagEiuTDkHzsxHpFKVK7q4+63SM1N95R1NbdWhscdCb+ZAJzVc
oyi3B43njTOQ5yOf+1CceWxG1bQVs5ZufpsMljq4Ui0/1lvh+wjChP4kqKOJ2qxq
4RgqsahDYVvTH9w7jXbyLeiNdd8XM2w9U/t7y0Ff/9yi0GE44Za4rF2LN9d11TPA
mRGunUHBcnWEvgJBQl9nJEiU0Zsnvgc/ubhPgXRR4Xq37Z0j4r7g1SgEEzwxA57d
emyPxgcYxn/eR44/KJ4EBs+lVDR3veyJm+kXQ99b21/+jh5Xos1AnX5iItreGCc=
-----END CERTIFICATE-----
)EOF";

// Machine states
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
bool is_first_time = true;

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
void wakeup_grbl();
void cycle_resume();
void setup_button();
void setup_serial();
void handle_states();
void setup_display();
void handle_buttons();
void setup_mqtt_client();
void get_next_gcode_chunk();
void display_print(String head, String msg);
void mqtt_callback(char* topic, byte* payload, unsigned int length);

// SETUP====================================
void setup() {
  setup_serial();  
  setup_display();
  setup_wifi();
  setup_mqtt_client();
  setup_button();
  wakeup_grbl();
}

// LOOP=======================================
void loop() {
  // if (!client.connected()) {
    // reconnect();
  // }
  while(!client.connected()){
    reconnect();
  }
  handle_states();
  handle_buttons();
  client.loop();
}

// Functions==================================
void setup_wifi() {
  display_print("Connecting wifi...", ssid);
  delay(1000);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    if (WiFi.status() == WL_CONNECT_FAILED) {
      display_print("Connection failed", ssid);
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
  //When it finishes receiving the gcode
  if (length == 0) {
    state = DONE;
    return;
  }

  //Parsing byte array to String
  char char_array[length+1];
  memcpy(char_array, payload, length);
  char_array[length] = '\n';
  char_array[length+1] = 0x0;
  gcode = char_array;

  start_pos = 0;
  state = PRINTING;
}

//=================
void send_gcode() {
  // Fiding "\n" index to substring a line once callback receives several lines.
  int end_pos = gcode.indexOf('\n', start_pos);
  
  // When all the lines have been sent, it gets next gcode chunk.
  if (end_pos == -1) {
    state = WAITING_GCODE;
    get_next_gcode_chunk();
    return;
  }

  String line = gcode.substring(start_pos, end_pos);
  Serial1.println(line);
  Serial.println(line); //DEBUG

  start_pos = end_pos + 1;
  display_print("Printing...", line);
  delay(10);  //DEBUG
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
  delay(100);
}

//====================
void handle_states() {
  switch (state) {
    case IDLE: 
      display_print("LaserCraft", "Waiting something to print...");
    break;

    case WAITING_GCODE: 
      display_print("Waiting for next g-code", "..."); 
    break;

    case DONE: 
      display_print("Printing done!", "Restart and re-home the machine before printing something else.");
      delay(5000);
      state = FREEZE;
    break;

    case PRINTING: 
      if(Serial1.available() > 0){
        // Receives grbl response messages
        String grbl_response = Serial1.readStringUntil('\n');
        Serial.println(grbl_response); //DEBUG

        //Does not need an "ok" to send the gcode if it's the first time
        if (is_first_time) {
         send_gcode();
         is_first_time = false; 
        }

        //Send next gcode if response is "ok"
        if (grbl_response.indexOf("ok") != -1) {
          send_gcode();
        }
      }
    break;

    case PAUSED:
    break;

    case FREEZE:  // If soft reset is activated
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
  display_print("Resuming...", "Restoring spindle!");
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

//===================
void setup_serial() {
  Serial.begin(BAUD_RATE);   // Serial monitor->
  Serial1.begin(BAUD_RATE, SERIAL_8N1, RX, TX);    // GRBL->
  delay(300);
}

//===================
void wakeup_grbl() {
  Serial1.print("\r\n\r\n");
  delay(2500);
  Serial1.flush();
}
