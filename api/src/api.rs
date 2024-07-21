use regex::Regex;
use std::env;
use std::fs::File;
use std::io::{BufRead, BufReader, Cursor, Read};
use std::path::Path;
use std::sync::Arc;

use crate::mqtt_helper;

use actix_multipart::Field;
use actix_multipart::Multipart;
use actix_web::get;
use actix_web::http::StatusCode;
use actix_web::rt::task::JoinHandle;
use actix_web::web;
use actix_web::ResponseError;
use actix_web::{post, HttpResponse, Responder};
use anyhow::anyhow;
use futures::StreamExt;
use futures::TryStreamExt;
use lazy_static::lazy_static;
use mqtt_helper::MqttHelper;
use std::io::Write;
use std::process::Command;
use std::sync::Mutex;

const PNG_IMG_PATH: &str = "./image.png";
const SVG_IMG_PATH: &str = "./image.svg";
const SCALED_SVG_IMG_PATH: &str = "./scaled-image.svg";
const GCODE_IMG_PATH: &str = "./image-gcode.png";
const GCODE_PATH: &str = "./image.gcode";

#[derive(Debug)]
struct ApiError(anyhow::Error);

lazy_static! {
    static ref mqtt_send_chunk_handle: Mutex<Option<JoinHandle<()>>> = Mutex::new(None);
}

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
    log::debug!("Checking for mqtt send gcode chunk background task");
    if let Some(handle) = mqtt_send_chunk_handle
        .lock()
        .map_err(|e| anyhow!("Error while locking mutex: {e:?}"))?
        .take()
    {
        log::debug!("Closing mqtt send gcode chunk background task");
        handle.abort();
    }

    let mqtt_gcode_next_chunk_topic = env::var("MQTT_GCODE_NEXT_CHUNK_TOPIC")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    let mqtt_gcode_next_chunk_message = env::var("MQTT_GCODE_NEXT_CHUNK_MESSAGE")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    let mut gcode_file =
        File::open(GCODE_PATH).map_err(|e| anyhow!("Could no open gcode file: {e:?}"))?;

    let mut file_contents = String::new();
    gcode_file.read_to_string(&mut file_contents).unwrap();
    let simplified_gcode = simplify_gcode(&file_contents).unwrap();

    let cursor = Cursor::new(simplified_gcode);
    let mut reader = BufReader::new(cursor);

    publish_next_gcode_chunk(&mut reader, mqtt_helper.clone())?;

    let handle = actix_web::rt::spawn(async move {
        let _ = mqtt_helper
            .subscribe(&mqtt_gcode_next_chunk_topic, |msg| {
                if msg == mqtt_gcode_next_chunk_message {
                    if let Err(e) = publish_next_gcode_chunk(&mut reader, mqtt_helper.clone()) {
                        log::error!("error while publish_next_gcode_chunk: {e:?}");
                    }
                }
            })
            .map_err(|e| log::error!("error while getting mqtt message: {e:?}"));
    });

    log::debug!("Saving mqtt_send_chunk_handle");
    *mqtt_send_chunk_handle
        .lock()
        .map_err(|e| anyhow!("Error while locking mutex: {e:?}"))? = Some(handle);

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

    log::info!("Received file: {filename}");
    log::info!("Saving file: {filename} to disk...");
    save_file_to_disk(PNG_IMG_PATH, &mut field).await?;

    log::info!("Converting image to svg...");
    convert_img_to_svg(PNG_IMG_PATH, SVG_IMG_PATH)?;
    log::info!("Image converted to svg successfully!");

    log::info!("Scaling svg image...");
    scale_image(SVG_IMG_PATH, SCALED_SVG_IMG_PATH)?;
    log::info!("Image scaled successfully!");

    log::info!("Converting image to gcode...");
    convert_img_to_gcode(GCODE_PATH, SCALED_SVG_IMG_PATH)?;
    log::info!("Image converted to gcode successfully");

    log::info!("Converting gcode to png image...");
    convert_gcode_to_png(GCODE_PATH, GCODE_IMG_PATH)?;
    log::info!("Gcode converted to png successfully!");

    let mqtt_img_topic = env::var("MQTT_IMG_TOPIC")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    log::info!(
        "Publishing image '{}' to mqtt topic '{}'",
        GCODE_IMG_PATH,
        mqtt_img_topic
    );
    mqtt_helper
        .publish_image(&mqtt_img_topic, GCODE_IMG_PATH)
        .map_err(|e| anyhow!("Could not publish image: {e:?}"))?;

    log::info!("Image successfully published!");

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
    let gcode_write_feedrate = env::var("GCODE_WRITE_FEEDRATE")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<i32>()
        .map_err(|e| anyhow!("Could not parse GCODE_WRITE_FEEDRATE to i32: {e:?}"))?;

    Command::new("svg2gcode")
        .arg("--feedrate")
        .arg(gcode_write_feedrate.to_string())
        .arg("--dimensions")
        .arg("52mm,52mm")
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

fn scale_image(img_path: &str, scaled_img_path: &str) -> anyhow::Result<()> {
    let img_target_width = env::var("IMG_TARGET_WIDTH")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<u32>()
        .map_err(|e| anyhow!("Could not parse IMG_TARGET_WIDTH to u32: {e:?}"))?;

    let img_target_height = env::var("IMG_TARGET_HEIGHT")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<u32>()
        .map_err(|e| anyhow!("Could not parse IMG_TARGET_WIDTH to u32: {e:?}"))?;

    Command::new("rsvg-convert")
        .arg("--keep-aspect-ratio")
        .arg("--width")
        .arg(img_target_width.to_string())
        .arg("--height")
        .arg(img_target_height.to_string())
        .arg(img_path)
        .arg("--format")
        .arg("svg")
        .arg("--output")
        .arg(scaled_img_path)
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

    let mut file = File::create(file_path)?;
    file.write_all(&bytes)?;

    Ok(())
}

fn publish_next_gcode_chunk(
    reader: &mut BufReader<Cursor<String>>,
    mqtt_helper: web::Data<Arc<MqttHelper>>,
) -> Result<(), ApiError> {
    let mqtt_gcode_topic = env::var("MQTT_GCODE_TOPIC")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?;

    let gcode_chunk_amount_lines = env::var("GCODE_CHUNK_AMOUNT_LINES")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<usize>()
        .map_err(|e| anyhow!("Could not parse GCODE_CHUNK_AMOUNT_LINES into usize: {e:?}"))?;

    let lines = reader
        .lines()
        .take(gcode_chunk_amount_lines)
        .filter_map(Result::ok)
        .collect::<Vec<_>>()
        .join("\n");

    mqtt_helper
        .publish_gcode(&mqtt_gcode_topic, &lines)
        .map_err(|e| anyhow!("Could not publish gcode: {e:?}"))?;

    log::info!("Gcode chunk published successfully!");

    Ok(())
}

fn simplify_gcode(buffer: &str) -> anyhow::Result<String, anyhow::Error> {
    let gcode_position_feedrate = env::var("GCODE_POSITION_FEEDRATE")
        .map_err(|e| anyhow!("Could not load environment variable: {e:?}"))?
        .parse::<i32>()
        .map_err(|e| anyhow!("Could not parse GCODE_POSITION_FEEDRATE to i32: {e:?}"))?;

    let g0_re = Regex::new(r"G0 (?P<rest_of_line>.*)").unwrap();
    let replace_g0_str = format!("G1 ${{rest_of_line}} F{gcode_position_feedrate}");
    let gcode_no_g0 = g0_re.replace_all(buffer, replace_g0_str);

    let number_format_re = Regex::new(r"(?P<formated_number>\d+\.\d\d)\d+").unwrap();
    let gcode_formated = number_format_re.replace_all(&gcode_no_g0, "${formated_number}");

    let gcode_comment_re = Regex::new(r";.*").unwrap();
    let gcode_no_comment = gcode_comment_re.replace_all(&gcode_formated, "");

    Ok(gcode_no_comment.to_string())
}
