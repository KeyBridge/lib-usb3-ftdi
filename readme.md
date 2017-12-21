# usb3-ftdi

A Java device driver to access FTDI's UART/FIFO chips including the popular bitbang mode.

# How to Use

Use the utility class **FTDIUtility** to find all FTDI devices attached to the host
computer system. Set the serial port configuration and then read from and write
to the device. e.g.

```java
    // Grab the first found device:
    Collection<IUsbDevice> devices = FTDIUtility.findFTDIDevices();
    IUsbDevice usbDevice = devices.iterator().next();

    // Wrap and claim the generic USB device as a FTDI device
    // This sets the serial port configuration to [115200, 8, N, 1, N].
    FTDI ftdiDevice = FTDI.getInstance(usbDevice);

    // Write data to the FTDI device input buffer
    ftdiDevice.write(new byte[]{ .... } );

    // Read data from the FTDI device output buffer
    byte[] usbFrame = ftdiDevice.read();
    while (usbFrame.length > 0) {
      System.out.println("   READ " + usbFrame.length + " bytes: " + ByteUtility.toString(usbFrame));
      usbFrame = ftdi.read();
    }
```

## Tested Compatibility

This (Java) library has been tested against, and is affirmed to support the
following FTDI devices.

- FTDI Devices: FT232BM/L/Q, FT245BM/L/Q, FT232RL/Q, FT245RL/Q, VNC1L with VDPS Firmware
  - idVendor  :      0403
  - idProduct :      6001
- FTDI Devices: FT2232C/D/L, FT2232HL/Q
  - idVendor  :      0403
  - idProduct :      6010
- FTDI Devices: FT4232HL/Q
  - idVendor  :      0403
  - idProduct :      6011

The following FTDI chips should be supported:
- FT4232H / FT2232H
- FT232R  / FT245R
- FT2232L / FT2232D / FT2232C
- FT232BM / FT245BM (and the BL/BQ variants)
- FT8U232AM / FT8U245AM

## Background

This (Java) device driver is based upon and translated from the original source
code driver library in the C located here: [libftdi](docs/libftdi). libFTDI is
an open source library to talk to FTDI chips: FT232BM, FT245BM, FT2232C, FT2232D,
FT245R and FT232H including the popular bitbang mode.

## Limits

This JAVA implementation does not support EEPROM programming.

# Requirements / Dependencies

This project requires **javax-usb3**, which is a JNI wrapper for libusb 1.x and
with run-time implementations for Linux (actively supported) plus OSX and Windows
(cross compiled).

# License

Apache 2.0.

# References

- [FT232 UART](docs/FTDI FT232R_v104.pdf)
- Application Notes:
  - [Determining USB Peripheral Device Class](docs/FTDI AN_174_Determining USB Peripheral Device Class.pdf)
  - [Baud Rates](docs/FTDI AN232B-05_BaudRates.pdf)
  - [Advanced Driver Options](docs/FTDI AN232B-10_Advanced_Driver_Options.pdf)
- [EEPROM bits and bytes](docs/EEPROM-structure)
- [udev rules for Linux](docs/99-libftdi.rules)
- [libftdi](http://www.intra2net.com/en/developer/libftdi/index.php)

# Contact

For more information about this and related software please contact
[Key Bridge](http://keybridge.ch)
