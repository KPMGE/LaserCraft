mod mqtt_helper;
mod api;
mod utils;

use dotenv::dotenv;
use std::env;

use actix_web::{App, HttpServer};

extern crate paho_mqtt as mqtt;

#[actix_web::main]
async fn main() -> std::io::Result<()> {
    dotenv().ok();

    let host = env::var("API_HOST").unwrap();

    HttpServer::new(|| App::new()
        .service(api::process_image)
        .service(api::healthcheck))
        .bind(host)?
        .run()
        .await
}
