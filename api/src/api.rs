use std::env;
use std::fs::File;
use std::path::Path;

use crate::mqtt_helper;

use actix_multipart::Multipart;
use actix_web::get;
use dotenv::dotenv;
use futures::StreamExt;
use futures::TryStreamExt;
use mqtt_helper::MqttHelper;
use std::io::Write;
use uuid::Uuid;
use std::process::Command;
use actix_web::{post, HttpResponse, Responder};
use actix_multipart::Field;

#[get("/healthcheck")]
async fn healthcheck() -> impl Responder {
    HttpResponse::Ok().finish()
}

#[post("/img")]
pub async fn process_image(mut payload: Multipart) -> impl Responder {
    let png_img_path = "./test-api.png";
    let svg_img_path = "./test-api.svg";
    let gcode_path = "./test-api.gcode";
    let gcode_img_path = "./test-api-gcode.png";

    let mut field = payload.try_next().await.unwrap().unwrap();
    let content_disposition = field.content_disposition();
    let filename = content_disposition.get_filename().unwrap_or_default();

    println!("Received file: {filename}");

    println!("Saving file: {filename} to disk");
    save_file_to_disk(png_img_path, &mut field).await;

    println!("Converting image to svg...");
    convert_img_to_svg(png_img_path, svg_img_path);

    println!("Converting image to gcode...");
    convert_img_to_gcode(gcode_path, svg_img_path);

    println!("Converting gcode to png image...");
    convert_gcode_to_png(gcode_path, gcode_img_path);

    println!("Publishing image: {} to mqtt topic", gcode_img_path);
    publish_img(gcode_img_path);

    println!("Success!");

    HttpResponse::Ok().finish()
}

fn convert_img_to_svg(input_path: &str, output_path: &str) {
    vtracer::convert_image_to_svg(
        Path::new(input_path),
        Path::new(output_path),
        vtracer::Config::default(),
    )
    .expect("Error while converting image");
}

fn convert_img_to_gcode(gcode_path: &str, svg_path: &str) {
    Command::new("svg2gcode")
        .arg("--on")
        .arg("M3 S300")
        .arg("--off")
        .arg("M5")
        .arg("--out")
        .arg(gcode_path)
        .arg(svg_path)
        .spawn().unwrap()
        .wait()
        .unwrap();
}

fn convert_gcode_to_png(gcode_path: &str, output_path: &str) {
    Command::new("gcode2image")
        .arg("--flip")
        .arg(gcode_path)
        .arg(output_path)
        .spawn().unwrap()
        .wait()
        .unwrap();
}

fn publish_img(img_path: &str) {
    dotenv().ok();
    
    let mqtt_broker = env::var("MQTT_BROKER").unwrap();
    let mqtt_topic = env::var("MQTT_TOPIC").unwrap();
    let mqtt_client_prefix = env::var("MQTT_CLIENT_PREFIX").unwrap();
    let uuid = Uuid::new_v4().to_string();
    let mqtt_client_id = mqtt_client_prefix + &uuid;
    
    let mqtt_helper = MqttHelper::new(mqtt_broker, mqtt_client_id).unwrap();
    mqtt_helper.connect().unwrap();

    mqtt_helper.publish_image(&mqtt_topic, img_path).unwrap();
    
    mqtt_helper.disconnect();
}

async fn save_file_to_disk(file_path: &str, field: &mut Field) {
    let mut bytes = Vec::new();
    while let Some(chunk) = field.next().await {
        let chunk = chunk.unwrap();
        bytes.extend_from_slice(&chunk);
    }

    // save received file to disk
    let mut file = File::create(file_path).unwrap();
    file.write_all(&bytes).unwrap();
}
