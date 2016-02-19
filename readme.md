# javax-usb3-ftdi

A Java library (using javax-usb3) to talk to FTDI's UART/FIFO chips including the
popular bitbang mode.

This (Java) device driver is based upon and translated from the original source
code driver library in the C located here: [libftdi](docs/libftdi).

The following FTDI chips are supported:
- FT4232H / FT2232H
- FT232R  / FT245R
- FT2232L / FT2232D / FT2232C
- FT232BM / FT245BM (and the BL/BQ variants)
- FT8U232AM / FT8U245AM

# Requirements

This project requires **javax-usb3**, which is a JNI wrapper for libusb 1.x and
with run-time implementations for Linux (actively supported) plus OSX and Windows
(cross compiled).

# How to Use

Use the utility class **FTDIUtility** to find all FTDI devices attached to the host
computer system. Set the serial port configuration and then read from and write
to the device. e.g.

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

# Reference Material

- [FT232 UART](docs/FTDI FT232R_v104.pdf)
- Application Notes:
  - [Determining USB Peripheral Device Class](docs/FTDI AN_174_Determining USB Peripheral Device Class.pdf)
  - [Baud Rates](docs/FTDI AN232B-05_BaudRates.pdf)
  - [Advanced Driver Options](docs/FTDI AN232B-10_Advanced_Driver_Options.pdf)
- [EEPROM bits and bytes](docs/EEPROM-structure)
- [udev rules for Linux](docs/99-libftdi.rules)

