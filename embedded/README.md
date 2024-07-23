# LaserCraft - Embedded
This is the project for the embedded part of LaserCraft. It will be deployed on
an [esp32](https://en.wikipedia.org/wiki/ESP32) that will then receive the gcode
and send it to our laser engraving machine!


## How to configure it?
First of all, make sure you got [Arduino IDE](https://www.arduino.cc/en/software) properly installed on your
machine and that you have selected the right type of board before sending the
code to the esp32.

Now, you can copy the code. Make sure to set the variables correctly for
connecting to wifi and mqtt broker.

If you're trying to connect to a mqtt instance through ssl, make sure to provide
a certificate. You can set the `root_ca` variable for this!

Once everything is properly configured, you
can deploy the code to esp32. It will log some useful information on the serial
monitor, like the available networks, and connection information. Some data will
be also logged on the display.


## How it works?
Basically, there are 4 states this program can be in: `IDLE`, `WAITING_GCODE`,
`PRINTING` or `DONE`. 

The `IDLE` state is the state where the machine is doing nothing, is just
hanging around waiting for some data to start printing.

The `PRINTING` state on the other hand, is when the machine is actually sending
the gcode commands to arduino. This state is set when some gcode is received on
the mqtt topic.

Next up, the `WAITING_GCODE` state happens when all the commands from a gcode
chunk have been consumed, then the esp32 publishes into an mqtt topic, asking
for the next chunk, then it goes to this waiting state.

Finally, the `DONE` state is when the gcode has been fully consumed and the
image has been engraved!

