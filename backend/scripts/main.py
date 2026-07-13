from sensors.BME280 import BME280Sensor
import time
import csv
from datetime import datetime

if __name__ == "__main__":


    CSV_FILE = f"bme280_data_{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}.csv"

    # Falls Datei neu ist → Header schreiben
    try:
        with open(CSV_FILE, "x", newline="") as f:
            writer = csv.writer(f)
            writer.writerow([
                "timestamp",
                "temperature_c",
                "humidity_percent",
                "pressure_hpa"
            ])
    except FileExistsError:
        pass

    INTERVAL_SECONDS = 60  # Messintervall
    print("Starte Messung (STRG+C zum Beenden)")

    bme280_sensor = BME280Sensor()
    lastTemp = 0.0
    while True:
        bme280_sensor.read()
        with open(CSV_FILE, "a", newline="") as f:
            writer = csv.writer(f)
            writer.writerow([
                bme280_sensor.timestamp,
                bme280_sensor.temperature,
                bme280_sensor.humidity,
                bme280_sensor.pressure
            ])
        print(f"{bme280_sensor.timestamp} | {bme280_sensor.temperature} °C | {bme280_sensor.humidity} % | {bme280_sensor.pressure} hPa")
        time.sleep(INTERVAL_SECONDS)
