#include <Adafruit_SSD1306.h> //INCLUSÃO DE BIBLIOTECA
#include <SoftwareSerial.h>
#include <Adafruit_GFX.h> //INCLUSÃO DE BIBLIOTECA
#include <string.h>
#include <Wire.h> //INCLUSÃO DE BIBLIOTECA

#define SCREEN_WIDTH 128 // OLED display width, in pixels
#define SCREEN_HEIGHT 64 // OLED display height, in pixels

// Objects
SoftwareSerial myUart(16, 17);
Adafruit_SSD1306 display(SCREEN_WIDTH, SCREEN_HEIGHT, &Wire, -1);

// Variables
String gcode = 
"M5\n" \
"G1 X15 Y30 F500\n" \
"M3 S300\n" \
"G1 X11 Y19 F30\n" \
"G1 X2 Y19\n" \
"G1 X0 Y19\n" \
"G1 X9 Y12\n" \
"G1 X4 Y0\n" \
"G1 X15 Y6\n" \
"G1 X24 Y1\n" \
"G1 X26 Y0\n" \
"G1 X21 Y12\n" \
"G1 X30 Y19\n" \
"G1 X28 Y19\n" \
"G1 X19 Y19\n" \
"G1 X16 Y28\n" \
"G1 X15 Y30\n" \
"G1 X15 Y30\n" \
"M5\n" \
"G1 X15 Y28\n" \
"M3 S300\n" \
"G1 X12 Y18\n" \
"G1 X2 Y18\n" \
"G1 X10 Y12\n" \
"G1 X6 Y2\n" \
"G1 X15 Y7\n" \
"G1 X25 Y2\n" \
"G1 X21 Y12\n" \
"G1 X28 Y18\n" \
"G1 X18 Y18\n" \
"G1 X15 Y28\n" \
"G1 X15 Y28\n" \
"M5\n" \
"G1 X11 Y18\n" \
"M3 S300\n" \
"G1 X11 Y16\n" \
"G1 X12 Y16\n" \
"G1 X14 Y8\n" \
"G1 X16 Y8\n" \
"G1 X18 Y16\n" \
"G1 X19 Y16\n" \
"G1 X19 Y18\n" \
"G1 X16 Y18\n" \
"G1 X16 Y16\n" \
"G1 X17 Y16\n" \
"G1 X15 Y10\n" \
"G1 X13 Y16\n" \
"G1 X14 Y16\n" \
"G1 X14 Y18\n" \
"G1 X11 Y18\n" \
"G1 X11 Y18\n" \
"M5\n" \
"M2\n";

int startPos = 0;

// ======================
void setup() {
  Serial.begin(115200);
  myUart.begin(115200);
  delay(300);
  send_gcode();

  if(!display.begin(SSD1306_SWITCHCAPVCC, 0x3C)) {
    Serial.println(F("SSD1306 allocation failed"));
    for(;;);
  }
  delay(2000);
  display.clearDisplay();
  display.setTextSize(2);
  display.setTextColor(WHITE);
  
  display.setCursor(0, 0);
  display.println("Printing...");

  display.display(); 
}

// ============================================
void loop(){
  if(myUart.available() > 0){
    delay(50);
    String gcode_result = myUart.readStringUntil('\n');
    delay(50);

    // Serial.println("GCODE RESULT: ");

    display.setCursor(0, 20);
    display.println(gcode_result);
    display.display();

    Serial.println(gcode_result);
    delay(50);

    if (gcode_result.indexOf("ok") != -1) {
      send_gcode();
    }
  }
}

// ==================================================
void send_gcode() {
  Serial.println("SEND GCODE");
  int endPos = gcode.indexOf('\n', startPos);
  if (endPos == -1) {return;}
  String line = gcode.substring(startPos, endPos);
  Serial.println("Sending line: ");
  Serial.println(line);
  myUart.println(line);
  startPos = endPos + 1;
}
