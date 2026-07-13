import smbus2
import bme280
from datetime import datetime
address = 0x76
port = 1
bus = smbus2.SMBus(port)
calibration_params = bme280.load_calibration_params(bus, address)

class BME280Sensor:
    def __init__(self, address=0x76, port=1):
        self.address = address
        self.port = port
        self.bus = smbus2.SMBus(port)
        self.calibration_params = bme280.load_calibration_params(self.bus, self.address)

        self.temperature = None
        self.humidity = None
        self.pressure = None
        self.timestamp = None

    def read(self):
        try:
            sample = bme280.sample(self.bus, self.address, self.calibration_params)
        except OSError:
            # I2C hiccup: reopen bus + reload calibration once
            self.bus.close()
            self.bus = smbus2.SMBus(self.port)
            self.calibration_params = bme280.load_calibration_params(self.bus, self.address)
            sample = bme280.sample(self.bus, self.address, self.calibration_params)

        self.temperature = round(sample.temperature, 2)
        self.humidity = round(sample.humidity, 2)
        self.pressure = round(sample.pressure, 2)  # hPa
        self.timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        return self