# LaserCraft API
This is the api for processing the image received from the LaserCraft app.

## How it works?
This api works by receiving an image via http. Then the image is converted into
svg using [vtracer](https://github.com/visioncortex/vtracer) and scaled using
[rsvg-convert](https://github.com/miyako/console-rsvg-convert). 

This is done so that we can turn it into gcode using the rust library
[svg2gcode](https://crates.io/crates/svg2gcode).

After that, a preview of the generated gcode is created using [gcode2image](https://pypi.org/project/gcode2image/). Finally the preview is 
published in a mqtt topic. With that, the app gets a chance to show the user how the image will be
engraved.

Once the `engrave` route is called, the generated gcode is read and load up into
memory, then it gets sliced and the first chunk is published into an mqtt topic. 

The api then starts listening on another mqtt topic, when a specific message is
received there, the api then publishes the next gcode chunk into the topic.

You can configure pretty much all those parameters through environment
variables. Take a look at the .env.example file to get a gist of the variables
you can set. Here's a brief overview of what they're meant for:

```bash
API_HOST=<host>:<port>                       # the api host
MQTT_BROKER=<host>:<port>                    # the mqtt broker the api will connect to
MQTT_PASSWORD=                               # mqtt broker password
MQTT_USER_NAME=                              # mqtt broker username
MQTT_CLIENT_PREFIX=laser_craft_api_server_   # api client prefix on mqtt 
MQTT_IMG_TOPIC=laser_craft_img               # topic where the preview image is published
MQTT_GCODE_TOPIC=laser_craft_gcode           # topic where the gcode is published
MQTT_GCODE_NEXT_CHUNK_TOPIC=gcode_next_chunk # topic where the api listens for the next gcode message
MQTT_GCODE_NEXT_CHUNK_MESSAGE=next_chunk     # next gcode message
GCODE_CHUNK_AMOUNT_LINES=5                   # amount of lines of each gcode chunk
IMG_TARGET_WIDTH=52                          # target width for image scaling
IMG_TARGET_HEIGHT=52                         # target height for image scaling
GCODE_WRITE_FEEDRATE=30                      # gcode feedrate for engraving
GCODE_POSITION_FEEDRATE=500                  # gcode feedrate for positioning 
```

## How to run it?
This whole project has been dockerized for easier use and deployment. So maek
sure you have [docker](https://www.docker.com/) properly installed on your machine. 

Then you'll need to set the environment variables. There's a file `.env.example` with the environment variables that are to
be set. You can, in the root of this project run: 

```bash
echo .env.example > .env
```

Now, open the `.env` file and correctly set the environment variables according
to your needs. Once
that's done you can run the api with docker-compose:

```bash
docker-compose up
```

> [!NOTE]  
> The docker-compose starts up a hivemq mqtt broker, you can set it in your
> environment variables if you don't have a deployed mqtt broker!

## Api routes
For now there are only 3 routes on this api, there's a `/healthcheck` one, for
checking if the api is up and a `/img` route for processing an image. And
finally, a `/engrave` one for sending the gcode to the engraving machine.
