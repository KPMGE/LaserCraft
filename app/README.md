# LaserCraft - App
This is the LaserCraft app project, it is responsible for taking images and
sending them to our laser engraving machine!

With this app, you can either select an image from you galery, or take a picture
with your camera, then a preview of the engraved image will be shown, you can
either then engrave the image, you use another one!

This project is a native mobile app, developed using the [jetpack compose framework](https://developer.android.com/develop/ui/compose)

## How it works
This is a quite simple app, it takes an image, either from the galery of from
the cellphone camera and send it to the api. Then, it starts listening on an
mqtt topic where the preview of the gcode will be published.

Once an image is published in the topic, it shows it to the user who can then
ask to engrave it, in which case the app sends a http request to another route
on the api, or send another image.

## How to configure this project
Once you've opened this project up on [Android Studio](https://developer.android.com/studio) a file called
`local.properties` should be automatically generated for you. In this file, you
need to configure the connection to the api and mqtt server. You can do so
pasting the following in this file: 

```.properties
# api connection
api_base_url=

# mqtt connection
mqtt_broker_url=
mqtt_client_id_prefix=laser_craft_android_
mqtt_receive_image_topic=laser_craft_img
mqtt_user_name=
mqtt_password=
```

Feel free to change this configs the way suits you best!
