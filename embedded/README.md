# LaserCraft - Embedded
This is the project for the embedded part of LaserCraft. It will be deployed on
an [esp32](https://en.wikipedia.org/wiki/ESP32) that will then receive the gcode
and send it to our laser engraving machine!


## How to configure it?
First of all, make sure you got [Arduino IDE](https://www.arduino.cc/en/software) properly installed on your
machine and that you have selected the right type of board before sending the
code to the esp32.

Now, you can copy the code. Make sure to set the variables correctly for
connecting to wifi and mqtt broker. Once everything is properly configured, you
can deploy the code to esp32. It will log some useful information on the serial
monitor, like the available networks, and connection information.


## How to handle incoming data?
The data incoming is handled on the **mqtt_callback** function. For every
incoming data, this function is called!
