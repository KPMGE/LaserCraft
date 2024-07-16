FROM rust:1.78-bookworm as build

WORKDIR /build

COPY . ./api

RUN apt-get update -y && apt-get upgrade -y && apt-get install git cmake -y

RUN cd api && cargo build --release

RUN cargo install vtracer

RUN git clone https://github.com/sameer/svg2gcode.git \
      && cd svg2gcode \
      && cargo build --release


FROM python:3.12-bookworm

COPY --from=build /usr/local/cargo/bin/vtracer /usr/bin/vtracer
COPY --from=build /build/svg2gcode/target/release/svg2gcode /usr/bin/svg2gcode
COPY --from=build /build/api/target/release/laser_craft_api /usr/bin/laser_craft_api 

WORKDIR /api

RUN pip install gcode2image

EXPOSE 3333

CMD [ "laser_craft_api" ]
