# LaserCraft API
This is the api for processing the image received from the LaserCraft app.

## How it works?
This api works by receiving an image via http. Then the image is converted into
svg using [vtracer](https://github.com/visioncortex/vtracer). This is done so that we can turn it into gcode using the rust library
[svg2gcode](https://crates.io/crates/svg2gcode).

After that, a preview of the generated gcode is created using [gcode2image](https://pypi.org/project/gcode2image/). Finally the preview is 
published in a mqtt topic. With that, the app gets a chance to show the user how the image will be
engraved.

## How to run it?
This whole project has been dockerized for easier use and deployment. So the
only thing you need to do before running it is, having docker properly installed
on your machine, set some environment variables. 

There's a file `.env.example` with the environment variables that are to
be set. You can, in the root of this project run: 

```bash
echo .env.example > .env
```

Now, open the `.env` file and correctly set the environment variables. Once
that's done you can run the api with docker-compose:

```bash
docker-compose up
```

## Api routes
For now there are only 2 routes on this api, there's a `/healthcheck` one, for
checking if the api is up and a `/img` route for processing an image.
