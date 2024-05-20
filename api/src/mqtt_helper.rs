use image::{io::Reader as ImageReader, ImageOutputFormat};
use paho_mqtt::{Client, ConnectOptionsBuilder, CreateOptionsBuilder, Message};
use std::time::Duration;

const QOS: i32 = 1;

pub struct MqttHelper {
    client: Client,
}

type HelperResult<A> = Result<A, Box<dyn std::error::Error>>;

impl MqttHelper {
    pub fn new(broker_url: String, client_id: String) -> HelperResult<Self> {
        // Define the set of options for the create.
        let create_opts = CreateOptionsBuilder::new()
            .server_uri(broker_url)
            .client_id(client_id)
            .finalize();

        // Create a client.
        let client = Client::new(create_opts)?;

        Ok(Self { client })
    }

    pub fn connect(&self) -> HelperResult<()> {
        // Define the set of options for the connection.
        let conn_opts = ConnectOptionsBuilder::new()
            .keep_alive_interval(Duration::from_secs(20))
            .clean_session(true)
            .finalize();

        self.client.connect(conn_opts)?;
        Ok(())
    }

    pub fn publish_image(&self, topic: &str, img_path: &str) -> HelperResult<()> {
        // Decode the image
        let img = ImageReader::open(img_path)?.decode()?;

        // Convert the image to a byte array
        let mut buffer: Vec<u8> = Vec::new();
        img.write_to(&mut buffer, ImageOutputFormat::Png)?;

        // publish image to topic
        let msg = Message::new(topic, buffer, QOS);
        self.client.publish(msg)?;

        Ok(())
    }

    pub fn disconnect(&self) {
        self.client.disconnect(None).unwrap();
    }
}
