#include <ezButton.h>

#define DEBOUNCE_TIME 50 // the debounce time in millisecond, increase this time if it still chatters

ezButton button(21); // create ezButton object that attach to pin GPIO21

void setup() {
  Serial.begin(115200);
  button.setDebounceTime(DEBOUNCE_TIME); // set debounce time to 50 milliseconds
}

void loop() {
  button.loop(); // MUST call the loop() function first

  if (button.isPressed())
    Serial.println("The button is pressed");

  if (button.isReleased())
    Serial.println("The button is released");
}
