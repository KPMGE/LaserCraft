use std::env;
use std::fs::File;
use std::io::{BufRead, BufReader, Read};
use std::path::Path;
use std::sync::Arc;

use crate::mqtt_helper;

use actix_multipart::Field;
use actix_multipart::Multipart;
use actix_web::get;
use actix_web::http::StatusCode;
use actix_web::web;
use actix_web::ResponseError;
use actix_web::{post, HttpResponse, Responder};
use anyhow::anyhow;
use futures::StreamExt;
use futures::TryStreamExt;
use mqtt_helper::MqttHelper;
use std::io::Write;
use std::process::Command;

const PNG_IMG_PATH: &str = "./image.png";
const SVG_IMG_PATH: &str = "./image.svg";
const GCODE_IMG_PATH: &str = "./image-gcode.png";
const GCODE_PATH: &str = "./image.gcode";

#[derive(Debug)]
struct ApiError(anyhow::Error);

impl std::fmt::Display for ApiError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        log::error!("{}", self.0);
        write!(f, "")
    }
}

impl ResponseError for ApiError {
    fn status_code(&self) -> actix_web::http::StatusCode {
        StatusCode::INTERNAL_SERVER_ERROR
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

#[get("/engrave")]
pub async fn engrave_img(
    mqtt_helper: web::Data<Arc<MqttHelper>>,
) -> Result<HttpResponse, ApiError> {
    log::debug!("Publishing image gcode on mqtt topic...");

    let mqtt_gcode_topic = env::var("MQTT_GCODE_TOPIC")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    let mqtt_gcode_buffer_size = env::var("MQTT_GCODE_BUFFER_SIZE")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<u8>()
        .map_err(|e| anyhow!("buffer size must be a number: {e:?}"))?;

    let mqtt_publish_gcode_delay_in_secs = env::var("MQTT_PUBLISH_GCODE_DELAY_IN_SECS")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<u64>()
        .map_err(|e| anyhow!("delay must be a number: {e:?}"))?;

    log::debug!("Gcode buffer size: {:?}", mqtt_gcode_buffer_size);

    let gcode_file =
        File::open(GCODE_PATH).map_err(|e| anyhow!("Could no open gcode file: {e:?}"))?;

    let mut reader = BufReader::new(gcode_file);

    let mut buffer: Vec<u8> = vec![0, mqtt_gcode_buffer_size];

    while reader.fill_buf().unwrap().len() > 0 {
        reader
            .read(&mut buffer)
            .map_err(|e| anyhow!("Could not read buffer from gcode file: {e:?}"))?;

        log::debug!("Trying to publish gcode chunk...");
        let gcode_chunk = String::from_utf8(buffer.clone())
            .map_err(|e| anyhow!("Could not convert buffer into string: {e:?}"))?;

        mqtt_helper
            .publish_gcode(&mqtt_gcode_topic, &gcode_chunk)
            .map_err(|e| anyhow!("Could not publish gcode: {e:?}"))?;

        log::debug!("Gcode chunk published successfully!");

        log::debug!("Sleeping for {} seconds", mqtt_publish_gcode_delay_in_secs);
        tokio::time::sleep(tokio::time::Duration::from_secs(
            mqtt_publish_gcode_delay_in_secs,
        ))
        .await;
        log::debug!("Done sleeping!");
    }

    log::debug!("Gcode published successfully!");

    Ok(HttpResponse::Ok().finish())
}

#[post("/img")]
pub async fn process_image(
    mut payload: Multipart,
    mqtt_helper: web::Data<Arc<MqttHelper>>,
) -> Result<HttpResponse, ApiError> {
    let mut field = payload
        .try_next()
        .await
        .map_err(|e| anyhow!("Could not get image from multipart form: {e:?}"))?
        .ok_or(anyhow!("No image present on form"))?;
    let content_disposition = field.content_disposition();
    let filename = content_disposition.get_filename().unwrap_or_default();

    log::debug!("Received file: {filename}");
    log::debug!("Saving file: {filename} to disk...");
    save_file_to_disk(PNG_IMG_PATH, &mut field).await?;

    log::debug!("Converting image to svg...");
    convert_img_to_svg(PNG_IMG_PATH, SVG_IMG_PATH)?;

    log::debug!("Converting image to gcode...");
    convert_img_to_gcode(GCODE_PATH, SVG_IMG_PATH)?;

    log::debug!("Converting gcode to png image...");
    convert_gcode_to_png(GCODE_PATH, GCODE_IMG_PATH)?;

    let mqtt_img_topic = env::var("MQTT_IMG_TOPIC")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    log::debug!(
        "Publishing image '{}' to mqtt topic '{}'",
        GCODE_IMG_PATH,
        mqtt_img_topic
    );
    mqtt_helper
        .publish_image(&mqtt_img_topic, GCODE_IMG_PATH)
        .map_err(|e| anyhow!("Could not publish image: {e:?}"))?;

    log::debug!("Image successfully published!");

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
