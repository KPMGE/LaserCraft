mod api;
mod mqtt_helper;

use dotenv::dotenv;
use std::env;

use actix_web::{App, HttpServer};

extern crate paho_mqtt as mqtt;

#[actix_web::main]
async fn main() -> anyhow::Result<()> {
    dotenv().ok();

    let host = env::var("API_HOST")?;

    println!("Running on: http://{host}");

    HttpServer::new(|| {
        App::new()
            .service(api::process_image)
            .service(api::healthcheck)
    })
    .bind(host.clone())?
    .run()
    .await?;

    Ok(())
}
