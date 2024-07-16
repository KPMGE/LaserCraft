use image::{io::Reader as ImageReader, ImageOutputFormat};
use mqtt::{SslOptions, MQTT_VERSION_5};
use paho_mqtt::{Client, ConnectOptionsBuilder, CreateOptionsBuilder, Message};

const QOS: i32 = 1;

#[derive(Clone)]
pub struct MqttHelper {
    client: Client,
    user_name: String,
    password: String,
}

type HelperResult<A> = Result<A, Box<dyn std::error::Error>>;

impl MqttHelper {
    pub fn new(
        broker_url: String,
        client_id: String,
        user_name: String,
        password: String,
    ) -> HelperResult<Self> {
        let client_opts = CreateOptionsBuilder::new()
            .client_id(client_id)
            .mqtt_version(MQTT_VERSION_5)
            .server_uri(broker_url)
            .finalize();

        // Create the MQTT client
        let client = Client::new(client_opts).expect("Error creating client");

        Ok(Self {
            client,
            user_name,
            password,
        })
    }

    pub fn connect(&self) -> HelperResult<()> {
        let options = ConnectOptionsBuilder::new()
            .ssl_options(SslOptions::default())
            .clean_start(true)
            .user_name(self.user_name.clone())
            .password(self.password.clone())
            .finalize();

        self.client.connect(options)?;
        Ok(())
    }

    pub fn publish_gcode(&self, topic: &str, content: &str) -> HelperResult<()> {
        let msg = Message::new(topic, content, QOS);
        self.client.publish(msg)?;
        Ok(())
    }

    pub fn publish_image(&self, topic: &str, img_path: &str) -> HelperResult<()> {
        // Decode the image
        let img = ImageReader::open(img_path)?.decode()?;

        // Convert the image to a byte array
        let mut buffer: Vec<u8> = Vec::new();
        img.write_to(&mut buffer, ImageOutputFormat::Png)?;

        let msg = Message::new(topic, buffer, QOS);
        self.client.publish(msg)?;

        Ok(())
    }
}
