mod api;
mod mqtt_helper;

use dotenv::dotenv;
use std::{env, sync::Arc};
use uuid::Uuid;

use actix_web::{web, App, HttpServer};
use anyhow::anyhow;
use mqtt_helper::MqttHelper;

extern crate paho_mqtt as mqtt;

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    dotenv().ok();
    env_logger::init();

    let api_host = env::var("API_HOST")?;
    let mqtt_broker = env::var("MQTT_BROKER")?;
    let mqtt_client_prefix = env::var("MQTT_CLIENT_PREFIX")?;
    let uuid = Uuid::new_v4().to_string();
    let mqtt_client_id = mqtt_client_prefix + &uuid;

    let mqtt_helper = MqttHelper::new(mqtt_broker, mqtt_client_id)
        .map_err(|e| anyhow!("Could not create mqtt helper: {e:?}"))?;
    mqtt_helper
        .connect()
        .map_err(|e| anyhow!("Could not connect to mqtt server: {e:?}"))?;

    log::info!("Mqtt server connected!");
    log::info!("Running on: http://{api_host}");

    HttpServer::new(move || {
        let helper_data = web::Data::new(Arc::new(mqtt_helper.clone()));

        App::new()
            .service(api::process_image)
            .service(api::engrave_img)
            .service(api::healthcheck)
            .app_data(helper_data)
    })
    .bind(api_host.clone())?
    .run()
    .await?;

    Ok(())
}
