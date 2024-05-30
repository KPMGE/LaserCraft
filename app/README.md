# LaserCraft - App
This is the LaserCraft app project, it is responsible for taking images and
sending them to our laser engraving machine!

With this app, you can either select an image from you galery, or take a picture
with your camera, then a preview of the engraved image will be shown, you can
either then engrave the image, you use another one!

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
```

Feel free to change this configs the way suits you best!
