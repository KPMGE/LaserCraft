use std::env;
use std::fs::File;
use std::path::Path;

use crate::mqtt_helper;

use actix_multipart::Field;
use actix_multipart::Multipart;
use actix_web::get;
use actix_web::http::StatusCode;
use actix_web::ResponseError;
use actix_web::{post, HttpResponse, Responder};
use anyhow::anyhow;
use dotenv::dotenv;
use futures::StreamExt;
use futures::TryStreamExt;
use mqtt_helper::MqttHelper;
use std::io::Write;
use std::process::Command;
use uuid::Uuid;

#[derive(Debug)]
struct ApiError(anyhow::Error);

impl std::fmt::Display for ApiError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{:?}", self.0)
    }
}

impl ResponseError for ApiError {
    fn status_code(&self) -> actix_web::http::StatusCode {
        log::error!("ERROR: {:?}", self);
        StatusCode::BAD_REQUEST
    }
}

impl From<anyhow::Error> for ApiError {
    fn from(err: anyhow::Error) -> Self {
        ApiError(err)
    }
}

#[get("/healthcheck")]
async fn healthcheck() -> impl Responder {
    HttpResponse::Ok().finish()
}

#[post("/img")]
pub async fn process_image(mut payload: Multipart) -> Result<HttpResponse, ApiError> {
    let png_img_path = "./test-api.png";
    let svg_img_path = "./test-api.svg";
    let gcode_path = "./test-api.gcode";
    let gcode_img_path = "./test-api-gcode.png";

    let mut field = payload
        .try_next()
        .await
        .map_err(|e| anyhow!("Could not get image from multipart form: {e:?}"))?
        .ok_or(anyhow!("No image present on form"))?;
    let content_disposition = field.content_disposition();
    let filename = content_disposition.get_filename().unwrap_or_default();

    log::info!("Received file: {filename}");

    log::info!("Saving file: {filename} to disk");
    save_file_to_disk(png_img_path, &mut field).await?;

    log::info!("Converting image to svg...");
    convert_img_to_svg(png_img_path, svg_img_path)?;

    log::info!("Converting image to gcode...");
    convert_img_to_gcode(gcode_path, svg_img_path)?;

    log::info!("Converting gcode to png image...");
    convert_gcode_to_png(gcode_path, gcode_img_path)?;

    log::info!("Publishing image: {} to mqtt topic", gcode_img_path);
    publish_img(gcode_img_path)?;

    log::info!("Success!");

    Ok(HttpResponse::Ok().finish())
}

fn convert_img_to_svg(input_path: &str, output_path: &str) -> anyhow::Result<()> {
    vtracer::convert_image_to_svg(
        Path::new(input_path),
        Path::new(output_path),
        vtracer::Config::default(),
    )
    .map_err(|e| anyhow!("Could not convert image to svg: {e:?}"))
}

fn convert_img_to_gcode(gcode_path: &str, svg_path: &str) -> anyhow::Result<()> {
    Command::new("svg2gcode")
        .arg("--on")
        .arg("M3 S300")
        .arg("--off")
        .arg("M5")
        .arg("--out")
        .arg(gcode_path)
        .arg(svg_path)
        .spawn()
        .map_err(|e| anyhow!("Could not spawn command: {e:?}"))?
        .wait()
        .map_err(|e| anyhow!("Could execute command: {e:?}"))?;

    Ok(())
}

fn convert_gcode_to_png(gcode_path: &str, output_path: &str) -> anyhow::Result<()> {
    Command::new("gcode2image")
        .arg("--flip")
        .arg(gcode_path)
        .arg(output_path)
        .spawn()
        .map_err(|e| anyhow!("Could not spawn command: {e:?}"))?
        .wait()
        .map_err(|e| anyhow!("Could execute command: {e:?}"))?;

    Ok(())
}

fn publish_img(img_path: &str) -> anyhow::Result<()> {
    dotenv().ok();

    let mqtt_broker = env::var("MQTT_BROKER")?;
    let mqtt_topic = env::var("MQTT_TOPIC")?;
    let mqtt_client_prefix = env::var("MQTT_CLIENT_PREFIX")?;
    let uuid = Uuid::new_v4().to_string();
    let mqtt_client_id = mqtt_client_prefix + &uuid;

    let mqtt_helper = MqttHelper::new(mqtt_broker, mqtt_client_id)
        .map_err(|e| anyhow!("Could not create mqtt helper: {e:?}"))?;
    mqtt_helper
        .connect()
        .map_err(|e| anyhow!("Could not connect to mqtt server: {e:?}"))?;

    mqtt_helper
        .publish_image(&mqtt_topic, img_path)
        .map_err(|e| anyhow!("Could not publish image: {e:?}"))?;

    mqtt_helper.disconnect();

    Ok(())
}

async fn save_file_to_disk(file_path: &str, field: &mut Field) -> anyhow::Result<()> {
    let mut bytes = Vec::new();
    while let Some(chunk) = field.next().await {
        let chunk = chunk.map_err(|e| anyhow!("Could not parse chunk: {e:?}"))?;
        bytes.extend_from_slice(&chunk);
    }

    // save received file to disk
    let mut file = File::create(file_path)?;
    file.write_all(&bytes)?;

    Ok(())
}
