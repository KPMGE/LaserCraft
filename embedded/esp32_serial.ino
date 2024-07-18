#include <SoftwareSerial.h>
// #define SERIAL_SIZE_RX 1024

SoftwareSerial myUart(16, 17);

void setup() {
  Serial.begin(115200);
  myUart.begin(115200);
}

void loop() {
  if(Serial.available()>0){
    byte x = Serial.read();
    myUart.write(x);
  } 
  if(myUart.available()>0){
    Serial.write((char)myUart.read());
  }
}
